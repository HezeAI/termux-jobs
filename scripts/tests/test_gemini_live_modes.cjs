'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{pathToFileURL}=require('node:url');
const {createBridge}=require('../../app/src/main/assets/gemini-live.cjs');
const tick=()=>new Promise(setImmediate);
(async()=>{
 let actual='default',sets=[],callbacks=[];const config={model:'flash',_activeModel:'flash',getApprovalMode:()=>actual,getContentGenerator:()=>({}),getContentGeneratorConfig:()=>({authType:'gemini-api-key'}),setModel(){},isTrustedFolder:()=>true,isPlanEnabled:()=>true};
 const bridge=createBridge(()=>{});bridge.attach(config);bridge.ui('idle');
 assert.equal((await bridge.command({op:'mode',mode:'auto_edit',id:'no-controller'})).error,'MODE_UNAVAILABLE');
 let cleanup=bridge.approval(config,async m=>{sets.push(m);actual=m;callbacks.push(m);},true,true);
 assert(bridge.snapshot().canChangeMode);assert((await bridge.command({op:'mode',mode:'auto_edit',id:'first'})).ok);assert.equal(actual,'autoEdit');assert.equal(bridge.snapshot().modeChange.state,'applied');assert.deepEqual(callbacks,['autoEdit']);
 bridge.ui('responding');await bridge.command({op:'mode',mode:'plan',id:'queued'});assert.equal(actual,'autoEdit');bridge.ui('waiting_for_confirmation');await tick();assert.equal(actual,'autoEdit');bridge.ui('idle');await tick();assert.equal(actual,'plan');assert.equal(bridge.snapshot().modeChange.state,'applied');
 bridge.ui('responding');await bridge.command({op:'mode',mode:'auto_edit',id:'cancelled'});actual='default';bridge.ui('idle');await tick();assert.equal(actual,'default');assert.equal(bridge.snapshot().modeChange.state,'cancelled');
 bridge.ui('responding');await bridge.command({op:'mode',mode:'auto_edit',id:'replaced'});await bridge.command({op:'mode',mode:'plan',id:'replacement'});bridge.ui('idle');await tick();assert.equal(actual,'plan');assert.equal(bridge.snapshot().modeChange.id,'replacement');
 cleanup();assert(!bridge.snapshot().canChangeMode);assert.equal((await bridge.command({op:'mode',mode:'default',id:'detached'})).ok,false);
 const oldCleanup=bridge.approval(config,()=>{throw Error('native trust/policy rejection');},true,true);const currentCleanup=bridge.approval(config,()=>{throw Error('native trust/policy rejection');},true,true);oldCleanup();assert(bridge.snapshot().canChangeMode);
 await bridge.command({op:'mode',mode:'auto_edit',id:'rejected'});assert.equal(bridge.snapshot().modeChange.state,'failed');assert.equal(actual,'plan');currentCleanup();
 assert.equal((await bridge.command({op:'mode',mode:'yolo',id:'forbidden'})).error,'INVALID_MODE');assert.equal((await bridge.command({op:'mode',mode:'$(bad)',id:'bad'})).error,'INVALID_MODE');
 cleanup=bridge.approval(config,m=>{actual=m;},false,true);await bridge.command({op:'mode',mode:'plan',id:'blocked-until-ui'});assert.equal(bridge.snapshot().modeChange.state,'queued');assert.equal(actual,'plan');cleanup();
 bridge.approval(config,m=>{actual=m;},true,true);await tick();assert.equal(bridge.snapshot().modeChange.state,'applied');
 config.isPlanEnabled=()=>false;assert.equal((await bridge.command({op:'mode',mode:'plan',id:'disabled'})).error,'MODE_UNAVAILABLE');
 const guarded=createBridge(()=>{});actual='default';let project='/old';config.getProjectRoot=()=>project;config.isPlanEnabled=()=>true;guarded.attach(config);guarded.ui('responding');guarded.approval(config,m=>{actual=m;},true,true);await guarded.command({op:'mode',mode:'auto_edit',id:'old-project'});project='/new';guarded.ui('idle');await tick();assert.equal(actual,'default');assert.equal(guarded.snapshot().modeChange.state,'cancelled');
 bridge.unsupported();assert.equal((await bridge.command({op:'mode',mode:'default',id:'unsupported'})).ok,false);
 // Exercise the precise pinned native hook fragment, including state setter + native callback + cleanup.
 const {instrument}=await import(pathToFileURL(path.resolve('app/src/main/assets/gemini-live-loader.mjs')));
 const bundle='/root/.local/share/termux-jobs/gemini-cli/0.64.0/bundle';
 for(const name of ['interactiveCli-2T27ZS4G.js','interactiveCli-66E5CZDR.js','interactiveCli-IX4FVS6G.js']){
  const original=fs.readFileSync(path.join(bundle,name),'utf8'),source=instrument(original,name);assert(!source.startsWith('globalThis.__jobsGeminiLive?.unsupported();'));
  const start=source.indexOf('  (0, import_react106.useEffect)(() => globalThis.__jobsGeminiLive?.approval('),end=source.indexOf('[config,onApprovalModeChange,allowPlanMode,isActive]);',start)+'[config,onApprovalModeChange,allowPlanMode,isActive]);'.length;
  let installed,called=0,policy='',display='';const local={setApprovalMode:m=>policy=m};
  new Function('import_react106','globalThis','config','setApprovalMode','onApprovalModeChange','allowPlanMode','isActive',source.slice(start,end))({useEffect:fn=>{installed=fn();}},{__jobsGeminiLive:{approval:(c,set,plan,active)=>{assert.equal(c,local);assert(plan&&active);set('autoEdit');return()=>called++;}}},local,m=>display=m,m=>{assert.equal(m,'autoEdit');called++;},true,true);
  assert.equal(policy,'autoEdit');assert.equal(display,'autoEdit');assert.equal(called,1);installed();assert.equal(called,2);assert.equal(fs.readFileSync(path.join(bundle,name),'utf8'),original);
 }
 console.log('PASS: native mode bridge, busy/confirmation queue, idle apply, manual override, replacement, native rejection, controller cleanup, native policy callback hooks, no YOLO or SDK writes');
})().catch(e=>{console.error(e);process.exitCode=1;});
