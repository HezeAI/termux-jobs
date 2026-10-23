'use strict';
// Read-only metadata RPC against our own private fake server; no real Codex/auth/history.
const http=require('node:http'),crypto=require('node:crypto'),fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),{spawn}=require('node:child_process');
const root=path.resolve(__dirname,'../..'),directory=require('./fixture-paths.cjs').fixture('cxstat');fs.mkdirSync(directory,{recursive:true});
const endpoint=path.join(directory,'status-'+process.pid+'.sock'),bridge=path.join(root,'app/src/main/assets/codex-control.js');
const rollout=path.join(directory,'materialized-'+process.pid+'.jsonl');fs.writeFileSync(rollout,'');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));let phase=0,peer;
const window=(minutes,used)=>({windowDurationMins:minutes,usedPercent:used,resetsAt:1792888860});
const snapshots=[{primary:window(10080,14),secondary:null},{primary:window(300,20),secondary:window(10080,40)},{primary:{usedPercent:14},secondary:null},null,{primary:window(10080,25)}];
const accounts=[{type:'chatgpt',planType:'pro',email:'PRIVATE_ACCOUNT_EMAIL',accountId:'PRIVATE_ID'},{type:'chatgpt',planType:'plus',email:'PRIVATE_ACCOUNT_EMAIL'},null,{type:'apiKey',email:'PRIVATE_ACCOUNT_EMAIL'},{type:'chatgpt',planType:'PRIVATE_ACCOUNT_EMAIL@example.com'}];
const calls=[];const frame=message=>{const data=Buffer.from(JSON.stringify(message));let head=data.length<126?Buffer.from([129,data.length]):Buffer.alloc(4);if(data.length>=126){head[0]=129;head[1]=126;head.writeUInt16BE(data.length,2);}return Buffer.concat([head,data]);};
const server=http.createServer();server.on('upgrade',(request,socket)=>{
 peer=socket;socket.on('error',()=>{});socket.write('HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: '+crypto.createHash('sha1').update(request.headers['sec-websocket-key']+'258EAFA5-E914-47DA-95CA-C5AB0DC85B11').digest('base64')+'\r\n\r\n');let buffer=Buffer.alloc(0);
 socket.on('data',data=>{buffer=Buffer.concat([buffer,data]);while(buffer.length>=2){let size=buffer[1]&127,offset=2;if(size===126){if(buffer.length<4)return;size=buffer.readUInt16BE(2);offset=4;}if(size===127)throw Error('Oversized fixture');if(buffer.length<offset+4+size)return;const mask=buffer.subarray(offset,offset+4),payload=Buffer.from(buffer.subarray(offset+4,offset+4+size));buffer=buffer.subarray(offset+4+size);for(let i=0;i<size;i++)payload[i]^=mask[i%4];const q=JSON.parse(payload);if(q.id===undefined)continue;calls.push(q.method);let result={};
  if(q.method==='model/list')result={data:[]};
  else if(q.method==='account/rateLimits/read')result={rateLimits:{primary:window(60,99)},rateLimitsByLimitId:{codex:snapshots[phase]}};
  else if(q.method==='account/read'){assert.equal(q.params.refreshToken,false);if(phase===2){socket.write(frame({id:q.id,error:{code:-32601,message:'unsupported optional endpoint'}}));continue;}result={account:accounts[phase]};}
  else if(q.method==='thread/loaded/list')result={data:['fixture-thread']};
  else if(q.method==='thread/read')result={thread:{id:'fixture-thread',path:phase===0?undefined:phase===3?null:rollout,model:'fixture-model',reasoningEffort:'medium',cwd:'/fixture',status:{type:'idle'}}};
  else assert.equal(q.method,'initialize','Unexpected write/inference call: '+q.method);
  socket.write(frame({id:q.id,result}));
 }});
});
(async()=>{await new Promise(r=>server.listen(endpoint,r));const child=spawn(process.execPath,[bridge,'status',endpoint,''],{stdio:['ignore','ignore','pipe']});let err='';child.stderr.on('data',d=>err+=d);
 try{
  let previous=0;
  for(phase=0;phase<5;phase++){
   if(phase===4)fs.writeFileSync(rollout,'fixture history');
   if(phase)fs.writeFileSync(endpoint+'.refresh','1');let value;
   for(let i=0;i<100;i++){try{const next=JSON.parse(fs.readFileSync(endpoint+'.state.json'));if(next.updated>previous){value=next;break;}}catch{}await sleep(50);}
   assert(value,'No status: '+err);previous=value.updated;assert.equal(value.thread,'fixture-thread');assert.equal(value.materialized,phase===4);assert(!JSON.stringify(value).includes('PRIVATE_ACCOUNT_EMAIL'));assert(!JSON.stringify(value).includes('PRIVATE_ID'));
   if(phase===0){assert.equal(value.planType,'pro');assert.equal(value.limits.primary.windowDurationMins,10080);assert.equal(value.limits.secondary,undefined);}
   if(phase===1){assert.equal(value.planType,'plus');assert.equal(value.limits.primary.windowDurationMins,300);assert.equal(value.limits.secondary.windowDurationMins,10080);}
   if(phase===2){assert.equal(value.planType,'');assert.equal(value.limits.primary.windowDurationMins,null);}
   if(phase===3){assert.equal(value.planType,'');assert.equal(value.accountType,'apiKey');}
   if(phase===4)assert.equal(value.planType,'');
  }
  assert(calls.includes('account/read'));assert(calls.every(c=>['initialize','model/list','account/rateLimits/read','account/read','thread/loaded/list','thread/read'].includes(c)));
  console.log('PASS: Pro weekly-only primary, standard dual windows, optional account failure, API auth, plan redaction, account changes, no inference/turn/resume/permission calls');
 }finally{child.kill('SIGTERM');await new Promise(r=>child.once('exit',r));peer?.destroy();await new Promise(r=>server.close(r));for(const suffix of ['','.state.json','.refresh'])try{fs.unlinkSync(endpoint+suffix);}catch{}}
})().catch(e=>{console.error(e);process.exitCode=1;});
