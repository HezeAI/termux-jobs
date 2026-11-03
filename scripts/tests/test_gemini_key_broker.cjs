'use strict';
const assert=require('node:assert/strict'),net=require('node:net'),crypto=require('node:crypto');
const {spawn}=require('node:child_process');
const {takeApiKeySocket}=require('../../app/src/main/assets/gemini-control.js');
(async()=>{
 const make=async reply=>{const run=crypto.randomUUID(),address='termux-jobs-gemini-key-'+run;const server=net.createServer(peer=>{let bytes=Buffer.alloc(0);peer.on('error',()=>{});peer.on('data',b=>{bytes=Buffer.concat([bytes,b]);if(bytes.length===38){assert.equal(bytes.readUInt16BE(),36);assert.equal(bytes.toString('ascii',2),run);reply(peer);}});});await new Promise(r=>server.listen('\0'+address,r));return {run,address,server,socket:'/fixture/'+run+'.sock'};};
 const packet=value=>{const bytes=Buffer.from(value),result=Buffer.alloc(bytes.length+4);result.writeInt32BE(bytes.length);bytes.copy(result,4);return result;};
 const good=await make(peer=>peer.end(packet('SYNTHETIC_KEY_ONLY')));
 try{assert.equal(await takeApiKeySocket(good.address,good.socket),'SYNTHETIC_KEY_ONLY');assert.throws(()=>takeApiKeySocket(good.address,'/fixture/'+crypto.randomUUID()+'.sock'),/owner/);}finally{await new Promise(r=>good.server.close(r));}
 const partial=await make(peer=>peer.end(Buffer.from([0,0,0,24,65])));try{await assert.rejects(()=>takeApiKeySocket(partial.address,partial.socket),/unavailable/);}finally{await new Promise(r=>partial.server.close(r));}
 const malformed=await make(peer=>peer.end(packet('bad\nkey')));try{await assert.rejects(()=>takeApiKeySocket(malformed.address,malformed.socket),/unavailable/);}finally{await new Promise(r=>malformed.server.close(r));}
 const unavailable=await make(peer=>{const error=Buffer.alloc(4);error.writeInt32BE(-1);peer.end(error);});try{await assert.rejects(()=>takeApiKeySocket(unavailable.address,unavailable.socket),/saved Gemini API key is unavailable/);}finally{await new Promise(r=>unavailable.server.close(r));}
 const run=crypto.randomUUID();await assert.rejects(()=>takeApiKeySocket('termux-jobs-gemini-key-'+run,'/fixture/'+run+'.sock'),/unavailable/);
 const repeat=await make(peer=>peer.end(packet('SYNTHETIC_RETRY_ONLY')));try{assert.equal(await takeApiKeySocket(repeat.address,repeat.socket),'SYNTHETIC_RETRY_ONLY');}finally{await new Promise(r=>repeat.server.close(r));}
 const crashedRun=crypto.randomUUID(),crashedAddress='termux-jobs-gemini-key-'+crashedRun;
 const child=spawn(process.execPath,['-e',"const net=require('net');net.createServer(()=>{}).listen('\\0'+process.argv[1],()=>process.stdout.write('READY'));",crashedAddress],{env:{PATH:process.env.PATH},stdio:['ignore','pipe','ignore']});
 try{await new Promise((resolve,reject)=>{child.once('error',reject);child.stdout.once('data',resolve);});child.kill('SIGKILL');await new Promise(r=>child.once('exit',r));await assert.rejects(()=>takeApiKeySocket(crashedAddress,'/fixture/'+crashedRun+'.sock'),/unavailable/);}
 finally{if(child.exitCode===null&&child.signalCode===null){child.kill('SIGTERM');await new Promise(r=>child.once('exit',r));}}
 console.log('PASS actual abstract Unix transport: run binding, artificial key, truncated/cancelled response, malformed key, absent/crashed broker, successful new run; no key files');
})().catch(e=>{console.error(e);process.exitCode=1;});
