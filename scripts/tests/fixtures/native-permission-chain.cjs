'use strict';
// Isolated native tools + production approval adapters. No accounts, MCP or extensions.
const fs=require('node:fs'),path=require('node:path'),http=require('node:http'),crypto=require('node:crypto'),assert=require('node:assert/strict'),{spawn,spawnSync}=require('node:child_process');
const [agent,kind,where,root,repo,run]=process.argv.slice(2);
for(const d of ['home','tmp','config/opencode','data','cache','state','project','outside'])fs.mkdirSync(path.join(root,d),{recursive:true});
const project=path.join(root,'project'),target=path.join(root,where==='inside'?'project':'outside',kind+'.txt');fs.writeFileSync(target,'SYNTHETIC_BEFORE');
// Must precede native module imports. Never inherit a real CLI home or account.
for(const key of Object.keys(process.env))if(!['PATH'].includes(key))delete process.env[key];
Object.assign(process.env,{HOME:path.join(root,'home'),GEMINI_CLI_HOME:path.join(root,'home'),TMPDIR:path.join(root,'tmp'),OTEL_SDK_DISABLED:'true',GEMINI_TELEMETRY_ENABLED:'false'});
assert.equal(spawnSync('git',['init','--quiet',project],{env:{PATH:process.env.PATH,HOME:process.env.HOME,GIT_CONFIG_NOSYSTEM:'1',GIT_CONFIG_GLOBAL:'/dev/null'}}).status,0);process.chdir(project);console.log=(...args)=>process.stderr.write(args.join(' ')+'\n');
let nativeSession=null;let done=null,start,command,cleanup=async()=>{},abort=new AbortController(),inFlight;
let scopeConsent=null;const scopeDecisions=[];const calls=[];const write=(file,value)=>fs.writeFileSync(path.join(root,file),JSON.stringify(value,null,2));
async function gemini(){
 const core=await import('/root/.local/share/termux-jobs/gemini-cli/0.64.0/bundle/chunk-NR7L3FXV.js');
 const {createBridge}=require(path.join(repo,'app/src/main/assets/gemini-live.cjs'));
 const {permissionReply}=require(path.join(repo,'app/src/main/assets/gemini-control.js'));
 const policy=await core.createPolicyEngineConfig({},'default','/root/.local/share/termux-jobs/gemini-cli/0.64.0/bundle/policies',true);
 const config=new core.Config({sessionId:run,model:'gemini-2.5-flash',targetDir:project,cwd:project,interactive:true,approvalMode:'default',policyEngineConfig:policy,folderTrust:true,mcpEnabled:false,extensionsEnabled:false,enableHooks:false,usageStatisticsEnabled:false,telemetry:{enabled:false,traces:false,logPrompts:false},coreTools:['read_file','write_file','run_shell_command'],useRipgrep:false,checkpointing:false});
 config.setExperiments({flags:{},experimentIds:[]});config.contentGeneratorConfig={authType:'gemini-api-key',apiKey:'SYNTHETIC_KEY_ONLY'};config.contentGenerator={generateContent:async()=>{throw Error("Unexpected inference in native tool fixture");}};
 await config.storage.initialize();const registry=await config.createToolRegistry(),bus=config.getMessageBus();config.toolRegistry=registry;
 // Real native chat/history object, containing only artificial user input; no inference.
 config.getGeminiClient().chat=new core.GeminiChat(config,'',[],[{role:'user',parts:[{text:'Synthetic tool audit only'}]}]);await config.getGeminiClient().chat.initialize();
 const bridge=createBridge(()=>{},null);bridge.attach(config);bridge.ui('waiting_for_confirmation');
 const uiFile='/root/.local/share/termux-jobs/gemini-cli/0.64.0/bundle/interactiveCli-2T27ZS4G.js',original=fs.readFileSync(uiFile,'utf8');
 const begin=original.indexOf('    async (callId, outcome, payload) => {',original.indexOf('  const confirm ='));
 const end=original.indexOf('\n    },',begin)+6;assert(begin>0&&end>begin);
 const callbackSource=original.slice(begin,end);
 const {instrument}=await import(path.join(repo,'app/src/main/assets/gemini-live-loader.mjs'));const hooked=instrument(original,path.basename(uiFile));const mark=hooked.indexOf('  (0, import_react35.useEffect)(() => globalThis.__jobsGeminiLive?.permission(');assert(mark>0);const fragment=hooked.slice(mark,hooked.indexOf('\n',mark));
 let installed,seen='';
 bus.subscribe(core.MessageBusType.TOOL_CALLS_UPDATE,event=>{for(const call of event.toolCalls){if(call.status!=='awaiting_approval'||!call.correlationId||call.correlationId===seen)continue;seen=call.correlationId;
  const nativeTool={...call,callId:call.request.callId};
  const confirm=new Function('toolCalls','config','isDiffingEnabled','ideClient','debugLogger','hasLegacyCallback','return ('+callbackSource+');')([nativeTool],config,false,null,{warn:()=>{}},()=>false);
  new Function('import_react35','globalThis','callId','confirmationDetails','config','confirm','getOptions','isFocused','toolName','deceptiveUrlWarningText',fragment)({useEffect:f=>installed=f()},{__jobsGeminiLive:bridge},call.request.callId,call.confirmationDetails,config,async(id,outcome)=>{bridge.consumePermission(id);return confirm(id,outcome);},()=>[{value:'proceed_once'},{value:'cancel'}],true,call.tool.displayName,null);
 }});
 const scheduler=new core.Scheduler({context:{config,toolRegistry:registry,messageBus:bus},messageBus:bus,schedulerId:'root',getPreferredEditor:()=>undefined});
 command=async q=>{calls.push({op:q.op,outcome:q.outcome});return permissionReply(await bridge.command(q),q);};
 start=()=>scheduler.schedule({callId:crypto.randomUUID(),name:kind==='read'?'read_file':kind==='write'?'write_file':'run_shell_command',args:kind==='read'?{file_path:target}:kind==='write'?{file_path:target,content:'SYNTHETIC_AFTER'}:{command:`printf SYNTHETIC_AFTER > '${target}'`,dir_path:project},isClientInitiated:false,prompt_id:'synthetic'},abort.signal).then(result=>({native:result.map(c=>({status:c.status,response:c.response}))}));
 cleanup=async()=>{abort.abort();if(inFlight)await inFlight.catch(()=>{});if(installed)installed();scheduler.dispose();};
 write('native-identity.json',{version:'0.64.0',uiFile,uiSha256:crypto.createHash('sha256').update(original).digest('hex'),callbackSha256:crypto.createHash('sha256').update(callbackSource).digest('hex'),policy:'default',mcpStarted:false,provider:'none; real native scheduler and synthetic history'});
}
async function opencode(){
 const {accessRules}=require(path.join(repo,'app/src/main/assets/opencode-session-settings.cjs'));
 const {OpenCodePermissions}=require(path.join(repo,'app/src/main/assets/opencode-permissions.cjs'));
 let sent=false,toolSeen=false,providerCalls=0,modelRead=false;
 const provider=http.createServer((req,res)=>{let data='';req.on('data',b=>data+=b);req.on('end',()=>{try{
  const q=JSON.parse(data||'{}');providerCalls++;const tools=q.tools||[];toolSeen||=tools.length>0;modelRead||=JSON.stringify(q.messages||[]).includes('SYNTHETIC_BEFORE');
  const schemas=tools.map(t=>t.function);write('native-tool-schemas.json',schemas);
  res.writeHead(200,{'Content-Type':'text/event-stream'});const emit=(delta,finish)=>res.write('data: '+JSON.stringify({id:'chat_synthetic',object:'chat.completion.chunk',created:1,model:'audit',choices:[{index:0,delta,finish_reason:finish}]})+'\n\n');
  if(tools.length&&!sent){sent=true;const name=kind==='write'?'write':kind;assert(schemas.some(s=>s.name===name));const args=kind==='read'?{path:target}:kind==='write'?{path:target,content:'SYNTHETIC_AFTER'}:{command:`printf SYNTHETIC_AFTER > '${target}'`,workdir:project};emit({role:'assistant',tool_calls:[{index:0,id:'call_audit',type:'function',function:{name,arguments:JSON.stringify(args)}}]},null);emit({},'tool_calls');}
  else emit({role:'assistant',content:'Synthetic done'},'stop');res.end('data: [DONE]\n\n');
 }catch(e){res.destroy();done={error:String(e)};}});});await new Promise(r=>provider.listen(0,'127.0.0.1',r));
 const baseURL='http://127.0.0.1:'+provider.address().port+'/v1';const config={providers:{audit:{canonical:'openai',name:'Synthetic audit',env:['AUDIT_API_KEY'],package:'@opencode/ai/providers/openai-compatible',settings:{baseURL,apiKey:'SYNTHETIC_KEY_ONLY',transport:'http'},models:{audit:{name:'Synthetic audit',limit:{context:16384,output:2048}}}}}};
 fs.writeFileSync(path.join(root,'config/opencode/opencode.json'),JSON.stringify(config));
 const password=crypto.randomBytes(24).toString('hex');const env={PATH:process.env.PATH,HOME:process.env.HOME,TMPDIR:process.env.TMPDIR,XDG_CONFIG_HOME:path.join(root,'config'),XDG_DATA_HOME:path.join(root,'data'),XDG_CACHE_HOME:path.join(root,'cache'),XDG_STATE_HOME:path.join(root,'state'),AUDIT_API_KEY:'SYNTHETIC_KEY_ONLY',OPENCODE_SERVER_PASSWORD:password,OPENCODE_DISABLE_AUTOUPDATE:'true',OPENCODE_DISABLE_MODELS_FETCH:'true'};
 const child=spawn(path.join(repo,'.tools/ai-agents/runtime/opencode'),['serve','--hostname','127.0.0.1','--port','0'],{cwd:project,env,stdio:['ignore','pipe',fs.openSync(path.join(root,'native.stderr'),'w')]});let endpoint;
 cleanup=async()=>{if(child.exitCode===null&&child.signalCode===null){child.kill('SIGTERM');await new Promise(r=>child.once('exit',r));}provider.closeAllConnections();await new Promise(r=>provider.close(r));};
 await new Promise((resolve,reject)=>{let data='';const timer=setTimeout(()=>reject(Error('OpenCode startup timeout')),20000);child.once('error',reject);child.once('exit',()=>reject(Error('OpenCode exited')));child.stdout.on('data',b=>{data=(data+b).slice(-8192);const m=data.match(/http:\/\/127\.0\.0\.1:\d+/);if(m){endpoint=m[0];clearTimeout(timer);resolve();}});});
 const request=(method,route,body)=>new Promise((resolve,reject)=>{const req=http.request(endpoint+route,{method,headers:{Authorization:'Basic '+Buffer.from('opencode:'+password).toString('base64'),'Content-Type':'application/json'}},res=>{let text='';res.on('data',b=>text+=b);res.on('end',()=>{try{if(res.statusCode>=400)throw Error('Native HTTP '+res.statusCode);resolve(JSON.parse(text||'{}'));}catch(e){reject(e);}});});req.setTimeout(10000,()=>req.destroy(Error('timeout')));req.on('error',reject);req.end(body?JSON.stringify(body):undefined);});
 await request('GET','/api/model');const providers=(await request('GET','/api/provider')).data;write('provider-info.json',providers);const providerInfo=providers.find(p=>p.id==='audit');assert.equal(providerInfo?.settings?.baseURL,baseURL,'Reject non-loopback provider before inference');
 assert.equal((await request('GET','/api/info')).version,'2.0.28');const created=(await request('POST','/api/session',{agent:'build',model:{providerID:'audit',id:'audit'},permissions:accessRules('ask')})).data;assert.deepEqual(created.permissions,accessRules('ask'));const session=created.id;nativeSession=session;
 const approvals=new OpenCodePermissions(request,session,path.join(root,'receipts.json'));
 command=async q=>{calls.push({op:q.op,outcome:q.outcome});if(q.op==='permission'){
  const pending=(await request('GET','/api/session/'+session+'/permission')).data||[];
  for(const gate of pending.filter(p=>p.action==='external_directory')){if(scopeConsent){assert(gate.resources.every(p=>p.startsWith(path.join(root,'outside')+'/')),'Refuse scope consent outside this fixture');scopeDecisions.push({action:gate.action,resources:gate.resources,decision:scopeConsent});await request('POST','/api/session/'+session+'/permission/'+gate.id+'/reply',{decision:scopeConsent});}}
  const messages=(await request('GET','/api/session/'+session+'/message')).data||[];
  // Native inbox is observed separately; never use a missing prompt as proof of denial.
  write('native-messages.json',messages);
 }return {ok:true,...await approvals.command(q)};};
 start=async()=>{await request('POST','/api/session/'+session+'/prompt',{text:'Synthetic '+kind+' check only.'});for(let n=0;n<300;n++){
  const inbox=(await request('GET','/api/session/'+session+'/inbox')).data||[];const item=inbox.at(-1);if(item&&['completed','failed','cancelled','rejected'].includes(item.state?.status||item.status)){return {nativeInbox:item,providerCalls,toolSeen,modelRead};}
  const messages=(await request('GET','/api/session/'+session+'/message')).data||[];write('native-messages.json',messages);if(messages.some(m=>m.type==='assistant'&&m.time?.completed&&m.error?.type==='aborted'))return {nativeMessages:messages,providerCalls,toolSeen,modelRead};
  if(sent&&providerCalls>=2){const permissions=(await request('GET','/api/session/'+session+'/permission')).data||[];write('pending-native.json',permissions);if(!permissions.length&&JSON.stringify(messages).includes('Synthetic done'))return {nativeMessages:messages,providerCalls,toolSeen,modelRead};}
  await new Promise(r=>setTimeout(r,100));
 }throw Error('Native completion timeout');};
 write('native-identity.json',{version:'2.0.28',policy:created.permissions,provider:'verified loopback custom provider',mcpConfigured:false});
}
process.on('SIGTERM',()=>{cleanup().finally(()=>process.exit(143));});
(async()=>{
 try{
  if(agent==='gemini')await gemini();else if(agent==='opencode')await opencode();else throw Error('Unknown agent');
  const control=http.createServer((req,res)=>{let body='';req.on('data',b=>body+=b);req.on('end',async()=>{try{
   if(req.url==='/shutdown'){await cleanup();res.end('{}');control.close(()=>process.exit(0));return;}
   if(req.url==='/scope-consent'){const q=JSON.parse(body);assert(!inFlight&&where==='outside'&&agent==='opencode'&&['once','reject'].includes(q.decision));scopeConsent=q.decision;res.end('{}');return;}
   if(req.url==='/start'){assert(!inFlight);inFlight=start().then(value=>{done={...value,contentChanged:fs.readFileSync(target,'utf8')!=='SYNTHETIC_BEFORE',calls,scopeDecisions};write('result.json',done);}).catch(e=>{done={error:String(e.stack||e),calls};write('result.json',done);});res.end('{}');return;}
   if(req.url==='/result'){res.end(JSON.stringify({done}));return;}
   const value=await command(JSON.parse(body));res.end(JSON.stringify(value));
  }catch(e){res.statusCode=500;res.end(JSON.stringify({error:String(e)}));}});});await new Promise(r=>control.listen(0,'127.0.0.1',r));write('control.json',{port:control.address().port});process.stdout.write('CHAIN_READY '+JSON.stringify({port:control.address().port,target,run,nativeSession})+'\n');
 }catch(e){await cleanup();process.stderr.write(String(e)+'\n');process.exitCode=1;}
})();
