#!/usr/bin/env python3
"""Real curl/HTTPS and production Job queue, task-owned fake environments only."""
from pathlib import Path
import hashlib,http.server,ssl,threading,subprocess,shutil,os,time,signal,json,shlex,tempfile,runpy
REPO=Path(__file__).resolve().parents[2];BASE=REPO/'.verification/preparation-download-bounds';BASE.mkdir(parents=True,exist_ok=True)
ROOT=Path(tempfile.mkdtemp(prefix='tls-',dir=BASE));curl=shutil.which('curl');cert=ROOT/'server.crt';key=ROOT/'fixture.key'
config=ROOT/'openssl.cnf';config.write_text('[req]\ndistinguished_name=dn\nx509_extensions=ext\nprompt=no\n[dn]\nCN=local-fixture\n[ext]\nsubjectAltName=IP:127.0.0.1\nbasicConstraints=critical,CA:TRUE\n')
env={'PATH':os.environ['PATH'],'HOME':str(ROOT),'TMPDIR':str(ROOT),'CURL_CA_BUNDLE':str(cert)}
subprocess.run(['openssl','req','-x509','-newkey','rsa:2048','-nodes','-days','1','-config',str(config),'-keyout',str(key),'-out',str(cert)],env=env,check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL);key.chmod(0o600)
production=(REPO/'app/src/main/assets/agent-download.sh').read_text();policy='local connect=45 idle=180 budget=7200 attempts=3 delay=2';assert production.count(policy)==1
helper=ROOT/'agent-download.sh';helper.write_text(production.replace(policy,'local connect=1 idle=1 budget=12 attempts=3 delay=1'))
release=threading.Event();counts={};lock=threading.Lock();payload=b'SYNTHETIC_NOT_A_SECRET';queue_connected=ROOT/'queue-connected'
class Handler(http.server.BaseHTTPRequestHandler):
 def log_message(self,*a):pass
 def do_GET(self):
  with lock:counts[self.path]=counts.get(self.path,0)+1;number=counts[self.path]
  if self.path=='/exhaust' or self.path=='/retry' and number==1:
   self.send_response(503);self.send_header('Content-Length','0');self.end_headers();return
  self.send_response(200);self.send_header('Content-Length',str(128 if self.path in ['/stall','/queue','/interrupt'] else len(payload)));self.end_headers()
  try:
   if self.path in ['/stall','/queue','/interrupt']:
    self.wfile.write(b'x');self.wfile.flush()
    if self.path=='/queue':queue_connected.write_text('first byte received')
    if self.path=='/interrupt':(ROOT/'interrupt-connected').write_text('yes')
    release.wait(60)
   elif self.path=='/slow':
    for c in payload:self.wfile.write(bytes([c]));self.wfile.flush();time.sleep(.12)
   else:self.wfile.write(payload);self.wfile.flush()
  except (BrokenPipeError,ConnectionResetError,ssl.SSLError):pass
server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);server.daemon_threads=True
ctx=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);ctx.load_cert_chain(cert,key);server.socket=ctx.wrap_socket(server.socket,server_side=True);threading.Thread(target=server.serve_forever,daemon=True).start();url='https://127.0.0.1:'+str(server.server_port)
# A TCP connection that never responds to the TLS ClientHello.
plain=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);plain.daemon_threads=True
class NoHandshake(http.server.BaseHTTPRequestHandler):
 def handle(self):release.wait(60)
 def log_message(self,*a):pass
plain.RequestHandlerClass=NoHandshake;threading.Thread(target=plain.serve_forever,daemon=True).start()
results=[]
def command(route,source=helper,base=url):return 'source '+shlex.quote(str(source))+'; jobs_agent_download claude-cli '+shlex.quote(str(ROOT/'body.bin'))+' '+shlex.quote(base+route)
def run(name,route,code,source=helper,base=url):
 started=time.monotonic();r=subprocess.run(['/bin/bash','--noprofile','--norc','-c',command(route,source,base)],env=env,capture_output=True,text=True,timeout=25)
 assert r.returncode==code,(name,r.returncode,r.stdout,r.stderr)
 assert 'SYNTHETIC' not in r.stdout+r.stderr and 'curl:' not in r.stdout+r.stderr
 results.append(dict(name=name,exit=code,seconds=round(time.monotonic()-started,2),records=r.stdout.splitlines()+r.stderr.splitlines()));return r
try:
 run('stalled TLS connection','/connect',28,base='https://127.0.0.1:'+str(plain.server_port))
 run('body stops after first byte','/stall',28)
 run('slow continuous successful body','/slow',0);assert (ROOT/'body.bin').read_bytes()==payload
 assert results[-1]['seconds']>1 # Longer than scaled idle period; ongoing progress is accepted.
 run('finite HTTP retry exhaustion','/exhaust',22);assert counts['/exhaust']==3
 run('retry then success','/retry',0);assert counts['/retry']==2
 budget=ROOT/'budget.sh';budget.write_text(helper.read_text().replace('budget=12','budget=1'))
 r=run('native attempt budgets exhausted','/slow',28,source=budget);assert 'ERROR:COMPONENT_DOWNLOAD_TIMEOUT' in r.stderr
 run('next explicit invocation succeeds','/complete',0)
 # Capture the exact owned curl PID. No name-based/group/global signals.
 bin=ROOT/'bin';bin.mkdir();pidfile=ROOT/'curl.pid';wrapper=bin/'curl';wrapper.write_text('#!/bin/bash\nprintf "%s" "$$" > '+shlex.quote(str(pidfile))+'\nexec '+shlex.quote(curl)+' "$@"\n');wrapper.chmod(0o700)
 interrupt_helper=ROOT/'interrupt.sh';interrupt_helper.write_text(helper.read_text().replace('idle=1','idle=10'))
 penv=dict(env,PATH=str(bin)+':'+env['PATH']);p=subprocess.Popen(['/bin/bash','--noprofile','--norc','-c',command('/interrupt',interrupt_helper)],env=penv,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
 deadline=time.monotonic()+10
 while not (ROOT/'interrupt-connected').exists():assert time.monotonic()<deadline and p.poll() is None;time.sleep(.02)
 pid=int(pidfile.read_text());stat=Path('/proc',str(pid),'stat').read_text();rest=stat[stat.rindex(')')+2:].split();assert int(rest[1])==p.pid
 assert (url+'/interrupt').encode() in Path('/proc',str(pid),'cmdline').read_bytes().split(b'\0')
 current=Path('/proc',str(pid),'stat').read_text();current_rest=current[current.rindex(')')+2:].split();assert current_rest[1]==rest[1] and current_rest[19]==rest[19]
 os.kill(pid,signal.SIGTERM);out,err=p.communicate(timeout=10);assert p.returncode==143 and 'ERROR:COMPONENT_DOWNLOAD_INTERRUPTED' in err;assert 'Terminated' not in out+err and 'curl --' not in out+err
 results.append(dict(name='owned transfer interruption',exit=143,identityVerified=True,records=out.splitlines()+err.splitlines()))
 # Reuse existing task-owned Java stubs, not a second implementation of the Job.
 fixture=runpy.run_path(str(REPO/'scripts/tests/test_agent_preparation_output.py'))
 src,classes,prefix,files,tmp=(fixture[k] for k in ['src','classes','prefix','files','tmp']);android=fixture['android']
 shellenv=src/'com/termux/shared/shell/TermuxShellUtils.java';s=shellenv.read_text().replace('"LANG=C.UTF-8"','"LANG=C.UTF-8",'+json.dumps('CURL_CA_BUNDLE='+str(cert)));shellenv.write_text(s)
 # Host fixture invokes the actual downloader; its EXIT cleanup deliberately outlasts curl.
 cleanup_started=ROOT/'cleanup-started';cleanup_done=ROOT/'cleanup-done';second_started=ROOT/'second-started';retry=ROOT/'retry'
 host='jobs_agent_host_prepare(){ printf "%s" "$$" > '+shlex.quote(str(ROOT/'owned-shell.pid'))+'; trap '+shlex.quote('touch '+shlex.quote(str(cleanup_started))+'; sleep 1; touch '+shlex.quote(str(cleanup_done)))+' EXIT; source '+shlex.quote(str(helper))+'; local route=queue; [[ ! -f '+shlex.quote(str(retry))+' ]] || route=complete; jobs_agent_download claude-cli '+shlex.quote(str(ROOT/'queue-body'))+' '+shlex.quote(url)+'/"$route"; }\n'
 (files/'.agent-preparation/agent-host-setup.sh').write_text(host)
 java='''package com.termux.app.workspace;import java.nio.file.*;public class QueueDownloadProbe{
 static void await(java.util.function.BooleanSupplier p)throws Exception{long end=System.nanoTime()+30000000000L;while(!p.getAsBoolean()){if(System.nanoTime()>end)throw new AssertionError("fixture deadline");Thread.sleep(10);}}
 public static void main(String[] args){try{Path root=Path.of(args[0]);android.content.Context c=new android.content.Context();WorkspaceStore.Session firstProfile=new WorkspaceStore.Session(),otherProfile=new WorkspaceStore.Session();firstProfile.agent="claude";otherProfile.agent="codex";AgentPreparation.Runner check=(ctx,p)->new AgentPreparation.Plan();
 AgentPreparationOperation.Job first=AgentPreparationOperation.start(c,firstProfile,check,AgentPreparationOperation::prepare);await(()->Files.exists(root.resolve("queue-connected")));
 AgentPreparationOperation.Job next=AgentPreparationOperation.start(c,otherProfile,check,(ctx,p,o)->{if(!Files.exists(root.resolve("cleanup-done")))throw new AssertionError("queue released before child cleanup");long pid=Long.parseLong(Files.readString(root.resolve("owned-shell.pid")));if(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false))throw new AssertionError("owned child still alive");Files.writeString(root.resolve("second-started"),"yes");return new AgentPreparation.Plan();});
 if(first!=AgentPreparationOperation.start(c,firstProfile,check,(ctx,p,o)->{throw new AssertionError("duplicate");}))throw new AssertionError("duplicate Job");Runnable observer=()->{};first.observers.add(observer);first.observers.remove(observer);
 await(()->Files.exists(root.resolve("cleanup-started")));if(!first.active||Files.exists(root.resolve("second-started")))throw new AssertionError("child still cleaning up");await(()->!first.active&&!next.active);
 if(!first.stage.equals("failed")||first.exitCode==null||first.exitCode!=28||!first.error.startsWith("COMPONENT_DOWNLOAD_")||!Files.exists(root.resolve("second-started")))throw new AssertionError("failed timeout handling");
 Files.writeString(root.resolve("retry"),"yes");AgentPreparationOperation.Job repeated=AgentPreparationOperation.start(c,firstProfile,check,AgentPreparationOperation::prepare);await(()->!repeated.active);if(!repeated.stage.equals("complete"))throw new AssertionError("retry");
 Files.writeString(root.resolve("queue-result.json"),"{\\"queueWaitsForChildCleanup\\":true,\\"sameJobReused\\":true,\\"observerDetachDoesNotCancel\\":true,\\"nextJobRuns\\":true,\\"retrySucceeds\\":true,\\"structuredError\\":true}");System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}}'''
 (src/'com/termux/app/workspace/QueueDownloadProbe.java').write_text(java)
 production_java=REPO/'app/src/main/java/com/termux/app/workspace'
 subprocess.run(['javac','-J-Djava.io.tmpdir='+str(tmp),'-cp',str(android),'-d',str(classes),*(str(p) for p in src.rglob('*.java')),*(str(production_java/n) for n in ['AgentPreparationOperation.java','AgentPreparationOutput.java','EnvironmentInstallProgress.java','EnvironmentSetupStatus.java'])],check=True,capture_output=True,text=True)
 q=subprocess.run(['java','-Djava.io.tmpdir='+str(tmp),'-Daudit.files='+str(files),'-cp',str(classes)+':'+str(android),'com.termux.app.workspace.QueueDownloadProbe',str(ROOT)],capture_output=True,text=True,timeout=40);assert q.returncode==0,(q.stdout,q.stderr)
 results.append(dict(name='production queue and reader',ownedChildExitChecked=True,**json.loads((ROOT/'queue-result.json').read_text())))
finally:release.set();server.shutdown();server.server_close();plain.shutdown();plain.server_close()
(BASE/'download-results.json').write_text(json.dumps(dict(productionHelperSha256=hashlib.sha256(production.encode()).hexdigest(),realInstallation=False,androidUi=False,externalConnections=0,policy='production control flow; connect/idle/budget/backoff shortened only in fixture copy',results=results),indent=2)+'\n')
print(json.dumps(results,indent=2))
