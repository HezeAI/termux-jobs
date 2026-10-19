'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {createBridge}=require('../../app/src/main/assets/gemini-live.cjs');
const {permissionReply}=require('../../app/src/main/assets/gemini-control.js');
(async()=>{
 let project='/synthetic',trusted=true,calls=[],resolve;
 const config={model:'flash',getModel:()=> 'flash',setModel(){},getProjectRoot:()=>project,isTrustedFolder:()=>trusted,getDisableAlwaysAllow:()=>false,getContentGeneratorConfig:()=>({authType:'gemini-api-key'}),getContentGenerator:()=>({})};
 const events=[],bridge=createBridge(v=>events.push(v));bridge.attach(config);bridge.ui('waiting_for_confirmation');
 const options=[{value:'proceed_once'},{value:'proceed_always'},{value:'cancel'},{value:'proceed_always_and_save'}];
 const register=(call='call-test',details={type:'exec',command:'SYNTHETIC_PRIVATE_COMMAND'},fn=async(id,outcome)=>{bridge.consumePermission(id);calls.push({id,outcome});},warnings=false,choices=options)=>bridge.permission(call,details,config,fn,choices,true,'Shell',warnings);
 let cleanup=register();const id=bridge.snapshot().permissionId;assert.match(id,/^[0-9a-f-]{36}$/);assert(!JSON.stringify(events).includes('SYNTHETIC_PRIVATE_COMMAND'));assert(!JSON.stringify(events).includes(project));assert(!JSON.stringify(events).includes('call-test'));
 const request=(await bridge.command({op:'permission'})).permission;assert.equal(request.body,'SYNTHETIC_PRIVATE_COMMAND');assert.deepEqual(request.options,['proceed_once','proceed_always','cancel']);
 assert((await bridge.command({op:'answer',id,outcome:'proceed_once'})).ok);assert.equal(calls.length,1);assert.equal((await bridge.command({op:'answer',id,outcome:'proceed_once'})).error,'STALE_PERMISSION');cleanup();
 cleanup=register();const native=bridge.snapshot().permissionId;bridge.consumePermission('call-test');assert.equal((await bridge.command({op:'answer',id:native,outcome:'cancel'})).ok,false);cleanup();
 const old=register();const stale=bridge.snapshot().permissionId;cleanup=register();old();assert.notEqual(bridge.snapshot().permissionId,stale);assert.equal((await bridge.command({op:'answer',id:stale,outcome:'proceed_once'})).ok,false);cleanup();
 cleanup=register();const changed=bridge.snapshot().permissionId;project='/different';assert.equal((await bridge.command({op:'answer',id:changed,outcome:'proceed_once'})).ok,false);project='/synthetic';cleanup();
 for(const d of [{type:'ask_user',command:'x'},{type:'edit',fileName:'x',fileDiff:'diff',isBuildFile:true},{type:'exec',command:'x',untrustedFlags:['unsafe']},{type:'exec',command:'x',modifiedBuildFiles:['build']}]){register('unsupported',d);assert.equal(bridge.snapshot().permissionId,'');}
 register('warnings',{type:'exec',command:'x'},undefined,true);assert.equal(bridge.snapshot().permissionId,'');
 const mutated={type:'exec',command:'old'};cleanup=register('mutated',mutated);const mutatedId=bridge.snapshot().permissionId;mutated.command='different';assert.equal((await bridge.command({op:'answer',id:mutatedId,outcome:'proceed_once'})).ok,false);cleanup();
 cleanup=register('once',{type:'exec',command:'x'},undefined,false,[{value:'proceed_once'},{value:'cancel'}]);assert.equal((await bridge.command({op:'answer',id:bridge.snapshot().permissionId,outcome:'proceed_always'})).ok,false);cleanup();
 cleanup=register();trusted=false;assert.equal((await bridge.command({op:'answer',id:bridge.snapshot().permissionId,outcome:'proceed_always'})).ok,false);trusted=true;cleanup();
 cleanup=register('pending',{type:'exec',command:'slow'},async(id,outcome)=>{bridge.consumePermission(id);calls.push({id,outcome});await new Promise(r=>resolve=r);});const pending=bridge.snapshot().permissionId,promise=bridge.command({op:'answer',id:pending,outcome:'cancel'});assert.equal((await bridge.command({op:'answer',id:pending,outcome:'cancel'})).ok,false);resolve();assert((await promise).ok);cleanup();
 cleanup=register('failure',{type:'exec',command:'x'},async()=>{throw Error('native failed');});assert.equal((await bridge.command({op:'answer',id:bridge.snapshot().permissionId,outcome:'proceed_once'})).error,'PERMISSION_FAILED');cleanup();
 cleanup=register('escaped',{type:'exec',command:'a\x1b\u202eb'});assert((await bridge.command({op:'permission'})).permission.body.includes('\\u202e'));cleanup();
 const valid={id,type:'exec',title:'Shell',body:'private',options:['proceed_once','cancel'],apiKey:'DROP'};const safe=permissionReply({ok:true,permission:valid,apiKey:'DROP'},{op:'permission'});assert(!JSON.stringify(safe).includes('DROP'));
 assert.equal(permissionReply({ok:true,permission:{...valid,options:['proceed_always_and_save']}},{op:'permission'}).ok,false);
 assert.equal(permissionReply({ok:true,resolved:{id,outcome:'cancel'}},{op:'answer',id:'wrong',outcome:'cancel'}).ok,false);
 // Execute exact added hook fragments for all three pinned UI layouts.
 const {instrument}=await import(path.resolve('app/src/main/assets/gemini-live-loader.mjs'));const bundle='/root/.local/share/termux-jobs/gemini-cli/0.64.0/bundle';
 for(const name of ['interactiveCli-2T27ZS4G.js','interactiveCli-66E5CZDR.js','interactiveCli-IX4FVS6G.js']){
  const original=fs.readFileSync(path.join(bundle,name),'utf8'),source=instrument(original,name);assert(!source.startsWith('globalThis.__jobsGeminiLive?.unsupported();'));
  const marker='  (0, import_react35.useEffect)(() => globalThis.__jobsGeminiLive?.permission(',start=source.indexOf(marker),end=source.indexOf('\n',start),fragment=source.slice(start,end);assert(start>=0);
  let installed;new Function('import_react35','globalThis','callId','confirmationDetails','config','confirm','getOptions','isFocused','toolName','deceptiveUrlWarningText',fragment)({useEffect:fn=>installed=fn()},{__jobsGeminiLive:bridge},'hook',{type:'exec',command:'hook-test'},config,async()=>{},()=>options,true,'Shell',null);assert(bridge.snapshot().permissionId);installed();assert.equal(bridge.snapshot().permissionId,'');
  assert(source.includes('globalThis.__jobsGeminiLive?.consumePermission(callId);'));assert.equal(fs.readFileSync(path.join(bundle,name),'utf8'),original);
 }
 console.log('PASS: once/session/deny, native consumption, stale IDs/replacement, project/trust guard, unsupported warnings fallback, single-use and concurrent answers, callback failure, private scalars/reply allowlist, 3 native UI hooks; no SDK writes');
})().catch(e=>{console.error(e);process.exitCode=1;});
