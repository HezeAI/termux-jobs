'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {OpenCodeChat}=require('../../app/src/main/assets/opencode-chat.cjs');
const {ReceiptStore}=require('../../app/src/main/assets/agent-receipts.cjs');
const root=require('./fixture-paths.cjs').fixture('oc');
(async()=>{
 const session='ses_synthetic',file=path.join(root,'receipts.json');let posts=0,formState='pending';
 const assistant={id:'msg_assistant',type:'assistant',content:[{type:'reasoning',text:'first thought'},{type:'text',text:'answer'},{type:'tool',id:'tool_one',name:'read',state:{status:'completed',input:{filePath:'/project/skills/example/SKILL.md'},content:[{type:'text',text:'data'}]}}]};
 const form={id:'frm_test',sessionID:session,title:'Question',fields:[{key:'q',type:'string'}]};
 const request=async(method,route,body)=>{
  if(route==='/api/shell/sh_foreign'){assert.equal(method,'GET');return {data:{id:'sh_foreign',status:'running',metadata:{sessionID:'ses_other'}}};}
  assert(route.startsWith('/api/session'));if(route==='/api/session/'+session)return {data:{model:{providerID:'opencode',id:'test-model',variant:'high',headers:{Authorization:'private fixture'}}}};if(route==='/api/session/active')return {data:{[session]:{type:'running'}}};
  if(method==='GET'&&(route.endsWith('/inbox')||route.endsWith('/permission')))return {data:[]};
  if(method==='GET'&&route.endsWith('/form'))return {data:[form]};
  if(method==='GET'&&route.endsWith('/form/frm_test'))return {data:{fields:form.fields,title:form.title,id:form.id,sessionID:session,state:{status:formState}}};
  if(method==='GET'&&route.includes('/message?'))return {data:[assistant,{id:'msg_user',type:'user',text:'hello'}],cursor:{next:'opaque/cursor'}};
  if(method==='GET'&&route.endsWith('/message/msg_assistant'))return {data:assistant};
  if(method==='POST'){posts++;return {};}
  throw Error('Unexpected route '+route);
 };
 const chat=new OpenCodeChat(request,session,file);chat.connected=true;
 let r=await chat.command({op:'chat-snapshot'});assert(r.nativeChat);assert.deepEqual(r.model,{providerID:'opencode',id:'test-model',variant:'high'});assert(r.busy);assert.equal(r.rows[0].kind,'user');assert.deepEqual(r.rows.slice(1).map(r=>r.kind),['reasoning','text','tool']);
 const action=r.rows.find(r=>r.kind==='tool');assert.equal(action.toolName,'read');assert.equal(action.status,'completed');assert.equal(action.toolPath,'/project/skills/example/SKILL.md');
 chat.event({type:'session.reasoning.delta',data:{sessionID:'ses_other',assistantMessageID:'msg_assistant',ordinal:0,delta:'foreign'}});assert.equal(chat.live.size,0);
 chat.event({type:'session.reasoning.delta',data:{sessionID:session,assistantMessageID:'msg_assistant',ordinal:0,delta:'live thought longer'}});
 r=await chat.command({op:'chat-snapshot'});assert.equal(r.rows[1].body,'live thought longer');
 chat.event({type:'permission.asked',data:{id:'per_test',sessionID:session,action:'read',source:{id:'tool_one',messageID:'msg_assistant'}}});
 chat.event({type:'permission.replied',data:{requestID:'per_test',sessionID:session,reply:'once'}});
 r=await chat.command({op:'chat-snapshot'});assert.deepEqual(r.rows.slice(1).map(r=>r.kind),['reasoning','text','receipt','tool']);
 const stable=r.rows.map(r=>r.key);assert.deepEqual((await chat.command({op:'chat-snapshot'})).rows.map(r=>r.key),stable);assert.equal(new ReceiptStore(file,'O').rows.length,1);
 const q={session,op:'chat-send',id:'msg_send',kind:'message',text:'hello'};await Promise.all([chat.command(q),chat.command(q)]);assert.equal(posts,1);await assert.rejects(()=>chat.command({...q,text:'different'}));
 await assert.rejects(()=>chat.command({...q,id:'../../passwd'}));await assert.rejects(()=>chat.command({...q,text:'x'.repeat(32001)}));
 const nonce=r.forms[0].nonce;await chat.command({session,op:'chat-form',id:'frm_test',nonce,answer:{q:'yes'}});assert.equal(posts,2);
 r=await chat.command({op:'chat-snapshot'});formState='answered';await assert.rejects(()=>chat.command({session,op:'chat-form',id:'frm_test',nonce:r.forms[0].nonce,answer:{q:'yes'}}));assert.equal(posts,2);
 assistant.content[1].text='x'.repeat(26000);const detail=await chat.command({session,op:'chat-detail',message:'msg_assistant',part:'text:0'});assert.equal(detail.body.length,24000);assert.equal(detail.next,24000);assert.equal((await chat.command({session,op:'chat-detail',message:'msg_assistant',part:'text:0',offset:detail.next})).body.length,2000);
 await assert.rejects(()=>chat.command({session,op:'chat-detail',message:'../../auth',part:'text:0'}));
 form.fields[0].when=[{key:'previous',op:'eq',value:'yes'}];assert((await chat.command({op:'chat-snapshot'})).needsTerminal);delete form.fields[0].when;
 chat.event({type:'session.shell.started',data:{sessionID:session,shell:{id:'sh_foreign'}}});await assert.rejects(()=>chat.command({session,op:'chat-interrupt'}),/Foreign shell/);assert.equal(posts,2);
 const notice={id:'msg_notice',type:'synthetic',text:'The following shell command was executed by the user:\n<shell id="sh_demo" state="completed" command="false">\nExited with code 127\n</shell>'};
 assert.equal(chat.rows([notice])[0].kind,'tool');assert.equal(chat.rows([notice])[0].part,'synthetic');
 assert.equal(chat.rows([{id:'msg_shell',type:'shell',shellID:'sh_demo'},notice]).length,1);
 assert.equal(chat.rows([{...notice,type:'user'}])[0].kind,'user');
 chat.close();assert.equal(chat.live.size,0);console.log('PASS: native chat ordering, live reasoning, foreign isolation, stable tool receipt location, dedup sends, bounds, stale forms, paged detail');
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(()=>fs.rmSync(root,{recursive:true,force:true}));
