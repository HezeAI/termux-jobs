'use strict';
// Structured projection of native committed React state. Never parses terminal output.
const crypto=require('node:crypto');
const {clean}=require('./agent-receipts.cjs');
const {Attachments,attachmentRows}=require('./agent-attachments.cjs');
const text=v=>typeof v==='string'?clean(v.slice(0,200000)):'';
const hash=v=>'msg_g'+crypto.createHash('sha256').update(String(v)).digest('hex').slice(0,24);
function display(value,depth=0,budget={left:200000,nodes:10000}){
 if(depth>6||--budget.nodes<0||budget.left<=0)return '';if(typeof value==='string'){const s=text(value).slice(0,budget.left);budget.left-=s.length;return s;}
 if(Array.isArray(value)){const separator=Array.isArray(value[0])?'\n':value.some(v=>v&&typeof v==='object'&&!Array.isArray(v)&&typeof v.text==='string')?'':'\n';const out=[];for(const v of value.slice(0,2000)){if(budget.left<=0||budget.nodes<=0)break;out.push(display(v,depth+1,budget));}return out.join(separator);}
 if(!value||typeof value!=='object')return '';if(typeof value.text==='string')return display(value.text,depth+1,budget);
 return ['fileName','originalContent','newContent','message'].filter(k=>typeof value[k]==='string').map(k=>k+': '+display(value[k],depth+1,budget)).join('\n');
}

class GeminiChat {
 constructor(api){this.api=api;this.attachments=new Attachments(api.attachmentRoot||__dirname);this.binding=null;this.shell=null;this.groups=[];this.full=new Map();this.sent=new Map();this.thoughts=new Map();this.revision=0;this.native=false;}
 observe(config,data,submit,shellMode,setShellMode){
  const session=this.api.session();if(this.sessionKey!==session){this.sessionKey=session;this.thoughts.clear();}
  const binding={config,submit,shellMode,setShellMode,ready:data.ready===true,native:data.native===true};this.binding=binding;this.revision++;
  this.source=data;this.dirty=true;
  return ()=>{if(this.binding===binding)this.binding=null;};
 }
 invalidate(){this.dirty=true;}
 rebuild(){
  if(!this.dirty||!this.source)return;this.dirty=false;const data=this.source;
  const history=Array.isArray(data.history)?data.history.slice(-500):[],pending=Array.isArray(data.pending)?data.pending.slice(0,25):[];
  const lastUser=history.findLast(m=>m.type==='user'||m.type==='user_shell');const anchor=lastUser?hash(lastUser.id+':'+lastUser.type):'';
  const thought=[text(data.thought?.subject),text(data.thought?.description)].filter(Boolean).join('\n');if(anchor&&history.slice(history.lastIndexOf(lastUser)+1).some(m=>m.type==='thinking'))this.thoughts.delete(anchor);else if(anchor&&thought)this.thoughts.set(anchor,thought.slice(0,12000));while(this.thoughts.size>100)this.thoughts.delete(this.thoughts.keys().next().value);
  this.groups=[];this.full.clear();let remaining=2*1024*1024;
  const project=(item,index,live=false)=>{
   if(!item||typeof item!=='object')return;const message=live?hash('pending:'+item.type+':'+index):hash(item.id+':'+item.type);const rows=[];
   const row=(part,kind,body,title='',meta={})=>{body=text(body).slice(0,Math.max(0,Math.min(200000,remaining)));remaining-=body.length;if(!body&&kind!=='tool')return;const key=message+':'+part;this.full.set(key,body);rows.push({key,message,part,kind,...meta,title:text(title).slice(0,160),body:body.slice(0,12000),more:body.length>12000,...(live?{queued:true}:{})});};
   if(item.type==='user'){const projected=this.attachments.project(item.text,this.api.session());if(projected.text)row('user','user',projected.text);rows.push(...attachmentRows(message,projected.files));}else if(item.type==='user_shell')row('shell-input','user','Shell: '+text(item.text));
   else if(['gemini','gemini_content'].includes(item.type)){row('text','text',item.text);rows.push(...attachmentRows(message,this.attachments.output(item.text)));}
   else if(item.type==='thinking')row('thought','reasoning',[text(item.thought?.subject),text(item.thought?.description)].filter(Boolean).join('\n'));
   else if(item.type==='tool_group')for(const tool of (item.tools||[]).slice(0,100)){
    const id=typeof tool.callId==='string'?tool.callId.slice(0,160):'';if(!id)continue;const receipt=this.api.receipt(id);if(receipt)row('receipt:'+id,'receipt',receipt.body,receipt.label);
    row('tool:'+id,'tool',[text(tool.description),display(tool.resultDisplay)].filter(Boolean).join('\n\n'),text(tool.name)+' · '+text(tool.status),{toolName:text(tool.name),toolType:text(tool.name),status:text(tool.status)});
   }
   else if(['info','error','warning','hint'].includes(item.type))row(item.type,item.type==='error'?'error':'info',item.text);
   if(!live&&this.thoughts.has(message))row('summary','reasoning',this.thoughts.get(message));
   if(rows.length)this.groups.push({id:message,rows});
  };
  // Native history is already owned by CLI. Bound the additional projection, not model context.
  const chosen=[];let estimate=2*1024*1024;for(let i=history.length-1;i>=0;i--){const m=history[i];const size=text(m.text).length+(m.tools||[]).slice(0,100).reduce((n,t)=>n+text(t.description).length+display(t.resultDisplay).length,0);if(size>estimate&&chosen.length)break;estimate-=Math.min(size,estimate);chosen.push(m);}const records=chosen.reverse().map((m,i)=>({m,i,live:false})).concat(pending.map((m,i)=>({m,i,live:true})));for(let i=records.length-1;i>=0;i--){const r=records[i];project(r.m,r.i,r.live);}this.groups.reverse();

 }

 bindShell(config,send){const record={config,send};this.shell=record;return ()=>{if(this.shell===record)this.shell=null;};}
 snapshot(q){
  this.rebuild();const s=this.api.status();if(!this.api.supported())return {ok:true,nativeChat:false};let end=this.groups.length;
  if(q.cursor){if(typeof q.cursor!=='string'||q.cursor.length>128)throw Error('Invalid cursor');end=this.groups.findIndex(g=>g.id===q.cursor);if(end<0)throw Error('Expired cursor');}
  const groups=this.groups.slice(Math.max(0,end-20),end);let rows=groups.flatMap(g=>g.rows);if(rows.length>200)rows=rows.slice(-200);
  let budget=60000;rows=rows.map((r,i)=>{const max=Math.min(12000,Math.max(100,Math.floor(budget/(rows.length-i))));budget-=Math.min(r.body.length,max);return {...r,body:r.body.slice(0,max),more:r.more||r.body.length>max};});
  return {ok:true,nativeChat:true,attachmentsSupported:true,authError:this.source?.authError===true,session:this.api.session(),nativeUiReady:!!this.binding,knownMessages:this.groups.map(g=>g.id),connected:!!this.binding&&s.available&&(this.binding.ready||this.binding.native),rows,older:end>20?groups[0]?.id||'':'',forms:[],permission:this.api.permission(),busy:s.state==='responding'||s.state==='waiting_for_confirmation',needsTerminal:!!this.binding?.native||!s.available||(s.state==='waiting_for_confirmation'&&!this.api.permission()),directShell:false,model:{providerID:'google',id:s.model||s.selectedModel||''}};
 }
 async command(q){
  if(q.op==='chat-snapshot')return this.snapshot(q);
  if(q.op==='terminal-open'||q.op==='terminal-close'){this.native=q.op==='terminal-open';return {ok:true};}
  if(q.op==='chat-detail'){
   this.rebuild();
   if(q.session!==this.api.session())throw Error('Stale session');
   if(typeof q.message!=='string'||typeof q.part!=='string'||q.message.length>128||q.part.length>256)throw Error('Invalid detail');const body=this.full.get(q.message+':'+q.part);if(body===undefined)throw Error('Missing detail');const offset=Number.isSafeInteger(q.offset)&&q.offset>=0?q.offset:0;return {ok:true,body:body.slice(offset,offset+24000),next:offset+24000<body.length?offset+24000:null};
  }
  if(q.op!=='chat-send')throw Error('Unsupported chat operation');
  if(typeof q.id!=='string'||!/^msg_[A-Za-z0-9_-]{1,124}$/.test(q.id)||typeof q.text!=='string'||(!q.text.trim()&&!(Array.isArray(q.attachments)&&q.attachments.length))||Buffer.byteLength(q.text)>32000||!['message','shell'].includes(q.kind))throw Error('Invalid message');
  if(!this.api.session()||q.session!==this.api.session())return {ok:false,error:'STALE_SESSION'};
  const session=this.api.session(),raw=JSON.stringify([session,q.kind,q.text,q.attachments||[]]);let previous=this.sent.get(q.id);if(previous){if(previous.raw!==raw)throw Error('Changed message ID');return previous.promise;}
  let binding=this.binding;const s=this.api.status();if(this.native||!this.api.supported()||!s.available||!binding?.ready||binding.native||this.api.permission())return {ok:false,error:'NOT_READY'};
  if(q.kind==='shell'&&(s.state!=='idle'||this.shell?.config!==binding.config||binding.config.isTrustedFolder?.()!==true))return {ok:false,error:'NOT_READY'};
  // The native chat composer must never inherit the terminal's shell-input mode.
  if(q.kind==='message'&&binding.shellMode){binding.setShellMode(false);const started=Date.now();while(this.binding?.config===binding.config&&this.binding.shellMode&&Date.now()-started<2000)await new Promise(r=>setTimeout(r,20));binding=this.binding;if(!binding||binding.shellMode||!binding.ready||binding.native)return {ok:false,error:'NOT_READY'};}
  previous=this.sent.get(q.id);if(previous){if(previous.raw!==raw)throw Error('Changed message ID');return previous.promise;}
  const current=this.binding;if(current?.config!==binding.config||!current.ready||current.native)return {ok:false,error:'NOT_READY'};
  const files=this.attachments.prepare(q.attachments,session,q.kind),prompt=this.attachments.prompt(q.text,files,true);
  // Reserve before invoking native callbacks; ambiguous execution is never blindly retried.
  const promise=Promise.resolve().then(async()=>{
   // No native callback has run at these guards. Keep same-ID retry possible
   // after a temporary UI/idle transition; uncertain native calls stay reserved.
   const notReady=()=>{if(this.sent.get(q.id)?.promise===promise)this.sent.delete(q.id);return {ok:false,error:'NOT_READY'};};
   if(this.api.session()!==session||this.binding?.config!==binding.config||!this.binding.ready||this.binding.native)return notReady();
   if(q.kind==='shell'){if(this.shell?.config!==binding.config||this.api.status().state!=='idle')return notReady();if(!this.shell.send(q.text))throw Error('Shell rejected');}else await this.binding.submit(prompt);
   return {ok:true,accepted:true,id:q.id};
  });
  this.sent.set(q.id,{raw,promise});while(this.sent.size>200)this.sent.delete(this.sent.keys().next().value);return promise;
 }
}
module.exports={GeminiChat};
