'use strict';
// Bounded presentation metadata only. Never opens files, fetches URLs or runs tools.
const str=(v,n=2048)=>typeof v==='string'?v.replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f]/g,'').slice(0,n):'';
const object=v=>{if(v&&typeof v==='object'&&!Array.isArray(v))return v;if(typeof v==='string'&&v.length<=200000)try{const p=JSON.parse(v);if(p&&typeof p==='object'&&!Array.isArray(p))return p;}catch{}return {};};
const leaf=n=>str(n,160).split(/\.|\/|__/).at(-1);
const imagePath=p=>typeof p==='string'&&p.startsWith('/')&&p.length<=4096&&!/[\u0000-\u001f]/.test(p)&&/\.(png|jpe?g|webp|gif|bmp|avif|heic|heif)$/i.test(p);
function url(value){try{const u=new URL(str(value,4096));return ['http:','https:'].includes(u.protocol)&&!u.username&&!u.password?u.href:'';}catch{return '';}}
function files(changes){
 if(!Array.isArray(changes))return null;const result=[];let budget=16000;
 for(const c of changes.slice(0,100)){
  if(!c||typeof c!=='object'||!str(c.path))continue;const raw=typeof c.kind==='string'?c.kind:c.kind?.type;
  const operation={add:'created',create:'created',delete:'deleted',remove:'deleted',update:'modified',modify:'modified',move:'renamed',rename:'renamed'}[raw]||'changed';
  const original=typeof c.diff==='string'?c.diff:'';const diff=str(original,Math.min(budget,8000));budget-=diff.length;
  const f={path:str(c.path),operation,diff,diffTruncated:diff.length<original.length};
  const moved=str(c.kind?.move_path||c.kind?.movePath||c.move_path);if(moved)Object.assign(f,{operation:'renamed',destination:moved});
  if(original&&original.length<=200000){let added=0,removed=0;for(const line of original.split('\n')){if(line.startsWith('+')&&!line.startsWith('+++'))added++;if(line.startsWith('-')&&!line.startsWith('---'))removed++;}if(added||removed)Object.assign(f,{added,removed});}
  result.push(f);
 }return result.length?{kind:'files',files:result,count:changes.length,filesTruncated:changes.length>result.length}:null;
}
function preview(item,name,input){
 const n=leaf(name||item?.tool||item?.name||item?.type),args=object(input||item?.arguments||item?.input);
 if(item?.type==='fileChange')return files(item.changes);
 if(['imageView','image_view','view_image'].includes(n)){
  const p=item?.path||args.path||args.file_path;if(imagePath(p))return {kind:'image',image:{type:'image',name:p.split('/').at(-1),path:p}};
 }
 if(['Read','read','read_file','read_many_files'].includes(n)&&imagePath(args.file_path||args.filePath||args.absolute_path||args.path)){
  const p=args.file_path||args.filePath||args.absolute_path||args.path;return {kind:'image',image:{type:'image',name:p.split('/').at(-1),path:p}};
 }
 if(['webSearch','web_search','WebSearch','google_web_search','WebFetch','web_fetch','webfetch','findInPage','openPage'].includes(n)){
  const action=object(item?.action),kind=action.type||n;
  const operation=['openPage','open_page','WebFetch','web_fetch','webfetch'].includes(kind)?'open':['findInPage','find_in_page'].includes(kind)?'find':'search';
  const query=str(action.query||item?.query||args.query||args.q||(Array.isArray(action.queries)?action.queries.slice(0,5).map(q=>str(q,200)).join(' · '):''),1000),address=url(action.url||item?.url||args.url),pattern=str(action.pattern||args.pattern,1000);
  const out={kind:'web',operation,query,url:address,pattern,title:str(item?.title||action.title,300)};
  const results=item?.results||item?.result?.results;if(Array.isArray(results))out.results=results.slice(0,10).map(r=>({url:url(r?.url),title:str(r?.title,300)})).filter(r=>r.url);
  return out;
 }
 if(['Read','read','read_file','read_many_files','Write','write','write_file','Edit','edit','replace','apply_patch'].includes(n)){
  const p=str(args.file_path||args.filePath||args.absolute_path||args.path);if(p)return {kind:'fileTool',path:p,operation:['Read','read','read_file','read_many_files'].includes(n)?'read':['Write','write','write_file'].includes(n)?'write':'edit'};
 }
 return null;
}
function trimPreviews(rows){let left=60000,tailBudget=12000;const out=rows.slice();for(let i=out.length-1;i>=0;i--){if(out[i].outputTail){const n=out[i].outputTail.length;if(n>tailBudget)out[i]={...out[i],outputTail:undefined};else tailBudget-=n;}const p=out[i].actionPreview;if(!p)continue;const size=JSON.stringify(p).length;if(size<=left){left-=size;continue;}const compact=p.kind==='files'?{kind:'files',count:p.count,files:[],filesTruncated:true}:undefined;const n=compact?JSON.stringify(compact).length:0;out[i]={...out[i],actionPreview:n<=left?compact:undefined,more:out[i].more||!!compact};left-=Math.min(n,left);}return out;}
function outputTail(value){if(typeof value!=='string')return '';const lines=value.slice(-4096).replace(/\x1b\[[0-?]*[ -/]*[@-~]/g,'').split(/\r\n|\r|\n/).filter(v=>v.trim());return str((lines.at(-1)||'').slice(-320),320).trim();}
module.exports={preview,files,url,imagePath,trimPreviews,outputTail};
