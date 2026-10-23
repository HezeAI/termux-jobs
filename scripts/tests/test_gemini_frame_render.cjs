'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),path=require('node:path');
(async()=>{
 const {instrument}=await import(path.resolve('app/src/main/assets/gemini-live-loader.mjs'));
 const root=path.resolve(process.env.JOBS_GEMINI_FIXTURE_BUNDLE||'.tools/gemini-cli/package/bundle');
 assert(fs.existsSync(root),'Prepare the pinned Gemini bundle in .tools/gemini-cli/package/bundle or set JOBS_GEMINI_FIXTURE_BUNDLE');
 const output=require('./fixture-paths.cjs').fixture('frame');
 const names=['chunk-V7G7CIBW.js','chunk-MA35JFUP.js','chunk-KJWA3D3G.js'];
 const ESC='\x1b[';const base={cursorUp:(n=1)=>ESC+n+'A',cursorDown:(n=1)=>ESC+n+'B',cursorTo:(x,y)=>typeof y==='number'?ESC+(y+1)+';'+(x+1)+'H':ESC+(x+1)+'G',eraseLines:n=>Array.from({length:n},(_,i)=>ESC+'2K'+(i<n-1?ESC+'1A':'')).join('')+(n?ESC+'G':'')};
 for(const name of names){
  const original=fs.readFileSync(path.join(root,name),'utf8'),modified=instrument(original,name);assert(modified.includes('const jobsGeminiFrameWrite ='));assert(!modified.startsWith('globalThis.__jobsGeminiLive?.unsupported();'));
  const start=modified.indexOf('const jobsGeminiFrameWrite ='),end=modified.indexOf('// node_modules/ink/build/instances.js',start);
  const context={base_exports:base,isCursorPositionEqual:(a,b)=>(a?.row??0)===(b?.row??0)&&(a?.col??0)===(b?.col??0)};
  vm.createContext(context);vm.runInContext(modified.slice(start,end).replace(/^import .*;$/gm,''),context);
  const writes=[],stream={write:text=>writes.push(text)},render=context.createStandard(stream,{showCursor:true});
  const panel='\x1b[48;2;42;42;42m> Type your message                         \x1b[0m';
  render('Spinner A\nstatus\n'+panel+'\nworkspace');writes.length=0;
  render('Spinner B\nstatus\n'+panel+'\nworkspace');assert.equal(writes.length,1);assert(writes[0].startsWith(ESC+'?2026h'));assert(writes[0].endsWith(ESC+'?2026l'));assert(writes[0].includes(panel));
  const plainCtx={base_exports:base,isCursorPositionEqual:context.isCursorPositionEqual};vm.createContext(plainCtx);
  vm.runInContext(original.slice(original.indexOf('var createStandard ='),original.indexOf('// node_modules/ink/build/instances.js')).replace(/^import .*;$/gm,''),plainCtx);
  const oldWrites=[],old=plainCtx.createStandard({write:s=>oldWrites.push(s)},{showCursor:true});old('Spinner A\nstatus\n'+panel+'\nworkspace');oldWrites.length=0;old('Spinner B\nstatus\n'+panel+'\nworkspace');
  assert.equal(writes[0].slice(8,-8),oldWrites[0]);
  context.testStream=stream;vm.runInContext('jobsGeminiFrameWrite(testStream,"\\x1b[?2026hwhole\\x1b[?2026l")',context);assert.equal(writes.at(-1),ESC+'?2026hwhole'+ESC+'?2026l');
  assert.equal(fs.readFileSync(path.join(root,name),'utf8'),original);
  assert(instrument(original+'\n',name).startsWith('globalThis.__jobsGeminiLive?.unsupported();'));
  const file=path.join(output,name);fs.writeFileSync(file,modified);const syntax=require('node:child_process').spawnSync(process.execPath,['--check',file]);assert.equal(syntax.status,0,syntax.stderr?.toString());
 }
 console.log('PASS: all 3 pinned Ink layouts, native legacy redraw framed, payload identical, existing frames not nested, hash mismatch fallback, SDK unchanged');
})().catch(e=>{console.error(e);process.exitCode=1;});
