'use strict';
// Structured view of one owned native session. Never parses terminal escape output.
const crypto=require('node:crypto');
const {Attachments,attachmentRows}=require('./agent-attachments.cjs');
const {ReceiptStore,clean}=require('./agent-receipts.cjs');
const id=v=>typeof v==='string'&&/^msg_[A-Za-z0-9_-]{1,124}$/.test(v);
const canonical=v=>JSON.stringify(v,(_,value)=>value&&typeof value==='object'&&!Array.isArray(value)?Object.fromEntries(Object.keys(value).sort().map(k=>[k,value[k]])):value);
const text=v=>typeof v==='string'?clean(v):'';
class OpenCodeChat {
 constructor(request,session,file,options={}){this.request=request;this.session=session;this.file=file;this.attachments=new Attachments(options.attachmentRoot||require("node:path").dirname(file));this.live=new Map();this.asked=new Map();this.pending=new Map();this.forms=new Map();this.connected=false;this.closed=false;this.lastMessage='';this.shells=new Set();this.endedShells=new Set();this.admissions=new Map();}
 event(e){
  const d=e.data||{};if(d.sessionID!==this.session && d.form?.sessionID!==this.session)return;
  if(id(d.assistantMessageID))this.lastMessage=d.assistantMessageID;
  if(e.type==='permission.asked')this.asked.set(d.id,{...d,anchorMessage:this.lastMessage});
  if(e.type==='permission.replied'){
   const p=this.asked.get(d.requestID);this.asked.delete(d.requestID);
   if(p)try{new ReceiptStore(this.file,'O').add(p.source?.id||p.id,d.reply==='once'?'proceed_once':d.reply==='always'?'proceed_always':'cancel',p.action,JSON.stringify(p));}catch{}
  }
  if(e.type==='session.shell.started'&&/^sh_[A-Za-z0-9_-]+$/.test(d.shell?.id||'')){this.shells.add(d.shell.id);this.admissions.get(String(e.id||'').replace(/^evt_/, 'msg_'))?.();}
  if(e.type==='session.shell.ended'){this.shells.delete(d.shell?.id);this.endedShells.add(d.shell?.id);while(this.endedShells.size>2000)this.endedShells.delete(this.endedShells.values().next().value);}
  const match=/^session\.(text|reasoning)\.(delta|ended)$/.exec(e.type);
  if(match&&id(d.assistantMessageID)){
   const key=d.assistantMessageID+':'+match[1]+':'+d.ordinal;
   const prior=this.live.get(key);const value=match[2]==='ended'?text(d.text):(prior?.text||'')+text(d.delta);
   this.live.set(key,{message:d.assistantMessageID,kind:match[1],ordinal:d.ordinal,text:value.slice(0,200000)});
   while(this.live.size>32)this.live.delete(this.live.keys().next().value);
  }
 }
 async messages(cursor){
  if(cursor!==undefined&&(typeof cursor!=='string'||cursor.length>4096))throw Error('Invalid cursor');
  const data=await this.request('GET','/api/session/'+this.session+'/message?limit=20'+(cursor?'&cursor='+encodeURIComponent(cursor):'&order=desc'));
  return {messages:(data.data||[]).slice().reverse(),older:data.cursor?.next||''};
 }
 rows(messages){
  const receipts=new ReceiptStore(this.file,'O');const rows=[];const unlinked=receipts.rows.map(r=>{try{return {row:r,anchor:JSON.parse(r.body).anchorMessage};}catch{return {row:r};}});const used=new Set();
  for(const m of messages){
   if(!id(m.id))continue;
   const push=(key,kind,body,title='',meta={})=>rows.push({...meta,key:m.id+':'+key,message:m.id,part:key,kind,title:text(title),body:text(body).slice(0,12000),more:text(body).length>12000});
   if(m.type==='user'){const projected=this.attachments.project(m.text,this.session);if(projected.text)push('user','user',projected.text);rows.push(...attachmentRows(m.id,projected.files));}
   else if(m.type==='assistant'){
    const ord={text:0,reasoning:0};
    for(const part of m.content||[]){
     if(part.type==='text'||part.type==='reasoning'){
      const key=part.type+':'+ord[part.type]++;const live=this.live.get(m.id+':'+key);
      const body=live&&live.text.length>(part.text||'').length?live.text:part.text;
      if(body){push(key,part.type,body);if(part.type==='text')rows.push(...attachmentRows(m.id,this.attachments.output(body),'output:'+key));}
     }else if(part.type==='tool'){
      const receipt=receipts.get(part.id);
      if(receipt){used.add(receipt.key);push('receipt:'+part.id,'receipt',receipt.body,receipts.label(receipt,false));}
      push('tool:'+part.id,'tool',JSON.stringify(part.state||{},null,2),part.name+' · '+(part.state?.status||''),{toolName:text(part.name),toolType:text(part.name),status:text(part.state?.status),toolPath:text(part.state?.input?.filePath||part.state?.input?.file_path||part.state?.input?.path).slice(0,2048),...(['bash','run_shell_command'].includes(part.name)&&typeof part.state?.input?.command==='string'?{actionType:'shell',command:text(part.state.input.command).slice(0,2000)}:{})});
     }
    }
    if(m.error)push('error','error',JSON.stringify(m.error,null,2));
   }else if(m.type==='shell')push('shell','tool',JSON.stringify(m,null,2),'Shell');
   else if(m.type==='synthetic'||m.type==='system'){
    const body=m.description||m.text||'';
    const shell=m.type==='synthetic'&&/^The following shell command was executed by the user:\s*<shell id="(sh_[A-Za-z0-9_-]+)"/.exec(body);
    // Native shell notifications are model context, not a second transcript entry.
    // Keep unmatched notices inspectable (including when their shell is on an older page).
    if(!shell||!messages.some(other=>other.type==='shell'&&other.shellID===shell[1]))push(m.type,shell?'tool':'info',body,shell?'Shell':'');
   }
   for(const item of unlinked)if(item.anchor===m.id&&!used.has(item.row.key)){used.add(item.row.key);push('receipt:'+item.row.key,'receipt',item.row.body,receipts.label(item.row,false));}
  }
  return rows;
 }
 async snapshot(q){
  const page=await this.messages(q.cursor);const meta=await this.request('GET','/api/session/'+this.session);const model=(meta.data||meta).model||{};const active=await this.request('GET','/api/session/active');
  const inbox=await this.request('GET','/api/session/'+this.session+'/inbox');
  if(!q.cursor&&page.messages.length)this.lastMessage=page.messages.at(-1).id;
  const permissions=await this.request('GET','/api/session/'+this.session+'/permission');
  for(const p of permissions.data||[])if(p.sessionID===this.session&&!this.asked.has(p.id))this.asked.set(p.id,{...p,anchorMessage:this.lastMessage});while(this.asked.size>200)this.asked.delete(this.asked.keys().next().value);
  const forms=await this.request('GET','/api/session/'+this.session+'/form');
  const pending=(forms.data||[]).filter(f=>f.sessionID===this.session&&(!f.state||f.state.status==='pending'));
  this.forms=new Map(pending.map(f=>[f.id,{raw:canonical(f),nonce:crypto.createHash('sha256').update(canonical(f)).digest('hex')} ]));
  for(const m of page.messages)if(m.type==='shell'&&m.status==='running'&&!this.endedShells.has(m.shellID)&&/^sh_[A-Za-z0-9_-]+$/.test(m.shellID||''))this.shells.add(m.shellID);
  const rows=this.rows(page.messages);if(!q.cursor)for(const pending of inbox.data||[])if(pending.type==='user'&&id(pending.id)&&!rows.some(r=>r.message===pending.id)){const projected=this.attachments.project(pending.payload?.text,this.session);if(projected.text)rows.push({key:pending.id+':user',message:pending.id,part:'user',kind:'user',body:text(projected.text).slice(0,12000),queued:true});rows.push(...attachmentRows(pending.id,projected.files).map(r=>({...r,queued:true})));}let budget=60000;for(const r of rows){const max=Math.max(100,Math.floor(budget/Math.max(1,rows.length)));if(r.body.length>max){r.body=r.body.slice(0,max);r.more=true;}}
  return {nativeChat:true,attachmentsSupported:true,session:this.session,model:{providerID:text(model.providerID).slice(0,256),id:text(model.id).slice(0,256),variant:text(model.variant).slice(0,80)},rows,older:page.older,needsTerminal:pending.some(f=>(f.fields||[]).some(field=>field.when||field.hidden||!['string','boolean','number','integer','multiselect'].includes(field.type)||(field.type==='multiselect'&&field.custom)))||(permissions.data||[]).some(p=>!['shell','edit','read','list','glob','grep','webfetch','websearch'].includes(p.action)),directShell:this.shells.size>0,busy:this.shells.size>0||!!active.data?.[this.session]||(inbox.data||[]).some(item=>item.type!=='synthetic'),connected:this.connected,forms:pending.map(f=>({id:f.id,title:text(f.title),fields:f.fields,nonce:this.forms.get(f.id).nonce}))};
 }
 async command(q){
  if(this.closed)throw Error('Closed chat');
  if(['chat-send','chat-detail','chat-interrupt','chat-form'].includes(q.op)&&q.session!==this.session)throw Error('Stale session');
  if(q.op==='chat-snapshot')return this.snapshot(q);
  if(q.op==='chat-detail'){
   if(!id(q.message))throw Error('Invalid message');
   const r=await this.request('GET','/api/session/'+this.session+'/message/'+q.message);
   const row=this.rows([r.data]).find(r=>r.part===q.part);if(!row)throw Error('Missing part');
   // Details are paged, so a large tool result never overflows the private UI socket.
   const body=this.fullBody(r.data,q.part);const offset=Number.isSafeInteger(q.offset)&&q.offset>=0?q.offset:0;
   return {body:text(body).slice(offset,offset+24000),next:offset+24000<text(body).length?offset+24000:null};
  }
  if(q.op==='chat-send'){
   if(!id(q.id)||typeof q.text!=='string'||(!q.text.trim()&&!(Array.isArray(q.attachments)&&q.attachments.length))||Buffer.byteLength(q.text)>32000||!['message','shell'].includes(q.kind))throw Error('Invalid message');
   const previous=this.pending.get(q.id);const raw=JSON.stringify([q.kind,q.text,q.attachments||[]]);
   if(previous){if(previous.raw!==raw)throw Error('Changed message');return previous.promise;}
   const files=this.attachments.prepare(q.attachments,this.session,q.kind);const prompt=this.attachments.prompt(q.text,files);
   const promise=q.kind==='shell'?this.startShell(q):this.request('POST','/api/session/'+this.session+'/prompt',{id:q.id,text:prompt,...(files.length?{files:this.attachments.openCode(files)}:{})}).then(()=>({accepted:true,id:q.id}));
   this.pending.set(q.id,{raw,promise});while(this.pending.size>200)this.pending.delete(this.pending.keys().next().value);
   // Retain failed/ambiguous admission too: retrying a direct shell must never execute twice.
   return promise;
  }
  if(q.op==='chat-interrupt'){
   let commandsStopped=0;
   for(const shell of this.shells){const r=await this.request('GET','/api/shell/'+shell),info=r.data||r;if(info.id!==shell||info.metadata?.sessionID!==this.session)throw Error('Foreign shell');if(info.status==='running'){await this.request('DELETE','/api/shell/'+shell);commandsStopped++;}this.shells.delete(shell);}
   const r=await this.request('POST','/api/session/'+this.session+'/interrupt?resume=false');return {interrupted:r.interrupted===true,commandsStopped};
  }
  if(q.op==='chat-form'){
   const captured=this.forms.get(q.id);if(!captured||captured.nonce!==q.nonce)throw Error('Stale form');
   const current=await this.request('GET','/api/session/'+this.session+'/form/'+encodeURIComponent(q.id));
   if(current.data?.state?.status!=='pending'||canonical(Object.fromEntries(Object.entries(current.data||{}).filter(([k])=>k!=='state')))!==captured.raw)throw Error('Changed form');
   if(q.cancel===true)await this.request('DELETE','/api/session/'+this.session+'/form/'+encodeURIComponent(q.id));
   else {if(!q.answer||typeof q.answer!=='object'||Array.isArray(q.answer))throw Error('Invalid answer');await this.request('POST','/api/session/'+this.session+'/form/'+encodeURIComponent(q.id)+'/reply',{answer:q.answer});}
   this.forms.delete(q.id);return {answered:true};
  }
  throw Error('Unsupported chat operation');
 }
 startShell(q){
  return new Promise((resolve,reject)=>{
   let done=false,poll,timer;
   const finish=error=>{if(done)return;done=true;clearTimeout(timer);clearTimeout(poll);this.admissions.delete(q.id);if(error)reject(error);else resolve({accepted:true,id:q.id});};
   this.admissions.set(q.id,()=>finish());timer=setTimeout(()=>finish(Error('Shell admission unconfirmed')),10000);
   this.request('POST','/api/session/'+this.session+'/shell',{id:q.id,command:q.text}).then(()=>finish(),error=>finish(error));
   const check=async()=>{if(done)return;if(this.closed){finish(Error('Closed chat'));return;}try{const r=await this.request('GET','/api/session/'+this.session+'/message/'+q.id);if((r.data||r).type==='shell')finish();}catch{}if(!done)poll=setTimeout(check,250);};poll=setTimeout(check,250);
  });
 }
 fullBody(m,part){
  if(part==='user')return m.text;
  if(part==='error')return JSON.stringify(m.error,null,2);
  if(part==='synthetic'||part==='system')return m.description||m.text;
  if(part==='shell')return JSON.stringify(m,null,2);
  if(part.startsWith('receipt:'))return new ReceiptStore(this.file,'O').get(part.slice(8))?.body||'';
  if(part.startsWith('tool:'))return JSON.stringify((m.content||[]).find(p=>p.id===part.slice(5))?.state||{},null,2);
  const [kind,ordinal]=part.split(':');const p=(m.content||[]).filter(p=>p.type===kind)[Number(ordinal)];return this.live.get(m.id+':'+part)?.text||p?.text||'';
 }
 close(){this.closed=true;this.live.clear();this.pending.clear();}
}
module.exports={OpenCodeChat};
