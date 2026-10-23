#!/usr/bin/env python3
"""Ephemeral no-tool app-server test; summaries only, no credentials inspected."""
import json
import os
from pathlib import Path
import queue
import signal
import subprocess
import sys
import threading
import time

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'.verification/codex-geohide'
LAUNCH=ROOT/'.tools/codex-geohide/codex-via-selectel'

def run(interrupt=False):
    os.umask(0o077)
    name='stream-recovery' if interrupt else 'stream'
    events=queue.Queue();deltas=[];errors=[];methods={};cut=False;thread_id=None;done=False
    before=json.loads(subprocess.check_output([str(LAUNCH),'status'],text=True))
    started=time.monotonic();first=None
    with (OUT/(name+'.stderr')).open('w') as err:
        child=subprocess.Popen([str(LAUNCH),'-c','project_doc_max_bytes=0',
            '-c','model_reasoning_effort="low"','app-server','--listen','stdio://'],
            stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=err,text=True,
            cwd=OUT/'empty',start_new_session=True,bufsize=1)
        def reader():
            for line in child.stdout:
                try:events.put(json.loads(line))
                except ValueError:pass
            events.put(None)
        threading.Thread(target=reader,daemon=True).start()
        def send(value):
            child.stdin.write(json.dumps(value)+'\n');child.stdin.flush()
        try:
            send({'id':1,'method':'initialize','params':{'clientInfo':{'name':'private-tunnel-check','version':'1'},'capabilities':{'experimentalApi':True}}})
            while time.monotonic()-started<150:
                try:r=events.get(timeout=1)
                except queue.Empty:continue
                if r is None:break
                if 'error' in r:
                    errors.append({'rpcCode':r['error'].get('code')});break
                if r.get('id')==1:
                    send({'method':'initialized','params':{}})
                    send({'id':2,'method':'thread/start','params':{'cwd':str(OUT/'empty'),'ephemeral':True,'model':'gpt-6.1-sol','approvalPolicy':'never','sandbox':'read-only','config':{'project_doc_max_bytes':0},'baseInstructions':'You are testing streaming connectivity. Do not use tools or inspect files. Follow the user text request only.'}})
                elif r.get('id')==2:
                    thread_id=r['result']['thread']['id']
                    prompt='Do not use tools. Write 50 numbered short sentences about the sky, in English. End with STREAM_OK.'
                    send({'id':3,'method':'turn/start','params':{'threadId':thread_id,'input':[{'type':'text','text':prompt}],'effort':'low'}})
                method=r.get('method','')
                if method:
                    methods[method]=methods.get(method,0)+1
                if method=='item/agentMessage/delta':
                    deltas.append(r.get('params',{}).get('delta',''))
                    if first is None:first=time.monotonic()-started
                    if interrupt and not cut:
                        # Drops only this setup's SSH process. Existing chats use another route.
                        subprocess.run([str(LAUNCH),'reconnect'],stdout=subprocess.DEVNULL,check=True)
                        cut=True
                if method=='error':
                    value=r.get('params',{});errors.append({'retrying':value.get('willRetry')})
                if method=='turn/completed':
                    status=r.get('params',{}).get('turn',{}).get('status')
                    done=status=='completed';break
        finally:
            if child.poll() is None:
                os.killpg(child.pid,signal.SIGTERM)
                try:child.wait(timeout=5)
                except subprocess.TimeoutExpired:os.killpg(child.pid,signal.SIGKILL);child.wait()
            child.stdin.close();child.stdout.close()
    # A separate request can be retried after restoration even when the old stream cannot resume.
    after=json.loads(subprocess.check_output([str(LAUNCH),'start'],text=True))
    result={'completed':done,'deltaEvents':len(deltas),'textChars':sum(map(len,deltas)),
        'endMarker':'STREAM_OK' in ''.join(deltas),'elapsedSeconds':round(time.monotonic()-started,2),
        'firstDeltaSeconds':round(first,2) if first is not None else None,
        'interrupted':cut,'tunnelBefore':before,'tunnelAfter':after,'errors':errors,
        'eventCounts':methods}
    (OUT/(name+'-result.json')).write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:v for k,v in result.items() if k!='eventCounts'},indent=2))
    return done

if __name__=='__main__':raise SystemExit(0 if run('--interrupt' in sys.argv) else 1)
