'use strict';
// OpenCode v2.0.28 adapter. Only local, authenticated servers owned by this invocation.
const fs = require('fs'), path = require('path'), net = require('net'), http = require('http');
const {spawn} = require('child_process'), crypto = require('crypto');
const {accessRules,OpenCodeSessionSettings,SerialControl}=require('./opencode-session-settings.cjs');
const controlQueue=new SerialControl();let sessionSettings;
const MAX = 2 * 1024 * 1024;
const MEDIA_MAX = 40 * 1024 * 1024;
const validID = s => typeof s === 'string' && /^[A-Za-z0-9][A-Za-z0-9_-]{0,127}$/.test(s);
let server, client, socketServer, endpoint, password, connectionRoot, closing = false, reopenClient, approvalControl, chatControl, finishChat, eventRequest, terminalControl, reconnectTimer;
const originalParent=process.ppid;
const parentWatch=setInterval(()=>{if(process.ppid!==originalParent){stop();setTimeout(()=>process.exit(143),500).unref();}},500);parentWatch.unref();
function stop() {
  if (closing) return; closing = true;
  if (socketServer) socketServer.close();
  if (client) client.kill('SIGTERM');
  if (server) server.kill('SIGTERM');
  if(reconnectTimer)clearTimeout(reconnectTimer);if(eventRequest)eventRequest.destroy();if(chatControl)chatControl.close();if(finishChat)finishChat();
}
process.on('SIGTERM', () => {stop(); setTimeout(() => process.exit(143), 500).unref();});
process.on('SIGHUP', () => {stop(); setTimeout(() => process.exit(129), 500).unref();});
process.on('SIGINT', () => {stop(); setTimeout(() => process.exit(130), 500).unref();});
function request(method, route, body) {
  return new Promise((resolve, reject) => {
    const data = body == null ? null : JSON.stringify(body);
    // Native history/inbox/prompt replies can include image base64; Android receives
    // only the bounded chat projection, never these raw media payloads.
    const limit=/^\/api\/session\/[^/?]+\/(?:prompt|inbox|message)(?:[/?]|$)/.test(route)?MEDIA_MAX:MAX;
    const req = http.request(endpoint + route, {method, headers: {
      Authorization: 'Basic ' + Buffer.from('opencode:' + password).toString('base64'),
      'Content-Type': 'application/json', ...(data ? {'Content-Length':Buffer.byteLength(data)} : {})}}, res => {
      let buf = '', size = 0;
      res.on('data', b => {size += b.length; if (size > limit) req.destroy(new Error('Oversized response')); else buf += b;});
      res.on('end', () => {if (res.statusCode >= 400) return reject(new Error('OpenCode HTTP ' + res.statusCode));
        try {resolve(buf ? JSON.parse(buf) : {});} catch (_) {reject(new Error('Invalid response'));}});
    });
    req.setTimeout(30000, () => req.destroy(new Error('OpenCode timeout')));
    req.on('error', reject); req.end(data);
  });
}
function resolvedID(root,id) {
  if (!/^ses[A-Za-z0-9_-]{1,124}$/.test(id)) throw new Error('Invalid session');
  const file=path.join(root,'session-links',id+'.json');
  if(!fs.existsSync(file))return id;
  const value=JSON.parse(fs.readFileSync(file,'utf8')).id;
  if(!/^ses[A-Za-z0-9_-]{1,124}$/.test(value))throw new Error('Invalid session link');
  return value;
}
function rememberID(root,alias,id) {
  if(!/^ses[A-Za-z0-9_-]{1,124}$/.test(id))throw new Error('Invalid native session');
  const dir=path.join(root,'session-links');fs.mkdirSync(dir,{recursive:true,mode:0o700});
  const file=path.join(dir,alias+'.json'),temporary=file+'.'+crypto.randomBytes(8).toString('hex');
  fs.writeFileSync(temporary,JSON.stringify({id}),{mode:0o600,flag:'wx'});fs.renameSync(temporary,file);
}
async function start(root) {
  if (!path.isAbsolute(root)) throw new Error('Invalid connection path');
  fs.mkdirSync(root, {recursive:true, mode:0o700}); fs.chmodSync(root,0o700);connectionRoot=root;
  password = crypto.randomBytes(32).toString('hex');
  const inherited={...process.env};for(const key of Object.keys(inherited))if(key.startsWith('OPENCODE_'))delete inherited[key];
  const env = {...inherited, OPENCODE_SERVER_PASSWORD:password, OPENCODE_DISABLE_AUTOUPDATE:'true',
    XDG_CONFIG_HOME:path.join(root,'config'), XDG_DATA_HOME:path.join(root,'data'),
    XDG_CACHE_HOME:path.join(root,'cache'), XDG_STATE_HOME:path.join(root,'state')};
  // Prevent ambient API keys silently overriding the named connection.
  for (const key of ['OPENAI_API_KEY','ANTHROPIC_API_KEY','GOOGLE_API_KEY','GOOGLE_GENERATIVE_AI_API_KEY','GEMINI_API_KEY','OPENROUTER_API_KEY','OPENCODE_API_KEY']) delete env[key];
  const direct=path.join(process.env.HOME || '', '.local/share/termux-jobs/agents/bin/opencode');
  const local = fs.existsSync(direct)?direct:path.join(process.env.HOME || '', '.local/share/termux-jobs/agents/node_modules/.bin/opencode');
  const exe = fs.existsSync(local) ? local : 'opencode';
  server = spawn(exe, ['serve','--hostname','127.0.0.1','--port','0'], {env, stdio:['ignore','pipe','ignore']});
  server.on('exit', () => {if (client && !closing) client.kill('SIGTERM');});
  await new Promise((resolve,reject) => {
    let data = ''; const timeout = setTimeout(() => reject(new Error('OpenCode startup timeout')),30000);
    server.once('error', e => {clearTimeout(timeout);reject(e);});
    server.once('exit', () => {clearTimeout(timeout);reject(new Error('OpenCode exited before readiness'));});
    server.stdout.on('data', b => {data = (data + b).slice(-4096); const m = data.match(/http:\/\/127\.0\.0\.1:\d+/);
      if (m) {endpoint=m[0];clearTimeout(timeout);resolve();}});
  });
  const info = await request('GET','/api/info');
  if (info.version !== '2.0.28') throw new Error('Required OpenCode v2.0.28');
  return {env,exe};
}
function models(data) {
  // Never expose upstream settings, headers, credentials or secrets to the Android UI.
  return (data.data || []).slice(0,5000).map(m => ({id:m.id, provider:m.providerID, name:m.name || m.id,
    variants:(m.variants || []).map(v => ({id:v.id})), enabled:m.enabled === true}));
}
async function handle(q, session) {
  if(q.op.startsWith('settings-')&&sessionSettings)return sessionSettings.command(q);
  if(session&&['terminal-open','terminal-close'].includes(q.op)&&terminalControl)return terminalControl(q.op==='terminal-open');
  if(session&&q.op.startsWith('chat-')&&chatControl){const r=await chatControl.command(q);if(q.op==='chat-snapshot')r.permission=(await approvalControl.command({op:'permission'})).permission;return r;}
  if(session&&['permission','answer','receipt'].includes(q.op)&&approvalControl)return approvalControl.command(q);
  if (q.op === 'models') return {models:models(await request('GET','/api/model'))};
  if (q.op === 'connect') {
    if (!['openai','anthropic','google','openrouter'].includes(q.provider) || typeof q.key !== 'string' || !q.key.trim() || q.key.length > 4096 || /[\r\n\x00]/.test(q.key)) throw new Error('Invalid credential');
    await request('POST','/api/integration/' + q.provider + '/connect/key',{key:q.key,label:'Termux Jobs'});
    return {stored:true}; // Storing a credential is not a paid inference/key validity check.
  }
  if(q.op === 'chat' && /^ses[A-Za-z0-9_-]{1,124}$/.test(q.id || '')) {const data=await request('GET','/api/session/'+resolvedID(connectionRoot,q.id)),s=data.data || data;return {id:s.id};}
  if (q.op === 'sessions') {
    const data = await request('GET','/api/session');
    return {sessions:(data.data || []).slice(0,500).map(s => ({id:s.id,name:s.title || s.id,directory:s.location && s.location.directory || ''}))};
  }
  if (q.op === 'status' && session) {
    const data = await request('GET','/api/session/' + session), s=data.data || data;
    return {nativeChat:!!chatControl,id:s.id, name:s.title || '', model:s.model || {}, agent:s.agent || 'build',permissions:(s.permissions || []).map(p=>({action:p.action,resource:p.resource,effect:p.effect}))};
  }
  if (q.op === 'model' && session && validID(q.provider) && typeof q.model === 'string' && q.model.length <= 256 && typeof (q.variant || '') === 'string' && (q.variant || '').length <= 80) {
    const model={providerID:q.provider,id:q.model}; if(q.variant)model.variant=q.variant;
    if(q.reopen!==true || !reopenClient)return {applied:false,reason:'reopen_required'};
    return reopenClient(model);
  }
  if (q.op === 'stop' && session) {setTimeout(stop,50);return {stopped:true};}
  throw new Error('Unsupported operation');
}
function readJSON(stream) {return new Promise((resolve,reject) => {
  let data='',size=0;stream.on('data',b=>{size+=b.length;if(size>65536){reject(new Error('Oversized request'));stream.destroy();}else data+=b;});
  stream.on('end',()=>{try{resolve(JSON.parse(data));}catch(_){reject(new Error('Invalid JSON'));}});stream.on('error',reject);
});}
function createChat(request,id,receiptFile,sock){
  // Attachments are staged beside the control socket; receipts live under the
  // connection directory and must not determine the attachment namespace.
  return new (require('./opencode-chat.cjs').OpenCodeChat)(request,id,receiptFile,{attachmentRoot:path.dirname(sock)});
}
async function run(root,sock,id,model,mode,resume,variant,access='ask',scope='unrestricted',headless=false) {
  if (!/^ses[A-Za-z0-9_-]{1,124}$/.test(id) || !['build','plan'].includes(mode)) throw new Error('Invalid session');
  const permissions=accessRules(access,scope);
  const runtime=await start(root);
  const alias=id;id=resolvedID(root,id);
  let found;
  try {found=await request('GET','/api/session/'+id);}catch(e){if(resume==='resume')throw new Error('Saved OpenCode chat not found');}
  if(found) {await request('POST','/api/session/'+id+'/agent',{agent:mode});await request('PATCH','/api/session/'+id,{permissions});if(model){const i=model.indexOf('/');if(i<1)throw new Error('Invalid model');const ref={providerID:model.slice(0,i),id:model.slice(i+1)};if(variant)ref.variant=variant;await request('POST','/api/session/'+id+'/model',{model:ref});}}
  if(!found) {
    const spec={agent:mode,permissions};
    if(model){const i=model.indexOf('/');if(i<1)throw new Error('Model must be provider/model');spec.model={providerID:model.slice(0,i),id:model.slice(i+1)};if(variant)spec.model.variant=variant;}
    const created=await request('POST','/api/session',spec),info=created.data || created;id=info.id;rememberID(root,alias,id);
  }
  const receiptFile=path.join(root,'receipts',id+'.json');
  approvalControl=new (require('./opencode-permissions.cjs').OpenCodePermissions)(request,id,receiptFile);
  if(headless)chatControl=createChat(request,id,receiptFile,sock);
  sessionSettings=new OpenCodeSessionSettings(request,id,scope,async()=>{
    if(!headless||closing||client&&client.exitCode===null&&client.signalCode===null||chatControl&&chatControl.shells.size||await busy())return false;
    const permissions=await request('GET','/api/session/'+id+'/permission'),forms=await request('GET','/api/session/'+id+'/form');
    return !(permissions.data||[]).length&&!(forms.data||[]).some(f=>!f.state||f.state.status==='pending');
  });
  await new Promise((resolve,reject)=>{
    socketServer=net.createServer({allowHalfOpen:true},connection=>{connection.setTimeout(35000,()=>connection.destroy());
      readJSON(connection).then(q=>{if(!['models','status','model','settings-read','settings-mode','settings-permissions','stop','permission','answer','receipt','chat-snapshot','chat-detail','chat-send','chat-interrupt','chat-form','terminal-open','terminal-close'].includes(q.op))throw new Error('Unsupported live operation');return ['chat-send','chat-form','answer','chat-interrupt','model','settings-mode','settings-permissions','terminal-open'].includes(q.op)?controlQueue.run(()=>handle(q,id)):handle(q,id);}).then(result=>connection.end(JSON.stringify({ok:true,...result})))
        .catch(()=>connection.end(JSON.stringify({ok:false,error:'OpenCode request failed'})));
    });socketServer.on('error',reject);socketServer.listen(sock,()=>{fs.chmodSync(sock,0o600);resolve();});
  });
  let reopening=false, finishRun;
  const finished=new Promise(resolve=>{finishRun=resolve;});
  function launchClient() {
    if(closing)throw new Error('OpenCode closing');
    client=spawn(runtime.exe,['mini','--server',endpoint,'--session',id,'--replay'],{env:runtime.env,stdio:'inherit'});
    client.once('error',()=>{if(!reopening&&!headless)finishRun();});
    client.once('exit',()=>{if(!reopening&&!headless)finishRun();});
    const next=client;return new Promise((resolve,reject)=>{next.once('spawn',resolve);next.once('error',reject);});
  }
  async function busy() {
    const active=await request('GET','/api/session/active');
    const inbox=await request('GET','/api/session/'+id+'/inbox');
    return !!(active.data || {})[id] || (headless?(inbox.data || []).some(item=>item.type!=='synthetic'):(inbox.data || []).length>0);
  }
  reopenClient=async model=>{
    if(headless){
      if(closing||await busy())return {applied:false,reason:'busy'};
      if(client&&client.exitCode===null&&client.signalCode===null)return {applied:false,reason:'busy'};
      await request('POST','/api/session/'+id+'/model',{model});
      const r=await request('GET','/api/session/'+id),actual=(r.data||r).model||{};
      return {applied:actual.providerID===model.providerID&&actual.id===model.id,reopened:true,id,model:actual};
    }
    if(reopening || closing || !client || client.exitCode!==null || client.signalCode!==null)return {applied:false,reason:'unavailable'};
    if(await busy())return {applied:false,reason:'busy'};
    if(reopening || closing)return {applied:false,reason:'unavailable'};
    // Only our mini frontend is replaced. The server, session ID and history stay alive.
    reopening=true;const previous=client;
    try {
      await new Promise((resolve,reject)=>{
        const timeout=setTimeout(()=>reject(new Error('Frontend did not exit')),5000);
        previous.once('exit',()=>{clearTimeout(timeout);resolve();});
        if(!previous.kill('SIGTERM')){clearTimeout(timeout);reject(new Error('Frontend not running'));}
      });
      if(closing)return {applied:false,reason:'unavailable'};
      // A prompt could have been admitted just before mini exited. Do not change its model.
      if(await busy()) {await launchClient();return {applied:false,reason:'busy'};}
      await request('POST','/api/session/'+id+'/model',{model});
      const readback=await request('GET','/api/session/'+id),actual=(readback.data || readback).model || {};
      if(actual.providerID!==model.providerID || actual.id!==model.id || (model.variant && actual.variant!==model.variant))throw new Error('Model not confirmed');
      await launchClient();
      return {applied:true,reopened:true,id,model:{providerID:actual.providerID,id:actual.id,variant:actual.variant || ''}};
    } catch(e) {
      if(!closing && previous.exitCode===null && previous.signalCode===null)return {applied:false,reason:'unavailable'};
      if(!closing && client===previous) {try{await launchClient();}catch(_){stop();finishRun();}}
      else if(!closing){stop();finishRun();}
      return {applied:false,reason:'unavailable'};
    } finally {reopening=false;if(closing || (client && (client.exitCode!==null || client.signalCode!==null)))finishRun();}
  };
  if(headless){
    finishChat=finishRun;server.once('exit',finishRun);if(server.exitCode!==null||server.signalCode!==null)throw Error('OpenCode exited');
    const retry=()=>{chatControl.connected=false;if(closing||reconnectTimer)return;reconnectTimer=setTimeout(()=>{reconnectTimer=null;subscribe();},1000);reconnectTimer.unref();};
    const subscribe=()=>{
      if(closing)return;
      eventRequest=http.get(endpoint+'/api/event',{headers:{Authorization:'Basic '+Buffer.from('opencode:'+password).toString('base64')}},res=>{
        chatControl.connected=res.statusCode===200;let buffer='';res.setEncoding('utf8');
        res.on('data',chunk=>{buffer+=chunk.replace(/\r/g,'');if(buffer.length>2*1024*1024){res.destroy();return;}let split;while((split=buffer.indexOf('\n\n'))>=0){const frame=buffer.slice(0,split);buffer=buffer.slice(split+2);const raw=frame.split('\n').filter(l=>l.startsWith('data:')).map(l=>l.slice(5).trimStart()).join('\n');try{const e=JSON.parse(raw);chatControl.event(e);}catch{}}});
        res.on('error',()=>{});res.once('close',retry);
      });eventRequest.on('error',retry);
    };subscribe();
    terminalControl=async open=>{if(open){if(!client||client.exitCode!==null||client.signalCode!==null)await launchClient();return {opened:true};}if(client&&client.exitCode===null&&client.signalCode===null){const previous=client;await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(Error('Frontend did not exit')),5000);previous.once('exit',()=>{clearTimeout(timer);resolve();});previous.kill('SIGTERM');});if(client===previous)client=undefined;}return {closed:true};};
    process.stdout.write('OpenCode chat ready.\r\n');
  }else await launchClient();
  await finished;
  terminalControl=undefined;

  sessionSettings=undefined;reopenClient=undefined;
}
async function main() {
  const [kind,...args]=process.argv.slice(2);
  if(kind==='proxy') {
    const q=await readJSON(process.stdin);
    const result=await new Promise((resolve,reject)=>{let out='',size=0;const c=net.connect(args[0],()=>c.end(JSON.stringify(q)));
      c.setTimeout(35000,()=>c.destroy(new Error('Timeout')));c.on('data',b=>{size+=b.length;if(size>MAX)c.destroy(new Error('Oversized response'));else out+=b;});c.on('end',()=>resolve(out));c.on('error',reject);});
    process.stdout.write(result);return;
  }
  try {
    if(kind==='request') {const q=await readJSON(process.stdin);await start(args[0]);process.stdout.write(JSON.stringify({ok:true,...await handle(q,null)}));}
    else if(kind==='run'||kind==='run-chat') await run(...args,...(kind==='run-chat'?[true]:[]));
    else throw new Error('Unknown mode');
  } finally {
    stop(); if((kind==='run'||kind==='run-chat') && args[1]) {try{fs.unlinkSync(args[1]);}catch(_) {}}
  }
}
if(require.main===module)main().catch(()=>{if(['run','run-chat'].includes(process.argv[2]))process.stderr.write('OpenCode could not start. Install v2.0.28 and Node.js 20+ in Ubuntu; check the project and selected connection.\n');else process.stdout.write(JSON.stringify({ok:false,error:'OpenCode unavailable. Check v2.0.28, Node.js 20+ and Ubuntu.'}));stop();process.exitCode=1;});

module.exports={accessRules,createChat};
