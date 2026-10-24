'use strict';
const assert=require('node:assert/strict'),path=require('node:path');
const {OpenCodeChat}=require('../../app/src/main/assets/opencode-chat.cjs');
const root=require('./fixture-paths.cjs').fixture('ocrec');
(async()=>{
 const session='ses_current';let posts=0,deletes=0;
 const chat=new OpenCodeChat(async(method,route)=>{if(method==='POST')posts++;if(method==='DELETE')deletes++;return {data:{id:'sh_owned',status:'running',metadata:{sessionID:session}}};},session,path.join(root,'receipts.json'));
 const q={op:'chat-send',session:'ses_previous',id:'msg_delayed',text:'delayed ordinary message',kind:'message'};
 await assert.rejects(()=>chat.command(q),/Stale session/,'a delayed plain-text request from a replaced conversation must be rejected');assert.equal(posts,0);
 await assert.rejects(()=>chat.command({...q,session:undefined}),/Stale session/,'unscoped sends are not admitted by the updated controller');
 assert((await chat.command({...q,session})).accepted);assert.equal(posts,1);
 await assert.rejects(()=>chat.command(q),/Stale session/,'dedup must not return an old ACK to a foreign conversation');assert.equal(posts,1);
 await chat.command({...q,session});assert.equal(posts,1);
 await assert.rejects(()=>chat.command({op:'chat-detail',session:'ses_previous',message:'msg_x',part:'user'}),/Stale session/);
 chat.shells.add('sh_owned');await assert.rejects(()=>chat.command({op:'chat-interrupt',session:'ses_previous'}),/Stale session/);assert.equal(deletes,0);assert.equal(posts,1);
 await assert.rejects(()=>chat.command({op:'chat-form',session:'ses_previous',id:'frm_x'}),/Stale session/);
 assert((await chat.command({op:'chat-interrupt',session})).commandsStopped===1);assert.equal(deletes,1);
 chat.close();console.log('PASS OpenCode recovery: stale/unscoped plain sends rejected before admission/dedup, scoped retry once, stale detail/form/interrupt refused without stopping a command, owned interrupt preserved');
})().catch(e=>{console.error(e);process.exitCode=1;});
