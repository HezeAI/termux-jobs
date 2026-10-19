'use strict';
const fs=require('node:fs'),net=require('node:net'),path=require('node:path');const {StringDecoder}=require('node:string_decoder');const {ClaudeChat,uuid}=require('./claude-chat.cjs');
async function run(socket,model,mode,chat,thread){
 if(!uuid.test(path.basename(socket,'.sock'))||!path.isAbsolute(socket)||fs.existsSync(socket))throw Error('Invalid or occupied Claude socket');const binary=process.env.JOBS_CLAUDE_BINARY;if(!binary||!path.isAbsolute(binary))throw Error('Invalid Claude runtime');
 const backend=new ClaudeChat({socket,binary,model,mode,chat,thread,language:process.env.JOBS_CLAUDE_LANGUAGE||'en'});const peers=new Set();let stopping=false,parent=process.ppid,watch;
 const reply=(peer,r)=>{if(!peer.destroyed){const data=JSON.stringify(r)+'\n';peer.end(Buffer.byteLength(data)<=524288?data:'{"ok":false,"error":"RESPONSE_TOO_LARGE"}\n');}};
 const server=net.createServer(peer=>{peers.add(peer);peer.on('close',()=>peers.delete(peer));peer.on('error',()=>{});peer.setTimeout(30000,()=>peer.destroy());let input='',handled=false;const decoder=new StringDecoder('utf8');peer.on('data',data=>{if(handled)return;input+=decoder.write(data);if(Buffer.byteLength(input)>65536)return peer.destroy();const end=input.indexOf('\n');if(end<0)return;handled=true;let q;try{q=JSON.parse(input.slice(0,end));}catch{return reply(peer,{ok:false,error:'INVALID_REQUEST'});}if(q.op==='stop'){stop(peer);return;}if(stopping)return reply(peer,{ok:false,error:'STOPPING'});Promise.resolve().then(()=>q.op==='native-event'?backend.nativeEvent(q):backend.command(q)).then(r=>reply(peer,r),()=>reply(peer,{ok:false,error:'CONTROL_FAILED'}));});});
 async function stop(peer){if(stopping)return;stopping=true;clearInterval(watch);await backend.stop();if(peer)reply(peer,{ok:true});for(const p of peers)if(p!==peer)reply(p,{ok:false,error:'ENDED'});server.close();try{fs.unlinkSync(socket);}catch{}}
 await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(socket,resolve);});fs.chmodSync(socket,0o600);
 const boot=backend.start().catch(()=>backend.error('Claude initialization failed. Open the terminal to check startup.'));backend.boot=boot;
 for(const signal of ['SIGTERM','SIGINT','SIGHUP'])process.on(signal,()=>stop());watch=setInterval(()=>{if(process.ppid!==parent)stop();},500);
 return {backend,server,stop};
}
function request(socket,q){return new Promise((resolve,reject)=>{const c=net.createConnection(socket);let data='';c.setTimeout(26000,()=>c.destroy(Error('Claude control timeout')));c.on('connect',()=>c.write(JSON.stringify(q)+'\n'));c.on('error',reject);c.on('data',b=>{data+=b.toString('utf8');if(Buffer.byteLength(data)>524288)c.destroy(Error('Oversized reply'));});c.on('end',()=>{try{const r=JSON.parse(data);if(!r.ok)throw Error('Claude control rejected');resolve(r);}catch(e){reject(e);}});});}
module.exports={run,request};
if(require.main===module){const [op,socket,...args]=process.argv.slice(2);(op==='run'?run(socket,...args):op==='stop'?request(socket,{op:'stop'}):Promise.reject(Error('Unknown operation'))).catch(()=>{console.error('Claude chat controller unavailable');process.exitCode=1;});}
