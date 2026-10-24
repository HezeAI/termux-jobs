'use strict';
const assert=require('node:assert/strict');
const {GeminiChat}=require('../../app/src/main/assets/gemini-chat.cjs');
(async()=>{
 let session='11111111-1111-4111-8111-111111111111',state='idle',submits=0,shells=0;
 const api={session:()=>session,status:()=>({available:true,state}),supported:()=>true,permission:()=>null,receipt:()=>null};
 const chat=new GeminiChat(api),config={isTrustedFolder:()=>true},observe=(ready,submit=async()=>{submits++;})=>chat.observe(config,{ready,native:false,history:[],pending:[]},submit,false,()=>{});
 observe(true);const q={op:'chat-send',session,id:'msg_preflight',kind:'message',text:'retry once ready'};
 const pending=chat.command(q);observe(false);const first=await pending.catch(e=>({ok:false,error:e.message}));assert.equal(first.ok,false);assert.equal(submits,0);
 observe(true);assert((await chat.command(q)).accepted,'a request rejected before invoking native submit must be retryable with its original ID');assert.equal(submits,1);await chat.command(q);assert.equal(submits,1);
 // Preserve ambiguous failures after handing control to the native callback.
 observe(true,async()=>{submits++;throw Error('ambiguous native callback failure');});const uncertain={...q,id:'msg_ambiguous'};
 await assert.rejects(()=>chat.command(uncertain));await assert.rejects(()=>chat.command(uncertain));assert.equal(submits,2,'an uncertain native call must not run twice');
 chat.bindShell(config,()=>{shells++;return true;});observe(true);const sq={...q,id:'msg_shell',kind:'shell',text:'printf fixture'};
 const shellPending=chat.command(sq);state='responding';const rejected=await shellPending.catch(e=>({ok:false,error:e.message}));assert.equal(rejected.ok,false);assert.equal(shells,0);state='idle';assert((await chat.command(sq)).accepted);await chat.command(sq);assert.equal(shells,1);
 session='22222222-2222-4222-8222-222222222222';assert.equal((await chat.command({...q,id:'msg_foreign'})).ok,false);assert.equal(submits,2);
 console.log('PASS Gemini recovery: UI/shell readiness loss before native dispatch permits same-ID retry, admitted/ambiguous callbacks remain deduplicated, foreign-session sends refused');
})().catch(e=>{console.error(e);process.exitCode=1;});
