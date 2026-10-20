'use strict';
// Private local bridge. No network listener, no credentials, no global config writes.
const net = require('net');
const fs = require('fs');
const crypto = require('crypto');
const readline = require('readline');
const MAX = 2 * 1024 * 1024;
function rpcError(error) {
  const message=String(error?.message || '').toLowerCase();
  let reason='причина не распознана';
  if(/already.*(running|active|loaded|use)|locked|busy/.test(message))reason='чат или хранилище уже заняты';
  else if(/thread not loaded|not materialized|rollout.*not.*(ready|available)/.test(message))reason='история выбранного чата пока недоступна; выберите сохранённый чат или Новый чат';
  else if(/not found|no.*(thread|rollout)|does not exist/.test(message))reason='история чата не найдена сервером';
  else if(/sandbox|approval|permission/.test(message))reason='сервер отклонил параметры доступа';
  else if(/sqlite|database/.test(message))reason='ошибка базы истории';
  else if(/parse|deserialize|invalid.*(thread|rollout|session)/.test(message))reason='несовместимый формат истории';
  else if(/auth|login|credential/.test(message))reason='ошибка авторизации сервера';
  const code=Number.isInteger(error?.code)?error.code:'unknown';
  return '(code='+code+', '+reason+')';
}
class Connection {
  constructor(path, options = {}) {
    this.max = options.largeHistory === true ? 32 * 1024 * 1024 : MAX; this.failed = false;
    this.socket = net.createConnection({path}); this.buffer = Buffer.alloc(0); this.fragments = []; this.size = 0;
    this.pending = new Map(); this.serial = 0; this.upgraded = false;
    this.ready = new Promise((resolve, reject) => { this.resolve = resolve; this.reject = reject; });
    this.key = crypto.randomBytes(16).toString('base64');
    this.timeout = setTimeout(() => this.fail(new Error('Connection timeout')), 20000);
    this.socket.on('connect', () => this.socket.write('GET / HTTP/1.1\r\nHost: localhost\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: '+this.key+'\r\nSec-WebSocket-Version: 13\r\n\r\n'));
    this.socket.on('error', () => this.fail(new Error('Local Codex connection failed')));
    this.socket.on('close', () => this.fail(new Error('Codex disconnected')));
    this.socket.on('data', chunk => { try { this.parse(chunk); } catch(e) { this.fail(e); } });
  }
  fail(error) {
    if (this.failed) return; this.failed = true;
    clearTimeout(this.timeout); this.reject(error);
    for (const item of this.pending.values()) { clearTimeout(item.timer); item.reject(error); } this.pending.clear();
    if (this.onFailure) this.onFailure(error); this.socket.destroy();
  }
  close() { clearTimeout(this.timeout); this.socket.destroy(); }
  send(message) { this.frame(1, Buffer.from(JSON.stringify(message))); }
  frame(opcode, payload) {
    if(payload.length>this.max) throw new Error('Message too large');
    let header;
    if(payload.length<126) header=Buffer.from([0x80|opcode,0x80|payload.length]);
    else if(payload.length<65536) {header=Buffer.alloc(4);header[0]=0x80|opcode;header[1]=254;header.writeUInt16BE(payload.length,2);}
    else {header=Buffer.alloc(10);header[0]=0x80|opcode;header[1]=255;header.writeBigUInt64BE(BigInt(payload.length),2);}
    const mask=crypto.randomBytes(4), body=Buffer.alloc(payload.length);
    for(let i=0;i<payload.length;i++) body[i]=payload[i]^mask[i%4];
    this.socket.write(Buffer.concat([header,mask,body]));
  }
  parse(chunk) {
    this.buffer=Buffer.concat([this.buffer,chunk]);
    if(!this.upgraded) {
      const end=this.buffer.indexOf('\r\n\r\n');
      if(end<0) {if(this.buffer.length>16384) throw new Error('Invalid handshake');return;}
      const header=this.buffer.subarray(0,end).toString('ascii');
      const expected=crypto.createHash('sha1').update(this.key+'258EAFA5-E914-47DA-95CA-C5AB0DC85B11').digest('base64');
      const accept=header.split('\r\n').find(line=>line.toLowerCase().startsWith('sec-websocket-accept:'));
      if(!header.startsWith('HTTP/1.1 101') || !accept || accept.split(':').slice(1).join(':').trim()!==expected) throw new Error('Invalid handshake');
      this.buffer=this.buffer.subarray(end+4);this.upgraded=true;clearTimeout(this.timeout);this.resolve();
    }
    while(this.buffer.length>=2) {
      const first=this.buffer[0], second=this.buffer[1], opcode=first&15;
      if(first&0x70 || second&0x80) throw new Error('Invalid server frame');
      let size=second&127,offset=2;
      if(size===126) {if(this.buffer.length<4)return;size=this.buffer.readUInt16BE(2);offset=4;}
      else if(size===127) {if(this.buffer.length<10)return;const n=this.buffer.readBigUInt64BE(2);if(n>BigInt(this.max))throw new Error('Message too large');size=Number(n);offset=10;}
      if(size>this.max || this.size+size>this.max)throw new Error('Message too large');
      if(this.buffer.length<offset+size)return;
      const data=Buffer.from(this.buffer.subarray(offset,offset+size));this.buffer=this.buffer.subarray(offset+size);
      if(opcode===8) {this.close();return;} if(opcode===9) {this.frame(10,data);continue;} if(opcode===10)continue;
      if(opcode!==0 && opcode!==1)throw new Error('Invalid text frame');
      this.fragments.push(data);this.size+=size;
      if(first&0x80) {
        const message=JSON.parse(Buffer.concat(this.fragments,this.size).toString('utf8'));this.fragments=[];this.size=0;
        const pending=this.pending.get(message.id);
        if(pending && !message.method) {this.pending.delete(message.id);clearTimeout(pending.timer);if(message.error)pending.reject(new Error('Codex rejected '+pending.method+' '+rpcError(message.error)));else pending.resolve(message.result);}
        if(this.onMessage)this.onMessage(message);
      }
    }
  }
  async call(method,params={}) {
    const id=++this.serial;
    const result=new Promise((resolve,reject)=> {const timer=setTimeout(()=>{this.pending.delete(id);reject(new Error('Codex request timeout'));},25000);this.pending.set(id,{resolve,reject,timer,method});});
    this.send({id,method,params});return result;
  }
  async initialize() {await this.ready;await this.call('initialize',{clientInfo:{name:'termux_jobs_control',version:'0.2.21'},capabilities:{experimentalApi:true}});this.send({method:'initialized'});}
}
function writeStatus(path, value) {
  const temporary = path+'.state.json.tmp-'+process.pid;
  fs.writeFileSync(temporary, JSON.stringify({schema:1,updated:Date.now(),...value}), {mode:0o600});
  fs.renameSync(temporary,path+'.state.json');
}
function limitedWindow(window) {
  if (!window || typeof window.usedPercent!=='number' || !Number.isFinite(window.usedPercent) || window.usedPercent<0) return null;
  return {usedPercent:window.usedPercent,resetsAt:Number.isSafeInteger(window.resetsAt)&&window.resetsAt>0?window.resetsAt:null,windowDurationMins:Number.isSafeInteger(window.windowDurationMins)&&window.windowDurationMins>0?window.windowDurationMins:null};
}
function safePlan(value){return typeof value==='string'&&/^[a-z][a-z0-9_-]{0,47}$/.test(value)?value:'';}
async function statusWorker(path, preferred) {
  let stage='initialize';
  const connection=new Connection(path);
  let disconnected=false;
  connection.onFailure=()=>{disconnected=true;};
  try {
    await connection.initialize();
    const models=[];
    let cursor;
    try {
      do {
        const page=await connection.call('model/list',{limit:50,includeHidden:false,...(cursor?{cursor}:{})});
        for(const item of page?.data || []) {
          if(item.hidden || !/^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$/.test(item.model || ''))continue;
          models.push({model:item.model,displayName:String(item.displayName || item.model).slice(0,128),defaultReasoningEffort:item.defaultReasoningEffort,
            supportedReasoningEfforts:(item.supportedReasoningEfforts || []).map(option=>({reasoningEffort:option.reasoningEffort,description:String(option.description || '').slice(0,180)}))});
        }
        cursor=page?.nextCursor;
      } while(cursor && models.length<100);
    } catch(unavailable) { /* Status and limits remain useful without a catalog. */ }
    while(!disconnected) {
      const value={thread:'',model:'',effort:'',state:'',error:'',limits:{},planType:'',accountType:'',models};
      stage='account/rateLimits/read';
      try {
        const response=await connection.call(stage,{});const limits=response?.rateLimitsByLimitId?.codex || response?.rateLimits;
        value.planType=safePlan(limits?.planType);
        for(const name of ['primary','secondary']) {const window=limitedWindow(limits?.[name]);if(window)value.limits[name]=window;}
      } catch(unavailable) {if(disconnected)throw unavailable;}
      stage='account/read';
      try {
        // Extract plan/type only. Never persist account email, identifiers or auth data.
        const account=(await connection.call(stage,{refreshToken:false}))?.account;
        value.accountType=['chatgpt','apiKey'].includes(account?.type)?account.type:'';
        if(value.accountType==='chatgpt')value.planType=safePlan(account.planType)||value.planType;
      } catch(unavailable) {if(disconnected)throw unavailable;}
      stage='thread/loaded/list';
      const ids=(await connection.call(stage,{}))?.data || [];
      for(const id of ids) {
        stage='thread/read';
        const response=await connection.call(stage,{threadId:id,includeTurns:false});
        const thread=response?.thread;
        if(!thread || thread.parentThreadId || (preferred && id!==preferred))continue;
        if(value.thread) {value.thread='';value.error='Несколько чатов: выберите чат в терминале';break;}
        value.materialized=false;try{const rollout=typeof thread.path==='string'?fs.lstatSync(thread.path):null;value.materialized=!!rollout&&rollout.isFile()&&rollout.size>0;}catch{}
        value.thread=id;value.cwd=thread.cwd || '';value.title=String(thread.name || '').slice(0,256);value.model=thread.model || response.model || '';value.effort=thread.reasoningEffort || response.reasoningEffort || '';value.state=thread.status?.type || '';
      }
      writeStatus(path,value);
      // Poll every 30 seconds; a private marker requests an earlier refresh.
      for(let i=0;i<120 && !disconnected;i++) {
        await new Promise(resolve=>setTimeout(resolve,250));
        try {fs.unlinkSync(path+'.refresh');break;}catch(missing) {if(missing.code!=='ENOENT')throw missing;}
      }
    }
    throw new Error('Disconnected');
  } catch(unavailable) {
    writeStatus(path,{error:'Статус Codex недоступен внутри терминала ('+stage+').',limits:{}});
  } finally {connection.onFailure=null;connection.close();}
}
async function main() {
  const [mode,path,access,chat,requested,cwd,confirmedCwd,requestedApproval]=process.argv.slice(2);
  if(!path || !['proxy','resume','status'].includes(mode))throw new Error('Invalid control arguments');
  if(mode==='status') {await statusWorker(path,access || '');return;}
  const connection=new Connection(path);
  if(mode==='proxy') {
    await connection.ready;
    connection.onMessage=message=>process.stdout.write(JSON.stringify(message)+'\n');
    connection.onFailure=()=>process.exit(1);
    const lines=readline.createInterface({input:process.stdin,crlfDelay:Infinity});
    lines.on('line',line=> {try {if(Buffer.byteLength(line)>MAX)throw new Error('Message too large');connection.send(JSON.parse(line));}catch(e){connection.close();process.exit(1);}});
    lines.on('close',()=> {connection.close();process.exit(0);});return;
  }
  try {
    await connection.initialize();
    let id=requested;
    if(chat==='last') {const result=await connection.call('thread/list',{limit:1,sortKey:'updated_at',archived:false,cwd,sourceKinds:['cli','vscode','appServer']});id=result.data?.[0]?.id;}
    if(!id || !/^[A-Za-z0-9][A-Za-z0-9_-]{0,127}$/.test(id))throw new Error('No saved chat in this directory. Select a chat ID.');
    if(!['full','project','read'].includes(access))throw new Error('Invalid access mode');
    const approvalPolicy=requestedApproval ?? (access==='full'?'never':'on-request');
    if(!['never','on-request'].includes(approvalPolicy))throw new Error('Invalid approval policy');
    const sandbox=access==='full'?'danger-full-access':access==='read'?'read-only':'workspace-write';
    const metadata=await connection.call('thread/read',{threadId:id,includeTurns:false});
    const normalize=p=>require('path').posix.normalize(String(p || '')).replace(/\/$/,'') || '/';
    if(!metadata.thread?.cwd || (normalize(metadata.thread.cwd)!==normalize(cwd) && (!confirmedCwd || normalize(metadata.thread.cwd)!==normalize(confirmedCwd))))throw new Error('Каталог чата изменился. Подтвердите исходную папку в настройках сессии.');
    const result=await connection.call('thread/resume',{threadId:id,approvalPolicy,sandbox,excludeTurns:true,cwd});
    const expected=access==='full'?'dangerFullAccess':access==='read'?'readOnly':'workspaceWrite';
    if(!result.cwd || normalize(result.cwd)!==normalize(cwd))throw new Error('Codex did not confirm requested working directory');
    if(result.approvalPolicy!==approvalPolicy || result.sandbox?.type!==expected)throw new Error('Codex did not confirm requested permissions');
    process.stdout.write(id+'\n');
  } finally {connection.close();}
}
if(require.main===module)main().catch(error=> {process.stderr.write('Termux Jobs: '+error.message+'\n');process.exit(1);});
module.exports={Connection,statusWorker};
