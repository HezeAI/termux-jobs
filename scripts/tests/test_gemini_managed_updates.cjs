'use strict';
// Exact pinned native settings/update functions. Artificial HOME; no external provider.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),{EventEmitter}=require('node:events');
const {authEnvironment}=require('../../app/src/main/assets/gemini-control.js');
const {GeminiChat}=require('../../app/src/main/assets/gemini-chat.cjs');
const root=require('./fixture-paths.cjs').fixture('gup');
const bundle='/root/.local/share/termux-jobs/gemini-cli/0.64.0/bundle';
const read=n=>fs.readFileSync(path.join(bundle,n),'utf8');
const hashes={};const pinned=n=>{const value=read(n);hashes[n]=crypto.createHash('sha256').update(value).digest('hex');return value;};
(async()=>{
 const {instrument}=await import(path.resolve('app/src/main/assets/gemini-live-loader.mjs'));
 pinned('chunk-QCHFD56F.js');
 const nativeSettings=await import('file://'+bundle+'/chunk-QCHFD56F.js');
 assert.equal(nativeSettings.getSettingsSchema().general.properties.enableAutoUpdate.default,true);
 assert.equal(nativeSettings.getSettingsSchema().general.properties.enableAutoUpdateNotification.default,true);
 fs.mkdirSync(path.join(process.env.HOME,'.gemini'),{recursive:true});
 const userFile=path.join(process.env.HOME,'.gemini/settings.json'),userBytes=JSON.stringify({general:{enableAutoUpdate:true,enableAutoUpdateNotification:true}});fs.writeFileSync(userFile,userBytes);
 for(const mode of ['api','vertex','google','auto'])for(let attempt=0;attempt<2;attempt++){
  const project=path.join(root,mode+'-'+attempt);fs.mkdirSync(project);fs.mkdirSync(path.join(project,'.gemini'));fs.writeFileSync(path.join(project,'.gemini/settings.json'),userBytes);
  const file=path.join(root,mode+'-'+attempt+'.json'),original={GEMINI_CLI_AUTH_OVERRIDE:'oauth-personal'},policy=authEnvironment(mode,file,original);
  try{
   const parsed=JSON.parse(fs.readFileSync(file));assert.deepEqual(parsed.general,{enableAutoUpdate:false,enableAutoUpdateNotification:true});assert.equal(fs.statSync(file).mode&0o777,0o600);
   if(mode==='auto'){assert(!parsed.security);assert.equal(policy.env.GEMINI_CLI_AUTH_OVERRIDE,'oauth-personal');}else assert(parsed.security.auth.enforcedType);
   process.env.GEMINI_CLI_SYSTEM_SETTINGS_PATH=file;process.env.GEMINI_CLI_SYSTEM_DEFAULTS_PATH=policy.env.GEMINI_CLI_SYSTEM_DEFAULTS_PATH;
   const loaded=nativeSettings.loadSettings(project);assert.equal(loaded.merged.general.enableAutoUpdate,false);assert.equal(loaded.merged.general.enableAutoUpdateNotification,true);
   assert.equal(fs.readFileSync(userFile,'utf8'),userBytes);assert.equal(fs.readFileSync(path.join(project,'.gemini/settings.json'),'utf8'),userBytes);
  }finally{policy.cleanup();delete process.env.GEMINI_CLI_SYSTEM_SETTINGS_PATH;delete process.env.GEMINI_CLI_SYSTEM_DEFAULTS_PATH;}assert(!fs.existsSync(file));
 }
 for(const mode of ['api','vertex','google','auto'])assert.throws(()=>authEnvironment(mode,path.join(root,'external.json'),{GEMINI_CLI_SYSTEM_SETTINGS_PATH:'/synthetic/externally-managed.json'}),/Managed CLI policy/);
 const updater=pinned('chunk-DPNFVVVC.js'),start=updater.indexOf('function handleAutoUpdate('),end=updater.indexOf('\n// packages/cli/src/utils/processUtils.ts',start);assert(start>=0&&end>start);
 const events=new EventEmitter(),notices=[],updateStates=[],installed={updateCommand:'npm install -g @google/gemini-cli@latest',packageManager:'npm'};let spawned=0;
 const make=new Function('updateEventEmitter','getInstallationInfo','getChannelFromVersion','RELEASE_CHANNEL_STABILITY','debugLogger','spawnWrapper','setTimeout','let _updateInProgress=false;'+updater.slice(start,end)+';return {handleAutoUpdate,setUpdateHandler};');
 const native=make(events,()=>installed,()=> 'stable',{stable:1},{warn(){}},()=>{spawned++;const p=new EventEmitter();p.unref=()=>{};return p;},()=>{});
 const off=native.setUpdateHandler(v=>notices.push(v),v=>updateStates.push(v));const update={message:'Synthetic newer version',update:{current:'0.64.0',latest:'0.66.0'}};
 const managed={merged:{general:{enableAutoUpdate:false,enableAutoUpdateNotification:true}}};
 assert.equal(native.handleAutoUpdate(update,managed,root,false),undefined);assert.equal(spawned,0);assert.equal(notices.length,0);assert(updateStates.some(v=>v?.isUpdating===false&&v.update.latest==='0.66.0'));
 const old={merged:{general:{enableAutoUpdate:true,enableAutoUpdateNotification:true}}};const processStub=native.handleAutoUpdate(update,old,root,false);assert.equal(spawned,1);processStub.emit('close',1);assert.equal(notices.at(-1).type,'error');assert.match(notices.at(-1).text,/Automatic update failed/);off();
 for(const file of ['interactiveCli-2T27ZS4G.js','interactiveCli-66E5CZDR.js','interactiveCli-IX4FVS6G.js']){
  const source=pinned(file),changed=instrument(source,file);assert(!changed.startsWith('globalThis.__jobsGeminiLive?.unsupported();'));
  const prefix='() => setUpdateHandler((item,...args)',at=changed.indexOf(prefix),stop=changed.indexOf(',\n',at);assert(at>=0&&stop>at);let marked;
  new Function('setUpdateHandler','historyManager','setUpdateInfo','return ('+changed.slice(at,stop)+')();')((add)=>add({type:'error',text:'Synthetic updater notice'},7),{addItem:(item,time)=>{marked=item;assert.equal(time,7);}},()=>{});
  assert.equal(marked.jobsNotice,'cli-update');assert.equal(marked.type,'error');
  const begin=source.indexOf('async function checkForUpdates(settings) {'),finish=source.indexOf('\n// ',begin);let calls=0;
  const check=new Function('getPackageJson','__dirname2','getChannelFromVersion','latestVersion','RELEASE_CHANNEL_STABILITY','import_semver2','debugLogger',source.slice(begin,finish)+';return checkForUpdates;')(()=>({name:'@google/gemini-cli',version:'0.64.0'}),root,()=> 'stable',async()=>{calls++;return '0.66.0';},{stable:1},{default:{gt:()=>true,diff:()=> 'minor'}},{warn(){}});const info=await check(managed);assert.equal(info.update.latest,'0.66.0');assert.equal(calls,1);
 }
 for(const language of ['ru','en']){
  const api={language,attachmentRoot:root,session:()=> 'synthetic-session',status:()=>({available:true,state:'idle'}),supported:()=>true,permission:()=>null,receipt:()=>null};const chat=new GeminiChat(api),data={ready:true,native:false,pending:[],history:[{id:1,type:'error',jobsNotice:'cli-update',text:'Synthetic updater failure'}]};
  chat.observe({},data,async()=>{},false,()=>{});let snap=chat.snapshot({});assert.equal(snap.agentError,false);assert.equal(snap.connected,true);assert.equal(snap.rows[0].kind,'info');assert.equal(snap.rows[0].notice,'cli-update');assert.match(snap.rows[0].body,language==='ru'?/Уведомление обновления/:/update notice/);
  data.history=[{id:2,type:'error',text:'Automatic update failed. This is untagged and stays an error.'},data.history[0]];chat.observe({},data,async()=>{},false,()=>{});snap=chat.snapshot({});assert.equal(snap.agentError,true);assert(snap.rows.some(r=>r.kind==='error'));data.authError=true;data.history=[];chat.observe({},data,async()=>{},false,()=>{});assert.equal(chat.snapshot({}).authError,true);assert.equal(chat.snapshot({}).agentError,true);
 }
 for(const [n,h]of Object.entries(hashes))assert.equal(crypto.createHash('sha256').update(read(n)).digest('hex'),h);
 fs.writeFileSync(path.join(root,'result.json'),JSON.stringify({settingsModes:4,launchesEach:2,updaterSpawnedWithManagedSettings:0,versionCheckPreserved:true,defaultUpdaterFailureIsNonfatal:true,taggedLayouts:3,ruEnNoticeProjection:true,realErrorsPreserved:true,nativeVersion:'0.64.0',hashes},null,2));
 console.log('PASS native settings precedence for 4 modes x 2 launches; no automatic install; version-check preserved; exact updater failure is nonfatal; 3 layout provenance hooks; RU/EN notices and genuine errors; installed CLI unchanged');
})().catch(e=>{console.error(e);process.exitCode=1;});
