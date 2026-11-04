"""Stock CLI experimental window mechanism only, fake provider and private HOME.
No current-account eligibility claim, no model capability gate override or alpha API.
"""
from pathlib import Path
base=Path(__file__).with_name('probe-codex.py').read_text();separator="try:\n start();thread=rpc('thread/start'";assert base.count(separator)==1
exec(compile(base.split(separator)[0],str(Path(__file__).with_name('probe-codex.py')),'exec'),globals())
# This explicit experimental opt-in exists only in the newly generated fake HOME.
c=HOME/'config.toml';c.write_text('features.token_budget={enabled=true,use_history_notes_extension=false}\n'+c.read_text())
start_original=start
owned_start=None
def identity():
 try:return Path('/proc',str(process.pid),'stat').read_text().rsplit(')',1)[1].split()[19]
 except FileNotFoundError:return None
def start():
 global owned_start
 try:start_original()
 finally:
  if process is not None:owned_start=identity()
def stop():
 if process is None or process.poll() is not None:return
 process.stdin.close()
 try:process.wait(timeout=8)
 except subprocess.TimeoutExpired:
  assert identity()==owned_start,'No signal: process identity mismatch';process.terminate()
  try:process.wait(timeout=8)
  except subprocess.TimeoutExpired:
   assert identity()==owned_start,'No signal: process identity mismatch';process.kill();process.wait(timeout=8)

def api_history():
 data=rpc('thread/turns/list',{'threadId':thread,'limit':100,'itemsView':'full'})['data']
 assert not rpc('thread/turns/list',{'threadId':thread,'limit':100,'itemsView':'full'}).get('nextCursor')
 return {t['id']:hashlib.sha256(json.dumps(t,sort_keys=True).encode()).hexdigest() for t in data}
result={'scope':'Actual pinned stock binary with experimental token_budget enabled only in fake HOME; scripted provider, not real model/account or automatic selection','completed':False}
originals={n:hashlib.sha256((PROJECT/n).read_bytes()).hexdigest() for n in ('png-600-0.png','png-600-1.png')}
try:
 start();thread=rpc('thread/start',{'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access'})['thread']['id']
 turn('task-and-images',[text('REQ_KEEP_ORIGINALS REQ_NO_PUBLISH REQ_VERIFY_RESULT. Preserve exact task facts: ARTIFICIAL_DETAIL_7821. Original paths '+str(PROJECT/'png-600-0.png')+' and '+str(PROJECT/'png-600-1.png')),image('png-600-0.png'),image('png-600-1.png')])
 before=api_history();result['originalApiTurnHashes']=before
 turn('request-stock-new-context',[text('SYNTHETIC invoke the declared new_context once')],tool={'name':'new_context','args':{}})
 turn('continue-after-window',[text('SYNTHETIC continue after window without replaying old images')])
 result['afterWindowRequest']=dict(records[-1]);after=api_history();result['originalTurnsPreserved']=all(after.get(k)==v for k,v in before.items())
 stop();start();rpc('thread/resume',{'threadId':thread,'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access','excludeTurns':True})
 turn('reopen-after-window',[text('SYNTHETIC reopen after window without replaying old images')])
 result['afterReopenRequest']=dict(records[-1]);turn('reopen-old-original',[text('SYNTHETIC inspect the archived original using the native tool')],tool={'name':'view_image','args':{'path':str(PROJECT/'png-600-0.png')}})
 result['oldOriginalReviewRequest']=dict(records[-1]);result['originalTurnsPreservedAfterReopen']=all(api_history().get(k)==v for k,v in before.items())
 assert result['originalTurnsPreserved'] and result['originalTurnsPreservedAfterReopen']
 assert result['afterWindowRequest']['imageCount']==0 and result['afterReopenRequest']['imageCount']==0 and result['oldOriginalReviewRequest']['imageCount']==1
 # A clean request and preserved archive are not sufficient task retention.
 result['rawRequirementTextStillInRequest']=result['afterReopenRequest']['requiredMarkers']
 result['completed']=True
finally:
 stop();server.shutdown();server.server_close();persist()
 result['allOwnedNativeExited']=process.poll() is not None if process else True
 result['originalsUnchanged']=all(hashlib.sha256((PROJECT/n).read_bytes()).hexdigest()==v for n,v in originals.items())
 (RUN/'stock-window-result.json').write_text(json.dumps(result,indent=2)+'\n');(OUT/'latest-stock-window-run.txt').write_text(str(RUN)+'\n');print(json.dumps({'run':str(RUN),'completed':result['completed'],'originalsUnchanged':result['originalsUnchanged'],'allOwnedNativeExited':result['allOwnedNativeExited']}),flush=True)
