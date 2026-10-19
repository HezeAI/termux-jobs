'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),{execFileSync}=require('node:child_process'),{pathToFileURL}=require('node:url');
(async()=>{
 const {instrument}=await import(pathToFileURL(path.resolve('app/src/main/assets/gemini-live-loader.mjs')));const bundle=path.resolve('.tools/gemini-cli/package/bundle'),out=path.resolve('.verification/gemini-chat-v1.5.17/pinned-syntax');fs.mkdirSync(out,{recursive:true});let count=0;
 for(const name of fs.readdirSync(bundle).filter(n=>/^interactiveCli-.*\.js$/.test(n))){
  const original=fs.readFileSync(path.join(bundle,name),'utf8'),source=instrument(original,name);assert(!source.startsWith('globalThis.__jobsGeminiLive?.unsupported();'));const copy=path.join(out,name+'.mjs');fs.writeFileSync(copy,source);execFileSync(process.execPath,['--check',copy]);
  const marker='  (0, import_react119.useEffect)(() => globalThis.__jobsGeminiLive?.chatUI(',start=source.indexOf(marker);assert(start>=0);const fragment=source.slice(start,source.indexOf('\n',start));
  const config={},history=[{id:1,type:'user',text:'owned'}],pending=[],submit=()=>{},toggle=()=>{};let received,cleanup;
  const bridge={chatUI:(c,data,send,shell,set)=>{assert.equal(c,config);assert.equal(send,submit);assert.equal(set,toggle);assert.equal(shell,false);received=data;return()=>cleanup=true;}};
  const fn=new Function('import_react119','globalThis','config','historyManager','pendingHistoryItems','thought','isInputActive','isConfigInitialized','isMcpReady','isCompressing','isResuming','uiState','handleFinalSubmit','shellModeActive','setShellModeActive',fragment);
  let dispose;const react={useEffect:cb=>dispose=cb()};const args=[react,{__jobsGeminiLive:bridge},config,{history},pending,{subject:'native summary'},true,true,true,false,false,{},submit,false,toggle];fn(...args);assert(received.ready);assert(!received.native);assert.equal(received.history,history);dispose();assert(cleanup);
  args[11]={isAuthDialogOpen:true,apiKeyDefaultValue:'PRIVATE_NOT_FORWARDED'};fn(...args);assert(received.native);assert(!JSON.stringify(received).includes('PRIVATE_NOT_FORWARDED'));
  const sm='  (0, import_react97.useEffect)(() => globalThis.__jobsGeminiLive?.chatShell(',ss=source.indexOf(sm);assert(ss>=0);const sf=source.slice(ss,source.indexOf('\n',ss));let send,called=0;new Function('import_react97','globalThis','config','handleShellCommand',sf)(react,{__jobsGeminiLive:{chatShell:(c,f)=>{assert.equal(c,config);send=f;}}},config,(command,signal)=>{assert.equal(command,'printf probe');assert(signal instanceof AbortSignal);called++;return true;});assert(send('printf probe'));assert.equal(called,1);
  assert.equal(fs.readFileSync(path.join(bundle,name),'utf8'),original);count++;
 }
 assert.equal(count,3);console.log('PASS all3 pinned Gemini native UI/shell bindings, auth fallback, credential exclusion, transformed syntax, SDK untouched');
})().catch(e=>{console.error(e);process.exitCode=1;});
