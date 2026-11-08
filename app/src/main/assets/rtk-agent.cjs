'use strict';
// Optional hooks are process/thread scoped. Never edits config, hooks or instructions.
const fs=require('node:fs'),path=require('node:path'),cp=require('node:child_process');
const COMPONENTS=path.join(process.env.HOME||'/root','.local/share/termux-jobs/components/rtk');
const CURRENT=path.join(COMPONENTS,'1.1.0-jobs.1');
const ROOT=fs.existsSync(CURRENT+'/rtk-mode.py')?CURRENT:path.join(COMPONENTS,'1.1.0');
function binding(){const profile=process.env.JOBS_RTK_PROFILE,run=process.env.JOBS_RTK_RUN;return /^[\da-f-]{36}$/i.test(profile||'')&&/^[\da-f-]{36}$/i.test(run||'')?{profile,run}:null;}
function available(){return !!binding()&&fs.existsSync(ROOT+'/rtk-mode.py');}
function quote(s){return "'"+s.replace(/'/g,"'\\''")+"'";}
function command(agent){const b=binding();return 'python3 -I '+quote(ROOT+'/rtk-mode.py')+' hook '+quote(b.profile)+' '+quote(b.run)+' '+agent;}
function callback(input,agent='claude',owner=process.pid){
 if(!available())return {};
 try{const b=binding(),r=cp.spawnSync('python3',['-I',ROOT+'/rtk-mode.py','hook',b.profile,b.run,agent],{input:JSON.stringify(input),encoding:'utf8',timeout:3000,maxBuffer:65536,env:{...process.env,JOBS_RTK_OWNER_PID:String(owner)}});return r.status===0?JSON.parse(r.stdout):{};}catch{return {};}
}
function claudeHooks(){return available()?{PreToolUse:[{matcher:'Bash',hookCallbackIds:['jobs_rtk_pre']}],PostToolUse:[{matcher:'Bash|Read',hookCallbackIds:['jobs_rtk_post']}]}:{};}
function ready(pid){if(Number.isInteger(pid)&&pid>0)callback({hook_event_name:'SessionStart',statisticsReady:true},'claude',pid);}
async function confirmCodex(rpc,thread){
 const listed=await rpc.call('hooks/list',{threadId:thread});
 const own=(listed.data||[]).flatMap(e=>e.hooks||[]).filter(h=>h.command===command('codex'));
 if(own.length!==3||own.some(h=>!h.enabled||h.trustStatus!=='trusted'))return false;
 callback({hook_event_name:'SessionStart',verified:true,statisticsReady:true},'codex');return true;
}
async function codexConfig(rpc,cwd){
 if(!available())return null;
 // Read resolved configuration for this cwd. Keep all existing inline hooks.
 // No configuration or account data is persisted or logged.
 const r=await rpc.call('config/read',{includeLayers:false,cwd});const config=r.config;
 if(!config||config.features?.codex_hooks===false||config.features?.hooks===false)return null;
 const result={},states={};
 // Native discovery reads each config layer separately. Add only our session layer;
 // copying resolved user hooks would duplicate them and change their trust identity.
 const crypto=require('node:crypto');
 const sorted=v=>Array.isArray(v)?v.map(sorted):v&&typeof v==='object'?Object.fromEntries(Object.keys(v).sort().map(k=>[k,sorted(v[k])])):v;
 for(const [event,label] of [['PreToolUse','pre_tool_use'],['SessionStart','session_start'],['PostToolUse','post_tool_use']]){
  const handler={type:'command',command:command('codex'),timeout:5,async:false};
  const group={...(event==='PreToolUse'?{matcher:'Bash'}:event==='PostToolUse'?{matcher:'Bash|Read'}:{}),hooks:[handler]};
  result['hooks.'+event]=[group];
  const hash='sha256:'+crypto.createHash('sha256').update(JSON.stringify(sorted({event_name:label,...group}))).digest('hex');
  states['/<session-flags>/config.toml:'+label+':0:0']={trusted_hash:hash,enabled:true};
 }
 result['hooks.state']={...(config.hooks?.state||{}),...states};
 return result;
}
module.exports={available,callback,claudeHooks,ready,codexConfig,confirmCodex,command};
