'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'../..'),base=require('./fixture-paths.cjs').fixture('files');
for(const [a,b]of [['codex-chat.cjs','codex-chat.cjs'],['codex-control.js','codex-wire-v0.2.22.js'],['agent-receipts.cjs','agent-receipts.cjs']])fs.copyFileSync(path.join(root,'app/src/main/assets',a),path.join(base,b));
const {CodexChat}=require(path.join(base,'codex-chat.cjs'));
const {Attachments}=require('../../app/src/main/assets/agent-attachments.cjs');
const {ClaudeChat}=require('../../app/src/main/assets/claude-chat.cjs');
const {GeminiChat}=require('../../app/src/main/assets/gemini-chat.cjs');
const {OpenCodeChat}=require('../../app/src/main/assets/opencode-chat.cjs');
process.env.CLAUDE_CONFIG_DIR=path.join(base,'config');
const session='11111111-1111-4111-8111-111111111111';
const rpc={send(){},close(){},async call(){return {};}};
const codex=new CodexChat(rpc,{endpoint:path.join(base,'socket'),access:'project',approval:'on-request',cwd:base,language:'en'});codex.ready=true;codex.thread=session;
const claude=new ClaudeChat({socket:path.join(base,'claude.sock'),binary:'/not-used',model:'sonnet',mode:'manual',chat:'new',thread:session,language:'en'});claude.ready=true;claude.auth=true;
const gemini=new GeminiChat({attachmentRoot:base,session:()=>session,status:()=>({available:true,state:'idle'}),supported:()=>true,permission:()=>null,receipt:()=>null});
const opencode=new OpenCodeChat(async()=>{throw Error('Network forbidden');},'ses_fixture',path.join(base,'receipts.json'));
let serial=0;
async function project(text){
 const id='msg_file_'+(++serial);
 codex.event({method:'item/completed',params:{threadId:session,turnId:'turn_'+serial,item:{id,type:'agentMessage',text}}});
 // The other adapters' real transcript projection paths share Attachments.output.
 claude.messages.set(id,{type:'assistant',message:{id,content:[{type:'text',text}]}});
 gemini.observe({isTrustedFolder:()=>true},{ready:true,native:false,history:[{id:serial,type:'gemini',text}],pending:[]},async()=>{},false,()=>{});
 return [(await codex.snapshot()).rows.filter(r=>r.attachment).map(r=>r.attachment),claude.snapshot().rows.filter(r=>r.attachment).map(r=>r.attachment),gemini.snapshot({}).rows.filter(r=>r.attachment).map(r=>r.attachment),opencode.rows([{id,type:'assistant',content:[{type:'text',text}]}]).filter(r=>r.attachment).map(r=>r.attachment)];
}
(async()=>{
 const names=['archive.zip','archive.7z','archive.rar','archive.gzip','archive.tar.gz','archive.bz2','archive.xz','archive.zst','part.3mf','part.STL','tool.exe','build.bat','model.step','model.obj','print.gcode','custom.unknown','README','space (кириллица).3mf'];
 for(const name of names){const file=path.join(base,name);fs.writeFileSync(file,Buffer.from([80,75,0,255,1]));const text='[Download](<'+file+'>)';const groups=await project(text);for(const files of groups){const a=files.find(a=>a.path===file);assert(a,name+' missing');assert.equal(a.type,'binary');assert.equal(a.bytes,5);assert.equal(a.name,name);}}
 const normal=path.join(base,'archive.zip'),alias=path.join(base,'alias.zip');fs.symlinkSync(normal,alias);const directory=path.join(base,'directory.zip');fs.mkdirSync(directory);const empty=path.join(base,'empty.zip');fs.writeFileSync(empty,'');
 const invalid='[remote](https://example.invalid/a.zip) [missing](/missing/path.zip) [link]('+alias+') [dir]('+directory+') [empty]('+empty+')';
 for(const files of await project(invalid))assert(!files.some(a=>[alias,directory,empty].includes(a.path)));
 const large=path.join(base,'huge.3mf'),over=path.join(base,'over.rar');for(const [f,size]of [[large,5*1024**3],[over,5*1024**3+1]]){const fd=fs.openSync(f,'w');fs.ftruncateSync(fd,size);fs.closeSync(fd);}
 for(const files of await project('[large]('+large+') [over]('+over+')')){assert.equal(files.find(a=>a.path===large)?.bytes,5*1024**3);assert(!files.some(a=>a.path===over));}
 const text='[a]('+normal+') [b]('+normal+')',shared=new Attachments(base);assert.equal(shared.output(text).length,1);
 const originals=fs.readFileSync;fs.readFileSync=()=>{throw Error('Artifact projection must be metadata-only');};try{assert.equal(new Attachments(base).output('[a]('+normal+')')[0].bytes,5);codex.event({method:'item/completed',params:{threadId:session,turnId:'metadata',item:{id:'no-reads',type:'agentMessage',text:'[a]('+normal+')'}}});codex.groups();}finally{fs.readFileSync=originals;}
 fs.unlinkSync(large);fs.unlinkSync(over);await codex.close();console.log('PASS all four agent output paths: archives,3MF/STL,EXE/BAT,arbitrary/no extensions,spaces/Unicode,metadata-only,5GiB bounds,links/directories/empty refusal,dedup');
})().catch(e=>{console.error(e);process.exitCode=1;});
