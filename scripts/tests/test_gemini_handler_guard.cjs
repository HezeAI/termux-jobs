'use strict';
// Real controller/IPC with deliberately absent UI hooks. Never executes a model request.
const fs=require('node:fs'),path=require('node:path'),net=require('node:net'),crypto=require('node:crypto'),assert=require('node:assert/strict'),{spawn}=require('node:child_process');
const root=require('./fixture-paths.cjs').fixture('ghand');
const request=(socket,q)=>new Promise((resolve,reject)=>{const c=net.createConnection(socket),data=[];c.setTimeout(3000,()=>c.destroy(Error('timeout')));c.on('connect',()=>c.write(JSON.stringify(q)+'\n'));c.on('data',d=>data.push(d));c.on('error',reject);c.on('end',()=>{try{resolve(JSON.parse(Buffer.concat(data)));}catch(e){reject(e);}});});
(async()=>{
 for(const unsupported of [false,true])for(const alias of [false,true]){
  const run=crypto.randomUUID(),socket=path.join(root,run+'.sock'),real=path.join(root,'fixture-'+run+'.cjs'),link=path.join(root,'alias-'+run+'.cjs'),touch=path.join(root,run+'.submitted');
  fs.writeFileSync(real,`const fs=require('fs'),bridge=globalThis.__jobsGeminiLive;if(process.env.JOBS_GEMINI_BINARY!==fs.realpathSync(__filename))process.exit(33);bridge.attach({setModel(){},model:'gemini-2.5-flash',getModel:()=> 'gemini-2.5-flash',getSessionId:()=>${JSON.stringify(run)},getApprovalMode:()=> 'default',getProjectRoot:()=>${JSON.stringify(root)},getContentGenerator:()=>({}),getContentGeneratorConfig:()=>({authType:'gemini-api-key'})});${unsupported?'bridge.unsupported();':''}setInterval(()=>{},1000);`);
  fs.symlinkSync(real,link);
  const child=spawn(process.execPath,[path.resolve('app/src/main/assets/gemini-control.js'),'run',socket,'gemini-2.5-flash','default','new',run,'auto'],{cwd:root,env:{PATH:process.env.PATH,HOME:root,JOBS_GEMINI_BINARY:alias?link:real,JOBS_GEMINI_AUTH_DIR:path.join(root,'auth'),JOBS_GEMINI_ATTACHMENT_ROOT:root},stdio:'ignore'});
  try{
   let snap;for(let i=0;i<100;i++){await new Promise(r=>setTimeout(r,25));try{snap=await request(socket,{op:'chat-snapshot'});if(snap.session===run||snap.nativeChat===false)break;}catch{}}
   assert(snap,'controller never replied');assert.equal(snap.connected===true,false);if(unsupported)assert.equal(snap.nativeChat,false);else assert.equal(snap.nativeUiReady,false);
   const result=await request(socket,{op:'chat-send',session:run,id:'msg_no_handlers',kind:'message',text:'SYNTHETIC do not submit'});assert.equal(result.ok,false);assert.equal(result.error,'NOT_READY');assert.notEqual(result.accepted,true);assert(!fs.existsSync(touch));
   await request(socket,{op:'stop'});
   if(child.exitCode===null&&child.signalCode===null)await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(Error('owned controller did not exit')),8000);child.once('exit',()=>{clearTimeout(timer);resolve();});});assert(!fs.existsSync(socket));
  }finally{if(child.exitCode===null&&child.signalCode===null){child.kill('SIGTERM');await new Promise(r=>child.once('exit',r));}}
 }
 console.log('PASS real controller normal/symlink path canonical environment; missing UI handlers and unsupported layout explicitly refuse chat-send with NOT_READY; no accepted request; owned shutdown');
})().catch(e=>{console.error(e);process.exitCode=1;});
