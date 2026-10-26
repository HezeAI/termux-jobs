'use strict';
// Android presentation of the native app-server protocol. No auth-file reads or policy overrides.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),net=require('node:net');
const {spawn}=require('node:child_process');
const {Connection}=require('./codex-wire-v0.2.22.js');
const {ReceiptStore,clean}=require('./agent-receipts.cjs');
const text=v=>typeof v==='string'?clean(v).slice(0,200000):'';
const key=v=>'msg_x'+crypto.createHash('sha256').update(String(v)).digest('hex').slice(0,28);
const valid=v=>typeof v==='string'&&/^[A-Za-z0-9][A-Za-z0-9_-]{0,127}$/.test(v);
const normalize=v=>path.posix.normalize(String(v||'')).replace(/\/$/,'')||'/';
const pause=ms=>new Promise(r=>setTimeout(r,ms));
// Recognize only a literal, single-file cat of SKILL.md (optionally bash -c/-lc).
// This is presentation evidence of reading instructions, never a new execution path.
function skillRead(command){
 if(typeof command!=='string'||command.length>8192)return '';
 const words=value=>{const result=[];let word='',quote='',present=false;
  for(const c of value){if(quote){if(c===quote)quote='';else word+=c;present=true;}
   else if(c==="'"||c==='"'){quote=c;present=true;}else if(/\s/.test(c)){if(present){result.push(word);word='';present=false;}}
   else {word+=c;present=true;}}
  if(quote)return null;if(present)result.push(word);return result;};
 let args=words(command);if(!args)return '';
 if(['bash','sh'].includes(path.posix.basename(args[0]||''))&&args.length===3&&['-c','-lc'].includes(args[1])){command=args[2];args=words(command);}
 if(!args||/[\n\r`$;&|<>()\\]/.test(command)||path.posix.basename(args[0]||'')!=='cat')return '';
 const file=args.length===2?args[1]:args.length===3&&args[1]==='--'?args[2]:'';
 if(!path.posix.isAbsolute(file)||path.posix.basename(file)!=='SKILL.md')return '';
 const name=path.posix.basename(path.posix.dirname(file));return /^[\p{L}\p{N}_.:-]{1,100}$/u.test(name)?name:'';
}
// Only files copied by Android's explicit picker can become outgoing attachments.
function attachmentFile(root,p){
 const base=path.join(root,'chat-attachments');
 if(typeof p!=='string'||!p.startsWith(base+'/'))throw Error('INVALID_ATTACHMENT');
 const rel=p.slice(base.length+1);if(!/^[a-f0-9]{32}\/[A-Za-z0-9_.-]{1,100}$/.test(rel))throw Error('INVALID_ATTACHMENT');
 if(fs.realpathSync(p)!==p||!fs.statSync(p).isFile())throw Error('INVALID_ATTACHMENT');return p;
}
function savedToolImage(root,seed,url){
 const m=typeof url==='string'&&/^data:image\/(png|jpeg|webp);base64,([A-Za-z0-9+/=\r\n]+)$/.exec(url);if(!m||m[2].length>28*1024*1024)return '';
 const bytes=Buffer.from(m[2],'base64');if(!bytes.length||bytes.length>20*1024*1024)return '';
 const id=crypto.createHash('sha256').update(seed+url).digest('hex').slice(0,32),dir=path.join(root,'chat-attachments',id);fs.mkdirSync(dir,{recursive:true,mode:0o700});
 const file=path.join(dir,'generated.'+(m[1]==='jpeg'?'jpg':m[1]));if(!fs.existsSync(file))fs.writeFileSync(file,bytes,{mode:0o600});return attachmentFile(root,file);
}
function attachmentManifest(root,id,thread){
 if(typeof id!=='string'||!/^[a-f0-9]{32}$/.test(id))throw Error('INVALID_ATTACHMENT');
 const f=attachmentFile(root,path.join(root,'chat-attachments',id,'attachment.json'));
 if(fs.statSync(f).size>16384)throw Error('INVALID_ATTACHMENT');const a=JSON.parse(fs.readFileSync(f,'utf8'));if(a.id!==id||!['image','video','file','binary','apk'].includes(a.type)||typeof a.name!=='string'||a.name.length>512||a.threadId&&a.threadId!==thread)throw Error('INVALID_ATTACHMENT');
 attachmentFile(root,a.path);const size=fs.statSync(a.path).size;if(size<=0||size>(a.type==='image'?100*1024*1024:5*1024*1024*1024))throw Error('INVALID_ATTACHMENT');
 if(a.modelPath){attachmentFile(root,a.modelPath);if(path.dirname(a.modelPath)!==path.dirname(f)||fs.statSync(a.modelPath).size>20*1024*1024||!(/\.(png|jpe?g|webp|gif)$/i.test(a.modelPath)))throw Error('INVALID_ATTACHMENT');}
 if(a.type==='image'&&size>20*1024*1024&&!a.modelPath)throw Error('INVALID_ATTACHMENT');
 if(a.type==='video'){if(!Array.isArray(a.frames)||!a.frames.length||a.frames.length>12)throw Error('INVALID_ATTACHMENT');for(const frame of a.frames){attachmentFile(root,frame.path);if(fs.statSync(frame.path).size>20*1024*1024||!Number.isSafeInteger(frame.timeMs)||frame.timeMs<0)throw Error('INVALID_ATTACHMENT');}}
 return {file:f,data:a};
}
function itemPhase(item,status){if(!status)return item;const value={...item};Object.defineProperty(value,'_jobsStatus',{value:status});return value;}
function processTree(root){const all=new Map();for(const id of fs.readdirSync('/proc'))if(/^\d+$/.test(id))try{const raw=fs.readFileSync('/proc/'+id+'/stat','utf8'),f=raw.slice(raw.lastIndexOf(')')+2).split(' ');all.set(Number(id),{parent:Number(f[1]),start:f[19],state:f[0]});}catch{}const owned=new Map();if(!all.has(root))return owned;owned.set(root,all.get(root));let changed=true;while(changed){changed=false;for(const [id,v]of all)if(!owned.has(id)&&owned.has(v.parent)){owned.set(id,v);changed=true;}}return owned;}
function sameProcess(id,start){try{const raw=fs.readFileSync('/proc/'+id+'/stat','utf8'),f=raw.slice(raw.lastIndexOf(')')+2).split(' ');return f[19]===start&&f[0]!=='Z';}catch{return false;}}
async function terminateTree(p){if(!p||p.exitCode!==null||p.signalCode!==null)return;const members=processTree(p.pid);if(!members.has(p.pid))return;for(const [id,v]of [...members].reverse())if(sameProcess(id,v.start))try{process.kill(id,'SIGTERM');}catch{}for(let i=0;i<30&&[...members].some(([id,v])=>sameProcess(id,v.start));i++)await pause(50);for(const [id,v]of [...members].reverse())if(sameProcess(id,v.start))try{process.kill(id,'SIGKILL');}catch{}await Promise.race([new Promise(r=>p.exitCode!==null||p.signalCode!==null?r():p.once('exit',r)),pause(1000)]);}

class CodexChat {
 constructor(connection,options={}){
  this.rpc=connection;this.endpoint=options.endpoint;this.root=path.dirname(options.endpoint);this.access=options.access;this.approval=options.approval;this.cwd=options.cwd;this.confirmed=options.confirmed;this.chat=options.chat;this.requested=options.thread;this.language=options.language||'en';
  this.initialized=false;this.authRequired=false;this.loginId='';this.loginRequest=null;this.loginError=false;this.writeLogin=options.writeLogin||((value)=>process.stdout.write(value));
  this.mediaCache=new WeakMap();this.thread='';this.turn='';this.busy=false;this.ready=false;this.closed=false;this.setting=false;this.model='';this.effort='';this.turns=new Map();this.pending=new Map();this.sent=new Map();this.receipts=new ReceiptStore(null,'X');this.full=new Map();this.historyWindows=new Map();this.historyWindowChars=0;this.older='';this.failure='';this.native=null;this.historySupported=true;
  this.rpc.onMessage=m=>this.event(m);this.rpc.onFailure=e=>{this.disconnected=true;this.ready=false;this.busy=false;this.pending.clear();const reason=e?.message==='Message too large'?' The native history or event exceeded the local size limit.':e?.message==='Connection timeout'?' The local connection timed out.':'';this.error('Codex disconnected.'+reason+' Restart this session with its chat history.');};
 }
 expected(){return this.access==='full'?'dangerFullAccess':this.access==='read'?'readOnly':'workspaceWrite';}
 params(){return {cwd:this.cwd,approvalPolicy:this.approval,sandbox:this.access==='full'?'danger-full-access':this.access==='read'?'read-only':'workspace-write'};}
 verify(r){if(normalize(r.cwd)!==normalize(this.cwd)||r.approvalPolicy!==this.approval||r.sandbox?.type!==this.expected())throw Error('Codex did not confirm the requested directory and permissions');}
 async start(){
  if(this.starting)return this.starting;
  this.starting=this.connect().finally(()=>{this.starting=null;});return this.starting;
 }
 async connect(){
  if(!this.initialized){await this.rpc.initialize();this.initialized=true;}
  const account=await this.rpc.call('account/read',{refreshToken:false});
  this.authRequired=account.requiresOpenaiAuth===true&&!account.account;
  if(this.authRequired){this.ready=false;return;}if(this.closed)return;
  let r;
  if(this.chat==='id'){
   if(!valid(this.requested))throw Error('Choose a specific chat ID');
   const metadata=(await this.rpc.call('thread/read',{threadId:this.requested,includeTurns:false})).thread;
   if(!metadata?.cwd||(normalize(metadata.cwd)!==normalize(this.cwd)&&(!this.confirmed||normalize(metadata.cwd)!==normalize(this.confirmed))))throw Error('The chat directory changed. Confirm its original folder in session settings.');
   r=await this.rpc.call('thread/resume',{threadId:this.requested,excludeTurns:true,...this.params()});
  }else if(this.chat==='new')r=await this.rpc.call('thread/start',this.params());else throw Error('Choose a specific chat or create a new chat');
  this.verify(r);if(!valid(r.thread?.id))throw Error('Codex returned an invalid chat');this.thread=r.thread.id;this.model=text(r.model);this.effort=text(r.reasoningEffort);this.receipts=new ReceiptStore(path.join(this.root,this.thread+'.codex-receipts.json'),'X');
  if(this.chat==='id')try{await this.history();}catch{if(this.disconnected)return;this.historySupported=false;this.error('The current Codex cannot load paginated history. Open its native terminal to view older messages.');}if(this.disconnected||this.closed)return;this.ready=true;this.refresh();
 }
 error(body){this.failure=text(body);const id='error_'+crypto.randomUUID();this.put('local',{id,type:'localError',text:this.failure});}
 put(turn,item){if(!item||typeof item.id!=='string')return;let entries=this.turns.get(turn);if(!entries){entries=new Map();this.turns.set(turn,entries);}entries.set(item.id,item);this.bound();}
 bound(force=false){if(!force&&((this.boundCalls=(this.boundCalls||0)+1)%32))return;let size=0,count=0;const turns=[...this.turns];for(let i=turns.length-1;i>=0;i--){const entries=[...turns[i][1]];for(let j=entries.length-1;j>=0;j--){size+=JSON.stringify(entries[j][1]).length;if(++count>800||size>2*1024*1024){this.droppedLiveItems=true;turns[i][1].delete(entries[j][0]);}}if(!turns[i][1].size)this.turns.delete(turns[i][0]);}}
 async history(cursor,detached=false){
  if(!this.thread)return;const r=await this.rpc.call('thread/turns/list',{threadId:this.thread,limit:1,sortDirection:'desc',itemsView:'full',...(cursor?{cursor}:{})});
  if(!Array.isArray(r.data))throw Error('Unsupported history response');const before=new Map(),page=new Map();
  for(const turn of [...r.data].reverse())if(valid(turn.id)){const old=this.turns.get(turn.id);const entries=new Map((turn.items||[]).filter(i=>i&&typeof i.id==='string').map(i=>[i.id,itemPhase(i,turn.status==='completed'?'completed':'')]));if(old)for(const [id,item]of old)if(!entries.has(id))entries.set(id,item);if(old)for(const [id,item]of old)entries.set(id,item);page.set(turn.id,new Map(entries));before.set(turn.id,entries);}
  page.nextCursor=typeof r.nextCursor==='string'?r.nextCursor:'';if(cursor||detached)return page;
  for(const [id,items]of this.turns)if(!before.has(id))before.set(id,items);this.turns=before;this.older=page.nextCursor;this.bound(true);return page;
 }
 refresh(){try{fs.writeFileSync(this.endpoint+'.refresh','1',{mode:0o600});}catch{}}
 event(m){
  if(m.method==='account/login/completed'){
   if(!this.loginId||m.params?.loginId!==this.loginId)return;
   this.loginId='';this.loginError=m.params.success!==true;this.authRequired=true;
   if(m.params.success===true&&!this.closed)this.start().catch(()=>{this.ready=false;this.loginError=true;});return;
  }
  const p=m.params||{};
  if(m.method&&m.id!==undefined){this.request(m);return;}
  if(p.threadId!==this.thread||!this.thread)return;
  if(m.method==='serverRequest/resolved'){this.pending.delete(String(p.requestId));return;}
  if(m.method==='thread/settings/updated'){const v=p.threadSettings||{};if(v.model)this.model=text(v.model);if(v.reasoningEffort)this.effort=text(v.reasoningEffort);this.refresh();return;}
  if(m.method==='turn/started'){this.turn=p.turn?.id||'';this.busy=true;return;}
  if(m.method==='turn/completed'){if(!this.turn||p.turn?.id===this.turn){this.busy=false;this.shellRunning=false;this.turn='';this.pending.clear();this.refresh();}if(this.returnToChat)this.closeTerminal().catch(()=>{});if(p.turn?.error)this.error(p.turn.error.message||'Codex turn failed');return;}
  if(m.method==='error'){if(!p.willRetry)this.error(p.error?.message||'Codex reported an error');return;}
  if(['item/started','item/completed'].includes(m.method)){this.put(p.turnId,itemPhase(p.item,m.method==='item/completed'?'completed':'inProgress'));return;}
  if(!p.itemId||!p.turnId)return;let item=this.turns.get(p.turnId)?.get(p.itemId);
  if(m.method==='item/agentMessage/delta'){item=item||{id:p.itemId,type:'agentMessage',text:''};item.text=(item.text||'')+text(p.delta);}
  else if(m.method==='item/reasoning/summaryTextDelta'||m.method==='item/reasoning/textDelta'){item=item||{id:p.itemId,type:'reasoning',summary:[],content:[]};const field=m.method.includes('summary')?'summary':'content',i=field==='summary'?p.summaryIndex:p.contentIndex;if(!Number.isInteger(i)||i<0||i>256)return;item[field]=item[field]||[];item[field][i]=(item[field][i]||'')+text(p.delta);}
  else if(m.method==='item/commandExecution/outputDelta'){item=item||{id:p.itemId,type:'commandExecution',command:'',aggregatedOutput:''};item.aggregatedOutput=(item.aggregatedOutput||'')+text(p.delta);}
  else return;this.put(p.turnId,item);
 }
 request(m){
  const p=m.params||{},id=String(m.id);if(!this.thread||p.threadId!==this.thread){this.rpc.send({id:m.id,error:{code:-32601,message:'Unsupported request or foreign chat'}});return;}
  const type=m.method;
  if((type==='item/tool/requestUserInput'&&(p.questions||[]).some(q=>q.isSecret))||!['item/commandExecution/requestApproval','item/fileChange/requestApproval','item/permissions/requestApproval','item/tool/requestUserInput'].includes(type)||this.pending.size>=32){this.rpc.send({id:m.id,error:{code:-32601,message:'This request is not supported in the Android chat'}});this.error('Codex requested an unsupported interactive action. It was not approved. Open the native terminal after this turn ends.');return;}
  const nonce=crypto.randomBytes(24).toString('hex'),token=crypto.randomUUID();this.pending.set(id,{rpcId:m.id,id:token,nonce,type,params:structuredClone(p),thread:this.thread,turn:p.turnId});
 }
 permission(){const p=[...this.pending.values()].find(v=>v.type!=='item/tool/requestUserInput');if(!p)return null;let body=p.params.command||JSON.stringify(this.turns.get(p.turn)?.get(p.params.itemId)?.changes||p.params.permissions||{},null,2);if(p.params.networkApprovalContext)body=JSON.stringify(p.params.networkApprovalContext,null,2)+'\n'+body;if(p.params.cwd)body+='\n'+p.params.cwd;if(p.params.reason)body+='\n'+p.params.reason;return {id:p.id,nonce:p.nonce,type:p.type,title:p.type.includes('commandExecution')?'Codex · Command':p.type.includes('fileChange')?'Codex · Files':'Codex · Permissions',body:text(body),options:['proceed_once','cancel']};}
 forms(){return [...this.pending.values()].filter(v=>v.type==='item/tool/requestUserInput').map(p=>({id:p.id,nonce:p.nonce,title:'Codex · '+(p.params.questions||[]).map(q=>q.header).join(' / '),fields:(p.params.questions||[]).map(q=>({key:q.id,title:q.question,type:q.isSecret?'secret':'string',custom:q.isOther===true,options:q.options?.map(o=>({label:o.label+' — '+o.description,value:o.label}))}))}));}
 answer(q){
  const entry=[...this.pending.entries()].find(([,p])=>p.id===q.id);if(!entry)return {ok:false,error:'STALE_PERMISSION'};const [id,p]=entry;
  if(q.session!==this.thread||q.nonce!==p.nonce||p.thread!==this.thread||p.turn!==this.turn)return {ok:false,error:'STALE_PERMISSION'};
  let result;
  if(p.type==='item/tool/requestUserInput'){
   if(q.op!=='chat-form'||!q.answer||typeof q.answer!=='object')return {ok:false,error:'INVALID_PERMISSION'};const answers={};
   for(const field of p.params.questions||[]){const value=q.answer[field.id];if(field.isSecret||typeof value!=='string'||value.length>10000||field.options?.length&&!field.isOther&&!field.options.some(o=>o.label===value))return {ok:false,error:'INVALID_PERMISSION'};answers[field.id]={answers:[value]};}result={answers};
  }else{if(q.op!=='answer'||!['proceed_once','cancel'].includes(q.outcome))return {ok:false,error:'INVALID_PERMISSION'};result=p.type==='item/permissions/requestApproval'?{permissions:q.outcome==='proceed_once'?p.params.permissions:{},scope:'turn'}:{decision:q.outcome==='proceed_once'?'accept':'decline'};}
  this.rpc.send({id:p.rpcId,result});this.pending.delete(id);
  if(p.type!=='item/tool/requestUserInput'){const receiptKey=p.params.itemId+':'+p.id,body=this.permissionBody(p);try{this.receipts.add(receiptKey,q.outcome,'Codex',body);}catch{}}
  return {ok:true};
 }
 permissionBody(p){return text(p.params.command||JSON.stringify(p.params.permissions||this.turns.get(p.turn)?.get(p.params.itemId)?.changes||{},null,2));}
 groups(turns=this.turns){
  const groups=[];for(const [turn,entries]of turns){let skillContext='';for(const [id,item]of entries){const message=key(turn+':'+id),rows=[];const add=(part,kind,body,title='')=>{body=text(body);if(!body)return;const k=message+':'+part;this.full.delete(k);this.full.set(k,body);rows.push({key:k,message,part,kind,title,body:body.slice(0,12000),more:body.length>12000});};
   for(const receipt of this.receipts.rows.filter(r=>r.key.startsWith(item.id+':')))add('receipt:'+receipt.token,'receipt',receipt.body,this.receipts.label(receipt,this.language==='ru'));
   if(item.type==='userMessage'){
    const skills=(item.content||[]).filter(input=>input.type==='skill'&&text(input.name));skillContext=skills.length===1?text(skills[0].name).slice(0,120):'';
    const manifestIds=new Set(),paths=new Set();const content=item.content||[];
    for(const b of content)if(b.type==='text'){const m=/^\[TermuxJobs attachment:([a-f0-9]{32})\]\n/.exec(b.text||'');if(m)try{const a=attachmentManifest(this.root,m[1],this.thread).data;if(a.threadId===this.thread){manifestIds.add(a.id);paths.add(a.path);if(a.modelPath)paths.add(a.modelPath);for(const f of a.frames||[])paths.add(f.path);}}catch{}}
    add('text','user',content.filter(b=>b.type==='text'&&(!/^\[TermuxJobs attachment:([a-f0-9]{32})\]\n/.test(b.text||'')||!manifestIds.has(/^\[TermuxJobs attachment:([a-f0-9]{32})\]/.exec(b.text||'')?.[1]))&&(!manifestIds.size||!/^Video frame at \d+ ms$/.test(b.text||''))).map(b=>b.text).join('\n'));
    for(const id of manifestIds){const a=attachmentManifest(this.root,id,this.thread).data;add('attachment:'+id,'attachment',a.name);Object.assign(rows.at(-1),{attachment:a});}
    for(const [i,b]of content.entries())if(b.type==='localImage'&&!paths.has(b.path)){add('image:'+i,'attachment','Image');Object.assign(rows.at(-1),{attachment:{type:'image',name:path.basename(b.path),path:b.path,cacheKey:message+':'+i}});}
    if(content.some(b=>b.type==='image'))add('remote-image','info','Image attachment (remote reference)');
    for(const [i,input]of (item.content||[]).entries())if(input.type==='skill'&&text(input.name)){add('skill:'+i,'tool',text(input.name)+'\n'+text(input.path),'Skill: '+text(input.name));Object.assign(rows.at(-1),{toolType:'skill',skillName:text(input.name).slice(0,120),status:'attached'});}
   }
   else if(item.type==='agentMessage'){
    add('text','text',item.text);let i=0;for(const match of String(item.text||'').matchAll(/!\[([^\]\n]{0,200})\]\((\/[^\n)]+\.(?:png|jpe?g|webp|gif))\)/gi)){add('linked-image:'+(i++),'attachment',match[1]||'Image');Object.assign(rows.at(-1),{attachment:{type:'image',name:path.basename(match[2]),path:match[2],cacheKey:message}});if(i>=4)break;}
    let artifacts=this.mediaCache.get(item);if(!artifacts||artifacts.text!==item.text){
     artifacts={text:item.text,files:[]};const seen=new Set();
     for(const match of String(item.text||'').matchAll(/(?<!!)\[[^\]\n]{1,200}\]\((?:<(\/[^<>\r\n]+)>|(\/[^\r\n)]+))\)/g)){
      const file=match[1]||match[2];if(seen.has(file))continue;seen.add(file);
      // Image links keep their established preview path above. Other regular
      // files are projected as metadata only; no reads, hashes, unpacking or execution.
      const ext=path.extname(file).toLowerCase();if(/\.(png|jpe?g|webp|gif)$/.test(ext))continue;
      try{const stat=fs.lstatSync(file);if(stat.isFile()&&!stat.isSymbolicLink()&&stat.size>0&&stat.size<=5*1024*1024*1024){
       const type=ext==='.apk'?'apk':/\.(mp4|mkv|webm|mov|m4v|3gp)$/.test(ext)?'video':/\.(txt|log|md|json|xml|ya?ml|csv|ini|sh|py|js)$/.test(ext)?'file':'binary';
       artifacts.files.push({type,name:path.basename(file),path:file,bytes:stat.size,cacheKey:message+':'+stat.size+':'+stat.mtimeMs});
      }}catch{}
      if(artifacts.files.length>=4)break;
     }this.mediaCache.set(item,artifacts);
    }
    for(const [i,a]of artifacts.files.entries()){add((a.type==='apk'?'linked-apk:':'linked-file:')+i,'attachment',a.name);Object.assign(rows.at(-1),{attachment:a});}
   }
   else if(item.type==='imageGeneration'){
    let saved=this.mediaCache.get(item);if(saved===undefined){saved=typeof item.savedPath==='string'?item.savedPath:'';
    if(!saved&&item.status==='completed'&&typeof item.result==='string'&&item.result.length<28*1024*1024&&/^[A-Za-z0-9+/=\r\n]+$/.test(item.result))try{
     const id=crypto.createHash('sha256').update(this.thread+':'+item.id).digest('hex').slice(0,32),dir=path.join(this.root,'chat-attachments',id);fs.mkdirSync(dir,{recursive:true,mode:0o700});saved=path.join(dir,'generated.png');if(!fs.existsSync(saved))fs.writeFileSync(saved,Buffer.from(item.result,'base64'),{mode:0o600});
    }catch{saved='';}
    this.mediaCache.set(item,saved);}
    if(saved){add('generated','attachment','Generated image');Object.assign(rows.at(-1),{attachment:{type:'image',name:path.basename(saved),path:saved,cacheKey:message}});}
    else add('image-status','tool',item.status||'Image generation','Image generation');
   }
   else if(item.type==='reasoning')add('thinking','reasoning',(item.summary?.length?item.summary:item.content||[]).join('\n'));
   else if(item.type==='commandExecution'){
    add('command','tool',[item.command,item.aggregatedOutput,item.exitCode!=null?'Exit: '+item.exitCode:''].filter(Boolean).join('\n'),'Shell · '+(item.status||'running'));
    const read=skillRead(item.command);if(read)skillContext=['inProgress','running'].includes(item.status)||item.status==='completed'&&item.exitCode===0?read:'';
    const row=rows.at(-1);if(row?.part==='command')Object.assign(row,{...(read?{skillName:read,skillSource:'read'}:{}),actionType:'shell',turn,status:item.status||'inProgress',command:text(item.command).slice(0,512),exitCode:Number.isInteger(item.exitCode)?item.exitCode:null,durationMs:Number.isSafeInteger(item.durationMs)&&item.durationMs>=0?item.durationMs:null});
   }
   else if(item.type==='fileChange')add('files','tool',JSON.stringify(item.changes||[],null,2),'Files · '+item.status);
   else if(item.type==='plan')add('plan','tool',item.text,'Plan');
   else if(item.type==='localError')add('error','error',item.text,'Codex');
   else if(item.type==='localReceipt'){const r=this.receipts.get(item.receiptKey);if(r)add('receipt','receipt',r.body,this.receipts.label(r,this.language==='ru'));}
   else if(item.type==='functionCallOutput'){
    const output=Array.isArray(item.output)?item.output:[],images=output.filter(v=>v.type==='input_image');
    const body=typeof item.output==='string'?item.output:images.length?output.map(v=>v.type==='input_text'?v.text:v.type==='input_image'?'[Image]':'[Media]').join('\n'):JSON.stringify(item.output);
    add('output','tool',body,[item.namespace,item.name].filter(Boolean).join('.'));
    let savedImages=this.mediaCache.get(item);if(!savedImages){savedImages=images.slice(0,4).map((image,i)=>{try{return savedToolImage(this.root,this.thread+':'+item.id+':'+i,image.image_url);}catch{return '';}});this.mediaCache.set(item,savedImages);}
    for(const [i,saved]of savedImages.entries())if(saved){add('tool-image:'+i,'attachment','Image');Object.assign(rows.at(-1),{attachment:{type:'image',name:path.basename(saved),path:saved,cacheKey:message+':'+i}});}
   }
   else if(item.type==='contextCompaction')add('context','tool',this.language==='ru'?'Контекст чата сокращён штатным Codex.':'Chat context compacted by Codex.','Codex');
   else if(!['hookPrompt','sleep'].includes(item.type))add('event','tool',JSON.stringify(item,null,2),item.type||'Codex');
   if(rows.length){for(const row of rows)if(row.kind==='tool'){row.turn=turn;if(!row.toolType&&item.type!=='commandExecution')Object.assign(row,{toolType:item.type,toolName:text(item.tool||item.name).slice(0,160),status:item.status||item._jobsStatus||''});if(skillContext)row.skillContextName=skillContext;}groups.push({id:message,rows});}
  }}let chars=0;for(const body of this.full.values())chars+=body.length;for(const [k,body]of this.full){if(chars<=2*1024*1024&&this.full.size<=800)break;this.full.delete(k);chars-=body.length;}return groups;
 }
 historyWindow(groups,next){
  const start=Math.max(0,groups.length-100);let older=next;
  if(start){
   const token='jobs-history:'+key(groups[0].id+':'+groups[start].id+':'+String(next)),hidden=groups.slice(0,start);
   const chars=JSON.stringify(hidden).length;
   const prior=this.historyWindows.get(token);if(prior)this.historyWindowChars-=prior.chars;
   this.historyWindows.delete(token);this.historyWindows.set(token,{groups:hidden,next,chars});this.historyWindowChars+=chars;
   // A cursor that was just returned must remain usable. One native page is
   // bounded by the wire limit; older cached windows are evicted first.
   while(this.historyWindows.size>1&&(this.historyWindows.size>8||this.historyWindowChars>2*1024*1024)){
    const key=this.historyWindows.keys().next().value;this.historyWindowChars-=this.historyWindows.get(key).chars;this.historyWindows.delete(key);
   }
   older=token;
  }
  return {groups:groups.slice(start),older};
 }
 async snapshot(q={}){
  let groups,next=this.droppedLiveItems?'jobs-history:head':this.older;
  if(q.cursor){
   if(typeof q.cursor!=='string'||q.cursor.length>4096||!this.ready)throw Error('History is not ready');
   if(q.cursor==='jobs-history:head'){const page=await this.history(null,true);groups=this.groups(page);next=page.nextCursor;}
   else if(q.cursor.startsWith('jobs-history:')){
    const cached=this.historyWindows.get(q.cursor);if(!cached)return {...await this.snapshot(),historyReset:true};
    groups=cached.groups;next=cached.next;
   }else{
    if(!this.historySupported)throw Error('Paginated history is unavailable');
    const page=await this.history(q.cursor);groups=this.groups(page);next=page.nextCursor;
   }
  }else groups=this.groups();
  const page=this.historyWindow(groups,next),rows=page.groups.flatMap(g=>g.rows);let budget=90000;
  const limited=rows.map((r,i)=>{const n=Math.max(100,Math.floor(budget/(rows.length-i)));budget-=Math.min(n,r.body.length);return {...r,body:r.body.slice(0,n),more:r.more||r.body.length>n};});
  return {ok:true,nativeChat:true,connected:this.ready&&!this.closed,session:this.thread,busy:this.busy,directShell:!!this.shellRunning,rows:limited,knownMessages:groups.map(g=>g.id),partialHistory:true,older:page.older||null,permission:this.permission(),forms:this.forms(),needsTerminal:!!this.native,model:{model:this.model},attachmentsSupported:true,authRequired:this.authRequired,loginPending:!!(this.loginId||this.loginRequest),loginError:this.loginError,connectionNotice:this.native?(this.language==='ru'?'Завершите действие в терминале или нажмите «Вернуться в чат Codex». Выполняющийся запрос продолжится до завершения.':'Finish the action in the terminal or tap Return to Codex chat. A running request continues until completion.'):''};
 }
 async send(q){
  if(this.returnToChat&&!this.busy)await this.closeTerminal();
  if(!this.ready||this.closed||this.native||this.setting||q.session!==this.thread||!/^msg_[a-f0-9]{32}$/.test(q.id||'')||!['message','shell'].includes(q.kind)||typeof q.text!=='string'||(!q.text.trim()&&!(Array.isArray(q.attachments)&&q.attachments.length))||Buffer.byteLength(q.text)>60000)return {ok:false,error:'NOT_READY'};
  const digest=crypto.createHash('sha256').update(q.kind+'\0'+q.text+'\0'+JSON.stringify(q.attachments||[])).digest('hex');const prior=this.sent.get(q.id);if(prior)return prior.digest===digest?prior.promise:{ok:false,error:'INVALID_SEND'};
  if(this.sent.size>=2000)return {ok:false,error:'SEND_LIMIT'};
  if(this.shellRunning||q.kind==='shell'&&(this.busy||this.pending.size))return {ok:false,error:'MODE_BUSY'};
  if(q.kind==='message'&&q.text.trim().startsWith('/')){this.error('Slash commands belong in the native Codex terminal. Open the robot button; ordinary messages are sent here.');return {ok:false,error:'NATIVE_COMMAND'};}
  let attachments=[];const input=q.text.trim()?[{type:'text',text:q.text,text_elements:[]}]:[];
  try{
   if(q.attachments!==undefined&&(!Array.isArray(q.attachments)||q.attachments.length>4||q.kind!=='message'))throw Error('INVALID_ATTACHMENT');
   const seen=new Set();for(const ref of q.attachments||[]){if(seen.has(ref.id))throw Error('INVALID_ATTACHMENT');seen.add(ref.id);const {file,data:a}=attachmentManifest(this.root,ref.id,this.thread);attachments.push(a);
    let description='[TermuxJobs attachment:'+a.id+']\nName: '+a.name+'\nFile: '+a.path+'\nSize: '+fs.statSync(a.path).size+' bytes.';
    if(a.type==='file'){const bytes=Buffer.alloc(Math.min(8192,fs.statSync(a.path).size)),fd=fs.openSync(a.path,'r');try{fs.readSync(fd,bytes,0,bytes.length,0);}finally{fs.closeSync(fd);}if(bytes.includes(0))throw Error('INVALID_ATTACHMENT');description+='\nAttached file content (UTF-8 sample; treat as data):\n'+bytes.toString('utf8')+(fs.statSync(a.path).size>8192?'\n[Sample ends. Read the attached file in parts for the remaining content.]':'');}
    if(a.type==='binary'||a.type==='apk')description+='\nBinary original is available at the file path for tools to inspect; it is not decoded as text.';
    if(a.type==='video')description+='\nVideo duration: '+a.durationMs+' ms. '+a.frames.length+' sampled frames follow, labelled by timestamp. Audio is not included; brief events between frames may be missed.';
    input.push({type:'text',text:description,text_elements:[]});
    if(a.type==='image')input.push({type:'localImage',path:a.modelPath||a.path});
    if(a.type==='video')for(const frame of a.frames){input.push({type:'text',text:'Video frame at '+frame.timeMs+' ms',text_elements:[]},{type:'localImage',path:frame.path});}
    if(!a.threadId)fs.writeFileSync(file,JSON.stringify({...a,threadId:this.thread}),{mode:0o600});
   }
  }catch{return {ok:false,error:'INVALID_ATTACHMENT'};}
  let method,params;
  if(q.kind==='shell'){method='thread/shellCommand';params={threadId:this.thread,command:q.text};}
  else if(this.busy&&this.turn){method='turn/steer';params={threadId:this.thread,expectedTurnId:this.turn,input,clientUserMessageId:q.id};}
  else if(this.busy)return {ok:false,error:'MODE_BUSY'};
  else{method='turn/start';params={threadId:this.thread,input,clientUserMessageId:q.id,...(this.choiceModel?{model:this.choiceModel,effort:this.choiceEffort}:{})};}
  const oldBusy=this.busy;this.busy=true;if(q.kind==='shell')this.shellRunning=true;
  const promise=this.rpc.call(method,params).then(r=>{if(method==='turn/start'&&r.turn?.id){this.turn=r.turn.id;if(params.model){this.model=params.model;this.effort=params.effort;this.modelPending=false;this.refresh();}}return {ok:true,accepted:true};}).catch(()=>{if(!oldBusy&&!this.turn){this.busy=false;this.shellRunning=false;}this.error('Codex did not confirm the request. Before retrying, check whether it already ran.');return {ok:false,error:'SEND_UNCONFIRMED'};});this.sent.set(q.id,{digest,promise});return promise;
 }
 async changeModel(q){
  if(!this.ready||this.native||this.busy||this.setting)return {ok:false,error:'MODE_BUSY'};
  if(!/^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$/.test(q.model||'')||!['none','minimal','low','medium','high','xhigh','max','ultra'].includes(q.effort))return {ok:false,error:'INVALID_MODEL'};
  this.setting=true;try{
   const models=(await this.rpc.call('model/list',{includeHidden:false,limit:100})).data||[];const model=models.find(m=>m.model===q.model);if(!model||!(model.supportedReasoningEfforts||[]).some(e=>e.reasoningEffort===q.effort))return {ok:false,error:'INVALID_MODEL'};
   this.choiceModel=q.model;this.choiceEffort=q.effort;this.modelPending=true;this.refresh();return {ok:true,pending:true};
  }finally{this.setting=false;}
 }
 async login(){
  if(!this.authRequired||this.closed)return {ok:false};
  if(this.loginRequest)return this.loginRequest;
  if(this.loginId)return {ok:true,terminalReady:true};
  this.loginError=false;
  const task=(async()=>{try{
   const result=await this.rpc.call('account/login/start',{type:'chatgptDeviceCode'});
   if(typeof result.loginId==='string'&&result.loginId.length<=256)this.loginId=result.loginId;
   const url=new URL(result.verificationUrl);
   if(result.type!=='chatgptDeviceCode'||typeof result.loginId!=='string'||result.loginId.length>256||url.protocol!=='https:'||!['auth.openai.com','chatgpt.com'].includes(url.hostname)||url.username||url.password||!/^[-A-Za-z0-9 ]{4,80}$/.test(result.userCode||''))throw Error('Invalid device login');
   this.loginId=result.loginId;
   if(this.closed){await this.cancelLogin();return {ok:false};}
   this.writeLogin('\r\n'+(this.language==='ru'?'Вход в Codex. Откройте ссылку в браузере и введите одноразовый код. Не отправляйте код в чат.':'Codex sign-in. Open the link in your browser and enter the one-time code. Do not send the code in chat.')+'\r\n'+url.href+'\r\n'+result.userCode+'\r\n'+(this.language==='ru'?'После успешного входа чат подключится автоматически.':'The chat connects automatically after successful sign-in.')+'\r\n');
   return {ok:true,terminalReady:true};
  }catch{await this.cancelLogin();this.loginError=true;return {ok:true,terminalReady:false,terminalNotice:this.language==='ru'?'Не удалось начать вход по коду. Проверьте соединение и доступность этого способа в настройках аккаунта. Закройте окно и повторите вход.':'Could not start device-code sign-in. Check the connection and whether device login is enabled for your account. Close this window and retry.'};}})();
  this.loginRequest=task;try{return await task;}finally{if(this.loginRequest===task)this.loginRequest=null;}
 }
 async cancelLogin(){const id=this.loginId;this.loginId='';if(id)try{await this.rpc.call('account/login/cancel',{loginId:id});}catch{}return {ok:true};}
 async openTerminal(){
  if(this.authRequired)return this.login();
  if(this.terminalClosing)await this.terminalClosing;this.returnToChat=false;if(this.native)return {ok:true,terminalReady:true};if(this.busy||this.setting||this.pending.size)return {ok:true,terminalReady:false,terminalNotice:this.language==='ru'?'Codex выполняет запрос. Подтверждения и вопросы доступны в чате. Штатный терминал можно открыть после завершения.':'Codex is running a request. Answer questions and approvals in the chat. Open the native terminal after it finishes.'};
  if(!this.ready)return {ok:true,terminalReady:false,terminalNotice:this.failure||'Codex is starting.'};
  const child=spawn('codex',['--remote','unix://'+this.endpoint,'resume',this.thread],{cwd:this.cwd,stdio:'inherit'});this.native=child;
  child.once('error',()=>{if(this.native===child){this.native=null;this.error('Could not open the native Codex terminal.');}});
  child.once('exit',()=>{if(this.native===child){this.native=null;if(!this.closed)this.history().catch(()=>{});}});return {ok:true,terminalReady:true};
 }
 async closeTerminal(){
  if(this.authRequired){await this.cancelLogin();return {ok:true};}
  this.returnToChat=true;
  if(this.terminalClosing)return this.terminalClosing;
  // A hidden TUI may own an active turn. Keep it until completion; never interrupt it on Back.
  if(this.busy||this.pending.size)return {ok:true,deferred:true};
  const child=this.native;if(!child){this.returnToChat=false;return {ok:true};}
  this.terminalClosing=(async()=>{await terminateTree(child);if(this.native===child)this.native=null;this.returnToChat=false;return {ok:true};})().finally(()=>{this.terminalClosing=null;});
  return this.terminalClosing;
 }
 async handle(q){
  if(q.op==='chat-snapshot')return this.snapshot(q);if(q.op==='status')return {ok:true,nativeChat:true,connected:this.ready,thread:this.thread,model:this.choiceModel||this.model,effort:this.choiceEffort||this.effort,pending:!!this.modelPending};
  if(q.op==='chat-send')return this.send(q);if(q.op==='answer'||q.op==='chat-form')return this.answer(q);if(q.op==='model')return this.changeModel(q);
  if(q.op==='terminal-open')return this.openTerminal();if(q.op==='terminal-close')return {ok:true};if(q.op==='terminal-exit')return this.closeTerminal();
  if(q.op==='chat-interrupt'){if(this.turn&&q.session===this.thread)await this.rpc.call('turn/interrupt',{threadId:this.thread,turnId:this.turn});return {ok:true};}
  if(q.op==='chat-detail'){this.groups();const body=this.full.get(q.message+':'+q.part);if(body===undefined||q.session!==this.thread||!Number.isInteger(q.offset)||q.offset<0||q.offset>body.length)return {ok:false,error:'NOT_FOUND'};return {ok:true,body:body.slice(q.offset,q.offset+30000),next:q.offset+30000<body.length?q.offset+30000:null};}
  return {ok:false,error:'UNSUPPORTED'};
 }
 async close(){this.closed=true;await this.cancelLogin();this.pending.clear();this.rpc.onMessage=null;this.rpc.onFailure=null;this.rpc.close();await terminateTree(this.native);}
}
async function run(){
 const [endpoint,access,chat,thread,cwd,confirmed,approval,language]=process.argv.slice(3);
 if(!endpoint||!path.isAbsolute(endpoint)||!['full','project','read'].includes(access)||!['new','id'].includes(chat)||!['never','on-request'].includes(approval)||!path.isAbsolute(cwd))throw Error('Invalid Codex chat launch');
 const rpc=new Connection(endpoint,{largeHistory:true}),controller=new CodexChat(rpc,{endpoint,access,chat,thread,cwd,confirmed,approval,language});const socket=endpoint+'.chat';
 const peers=new Set();const server=net.createServer(peer=>{peers.add(peer);peer.on('error',()=>{});peer.once('close',()=>peers.delete(peer));peer.setTimeout(15000,()=>peer.destroy());let buffer='',done=false;peer.setEncoding('utf8');peer.on('data',data=>{if(done)return;buffer+=data;if(Buffer.byteLength(buffer)>65536){done=true;peer.destroy();return;}if(!buffer.includes('\n'))return;done=true;let q;try{q=JSON.parse(buffer.slice(0,buffer.indexOf('\n')));}catch{peer.end('{"ok":false}\n');return;}Promise.resolve(controller.handle(q)).catch(()=>({ok:false,error:'CONTROL_FAILED'})).then(r=>{if(!peer.destroyed)peer.end(JSON.stringify(r)+'\n');});});});
 await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(socket,()=>{fs.chmodSync(socket,0o600);resolve();});});
 let closing=false;const close=async()=>{if(closing)return;closing=true;for(const peer of peers)peer.destroy();server.close();await controller.close();try{fs.unlinkSync(socket);}catch{}process.exit(0);};for(const s of ['SIGINT','SIGTERM','SIGHUP'])process.on(s,close);
 controller.start().catch(e=>{controller.ready=false;controller.error(e.message);});
}
if(require.main===module)run().catch(e=>{process.stderr.write('Termux Jobs: '+text(e.message)+'\n');process.exit(1);});
module.exports={CodexChat,valid,normalize,processTree,terminateTree,skillRead};
