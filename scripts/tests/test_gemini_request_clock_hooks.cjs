'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto'),{spawnSync}=require('node:child_process');
(async()=>{
 const {instrument}=await import(path.resolve('app/src/main/assets/gemini-live-loader.mjs'));
 const root=process.env.JOBS_GEMINI_CLOCK_BUNDLE||'/root/.local/share/termux-jobs/gemini-cli/0.64.0/bundle';const results=[];
 for(const name of ['interactiveCli-2T27ZS4G.js','interactiveCli-66E5CZDR.js','interactiveCli-IX4FVS6G.js']){
  const source=fs.readFileSync(path.join(root,name),'utf8'),output=instrument(source,name);assert(!output.startsWith('globalThis.__jobsGeminiLive?.unsupported();'),name+' was not fully instrumented');assert(output.includes('requestBegin(config,prompt_id,!!options?.isContinuation)'));assert(output.includes('requestEnd(config,prompt_id)'));assert(output.includes('requestEnd(config,lastPromptIdRef.current)'));
  const start=output.indexOf('const processGeminiStreamEvents'),end=output.indexOf('const submitQuery =',start),stream=output.slice(start,end);assert(stream.includes('toolCallRequests.length > 0'));assert(stream.includes('else if (!loopDetectedRef.current)'));assert(!stream.slice(stream.indexOf('toolCallRequests.length > 0'),stream.indexOf('} else {',stream.indexOf('toolCallRequests.length > 0'))).includes('requestBegin'));
  const syntax=spawnSync(process.execPath,['--check','--input-type=module'],{input:output,encoding:'utf8',maxBuffer:1024*1024});assert.equal(syntax.status,0,syntax.stderr);results.push({name,inputSha256:crypto.createHash('sha256').update(source).digest('hex'),outputSha256:crypto.createHash('sha256').update(output).digest('hex')});
 }
 console.log('PASS pinned Gemini 0.64.0 native user-prompt boundaries, tool continuation/cancel/failure/loop handlers and transformed syntax for all 3 supported UI bundles');console.log(JSON.stringify(results));
})().catch(e=>{console.error(e);process.exitCode=1;});
