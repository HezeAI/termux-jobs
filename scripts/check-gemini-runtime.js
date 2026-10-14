'use strict';
// Only private test processes and files. No real assistant sessions or authentication.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {spawn}=require('node:child_process');
const {once}=require('node:events');
const bridge=path.resolve(__dirname,'../app/src/main/assets/gemini-control.js');
const {argsFor,stopProxy}=require(bridge);
const root=fs.mkdtempSync(path.resolve(__dirname,'../.verification/gemini-v1.0.15/runtime-'));
const id='12345678-1234-4234-8234-123456789abc';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(fn){for(let i=0;i<100;i++){if(fn())return;await sleep(50);}throw new Error('Test timeout');}
function alive(pid){try{process.kill(pid,0);return true;}catch{return false;}}
async function testOwnedStop(ignoreTerm){
 const socket=path.join(root,(ignoreTerm?'22345678':'12345678')+'-1234-4234-8234-123456789abc.sock');
 const pidfile=socket+'.pid';
 const child=spawn(process.execPath,[bridge,'run',socket,'flash','default','new',id],{stdio:'ignore',env:{...process.env,JOBS_GEMINI_BINARY:stub,GEMINI_TEST_PID:pidfile,GEMINI_TEST_IGNORE:ignoreTerm?'1':'0'}});
 try{
  await until(()=>fs.existsSync(pidfile)&&fs.existsSync(socket));const pid=Number(fs.readFileSync(pidfile,'utf8'));assert(alive(pid));
  assert.equal(fs.statSync(socket).mode&0o777,0o600);
  const exit=once(child,'exit');await stopProxy(socket);await exit;
  await until(()=>!alive(pid));assert(!fs.existsSync(socket));
 }finally{if(child.exitCode===null&&child.signalCode===null)child.kill('SIGTERM');}
}
const stub=path.join(root,'stub');
fs.writeFileSync(stub,'#!'+process.execPath+'\n'+"const fs=require('fs');fs.writeFileSync(process.env.GEMINI_TEST_PID,String(process.pid));if(process.env.GEMINI_TEST_IGNORE==='1')process.on('SIGTERM',()=>{});setInterval(()=>{},1000);\n",{mode:0o700});
(async()=>{
 assert.deepEqual(argsFor('','default','new',id),['--approval-mode','default','--session-id',id]);
 assert.deepEqual(argsFor('pro','plan','choose',id),['--approval-mode','plan','--model','pro','--prompt-interactive','/resume']);
 assert.deepEqual(argsFor('auto','auto_edit','id',id),['--approval-mode','auto_edit','--model','auto','--resume',id]);
 for(const values of [['$(bad)','default','new',id],['','bypassPermissions','new',id],['','default','last',id],['','default','id','bad']])assert.throws(()=>argsFor(...values));
 // Official restart code must preserve control and carry only the native auth type.
 const restartStub=path.join(root,'restart.js'),restartState=path.join(root,'restart-state');
 fs.writeFileSync(restartStub,`const fs=require('fs');const state=process.env.GEMINI_TEST_RESTART;const count=fs.existsSync(state)?Number(fs.readFileSync(state,'utf8')):0;fs.writeFileSync(state,String(count+1));if(!count){process.send({type:'auth-selected-type',authType:'oauth-personal'},()=>process.exit(199));}else{if(process.env.GEMINI_CLI_AUTH_OVERRIDE!=='oauth-personal')process.exit(2);fs.writeFileSync(process.env.GEMINI_TEST_PID,String(process.pid));setInterval(()=>{},1000);}`);
 const restartSocket=path.join(root,'52345678-1234-4234-8234-123456789abc.sock'),restartPid=restartSocket+'.pid';
 const owner=spawn(process.execPath,[bridge,'run',restartSocket,'','default','new',id],{stdio:'ignore',env:{...process.env,JOBS_GEMINI_BINARY:restartStub,GEMINI_TEST_RESTART:restartState,GEMINI_TEST_PID:restartPid}});
 try{await until(()=>fs.existsSync(restartPid));assert.equal(fs.readFileSync(restartState,'utf8'),'2');const stopped=once(owner,'exit');await stopProxy(restartSocket);await stopped;assert(!fs.existsSync(restartSocket));}
 finally{if(owner.exitCode===null&&owner.signalCode===null)owner.kill('SIGTERM');}
 // Unrelated process must survive both normal and forced owned-child cleanup.
 const orphanSocket=path.join(root,'32345678-1234-4234-8234-123456789abc.sock');
 const orphanPid=orphanSocket+'.pid';
 const parentFile=path.join(root,'parent.js');
 fs.writeFileSync(parentFile,`require('child_process').spawn(${JSON.stringify(process.execPath)},${JSON.stringify([bridge,'run',orphanSocket,'','default','new',id])},{stdio:'ignore'});setInterval(()=>{},1000);`);
 const parent=spawn(process.execPath,[parentFile],{stdio:'ignore',env:{...process.env,JOBS_GEMINI_BINARY:stub,GEMINI_TEST_PID:orphanPid,GEMINI_TEST_IGNORE:'0'}});
 await until(()=>fs.existsSync(orphanPid)&&fs.existsSync(orphanSocket));
 const orphanChild=Number(fs.readFileSync(orphanPid,'utf8'));
 parent.kill('SIGTERM');await once(parent,'exit');
 await until(()=>!alive(orphanChild));await until(()=>!fs.existsSync(orphanSocket));
 const missingSocket=path.join(root,'42345678-1234-4234-8234-123456789abc.sock');
 const missing=spawn(process.execPath,[bridge,'run',missingSocket,'','default','new',id],{stdio:'ignore',env:{...process.env,JOBS_GEMINI_BINARY:path.join(root,'not-present')}});
 const [missingCode]=await once(missing,'exit');assert.equal(missingCode,1);assert(!fs.existsSync(missingSocket));
 const unrelated=spawn(process.execPath,['-e','setInterval(()=>{},1000)'],{stdio:'ignore'});
 try{await testOwnedStop(false);assert(alive(unrelated.pid));await testOwnedStop(true);assert(alive(unrelated.pid));}
 finally{unrelated.kill('SIGTERM');await once(unrelated,'exit');}
 console.log('PASS: launch modes, input rejection, owned graceful/forced close, private socket, restart/auth handoff, parent-loss cleanup, spawn failure and unrelated-process survival');
})().catch(e=>{console.error(e);process.exitCode=1;});
