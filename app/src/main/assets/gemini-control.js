'use strict';
// Gemini-only CLI owner. It does not use Codex/OpenCode servers or credentials.
const fs = require('node:fs');
const net = require('node:net');
const path = require('node:path');
const os = require('node:os');
const {spawn} = require('node:child_process');
const {safeModel}=require('./gemini-live.cjs');
const safeMode=v=>['default','auto_edit','plan'].includes(v);
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
function argsFor(model, mode, chat, thread) {
  if (!(model===''||safeModel(model)) || !['default', 'auto_edit', 'plan'].includes(mode)
      || !['new', 'choose', 'id'].includes(chat) || !uuid.test(thread)) throw new Error('Invalid Gemini profile');
  const args = ['--approval-mode', mode];
  if (model) args.push('--model', model);
  if (chat === 'new') args.push('--session-id', thread);
  if (chat === 'choose') args.push('--prompt-interactive', '/resume');
  if (chat === 'id') args.push('--resume', thread);
  return args;
}
// Explicit session preference uses the native enforcedType setting, never simulated keys.
function authEnvironment(mode, file, original = process.env) {
  if (!['auto','google','api','vertex'].includes(mode)) throw new Error('Invalid Gemini auth mode');
  const env = {...original};
  if (mode === 'auto') return {env, cleanup:()=>{}};
  // Preserve externally managed settings/policy discovery. Do not replace them.
  if (original.GEMINI_CLI_SYSTEM_SETTINGS_PATH || original.GEMINI_CLI_SYSTEM_DEFAULTS_PATH
      || fs.existsSync('/etc/gemini-cli')) {
    throw new Error('External Gemini system settings detected. Select terminal authentication mode.');
  }
  const type = mode === 'google' ? 'oauth-personal' : mode === 'vertex' ? 'vertex-ai' : 'gemini-api-key';
  if(mode==='vertex'){
    const project=original.JOBS_GEMINI_VERTEX_PROJECT||'',location=original.JOBS_GEMINI_VERTEX_LOCATION||'';
    if(/[\r\n]/.test(project+location)||project&&!/^(?:[a-z][a-z0-9-]{4,28}[a-z0-9]|[0-9]{6,20})$/.test(project)||location&&(location.length>63||!/^(?:global|[a-z]+(?:-[a-z0-9]+){1,4})$/.test(location)))throw new Error('Invalid Vertex AI project or location');
    if(project)env.GOOGLE_CLOUD_PROJECT=project;
    if(location)env.GOOGLE_CLOUD_LOCATION=location;
    env.GOOGLE_GENAI_USE_VERTEXAI='true';
  }
  // Native Gemini rejects system settings with a world-writable ancestor (/tmp).
  // Keep only the control socket in /tmp; settings belong to the private runtime.
  const directory = path.dirname(file);
  fs.mkdirSync(directory, {recursive:true, mode:0o700});
  const info = fs.lstatSync(directory);
  if (!info.isDirectory() || info.isSymbolicLink() || (info.mode & 0o077)) {
    throw new Error('Gemini authentication directory is not private');
  }
  fs.writeFileSync(file, JSON.stringify({security:{auth:{selectedType:type,enforcedType:type}}}), {mode:0o600,flag:'wx'});
  env.GEMINI_CLI_SYSTEM_SETTINGS_PATH = file;
  env.GEMINI_CLI_SYSTEM_DEFAULTS_PATH = '/etc/gemini-cli/system-defaults.json';
  delete env.GEMINI_CLI_AUTH_OVERRIDE;
  return {env, cleanup:()=>{try{fs.unlinkSync(file);}catch{}}};
}
function takeApiKey(file,socket){
  if(!file)return '';
  const directory=path.join(path.dirname(socket),'keys');
  if(path.dirname(file)!==directory||!uuid.test(path.basename(file,'.key'))||path.extname(file)!=='.key')throw Error('Invalid API handoff');
  const folder=fs.lstatSync(directory);if(!folder.isDirectory()||folder.isSymbolicLink()||folder.mode&0o077)throw Error('Invalid API handoff');
  const stat=fs.lstatSync(file);if(!stat.isFile()||stat.isSymbolicLink()||stat.mode&0o077||stat.size<=0||stat.size>4096)throw Error('Invalid API handoff');
  try{const key=fs.readFileSync(file,'utf8');if(!/^[\x21-\x7e]{1,4096}$/.test(key))throw Error('Invalid API handoff');return key;}finally{fs.unlinkSync(file);}
}
// Permission text is never included in the status writer or logs.
function permissionReply(value,request){
  if(!value||value.ok!==true)return {ok:false,error:['STALE_PERMISSION','PERMISSION_FAILED'].includes(value?.error)?value.error:'CONTROL_FAILED'};
  if(request.op==='answer'){
    const r=value.resolved;
    return r&&r.id===request.id&&r.outcome===request.outcome?{ok:true,resolved:{id:r.id,outcome:r.outcome}}:{ok:false,error:'CONTROL_FAILED'};
  }
  if(request.op==='receipt'){const r=value.receipt;return r&&r.token===request.token&&typeof r.title==='string'&&r.title.length<=160&&typeof r.body==='string'&&r.body.length<=40000&&['proceed_once','proceed_always','cancel'].includes(r.outcome)?{ok:true,receipt:{token:r.token,title:r.title,body:r.body,outcome:r.outcome}}:{ok:false,error:'CONTROL_FAILED'};}
  const p=value.permission;if(p==null)return {ok:true,permission:null};
  if(!uuid.test(p.id||'')||!['exec','edit','info'].includes(p.type)||typeof p.body!=='string'||!p.body||p.body.length>40000||typeof p.title!=='string'||p.title.length>80||!Array.isArray(p.options)||p.options.length>3||!p.options.includes('proceed_once')||!p.options.includes('cancel')||p.options.some(v=>!['proceed_once','proceed_always','cancel'].includes(v)))return {ok:false,error:'CONTROL_FAILED'};
  const result={ok:true,permission:{id:p.id,type:p.type,title:p.title,body:p.body,options:p.options}};
  return Buffer.byteLength(JSON.stringify(result),'utf8')<=60000?result:{ok:false,error:'CONTROL_FAILED'};
}
async function run(socket, model, mode, chat, thread, auth = 'auto') {
  process.env.JOBS_GEMINI_ATTACHMENT_ROOT=path.dirname(socket);
  process.env.JOBS_GEMINI_RECEIPT_DIR=require('node:path').join(require('node:path').dirname(socket),'receipts');
  const args = argsFor(model, mode, chat, thread);
  if (process.env.JOBS_GEMINI_DEBUG === '1') args.push('--debug');
  if (!uuid.test(require('node:path').basename(socket, '.sock'))) throw new Error('Invalid control socket');
  // Unique run sockets: refuse to overwrite a controller that might still be live.
  if (fs.existsSync(socket)) throw new Error('Gemini controller socket already exists');
  const binary = process.env.JOBS_GEMINI_BINARY;
  if (!binary || !require('node:path').isAbsolute(binary)) throw new Error('Missing Gemini runtime');
  const authRoot = process.env.JOBS_GEMINI_AUTH_DIR
    || path.join(os.homedir(), '.local/share/termux-jobs/gemini-cli/auth');
  if (!path.isAbsolute(authRoot)) throw new Error('Invalid Gemini authentication directory');
  const selectedKey=auth==='api'?takeApiKey(process.env.JOBS_GEMINI_KEY_FILE,socket):'';
  delete process.env.JOBS_GEMINI_KEY_FILE;
  const authConfig = authEnvironment(auth, path.join(authRoot, path.basename(socket, '.sock') + '.json'),{...process.env,...(selectedKey?{GEMINI_API_KEY:selectedKey}:{})});
  let child, finished = false, stopping = false, timer, watch;
  const peers = new Set(), commands=new Map();let serial=0,childEpoch=0;
  const startedAt=Date.now(),statusPath=socket.replace(/\.sock$/,'.status.json');
  let status={schema:1,available:false,state:'starting',model:'',selectedModel:model,mode,requests:0,input:0,output:0,cache:0,thoughts:0,total:0,apiMs:0,quotaAvailable:false};
  function publish(value){
    // Explicit scalar allowlist. Never write the child's arbitrary IPC/auth payload.
    const next={schema:1,run:path.basename(socket,'.sock'),startedAt,updated:Date.now(),available:value.available===true,quotaAvailable:false};
    for(const key of ['model','selectedModel'])next[key]=safeModel(value[key]);
    next.state=['starting','idle','responding','waiting_for_confirmation','unsupported','ended'].includes(value.state)?value.state:'starting';
    next.mode=['default','autoEdit','auto_edit','plan','yolo'].includes(value.mode)?value.mode:'';
    next.auth=['gemini-api-key','oauth-personal','vertex-ai','gateway','compute-default-credentials'].includes(value.auth)?value.auth:'';
    for(const key of ['requests','input','output','cache','thoughts','total','apiMs'])next[key]=Number.isSafeInteger(value[key])&&value[key]>=0?value[key]:0;
    if(value.thinking&&['default','minimal','low','medium','high'].includes(value.thinking.value)){const t=value.thinking;next.thinking={value:t.value,model:safeModel(t.model),options:Array.isArray(t.options)?t.options.filter(v=>['default','minimal','low','medium','high'].includes(v)).slice(0,5):[],available:!!t.available,effective:!!t.effective};}
    if(value.change&&/^[-a-zA-Z0-9]{1,64}$/.test(value.change.id)&&safeModel(value.change.model)&&['queued','applied','failed','cancelled'].includes(value.change.state))next.change={id:value.change.id,model:value.change.model,state:value.change.state};
    next.permissionId=uuid.test(value.permissionId||'')?value.permissionId:'';
    next.canChangeMode=value.canChangeMode===true;next.canPlan=value.canPlan===true;next.canEdit=value.canEdit===true;
    if(value.modeChange&&/^[-a-zA-Z0-9]{1,64}$/.test(value.modeChange.id)&&safeMode(value.modeChange.mode)&&['queued','applied','failed','cancelled'].includes(value.modeChange.state))next.modeChange={id:value.modeChange.id,mode:value.modeChange.mode,state:value.modeChange.state};
    status=next;
    const temporary=statusPath+'.'+require('node:crypto').randomUUID()+'.tmp';
    try{fs.writeFileSync(temporary,JSON.stringify(status),{mode:0o600,flag:'wx'});fs.renameSync(temporary,statusPath);}catch{}finally{try{fs.unlinkSync(temporary);}catch{}}
  }
  function childRequest(request){return new Promise(resolve=>{
    if(stopping||!child?.connected)return resolve({ok:false,error:'NOT_READY'});
    const id=++serial,epoch=childEpoch;
    const timer=setTimeout(()=>{commands.delete(id);resolve({ok:false,error:'CONTROL_TIMEOUT'});},12000);
    commands.set(id,{resolve,timer,epoch});
    child.send({...request,type:'jobs-gemini-command',serial:id},error=>{if(error){clearTimeout(timer);commands.delete(id);resolve({ok:false,error:'NOT_READY'});}});
  });}
  publish(status);
  const parent = process.ppid;
  const server = net.createServer(peer => {
    peers.add(peer); peer.on('close', () => peers.delete(peer)); peer.on('error', () => {});
    peer.setTimeout(15000, () => peer.destroy());
    let input = '',inputBytes=0;const decoder=new (require('node:string_decoder').StringDecoder)('utf8');
    peer.on('data', data => {
      inputBytes+=data.length;if(inputBytes>65536)return peer.destroy();input+=decoder.write(data);
      if (input.includes('\n')) {
        let request; try { request = JSON.parse(input.split('\n')[0]); } catch { return peer.destroy(); }
        if(peer.jobsHandled)return;peer.jobsHandled=true;
        if(!request||typeof request!=='object'||Array.isArray(request))return peer.end('{\"ok\":false,\"error\":\"INVALID_REQUEST\"}\n');
        if(request.op==='status')return peer.end(JSON.stringify({ok:true,status})+'\n');
        if(request.op==='receipt'&&!/^[a-f0-9]{8}$/.test(request.token||''))return peer.end('{"ok":false}\n');
        if(['chat-snapshot','chat-detail','chat-send','terminal-open','terminal-close'].includes(request.op)){const value={op:request.op};for(const k of ['id','kind','text','message','part','cursor','offset','session','attachments'])if(request[k]!==undefined)value[k]=request[k];childRequest(value).then(value=>{if(!peer.destroyed)peer.end(JSON.stringify(value)+'\n');});return;}
        if(['models','model','mode','thinking','permission','answer','receipt'].includes(request.op)){
          if(request.op==='model'&&(!safeModel(request.model)||!/^[-a-zA-Z0-9]{1,64}$/.test(request.id||'')))return peer.end('{"ok":false,"error":"INVALID_MODEL"}\n');
          if(request.op==='mode'&&(!safeMode(request.mode)||!/^[-a-zA-Z0-9]{1,64}$/.test(request.id||'')))return peer.end('{\"ok\":false,\"error\":\"INVALID_MODE\"}\n');
          if(request.op==='answer'&&(!uuid.test(request.id||'')||!['proceed_once','proceed_always','cancel'].includes(request.outcome)))return peer.end('{"ok":false,"error":"INVALID_PERMISSION"}\n');
          childRequest({op:request.op,model:request.model,level:request.level,mode:request.mode,id:request.id,outcome:request.outcome,token:request.token}).then(value=>{if(['permission','answer','receipt'].includes(request.op))value=permissionReply(value,request);if(!peer.destroyed)peer.end(JSON.stringify(value)+'\n');});return;
        }
        if (request.op !== 'stop') return peer.end('{"ok":false}\n');
        peer.jobsStop=true;stop(); // Acknowledge only after the owned child exits.
      }
    });
  });
  const finish = code => {
    if (finished) return; finished = true;
    clearTimeout(timer); clearInterval(watch);
    for(const task of commands.values()){clearTimeout(task.timer);task.resolve({ok:false,error:'ENDED'});}commands.clear();
    publish({...status,available:false,state:'ended'});
    for (const peer of peers) peer.end(JSON.stringify(peer.jobsStop?{ok:true}:{ok:false,error:'ENDED'})+'\n');
    server.close();
    authConfig.cleanup();
    try { fs.unlinkSync(socket); } catch {}
    process.exitCode = Number.isInteger(code) ? code : (stopping ? 0 : 1);
  };
  const stop = () => {
    if (stopping || finished) return; stopping = true;
    if (!child || child.exitCode !== null || child.signalCode !== null) return finish(0);
    child.kill('SIGTERM');
    timer = setTimeout(() => {
      if (!finished && child.exitCode === null && child.signalCode === null) child.kill('SIGKILL');
    }, 3000);
  };
  try {await new Promise((resolve, reject) => {server.once('error', reject); server.listen(socket, resolve);});}
  catch(error){authConfig.cleanup();throw error;}
  fs.chmodSync(socket, 0o600);
  for (const signal of ['SIGTERM','SIGHUP','SIGINT']) process.on(signal, stop);
  let restarts = 0, lastRestart = 0, authOverride = process.env.GEMINI_CLI_AUTH_OVERRIDE;
  const launch = () => {
    const env = {...authConfig.env, GEMINI_CLI_NO_RELAUNCH:'1',JOBS_GEMINI_LIVE:'1'};
    childEpoch++;publish({...status,available:false,state:'starting'});
    delete env.GEMINI_CLI_AUTH_OVERRIDE;
    if (auth === 'auto' && authOverride) env.GEMINI_CLI_AUTH_OVERRIDE = authOverride;
    const loader=require('node:url').pathToFileURL(path.join(__dirname,'gemini-live-loader.mjs')).href;
    // Node22's supported registration API avoids the experimental-loader warning.
    const registration='data:text/javascript,'+encodeURIComponent('import {register} from \"node:module\";register('+JSON.stringify(loader)+');');
    const next = spawn(process.execPath, ['--require',path.join(__dirname,'gemini-live.cjs'),'--import',registration,binary, ...args], {stdio:['inherit','inherit','inherit','ipc'], env});
    child = next; authOverride = undefined;
    next.on('message', message => {
      if(next!==child||finished||stopping)return;
      if(message?.type==='jobs-gemini-status'&&message.value&&typeof message.value==='object')publish(message.value);
      if(message?.type==='jobs-gemini-result'){
        const task=commands.get(message.serial);if(task&&task.epoch===childEpoch){clearTimeout(task.timer);commands.delete(message.serial);task.resolve(message.value&&typeof message.value==='object'?message.value:{ok:false,error:'INVALID_RESPONSE'});}
      }
      if (message && message.type === 'auth-selected-type' && typeof message.authType === 'string'
          && /^[a-z-]{1,64}$/.test(message.authType)) authOverride = message.authType;
    });
    next.once('error', () => {console.error('Cannot start Gemini CLI'); finish(1);});
    next.once('exit', code => {
      // Official CLI uses 199 after trust/auth changes. Keep one owned control socket.
      for(const task of commands.values()){clearTimeout(task.timer);task.resolve({ok:false,error:'RESTARTED'});}commands.clear();
      if (code === 199 && !stopping && !finished) {
        const now = Date.now(); if (now-lastRestart > 60000) restarts = 0;
        lastRestart = now;
        if (++restarts <= 5) return launch();
        console.error('Gemini CLI restart limit reached');
      }
      finish(code);
    });
  };
  launch();
  watch = setInterval(() => {if (process.ppid !== parent) stop();}, 500);
}
function stopProxy(socket) {
  return new Promise((resolve, reject) => {
    const client = net.createConnection(socket); let response = '';
    client.setTimeout(5500, () => client.destroy(new Error('Gemini stop timeout')));
    client.on('connect', () => client.write('{"op":"stop"}\n'));
    client.on('data', data => {response += data; if (response.length > 1024) client.destroy(new Error('Oversized response'));});
    client.on('error', reject);
    client.on('end', () => {try {if (!JSON.parse(response).ok) throw new Error('Stop failed'); resolve();} catch(e) {reject(e);}});
  });
}
module.exports = {takeApiKey,argsFor, run, stopProxy, authEnvironment, permissionReply};
if (require.main === module) {
  const [op, socket, ...args] = process.argv.slice(2);
  (op === 'run' ? run(socket, ...args) : op === 'stop' ? stopProxy(socket) : Promise.reject(new Error('Unknown operation')))
    .catch(error => {console.error(error.message === 'External Gemini system settings detected. Select terminal authentication mode.' ? error.message : 'Gemini session controller unavailable'); process.exitCode=1;});
}
