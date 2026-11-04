"""Raised attachment limits: guarded native/adapter measurements, no old APKs.
Android admission is source-derived. Pillow preparation is NOT Android Bitmap.
"""
from pathlib import Path
base=Path(__file__).with_name('probe-codex-versions.py').read_text()
separator='try:\n    start();thread=';assert base.count(separator)==1
exec(compile(base.split(separator)[0],str(Path(__file__).with_name('probe-codex-versions.py')),'exec'),globals())
import random
from PIL import Image
refs=('v2.0.5','v2.0.6','v2.1.3')
# Inspect only artificial image bytes actually received by the HTTP collector.
import io
summarize_original=summarize
def summarize(q,raw):
    value=summarize_original(q,raw);received=[]
    def visit(v):
        if isinstance(v,dict):
            if v.get('type')=='input_image':
                url=v.get('image_url','');assert isinstance(url,str) and url.startswith('data:image/')
                decoded=base64.b64decode(url.split(',',1)[1],validate=True)
                with Image.open(io.BytesIO(decoded)) as picture:
                    received.append({'decodedImageBytes':len(decoded),'sha256':hashlib.sha256(decoded).hexdigest(),'format':picture.format,'width':picture.width,'height':picture.height})
            for child in v.values():visit(child)
        elif isinstance(v,list):
            for child in v:visit(child)
    visit(q.get('input',[]));value['receivedImageMetadata']=received;return value
# All signal targets are Popen handles captured here with /proc start identities.
def identity(child):
    try:return Path('/proc',str(child.pid),'stat').read_text().rsplit(')',1)[1].split()[19]
    except FileNotFoundError:return None
owned_identities={}
def owned_stop(child):
    if child is None or child.poll() is not None:return
    assert identity(child)==owned_identities[child], 'No signal: owned identity mismatch'
    child.terminate()
    try:child.wait(timeout=12)
    except subprocess.TimeoutExpired:
        assert identity(child)==owned_identities[child], 'No signal: owned identity mismatch'
        child.kill();child.wait(timeout=8)
start_original=start
stop_original=stop
def start():
    try:start_original()
    finally:
        if process is not None:owned_identities[process]=identity(process)
def stop():
    if process is None or process.poll() is not None:return
    process.stdin.close()
    try:process.wait(timeout=8)
    except subprocess.TimeoutExpired:owned_stop(process)
node_original=node_child
def node_child(args,label):
    child=node_original(args,label);owned_identities[child]=identity(child);return child

data=RUN/'limit-data';data.mkdir();files=[]
for name,edge in [('small',600),('under20',2400),('over20',3000)]:
    source=data/(name+'.png')
    image_value=Image.frombytes('RGB',(edge,edge),random.Random('limits:'+name).randbytes(edge*edge*3))
    image_value.save(source)
    files.append({'name':name,'file':source,'type':'image','bytes':source.stat().st_size})
assert files[0]['bytes']<20*1024*1024 and files[1]['bytes']<20*1024*1024<files[2]['bytes']<100*1024*1024
binary=data/'large.bin'
with binary.open('wb') as f:f.truncate(300*1024*1024)
files.append({'name':'binary300','file':binary,'type':'binary','bytes':binary.stat().st_size})
for f in files:
    f['sha256']=hashlib.sha256(f['file'].read_bytes()).hexdigest()
limits=[]
for ref in refs:
    source=subprocess.check_output(['git','-C',str(repo),'show',ref+':app/src/main/java/com/termux/app/workspace/ChatAttachments.java'],env={'PATH':os.environ['PATH'],'HOME':str(HOME)}).decode()
    # Assert exact reviewed source conditions rather than guessing a version number.
    old='MAX_FILE=256L*1024*1024, MAX_IMAGE=20L*1024*1024' in source
    if not old:assert 'MAX_FILE=5L*1024*1024*1024, MAX_IMAGE=100L*1024*1024' in source
    assert 'file.length()>20L*1024*1024' in source or old
    assert 'bitmap(file,1280)' in source and 'Bitmap.CompressFormat.JPEG,85' in source
    limits.append({'ref':ref,'sourceSha256':hashlib.sha256(source.encode()).hexdigest(),'maxImageBytes':(20 if old else 100)*1024*1024,'maxFileBytes':(256*1024*1024 if old else 5*1024*1024*1024),'cases':[]})
try:
    start();threads={}
    thread=rpc('thread/start',{'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access'})['thread']['id']
    turn('persisted-short-seed',[text('SYNTHETIC short control')])
    for row in limits:
        for f in files:
            threads[row['ref'],f['name']]=rpc('thread/fork',{'threadId':thread,'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access','excludeTurns':True})['thread']['id']
    stop()
    launcher=node_child([private/'codex-launch.cjs',endpoint,'danger-full-access','never','1','en'],'limits-launch')
    end=time.monotonic()+30
    while not endpoint.exists():
        assert time.monotonic()<end and launcher.poll() is None
        time.sleep(.05)
    for row in limits:
        ref=row['ref'];row['adapterSources']=install_fixture_assets(ref)
        for f in files:
            bound=row['maxImageBytes'] if f['type']=='image' else row['maxFileBytes']
            c={'name':f['name'],'originalBytes':f['bytes'],'originalSha256':f['sha256'],'androidAdmissionFromSource':f['bytes']<=bound}
            row['cases'].append(c)
            if not c['androidAdmissionFromSource']:
                c.update(outcome='SOURCE_GATE_REJECTS_NO_INFERENCE',providerRequestNotAttempted=True);continue
            ident=threads[ref,f['name']];chat,_=start_chat(ident)
            aid=uuid.uuid4().hex;directory=private/'chat-attachments'/aid;directory.mkdir(parents=True)
            original=directory/('original'+f['file'].suffix)
            if f['type']=='binary':
                with original.open('wb') as stream:stream.truncate(f['bytes'])
            else:shutil.copy2(f['file'],original)
            attachment={'id':aid,'type':f['type'],'name':f['file'].name,'path':str(original),'bytes':f['bytes']}
            if f['type']=='image' and f['bytes']>20*1024*1024:
                prepared=directory/'model.jpg'
                with Image.open(original) as picture:
                    sample=1
                    while max(picture.size)//sample>1280:sample*=2
                    picture.convert('RGB').resize((picture.width//sample,picture.height//sample)).save(prepared,quality=85)
                attachment['modelPath']=str(prepared)
                c.update(preparation='Pillow proxy of source-selected sample and JPEG85, NOT Android Bitmap/RGB565',modelFileBytes=prepared.stat().st_size)
            else:c.update(preparation='original' if f['type']=='image' else 'metadata-only')
            (directory/'attachment.json').write_text(json.dumps(attachment))
            case={'name':ref+'-'+f['name'],'requests':[],'tool':None,'issued':False,'drop':False};beg=time.monotonic()
            value,control,reply=request({'op':'chat-send','session':ident,'id':'msg_'+uuid.uuid4().hex,'kind':'message','text':'SYNTHETIC attachment only; do not open files','attachments':[{'id':aid}]});assert value.get('ok'),value
            deadline=time.monotonic()+90
            while time.monotonic()<deadline:
                value,_,_=request({'op':'chat-snapshot'})
                if case['requests'] and not value.get('busy'):break
                time.sleep(.05)
            else:raise TimeoutError(ref+' '+f['name'])
            c.update(outcome='COMPLETED_NATIVE_INFERENCE',requests=list(case['requests']),controlBytes=control,seconds=round(time.monotonic()-beg,3))
            assert all(q['imageCount']==(1 if f['type']=='image' else 0) for q in c['requests'])
            owned_stop(chat)
        print(json.dumps({'ref':ref,'cases':[(c['name'],c['outcome'],[q['bodyBytes'] for q in c.get('requests',[])]) for c in row['cases']]}),flush=True)
    for name in ('small','under20'):
        examples=[next(c for c in r['cases'] if c['name']==name)['requests'][0] for r in limits]
        assert all(v['images']==examples[0]['images'] for v in examples),'Admitted original image changed across adapters'
finally:
    for child in reversed(children):owned_stop(child)
    stop();server.shutdown();server.server_close();persist()
    result={'scope':'Source-asserted Android limits plus actual Node/native HTTP collector. Pillow preparation is approximate; no Android Bitmap/UI execution. No native tool file reads in these inference cases.','nativeSha256':hashlib.sha256(Path(exe).read_bytes()).hexdigest(),'limits':limits,'originalsUnchanged':all(hashlib.sha256(f['file'].read_bytes()).hexdigest()==f['sha256'] for f in files),'allOwnedChildrenExited':all(c.poll() is not None for c in children)}
    (RUN/'limits-result.json').write_text(json.dumps(result,indent=2)+'\n')
    (OUT/'latest-limits-run.txt').write_text(str(RUN)+'\n')
    print(json.dumps({'run':str(RUN),'originalsUnchanged':result['originalsUnchanged'],'allOwnedChildrenExited':result['allOwnedChildrenExited']}),flush=True)
