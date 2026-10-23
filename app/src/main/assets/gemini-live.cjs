'use strict';
// Owned Gemini child. Status files contain scalars only; permission details use private RPC, in memory.
const MODEL=/^(?:auto|flash|pro|gemini-[A-Za-z0-9][A-Za-z0-9._-]{0,120})$/;
const safeModel=v=>typeof v==='string'&&MODEL.test(v)?v:'';
const number=v=>Number.isSafeInteger(v)&&v>=0?v:0;
function createBridge(send, fetcher=globalThis.fetch) {
  const {GeminiThinking}=require('./gemini-thinking.cjs');const reasoning=new GeminiThinking(process.env.JOBS_GEMINI_THINKING);
  let config, state='starting', uiSeen=false, unsupported=false, queued=null, changing=false, last='', lastSent=0;
  let totals={requests:0,input:0,output:0,cache:0,thoughts:0,total:0,apiMs:0}, observed='';
  let modeController=null, modeQueued=null, modeChange=null, modeApplying=false;
  let change=null, catalog=[], catalogAt=0, lastSelected='';
  const permissions=new Map(),receiptListeners=new Set();
  const {ReceiptStore}=require('./agent-receipts.cjs');let receiptStore=new ReceiptStore(null,'G'),receiptSession='';
  function receiptsFor(c){const id=c?.getSessionId?.();if(typeof id==='string'&&/^[a-zA-Z0-9_-]{1,128}$/.test(id)&&receiptSession!==id){receiptSession=id;const file=process.env.JOBS_GEMINI_RECEIPT_DIR?require('node:path').join(process.env.JOBS_GEMINI_RECEIPT_DIR,id+'.json'):null;receiptStore=new ReceiptStore(file,'G');}return receiptStore;}
  function rememberReceipt(p,outcome){if(!p)return;try{receiptsFor(p.config).add(p.callId,outcome,p.value.title,p.value.body);chat.invalidate();for(const f of receiptListeners)f();}catch{}}
  function prepareReceipt(callId,outcome){const p=permissions.get(callId);if(!p||!p.value.options.includes(outcome)||p.config!==config)return;return ()=>rememberReceipt(p,outcome);}
  const receiptSubscribe=f=>{receiptListeners.add(f);return ()=>receiptListeners.delete(f);};
  const receiptText=callId=>receiptStore.label(receiptStore.get(callId),process.env.JOBS_GEMINI_LANGUAGE==='ru');
  const currentPermission=()=>[...permissions.values()].find(p=>p.config===config&&!p.consumed&&!p.busy&&p.active&&p.project===p.config.getProjectRoot?.());
  const permissionBody=details=>details.type==='exec'?details.command:details.type==='edit'?[details.fileName,details.fileDiff].filter(v=>typeof v==='string').join('\n\n'):[details.prompt,...(Array.isArray(details.urls)?details.urls:[])].filter(v=>typeof v==='string').join('\n');
  function permission(callId,details,c,confirm,options,active,toolName,hasWarnings){
    if(typeof callId!=='string'||!details||!['exec','edit','info'].includes(details.type)||!active||hasWarnings)return ()=>{};
    if(details.isBuildFile||details.isModifying||details.untrustedFlags?.length||details.modifiedBuildFiles?.length)return ()=>{};
    const allowed=options.filter(o=>['proceed_once','proceed_always','cancel'].includes(o.value)).map(o=>o.value);
    if(!allowed.includes('proceed_once')||!allowed.includes('cancel'))return ()=>{};
    const body=permissionBody(details);
    if(typeof body!=='string'||!body.trim()||body.length>20000)return ()=>{};
    const safeText=v=>v.replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f\u202a-\u202e\u2066-\u2069]/g,ch=>'\\u'+ch.charCodeAt(0).toString(16).padStart(4,'0'));
    const value={id:require('node:crypto').randomUUID(),type:details.type,title:typeof toolName==='string'?safeText(toolName).slice(0,80):details.type,body:safeText(body),options:allowed};
    if(Buffer.byteLength(JSON.stringify(value),'utf8')>48000)return ()=>{};
    const record={value,callId,details,rawBody:body,config:c,confirm,active:!!active,project:c.getProjectRoot?.(),consumed:false,busy:false};
    permissions.set(callId,record);publish(true);
    return ()=>{if(permissions.get(callId)===record){permissions.delete(callId);publish(true);}};
  }
  function consumePermission(callId){const p=permissions.get(callId);if(p){p.consumed=true;publish(true);}}
  async function answerPermission(id,outcome){
    const p=currentPermission();
    if(!p||p.value.id!==id||!p.value.options.includes(outcome)||permissionBody(p.details)!==p.rawBody||p.details.isModifying||p.details.isBuildFile||p.details.untrustedFlags?.length||p.details.modifiedBuildFiles?.length||unsupported||(outcome==='proceed_always'&&(!p.config.isTrustedFolder?.()||p.config.getDisableAlwaysAllow?.())))return {ok:false,error:'STALE_PERMISSION'};
    p.busy=true;
    try{await p.confirm(p.callId,outcome);p.consumed=true;rememberReceipt(p,outcome);publish(true);return {ok:true,resolved:{id,outcome}};}
    catch{p.consumed=true;publish(true);return {ok:false,error:'PERMISSION_FAILED'};}
    finally{p.busy=false;}
  }
  const selected=()=>safeModel(config?.model);
  const actual=()=>{const active=safeModel(config?._activeModel);return observed||active||selected();};
  const nativeMode=()=>{const m=(modeController?.config||config)?.getApprovalMode?.();return m==='auto_edit'?'autoEdit':m;};
  const profileMode=m=>m==='autoEdit'?'auto_edit':m;
  function approval(c,set,plan,active){
    const controller={config:c,set,plan:!!plan,active:!!active};modeController=controller;publish();setImmediate(applyMode);
    return ()=>{if(modeController===controller){modeController=null;publish();}};
  }
  async function applyMode(){
    const controller=modeController;
    if(!modeQueued||modeApplying||state!=='idle'||!controller?.active||unsupported||!uiSeen)return;
    const request=modeQueued;
    if(controller.config!==request.config||nativeMode()!==request.before||controller.config.getProjectRoot?.()!==request.project){modeQueued=null;modeChange={id:request.id,mode:profileMode(request.mode),state:'cancelled'};publish();return;}
    if(request.mode==='plan'&&!controller.plan)return;
    modeQueued=null;modeApplying=true;
    try{await controller.set(request.mode);if(nativeMode()!==request.mode)throw Error('not_applied');modeChange={id:request.id,mode:profileMode(request.mode),state:'applied'};}
    catch{modeChange={id:request.id,mode:profileMode(request.mode),state:'failed'};}
    finally{modeApplying=false;publish();}
  }
  async function changeMode(mode,id){
    if(!['default','auto_edit','plan'].includes(mode)||typeof id!=='string'||!/^[-a-zA-Z0-9]{1,64}$/.test(id))return {ok:false,error:'INVALID_MODE'};
    if(!snapshot().available||!modeController?.active)return {ok:false,error:'MODE_UNAVAILABLE'};
    if(modeApplying)return {ok:false,error:'MODE_BUSY'};
    if(mode==='plan'&&!modeController.config.isPlanEnabled?.())return {ok:false,error:'MODE_UNAVAILABLE'};
    modeQueued={id,mode:mode==='auto_edit'?'autoEdit':mode,before:nativeMode(),config:modeController.config,project:modeController.config.getProjectRoot?.()};modeChange={id,mode,state:'queued'};publish();
    await applyMode();return {ok:true,status:snapshot()};
  }
  function snapshot(){
    const auth=config?.getContentGeneratorConfig?.()?.authType;
    const mode=nativeMode();
    return {schema:1,available:!!config&&uiSeen&&!unsupported&&!!auth&&!!config.getContentGenerator?.(),model:actual(),selectedModel:selected(),
      mode:['default','autoEdit','auto_edit','plan','yolo'].includes(mode)?mode:'',
      state:unsupported?'unsupported':uiSeen?state:'starting',auth:['gemini-api-key','oauth-personal','vertex-ai','gateway','compute-default-credentials'].includes(auth)?auth:'',
      ...totals,thinking:reasoning.status(actual()),change,modeChange,permissionId:unsupported?'':(currentPermission()?.value.id||''),canChangeMode:!!modeController?.active&&!unsupported,canPlan:!!modeController?.config?.isPlanEnabled?.(),canEdit:!!modeController?.config?.isTrustedFolder?.(),quotaAvailable:false};
  }
  function publish(force=false){try{const value=snapshot(),key=JSON.stringify(value);if(!force&&key===last&&Date.now()-lastSent<3000)return;last=key;lastSent=Date.now();send({type:'jobs-gemini-status',value});}catch{}}
  function attach(value){if(value&&typeof value.setModel==='function'){config=value;reasoning.attach(config);receiptsFor(config);if(lastSelected!==selected()){lastSelected=selected();observed='';}publish();}}
  function apply(){
    if(!queued||state!=='idle'||!config||unsupported||!uiSeen||changing)return;
    const request=queued;queued=null;
    if(selected()!==request.before){change={id:request.id,model:request.model,state:'cancelled'};publish();return;}
    changing=true;
    try{
      // Same native Config setter as /model set, temporary: no auth/rights/history mutation.
      config.setModel(request.model,true);
      if(selected()!==request.model)throw Error('not_applied');
      observed=request.model;change={id:request.id,model:request.model,state:'applied'};
    }catch{change={id:request.id,model:request.model,state:'failed'};}
    finally{changing=false;publish();}
  }
  function ui(value){if(!['idle','responding','waiting_for_confirmation'].includes(value))return;state=value;uiSeen=true;publish();if(value==='idle'){if(queued)setImmediate(apply);if(modeQueued)setImmediate(applyMode);}}
  function api(kind,c,event){
    attach(c);
    if(kind==='request')totals.requests++;
    if(kind==='response'){
      const u=event?.usage||{};totals.input+=number(u.input_token_count);totals.output+=number(u.output_token_count);
      totals.cache+=number(u.cached_content_token_count);totals.thoughts+=number(u.thoughts_token_count);totals.total+=number(u.total_token_count);totals.apiMs+=number(Math.round(event?.duration_ms||0));
      if(event?.role==='main')observed=safeModel(event?.model)||observed;
    }
    publish();
  }
  function changeModel(model,id){
    if(!MODEL.test(model)||typeof id!=='string'||!/^[-a-zA-Z0-9]{1,64}$/.test(id))return {ok:false,error:'INVALID_MODEL'};
    if(!snapshot().available)return {ok:false,error:'NOT_READY'};
    queued={model,id,before:selected()};change={id,model,state:'queued'};publish();
    if(state==='idle')apply();return {ok:true,status:snapshot()};
  }
  function nativeOptions(){
    const fallback=[{id:'auto',name:'Auto'},{id:'flash',name:'Flash'},{id:'pro',name:'Pro'}];
    try{const result=config.getModelConfigService().getAvailableModelOptions({hasAccessToPreview:config.getHasAccessToPreviewModel?.(),useGemini3_1:true,useLatestFlash:config.hasLatestFlashGAAccess?.(),useLatestFlashLite:config.hasLatestFlashLiteGAAccess?.()})
      .map(m=>({id:safeModel(m.modelId),name:String(m.name||m.modelId).slice(0,140)})).filter(m=>m.id);return result.length?result:fallback;}catch{return fallback;}
  }
  async function models(){
    if(!snapshot().available)return {ok:false,error:'NOT_READY'};
    if(catalog.length&&Date.now()-catalogAt<60000)return {ok:true,models:catalog,source:'api-catalog'};
    const auth=config.getContentGeneratorConfig?.();
    // API credentials stay inside this child and go only to the fixed official API host.
    if(auth?.authType==='gemini-api-key'&&typeof auth.apiKey==='string'&&auth.apiKey&&(!auth.baseUrl||/^https:\/\/generativelanguage\.googleapis\.com(?:\/|$)/.test(auth.baseUrl))&&fetcher){
      const abort=new AbortController(),timer=setTimeout(()=>abort.abort(),8000);
      try{
        const response=await fetcher('https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000',{headers:{'x-goog-api-key':auth.apiKey},signal:abort.signal,redirect:'error'});
        if(!response.ok)throw Error('catalog');
        let text='';if(response.body?.getReader){const reader=response.body.getReader(),decoder=new TextDecoder();let size=0;try{for(;;){const {value,done}=await reader.read();if(done)break;size+=value.byteLength;if(size>1024*1024)throw Error('catalog');text+=decoder.decode(value,{stream:true});}text+=decoder.decode();}finally{await reader.cancel();}}else{text=await response.text();if(text.length>1024*1024)throw Error('catalog');}
        catalog=(JSON.parse(text).models||[]).filter(m=>Array.isArray(m.supportedGenerationMethods)&&m.supportedGenerationMethods.includes('generateContent'))
          .map(m=>({id:String(m.name||'').replace(/^models\//,''),name:String(m.displayName||m.name||'').slice(0,140)}))
          .filter(m=>MODEL.test(m.id)&&m.id.startsWith('gemini-')&&!/(?:tts|image|live|audio|transcrib|computer-use|robotics|embedding|banana)/i.test(m.id)).slice(0,150);
        if(!catalog.length)throw Error('catalog');catalogAt=Date.now();return {ok:true,models:catalog,source:'api-catalog'};
      }catch{return {ok:true,models:nativeOptions(),source:'cli-options',warning:'CATALOG_UNAVAILABLE'};}
      finally{clearTimeout(timer);}
    }
    return {ok:true,models:nativeOptions(),source:'cli-options'};
  }
  const {GeminiChat}=require('./gemini-chat.cjs');
  const chat=new GeminiChat({attachmentRoot:process.env.JOBS_GEMINI_ATTACHMENT_ROOT||__dirname,session:()=>config?.getSessionId?.()||'',status:snapshot,supported:()=>!unsupported,permission:()=>currentPermission()?.value||null,receipt:id=>{const r=receiptStore.get(id);return r?{body:r.body,label:receiptStore.label(r,false)}:null;}});
  const chatUI=(c,...args)=>{attach(c);return chat.observe(c,...args);},chatShell=(...args)=>chat.bindShell(...args);
  async function command(request){
    if(request&&(['terminal-open','terminal-close'].includes(request.op)||request.op?.startsWith('chat-')))return chat.command(request);
    if(request?.op==='receipt'){const r=/^[a-f0-9]{8}$/.test(request.token||'')?receiptStore.token(request.token):null;return r?{ok:true,receipt:{token:r.token,title:r.title,body:r.body,outcome:r.outcome}}:{ok:false,error:'RECEIPT_NOT_FOUND'};}
    if(request?.op==='permission')return {ok:true,permission:unsupported?null:(currentPermission()?.value||null)};
    if(request?.op==='answer')return answerPermission(request.id,request.outcome);
    if(request?.op==='thinking'){
      if(!snapshot().available||state!=='idle'||changing||queued||modeQueued||modeApplying||request.model!==actual())return {ok:false,error:'THINKING_BUSY'};
      try{const value=reasoning.set(request.level,actual());publish(true);return {ok:true,thinking:value};}catch{return {ok:false,error:'THINKING_UNAVAILABLE'};}
    }
    if(request?.op==='models')return models();
    if(request?.op==='model')return changeModel(request.model,request.id);
    if(request?.op==='mode')return changeMode(request.mode,request.id);
    return {ok:false,error:'INVALID_OPERATION'};
  }
  return {attach,ui,chatUI,chatShell,approval,permission,consumePermission,prepareReceipt,receiptSubscribe,receiptText,api,publish,snapshot,command,unsupported(){unsupported=true;queued=null;modeQueued=null;publish(true);}};
}
module.exports={createBridge,safeModel};
if(process.env.JOBS_GEMINI_LIVE==='1'&&typeof process.send==='function'){
  const bridge=createBridge(message=>{if(process.connected)process.send(message,()=>{});});
  globalThis.__jobsGeminiLive=bridge;
  process.on('message',async request=>{
    if(request?.type!=='jobs-gemini-command')return;
    let value;try{value=await bridge.command(request);}catch{value={ok:false,error:'CONTROL_FAILED'};}
    if(process.connected)process.send({type:'jobs-gemini-result',serial:request.serial,value},()=>{});
  });
  setInterval(()=>bridge.publish(),3000).unref();
}
