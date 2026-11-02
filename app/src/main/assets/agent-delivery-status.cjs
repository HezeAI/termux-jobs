'use strict';
// Presentation evidence only. Does not execute, retry or authorize a command.
const {AsyncLocalStorage}=require('node:async_hooks');
const context=new AsyncLocalStorage(),queues=new WeakMap();
const validId=v=>typeof v==='string'&&/^msg_[A-Za-z0-9_-]{1,124}$/.test(v);
function receipt(raw,clientId){
 if(!validId(clientId)||!raw)return null;
 const r={ok:raw.ok!==false,accepted:raw.ok!==false&&raw.accepted===true,clientId,delivery:['api','queued','current','started','submitted','shell','rejected','unconfirmed'].includes(raw.delivery)?raw.delivery:'submitted'};
 for(const k of ['ack','error'])if(typeof raw[k]==='string'&&/^[A-Za-z0-9:_-]{1,100}$/.test(raw[k]))r[k]=raw[k];
 return r;
}
function attach(rows,receipts){const values=receipts.filter(Boolean);if(!values.length)return;const target=rows.find(r=>r.kind==='user')||rows.find(r=>r.kind==='attachment');if(target)target.deliveryReceipt=values.length===1?values[0]:{ok:true,accepted:values.every(r=>r.accepted),delivery:'batch',receipts:values.slice(0,200)};}
function current(){return context.getStore()||null;}
function run(value,callback){return value&&typeof value.session==='string'&&Array.isArray(value.ids)?context.run(value,callback):callback();}
function historyMeta(){const c=current();return c?{jobsInputSession:c.session,jobsClientIds:c.ids.filter(validId).slice(0,200)}:{};}
function queueContext(next,previous,value){const old=queues.get(previous)||[];queues.set(next,[...old,value&&value.ids?.every(validId)?value:null]);}
function queuedContext(queue,session){const contexts=(queues.get(queue)||[]).filter(v=>v&&v.session===session);return {session,ids:[...new Set(contexts.flatMap(v=>v.ids))].slice(0,200)};}
module.exports={validId,receipt,attach,current,run,historyMeta,queueContext,queuedContext};
