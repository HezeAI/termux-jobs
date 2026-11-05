'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../..'),dir=require('./fixture-paths.cjs').fixture('cctx');
for(const name of ['codex-chat.cjs','agent-receipts.cjs','agent-action-preview.cjs','agent-completion-times.cjs'])fs.copyFileSync(path.join(root,'app/src/main/assets',name),path.join(dir,name));fs.copyFileSync(path.join(root,'app/src/main/assets/codex-control.js'),path.join(dir,'codex-wire-v0.2.22.js'));
const {CodexChat}=require(path.join(dir,'codex-chat.cjs'));
let calls=[],reply=async()=>({}),history=[];
function controller(id='chat-a'){
 const rpc={async initialize(){return {userAgent:'termux_jobs_control/0.166.0 (Linux)'};},send(){},close(){},async call(method,params){calls.push({method,params});
  if(method==='account/read')return {requiresOpenaiAuth:false};
  if(method==='thread/start'||method==='thread/resume')return {cwd:dir,approvalPolicy:'untrusted',sandbox:{type:'dangerFullAccess'},model:'model-a',thread:{id,status:{type:'idle'}}};
  if(method==='thread/read')return {thread:{id,cwd:dir}};
  if(method==='thread/turns/list')return {data:history,nextCursor:null};
  if(method==='turn/start')return {turn:{id:'new-model-turn'}};
  if(method==='model/list')return {data:[{model:'model-b',supportedReasoningEfforts:[{reasoningEffort:'medium'}]}]};
  if(method==='thread/compact/start')return reply();throw Error('Unexpected '+method);
 }};
 return new CodexChat(rpc,{endpoint:path.join(dir,id+'-endpoint'),cwd:dir,access:'full',approval:'untrusted',chat:'id',thread:id});
}
const event=(c,method,extra={})=>c.event({method,params:{threadId:c.thread,...extra}});
const usage=(c,used=128000)=>event(c,'thread/tokenUsage/updated',{turnId:'normal',tokenUsage:{last:{totalTokens:used,inputTokens:used-10,cachedInputTokens:120000,outputTokens:10},total:{totalTokens:99999999},modelContextWindow:256000}});
(async()=>{
 const c=controller();await c.start();let state=(await c.handle({op:'status'})).context;assert.equal(state.usage,null);assert(state.compactSupported);assert(state.canCompact);
 usage(c);state=c.contextState();assert.equal(state.usage.used,128000);assert.equal(state.usage.capacity,256000);assert(!state.stale); // Cached input stays included; lifetime total is ignored.
 c.event({method:'thread/tokenUsage/updated',params:{threadId:'foreign',tokenUsage:{last:{totalTokens:1},modelContextWindow:1}}});assert.equal(c.contextState().usage.used,128000);
 let release;reply=()=>new Promise(r=>release=r);const command=c.handle({op:'chat-compact',session:'chat-a'});assert.equal(c.compaction.phase,'starting');assert.equal(c.contextState().usage.used,128000);assert(c.contextState().stale);
 assert.equal((await c.handle({op:'chat-compact',session:'chat-a'})).error,'COMPACT_BUSY');assert.equal((await c.handle({op:'chat-compact',session:'foreign'})).error,'STALE_REQUEST');assert(!c.contextState().canCompact);
 assert(!(await c.handle({op:'chat-send',session:'chat-a',kind:'message',id:'msg_'+'1'.repeat(32),text:'No parallel turn'})).ok);
 release({});assert((await command).accepted);assert.equal(c.compaction.phase,'starting');
 event(c,'turn/started',{turn:{id:'compact-1'}});event(c,'item/completed',{turnId:'compact-1',item:{id:'compact-item',type:'contextCompaction'}});assert.equal(c.compaction.phase,'running');
 event(c,'turn/completed',{turn:{id:'foreign',status:'completed'}});assert.equal(c.compaction.phase,'running');
 event(c,'turn/completed',{turn:{id:'compact-1',status:'completed'}});assert.equal(c.compaction.phase,'completed');assert(c.contextState().canCompact);assert.equal(c.contextState().usage.used,128000);assert(c.contextState().stale);
 usage(c,32000);assert.equal(c.contextState().usage.used,32000);assert(!c.contextState().stale);assert((await c.snapshot()).rows.some(r=>r.toolType==='contextCompaction'));
 let reopened=controller();await reopened.start();assert.equal(reopened.contextState().usage.used,32000);assert(reopened.contextState().stale);usage(reopened,33000);assert(!reopened.contextState().stale);
 assert((await c.handle({op:'model',model:'model-b',effort:'medium'})).ok);assert(c.contextState().stale);assert.equal(c.contextState().usage.used,32000);
 assert((await c.handle({op:'chat-send',session:'chat-a',kind:'message',id:'msg_'+'2'.repeat(32),text:'New model'})).ok);event(c,'thread/settings/updated',{threadSettings:{model:'model-b'}});assert.equal(c.contextState().usage,null);usage(c,50000);assert.equal(c.contextState().usage.model,'model-b');assert.equal(c.contextState().usage.used,50000);assert(!c.contextState().stale);
 event(c,'thread/tokenUsage/updated',{tokenUsage:{last:{totalTokens:'10'},total:{totalTokens:1000},modelContextWindow:200}});assert.equal(c.contextState().usage,null);
 const bad=controller('bad');await bad.start();reply=async()=>{throw Object.assign(Error('Synthetic compact rejection'),{rpcCode:-32000});};assert(!(await bad.handle({op:'chat-compact',session:'bad'})).ok);assert.equal(bad.compaction.phase,'failed');assert(bad.contextState().canCompact);
 reply=async()=>({});await bad.handle({op:'chat-compact',session:'bad'});event(bad,'turn/started',{turn:{id:'failed-turn'}});event(bad,'turn/completed',{turn:{id:'failed-turn',status:'failed',error:{message:'Synthetic model error'}}});assert.equal(bad.compaction.phase,'failed');assert(bad.contextState().canCompact);
 await bad.handle({op:'chat-compact',session:'bad'});bad.rpc.onFailure(Error('Synthetic disconnect'));assert.equal(bad.compaction.phase,'unknown');assert(!bad.contextState().canCompact);assert(!(await bad.handle({op:'chat-compact',session:'bad'})).ok);
 reopened=controller('bad');await reopened.start();assert.equal(reopened.compaction.phase,'unknown');assert(!reopened.contextState().canCompact); // No replay and no false completion on reopening.
 assert((await reopened.handle({op:'chat-send',session:'bad',kind:'message',id:'msg_'+'3'.repeat(32),text:'Continue after reconnect'})).ok);event(reopened,'turn/completed',{turn:{id:'new-model-turn',status:'completed'}});assert.equal(reopened.compaction.phase,'failed');assert(reopened.contextState().canCompact);
 const recover=controller('recover');await recover.start();await recover.handle({op:'chat-compact',session:'recover'});event(recover,'turn/started',{turn:{id:'known-compact'}});recover.rpc.onFailure(Error('Synthetic disconnect'));history=[{id:'known-compact',status:'completed',items:[{id:'restored-compact',type:'contextCompaction'}]}];const recovered=controller('recover');await recovered.start();assert.equal(recovered.compaction.phase,'completed');assert((await recovered.snapshot()).rows.some(r=>r.toolType==='contextCompaction'));history=[];
 const unsupported=controller('unsupported');unsupported.rpc.initialize=async()=>({userAgent:'unknown-cli/99.0.0'});await unsupported.start();assert(!unsupported.contextState().canCompact);assert.equal((await unsupported.handle({op:'chat-compact',session:'unsupported'})).error,'COMPACT_UNSUPPORTED');
 const known=controller('method-missing');await known.start();reply=async()=>{throw Object.assign(Error('Unsupported method'),{rpcCode:-32601});};await known.handle({op:'chat-compact',session:'method-missing'});assert(!known.contextState().compactSupported);
 const timeout=controller('timeout');await timeout.start();reply=async()=>{throw Error('Codex request timeout');};await timeout.handle({op:'chat-compact',session:'timeout'});assert.equal(timeout.compaction.phase,'unknown');assert(!timeout.contextState().canCompact);
 assert.equal(calls.filter(c=>c.method==='thread/compact/start').length,7);
 const cache=path.join(dir,'chat-a.codex-context.json');assert(fs.statSync(cache).size<8192);assert.equal(fs.statSync(cache).mode&0o777,0o600);
 console.log('PASS Codex context: native last/capacity, cached tokens included, scoped cache/model, stale reopen, compact ACK versus completion, duplicates, rejection, timeout, disconnect and matching-history recovery');
})().catch(e=>{console.error(e);process.exitCode=1;});
