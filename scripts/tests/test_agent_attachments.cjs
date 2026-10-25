'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {Attachments}=require('../../app/src/main/assets/agent-attachments.cjs');
const {OpenCodeChat}=require('../../app/src/main/assets/opencode-chat.cjs');
const {ClaudeChat}=require('../../app/src/main/assets/claude-chat.cjs');
const {GeminiChat}=require('../../app/src/main/assets/gemini-chat.cjs');
const root=require('./fixture-paths.cjs').fixture('allatt');process.env.CLAUDE_CONFIG_DIR=path.join(root,'claude-config');
const session='11111111-1111-4111-8111-111111111111';
const png=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jWZkAAAAASUVORK5CYII=','base64');
function stage(root,id,type='image'){
 const dir=path.join(root,'chat-attachments',id.repeat(32));fs.mkdirSync(dir,{recursive:true});
 const file=path.join(dir,type==='image'?'original.png':type==='video'?'original.mp4':type==='file'?'original.log':type==='apk'?'original.apk':'original.zip');
 fs.writeFileSync(file,type==='image'?png:type==='file'?'line\n'.repeat(3000):Buffer.from([80,75,0,1]));
 const a={id:id.repeat(32),path:file,name:path.basename(file),bytes:fs.statSync(file).size,type};
 if(type==='image'){a.modelPath=path.join(dir,'model.jpg');fs.writeFileSync(a.modelPath,png);}
 if(type==='video'){a.durationMs=1001;a.frames=[0,1000].map((timeMs,i)=>{const path2=path.join(dir,'frame-'+i+'.jpg');fs.writeFileSync(path2,png);return {path:path2,timeMs};});}
 fs.writeFileSync(path.join(dir,'attachment.json'),JSON.stringify(a));return a;
}
(async()=>{
 for(const kind of ['claude','gemini','opencode']){
  const base=path.join(root,kind);fs.mkdirSync(base);const image=stage(base,'a'),log=stage(base,'b','file'),video=stage(base,'c','video'),apk=stage(base,'d','apk');let serial=0;
  const refs=[image,log,video,apk].map(a=>({id:a.id})),q={op:'chat-send',kind:'message',id:'msg_all',session:kind==='opencode'?'ses_test':session,text:'Review attachments',attachments:refs};
  let chat,received,counts=0;
  if(kind==='claude'){
   chat=new ClaudeChat({socket:path.join(base,'s.sock'),binary:'/not-used',model:'sonnet',mode:'manual',chat:'new',thread:session,language:'en'});chat.ready=true;chat.auth=true;chat.workerKind='sdk';chat.write=m=>{counts++;received=m;};
   assert((await chat.command(q)).ok);assert.equal(received.message.content.filter(b=>b.type==='image').length,3);assert.equal(received.message.content.filter(b=>b.type==='text').length,7);assert(!JSON.stringify(chat.snapshot()).includes(png.toString('base64')));
   const rows=chat.snapshot().rows;assert.equal(rows.filter(r=>r.kind==='attachment').length,4);assert(rows.some(r=>r.kind==='user'&&r.body==='Review attachments'));assert(rows.every(r=>chat.snapshot().knownMessages.includes(r.message)));
  }else if(kind==='gemini'){
   const config={isTrustedFolder:()=>true},api={attachmentRoot:base,session:()=>session,status:()=>({available:true,state:'idle'}),supported:()=>true,permission:()=>null,receipt:()=>null};chat=new GeminiChat(api);
   chat.observe(config,{ready:true,native:false,history:[],pending:[]},async value=>{counts++;received=value;},false,()=>{});
   assert((await chat.command(q)).ok);assert(received.includes('@'+image.modelPath));assert(received.includes('@'+video.frames[1].path));assert(received.includes('1000 ms'));assert(!received.includes('@'+apk.path));assert(received.includes('Sample ends'));
   chat.observe(config,{ready:true,native:false,history:[{id:1,type:'user',text:received}],pending:[]},async()=>{},false,()=>{});const rows=chat.snapshot({}).rows;assert.equal(rows.filter(r=>r.kind==='attachment').length,4);assert.equal(rows.find(r=>r.kind==='user').body,'Review attachments');
  }else{
   chat=new OpenCodeChat(async(method,url,payload)=>{if(method==='POST'){counts++;received=payload;return {};}throw Error('No network in fixture');},'ses_test',path.join(base,'receipts.json'));
   assert((await chat.command(q)).accepted);assert.equal(received.files.length,3);assert(received.files.every(f=>f.uri.startsWith('file:///')));assert(received.text.includes(apk.path));
   const rows=chat.rows([{id:q.id,type:'user',text:received.text}]);assert.equal(rows.filter(r=>r.kind==='attachment').length,4);assert.equal(rows.find(r=>r.kind==='user').body,'Review attachments');assert(rows.every(r=>r.message===q.id));
  }
  await chat.command(q);assert.equal(counts,1);await assert.rejects(()=>chat.command({...q,attachments:[refs[0]]}));
  for(const bad of [[null],[refs[0],refs[0]],Array(5).fill(refs[0]),[{id:'../secret'}]])await assert.rejects(()=>chat.command({...q,id:'msg_bad_'+(++serial),attachments:bad}));
  const shellReply=await chat.command({...q,id:'msg_shell',kind:'shell'}).catch(()=>null);assert(!shellReply||shellReply.ok===false);assert.equal(counts,1);
  // The same staged IDs are not admissible to a different native thread.
  const own=chat.attachments;assert.throws(()=>own.prepare(refs,'other-session','message'));
  const symlink=stage(base,'e');fs.unlinkSync(symlink.path);fs.symlinkSync(image.path,symlink.path);assert.throws(()=>own.prepare([{id:symlink.id}],q.session,'message'));
  const cross=stage(base,'f');const meta=path.join(path.dirname(cross.path),'attachment.json');fs.writeFileSync(meta,JSON.stringify({...cross,path:image.path}));assert.throws(()=>own.prepare([{id:cross.id}],q.session,'message'));
  const out=path.join(base,'build (test).apk');fs.writeFileSync(out,'artifact-fixture');const outputs=own.output('[APK](<'+out+'>) ![image]('+image.path+') [remote](https://invalid/test.apk)');assert.equal(outputs.length,2);assert.equal(outputs[0].type,'apk');assert.equal(outputs[1].type,'image');assert.deepEqual(own.output('[missing](/missing.apk)'),[]);
 }
 const base=path.join(root,'empty');fs.mkdirSync(base);const a=stage(base,'1');const own=new Attachments(base);assert.equal(own.prepare([{id:a.id}],session,'message').length,1);assert(own.prompt('',[own.load(a.id,session)]).includes(a.path));
 // Shared Gemini/Claude/OpenCode admission and artifact cards use the same
 // bounds. Sparse files verify the contract without allocating file contents.
 const limit=5*1024*1024*1024,big=stage(base,'2','binary');
 const resize=(file,n)=>{const fd=fs.openSync(file,'r+');try{fs.ftruncateSync(fd,n);}finally{fs.closeSync(fd);}};
 resize(big.path,limit);const bigLoaded=own.prepare([{id:big.id}],session,'message')[0];assert.equal(bigLoaded.bytes,limit);assert(own.prompt('inspect',[bigLoaded]).includes('5368709120 bytes'));
 resize(big.path,limit+1);assert.throws(()=>own.prepare([{id:big.id}],session,'message'));
 const bigApk=path.join(base,'large.apk'),tooBig=path.join(base,'over.apk');fs.writeFileSync(bigApk,'test');fs.writeFileSync(tooBig,'test');resize(bigApk,limit);resize(tooBig,limit+1);
 const output=own.output('[Large]('+bigApk+') [Over]('+tooBig+')');assert.equal(output.length,1);assert.equal(output[0].bytes,limit);
 const largeImage=stage(base,'3');resize(largeImage.path,100*1024*1024);
 const prepared=own.prepare([{id:largeImage.id}],session,'message')[0];assert.equal(prepared.bytes,100*1024*1024);assert.equal(own.images(prepared)[0].path,largeImage.modelPath);assert(own.claude('image',[prepared]).some(p=>p.type==='image'));
 resize(largeImage.path,100*1024*1024+1);assert.throws(()=>own.prepare([{id:largeImage.id}],session,'message'));
 for(const f of [big.path,bigApk,tooBig,largeImage.path])fs.unlinkSync(f);
 console.log('PASS all-agent attachments: native Claude image blocks, Gemini @ references, OpenCode file URIs, originals/log samples/video timestamps, four cards, dedup, Shell refusal, scope/symlink/cross-file refusal, output cards; no account API');
})().catch(e=>{console.error(e);process.exitCode=1;});
