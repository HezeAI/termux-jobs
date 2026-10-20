'use strict';
const assert=require('node:assert/strict'),{Connection}=require('../../app/src/main/assets/codex-control.js');
function wire(largeHistory=false){const c=Object.create(Connection.prototype);Object.assign(c,{max:largeHistory?32*1024*1024:2*1024*1024,buffer:Buffer.alloc(0),fragments:[],size:0,upgraded:true,pending:new Map()});return c;}
function frame(data){const h=Buffer.alloc(10);h[0]=129;h[1]=127;h.writeBigUInt64BE(BigInt(data.length),2);return Buffer.concat([h,data]);}
const payload=Buffer.from(JSON.stringify({id:1,result:{output:'x'.repeat(3*1024*1024)}})),encoded=frame(payload);
assert.throws(()=>wire().parse(encoded),/Message too large/);
const c=wire(true);let got;c.onMessage=m=>{got=m;};for(let i=0;i<encoded.length;i+=65536)c.parse(encoded.subarray(i,i+65536));assert.equal(got.result.output.length,3*1024*1024);
const oversized=Buffer.alloc(10);oversized[0]=129;oversized[1]=127;oversized.writeBigUInt64BE(33n*1024n*1024n,2);assert.throws(()=>wire(true).parse(oversized),/Message too large/);
let failures=0,rejected=0,destroyed=0;const f=wire();Object.assign(f,{reject(){rejected++;},socket:{destroy(){destroyed++;}},onFailure(){failures++;}});f.fail(Error('Message too large'));f.fail(Error('Codex disconnected'));assert.equal(failures,1);assert.equal(rejected,1);assert.equal(destroyed,1);
let diagnostic='';const missing=wire();missing.pending.set(7,{method:'thread/read',timer:setTimeout(()=>{},1000),reject(error){diagnostic=error.message;}});missing.parse(frame(Buffer.from(JSON.stringify({id:7,error:{code:-32600,message:'thread not loaded: PRIVATE_THREAD_SENTINEL'}}))));assert(diagnostic.includes('Новый чат'));assert(diagnostic.includes('-32600'));assert(!diagnostic.includes('PRIVATE_THREAD_SENTINEL'));
console.log('PASS history wire: split 3 MiB response accepted only for chat; 33 MiB header refused before allocation; failure notification occurs once');
