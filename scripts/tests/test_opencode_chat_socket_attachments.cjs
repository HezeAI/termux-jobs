'use strict';
// Production controller/Unix socket and production nested directories. The HTTP
// peer is synthetic; real image decoding is covered by the native CLI fixture.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),net=require('node:net'),{spawn}=require('node:child_process'),{once}=require('node:events');
if(process.argv[2]==='fixture'){
 const session='ses_fixture',messages=[],root=process.env.FIXTURE_ROOT;let posts=0;
 const server=http.createServer((req,res)=>{
  if(req.headers.authorization!=='Basic '+Buffer.from('opencode:'+process.env.OPENCODE_SERVER_PASSWORD).toString('base64')){res.writeHead(401).end();return;}
  if(req.url==='/api/event'){res.writeHead(200,{'Content-Type':'text/event-stream'});res.write(': connected\n\n');return;}
  let data='';req.on('data',b=>data+=b);req.on('end',()=>{try{
   const body=data?JSON.parse(data):{},route=req.url.split('?')[0],send=value=>{res.setHeader('Content-Type','application/json');res.end(JSON.stringify(value));};
   if(route==='/api/info')return send({version:'2.0.28'});
   if(route==='/api/session/active')return send({data:{}});
   if(route==='/api/model')return send({data:[],padding:'A'.repeat(3*1024*1024)});
   if(route==='/api/session/'+session+'/prompt'){
    const files=(body.files||[]).map(f=>{const p=require('node:url').fileURLToPath(f.uri);assert(p.startsWith(path.join(root,'c','chat-attachments')+'/'));assert(fs.statSync(p).isFile());return {name:f.name,mime:'image/png',data:Buffer.from('fixture-image').toString('base64')+'A'.repeat(3*1024*1024)};});
    posts++;const record={id:body.id,type:'user',payload:{text:body.text,files}};messages.push(record);fs.writeFileSync(path.join(root,'posts.json'),JSON.stringify({posts,nativeFiles:body.files||[],text:body.text}));return send({data:record});
   }
   if(route.endsWith('/inbox'))return send({data:messages});
   if(route.endsWith('/permission')||route.endsWith('/form')||route.endsWith('/message'))return send({data:[]});
   if(route==='/api/session/'+session)return send({data:{id:session,agent:'build',model:{providerID:'fixture',id:'image-model'},permissions:[{action:'*',resource:'*',effect:'ask'}]}});
   if(req.method==='POST'||req.method==='PATCH')return send({});
   res.writeHead(404).end();
  }catch{res.writeHead(400).end('{}');}});
 });server.listen(0,'127.0.0.1',()=>console.log('http://127.0.0.1:'+server.address().port));
}else{
 const root=require('./fixture-paths.cjs').fixture('ocipc'),home=path.join(root,'h'),controls=path.join(root,'c'),connection=path.join(controls,'connections','one'),socket=path.join(controls,'run.sock'),project=path.join(root,'project');
 for(const dir of [home,controls,connection,project])fs.mkdirSync(dir,{recursive:true});
 const exe=path.join(home,'.local/share/termux-jobs/agents/bin/opencode');fs.mkdirSync(path.dirname(exe),{recursive:true});const quote=v=>"'"+v.replace(/'/g,"'\\''")+"'";
 fs.writeFileSync(exe,'#!/bin/sh\nexec '+quote(process.execPath)+' '+quote(__filename)+' fixture\n',{mode:0o700});
 const owner=spawn(process.execPath,[process.env.FIXTURE_CONTROLLER||path.resolve('app/src/main/assets/ai-agents-control.js'),'run-chat',connection,socket,'ses_fixture','','build','new','','ask','unrestricted'],{cwd:project,env:{PATH:process.env.PATH,HOME:home,TMPDIR:path.resolve('.tools/tmp'),FIXTURE_ROOT:root},stdio:['ignore','pipe','pipe']});
 let stderr='';owner.stderr.on('data',b=>stderr+=b);
 const request=q=>new Promise((resolve,reject)=>{let data='';const c=net.createConnection(socket,()=>c.end(JSON.stringify(q)));c.setTimeout(10000,()=>c.destroy(Error('Fixture socket timeout')));c.on('data',b=>data+=b);c.on('error',reject);c.on('end',()=>{try{resolve(JSON.parse(data));}catch(e){reject(e);}});});
 function stage(letter,type,ext){const id=letter.repeat(32),dir=path.join(controls,'chat-attachments',id);fs.mkdirSync(dir,{recursive:true});const file=path.join(dir,'original.'+ext);fs.writeFileSync(file,type==='file'?'log-fixture':fs.readFileSync(path.resolve('app/src/main/res/mipmap-mdpi/ic_launcher.png')));const a={id,type,name:path.basename(file),path:file,bytes:fs.statSync(file).size};if(type==='video')a.frames=[{path:file,timeMs:0}];fs.writeFileSync(path.join(dir,'attachment.json'),JSON.stringify(a));return {id};}
 (async()=>{try{
  await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(Error('Controller startup timed out')),15000);owner.once('error',e=>{clearTimeout(timer);reject(e);});owner.once('exit',()=>{clearTimeout(timer);reject(Error('Controller exited before readiness'));});owner.stdout.on('data',b=>{if(b.toString().includes('OpenCode chat ready')){clearTimeout(timer);resolve();}});});
  const image=stage('a','image','png'),log=stage('b','file','log'),video=stage('c','video','png'),binary=stage('d','binary','zip');
  const q={op:'chat-send',kind:'message',id:'msg_image_only',session:'ses_fixture',text:'',attachments:[image]};
  const reply=await request(q);
  if(process.env.FIXTURE_EXPECT_REJECT==='1'){assert.equal(reply.ok,false);assert(!fs.existsSync(path.join(root,'posts.json')));console.log('CONFIRMED old production controller rejects the attachment before the native HTTP prompt (nested root mismatch)');return;}
  assert(reply.accepted,'file-only send must cross production Unix controller');
  let state=await request({op:'chat-snapshot'});assert(state.ok);assert.equal(state.rows.filter(r=>r.kind==='attachment').length,1);assert(!JSON.stringify(state).includes('fixture-image'));assert(JSON.stringify(state).length<60000);
  await request(q);assert.equal(JSON.parse(fs.readFileSync(path.join(root,'posts.json'))).posts,1,'duplicate must not re-admit');
  assert((await request({...q,id:'msg_all',text:'Review files',attachments:[image,log,video,binary]})).accepted);
  const recorded=JSON.parse(fs.readFileSync(path.join(root,'posts.json')));assert.equal(recorded.posts,2);assert.equal(recorded.nativeFiles.length,2);assert(recorded.text.includes('original.zip'));assert(recorded.text.includes('log-fixture'));
  state=await request({op:'chat-snapshot'});assert(state.ok);assert.equal(state.rows.filter(r=>r.kind==='attachment').length,5);assert(state.rows.some(r=>r.kind==='user'&&r.body==='Review files'));assert(JSON.stringify(state).length<60000);
  assert.equal((await request({...q,id:'msg_foreign',session:'ses_other'})).ok,false);assert.equal((await request({...q,id:'msg_shell',kind:'shell'})).ok,false);assert.equal((await request({...q,id:'msg_missing',attachments:[{id:'e'.repeat(32)}]})).ok,false);
  assert((await request({op:'chat-send',kind:'message',id:'msg_plain',session:'ses_fixture',text:'normal message'})).accepted);assert.equal(JSON.parse(fs.readFileSync(path.join(root,'posts.json'))).posts,3);
  assert.equal((await request({op:'models'})).ok,false,'the non-media response limit must remain unchanged');
  const ended=once(owner,'exit');assert((await request({op:'stop'})).ok);await ended;assert(!fs.existsSync(socket));assert.equal(stderr,'');
  console.log('PASS production OpenCode Unix controller: nested connection/receipt roots, file-only and mixed input, native file URIs, >2MiB media replies/inbox, bounded cards, dedup, foreign/missing/Shell refusal, ordinary-send recovery and owned stop; synthetic HTTP peer, no account/model API');
 }finally{if(owner.exitCode===null&&owner.signalCode===null){const ended=once(owner,'exit');owner.kill('SIGTERM');await ended;}}})().catch(e=>{console.error(e);process.exitCode=1;});
}
