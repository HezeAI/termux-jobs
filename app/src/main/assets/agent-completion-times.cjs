'use strict';
// One immutable summary per completed request. Separate from native chat history.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
function instant(v){const n=typeof v==='number'?v:typeof v==='string'&&/^\d{4}-\d\d-\d\dT/.test(v)?Date.parse(v):NaN;const ms=typeof v==='number'&&n>0&&n<1e11?n*1000:n;return Number.isSafeInteger(ms)&&ms>0&&ms<=8640000000000000?ms:0;}
function duration(v){return Number.isSafeInteger(v)&&v>=0?v:null;}
class CompletionTimes {
 constructor(root,agent,session){
  this.values=new Map();this.messages=new Map();this.starts=new Map();this.failed=false;
  const name=crypto.createHash('sha256').update(agent+'\0'+String(session)).digest('hex');this.file=root&&session?path.join(root,'completion-times',name+'.jsonl'):'';
  if(this.file)try{const stat=fs.lstatSync(this.file);if(!stat.isFile()||stat.size>32*1024*1024){this.failed=true;return;}
   for(const line of fs.readFileSync(this.file,'utf8').split('\n'))try{const r=JSON.parse(line);if(typeof r.request==='string'&&r.request.length<=240&&typeof r.message==='string'&&r.message.length<=240&&instant(r.completedAt)&&!this.values.has(r.request)&&!this.messages.has(r.message)){this.values.set(r.request,r);this.messages.set(r.message,r);}}catch{}
  }catch(e){if(e.code!=='ENOENT')this.failed=true;}
 }
 start(request){if(request&&!this.starts.has(request)){this.starts.set(request,process.hrtime.bigint());while(this.starts.size>64)this.starts.delete(this.starts.keys().next().value);}}
 elapsed(request){const start=this.starts.get(request);return start===undefined?null:Number((process.hrtime.bigint()-start)/1000000n);}
 action(message){return this.summary('action:'+message)?.completedAt||0;}
 rememberAction(message,native=0,receivedAt=Date.now()){return this.finish('action:'+message,'action:'+message,{completedAt:native},true,receivedAt);}
 summary(message){return this.messages.get(message)||null;}
 finish(request,message,{completedAt=0,durationMs=null,startedAt=0}={},received=false,receivedAt=Date.now()){
  if(this.values.has(request))return this.values.get(request);if(this.messages.has(message))return this.messages.get(message);
  if(typeof request!=='string'||!request||request.length>240||typeof message!=='string'||!message||message.length>240||!this.file||this.failed)return null;
  const native=instant(completedAt),end=native||(received?instant(receivedAt):0);if(!end)return null;
  let span=duration(durationMs);if(span===null&&native&&instant(startedAt)&&native>=instant(startedAt))span=native-instant(startedAt);if(span===null&&received)span=this.elapsed(request);
  const row={request,message,completedAt:end,...(span!==null?{durationMs:span}:{}),source:native?'native':'received'};
  try{fs.mkdirSync(path.dirname(this.file),{recursive:true,mode:0o700});const fd=fs.openSync(this.file,fs.constants.O_WRONLY|fs.constants.O_APPEND|fs.constants.O_CREAT|fs.constants.O_NOFOLLOW,0o600);
   try{if(fs.fstatSync(fd).size>32*1024*1024){this.failed=true;return null;}fs.writeSync(fd,'\n'+JSON.stringify(row)+'\n');fs.fsyncSync(fd);}finally{fs.closeSync(fd);}
   this.values.set(request,row);this.messages.set(message,row);this.starts.delete(request);return row;
  }catch{this.failed=true;return null;}
 }
}
module.exports={CompletionTimes,instant,duration};
