'use strict';
// Per-owned-CLI, in-memory configuration. No settings/auth/SDK files are written.
const levels=['default','minimal','low','medium','high'];
function options(model,definition){
 if(definition?.features?.thinking===false)return [];
 if(/^gemini-3-pro(?:-preview)?$/.test(model))return ['low','high'];
 if(/^gemini-3(?:\.1)?-pro(?:-preview)?$/.test(model)||/^gemini-(?:3\.8-flash|4-argon)(?:-preview)?$/.test(model))return ['low','medium','high'];
 if(/^gemini-3(?:-flash|\.6-flash|\.5-flash-lite)(?:-preview)?$/.test(model))return ['minimal','low','medium','high'];
 if(/^gemini-2\.5-(?:pro|flash|flash-lite)$/.test(model))return ['low','medium','high'];
 return [];
}
function thinking(model,level){
 if(level==='default')return null;
 if(model.startsWith('gemini-2.5-'))return {thinkingBudget:{low:1024,medium:8192,high:model==='gemini-2.5-pro'?32768:24576}[level]};
 return {thinkingLevel:level.toUpperCase()};
}
class GeminiThinking {
 constructor(value='default'){this.value=levels.includes(value)?value:'default';this.services=new WeakMap();this.service=null;}
 attach(config){let service;try{service=config.getModelConfigService?.();}catch{return;}if(!service||typeof service.getResolvedConfig!=='function')return;this.service=service;if(this.services.has(service))return;
  const native=service.getResolvedConfig,owner=this;
  const wrapped=function(context){const resolved=native.call(this,context);if(!context?.isChatModel||context.overrideScope||owner.value==='default')return resolved;const supported=options(resolved.model,this.getModelDefinition?.(resolved.model));if(!supported.includes(owner.value))return resolved;const original=resolved.generateContentConfig||{},previous=original.thinkingConfig||{},next={...previous};delete next.thinkingBudget;delete next.thinkingLevel;Object.assign(next,thinking(resolved.model,owner.value));return {...resolved,generateContentConfig:{...original,thinkingConfig:next}};};
  this.services.set(service,{native,wrapped});service.getResolvedConfig=wrapped;
 }
 status(model){try{const entry=this.services.get(this.service);if(!entry||this.service.getResolvedConfig!==entry.wrapped)return {value:this.value,model:'',options:[],available:false};const resolved=entry.native.call(this.service,{model,isChatModel:true});const list=options(resolved.model,this.service.getModelDefinition?.(resolved.model));return {value:this.value,model:resolved.model,options:['default',...list],available:list.length>0,effective:this.value==='default'||list.includes(this.value)};}catch{return {value:this.value,model:'',options:[],available:false};}}
 set(value,model){const s=this.status(model);if(!s.available||!s.options.includes(value))throw Error('THINKING_UNAVAILABLE');this.value=value;return this.status(model);}
}
module.exports={GeminiThinking,options,thinking,levels};
