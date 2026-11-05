'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),{spawnSync}=require('node:child_process');
(async()=>{
 const {instrument}=await import(path.resolve('app/src/main/assets/gemini-live-loader.mjs'));
 const root='/root/.local/share/termux-jobs/gemini-cli/0.64.0/bundle',results=[];
 for(const name of ['chunk-J7SXW2FI.js','chunk-NR7L3FXV.js','chunk-PZ4GDG24.js']){
  const source=fs.readFileSync(path.join(root,name),'utf8'),out=instrument(source,name);assert(!out.startsWith('globalThis.__jobsGeminiLive?.unsupported();'));
  const needle='globalThis.__jobsGeminiLive?.contextResponse?.';assert.equal(out.split(needle).length,2);const call=out.slice(out.indexOf(needle),out.indexOf(needle)+500);assert(call.includes('model,usageMetadata,role'));assert(call.includes('includes(model)?tokenLimit(model):null'));assert(!call.includes('DEFAULT_TOKEN_LIMIT'));
  assert(out.includes('newHistory && [CompressionStatus.COMPRESSED,CompressionStatus.CONTENT_TRUNCATED].includes(info2.compressionStatus)'));assert(out.indexOf('contextCompressed?.')>out.indexOf('this.chat = await this.startChat(newHistory, resumedData)'));
  const syntax=spawnSync(process.execPath,['--check','--input-type=module'],{input:out,encoding:'utf8',maxBuffer:1024*1024});assert.equal(syntax.status,0,syntax.stderr);
  // Evaluate the actual injected expression against a fake native catalog: unknown must not call tokenLimit.
  const expression=call.match(/(\(\[GEMMA[\s\S]+?\?tokenLimit\(model\):null\))/)[1];const names=expression.match(/\[([^\]]+)\]/)[1].split(',');let called=0;const evalWindow=Function('model','tokenLimit',...names,'return '+expression);const native=names.map((_,n)=>'native-'+n);assert.equal(evalWindow('unknown',()=>{called++;return 1e6;},...native),null);assert.equal(called,0);assert.equal(evalWindow(native[2],()=>{called++;return 1234;},...native),1234);
  const bad=instrument(source+'\n',name);assert(bad.startsWith('globalThis.__jobsGeminiLive?.unsupported();'));
  results.push({name,sha256:crypto.createHash('sha256').update(source).digest('hex')});
 }
 console.log('PASS all 3 pinned core bundles: raw main usage, actual model, explicit native catalog only; successful compression after history replacement; syntax and hash rejection');console.log(JSON.stringify(results));
})().catch(e=>{console.error(e);process.exitCode=1;});
