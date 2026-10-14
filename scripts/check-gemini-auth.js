'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{spawn}=require('node:child_process'),{once}=require('node:events');
const bridge=path.resolve('app/src/main/assets/gemini-control.js'),{authEnvironment,stopProxy}=require(bridge);
const root=fs.mkdtempSync(path.resolve('.verification/gemini-auth-v1.0.17/auth-'));
(async()=>{
 const original={GEMINI_CLI_AUTH_OVERRIDE:'gemini-api-key',EXAMPLE:'unchanged'};
 const file=path.join(root,'policy.json');const policy=authEnvironment('google',file,original);
 assert.equal(policy.env.EXAMPLE,'unchanged');assert.equal(policy.env.GEMINI_CLI_AUTH_OVERRIDE,undefined);assert.equal(original.GEMINI_CLI_AUTH_OVERRIDE,'gemini-api-key');
 assert.deepEqual(JSON.parse(fs.readFileSync(file)),{security:{auth:{selectedType:'oauth-personal',enforcedType:'oauth-personal'}}});assert.equal(fs.statSync(file).mode&0o777,0o600);policy.cleanup();assert(!fs.existsSync(file));
 const api=authEnvironment('api',file,{});assert.equal(JSON.parse(fs.readFileSync(file)).security.auth.enforcedType,'gemini-api-key');api.cleanup();
 const auto=authEnvironment('auto',file,original);assert.deepEqual(auto.env,original);assert(!fs.existsSync(file));
 assert.throws(()=>authEnvironment('google',file,{GEMINI_CLI_SYSTEM_SETTINGS_PATH:'/external.json'}));assert(!fs.existsSync(file));
 assert.throws(()=>authEnvironment('invalid',file,{}));assert(!fs.existsSync(file));
 const unsafe=path.join(root,'unsafe');fs.mkdirSync(unsafe,{mode:0o777});fs.chmodSync(unsafe,0o777);assert.throws(()=>authEnvironment('google',path.join(unsafe,'policy.json'),{}));assert(!fs.existsSync(path.join(unsafe,'policy.json')));
 const socket=path.join(root,'12345678-1234-4234-8234-123456789abc.sock');const state=path.join(root,'state');const ready=path.join(root,'ready');
 const stub=path.join(root,'child.js');fs.writeFileSync(stub,`const fs=require('fs');if(!process.argv.includes('--debug'))process.exit(3);const auth=JSON.parse(fs.readFileSync(process.env.GEMINI_CLI_SYSTEM_SETTINGS_PATH)).security.auth;if(auth.enforcedType!=='oauth-personal'||auth.selectedType!=='oauth-personal'||process.env.GEMINI_CLI_AUTH_OVERRIDE)process.exit(2);const n=fs.existsSync(process.env.AUTH_TEST_STATE)?Number(fs.readFileSync(process.env.AUTH_TEST_STATE)):0;fs.writeFileSync(process.env.AUTH_TEST_STATE,String(n+1));if(n<2){process.send({type:'auth-selected-type',authType:'gemini-api-key'},()=>process.exit(199));}else{fs.writeFileSync(process.env.AUTH_TEST_READY,'ready');setInterval(()=>{},1000);}`);
 const owner=spawn(process.execPath,[bridge,'run',socket,'','default','new','22345678-1234-4234-8234-123456789abc','google'],{stdio:'ignore',env:{...process.env,JOBS_GEMINI_DEBUG:'1',JOBS_GEMINI_BINARY:stub,JOBS_GEMINI_AUTH_DIR:path.join(root,'private-auth'),AUTH_TEST_STATE:state,AUTH_TEST_READY:ready}});
 try{
  for(let i=0;i<100&&!fs.existsSync(ready);i++)await new Promise(r=>setTimeout(r,50));
  assert(fs.existsSync(ready));assert.equal(fs.readFileSync(state,'utf8'),'3');const exit=once(owner,'exit');await stopProxy(socket);await exit;assert(!fs.existsSync(socket));assert(!fs.existsSync(path.join(root,'private-auth',path.basename(socket,'.sock')+'.json')));
 }finally{if(owner.exitCode===null&&owner.signalCode===null)owner.kill('SIGTERM');}
 console.log('PASS: explicit Google/API/native modes; config0600; existing policy refusal; unchanged caller env; Google preserved across two restarts and conflicting IPC; owned stop cleanup');
})().catch(e=>{console.error(e);process.exitCode=1;});
