'use strict';
// Only private test processes and files. No real assistant sessions or authentication.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {spawn}=require('node:child_process');
const {once}=require('node:events');
const bridge=path.resolve(__dirname,'../app/src/main/assets/claude-control.js');
const {argsFor,stopProxy}=require(bridge);
const root=fs.mkdtempSync(path.resolve(__dirname,'../.verification/claude-v1.0.14/runtime-'));
const id='12345678-1234-4234-8234-123456789abc';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(fn){for(let i=0;i<100;i++){if(fn())return;await sleep(50);}throw new Error('Test timeout');}
function alive(pid){try{process.kill(pid,0);return true;}catch{return false;}}
async function testOwnedStop(ignoreTerm){
 const socket=path.join(root,(ignoreTerm?'22345678':'12345678')+'-1234-4234-8234-123456789abc.sock');
 const pidfile=socket+'.pid';
 const child=spawn(process.execPath,[bridge,'run',socket,'sonnet','manual','new',id],{stdio:'ignore',env:{...process.env,JOBS_CLAUDE_BINARY:stub,CLAUDE_TEST_PID:pidfile,CLAUDE_TEST_IGNORE:ignoreTerm?'1':'0'}});
 try{
  await until(()=>fs.existsSync(pidfile)&&fs.existsSync(socket));const pid=Number(fs.readFileSync(pidfile,'utf8'));assert(alive(pid));
  assert.equal(fs.statSync(socket).mode&0o777,0o600);
  const exit=once(child,'exit');await stopProxy(socket);await exit;
  await until(()=>!alive(pid));assert(!fs.existsSync(socket));
 }finally{if(child.exitCode===null&&child.signalCode===null)child.kill('SIGTERM');}
}
const stub=path.join(root,'stub');
fs.writeFileSync(stub,'#!'+process.execPath+'\n'+"const fs=require('fs');fs.writeFileSync(process.env.CLAUDE_TEST_PID,String(process.pid));if(process.env.CLAUDE_TEST_IGNORE==='1')process.on('SIGTERM',()=>{});setInterval(()=>{},1000);\n",{mode:0o700});
(async()=>{
 assert.deepEqual(argsFor('','manual','new',id),['--permission-mode','manual','--session-id',id]);
 assert.deepEqual(argsFor('opus','plan','choose',id),['--permission-mode','plan','--model','opus','--resume']);
 assert.deepEqual(argsFor('haiku','acceptEdits','id',id),['--permission-mode','acceptEdits','--model','haiku','--resume',id]);
 for(const values of [['$(bad)','manual','new',id],['','bypassPermissions','new',id],['','manual','last',id],['','manual','id','bad']])assert.throws(()=>argsFor(...values));
 // Unrelated process must survive both normal and forced owned-child cleanup.
 const orphanSocket=path.join(root,'32345678-1234-4234-8234-123456789abc.sock');
 const orphanPid=orphanSocket+'.pid';
 const parentFile=path.join(root,'parent.js');
 fs.writeFileSync(parentFile,`require('child_process').spawn(${JSON.stringify(process.execPath)},${JSON.stringify([bridge,'run',orphanSocket,'','manual','new',id])},{stdio:'ignore'});setInterval(()=>{},1000);`);
 const parent=spawn(process.execPath,[parentFile],{stdio:'ignore',env:{...process.env,JOBS_CLAUDE_BINARY:stub,CLAUDE_TEST_PID:orphanPid,CLAUDE_TEST_IGNORE:'0'}});
 await until(()=>fs.existsSync(orphanPid)&&fs.existsSync(orphanSocket));
 const orphanChild=Number(fs.readFileSync(orphanPid,'utf8'));
 parent.kill('SIGTERM');await once(parent,'exit');
 await until(()=>!alive(orphanChild));await until(()=>!fs.existsSync(orphanSocket));
 const missingSocket=path.join(root,'42345678-1234-4234-8234-123456789abc.sock');
 const missing=spawn(process.execPath,[bridge,'run',missingSocket,'','manual','new',id],{stdio:'ignore',env:{...process.env,JOBS_CLAUDE_BINARY:path.join(root,'not-present')}});
 const [missingCode]=await once(missing,'exit');assert.equal(missingCode,1);assert(!fs.existsSync(missingSocket));
 const unrelated=spawn(process.execPath,['-e','setInterval(()=>{},1000)'],{stdio:'ignore'});
 try{await testOwnedStop(false);assert(alive(unrelated.pid));await testOwnedStop(true);assert(alive(unrelated.pid));}
 finally{unrelated.kill('SIGTERM');await once(unrelated,'exit');}
 console.log('PASS: launch modes, input rejection, owned graceful/forced close, private socket, parent-loss cleanup, spawn failure and unrelated-process survival');
})().catch(e=>{console.error(e);process.exitCode=1;});
