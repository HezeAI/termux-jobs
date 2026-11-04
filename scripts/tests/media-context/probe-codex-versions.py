"""Execute stable-tag adapters in private fixtures; never install old APKs."""
from pathlib import Path
base=Path(__file__).with_name('probe-codex-chain.py').read_text();separator='try:\n    start();thread=';assert base.count(separator)==1
exec(compile(base.split(separator)[0],str(Path(__file__).with_name('probe-codex-chain.py')),'exec'),globals())
import re
refs=('v2.0.5','v2.0.6') if os.environ.get('SYNTHETIC_RELEASE_NARROW')=='1' else ('v2.0.2','v2.0.6','v2.0.8','v2.1.0','v2.1.1','v2.1.3')
version_rows=[]
def tag_asset(ref,name):
    assert ref in refs and re.fullmatch(r'[a-zA-Z0-9_.-]+',name)
    return subprocess.check_output(['git','-C',str(repo),'show',ref+':app/src/main/assets/'+name],env={'PATH':os.environ['PATH'],'HOME':str(HOME)})
def install_fixture_assets(ref):
    pending=['codex-chat.cjs'];copied={}
    while pending:
        name=pending.pop()
        if name in copied:continue
        source='codex-control.js' if name.startswith('codex-wire-') else name
        data=tag_asset(ref,source);(private/name).write_bytes(data);copied[name]={'source':source,'sha256':hashlib.sha256(data).hexdigest()}
        for required in re.findall(r"require\(['\"]\./([^'\"]+)['\"]\)",data.decode()):
            assert '/' not in required and '..' not in required
            pending.append(required if required.endswith(('.js','.cjs')) else required+'.cjs')
    return copied

def inspect_pages(ident):
    response,_,_=request({'op':'chat-snapshot'});seen=set();paths=set();pages=0
    while True:
        assert response.get('connected')
        for row in response.get('rows',[]):
            if row.get('attachment',{}).get('path'):paths.add(row['attachment']['path'])
        cursor=response.get('older');pages+=1
        if not cursor:break
        assert cursor not in seen and pages<30,'Bad pagination cursor'
        seen.add(cursor);response,_,_=request({'op':'chat-snapshot','session':ident,'cursor':cursor})
    assert all(str(PROJECT/f'png-600-{n}.png') in paths for n in range(20)), 'A historical attachment disappeared'
    return {'pages':pages,'attachmentPaths':len(paths),'all20OriginalsVisible':True}
try:
    start();thread=rpc('thread/start',{'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access'})['thread']['id']
    turn('short-control',[text('SYNTHETIC short control')])
    for i in range(0,20,4):turn('png-batch-'+str(i//4+1),[text('SYNTHETIC PNG batch')]+[image(f'png-600-{n}.png') for n in range(i,i+4)])
    forks={ref:rpc('thread/fork',{'threadId':thread,'cwd':str(PROJECT),'approvalPolicy':'never','sandbox':'danger-full-access','excludeTurns':True})['thread']['id'] for ref in refs}
    histories={ref:rpc('thread/turns/list',{'threadId':ident,'limit':100,'itemsView':'full'})['data'] for ref,ident in forks.items()}
    assert all(value==histories[refs[0]] for value in histories.values())
    history_hash=hashlib.sha256(json.dumps(histories[refs[0]],sort_keys=True).encode()).hexdigest();stop()
    launcher=node_child([private/'codex-launch.cjs',endpoint,'danger-full-access','never','1','en'],'launch')
    end=time.monotonic()+30
    while not endpoint.exists():
        assert time.monotonic()<end and launcher.poll() is None
        time.sleep(.05)
    for ref in refs:
        sources=install_fixture_assets(ref);ident=forks[ref];chat,snapshot=start_chat(ident)
        row={'ref':ref,'sources':sources,'identicalInitialHistorySha256':history_hash,'cases':[]}
        version_rows.append(row)
        row['beforePages']=inspect_pages(ident)
        row['cases'].append(chat_turn(ident,ref+'-cold-reopen','SYNTHETIC continue without new images'))
        ids=[]
        for number in range(2):
            aid=uuid.uuid4().hex;directory=private/'chat-attachments'/aid;directory.mkdir(parents=True)
            original=directory/'original.png';shutil.copy2(PROJECT/f'png-600-{number}.png',original)
            attachment={'id':aid,'type':'image','name':'synthetic-'+str(number)+'.png','path':str(original),'bytes':original.stat().st_size}
            if number:
                from PIL import Image
                prepared=directory/'model.jpg'
                with Image.open(original) as picture:picture.convert('RGB').save(prepared,quality=85)
                attachment['modelPath']=str(prepared)
            (directory/'attachment.json').write_text(json.dumps(attachment));ids.append({'id':aid})
        # Same original PNG and same real JPEG preparation across all adapters.
        global_case={'name':ref+'-attach-original-and-modelPath','requests':[],'tool':None,'issued':False,'drop':False}
        case=global_case;beg=time.monotonic()
        value,control_size,reply_size=request({'op':'chat-send','session':ident,'id':'msg_'+uuid.uuid4().hex,'kind':'message','text':'SYNTHETIC two attachments','attachments':ids});assert value.get('ok'),value
        deadline=time.monotonic()+90
        while time.monotonic()<deadline:
            value,_,_=request({'op':'chat-snapshot'})
            if case['requests'] and not value.get('busy'):break
            time.sleep(.05)
        else:raise TimeoutError(ref+' attachment turn')
        assert case['requests'][-1]['imageCount']==22
        row['cases'].append({'case':case['name'],'requests':list(case['requests']),'completed':True,'controlRequestBytes':control_size,'elapsedSeconds':round(time.monotonic()-beg,3)})
        owned_stop(chat);chat,snapshot=start_chat(ident)
        row['afterPages']=inspect_pages(ident)
        row['cases'].append(chat_turn(ident,ref+'-reopen-after-attachments','SYNTHETIC continue without new images'))
        row['cases'].append(chat_turn(ident,ref+'-response-drop','SYNTHETIC continue without new images',drop=True));owned_stop(chat)
        print(json.dumps({'ref':ref,'completed':True,'requestBytes':[[r['bodyBytes'] for r in c['requests']] for c in row['cases']]}),flush=True)
    baseline=[[i['sha256'] for i in r['images']] for r in version_rows[0]['cases'][0]['requests']]
    assert all([[i['sha256'] for i in r['images']] for r in row['cases'][0]['requests']]==baseline for row in version_rows), 'Cold baseline differs'
    for row in version_rows:
        image_hashes=[i['sha256'] for i in row['cases'][1]['requests'][0]['images']]
        assert image_hashes[-2]==baseline[0][0]
        row['modelPathUsed']=image_hashes[-1]!=baseline[0][1]
        assert row['modelPathUsed']==(row['ref'] not in ('v2.0.2','v2.0.5')), 'Unexpected modelPath boundary'
    for index in range(1,4):
        expected=[[i['sha256'] for i in r['images']] for r in version_rows[1]['cases'][index]['requests']]
        assert all([[i['sha256'] for i in r['images']] for r in row['cases'][index]['requests']]==expected for row in version_rows[1:]), 'Later stable adapters differ'
finally:
    for child in reversed(children):owned_stop(child)
    stop();server.shutdown();server.server_close();persist()
    (RUN/'versions-result.json').write_text(json.dumps({'scope':'Actual stable-tag JS adapters, pinned Codex 0.166.0 and artificial HTTP provider; not Android Bitmap/UI or external route','binarySha256':hashlib.sha256(Path(exe).read_bytes()).hexdigest(),'versions':version_rows,'allOwnedChildrenExited':all(c.poll() is not None for c in children)},indent=2)+'\n')
    (OUT/'latest-versions-run.txt').write_text(str(RUN)+'\n')
