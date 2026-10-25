'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'../..'),dir=require('./fixture-paths.cjs').fixture('cxatt');
for(const [from,to]of [['codex-chat.cjs','codex-chat.cjs'],['codex-control.js','codex-wire-v0.2.22.js'],['agent-receipts.cjs','agent-receipts.cjs']])fs.copyFileSync(path.join(root,'app/src/main/assets',from),path.join(dir,to));
const {CodexChat}=require(path.join(dir,'codex-chat.cjs'));const calls=[];
const rpc={send(){},close(){},async call(method,params){calls.push({method,params});return {turn:{id:'turn'}};}};
const c=new CodexChat(rpc,{endpoint:path.join(dir,'s'),access:'full',approval:'never',cwd:dir,language:'en'});c.ready=true;c.thread='fixture-thread';
const png=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jWZkAAAAASUVORK5CYII=','base64');
function stage(id,type='image'){
 const base=path.join(dir,'chat-attachments',id);fs.mkdirSync(base,{recursive:true});const file=path.join(base,type==='image'?'original.png':type==='video'?'original.mp4':'original.log');fs.writeFileSync(file,type==='image'?png:type==='file'?'log-line\n'.repeat(3000):'video-fixture');
 const a={id,type,name:type==='file'?'device report.log':type==='video'?'screen.mp4':'test.png',path:file,bytes:fs.statSync(file).size};
 if(type==='video'){a.durationMs=1200;a.frames=[0,1199].map((timeMs,i)=>{const p=path.join(base,'frame-'+i+'.jpg');fs.writeFileSync(p,png);return {path:p,timeMs};});}
 fs.writeFileSync(path.join(base,'attachment.json'),JSON.stringify(a));return a;
}
let serial=0;function request(attachments,text=''){c.busy=false;c.turn='';return {op:'chat-send',kind:'message',text,id:'msg_'+(++serial).toString(16).padStart(32,'0'),session:c.thread,attachments:attachments.map(a=>({id:a.id}))};}
function event(item){c.event({method:'item/completed',params:{threadId:c.thread,turnId:'history',item}});}
(async()=>{
 const image=stage('a'.repeat(32)),log=stage('b'.repeat(32),'file'),video=stage('c'.repeat(32),'video');
 assert.equal((await c.snapshot()).attachmentsSupported,true);
 const q=request([image]);assert((await c.send(q)).ok);assert(calls.at(-1).params.input.some(i=>i.type==='localImage'&&i.path===image.path));assert.equal(calls.at(-1).method,'turn/start');
 const sent=calls.length;assert((await c.send(q)).ok);assert.equal(calls.length,sent);assert(!(await c.send({...q,attachments:[{id:log.id}]})).ok);
 assert((await c.send(request([log],'Read this report'))).ok);const inp=calls.at(-1).params.input;assert(inp.some(i=>i.text==='Read this report'));assert(inp.some(i=>i.text?.includes('Read the attached file in parts')));assert(!inp.some(i=>i.type==='localImage'));
 assert((await c.send(request([video],'Check flicker'))).ok);const vin=calls.at(-1).params.input;assert.equal(vin.filter(i=>i.type==='localImage').length,2);assert(vin.some(i=>i.text?.includes('Audio is not included')));assert(vin.some(i=>i.text==='Video frame at 1199 ms'));
 event({id:'video-message',type:'userMessage',content:vin});let rows=(await c.snapshot()).rows;assert.equal(rows.filter(r=>r.kind==='attachment').length,1);assert.equal(rows.find(r=>r.kind==='attachment').attachment.type,'video');assert.equal(rows.find(r=>r.kind==='user').body,'Check flicker');
 event({id:'image-message',type:'userMessage',content:[{type:'localImage',path:image.path}]});assert((await c.snapshot()).rows.some(r=>r.kind==='attachment'&&r.attachment.path===image.path));
 event({id:'generated',type:'imageGeneration',status:'completed',result:png.toString('base64')});let snapshot=await c.snapshot();assert(!JSON.stringify(snapshot).includes(png.toString('base64')));let generated=snapshot.rows.find(r=>r.attachment?.name==='generated.png');assert(generated);assert.deepEqual(fs.readFileSync(generated.attachment.path),png);
 event({id:'linked',type:'agentMessage',text:'Here is ![Preview](/tmp/test.png)'});assert((await c.snapshot()).rows.some(r=>r.attachment?.path==='/tmp/test.png'));
 event({id:'tool-generated',type:'functionCallOutput',name:'exec',output:[{type:'input_text',text:'Generated preview'},{type:'input_image',image_url:'data:image/png;base64,'+png.toString('base64')}]});snapshot=await c.snapshot();assert(!JSON.stringify(snapshot).includes(png.toString('base64')));assert(snapshot.rows.filter(r=>r.kind==='attachment').length>=4);
 const artifact=path.join(dir,'build (local).apk');fs.writeFileSync(artifact,'fake-fixture-apk');
 event({id:'apk-link',type:'agentMessage',text:'[APK](<'+artifact+'>)'});snapshot=await c.snapshot();let apk=snapshot.rows.find(r=>r.attachment?.type==='apk');assert(apk);assert.equal(apk.attachment.bytes,16);assert.equal(apk.attachment.path,artifact);assert(!JSON.stringify(apk).includes('fake-fixture'));
 event({id:'remote-apk',type:'agentMessage',text:'[remote](https://example.invalid/test.apk) [missing](/missing.apk) plain /tmp/quoted.apk'});
 const symlink=path.join(dir,'linked.apk');fs.symlinkSync(artifact,symlink);event({id:'symlink-apk',type:'agentMessage',text:'[link]('+symlink+')'});assert.equal((await c.snapshot()).rows.filter(r=>r.attachment?.type==='apk').length,1);
 assert.deepEqual((await c.snapshot()).rows.find(r=>r.attachment?.type==='apk'),apk);
 const before=calls.length;
 for(const refs of [[{id:'../outside'}],Array(5).fill({id:image.id}),[{id:image.id},{id:image.id}],[null]]){assert(!(await c.send({...request([]),attachments:refs})).ok);}
 assert(!(await c.send({...request([image],'do not execute'),kind:'shell'})).ok);assert.equal(calls.length,before);
 const bad=stage('d'.repeat(32));const meta=path.join(path.dirname(bad.path),'attachment.json');fs.writeFileSync(meta,JSON.stringify({...bad,path:path.join(dir,'outside.png')}));fs.writeFileSync(path.join(dir,'outside.png'),png);assert(!(await c.send(request([bad]))).ok);
 const link=stage('e'.repeat(32));fs.unlinkSync(link.path);fs.symlinkSync(path.join(dir,'outside.png'),link.path);assert(!(await c.send(request([link]))).ok);
 const foreign=stage('f'.repeat(32));fs.writeFileSync(path.join(path.dirname(foreign.path),'attachment.json'),JSON.stringify({...foreign,threadId:'other-thread'}));assert(!(await c.send(request([foreign]))).ok);
 const bin=stage('1'.repeat(32),'file');fs.writeFileSync(bin.path,Buffer.from([65,0,66]));assert(!(await c.send(request([bin]))).ok);
 assert.equal(calls.length,before);
 // Sparse originals exercise >32-bit byte counts without reading GB into RAM.
 const limit=5*1024*1024*1024,large=stage('2'.repeat(32),'binary');
 const resize=(file,n)=>{const fd=fs.openSync(file,'r+');try{fs.ftruncateSync(fd,n);}finally{fs.closeSync(fd);}};
 resize(large.path,limit);fs.writeFileSync(path.join(path.dirname(large.path),'attachment.json'),JSON.stringify({...large,bytes:limit}));
 assert((await c.send(request([large],'Inspect by path'))).ok);assert(calls.at(-1).params.input.some(i=>i.text?.includes('5368709120 bytes')));assert(!calls.at(-1).params.input.some(i=>i.type==='localImage'));
 resize(large.path,limit+1);assert(!(await c.send(request([large]))).ok);
 const hugeApk=path.join(dir,'large-artifact.apk'),excessApk=path.join(dir,'oversize-artifact.apk');
 fs.writeFileSync(hugeApk,'fixture');resize(hugeApk,limit);fs.writeFileSync(excessApk,'fixture');resize(excessApk,limit+1);
 event({id:'large-apk',type:'agentMessage',text:'[Large APK]('+hugeApk+') [Too big]('+excessApk+')'});
 snapshot=await c.snapshot();assert.equal(snapshot.rows.find(r=>r.attachment?.path===hugeApk).attachment.bytes,limit);assert(!snapshot.rows.some(r=>r.attachment?.path===excessApk));
 const largeImage=stage('3'.repeat(32),'image'),modelPath=path.join(path.dirname(largeImage.path),'model.jpg');fs.writeFileSync(modelPath,png);resize(largeImage.path,100*1024*1024);
 fs.writeFileSync(path.join(path.dirname(largeImage.path),'attachment.json'),JSON.stringify({...largeImage,modelPath,bytes:100*1024*1024}));
 assert((await c.send(request([largeImage],'Large image'))).ok);const largeInput=calls.at(-1).params.input;assert(largeInput.some(i=>i.type==='localImage'&&i.path===modelPath));assert(!largeInput.some(i=>i.type==='localImage'&&i.path===largeImage.path));
 event({id:'large-image-history',type:'userMessage',content:largeInput});snapshot=await c.snapshot();assert.equal(snapshot.rows.filter(r=>r.attachment?.path===largeImage.path).length,1);assert(!snapshot.rows.some(r=>r.attachment?.path===modelPath));
 resize(largeImage.path,100*1024*1024+1);assert(!(await c.send(request([largeImage]))).ok);
 for(const f of [large.path,hugeApk,excessApk,largeImage.path])fs.unlinkSync(f);
 await c.close();console.log('PASS attachment adapter: image-only native input, bounded log sample/full path, timestamped video frames, history cards, generated PNG without base64 snapshot, scoped manifests, dedup, traversal/symlink/foreign-thread/binary/shell refusal');
})().catch(e=>{console.error(e);process.exitCode=1;});
