'use strict';
// Synthetic broker only. No CLI, credentials, key files, or application processes.
const assert=require('node:assert/strict'),net=require('node:net'),crypto=require('node:crypto');
const {spawn}=require('node:child_process');
const {once}=require('node:events');
const {takeApiKeySocket}=require('../../app/src/main/assets/gemini-control.js');
const clients=[],connect=net.createConnection;
net.createConnection=function(...args){const peer=connect.apply(this,args);clients.push(peer);return peer;};
const label=()=>{const run=crypto.randomUUID();return {run,address:'termux-jobs-gemini-key-'+run,socket:'/fixture/'+run+'.sock'};};
const packet=()=>{const key=Buffer.from('SYNTHETIC_RETRY_ONLY');const b=Buffer.alloc(4+key.length);b.writeInt32BE(key.length);key.copy(b,4);return b;};
async function broker(reply){
 const target=label(),peers=new Set();let accepted;
 const connected=new Promise(r=>accepted=r);
 const server=net.createServer({allowHalfOpen:true},peer=>{peers.add(peer);peer.on('error',()=>{});peer.on('close',()=>peers.delete(peer));let input=Buffer.alloc(0);peer.on('data',b=>{input=Buffer.concat([input,b]);if(input.length===38){assert.equal(input.readUInt16BE(),36);assert.equal(input.toString('ascii',2),target.run);reply(peer);accepted(peer);}});});
 await new Promise(r=>server.listen('\0'+target.address,r));
 return {...target,peers,connected,close:async()=>{for(const p of peers)p.destroy();await new Promise(r=>server.close(r));}};
}
const read=target=>takeApiKeySocket(target.address,target.socket);
async function retry(){const b=await broker(peer=>peer.end(packet()));try{assert.equal(await read(b),'SYNTHETIC_RETRY_ONLY');}finally{await b.close();}}
(async()=>{
 // Real 10-second consumer timer, not shortened or replaced. A partial body
 // remains open even after the client half-closes its request.
 const stalled=await broker(peer=>peer.write(Buffer.from([0,0,0,24,65])));
 const start=performance.now();const waiting=assert.rejects(()=>read(stalled),e=>e.message==='Gemini API handoff unavailable');
 try{
  const peer=await stalled.connected;const client=clients[0];
  await retry();assert.equal(stalled.peers.size,1,'independent run must not cancel stalled run');
  await waiting;const elapsed=performance.now()-start;assert(elapsed>=9500&&elapsed<15000,'bounded production timeout');
  assert.equal(client.destroyed,true,'production consumer destroys its socket');assert.equal(client.readyState,'closed');
  // allowHalfOpen deliberately leaves the synthetic server write side open.
  // A remote close event would not prove the client teardown; inspect the real
  // production client above, then explicitly close this owned fixture peer.
  const closed=once(peer,'close');peer.destroy();await closed;
  await retry();console.log('PASS partial response timeout; independent run; peer close; fresh retry; elapsedMs='+Math.round(elapsed));
 }finally{await stalled.close();}
 // Kill only the child created here, AFTER its partial response reached Node.
 const target=label();const child=spawn(process.execPath,['-e',
  "const net=require('net');net.createServer({allowHalfOpen:true},p=>{p.on('error',()=>{});p.once('data',()=>{p.write(Buffer.from([0,0,0,24,65]),()=>process.stdout.write('PARTIAL\\n'));});}).listen('\\0'+process.argv[1],()=>process.stdout.write('READY\\n'));",target.address],
  {env:{PATH:process.env.PATH},stdio:['ignore','pipe','ignore']});
 let output='';const events=[];child.stdout.on('data',b=>{output+=b;for(const entry of events)if(output.includes(entry.marker))entry.resolve();});
 const marker=value=>output.includes(value)?Promise.resolve():new Promise(resolve=>events.push({marker:value,resolve}));
 const stable=await broker(peer=>peer.end(packet()));
 try{
  await Promise.race([marker('READY'),once(child,'error').then(([e])=>{throw e;})]);
  const index=clients.length;const failure=assert.rejects(()=>read(target),e=>e.message==='Gemini API handoff unavailable');
  const consumer=clients[index];const received=once(consumer,'data');
  await marker('PARTIAL');const [part]=await received;assert.equal(part.length,5);assert.equal(consumer.bytesRead,5);
  const ended=once(child,'exit');assert(child.kill('SIGKILL'));const [,signal]=await ended;assert.equal(signal,'SIGKILL');await failure;
  assert.equal(await read(stable),'SYNTHETIC_RETRY_ONLY');await retry();
  console.log('PASS owned broker exit after partial response; safe error; unrelated run preserved; fresh retry');
 }finally{if(child.exitCode===null&&child.signalCode===null){const ended=once(child,'exit');child.kill('SIGTERM');await ended;}await stable.close();}
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(()=>{net.createConnection=connect;});
