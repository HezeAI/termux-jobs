'use strict';
// Read-only Claude JSONL adaptation to the existing chat-snapshot cursor protocol.
// Keep one bounded page, never the whole transcript, in memory.
const fs=require('node:fs'),path=require('node:path');
const MAX_RECORD=32*1024*1024,PAGE_BYTES=2*1024*1024,PAGE_ENTRIES=20;
function queued(e){const a=e?.attachment;return e?.type==='attachment'&&a?.type==='queued_command'&&a.humanTurn===true&&a.origin?.kind==='human'&&a.commandMode==='prompt'&&typeof a.prompt==='string'&&/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(a.source_uuid||'');}
const visible=e=>e&&(['user','assistant'].includes(e.type)||queued(e))&&typeof e.uuid==='string'&&!e.isSidechain&&!e.teamName&&!e.isMeta&&!e.parent_tool_use_id;
// Read-only projection: raw UUID walks ancestry, source_uuid owns UI identity.
// This record proves text persistence, never admission to the current model request.
function project(e){if(!queued(e))return e;const a=e.attachment;return {type:'user',uuid:a.source_uuid,parentUuid:e.parentUuid,sessionId:e.sessionId,timestamp:Number.isFinite(Date.parse(a.timestamp))?a.timestamp:e.timestamp,message:{role:'user',content:a.prompt},nativeQueueRecord:e.uuid};}
function locate(config,session){
 if(!/^[0-9a-f-]{36}$/i.test(session))return null;
 const root=path.join(config,'projects');let real;try{real=fs.realpathSync(root);}catch{return null;}
 const dirs=fs.readdirSync(root,{withFileTypes:true});
 for(const dir of dirs){if(!dir.isDirectory())continue;try{const file=fs.realpathSync(path.join(root,dir.name,session+'.jsonl'));if(file.startsWith(real+path.sep)&&fs.statSync(file).isFile())return file;}catch{}}
 return null;
}
function* reverse(fd,end){
 let pos=end,parts=[],bytes=0;
 while(pos>0){const n=Math.min(65536,pos);pos-=n;const buffer=Buffer.alloc(n);if(fs.readSync(fd,buffer,0,n,pos)!==n)throw Error('HISTORY_CHANGED');let stop=n;
  for(let i=n-1;i>=0;i--)if(buffer[i]===10){const part=buffer.subarray(i+1,stop);parts.unshift(part);bytes+=part.length;if(bytes>MAX_RECORD)throw Error('HISTORY_RECORD_TOO_LARGE');if(bytes)yield {at:pos+i+1,line:Buffer.concat(parts,bytes).toString('utf8')};parts=[];bytes=0;stop=i;}
  if(stop){parts.unshift(buffer.subarray(0,stop));bytes+=stop;if(bytes>MAX_RECORD)throw Error('HISTORY_RECORD_TOO_LARGE');}
 }
 if(bytes)yield {at:0,line:Buffer.concat(parts,bytes).toString('utf8')};
}
class ClaudeHistory {
 constructor(config,session,strip=v=>v){this.config=config;this.session=session;this.strip=strip;}
 token(state){return 'claude-history:'+Buffer.from(JSON.stringify(state)).toString('base64url');}
 parse(cursor){if(typeof cursor!=='string'||cursor.length>4096||!cursor.startsWith('claude-history:'))throw Error('HISTORY_CURSOR_EXPIRED');let c;try{c=JSON.parse(Buffer.from(cursor.slice(15),'base64url').toString('utf8'));}catch{throw Error('HISTORY_CURSOR_EXPIRED');}
  if(c.session!==this.session||!Number.isSafeInteger(c.end)||c.end<0||!(c.want===null||typeof c.want==='string'&&c.want.length<=256)||c.before!==undefined&&typeof c.before!=='string')throw Error('HISTORY_CURSOR_EXPIRED');return c;}
 page(cursor){
  const file=locate(this.config,this.session);if(!file)return {entries:[],older:'',anchor:null};
  const fd=fs.openSync(file,fs.constants.O_RDONLY|fs.constants.O_NOFOLLOW);
  try{const stat=fs.fstatSync(fd);if(!stat.isFile())throw Error('HISTORY_UNAVAILABLE');const identity=stat.dev+':'+stat.ino;
   const state=cursor?this.parse(cursor):{session:this.session,file:identity,end:stat.size,want:null};
   if(state.file!==identity||state.end>stat.size)throw Error('HISTORY_CURSOR_EXPIRED');
   if(!cursor&&this.head?.stamp===identity+':'+stat.size+':'+stat.mtimeMs)return this.head.page;
   const anchor={...state};delete anchor.before;let want=state.want,started=want!==null,bytes=0,older='',entries=[];
   for(const row of reverse(fd,state.end)){
    let e;try{e=JSON.parse(row.line);}catch{continue;}if(e.sessionId&&e.sessionId!==this.session)continue;
    if(!started){if(!visible(e))continue;started=true;want=e.uuid;}
    if(e.uuid!==want)continue;
    want=typeof e.parentUuid==='string'?e.parentUuid:typeof e.logicalParentUuid==='string'?e.logicalParentUuid:null;
    if(visible(e)){const entry=this.strip(project(e));entries.push(entry);bytes+=Buffer.byteLength(JSON.stringify(entry));}
    if(!want)break;
    if(entries.length>=PAGE_ENTRIES||bytes>=PAGE_BYTES){older=this.token({session:this.session,file:identity,end:row.at,want});break;}
   }
   const page={entries:entries.reverse(),older,anchor};if(!cursor)this.head={stamp:identity+':'+stat.size+':'+stat.mtimeMs,page};return page;
  }finally{fs.closeSync(fd);}
 }
}
module.exports={ClaudeHistory,reverse,locate,visible,MAX_RECORD,PAGE_BYTES,PAGE_ENTRIES,project,queued};
