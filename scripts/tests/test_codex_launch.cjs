'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{spawn}=require('node:child_process');
const {argumentsFor,reason}=require('../../app/src/main/assets/codex-launch.cjs');const root=require('./fixture-paths.cjs').fixture('clnch'),bin=path.join(root,'bin');fs.mkdirSync(bin);
assert(argumentsFor('/tmp/fixture.sock','danger-full-access','untrusted','1').includes('approval_policy="on-request"'));
for(const p of ['never','on-request'])assert(argumentsFor('/tmp/fixture.sock','read-only',p,'0').includes('approval_policy="'+p+'"'));
assert.throws(()=>argumentsFor('/tmp/f','full','never','0'));assert.throws(()=>argumentsFor('/tmp/f','read-only','untrusted','garbage'));
assert.equal(reason('approval_policy = "untrusted" is no longer supported; remove this setting'),'UNSUPPORTED_APPROVAL_CONFIG');assert.equal(reason('SYNTHETIC_KEY_NOT_A_REASON'),'');
function run(script){fs.writeFileSync(path.join(bin,'codex'),'#!/bin/sh\n'+script,{mode:0o700});return spawn(process.execPath,[path.resolve('app/src/main/assets/codex-launch.cjs'),path.join(root,'x.sock'),'danger-full-access','untrusted','0','en'],{env:{PATH:bin,HOME:root},stdio:['ignore','pipe','pipe']});}
(async()=>{
 // Runtime policy refusal/mismatch must stop before turn/start, without retrying never.
 const assets=path.resolve('app/src/main/assets');
 for(const name of ['codex-chat.cjs','agent-receipts.cjs','agent-action-preview.cjs','agent-completion-times.cjs'])fs.copyFileSync(path.join(assets,name),path.join(root,name));
 fs.copyFileSync(path.join(assets,'codex-control.js'),path.join(root,'codex-wire-v0.2.22.js'));
 const {CodexChat}=require(path.join(root,'codex-chat.cjs'));
 for(const fail of ['rejected','mismatch']){
  const calls=[],rpc={initialize:async()=>{},call:async(method,q)=>{calls.push({method,q});if(method==='account/read')return {requiresOpenaiAuth:false};if(method==='thread/start'){if(fail==='rejected')throw Error('Unsupported requested approval');return {cwd:root,approvalPolicy:'never',sandbox:{type:'dangerFullAccess'},thread:{id:'synthetic'}};}throw Error('Unexpected call');}};
  const chat=new CodexChat(rpc,{endpoint:path.join(root,'chat'),cwd:root,chat:'new',approval:'untrusted',access:'full'});
  await assert.rejects(chat.start());assert.equal(chat.ready,false);assert.equal(calls.filter(c=>c.method==='thread/start').length,1);assert.equal(calls.at(-1).q.approvalPolicy,'untrusted');assert(!calls.some(c=>c.method==='turn/start'));
 }
 let c=run("printf '%s\\n' SYNTHETIC_STDOUT; printf '%s\\n' SYNTHETIC_STDERR 'Error: approval_policy = \"untrusted\" is no longer supported; remove this setting' >&2; exit 7\n"),o='',e='';c.stdout.on('data',b=>o+=b);c.stderr.on('data',b=>e+=b);assert.equal(await new Promise(r=>c.once('exit',r)),7);assert(!o);assert(!e.includes('SYNTHETIC'));assert(e.includes('UNSUPPORTED_APPROVAL_CONFIG'));assert(e.includes('exit 7'));
 c=run('exit 0\n');assert.equal(await new Promise(r=>c.once('exit',r)),0);
 const files=fs.readdirSync(root);assert(!files.some(f=>/\.(log|stderr|stdout)$/.test(f)));
 c=run('trap \'printf STOPPED > \"'+path.join(root,'stopped')+'\"; exit 0\' TERM\nprintf \'%s\\n\' \"$$\" > \"'+path.join(root,'pid')+'\"\nwhile :; do /bin/sleep 1; done\n');for(let i=0;!fs.existsSync(path.join(root,'pid'));i++){assert(i<100);await new Promise(r=>setTimeout(r,20));}const pid=Number(fs.readFileSync(path.join(root,'pid'),'utf8'));c.kill('SIGTERM');assert.equal(await new Promise(r=>c.once('exit',r)),130);assert(fs.existsSync(path.join(root,'stopped')));assert.throws(()=>process.kill(pid,0),{code:'ESRCH'});console.log('PASS owned launcher: supported bootstrap args, unchanged legacy args, invalid input rejection, safe classified stderr, no raw output/files, exit/retry');
})().catch(e=>{console.error(e);process.exitCode=1;});
