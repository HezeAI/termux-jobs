#!/usr/bin/env python3
"""Termux Job's optional per-SSH-user agent, protocol 1. No public listener/root setup.
RPC uses stdin/stdout over authenticated SSH. Only the daemon owns task Popen handles.
"""
import argparse
import contextlib
import fcntl
import hashlib
import http.server
import json
import mimetypes
import os
from pathlib import Path
import re
import secrets
import selectors
import shutil
import signal
import socket
import sqlite3
import subprocess
import sys
import threading
import time
import urllib.parse
import uuid

VERSION = '1.0.0'
PROTOCOL = 1
ROOT = Path(__file__).absolute().parent
MAX_LOG = 2 * 1024 * 1024
MAX_TASKS = 40
MAX_RUNNING = 2
ID = re.compile(r'^[a-f0-9]{32}$')
SERVICE = re.compile(r'^[A-Za-z0-9_][A-Za-z0-9_.@:-]{0,180}\.service$')

def fail(code):
    raise ValueError(code)

def atomic(path, value):
    tmp = path.with_name('.' + path.name + '.' + secrets.token_hex(8))
    fd = os.open(tmp, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    try:
        with os.fdopen(fd, 'w') as out:
            json.dump(value, out, ensure_ascii=False); out.flush(); os.fsync(out.fileno())
        os.replace(tmp, path)
    finally:
        with contextlib.suppress(FileNotFoundError): tmp.unlink()

def read(path, default=None):
    try:
        if path.stat().st_size > 3 * 1024 * 1024: fail('RECORD_TOO_LARGE')
        return json.loads(path.read_text())
    except FileNotFoundError: return default

def directory():
    if ROOT.is_symlink() or ROOT.stat().st_uid != os.getuid(): fail('UNSAFE_AGENT_DIRECTORY')
    os.chmod(ROOT, 0o700)
    tasks = ROOT / 'tasks'
    if tasks.is_symlink(): fail('UNSAFE_AGENT_DIRECTORY')
    tasks.mkdir(mode=0o700, exist_ok=True)

def task_path(value):
    if not isinstance(value, str) or not ID.fullmatch(value): fail('INVALID_TASK')
    return ROOT / 'tasks' / (value + '.json')

def path(value):
    if not isinstance(value, str) or not value or len(value) > 4096 or '\0' in value: fail('INVALID_PATH')
    return Path(os.path.expanduser(value)).resolve(strict=True)

def records():
    return sorted((r for p in (ROOT / 'tasks').glob('*.json') if (r := read(p)) is not None), key=lambda r:r['created'], reverse=True)

def entry_path(value):
    if not isinstance(value,str) or not value or '\0' in value:fail('INVALID_PATH')
    raw=Path(os.path.expanduser(value)).absolute()
    target=raw.parent.resolve(strict=True)/raw.name
    if target==ROOT or ROOT in target.parents:fail('AGENT_FILES_PROTECTED')
    return target


def proc_stat():
    parts = Path('/proc/stat').read_text().splitlines()[0].split()[1:]
    n = list(map(int, parts[:8]))
    return sum(n), n[3] + n[4]

def metrics(previous=None):
    total, idle = proc_stat()
    mem = {}
    for line in Path('/proc/meminfo').read_text().splitlines():
        a = line.split(); mem[a[0].rstrip(':')] = int(a[1]) * 1024
    disk = shutil.disk_usage(str(Path.home()))
    cpu = None
    if previous and total > previous[0]: cpu = round(100 * (1 - (idle-previous[1])/(total-previous[0])), 1)
    return {'time':int(time.time()), 'cpu':cpu, 'ramTotal':mem['MemTotal'],
            'ramUsed':mem['MemTotal']-mem.get('MemAvailable',mem['MemFree']),
            'diskTotal':disk.total,'diskUsed':disk.used,'load':os.getloadavg()[0]}, (total,idle)

def database():
    db = sqlite3.connect(str(ROOT/'metrics.sqlite'), timeout=5)
    db.execute('create table if not exists samples(t integer primary key, data text not null)')
    return db

def daemon_active():
    fd = os.open(ROOT/'daemon.lock', os.O_RDWR | os.O_CREAT, 0o600)
    try:
        try: fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError: return True
        fcntl.flock(fd, fcntl.LOCK_UN); return False
    finally: os.close(fd)

def ensure_daemon():
    if (ROOT/'removing').exists(): fail('AGENT_REMOVING')
    if not daemon_active():
        subprocess.Popen([sys.executable,'-I',str(ROOT/'agent.py'),'--daemon'], stdin=subprocess.DEVNULL,
                         stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,start_new_session=True,close_fds=True)
        for _ in range(50):
            if daemon_active(): break
            time.sleep(.1)
    if not daemon_active(): fail('DAEMON_START_FAILED')

def clean_tasks():
    old = records()
    for r in old[MAX_TASKS:]:
        if r['state'] in ('queued','starting','running','unmanaged'): continue
        for suffix in ('.json','.log','.cancel'):
            with contextlib.suppress(FileNotFoundError): (ROOT/'tasks'/(r['id']+suffix)).unlink()

def tail(log):
    try:
        with open(log,'rb') as f:
            f.seek(0,2); f.seek(max(0,f.tell()-32768)); return f.read(32768).decode('utf-8','replace')
    except FileNotFoundError: return ''

class MediaHandler(http.server.BaseHTTPRequestHandler):
    def log_message(self, *args): pass  # URLs contain bearer capabilities; never log.
    def do_HEAD(self): self.serve(False)
    def do_GET(self): self.serve(True)
    def serve(self, body):
        token = urllib.parse.urlparse(self.path).path.lstrip('/')
        if not re.fullmatch('[a-f0-9]{64}',token): self.send_error(404); return
        lease = read(ROOT/'streams.json',{}).get(token)
        if not lease or lease['expires'] < time.time(): self.send_error(404); return
        try:
            with open(lease['path'],'rb') as f:
                stat = os.fstat(f.fileno())
                if [stat.st_dev,stat.st_ino,stat.st_size,stat.st_mtime_ns] != lease['identity']:
                    self.send_error(409); return
                size=stat.st_size; start=0; end=size-1
                value=self.headers.get('Range')
                if value:
                    match=re.fullmatch(r'bytes=(\d*)-(\d*)',value)
                    if not match or not any(match.groups()): self.send_error(416); return
                    if match[1]:
                        start=int(match[1]);end=min(size-1,int(match[2])) if match[2] else size-1
                    else: start=max(0,size-int(match[2]))
                    if start>end or start>=size: self.send_error(416); return
                self.send_response(206 if value else 200)
                self.send_header('Content-Type',mimetypes.guess_type(lease['path'])[0] or 'application/octet-stream')
                self.send_header('Accept-Ranges','bytes');self.send_header('Cache-Control','no-store')
                self.send_header('Content-Length',str(max(0,end-start+1)))
                if value:self.send_header('Content-Range',f'bytes {start}-{end}/{size}')
                self.end_headers()
                if body:
                    f.seek(start); remaining=end-start+1
                    while remaining>0:
                        b=f.read(min(65536,remaining))
                        if not b:break
                        self.wfile.write(b);remaining-=len(b)
        except (OSError,ValueError): return

class MediaServer(http.server.ThreadingHTTPServer):
    daemon_threads=True
    request_queue_size=8
    def __init__(self):
        self.slots=threading.BoundedSemaphore(8)
        super().__init__(('127.0.0.1',0),MediaHandler)
    def process_request(self,request,address):
        if not self.slots.acquire(False):request.close();return
        try:super().process_request(request,address)
        except BaseException:self.slots.release();raise
    def process_request_thread(self,request,address):
        try:super().process_request_thread(request,address)
        finally:self.slots.release()
    def get_request(self):
        request,address=super().get_request();request.settimeout(15);return request,address

def daemon():
    directory()
    lock=open(ROOT/'daemon.lock','a+')
    try:fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
    except BlockingIOError:return
    for r in records():
        if r['state'] in ('starting','running'):
            # A restarted daemon MUST NOT signal an inherited/stale PID.
            r['state']='unmanaged';r['ended']=time.time();atomic(task_path(r['id']),r)
    media=MediaServer(); threading.Thread(target=media.serve_forever,daemon=True).start()
    atomic(ROOT/'daemon.json',{'port':media.server_port,'protocol':PROTOCOL})
    db=database(); selector=selectors.DefaultSelector(); running={}; previous=None; last=0
    try:
        while True:
            if (ROOT/'removing').exists() and not running:break
            now=time.time()
            if now-last>=30:
                sample,previous=metrics(previous);last=now
                db.execute('insert or replace into samples values(?,?)',(sample['time'],json.dumps(sample)))
                db.execute('delete from samples where t<?',(int(now)-86400,));db.commit()
            for r in reversed(records()):
                if r['state']!='queued' or len(running)>=MAX_RUNNING:continue
                if (ROOT/'tasks'/(r['id']+'.cancel')).exists():
                    r.update(state='cancelled',ended=now);atomic(task_path(r['id']),r);continue
                r.update(state='starting',started=now);atomic(task_path(r['id']),r)
                try:
                    proc=subprocess.Popen(['/bin/bash','--noprofile','--norc','-c',r['command']],cwd=r['directory'],
                         stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,
                         start_new_session=True,close_fds=True)
                    os.set_blocking(proc.stdout.fileno(),False); selector.register(proc.stdout,selectors.EVENT_READ,r['id'])
                    running[r['id']]={'proc':proc,'record':r,'bytes':0,'stopping':0}
                    r.update(state='running',started=now);atomic(task_path(r['id']),r)
                except OSError:
                    r.update(state='failed',ended=now,error='START_FAILED');atomic(task_path(r['id']),r)
            for key,_ in selector.select(.2):
                job=running.get(key.data)
                if not job:continue
                try:b=os.read(key.fd,65536)
                except BlockingIOError:continue
                if b:
                    # Once capped, drain/discard to keep the child unblocked; disk use is bounded.
                    room=MAX_LOG-job['bytes']
                    if room>0:
                        with open(ROOT/'tasks'/(key.data+'.log'),'ab') as log: log.write(b[:room])
                        job['bytes']+=min(room,len(b))
                else:
                    selector.unregister(key.fileobj); key.fileobj.close()
            for ident,job in list(running.items()):
                proc=job['proc'];r=job['record']
                # poll()/wait happen only here. A live/unreaped child pins its PID;
                # group signalling is never based on a stored PID or another daemon.
                if job['stopping']:
                    if now-job['stopping'] <= 5: continue
                    with contextlib.suppress(ProcessLookupError):os.killpg(proc.pid,signal.SIGKILL)
                    # Only after final signal may poll reap the leader.
                code=proc.poll()
                if code is None:
                    stop=(ROOT/'tasks'/(ident+'.cancel')).exists() or now-r['started']>86400
                    if stop and not job['stopping']:
                        with contextlib.suppress(ProcessLookupError):os.killpg(proc.pid,signal.SIGTERM)
                        job['stopping']=now
                    continue
                # Background descendants are not retained as managed jobs. Closing the
                # pipe only affects this task; users must run foreground commands.
                try:selector.unregister(proc.stdout)
                except (KeyError,ValueError):pass
                if not proc.stdout.closed:
                    while True:
                        try:b=os.read(proc.stdout.fileno(),65536)
                        except (BlockingIOError,OSError):break
                        if not b:break
                        room=MAX_LOG-job['bytes']
                        if room>0:
                            with open(ROOT/'tasks'/(ident+'.log'),'ab') as log:log.write(b[:room])
                            job['bytes']+=min(room,len(b))
                    proc.stdout.close()
                r.update(state='cancelled' if job['stopping'] else 'done' if code==0 else 'failed',ended=now,exitCode=code,logCapped=job['bytes']>=MAX_LOG)
                atomic(task_path(ident),r);del running[ident]
            clean_tasks()
    finally:
        media.shutdown();media.server_close();db.close();selector.close()
        with contextlib.suppress(FileNotFoundError):(ROOT/'daemon.json').unlink()
        # Keep lock held until all cleanup ends; deleting only happens from uninstall RPC.
        lock.close()

def rpc(req):
    directory();op=req.get('op')
    if op=='hello':return {'protocol':PROTOCOL,'version':VERSION,'user':os.getuid(),'home':str(Path.home()),'agent':True}
    if op=='uninstall':
        # admission and removal serialize with task creation; external user processes untouched.
        with open(ROOT/'admission.lock','a+') as lock:
            fcntl.flock(lock,fcntl.LOCK_EX)
            if any(r['state'] in ('queued','starting','running','unmanaged') for r in records()):fail('ACTIVE_TASKS')
            atomic(ROOT/'removing',{})
            for _ in range(100):
                if not daemon_active():break
                time.sleep(.1)
            if daemon_active():
                (ROOT/'removing').unlink();fail('DAEMON_BUSY')
            if (ROOT/'streams.json').exists():
                # User explicitly confirms active video streams stop during uninstall.
                pass
            shutil.rmtree(ROOT)
        return {'removed':True}
    if op not in {'status','tasks','start','cancel','log','forget','list','rename','delete','properties','stream','services','service-log','service-restart'}:fail('UNKNOWN_OPERATION')
    ensure_daemon()
    if op=='status':
        with database() as db:
            samples=[json.loads(row[0]) for row in db.execute('select data from samples order by t desc limit 120')][::-1]
        current,_=metrics()
        if samples:current['cpu']=samples[-1]['cpu']
        return {'protocol':PROTOCOL,'version':VERSION,'metrics':current,'history':samples,'tasks':records(),'user':os.getuid()}
    if op=='tasks':return {'tasks':records()}
    if op=='start':
        command=req.get('command','');title=req.get('title','')
        if not isinstance(command,str) or not command.strip() or len(command)>8192 or '\0' in command:fail('INVALID_COMMAND')
        if not isinstance(title,str) or len(title)>160:fail('INVALID_TITLE')
        cwd=path(req.get('directory','~'))
        if not cwd.is_dir():fail('NOT_DIRECTORY')
        with open(ROOT/'admission.lock','a+') as lock:
            fcntl.flock(lock,fcntl.LOCK_EX)
            if (ROOT/'removing').exists():fail('AGENT_REMOVING')
            if sum(r['state'] in ('queued','starting','running','unmanaged') for r in records())>=10:fail('QUEUE_FULL')
            r={'id':uuid.uuid4().hex,'title':title or command[:60],'command':command,'directory':str(cwd),'state':'queued','created':time.time()}
            atomic(task_path(r['id']),r)
        return r
    if op=='cancel':
        p=task_path(req.get('id'));r=read(p)
        if not r or r['state'] not in ('queued','starting','running'):fail('TASK_NOT_MANAGED')
        atomic(ROOT/'tasks'/(r['id']+'.cancel'),{});return {'requested':True}
    if op=='log':
        p=task_path(req.get('id'));r=read(p)
        if not r:fail('TASK_NOT_FOUND')
        return {'task':r,'log':tail(p.with_suffix('.log'))}
    if op=='forget':
        p=task_path(req.get('id'));r=read(p)
        if not r or r['state'] in ('queued','starting','running'):fail('ACTIVE_TASKS')
        for suffix in ('.json','.log','.cancel'):
            with contextlib.suppress(FileNotFoundError):p.with_suffix(suffix).unlink()
        return {'forgotten':True}
    if op=='list':
        folder=path(req.get('path','~'))
        if not folder.is_dir():fail('NOT_DIRECTORY')
        entries=[]
        with os.scandir(folder) as listing:
            for e in listing:
                if len(entries)>=500:break
                try:s=e.stat();entries.append({'name':e.name,'path':str(folder/e.name),'directory':e.is_dir(),'size':s.st_size,'modified':s.st_mtime})
                except OSError:continue
        entries.sort(key=lambda e:(not e['directory'],e['name'].casefold()))
        return {'path':str(folder),'parent':str(folder.parent),'entries':entries,'limit':500}
    if op=='rename':
        source=entry_path(req.get('path'));name=req.get('name','')
        if not isinstance(name,str) or not name or name in ('.','..') or '/' in name or '\0' in name:fail('INVALID_NAME')
        dest=source.parent/name
        # no overwrites; Linux renameat2 RENAME_NOREPLACE if available, otherwise hardlink/unlink for files.
        if source.is_dir():fail('DIRECTORY_RENAME_UNSUPPORTED')
        os.link(source,dest,follow_symlinks=False);source.unlink();return {'path':str(dest)}
    if op=='delete':
        target=entry_path(req.get('path'))
        if not target.is_file() and not target.is_symlink():fail('ONLY_FILES')
        target.unlink();return {'deleted':True}
    if op=='properties':
        target=path(req.get('path'));s=target.stat();result={'path':str(target),'size':s.st_size,'modified':s.st_mtime}
        if target.is_file() and shutil.which('ffprobe'):
            r=subprocess.run(['ffprobe','-v','error','-protocol_whitelist','file,pipe','-show_entries','format=duration:stream=codec_name,width,height','-of','json',str(target)],stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,timeout=10)
            if len(r.stdout)<65536 and r.returncode==0:result['media']=json.loads(r.stdout)
        return result
    if op=='stream':
        target=path(req.get('path'))
        if not target.is_file():fail('ONLY_FILES')
        s=target.stat();token=secrets.token_hex(32)
        with open(ROOT/'streams.lock','a+') as lock:
            fcntl.flock(lock,fcntl.LOCK_EX)
            leases={k:v for k,v in read(ROOT/'streams.json',{}).items() if v['expires']>time.time()}
            if len(leases)>=8:fail('STREAM_LIMIT')
            leases[token]={'path':str(target),'expires':time.time()+1800,'identity':[s.st_dev,s.st_ino,s.st_size,s.st_mtime_ns]}
            atomic(ROOT/'streams.json',leases)
        daemon_info=read(ROOT/'daemon.json')
        if not daemon_info:fail('DAEMON_STARTING')
        return {'port':daemon_info['port'],'token':token,'mime':mimetypes.guess_type(target.name)[0] or 'application/octet-stream','expiresIn':1800}
    if op=='services':
        r=subprocess.run(['systemctl','list-units','--type=service','--all','--no-pager','--plain','--no-legend'],stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,timeout=10)
        return {'text':r.stdout[:65536].decode('utf-8','replace'),'available':r.returncode==0}
    if op in ('service-log','service-restart'):
        name=req.get('name','')
        if not isinstance(name,str) or not SERVICE.fullmatch(name):fail('INVALID_SERVICE')
        args=['systemctl','--no-ask-password','restart',name] if op=='service-restart' else ['journalctl','-u',name,'-n','100','--no-pager','-o','short-iso']
        r=subprocess.run(args,stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=20)
        if r.returncode:fail('SERVICE_PERMISSION_OR_UNAVAILABLE')
        return {'text':r.stdout[:65536].decode('utf-8','replace'),'ok':True}
    fail('UNKNOWN_OPERATION')

def main():
    os.umask(0o077)
    parser=argparse.ArgumentParser();parser.add_argument('--daemon',action='store_true');parser.add_argument('--rpc',action='store_true');args=parser.parse_args()
    if args.daemon:daemon();return
    try:
        raw=sys.stdin.buffer.read(65537)
        if len(raw)>65536:fail('REQUEST_TOO_LARGE')
        request=json.loads(raw)
        if not isinstance(request,dict):fail('INVALID_REQUEST')
        result=rpc(request);print(json.dumps({'ok':True,'result':result},ensure_ascii=False))
    except Exception as e:
        known={FileNotFoundError:'FILE_NOT_FOUND',PermissionError:'PERMISSION_DENIED',NotADirectoryError:'NOT_DIRECTORY',FileExistsError:'ALREADY_EXISTS',subprocess.TimeoutExpired:'COMMAND_TIMEOUT'}
        code=str(e) if isinstance(e,ValueError) and re.fullmatch('[A-Z_]{1,80}',str(e)) else known.get(type(e),'AGENT_OPERATION_FAILED')
        print(json.dumps({'ok':False,'error':code}));sys.exit(1)
if __name__=='__main__':main()
