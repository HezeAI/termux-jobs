'use strict';
const crypto=require('node:crypto');
function accessRules(access,scope='unrestricted') {
 if(!['full','ask','read'].includes(access)||!['unrestricted','project'].includes(scope))throw Error('Invalid permission scope');
 if(scope==='project')return [{action:'*',resource:'*',effect:'deny'},...['read','glob','grep','question'].map(action=>({action,resource:'*',effect:'allow'})),...(access==='read'?[]:[{action:'edit',resource:'*',effect:access==='ask'?'ask':'allow'}]),{action:'external_directory',resource:'*',effect:'deny'}];
 if(access==='full')return [{action:'*',resource:'*',effect:'allow'}];
 if(access==='ask')return [{action:'*',resource:'*',effect:'ask'}];
 return [{action:'*',resource:'*',effect:'deny'},...['read','glob','grep','question','external_directory'].map(action=>({action,resource:'*',effect:'allow'}))];
}
function normalized(rules){if(!Array.isArray(rules)||rules.length>256)throw Error('Invalid native policy');return rules.map(r=>{if(!r||!['action','resource','effect'].every(k=>typeof r[k]==='string'&&r[k].length<=4096))throw Error('Invalid native policy');return Object.fromEntries(Object.keys(r).sort().map(k=>[k,r[k]]));});}
const same=(a,b)=>JSON.stringify(normalized(a))===JSON.stringify(normalized(b));
class SerialControl {constructor(){this.pending=Promise.resolve();}run(fn){const p=this.pending.then(fn);this.pending=p.catch(()=>{});return p;}}
class OpenCodeSessionSettings {
 constructor(request,session,scope,allowed){if(!/^ses[A-Za-z0-9_-]{1,124}$/.test(session)||!['project','unrestricted'].includes(scope))throw Error('Invalid session');Object.assign(this,{request,session,scope,allowed});this.sent=new Map();}
 async read(){const r=await this.request('GET','/api/session/'+this.session),s=r.data||r;if(s.id!==this.session)throw Error('Foreign session');const rules=normalized(s.permissions),agent=s.agent||'';let access='custom';for(const candidate of ['full','ask','read'])if(same(rules,accessRules(candidate,this.scope)))access=candidate;return {session:this.session,mode:['build','plan'].includes(agent)?agent:'custom',access,scope:this.scope,revision:crypto.createHash('sha256').update(JSON.stringify({agent,rules})).digest('hex'),canChange:await this.allowed()};}
 async command(q){if(q.op==='settings-read')return this.read();if(!['settings-permissions','settings-mode'].includes(q.op)||q.session!==this.session||typeof q.id!=='string'||!/^cfg_[a-zA-Z0-9_-]{1,100}$/.test(q.id)||!/^([0-9a-f]{64})$/.test(q.revision||''))throw Error('Invalid settings request');if(q.op==='settings-permissions'&&!['full','ask','read'].includes(q.access)||q.op==='settings-mode'&&!['build','plan'].includes(q.mode))throw Error('Invalid choice');const raw=JSON.stringify([q.op,q.session,q.revision,q.access,q.mode]);const previous=this.sent.get(q.id);if(previous){if(previous.raw!==raw)throw Error('Changed request');return previous.promise;}const promise=this.apply(q);this.sent.set(q.id,{raw,promise});while(this.sent.size>100)this.sent.delete(this.sent.keys().next().value);return promise;}
 async apply(q){const before=await this.read();if(!before.canChange)return {applied:false,reason:'busy',settings:before};if(q.revision!==before.revision)return {applied:false,reason:'stale',settings:before};if(q.op==='settings-permissions')await this.request('PATCH','/api/session/'+this.session,{permissions:accessRules(q.access,this.scope)});else await this.request('POST','/api/session/'+this.session+'/agent',{agent:q.mode});const after=await this.read();return {applied:q.op==='settings-permissions'?after.access===q.access:after.mode===q.mode,reason:'readback',settings:after};}
}
module.exports={accessRules,OpenCodeSessionSettings,SerialControl};
