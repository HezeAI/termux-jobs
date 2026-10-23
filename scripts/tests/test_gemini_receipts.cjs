'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {createBridge}=require('../../app/src/main/assets/gemini-live.cjs');
(async()=>{
 const dir=require('./fixture-paths.cjs').fixture('grecv');const old=process.env.JOBS_GEMINI_RECEIPT_DIR;process.env.JOBS_GEMINI_RECEIPT_DIR=dir;
 try{
  const config={setModel(){},getSessionId:()=> 'synthetic-chat',getProjectRoot:()=> '/synthetic',isTrustedFolder:()=>true};
  const b=createBridge(()=>{});b.attach(config);
  const cleanup=b.permission('call1',{type:'exec',command:'printf synthetic'},config,async()=>{},[{value:'proceed_once'},{value:'cancel'}],true,'shell',false);
  const commit=b.prepareReceipt('call1','proceed_once');assert.equal(typeof commit,'function');assert.equal(b.receiptText('call1'),'');b.consumePermission('call1');cleanup();commit();commit();
  assert.match(b.receiptText('call1'),/Allowed once/);const token=b.receiptText('call1').match(/G:([a-f0-9]{8})/)[1];let r=await b.command({op:'receipt',token});assert.equal(r.receipt.body,'printf synthetic');
  assert(!JSON.stringify(b.snapshot()).includes('printf synthetic'));
  const resumed=createBridge(()=>{});resumed.attach(config);assert.match(resumed.receiptText('call1'),/Allowed once/);
  assert.equal(fs.statSync(path.join(dir,'synthetic-chat.json')).mode&0o777,0o600);
  assert.equal((await b.command({op:'receipt',token:'../bad'})).ok,false);
  b.permission('warn',{type:'exec',command:'unsafe',untrustedFlags:['warning']},config,async()=>{},[{value:'proceed_once'},{value:'cancel'}],true,'shell',false);assert.equal(b.prepareReceipt('warn','proceed_once'),undefined);
  const {instrument}=await import(path.resolve('app/src/main/assets/gemini-live-loader.mjs'));
  let count=0;
  for(const name of ['interactiveCli-2T27ZS4G.js','interactiveCli-66E5CZDR.js','interactiveCli-IX4FVS6G.js']){
   const original=fs.readFileSync('/root/.local/share/termux-jobs/gemini-cli/0.64.0/bundle/'+name,'utf8'),patched=instrument(original,name);assert(!patched.startsWith('globalThis.__jobsGeminiLive?.unsupported'));
   const source=patched.match(/var JobsPermissionReceipt = ([\s\S]*?);\nvar ToolMessage/)[1];
   const component=new Function('globalThis','import_react10','import_jsx_runtime17','Box_default','Text','return ('+source+');')({__jobsGeminiLive:b},{useSyncExternalStore:(_subscribe,get)=>get()},{jsx:(type,props)=>({type,props})},'Box','Text');
   const view=component({callId:'call1',borderColor:'native-theme'});assert.equal(view.props.borderStyle,'round');assert.match(view.props.children.props.children,/Allowed once/);assert.equal(component({callId:'none'}),null);
   assert(patched.includes('JobsPermissionReceipt,{callId:tool.callId,borderColor}'));count++;
  }
  assert.equal(count,3);console.log('PASS: native receipt after callback, cleanup/dedup, private per-chat persistence/resume, scalar privacy/warnings, all 3 receipt components execute and cover shell/dense/normal wrapper');
 }finally{if(old===undefined)delete process.env.JOBS_GEMINI_RECEIPT_DIR;else process.env.JOBS_GEMINI_RECEIPT_DIR=old;fs.rmSync(dir,{recursive:true,force:true});}
})().catch(e=>{console.error(e);process.exitCode=1;});
