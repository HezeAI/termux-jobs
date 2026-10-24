'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=require('./fixture-paths.cjs').fixture('cxlogin');for(const [a,b]of [['codex-chat.cjs','codex-chat.cjs'],['codex-control.js','codex-wire-v0.2.22.js'],['agent-receipts.cjs','agent-receipts.cjs']])fs.copyFileSync(path.join(__dirname,'../../app/src/main/assets',a),path.join(root,b));
const {CodexChat}=require(path.join(root,'codex-chat.cjs'));
(async()=>{
 let signed=false,starts=0,initializes=0,display='',cancelled=0,loginStarted=0;
 const rpc={async initialize(){initializes++;},close(){},send(){},async call(method,params){
  if(method==='account/read'){assert.equal(params.refreshToken,false);return {requiresOpenaiAuth:true,account:signed?{type:'chatgpt',email:'private fixture'}:null};}
  if(method==='account/login/start'){loginStarted++;assert.deepEqual(params,{type:'chatgptDeviceCode'});return {type:'chatgptDeviceCode',loginId:'owned-login',verificationUrl:'https://auth.openai.com/codex/device',userCode:'TEST-CODE'};}
  if(method==='account/login/cancel'){cancelled++;assert.equal(params.loginId,'owned-login');return {};}
  if(method==='thread/start'){starts++;return {cwd:root,approvalPolicy:'on-request',sandbox:{type:'workspaceWrite'},thread:{id:'fixture-chat'},model:'fixture'};}
  throw Error('Unexpected method '+method);
 }};
 const chat=new CodexChat(rpc,{endpoint:path.join(root,'s'),cwd:root,chat:'new',access:'project',approval:'on-request',writeLogin:s=>display+=s});await chat.start();assert.equal(initializes,1);assert.equal(starts,0);assert.equal(loginStarted,0);assert((await chat.snapshot()).authRequired);
 await Promise.all([chat.handle({op:'terminal-open'}),chat.handle({op:'terminal-open'})]);assert.equal(loginStarted,1);assert(display.includes('TEST-CODE'));assert(!JSON.stringify(await chat.snapshot()).includes('TEST-CODE'));assert(!JSON.stringify(await chat.snapshot()).includes('auth.openai.com'));
 chat.event({method:'account/login/completed',params:{loginId:'foreign-login',success:true}});assert.equal(starts,0);
 await chat.handle({op:'terminal-close'});assert.equal(cancelled,0);assert((await chat.snapshot()).loginPending);await chat.handle({op:'terminal-exit'});assert.equal(cancelled,1);assert((await chat.snapshot()).authRequired);await chat.handle({op:'terminal-open'});assert.equal(loginStarted,2);
 signed=true;chat.event({method:'account/login/completed',params:{loginId:'owned-login',success:true}});for(let i=0;i<30&&!chat.ready;i++)await new Promise(r=>setTimeout(r,5));assert(chat.ready);assert.equal(starts,1);assert.equal(initializes,1);assert(!(await chat.snapshot()).authRequired);assert(!JSON.stringify(await chat.snapshot()).includes('private fixture'));
 await chat.close();assert.equal(cancelled,1);
 let printed=false;const bad=new CodexChat({...rpc,async call(m,p){if(m==='account/read')return {requiresOpenaiAuth:true,account:null};if(m==='account/login/start')return {type:'chatgptDeviceCode',loginId:'owned-login',verificationUrl:'http://attacker.invalid/',userCode:'BAD-CODE'};return rpc.call(m,p);}},{endpoint:path.join(root,'bad'),cwd:root,chat:'new',access:'read',approval:'never',writeLogin:()=>printed=true});await bad.start();const r=await bad.openTerminal();assert.equal(r.terminalReady,false);assert.equal(printed,false);assert((await bad.snapshot()).loginError);assert.equal(cancelled,2);await bad.close();
 let providerStart=0;const custom=new CodexChat({...rpc,async call(m,p){if(m==='account/read')return {requiresOpenaiAuth:false,account:null};if(m==='thread/start'){providerStart++;return {cwd:root,approvalPolicy:'never',sandbox:{type:'readOnly'},thread:{id:'custom-chat'}};}return rpc.call(m,p);}},{endpoint:path.join(root,'custom'),cwd:root,chat:'new',access:'read',approval:'never'});await custom.start();assert(custom.ready);assert.equal(providerStart,1);await custom.close();
 console.log('PASS Codex first-login flow: no model/flow before consent, native device RPC, dedup, terminal-only code, scope, cancel/retry, reconnect, bad-origin refusal/cancel, auth-free provider preservation; no real login/account API');
})().catch(e=>{console.error(e);process.exitCode=1;});
