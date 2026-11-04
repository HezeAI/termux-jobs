'use strict';
// Isolated native tools + production approval adapters. No accounts, MCP or extensions.
const fs=require('node:fs'),path=require('node:path'),http=require('node:http'),crypto=require('node:crypto'),assert=require('node:assert/strict'),{spawn,spawnSync}=require('node:child_process');
const [agent,kind,where,root,repo,run,profileFile]=process.argv.slice(2);
const profile=profileFile?JSON.parse(fs.readFileSync(profileFile,'utf8')):{};
for(const d of ['home','tmp','config/opencode','data','cache','state','project','outside'])fs.mkdirSync(path.join(root,d),{recursive:true});
const project=path.join(root,'project'),target=path.join(root,where==='inside'?'project':'outside',kind+'.txt');fs.writeFileSync(target,'SYNTHETIC_BEFORE');
// Must precede native module imports. Never inherit a real CLI home or account.
for(const key of Object.keys(process.env))if(!['PATH'].includes(key))delete process.env[key];
Object.assign(process.env,{HOME:path.join(root,'home'),GEMINI_CLI_HOME:path.join(root,'home'),TMPDIR:path.join(root,'tmp'),OTEL_SDK_DISABLED:'true',GEMINI_TELEMETRY_ENABLED:'false'});
assert.equal(spawnSync('git',['init','--quiet',project],{env:{PATH:process.env.PATH,HOME:process.env.HOME,GIT_CONFIG_NOSYSTEM:'1',GIT_CONFIG_GLOBAL:'/dev/null'}}).status,0);process.chdir(project);console.log=(...args)=>process.stderr.write(args.join(' ')+'\n');
let nativeSession=null;let done=null,start,command,cleanup=async()=>{},abort=new AbortController(),inFlight;
let scopeConsent=null;const scopeDecisions=[];const calls=[];const write=(file,value)=>fs.writeFileSync(path.join(root,file),JSON.stringify(value,null,2));
async function codex(){
 const net=require('node:net');const bin=path.join(root,'bin');fs.mkdirSync(bin);const exe='/usr/local/lib/node_modules/@openai/codex/node_modules/@openai/codex-linux-arm64/vendor/aarch64-unknown-linux-musl/bin/codex';fs.symlinkSync(exe,path.join(bin,'codex'));process.env.PATH=bin+':'+process.env.PATH;
 const endpoint=path.join(root,run+'.sock');let issued=false,callsCount=0;
 const provider=http.createServer((req,res)=>{let raw='';req.on('data',b=>raw+=b);req.on('end',()=>{JSON.parse(raw);callsCount++;let item;
  if(!issued){issued=true;item=kind==='write'?{type:'custom_tool_call',id:'patch',call_id:'call_patch',name:'apply_patch',input:'*** Begin Patch\n*** Update File: '+target+'\n@@\n-SYNTHETIC_BEFORE\n+SYNTHETIC_AFTER\n*** End Patch'}:{type:'function_call',id:'shell',call_id:'call_shell',name:'exec_command',arguments:JSON.stringify({cmd:`printf SYNTHETIC_AFTER > '${target}'`,yield_time_ms:1000,max_output_tokens:100})};}
  else item={type:'message',id:'message_'+callsCount,status:'completed',role:'assistant',content:[{type:'output_text',text:'Artificial tool check finished',annotations:[]}]};res.writeHead(200,{'Content-Type':'text/event-stream'});for(const [type,value]of [['response.created',{response:{id:'response_'+callsCount,status:'in_progress'}}],['response.output_item.added',{output_index:0,item}],['response.output_item.done',{output_index:0,item}],['response.completed',{response:{id:'response_'+callsCount,status:'completed',output:[item],usage:{input_tokens:1000,output_tokens:10,total_tokens:1010}}}]])res.write('event: '+type+'\ndata: '+JSON.stringify({type,...value})+'\n\n');res.end();});});await new Promise(r=>provider.listen(0,'127.0.0.1',r));
 fs.writeFileSync(path.join(root,'home/config.toml'),'model="gpt-6.1-sol"\nmodel_provider="fixture"\n[model_providers.fixture]\nname="Artificial local provider"\nbase_url="http://127.0.0.1:'+provider.address().port+'/v1"\nwire_api="responses"\nrequires_openai_auth=false\n');
 const env={PATH:process.env.PATH,HOME:process.env.HOME,CODEX_HOME:process.env.HOME,TMPDIR:path.join(root,'tmp')};const children=[];
 const startChild=(file,args)=>{const child=spawn(process.execPath,[file,...args],{cwd:project,env,stdio:['ignore','ignore',fs.openSync(path.join(root,'codex-launch.stderr'),'a')]});children.push(child);return child;};
 for(const asset of ['codex-chat.cjs','agent-receipts.cjs','agent-action-preview.cjs','agent-completion-times.cjs'])fs.copyFileSync(path.join(repo,'app/src/main/assets',asset),path.join(root,asset));
 fs.copyFileSync(path.join(repo,'app/src/main/assets/codex-control.js'),path.join(root,'codex-wire-v0.2.22.js'));
 let child,chat;
 if(profile.generatedLaunch){
  const command=profile.generatedLaunch.replaceAll('/tmp/termux-jobs-codex',root);
  assert(command.includes('codex-launch-v1.cjs')&&command.includes(endpoint));
  fs.copyFileSync(path.join(repo,'app/src/main/assets/codex-launch.cjs'),path.join(root,'codex-launch-v1.cjs'));
  fs.copyFileSync(path.join(repo,'app/src/main/assets/codex-chat.cjs'),path.join(root,'codex-chat-v1.cjs'));
  fs.copyFileSync(path.join(repo,'app/src/main/assets/codex-control.js'),path.join(root,'bridge-v0.2.21.js'));
  const version=/instructions-\d+/.exec(command)[0],instructions=path.join(root,version);fs.mkdirSync(instructions);
  for(const asset of ['agent-instructions.py','agent-instructions-ubuntu-ai.md'])fs.copyFileSync(path.join(repo,'app/src/main/assets',asset),path.join(instructions,asset));
  fs.writeFileSync(path.join(root,'executed-launch.sh'),command);
  child=spawn('/bin/bash',['--noprofile','--norc','-c',command],{cwd:project,env,stdio:['ignore','ignore',fs.openSync(path.join(root,'codex-launch.stderr'),'a')]});children.push(child);chat=child;
 }else child=startChild(path.join(repo,'app/src/main/assets/codex-launch.cjs'),[endpoint,'danger-full-access',profile.codexApproval||'untrusted','1','en']);
 cleanup=async()=>{for(const c of children.reverse())if(c.exitCode===null&&c.signalCode===null){c.kill('SIGTERM');await new Promise(r=>c.once('exit',r));}provider.closeAllConnections();await new Promise(r=>provider.close(r));};
 for(let n=0;!fs.existsSync(endpoint);n++){assert(n<500&&child.exitCode===null,'Native app-server startup failed');await new Promise(r=>setTimeout(r,20));}
 if(!chat)chat=startChild(path.join(root,'codex-chat.cjs'),['run',endpoint,'full','new','',project,'',profile.codexApproval||'untrusted','en']);
 const request=q=>new Promise((resolve,reject)=>{const socket=net.createConnection(endpoint+'.chat');let raw='';socket.setTimeout(15000,()=>socket.destroy(Error('Test control timeout')));socket.once('connect',()=>socket.write(JSON.stringify(q)+'\n'));socket.on('data',b=>raw+=b);socket.once('end',()=>{try{resolve(JSON.parse(raw));}catch(e){reject(e);}});socket.once('error',reject);});
 let snapshot;for(let n=0;n<600;n++){try{snapshot=await request({op:'chat-snapshot'});if(snapshot.connected)break;}catch{}assert(chat.exitCode===null,'Chat startup exited');await new Promise(r=>setTimeout(r,20));}assert(snapshot?.connected,'Native initialization exchange did not finish');nativeSession=snapshot.session;
 command=q=>{calls.push({op:q.op,outcome:q.outcome});return request(q);};
 start=async()=>{const r=await request({op:'chat-send',id:'msg_'+crypto.randomBytes(16).toString('hex'),kind:'message',session:nativeSession,text:'Perform the artificial '+kind+' operation only.'});assert(r.ok);for(let n=0;n<1000;n++){const snap=await request({op:'chat-snapshot'});if(!snap.busy&&callsCount>=2)return {providerCalls:callsCount,nativePolicy:profile.codexApproval||'untrusted',connected:snap.connected};await new Promise(r=>setTimeout(r,20));}throw Error('Native tool timeout');};
 write('native-identity.json',{version:'0.166.0',requested:profile.codexApproval||'untrusted',bootstrapPolicy:'on-request',transport:'actual Unix WebSocket app-server and production private chat socket',initialized:true,provider:'local artificial; no account'});
}
async function claude(){
 const {ClaudeChat}=require(path.join(repo,'app/src/main/assets/claude-chat.cjs'));let issued=0,count=0;
 const provider=http.createServer((req,res)=>{let raw='';req.on('data',b=>raw+=b);req.on('end',()=>{if(req.url.includes('count_tokens'))return res.end('{"input_tokens":1000}');if(!req.url.includes('/messages')){res.statusCode=404;return res.end('{}');}const q=JSON.parse(raw);count++;const tool=issued<(kind==='write'?2:1),reading=kind==='write'&&issued===0;issued++;const content=tool?{type:'tool_use',id:'call_'+count,name:reading?'Read':kind==='write'?'Write':'Bash',input:reading?{file_path:target}:kind==='write'?{file_path:target,content:'SYNTHETIC_AFTER'}:{command:`printf SYNTHETIC_AFTER > '${target}'`}}:{type:'text',text:'Artificial check finished'};res.writeHead(200,{'Content-Type':'text/event-stream'});const emit=(event,value)=>res.write('event: '+event+'\ndata: '+JSON.stringify(value)+'\n\n');emit('message_start',{type:'message_start',message:{id:'msg_'+count,type:'message',role:'assistant',model:q.model,content:[],stop_reason:null,stop_sequence:null,usage:{input_tokens:1000,output_tokens:1}}});emit('content_block_start',{type:'content_block_start',index:0,content_block:tool?{...content,input:{}}:{type:'text',text:''}});emit('content_block_delta',{type:'content_block_delta',index:0,delta:tool?{type:'input_json_delta',partial_json:JSON.stringify(content.input)}:{type:'text_delta',text:content.text}});emit('content_block_stop',{type:'content_block_stop',index:0});emit('message_delta',{type:'message_delta',delta:{stop_reason:tool?'tool_use':'end_turn',stop_sequence:null},usage:{output_tokens:10}});emit('message_stop',{type:'message_stop'});res.end();});});await new Promise(r=>provider.listen(0,'127.0.0.1',r));
 Object.assign(process.env,{CLAUDE_CONFIG_DIR:path.join(root,'config/claude'),ANTHROPIC_API_KEY:'SYNTHETIC_KEY_ONLY',ANTHROPIC_BASE_URL:'http://127.0.0.1:'+provider.address().port,CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC:'1',DISABLE_AUTOUPDATER:'1'});
 const backend=new ClaudeChat({socket:path.join(root,run+'.sock'),binary:path.join(repo,'.tools/claude-code/claude'),model:'sonnet',mode:profile.claudeMode||'manual',chat:'new',thread:profile.claudeThread||run,language:'en'});cleanup=async()=>{await backend.stop();provider.closeAllConnections();await new Promise(r=>provider.close(r));};await backend.start();assert(backend.ready);nativeSession=backend.session;
 command=async q=>{calls.push({op:q.op,outcome:q.outcome});return {ok:true,...await backend.command(q)};};start=async()=>{assert((await backend.command({op:'chat-send',session:nativeSession,kind:'message',id:'msg_'+crypto.randomBytes(16).toString('hex'),text:'Perform the artificial '+kind+' operation.'})).ok);for(let n=0;n<1000;n++){if(!backend.busy&&count>=(kind==='write'?3:2))return {providerCalls:count,nativePolicy:backend.mode};await new Promise(r=>setTimeout(r,20));}throw Error('Native Claude tool timeout');};write('native-identity.json',{version:'2.1.296',policy:backend.mode,provider:'local artificial; no account'});
}
async function gemini(){
 const core=await import('/root/.local/share/termux-jobs/gemini-cli/0.64.0/bundle/chunk-NR7L3FXV.js');
 const {createBridge}=require(path.join(repo,'app/src/main/assets/gemini-live.cjs'));
 const {permissionReply}=require(path.join(repo,'app/src/main/assets/gemini-control.js'));
 const policy=await core.createPolicyEngineConfig({},profile.geminiMode||'default','/root/.local/share/termux-jobs/gemini-cli/0.64.0/bundle/policies',true);
 const config=new core.Config({sessionId:run,model:'gemini-2.5-flash',targetDir:project,cwd:project,interactive:true,approvalMode:profile.geminiMode||'default',policyEngineConfig:policy,folderTrust:true,mcpEnabled:false,extensionsEnabled:false,enableHooks:false,usageStatisticsEnabled:false,telemetry:{enabled:false,traces:false,logPrompts:false},coreTools:['read_file','write_file','run_shell_command'],useRipgrep:false,checkpointing:false});
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
 assert.equal((await request('GET','/api/info')).version,'2.0.28');const created=(await request('POST','/api/session',{agent:'build',model:{providerID:'audit',id:'audit'},permissions:accessRules(profile.aiAccess||'ask')})).data;assert.deepEqual(created.permissions,accessRules(profile.aiAccess||'ask'));const session=created.id;nativeSession=session;
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
  if(agent==='codex')await codex();else if(agent==='claude')await claude();else if(agent==='gemini')await gemini();else if(agent==='opencode')await opencode();else throw Error('Unknown agent');
  const control=http.createServer((req,res)=>{let body='';req.on('data',b=>body+=b);req.on('end',async()=>{try{
   if(req.url==='/shutdown'){await cleanup();res.end('{}');control.close(()=>process.exit(0));return;}
   if(req.url==='/scope-consent'){const q=JSON.parse(body);assert(!inFlight&&where==='outside'&&agent==='opencode'&&['once','reject'].includes(q.decision));scopeConsent=q.decision;res.end('{}');return;}
   if(req.url==='/start'){assert(!inFlight);inFlight=start().then(value=>{done={...value,contentChanged:fs.readFileSync(target,'utf8')!=='SYNTHETIC_BEFORE',calls,scopeDecisions};write('result.json',done);}).catch(e=>{done={error:String(e.stack||e),calls};write('result.json',done);});res.end('{}');return;}
   if(req.url==='/result'){res.end(JSON.stringify({done}));return;}
   const value=await command(JSON.parse(body));res.end(JSON.stringify(value));
  }catch(e){res.statusCode=500;res.end(JSON.stringify({error:String(e)}));}});});await new Promise(r=>control.listen(0,'127.0.0.1',r));write('control.json',{port:control.address().port});process.stdout.write('CHAIN_READY '+JSON.stringify({port:control.address().port,target,run,nativeSession})+'\n');
 }catch(e){await cleanup();process.stderr.write(String(e)+'\n');process.exitCode=1;}
})();
