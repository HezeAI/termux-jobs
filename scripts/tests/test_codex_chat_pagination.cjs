'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../..'),base=path.join(root,'.verification/chat-history-v1.7.5');fs.mkdirSync(base,{recursive:true});const dir=fs.mkdtempSync(path.join(base,'paging-'));
for(const [from,to]of [['codex-chat.cjs','codex-chat.cjs'],['codex-control.js','codex-wire-v0.2.22.js'],['agent-receipts.cjs','agent-receipts.cjs']])fs.copyFileSync(path.join(root,'app/src/main/assets',from),path.join(dir,to));
const {CodexChat}=require(path.join(dir,'codex-chat.cjs'));
const item=(prefix,n)=>({id:prefix+n,type:'agentMessage',text:prefix+' message '+n});
(async()=>{
 let requests=0;
 const rpc={async call(method,q){requests++;assert.equal(method,'thread/turns/list');assert.equal(q.cursor,'native-page');return {data:[{id:'ancient-turn',items:Array.from({length:1200},(_,n)=>item('ancient',n))}],nextCursor:null};}};
 const c=new CodexChat(rpc,{endpoint:path.join(dir,'control'),cwd:dir});c.thread='thread';c.ready=true;c.older='native-page';
 for(let n=0;n<250;n++)c.put('recent-turn',item('recent',n));
 let page=await c.snapshot(),cursor=page.older;assert.equal(page.rows.length,100);assert(page.partialHistory);assert(cursor.startsWith('jobs-history:'));const bodies=page.rows.map(r=>r.body);
 for(let i=0;i<4&&cursor!=='native-page';i++){page=await c.snapshot({cursor});assert.equal(page.rows.at(-1).body.includes('recent'),true);bodies.unshift(...page.rows.map(r=>r.body));cursor=page.older;}
 assert.equal(cursor,'native-page');assert.equal(bodies.length,250);assert.equal(new Set(bodies).size,250);assert.equal(requests,0);
 const old=[];page=await c.snapshot({cursor});cursor=page.older;old.unshift(...page.rows.map(r=>r.body));assert(page.rows.every(r=>r.body.startsWith('ancient')));assert.equal(requests,1);
 for(let i=0;i<20&&cursor;i++){
  const latest=await c.snapshot();assert(latest.rows.every(r=>r.body.startsWith('recent'))); // polling never replaces the old-page cursor
  page=await c.snapshot({cursor});cursor=page.older;old.unshift(...page.rows.map(r=>r.body));
 }
 assert.equal(cursor,null);assert.equal(old.length,1200);assert.equal(new Set(old).size,1200);assert.equal(requests,1);assert.equal(c.older,'native-page');assert.equal(old[0],'ancient message 0');assert.equal(old.at(-1),'ancient message 1199');
 assert(c.historyWindows.size<=8);assert.equal((await c.snapshot({cursor:'jobs-history:missing'})).historyReset,true);await assert.rejects(c.snapshot({cursor:'x'.repeat(4097)}),/not ready/);
 let headCalls=0;
 const headRpc={async call(method,q){headCalls++;assert.equal(method,'thread/turns/list');assert.equal(q.cursor,undefined);return {data:[{id:'oversized-turn',items:Array.from({length:1200},(_,n)=>item('head',n))}],nextCursor:null};}};
 const head=new CodexChat(headRpc,{endpoint:path.join(dir,'head'),cwd:dir});head.thread='thread-head';head.ready=true;await head.history();assert(head.droppedLiveItems);
 let headPage=await head.snapshot(),headCursor=headPage.older;const allHead=new Set(headPage.rows.map(r=>r.body));
 for(let i=0;i<40&&headCursor;i++){
  await head.snapshot();headPage=await head.snapshot({cursor:headCursor});assert(!headPage.historyReset);headCursor=headPage.older;for(const r of headPage.rows)allHead.add(r.body);
 }
 assert.equal(headCursor,null);assert.equal(allHead.size,1200);assert.equal(headCalls,2);
 console.log('PASS Codex pagination: 250 current items and 1200 native historical items across pages; full old page survives live-buffer bounds; polling does not advance frontier; 1200-item initial turn recovered after buffer trimming; expired cursor refresh; oversized cursor refusal; bounded window count');
})().catch(e=>{console.error(e);process.exitCode=1;});
