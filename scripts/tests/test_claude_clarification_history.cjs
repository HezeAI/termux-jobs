'use strict';
const assert=require('node:assert/strict'),fs=require('fs'),path=require('path'),crypto=require('crypto');
const root=require('./fixture-paths.cjs').fixture('clmid'),session='11111111-1111-4111-8111-111111111111';process.env.CLAUDE_CONFIG_DIR=path.join(root,'config');process.env.HOME=path.join(root,'home');
const {ClaudeChat}=require('../../app/src/main/assets/claude-chat.cjs'),{ClaudeHistory}=require('../../app/src/main/assets/claude-history.cjs');
const dir=path.join(process.env.CLAUDE_CONFIG_DIR,'projects','synthetic');fs.mkdirSync(dir,{recursive:true});let parent=null,records=[],sources=[];
function add(record){record={parentUuid:parent,sessionId:session,...record};records.push(record);parent=record.uuid;return record;}
add({type:'user',uuid:'initial',timestamp:'2026-11-05T00:00:00.000Z',message:{role:'user',content:'SYNTHETIC FIRST'}});
for(let n=0;n<45;n++){
 const source=crypto.randomUUID();sources.push(source);add({type:'attachment',uuid:'native-queue-'+n,timestamp:'2026-11-05T00:02:00.000Z',attachment:{type:'queued_command',source_uuid:source,origin:{kind:'human'},commandMode:'prompt',humanTurn:true,timestamp:new Date(1793836801000+n).toISOString(),prompt:'IDENTICAL ARTIFICIAL TEXT'}});
}
add({type:'attachment',uuid:'nonhuman',attachment:{type:'queued_command',source_uuid:crypto.randomUUID(),origin:{kind:'system'},commandMode:'prompt',humanTurn:true,prompt:'DO NOT SHOW INTERNAL'}});
add({type:'attachment',uuid:'malformed',attachment:{type:'queued_command',source_uuid:'no-source-uuid',origin:{kind:'human'},commandMode:'prompt',humanTurn:true,prompt:'DO NOT GUESS ID'}});
add({type:'assistant',uuid:'final',timestamp:'2026-11-05T00:03:00.000Z',message:{role:'assistant',content:'FINAL ARTIFICIAL ANSWER'}});
const file=path.join(dir,session+'.jsonl');fs.writeFileSync(file,records.map(r=>JSON.stringify(r)).join('\n')+'\n');const hash=()=>crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex'),before=hash();
function backend(){return new ClaudeChat({socket:path.join(root,crypto.randomUUID()+'.sock'),binary:'/unused',model:'opus',mode:'manual',chat:'id',thread:session,language:'en'})}
function collect(b){let page=b.snapshot(),all=[],pages=0;do{all.push(...page.rows);if(!page.older)break;page=b.snapshot({cursor:page.older});assert(++pages<10)}while(true);return {all,pages}}
for(let repeat=0;repeat<3;repeat++){
 const {all,pages}=collect(backend());assert(pages>=2);const copies=all.filter(r=>r.kind==='user'&&r.body==='IDENTICAL ARTIFICIAL TEXT');assert.equal(copies.length,45);assert.equal(new Set(copies.map(r=>r.key)).size,45);assert(copies.every(r=>!r.deliveryReceipt&&!r.queued&&!r.delivery),'history record alone must not claim current admission');assert(!JSON.stringify(all).includes('DO NOT'));assert.equal(hash(),before);
 const reader=new ClaudeHistory(process.env.CLAUDE_CONFIG_DIR,session);let entries=[],cursor='';do{let p=reader.page(cursor);entries.unshift(...p.entries);cursor=p.older}while(cursor);for(const source of sources){let matches=entries.filter(e=>e.uuid===source);assert.equal(matches.length,1);assert(matches[0].timestamp<'2026-11-05T00:02:00.000Z');}
}
console.log('PASS read-only native queued_command projection: 45 identical texts/distinct UUIDs across bounded pages, one copy per source UUID on three cold opens, original timestamp, no delivery inference, malformed/internal omission, native SHA unchanged');
