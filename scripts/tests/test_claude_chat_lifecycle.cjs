'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{spawn}=require('node:child_process'),{once}=require('node:events');
const {request}=require('../../app/src/main/assets/claude-control-v2.js');
const root=require('./fixture-paths.cjs').fixture('clife'),stub=path.join(root,'stub');
fs.writeFileSync(stub,'#!'+process.execPath+'\n'+String.raw`
const fs=require('fs'),net=require('net'),path=require('path'),{spawn}=require('child_process'),readline=require('readline');
if(process.argv[2]==='auth'){console.log(JSON.stringify({loggedIn:true}));process.exit(0);}
process.on('SIGTERM',()=>{});const kind=process.argv.includes('--print')?'sdk':'native';const child=spawn(process.execPath,['-e',"process.on('SIGTERM',()=>{});setInterval(()=>{},1000)"],{stdio:'ignore'});
fs.writeFileSync(path.join(process.env.CLAUDE_FIXTURE_ROOT,kind+'-'+process.pid+'.json'),JSON.stringify({pid:process.pid,descendant:child.pid,kind}));
if(kind==='sdk'){readline.createInterface({input:process.stdin}).on('line',line=>{const q=JSON.parse(line);if(q.type==='control_request')process.stdout.write(JSON.stringify({type:'control_response',response:{subtype:'success',request_id:q.request_id,response:{models:[{value:'sonnet',displayName:'Sonnet'}]}}})+'\n');});}
else {const send=event=>{const c=net.createConnection(process.env.JOBS_CLAUDE_EVENT_SOCKET);c.on('error',()=>{});c.on('connect',()=>c.end(JSON.stringify({op:'native-event',token:process.env.JOBS_CLAUDE_EVENT_TOKEN,event,session:process.env.CLAUDE_FIXTURE_SESSION,cwd:process.cwd()})+'\n'));};send('SessionStart');if(process.env.CLAUDE_FIXTURE_PROMPT==='1')setTimeout(()=>send('UserPromptSubmit'),100);}
setInterval(()=>{},1000);
`,{mode:0o700});
const pause=ms=>new Promise(r=>setTimeout(r,ms));async function until(fn,label){for(let i=0;i<160;i++){if(await fn())return;await pause(100);}throw Error(label);}
function active(pid){try{const v=fs.readFileSync('/proc/'+pid+'/stat','utf8');return v.slice(v.lastIndexOf(')')+2).split(' ')[0]!=='Z';}catch{return false;}}
const files=dir=>fs.readdirSync(dir).filter(n=>/^(sdk|native)-.*json$/.test(n)).map(n=>JSON.parse(fs.readFileSync(path.join(dir,n))));
(async()=>{const unrelated=spawn(process.execPath,['-e','setInterval(()=>{},1000)'],{stdio:'ignore'});try{for(const prompted of [false,true]){
 const dir=path.join(root,prompted?'prompted':'idle');fs.mkdirSync(dir);const session='55555555-5555-4555-8555-555555555555',socket=path.join(dir,'66666666-6666-4666-8666-666666666666.sock');
 const child=spawn(process.execPath,[path.resolve('app/src/main/assets/claude-control-v2.js'),'run',socket,'sonnet','manual','new',session],{cwd:dir,stdio:'ignore',env:{PATH:process.env.PATH,HOME:dir,CLAUDE_CONFIG_DIR:path.join(dir,'config'),JOBS_CLAUDE_BINARY:stub,CLAUDE_FIXTURE_ROOT:dir,CLAUDE_FIXTURE_SESSION:session,CLAUDE_FIXTURE_PROMPT:prompted?'1':'0'}});
 try{await until(async()=>{try{return (await request(socket,{op:'status'})).ready;}catch{return false;}},'SDK ready');const original=files(dir).find(p=>p.kind==='sdk');await request(socket,{op:'terminal-open'});await until(()=>files(dir).some(p=>p.kind==='native'),'native start');assert(!active(original.pid));assert(!active(original.descendant));const native=files(dir).find(p=>p.kind==='native');await pause(250);await assert.rejects(()=>request(socket,{op:'native-event',token:'wrong',event:'Stop',session,cwd:dir}));await request(socket,{op:'terminal-close'});
 assert(active(native.pid));assert.equal(files(dir).filter(p=>p.kind==='sdk').length,1);assert((await request(socket,{op:'chat-snapshot'})).needsTerminal);process.kill(-native.pid,'SIGKILL');await until(async()=>{try{return (await request(socket,{op:'status'})).ready;}catch{return false;}},'SDK resumes after explicit native exit');assert.equal(files(dir).filter(p=>p.kind==='sdk').length,2);

 const exit=once(child,'exit');await request(socket,{op:'stop'});await exit;for(const p of files(dir)){assert(!active(p.pid));assert(!active(p.descendant));}assert(active(unrelated.pid));assert(!fs.existsSync(socket));
 }finally{if(child.exitCode===null&&child.signalCode===null){try{await request(socket,{op:'stop'});}catch{child.kill('SIGTERM');}}}
 }}finally{unrelated.kill('SIGTERM');await once(unrelated,'exit');}console.log('PASS Claude SDK/native handoff, all native processes survive Back (including with no hook confirmation), forged-hook rejection, forced owned process-group/descendant cleanup, private socket and unrelated process survival');})().catch(e=>{console.error(e);process.exitCode=1;});
