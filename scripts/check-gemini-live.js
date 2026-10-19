'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),net=require('node:net');
const {spawn}=require('node:child_process'),{once}=require('node:events'),{pathToFileURL}=require('node:url');
const {createBridge}=require('../app/src/main/assets/gemini-live.cjs');
(async()=>{
 let sent=[],fetches=0,called=[];
 const config={model:'flash',_activeModel:'flash',getApprovalMode:()=> 'default',getContentGenerator:()=>({}),getContentGeneratorConfig:()=>({authType:'gemini-api-key',apiKey:'SYNTHETIC_SECRET'}),setModel(m,temp){called.push([m,temp]);this.model=this._activeModel=m;},getModelConfigService:()=>({getAvailableModelOptions:()=>[{modelId:'flash',name:'Flash'}]})};
 const bridge=createBridge(x=>sent.push(x),async(url,opts)=>{fetches++;assert.equal(new URL(url).hostname,'generativelanguage.googleapis.com');assert.equal(opts.headers['x-goog-api-key'],'SYNTHETIC_SECRET');assert.equal(opts.redirect,'error');return {ok:true,text:async()=>JSON.stringify({models:[{name:'models/gemini-test',supportedGenerationMethods:['generateContent']},{name:'models/gemini-test-tts',supportedGenerationMethods:['generateContent']},{name:'bad',supportedGenerationMethods:['generateContent']}]})};});
 bridge.attach(config);assert.equal(bridge.snapshot().available,false);bridge.ui('idle');assert.equal(bridge.snapshot().available,true);
 bridge.api('request',config,{prompt:'PRIVATE_PROMPT'});bridge.api('response',config,{role:'main',model:'gemini-test',duration_ms:25,usage:{input_token_count:8,output_token_count:3,cached_content_token_count:2,thoughts_token_count:1,total_token_count:12},response:'PRIVATE_REPLY'});
 assert.equal(bridge.snapshot().model,'gemini-test');assert.equal(bridge.snapshot().input,8);assert.equal(bridge.snapshot().requests,1);assert.equal(bridge.snapshot().total,12);
 const serialized=JSON.stringify(sent);for(const secret of ['SYNTHETIC_SECRET','PRIVATE_PROMPT','PRIVATE_REPLY'])assert(!serialized.includes(secret));
 assert.deepEqual((await bridge.command({op:'models'})).models.map(m=>m.id),['gemini-test']);await bridge.command({op:'models'});assert.equal(fetches,1);
 bridge.ui('responding');assert((await bridge.command({op:'model',model:'gemini-new',id:'one'})).ok);assert.equal(called.length,0);bridge.ui('idle');await new Promise(setImmediate);assert.deepEqual(called,[['gemini-new',true]]);assert.equal(bridge.snapshot().change.state,'applied');bridge.api('response',config,{role:'main',model:'gemini-actual',usage:{total_token_count:7}});assert.equal(bridge.snapshot().model,'gemini-actual');assert.equal(bridge.snapshot().total,19);
 bridge.ui('responding');await bridge.command({op:'model',model:'gemini-other',id:'two'});config.model='pro';bridge.ui('idle');await new Promise(setImmediate);assert.equal(called.length,1);assert.equal(bridge.snapshot().change.state,'cancelled');
 assert.equal((await bridge.command({op:'model',model:'x; touch /bad',id:'three'})).ok,false);
 config.getContentGeneratorConfig=()=>({authType:'gemini-api-key',apiKey:'SYNTHETIC_SECRET',baseUrl:'https://custom.example'});
 const custom=createBridge(()=>{},()=>{throw Error('key sent to wrong provider');});custom.attach(config);custom.ui('idle');assert.equal((await custom.command({op:'models'})).source,'cli-options');
 bridge.unsupported();assert.equal((await bridge.command({op:'model',model:'flash',id:'four'})).ok,false);
 const root=path.resolve('.verification/gemini-mode-v1.5.7');fs.mkdirSync(root,{recursive:true});
 const {instrument}=await import(pathToFileURL(path.resolve('app/src/main/assets/gemini-live-loader.mjs')));
 const bundle='/root/.local/share/termux-jobs/gemini-cli/0.64.0/bundle';
 const names=['chunk-J7SXW2FI.js','chunk-NR7L3FXV.js','chunk-PZ4GDG24.js','interactiveCli-2T27ZS4G.js','interactiveCli-66E5CZDR.js','interactiveCli-IX4FVS6G.js'];
 let layouts=0;
 for(const name of names){if(!fs.existsSync(path.join(bundle,name)))continue;const original=fs.readFileSync(path.join(bundle,name),'utf8'),result=instrument(original,name);assert(result.includes('__jobsGeminiLive'));assert(!result.startsWith('globalThis.__jobsGeminiLive?.unsupported();'));const copy=path.join(root,name);fs.writeFileSync(copy,result);const test=require('node:child_process').spawnSync(process.execPath,['--check',copy]);assert.equal(test.status,0,test.stderr?.toString());assert.equal(fs.readFileSync(path.join(bundle,name),'utf8'),original);layouts++;}
 assert(instrument('export const changed=1;',names[0]).startsWith('globalThis.__jobsGeminiLive?.unsupported();'));assert.equal(instrument('unrelated','other.js'),'unrelated');
 // Exercise the inserted native producer hook across committed states, with no temporary consumer mounted.
 for(const name of names.filter(n=>n.startsWith('interactiveCli-'))){
  if(!fs.existsSync(path.join(bundle,name)))continue;
  const source=instrument(fs.readFileSync(path.join(bundle,name),'utf8'),name),start=source.indexOf('  const streamingState = (0, import_react97.useMemo)'),end=source.indexOf('[streamingState]);',start)+'[streamingState]);'.length,fragment=source.slice(start,end);
  const values=[],effects=[];const run=new Function('import_react97','calculateStreamingState','isResponding','toolCalls','globalThis',fragment);
  const react={useMemo:fn=>fn(),useEffect:fn=>effects.push(fn)},globals={__jobsGeminiLive:{ui:v=>values.push(v)}};
  for(const state of ['responding','waiting_for_confirmation','idle']){run(react,()=>state,false,[],globals);assert.equal(values.length,0);}
  for(const commit of effects)commit();assert.deepEqual(values,['responding','waiting_for_confirmation','idle']);
 }
 const empty=createBridge(()=>{});empty.attach({...config,getModelConfigService:()=>({getAvailableModelOptions:()=>[]})});empty.ui('idle');assert.equal((await empty.command({op:'models'})).models.length,3);

 // Real controller + synthetic owned child, no credentials, model inference or installed processes.
 const temp=fs.mkdtempSync(path.join(root,'live-')),run='32345678-1234-4234-8234-123456789abc',socket=path.join(temp,run+'.sock'),stub=path.join(temp,'child.cjs');
 fs.writeFileSync(stub,`const b=globalThis.__jobsGeminiLive,c={model:'flash',_activeModel:'flash',mode:'default',getApprovalMode(){return this.mode;},setApprovalMode(m){this.mode=m;},isTrustedFolder:()=>true,isPlanEnabled:()=>true,getContentGeneratorConfig:()=>({authType:'gemini-api-key'}),getContentGenerator:()=>({}),setModel(m){this.model=this._activeModel=m;},getModelConfigService:()=>({getAvailableModelOptions:()=>[{modelId:'flash',name:'Flash'}]})};b.attach(c);b.approval(c,m=>c.setApprovalMode(m),true,true);b.permission('private-call',{type:'exec',command:'PRIVATE_PERMISSION_SENTINEL'},c,async(id)=>b.consumePermission(id),[{value:'proceed_once'},{value:'cancel'}],true,'Shell',false);b.ui('idle');setInterval(()=>{},1000);`);
 const env={PATH:process.env.PATH,HOME:temp,JOBS_GEMINI_BINARY:stub,JOBS_GEMINI_AUTH_DIR:path.join(temp,'auth')};
 const owner=spawn(process.execPath,[path.resolve('app/src/main/assets/gemini-control.js'),'run',socket,'flash','default','new',run,'auto'],{env,stdio:['ignore','ignore','pipe']});let stderr='';owner.stderr.on('data',d=>stderr+=d);
 const request=q=>new Promise((resolve,reject)=>{const c=net.createConnection(socket),chunks=[];c.setTimeout(4000,()=>c.destroy(Error('timeout')));c.on('connect',()=>c.write(JSON.stringify(q)+'\n'));c.on('data',x=>chunks.push(x));c.on('error',reject);c.on('end',()=>{try{resolve(JSON.parse(Buffer.concat(chunks)));}catch(e){reject(e);}});});
 try{
  let ready=false;for(let i=0;i<80;i++){await new Promise(r=>setTimeout(r,50));try{ready=(await request({op:'status'})).status.available;if(ready)break;}catch{}}assert(ready);
  assert.equal((await request({op:'models'})).models[0].id,'flash');assert((await request({op:'model',model:'gemini-new',id:'integration'})).ok);
  const status=(await request({op:'status'})).status;assert.equal(status.model,'gemini-new');assert.equal(status.change.state,'applied');assert.equal(status.run,run);assert((await request({op:'mode',mode:'auto_edit',id:'mode-integration'})).ok);const mode=(await request({op:'status'})).status;assert.equal(mode.mode,'autoEdit');assert.equal(mode.modeChange.state,'applied');assert.equal(mode.modeChange.mode,'auto_edit');assert(mode.canChangeMode);assert.equal((await request({op:'mode',mode:'yolo',id:'blocked'})).error,'INVALID_MODE');assert.equal((await request(null)).error,'INVALID_REQUEST');assert((await request({op:'status'})).status.available);
  const pendingStatus=(await request({op:'status'})).status;assert(pendingStatus.permissionId);assert(!JSON.stringify(pendingStatus).includes('PRIVATE_PERMISSION_SENTINEL'));const prompt=(await request({op:'permission'})).permission;assert.equal(prompt.body,'PRIVATE_PERMISSION_SENTINEL');assert.equal(prompt.id,pendingStatus.permissionId);const resolved=await request({op:'answer',id:prompt.id,outcome:'proceed_once'});assert(resolved.ok);assert.equal(resolved.resolved.id,prompt.id);assert.equal((await request({op:'answer',id:prompt.id,outcome:'cancel'})).error,'STALE_PERMISSION');assert.equal((await request({op:'answer',id:prompt.id,outcome:'proceed_always_and_save'})).error,'INVALID_PERMISSION');
  const file=socket.replace(/\.sock$/,'.status.json');assert.equal(fs.statSync(file).mode&0o777,0o600);assert(!fs.readFileSync(file,'utf8').includes('apiKey'));assert(!fs.readFileSync(file,'utf8').includes('PRIVATE_PERMISSION_SENTINEL'));
  assert.equal((await request({op:'model',model:'bad;',id:'bad'})).ok,false);const exit=once(owner,'exit');assert((await request({op:'stop'})).ok);await exit;assert(!fs.existsSync(socket));assert(!stderr.includes('ExperimentalWarning'),stderr);
 }finally{if(owner.exitCode===null&&owner.signalCode===null)owner.kill('SIGTERM');}
 console.log(`PASS: Gemini scalar privacy, real setter/idle queue, cancellation, catalog isolation, ${layouts} pinned layouts, no SDK writes, unsupported fallback, controller/socket/status/model integration and owned shutdown`);
})().catch(e=>{console.error(e);process.exitCode=1;});
