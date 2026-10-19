'use strict';
const crypto=require('node:crypto');
const {ReceiptStore,clean}=require('./agent-receipts.cjs');
class OpenCodePermissions {
 constructor(request,session,file){this.request=request;this.session=session;this.file=file;this.current=null;this.busy=false;}
 async pending(){const response=await this.request('GET','/api/session/'+this.session+'/permission');const rows=response.data;
  if(!Array.isArray(rows))throw Error('Invalid permissions');
  // Security-sensitive scope/trust, MCP and question forms remain in native OpenCode.
  const p=rows.find(p=>p&&p.sessionID===this.session&&/^per[A-Za-z0-9_-]{1,124}$/.test(p.id||'')&&['shell','edit','read','list','glob','grep','webfetch','websearch'].includes(p.action));
  if(!p)return null;const raw=JSON.stringify(p);if(Buffer.byteLength(raw)>20000)return null;return {native:p,raw};
 }
 async command(q){
  if(q.op==='receipt'){if(!/^[a-f0-9]{8}$/.test(q.token||''))throw Error('Invalid receipt');const r=new ReceiptStore(this.file,'O').token(q.token);if(!r)throw Error('Receipt not found');return {receipt:{token:r.token,title:r.title,body:r.body,outcome:r.outcome}};}
  if(q.op==='permission'){if(this.busy)return {permission:null};const p=await this.pending();if(!p){this.current=null;return {permission:null};}
   if(this.current?.raw!==p.raw)this.current={...p,id:crypto.randomUUID()};const c=this.current;
   const summary=typeof c.native.metadata?.command==='string'?'$ '+c.native.metadata.command:(Array.isArray(c.native.resources)?c.native.resources.join('\n'):'');
   const body=clean(summary+'\n\n'+JSON.stringify(c.native,null,2));if(body.length>40000)return {permission:null};
   return {permission:{id:c.id,type:c.native.action==='shell'?'exec':c.native.action==='edit'?'edit':'info',title:clean(c.native.action),body,options:['proceed_once','cancel']}};
  }
  if(q.op!=='answer'||this.busy||!this.current||q.id!==this.current.id||!['proceed_once','cancel'].includes(q.outcome))throw Error('Stale permission');
  const captured=this.current;this.busy=true;
  try{const current=await this.pending();if(!current||current.raw!==captured.raw)throw Error('Changed permission');
   await this.request('POST','/api/session/'+this.session+'/permission/'+captured.native.id+'/reply',{decision:q.outcome==='cancel'?'reject':'once'});
   this.current=null;return {resolved:{id:q.id,outcome:q.outcome}};
  }finally{this.busy=false;}
 }
}
module.exports={OpenCodePermissions};
