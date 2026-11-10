'use strict';
// Real adapter projection APIs, synthetic native events and fake receipt files; no CLI/model calls.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=require('./fixture-paths.cjs').fixture('timeline');
const assets=path.resolve(__dirname,'../../app/src/main/assets'),at=1793870400000;
for(const n of ['rtk-agent.cjs','codex-chat.cjs','agent-attachments.cjs','agent-receipts.cjs','agent-action-preview.cjs','agent-completion-times.cjs'])fs.copyFileSync(path.join(assets,n),path.join(root,n));
fs.copyFileSync(path.join(assets,'codex-control.js'),path.join(root,'codex-wire-v0.2.22.js'));
const {ReceiptStore}=require(path.join(assets,'agent-receipts.cjs'));
const specs=[['read','completed'],['edit','completed'],['bad','failed'],['retry','completed'],['no','failed'],['cancel','cancelled'],['abort','aborted'],['unknown','completed']];
const command=n=>'fixture-'+n+' --artificial-only'+(n==='retry'?' '+('argument '.repeat(400)):'');const file='/synthetic/'+('long-segment/'.repeat(12))+'NativeOnly.java';
const out={};
{
 const {CodexChat}=require(path.join(root,'codex-chat.cjs'));const c=new CodexChat({}, {endpoint:path.join(root,'fake.sock'),access:'full',approval:'never',cwd:root,chat:'new'});c.thread='synthetic';c.receipts.add('no:commandExecution/requestApproval','cancel','Synthetic refusal','Synthetic action proof');
 const entries=new Map(specs.map(([id,status])=>[id,id==='read'?{id,type:'dynamicToolCall',tool:'read_file',arguments:{path:file},status}:id==='edit'?{id,type:'fileChange',status,changes:[{path:file,kind:'update',diff:'-before\n+after\n'}]}:id==='unknown'?{id,type:'UnknownNativeTool',tool:'unrelated.Read',status}:{id,type:'commandExecution',description:"Native action description",command:command(id),status,exitCode:id==='bad'?1:status==='completed'?0:null,aggregatedOutput:'ONLY_TEST_TIMELINE '+id,durationMs:id==='retry'?250:null}]));
 const turns=new Map([['turn',entries]]),original=JSON.stringify([...entries]);const rows=c.groups(turns).flatMap(g=>g.rows).filter(r=>r.kind==='tool');assert.equal(JSON.stringify([...entries]),original);assert.equal(rows.length,8);assert.equal(rows.find(r=>r.command===command('no')).actionOutcome,'user_declined');assert.equal(rows.find(r=>r.command===command('abort')).status,'aborted');assert.deepEqual(c.groups(turns).flatMap(g=>g.rows).filter(r=>r.kind==='tool'),rows);out.codex=rows;
}
{
 process.env.CLAUDE_CONFIG_DIR=path.join(root,'claude-config');const {ClaudeChat}=require(path.join(assets,'claude-chat.cjs'));
 const make=()=>new ClaudeChat({socket:path.join(root,'claude.sock'),model:'',mode:'manual',chat:'new',thread:'00000000-0000-4000-8000-000000000001'});const c=make();c.receipts.add('no','cancel','Synthetic refusal','No this call only');
 const use={type:'assistant',timestamp:new Date(at).toISOString(),message:{content:specs.map(([id])=>({type:'tool_use',id,name:id==='read'?'Read':id==='edit'?'Edit':id==='unknown'?'unrelated.Read':'Bash',input:id==='read'||id==='edit'?{file_path:file}:{description:"Native action description",command:command(id)}}))}};
 const results={type:'user',timestamp:new Date(at+1000).toISOString(),message:{content:specs.map(([id,status])=>({type:'tool_result',tool_use_id:id,is_error:['failed','aborted','cancelled'].includes(status),content:'ONLY_TEST_TIMELINE '+id}))}};
 // Claude stream only supplies is_error for these results; unproven cancellations remain errors.
 const native=new Map([['calls',use],['results',results]]),original=JSON.stringify([...native]);let rows=c.groups(native,[]).flatMap(g=>g.rows).filter(r=>r.kind==='tool');assert.equal(rows.length,16);assert.equal(rows.find(r=>r.actionRole==='call'&&r.actionId==='no').status,'declined');assert.equal(rows.find(r=>r.actionRole==='call'&&r.actionId==='bad').status,'failed');assert.equal(JSON.stringify([...native]),original);assert.deepEqual(make().groups(native,[]).flatMap(g=>g.rows).filter(r=>r.kind==='tool'),rows);out.claude=rows;
}
{
 const {GeminiChat}=require(path.join(assets,'gemini-chat.cjs'));const api={attachmentRoot:path.join(root,'gemini'),session:()=> 'synthetic',status:()=>({state:'idle',available:true}),receipt:id=>id==='no'?{outcome:'cancel',body:'Synthetic proof',label:'Denied'}:null};
 const data={history:[{id:1,type:'tool_group',tools:specs.map(([id,status])=>({callId:id,name:id==='read'?'read_file':id==='edit'?'replace':id==='unknown'?'unrelated.Read':'run_shell_command',status:status==='completed'?'success':status==='failed'?'error':status,args:id==='read'||id==='edit'?{file_path:file}:{description:"Native action description",command:command(id)},description:"Native action description",resultDisplay:'ONLY_TEST_TIMELINE '+id}))}],ready:true};
 const project=()=>{const c=new GeminiChat(api);c.observe({},data,()=>assert.fail('No agent turn'),false,()=>assert.fail('No mode change'));c.rebuild();return c.groups.flatMap(g=>g.rows).filter(r=>r.kind==='tool');};const original=JSON.stringify(data),rows=project();assert.equal(rows.length,8);assert.equal(rows.find(r=>r.part==='tool:no').actionOutcome,'user_declined');assert.equal(rows.find(r=>r.part==='tool:abort').status,'aborted');assert.deepEqual(project(),rows);assert.equal(JSON.stringify(data),original);out.gemini=rows;
}
{
 const {OpenCodeChat}=require(path.join(assets,'opencode-chat.cjs'));const session='ses_synthetic',receipts=new ReceiptStore(path.join(root,'opencode.json'),'O');receipts.add('no','cancel','Synthetic proof',JSON.stringify({sessionID:session,source:{type:'tool',id:'no',messageID:'msg_synthetic'}}));
 const native=[{id:'msg_synthetic',type:'assistant',content:specs.map(([id,status],i)=>({id,type:'tool',name:id==='read'?'read':id==='edit'?'edit':id==='unknown'?'unrelated.Read':'bash',state:{status:id==='no'?'error':status,input:id==='read'||id==='edit'?{filePath:file}:{description:"Native action description",command:command(id)},output:'ONLY_TEST_TIMELINE '+id,...(id==='no'?{error:{type:'aborted',message:'Synthetic error'}}:{}),time:{start:at+i*1000,end:at+i*1000+250}}}))}];
 const project=()=>new OpenCodeChat(()=>assert.fail('No transport request'),session,path.join(root,'opencode.json')).rows(native,receipts).filter(r=>r.kind==='tool');const original=JSON.stringify(native),rows=project();assert.equal(rows.length,8);assert.equal(rows.find(r=>r.part==='tool:no').actionOutcome,'user_declined');assert.equal(rows.find(r=>r.part==='tool:retry').durationMs,250);assert.deepEqual(project(),rows);assert.equal(JSON.stringify(native),original);out.opencode=rows;
}
for(const [agent,rows]of Object.entries(out)){
 const shell=rows.filter(r=>r.actionType==='shell');assert(shell.length>0,agent);
 for(const r of shell)assert.equal(r.actionDescription,'Native action description',agent);
 const long=shell.find(r=>r.command.startsWith('fixture-retry'));assert(long.commandTruncated,agent);
 assert(long.body.includes(command('retry')),agent+' full original command remains in detail body');
 assert(long.body.includes('ONLY_TEST_TIMELINE retry'),agent+' output preserved');
}
if(process.env.TIMELINE_FIXTURE_OUTPUT)fs.writeFileSync(process.env.TIMELINE_FIXTURE_OUTPUT,JSON.stringify(out,null,2)+'\n');
console.log('PASS four real adapters: native event projection, same-action No versus unrelated error, retry retained, original data unchanged, cold reconstruction, no model or archive operations');
