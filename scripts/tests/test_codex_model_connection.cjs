'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const cp=require('node:child_process');for(const name of ['spawn','spawnSync','exec','execFile','execSync','execFileSync','fork'])cp[name]=()=>{throw Error('Real processes are forbidden in this fake-RPC fixture');};process.kill=()=>{throw Error('Real signals are forbidden in this fake-RPC fixture');};
const root=require('./fixture-paths.cjs').fixture('cxlink'),assets=path.resolve('app/src/main/assets');
for(const f of ['codex-chat.cjs','agent-receipts.cjs','agent-attachments.cjs','agent-action-preview.cjs','agent-completion-times.cjs'])fs.copyFileSync(path.join(assets,f),path.join(root,f));
fs.copyFileSync(path.join(assets,'codex-control.js'),path.join(root,'codex-wire-v0.2.22.js'));
const {CodexChat}=require(path.join(root,'codex-chat.cjs'));
const rpc=()=>({call:async()=>({}),send(){},close(){}});
function chat(id){const c=new CodexChat(rpc(),{endpoint:path.join(root,id),language:'en'});c.thread=id;c.turn='turn-'+id;c.busy=true;c.ready=true;return c;}
(async()=>{
 const a=chat('a'),b=chat('b');const emit=(method,params={})=>a.event({method,params:{threadId:a.thread,turnId:'turn-a',...params}});
 emit('error',{willRetry:true,error:{message:'Reconnecting... 2/5',additionalDetails:'synthetic reset'}});
 let s=await a.snapshot();assert.deepEqual(s.modelConnection,{state:'retrying',turn:'turn-a',reason:'Reconnecting... 2/5',details:'synthetic reset',attempt:2,limit:5});assert(s.connected&&s.busy);assert.equal((await b.snapshot()).modelConnection,null);assert(!s.rows.some(r=>r.kind==='error'));
 const q={op:'chat-send',session:a.thread,id:'msg_'+'a'.repeat(32),kind:'message',text:'Synthetic clarification'};assert((await a.send(q)).accepted);assert.equal((await a.snapshot()).modelConnection.state,'retrying');
 emit('item/started',{item:{id:'user',type:'userMessage',content:[{type:'text',text:q.text}],clientId:q.id}});assert.equal((await a.snapshot()).modelConnection.state,'retrying');
 emit('item/started',{item:{id:'tool',type:'commandExecution',command:'fixture',status:'inProgress'}});assert.equal((await a.snapshot()).modelConnection.state,'retrying');
 emit('item/agentMessage/delta',{turnId:'old-turn',itemId:'stale',delta:'late'});assert.equal((await a.snapshot()).modelConnection.state,'retrying');
 emit('item/agentMessage/delta',{itemId:'assistant',delta:'Resumed response'});assert.equal((await a.snapshot()).modelConnection,null);
 emit('error',{willRetry:true,error:{message:'Reconnecting... 3'}});s=await a.snapshot();assert.equal(s.modelConnection.attempt,3);assert(!Object.hasOwn(s.modelConnection,'limit'));
 emit('error',{willRetry:true,error:{message:'Transport retry pending'}});s=await a.snapshot();assert(!Object.hasOwn(s.modelConnection,'attempt'));assert(!Object.hasOwn(s.modelConnection,'limit'));
 emit('error',{willRetry:false,error:{message:'Synthetic final error'}});assert.equal((await a.snapshot()).modelConnection.state,'failed');
 emit('turn/completed',{turn:{id:'turn-a',status:'failed',error:{message:'Synthetic final error'}}});s=await a.snapshot();assert(!s.busy);assert.equal(s.modelConnection.state,'failed');assert.equal(s.rows.filter(r=>r.kind==='error').length,1);assert.equal((await b.snapshot()).modelConnection,null);
 emit('turn/started',{turn:{id:'next-turn'}});assert.equal((await a.snapshot()).modelConnection,null);assert(a.busy);
 // A normal request waiting on a tool is not labelled as reconnecting.
 emit('item/started',{turnId:'next-turn',item:{id:'tool2',type:'commandExecution',command:'fixture'}});assert.equal((await a.snapshot()).modelConnection,null);
 emit('error',{turnId:'next-turn',willRetry:true,error:{message:'Reconnecting... 1/5'}});emit('turn/completed',{turn:{id:'next-turn',status:'interrupted'}});assert.equal((await a.snapshot()).modelConnection,null);assert.equal(a.busy,false);
 a.rpc.onFailure(Error('fixture local RPC closed'));assert.equal((await a.snapshot()).modelConnection.state,'unknown');assert.equal((await a.snapshot()).connected,false);
 console.log('PASS native-event projection: retry count, no invented limit, ACK != recovery, stale events, terminal failure, two independent chats, normal tool wait');
})().catch(e=>{console.error(e);process.exitCode=1;});
