'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const root=require('./fixture-paths.cjs').fixture('sub');
for(const name of ['codex-model-catalog.cjs','codex-chat.cjs','agent-receipts.cjs','agent-attachments.cjs','agent-action-preview.cjs','agent-completion-times.cjs'])fs.copyFileSync(path.resolve('app/src/main/assets',name),path.join(root,name));
fs.copyFileSync(path.resolve('app/src/main/assets/codex-control.js'),path.join(root,'codex-wire-v0.2.22.js'));
const {CodexChat,subagentHistory}=require(path.join(root,'codex-chat.cjs'));
const parent='parent',child='child';
const event={tool:'spawnAgent',senderThreadId:parent,receiverThreadIds:[child],prompt:'Review this task'};
const metadata=owner=>({thread:{id:child,parentThreadId:owner,status:{type:'notLoaded'}}});
const answer={id:'answer',type:'agentMessage',text:'Full child answer'};
const page=(data,nextCursor=null)=>({data,nextCursor});
(async()=>{
 let calls=[];
 let rpc={call:async(method,p)=>{calls.push([method,p]);if(method==='thread/read')return metadata(parent);return p.cursor?page([answer]):page([{item:{id:'task',type:'userMessage',content:[{type:'text',text:'Full child task'}]},turnId:'t'}],'second');}};
 let loaded=await subagentHistory(rpc,parent,event);
 assert.equal(loaded.agentHistory.child.messages.length,2);assert(loaded.agentHistory.child.complete);
 assert.deepEqual(calls.map(c=>c[0]),['thread/read','thread/items/list','thread/items/list']);
 assert(calls.every(c=>!['turn/start','thread/resume','thread/start'].includes(c[0])));
 calls=[];loaded=await subagentHistory({call:async(method,p)=>{calls.push(method);assert.equal(method,'thread/read');return metadata('unrelated');}},parent,event);
 assert.deepEqual(calls,['thread/read']);assert(loaded.agentHistory.child.unavailable);assert(!loaded.agentHistory.child.messages);
 await assert.rejects(()=>subagentHistory(rpc,parent,{...event,senderThreadId:'foreign'}));
 calls=[];loaded=await subagentHistory({call:async()=>{throw Error('Must not read paths or the parent');}},parent,{receiverThreadIds:['../secret','/path',parent,42,'']});assert.equal(Object.keys(loaded.agentHistory).length,0);
 loaded=await subagentHistory({call:async(method,p)=>{if(method==='thread/items/list')throw Object.assign(Error('old CLI'),{rpcCode:-32601});return p.includeTurns?{thread:{...metadata(parent).thread,turns:[{items:[answer]}]}}:metadata(parent);}},parent,event);
 assert.equal(loaded.agentHistory.child.source,'thread/read');assert.equal(loaded.agentHistory.child.messages[0].text,answer.text);
 let reads=0;loaded=await subagentHistory({call:async(method,p)=>{if(method==='thread/read'){reads++;assert(!p.includeTurns);return metadata(parent);}throw Object.assign(Error('Permission denied'),{rpcCode:-32000});}},parent,event);
 assert.equal(reads,1);assert(loaded.agentHistory.child.unavailable);
 loaded=await subagentHistory({call:async method=>method==='thread/read'?metadata(parent):page([answer],'repeat')},parent,event);assert(loaded.agentHistory.child.unavailable);
 const prior={...event,agentHistory:{child:{relationVerified:true,messages:[{id:'old',text:'Preserved answer'}],complete:true}}};
 loaded=await subagentHistory({call:async()=>{throw Error('PRIVATE_PROTOCOL_ERROR');}},parent,prior);
 assert.equal(loaded.agentHistory.child.messages[0].text,'Preserved answer');assert(loaded.agentHistory.child.refreshUnavailable);assert(!JSON.stringify(loaded).includes('PRIVATE_PROTOCOL_ERROR'));
 // Large event text stays intact behind the existing paginated detail API.
 const long='BEGIN\n'+'Я'.repeat(160000)+'\nEND';
 const chat=new CodexChat(rpc,{endpoint:path.join(root,'chat'),access:'project',approval:'on-request',cwd:root,chat:'new'});chat.thread=parent;
 const item={id:'spawn',type:'collabAgentToolCall',...event,prompt:long,status:'inProgress'};
 chat.event({method:'item/started',params:{threadId:parent,turnId:'turn',item}});
 const row=chat.groups()[0].rows[0];assert.equal(row.toolName,'spawnAgent');assert.equal(row.toolType,'collabAgentToolCall');
 assert.equal(JSON.parse(chat.full.get(row.key)).prompt,long);
 chat.event({method:'item/completed',params:{threadId:'foreign',turnId:'turn',item:{...item,id:'foreign'}}});assert.equal(chat.groups().length,1);
 let result=await chat.handle({op:'chat-detail',session:'foreign',message:row.message,part:row.part,offset:0,subagent:true});assert(!result.ok);
 // A live completion received during history reading must retain its newer prompt and answer.
 let release;chat.rpc.call=async(method,p)=>{if(method==='thread/read')return metadata(parent);await new Promise(r=>release=r);return page([answer]);};
 const query={op:'chat-detail',session:parent,message:row.message,part:row.part,offset:0,subagent:true};
 const pending=chat.handle(query);while(!release)await new Promise(r=>setImmediate(r));
 chat.event({method:'item/completed',params:{threadId:parent,turnId:'turn',item:{...item,status:'completed',prompt:long+' UPDATED',agentsStates:{child:{status:'completed',message:'New response'}}}}});release();result=await pending;
 let full=result.body;while(result.next!==null){result=await chat.handle({...query,offset:result.next});assert(result.ok);full+=result.body;}
 const detail=JSON.parse(full);assert.equal(detail.prompt,long+' UPDATED');assert.equal(detail.agentsStates.child.message,'New response');assert.equal(detail.agentHistory.child.messages[0].text,answer.text);
 assert.equal(chat.groups()[0].rows[0].status,'completed');
 // New native activity events have the same stable row/detail identity.
 chat.event({method:'item/completed',params:{threadId:parent,turnId:'turn',item:{id:'activity',type:'subAgentActivity',kind:'completed',agentPath:'/parent/worker',agentThreadId:child}}});
 const activity=chat.groups().at(-1).rows[0];assert.equal(activity.toolName,'completed');assert.equal(activity.status,'completed');assert.equal(JSON.parse(chat.full.get(activity.key)).agentThreadId,child);
 console.log('PASS: subagent events, complete text, paging, read-only parent verification, foreign isolation, legacy fallback, failure retention, cursor bounds and live-update race');
})().catch(error=>{console.error(error);process.exitCode=1;});
