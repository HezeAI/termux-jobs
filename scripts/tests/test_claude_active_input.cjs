'use strict';
// Native adapter with synthetic events. Real process and signal APIs are blocked.
const assert=require('node:assert/strict'),path=require('node:path'),fs=require('node:fs'),cp=require('node:child_process');
for(const name of ['spawn','spawnSync','exec','execFile','execSync','execFileSync','fork'])cp[name]=()=>{throw Error('REAL PROCESS BLOCKED');};process.kill=()=>{throw Error('REAL SIGNAL BLOCKED');};
const root=require('./fixture-paths.cjs').fixture('clact');process.env.CLAUDE_CONFIG_DIR=path.join(root,'isolated-config');
const {ClaudeChat}=require('../../app/src/main/assets/claude-chat.cjs');
const session='11111111-1111-4111-8111-111111111111',b=new ClaudeChat({socket:path.join(root,'s.sock'),model:'sonnet',mode:'manual',chat:'new',thread:session,language:'en'});b.ready=true;b.auth=true;b.workerKind='sdk';b.capabilities=new Set(['msg_lifecycle_v1','interrupt_cancel_queued_v1']);let writes=[];b.write=q=>writes.push(q);
const q=(id,text)=>({op:'chat-send',session,id:'msg_'+id,text,kind:'message'});
function lifecycle(input,state){b.event({type:'command_lifecycle',session_id:session,uuid:'frame-'+state,command_uuid:input.uuid,state});}
(async()=>{
 let settled=false;const first=b.command(q('one','first')).then(r=>{settled=true;return r;});await Promise.resolve();assert(!settled);assert.equal(writes.length,1);b.event({type:'command_lifecycle',session_id:'foreign',command_uuid:writes[0].uuid,state:'queued'});await Promise.resolve();assert(!settled);lifecycle(writes[0],'queued');assert.equal((await first).delivery,'queued');assert(b.snapshot().rows.some(r=>r.kind==='user'&&r.queued));lifecycle(writes[0],'started');assert(!b.snapshot().rows.some(r=>r.queued));
 b.busy=true;b.event({type:'control_request',request_id:'permission',request:{subtype:'can_use_tool',tool_name:'Bash',input:{command:'never executed'},tool_use_id:'tool'}});const permission=b.snapshot().permission;
 const twoQ=q('two','second'),threeQ=q('three','third'),two=b.command(twoQ),same=b.command(twoQ),three=b.command(threeQ);assert.equal(writes.length,3);lifecycle(writes[1],'queued');lifecycle(writes[2],'queued');assert.equal((await two).delivery,'queued');assert((await same).replayed);assert.equal((await three).delivery,'queued');assert.deepEqual(writes.slice(1).map(m=>m.message.content),['second','third']);assert.equal(b.snapshot().permission.nonce,permission.nonce);assert.equal(b.mode,'manual');
 // A merged replay must not show the second message twice.
 lifecycle(writes[1],'started');lifecycle(writes[2],'started');b.event({...writes[2],isReplay:true,message:{role:'user',content:'second\nthird'}});let users=b.snapshot().rows.filter(r=>r.kind==='user').map(r=>r.body);assert.equal(users.filter(t=>t==='second').length,1);assert.equal(users.filter(t=>t==='third').length,1);assert(!users.includes('second\nthird'));
 // Completion and admission can cross without discarding either message.
 b.event({type:'result',session_id:session,is_error:false});const four=b.command(q('four','finish-race'));lifecycle(writes[3],'queued');assert((await four).accepted);
 // Explicit cancel consumes the native receipt. Same-ID retry never writes again.
 b.control=async(subtype,params)=>{assert.equal(subtype,'interrupt');assert.equal(params.cancel_queued,true);return {still_queued:[],cancelled:[writes[3].uuid]};};await b.command({op:'chat-interrupt',session});const before=writes.length,cancelled=await b.command(q('four','finish-race'));assert.equal(cancelled.error,'CANCELLED');assert.equal(writes.length,before);lifecycle(writes[3],'queued');assert(b.snapshot().rows.some(r=>r.delivery==='cancelled'));
 // A transport gap retains admission; reconnect only reads its cached result.
 const cached=await b.command(twoQ);assert(cached.accepted&&cached.replayed);assert.equal(writes.length,before);
 // Late acknowledgement after an ambiguous result recovers without re-sending.
 const lateQ=q('late','late'),late=b.command(lateQ),record=b.sent.get(lateQ.id);record.state='unconfirmed';record.finish({ok:false,delivery:'unconfirmed',error:'SEND_UNCONFIRMED'});assert.equal((await late).delivery,'unconfirmed');lifecycle(writes.at(-1),'queued');const count=writes.length;assert((await b.command(lateQ)).accepted);assert.equal(writes.length,count);
 // Restart/worker loss never silently replays an ambiguous write.
 const lostQ=q('lost','lost'),lost=b.command(lostQ);b.clearWorker();assert.equal((await lost).delivery,'unconfirmed');const lostCount=writes.length;b.ready=true;b.auth=true;b.workerKind='sdk';assert.equal((await b.command(lostQ)).delivery,'unconfirmed');assert.equal(writes.length,lostCount);
 const shell=await b.command({...q('shell','false'),kind:'shell'});assert.equal(shell.delivery,'rejected');assert.equal(writes.length,lostCount);
 b.clearWorker();console.log('PASS Claude active input: native lifecycle ACK, tool/response order, merged replay projection, finish race, native queued cancel receipt, cached reconnect, late ACK, worker-loss dedup; no actual process or signals');
})().catch(e=>{b.clearWorker();console.error(e);process.exitCode=1;});
