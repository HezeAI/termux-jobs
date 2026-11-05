'use strict';
// Bounded scalar checkpoint, separate from history and authorization. No polling or inference.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const count=v=>Number.isSafeInteger(v)&&v>=0?v:null;
class AgentContext {
 constructor(root,agent,session){this.agent=agent;this.session=session;this.usage=null;this.stale=true;this.file=root&&session?path.join(root,'context-usage',crypto.createHash('sha256').update(agent+'\0'+session).digest('hex')+'.json'):'';let fd;
  try{fd=fs.openSync(this.file,fs.constants.O_RDONLY|fs.constants.O_NOFOLLOW);const s=fs.fstatSync(fd);if(!s.isFile()||s.size>4096)return;const v=JSON.parse(fs.readFileSync(fd,'utf8'));if(v.schema!==1||v.agent!==agent||v.session!==session)return;const u=v.usage;if(u&&count(u.used)!==null&&typeof u.model==='string'&&u.model.length<=160&&count(u.updated)!==null)this.usage={used:u.used,capacity:count(u.capacity)>0?u.capacity:null,model:u.model,updated:u.updated,source:typeof u.source==='string'?u.source.slice(0,40):''};}catch{}finally{if(fd!==undefined)fs.closeSync(fd);}
 }
 accept(used,capacity,model,source){if(count(used)===null||typeof model!=='string'||!model||model.length>160){this.markStale();return;}this.usage={used,capacity:count(capacity)>0?capacity:null,model,source,updated:Date.now()};this.stale=false;this.save();}
 capacity(value,model){if(this.usage?.model===model&&count(value)>0){this.usage.capacity=value;this.save();}}
 markStale(){this.stale=true;}
 clear(){this.usage=null;this.stale=true;this.save();}
 snapshot(){return {session:this.session,usage:this.usage,stale:this.stale};}
 save(){if(!this.file)return;const temporary=this.file+'.tmp-'+crypto.randomUUID();try{fs.mkdirSync(path.dirname(this.file),{recursive:true,mode:0o700});const data=JSON.stringify({schema:1,agent:this.agent,session:this.session,usage:this.usage});if(Buffer.byteLength(data)>4096)return;fs.writeFileSync(temporary,data,{flag:'wx',mode:0o600});fs.renameSync(temporary,this.file);}catch{}finally{try{fs.unlinkSync(temporary);}catch{}}}
}
module.exports={AgentContext,count};
