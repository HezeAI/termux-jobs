'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {ClaudeChat}=require('../../app/src/main/assets/claude-chat.cjs');
const {ClaudeHistory}=require('../../app/src/main/assets/claude-history.cjs');
const root=require('./fixture-paths.cjs').fixture('clhis'),config=path.join(root,'config'),session='11111111-1111-4111-8111-111111111111';
process.env.CLAUDE_CONFIG_DIR=config;const dir=path.join(config,'projects','fixture');fs.mkdirSync(dir,{recursive:true});const file=path.join(dir,session+'.jsonl');
let parent=null,serial=0;const records=[];
function add(type,content,extra={}){const uuid='record-'+(++serial),entry={type,uuid,parentUuid:parent,sessionId:session,timestamp:new Date(1700000000000+serial*1000).toISOString(),message:{id:'native-'+serial,content},...extra};records.push(entry);parent=uuid;return entry;}
for(let i=0;i<620;i++)add(i%2?'assistant':'user',[{type:'text',text:'ROW-'+i}]);
const long='Длинный ответ 🚀 '.repeat(22000)+'THE-END';const answer=add('assistant',[{type:'text',text:long}]);
const dense=add('assistant',Array.from({length:680},(_,i)=>({type:'tool_use',id:'tool-'+i,name:'Bash',input:{command:'printf '+i}})));
const results=add('user',Array.from({length:680},(_,i)=>({type:'tool_result',tool_use_id:'tool-'+i,content:[{type:'text',text:'RESULT-'+i}]})));
for(let i=0;i<55;i++){add('user',[{type:'text',text:'TAIL-'+i}]);for(let j=0;j<150;j++)add('progress',[],{});}
const mainParent=parent;add('assistant',[{type:'text',text:'DO-NOT-SHOW-SIDECHAIN'}],{isSidechain:true});parent=mainParent;
fs.writeFileSync(file,records.map(e=>JSON.stringify(e)).join('\n')+'\n');
const checksum=()=>crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex'),before=checksum();
function backend(){const b=new ClaudeChat({socket:path.join(root,crypto.randomUUID()+'.sock'),binary:'/unused',model:'sonnet',mode:'manual',chat:'id',thread:session,language:'en'});b.auth=b.ready=true;b.workerKind='sdk';return b;}
function collect(b){let page=b.snapshot(),rows=[],seen=new Set(),pages=0;do{assert(page.partialHistory);assert(page.rows.length<=200);assert(Buffer.byteLength(JSON.stringify(page))<524288);for(const row of page.rows){assert(!seen.has(row.key),'duplicate page row '+row.key);seen.add(row.key);rows.push(row);}if(!page.older)break;const cursor=page.older;const replay=b.snapshot({cursor});assert.deepEqual(b.snapshot({cursor}).rows,replay.rows,'cursor replay is idempotent');page=replay;assert(++pages<250);}while(true);return {rows,pages};}
(async()=>{
 const b=backend();b.busy=true;const loaded=collect(b);assert(!loaded.rows.some(r=>r.kind==='tool'&&r.status==='inProgress'));b.busy=false;assert(loaded.pages>30);assert.equal(loaded.rows.filter(r=>/^ROW-\d+$/.test(r.body)).length,620);assert.equal(loaded.rows.filter(r=>/^TAIL-\d+$/.test(r.body)).length,55);
 assert.equal(loaded.rows.filter(r=>r.part.startsWith('tool:')).length,680);assert.equal(loaded.rows.filter(r=>r.part.startsWith('result:')).length,680);assert(!JSON.stringify(loaded.rows).includes('DO-NOT-SHOW-SIDECHAIN'));
 const row=loaded.rows.find(r=>r.bodyLength===long.length);assert(row?.more);let text='',offset=0;do{const r=await b.command({op:'chat-detail',session,message:row.message,part:row.part,offset});text+=r.body;if(r.next===null)break;offset=r.next;}while(true);assert.equal(text,long);
 assert.equal(checksum(),before,'reader must not modify native history');
 // Append a partial record while a historical cursor is held; complete it later.
 const held=b.snapshot().older;const fresh=add('assistant',[{type:'text',text:'AFTER-RESUME'}]);const line=JSON.stringify(fresh);fs.appendFileSync(file,line.slice(0,80));assert(!JSON.stringify(b.snapshot()).includes('AFTER-RESUME'));fs.appendFileSync(file,line.slice(80)+'\n');assert(b.snapshot().rows.some(r=>r.body==='AFTER-RESUME'));assert(!b.snapshot({cursor:held}).historyReset);
 const restarted=backend();const again=collect(restarted);assert.equal(again.rows.length,loaded.rows.length+1);assert(again.rows.some(r=>r.body==='AFTER-RESUME'));
 // One native response containing >200 rows preserves every row through its cursor.
 const huge=add('assistant',Array.from({length:520},(_,i)=>({type:'text',text:'PART-'+i})));fs.appendFileSync(file,JSON.stringify(huge)+'\n');const head=restarted.snapshot();assert(head.partialMessages.length>0);assert(head.rows.length<=200);const hugeAll=collect(restarted);assert.equal(hugeAll.rows.filter(r=>/^PART-/.test(r.body)).length,520);
 // Native message IDs may repeat across distinct UUID/block records.
 const one=add('assistant',[{type:'text',text:'SAME-ID-ONE'}]);const two=add('assistant',[{type:'text',text:'SAME-ID-TWO'}]);two.message.id=one.message.id;fs.appendFileSync(file,JSON.stringify(one)+'\n'+JSON.stringify(two)+'\n');const last=restarted.snapshot();assert(last.rows.some(r=>r.body==='SAME-ID-ONE'));assert(last.rows.some(r=>r.body==='SAME-ID-TWO'));
 const boundary=add('system',[],{parentUuid:null,logicalParentUuid:parent,subtype:'compact_boundary'});const afterCompact=add('assistant',[{type:'text',text:'AFTER-COMPACT'}]);fs.appendFileSync(file,JSON.stringify(boundary)+'\n'+JSON.stringify(afterCompact)+'\n');const compacted=collect(restarted);assert(compacted.rows.some(r=>r.body==='ROW-0'));assert(compacted.rows.some(r=>r.body==='AFTER-COMPACT'));
 // Truncated/replaced files invalidate their old cursor instead of silently returning no history.
 const oldCursor=restarted.snapshot().older;fs.writeFileSync(file,JSON.stringify({...fresh,parentUuid:null})+'\n');assert(restarted.snapshot({cursor:oldCursor}).historyReset);
 assert.deepEqual(new ClaudeHistory(config,'../../outside').page().entries,[]);
 // Read past the former64MiB tail cap without retaining all response bodies.
 const largeSession='22222222-2222-4222-8222-222222222222',largeFile=path.join(dir,largeSession+'.jsonl');const fd=fs.openSync(largeFile,'w');let previous=null;try{for(let i=0;i<23;i++){const uuid='large-'+i;fs.writeSync(fd,JSON.stringify({type:'assistant',uuid,parentUuid:previous,sessionId:largeSession,message:{content:[{type:'text',text:'LARGE-'+i+' '+'.'.repeat(3*1024*1024)}]}})+'\n');previous=uuid;}}finally{fs.closeSync(fd);}const reader=new ClaudeHistory(config,largeSession);let cursor='',count=0;do{const page=reader.page(cursor);count+=page.entries.length;assert(page.entries.length<=1);cursor=page.older;}while(cursor);assert.equal(count,23);assert(fs.statSync(largeFile).size>64*1024*1024);
 console.log('PASS Claude history: >8000 records/>500 messages, bounded pages and wire, 680tools/results, >200rows in one response, full Unicode answer, append/partial write, restart, stable UUIDs, cursor replay/reset and read-only checksum');
})().catch(e=>{console.error(e);process.exitCode=1;});
