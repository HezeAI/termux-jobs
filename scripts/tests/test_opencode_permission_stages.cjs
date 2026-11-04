'use strict';
// Directory consent and tool consent are independent events for the same tool.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {OpenCodeChat}=require('../../app/src/main/assets/opencode-chat.cjs');
const {ReceiptStore}=require('../../app/src/main/assets/agent-receipts.cjs');
const root=require('./fixture-paths.cjs').fixture('ostage'),session='ses_stages';
let messages=[],posts=0;
const request=async(method,url)=>{
 if(method==='POST'){posts++;return {data:{delivery:'normal'}};}
 if(url==='/api/session/'+session)return {data:{model:{providerID:'audit',id:'audit'}}};
 if(url==='/api/session/active')return {data:{}};
 if(url.includes('/message?'))return {data:messages.slice().reverse()};
 if(url.includes('/message/'))return {data:messages.find(m=>m.id===url.split('/').at(-1))};
 return {data:[]};
};
const native=tool=>({id:'msg_'+tool,type:'assistant',error:{type:'aborted',message:'Step interrupted'},content:[{type:'tool',id:'call_'+tool,name:tool,state:{status:'error',input:{path:'/synthetic/outside/file'},error:{type:'aborted',message:'The user declined this tool call'}}}]});
function decision(chat,tool,permission,action,reply){
 chat.event({type:'permission.asked',data:{id:permission,sessionID:session,action,source:{type:'tool',id:'call_'+tool,messageID:'msg_'+tool}}});
 chat.event({type:'permission.replied',data:{requestID:permission,sessionID:session,reply}});
}
async function neutral(chat){
 const snap=await chat.snapshot({});assert.equal(snap.agentError,false);
 assert.equal(snap.rows.find(r=>r.kind==='tool').status,'declined');
 assert.equal(snap.rows.find(r=>r.part==='error').kind,'info');
 assert.equal(snap.rows.find(r=>r.part==='error').actionOutcome,'user_declined');
 return snap;
}
(async()=>{
 for(const tool of ['read','write']){
  messages=[native(tool)];const before=JSON.stringify(messages);
  for(const stage of ['directory','action']){
   const file=path.join(root,tool+'-'+stage+'.json');let chat=new OpenCodeChat(request,session,file);
   if(stage==='action')decision(chat,tool,'per_directory','external_directory','once');
   decision(chat,tool,stage==='action'?'per_action':'per_directory',stage==='action'?(tool==='write'?'edit':'read'):'external_directory','reject');
   const receipts=new ReceiptStore(file,'O');assert.equal(receipts.rows.length,stage==='action'?2:1);
   if(stage==='action'){
    assert.equal(receipts.get('per_directory').outcome,'proceed_once');
    assert.equal(receipts.get('per_action').outcome,'cancel');
    // Delayed/replayed first-stage event cannot overwrite the second decision.
    decision(chat,tool,'per_directory','external_directory','once');
    assert.equal(new ReceiptStore(file,'O').get('per_action').outcome,'cancel');
   }
   const first=await neutral(chat),keys=first.rows.map(r=>r.key);
   assert.equal(first.rows.filter(r=>r.kind==='receipt').length,receipts.rows.length);
   chat.close();chat=new OpenCodeChat(request,session,file);
   assert.deepEqual((await neutral(chat)).rows.map(r=>r.key),keys,'Cold reopen retains both stage rows');
   assert.equal(JSON.stringify(messages),before,'Native history must stay unchanged');
   await chat.command({op:'chat-send',session,id:'msg_next_'+tool+'_'+stage,kind:'message',text:'Synthetic next request'});
   chat.close();
  }
  messages=[native(tool)];delete messages[0].error;messages[0].finish='stop';messages[0].content[0].state.status='completed';
  const allowedFile=path.join(root,tool+'-both-allowed.json');let allowed=new OpenCodeChat(request,session,allowedFile);
  decision(allowed,tool,'per_directory','external_directory','once');decision(allowed,tool,'per_action',tool==='write'?'edit':'read','once');
  const allowedView=await allowed.snapshot({});assert.equal(allowedView.agentError,false);assert.equal(allowedView.rows.filter(r=>r.kind==='receipt').length,2);assert.equal(allowedView.rows.find(r=>r.kind==='tool').status,'completed');assert(!allowedView.rows.some(r=>r.actionOutcome==='user_declined'));allowed.close();
  messages=[native(tool)];
  // A legacy file that lost the second decision is not repaired by error text.
  const file=path.join(root,tool+'-legacy-allow-only.json');
  new ReceiptStore(file,'O').add('call_'+tool,'proceed_once','external_directory',JSON.stringify({sessionID:session,source:{type:'tool',id:'call_'+tool,messageID:'msg_'+tool}}));
  let chat=new OpenCodeChat(request,session,file);assert.equal((await chat.snapshot({})).agentError,true);chat.close();
  // A duplicate/foreign/malformed event is never evidence of a user refusal.
  for(const variant of ['unknown-reply','foreign-session','wrong-source','wrong-key']){
   const file=path.join(root,tool+'-'+variant+'.json');chat=new OpenCodeChat(request,session,file);
   const p={id:'per_bad',sessionID:variant==='foreign-session'?'ses_other':session,action:'read',source:{type:'tool',id:variant==='wrong-source'?'call_other':'call_'+tool,messageID:'msg_'+tool}};
   chat.event({type:'permission.asked',data:p});chat.event({type:'permission.replied',data:{requestID:p.id,sessionID:p.sessionID,reply:variant==='unknown-reply'?'unexpected':'reject'}});
   if(variant==='wrong-key'){
    fs.writeFileSync(file,JSON.stringify([{key:'per_wrong',token:'12345678',outcome:'cancel',title:'read',body:JSON.stringify(p)}]));
   }
   assert.equal((await chat.snapshot({})).agentError,true,variant);chat.close();
  }
 }
 assert.equal(posts,4);
 console.log('PASS read/write: directory No and directory Allow -> action No; both receipts, cold reopen, next request, legacy missing proof, 8 invalid-event cases, unchanged native records');
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(()=>fs.rmSync(root,{recursive:true,force:true}));
