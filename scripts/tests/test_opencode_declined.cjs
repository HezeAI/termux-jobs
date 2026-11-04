'use strict';
// Only structured same-tool/message/session cancellation evidence is accepted.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {OpenCodeChat}=require('../../app/src/main/assets/opencode-chat.cjs');
const {ReceiptStore}=require('../../app/src/main/assets/agent-receipts.cjs');
const root=require('./fixture-paths.cjs').fixture('oden');const session='ses_declined';let messages=[],posts=0;
const request=async(method,url)=>{
 if(method==='POST'){posts++;return {data:{delivery:'normal'}};}
 if(url==='/api/session/'+session)return {data:{model:{providerID:'audit',id:'audit'}}};
 if(url==='/api/session/active')return {data:{}};
 if(url.includes('/message?'))return {data:messages.slice().reverse()};
 if(url.includes('/message/'))return {data:messages.find(m=>m.id===url.split('/').at(-1))};
 return {data:[]};
};
const native=()=>({id:'msg_denied',type:'assistant',error:{type:'aborted',message:'Step interrupted'},content:[{type:'tool',id:'call_denied',name:'shell',state:{status:'error',input:{command:'printf artificial'},error:{type:'aborted',message:'The user declined this tool call'}}}]});
function receipt(file,mutate=()=>{}){const proof={sessionID:session,source:{type:'tool',messageID:'msg_denied',id:'call_denied'}};mutate(proof);new ReceiptStore(file,'O').add('call_denied','cancel','shell',JSON.stringify(proof));}
(async()=>{
 const file=path.join(root,'valid.json');receipt(file);messages=[native()];let chat=new OpenCodeChat(request,session,file);chat.connected=true;const before=JSON.stringify(messages);
 let snap=await chat.snapshot({});assert.equal(snap.agentError,false);assert.equal(snap.rows.find(r=>r.part==='error').kind,'info');assert.equal(snap.rows.find(r=>r.part==='error').actionOutcome,'user_declined');assert.equal(snap.rows.find(r=>r.kind==='tool').status,'declined');assert.equal(JSON.stringify(messages),before,'Projection must not modify native history');assert((await chat.command({op:'chat-detail',session,message:'msg_denied',part:'error'})).body.includes('Step interrupted'),'Native diagnostic remains available');
 chat.close();chat=new OpenCodeChat(request,session,file);assert.equal((await chat.snapshot({})).agentError,false,'Persisted receipt works on reopen');
 let variants=0;
 for(const mutate of [p=>p.sessionID='ses_other',p=>p.source.id='call_other',p=>p.source.messageID='msg_other',p=>p.source.type='shell',p=>delete p.source]){
  const f=path.join(root,'wrong-'+variants+++'.json');receipt(f,mutate);let c=new OpenCodeChat(request,session,f);assert.equal((await c.snapshot({})).agentError,true);assert.equal(c.rows(messages).find(r=>r.part==='error').kind,'error');c.close();
 }
 for(const variant of ['missing','malformed','allowed','completed','tool-other-error','text-only','mixed','auth401','network','unrelated-abort']){
  const f=path.join(root,variant+'.json');messages=[native()];
  if(!['missing','allowed','malformed'].includes(variant))receipt(f);
  if(variant==='malformed')new ReceiptStore(f,'O').add('call_denied','cancel','shell','not JSON');
  if(variant==='allowed')new ReceiptStore(f,'O').add('call_denied','proceed_once','shell',JSON.stringify({sessionID:session,source:{type:'tool',messageID:'msg_denied',id:'call_denied'}}));
  if(variant==='completed')messages[0].content[0].state.status='completed';
  if(variant==='tool-other-error')messages[0].content[0].state.error.type='io_error';
  if(variant==='text-only')messages[0].content=[{type:'text',text:'The user declined this tool call'}];
  if(variant==='mixed')messages[0].content.push({type:'tool',id:'call_unrelated',name:'read',state:{status:'error',error:{type:'aborted',message:'Step interrupted'}}});
  if(variant==='auth401')messages[0].error={type:'api_error',code:401,message:'Unauthorized'};
  if(variant==='network')messages[0].error={type:'network_error',message:'Connection failed'};
  if(variant==='unrelated-abort')messages[0].content[0].id='call_unrelated';
  const c=new OpenCodeChat(request,session,f),r=await c.snapshot({});assert.equal(r.agentError,true,variant);assert.equal(r.rows.find(r=>r.part==='error').kind,'error',variant);c.close();
 }
 messages=[native()];chat=new OpenCodeChat(request,session,file);await chat.command({op:'chat-send',session,id:'msg_after_decline',kind:'message',text:'Synthetic next request'});assert.equal(posts,1);messages.push({id:'msg_next',type:'assistant',content:[{type:'text',text:'Synthetic next answer'}]});snap=await chat.snapshot({});assert.equal(snap.agentError,false);assert(snap.rows.some(r=>r.body==='Synthetic next answer'));assert(!snap.rows.some(r=>r.kind==='error'));chat.close();
 console.log('PASS same-action proof, persisted reopen, 15 unproven/unrelated cases retain errors, unchanged native records, next request');
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(()=>fs.rmSync(root,{recursive:true,force:true}));
