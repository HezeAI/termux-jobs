'use strict';
// Own one server child. Drain stderr; publish only fixed diagnostic codes, never raw output.
const {spawn}=require('node:child_process');
const fs=require('node:fs');
const {StringDecoder}=require('node:string_decoder');
function argumentsFor(endpoint,sandbox,approval,questions){
 if(typeof endpoint!=='string'||!endpoint.startsWith('/')||endpoint.includes('\0')||Buffer.byteLength(endpoint)>100||!['danger-full-access','workspace-write','read-only'].includes(sandbox)||!['untrusted','on-request','never'].includes(approval)||!['0','1'].includes(questions))throw Error('INVALID_LAUNCH');
 // 0.166 rejects untrusted in public CLI/TOML config. The chat controller sends
 // the requested policy through thread/start or thread/resume and verifies it.
 const bootstrap=approval==='untrusted'?'on-request':approval;
 return ['--no-daemon',...(questions==='1'?['--enable','default_mode_request_user_input']:[]),'-c','sandbox_mode='+JSON.stringify(sandbox),'-c','approval_policy='+JSON.stringify(bootstrap),'app-server','--listen','unix://'+endpoint];
}
function reason(value){
 if(/approval_policy\s*=\s*"untrusted" is no longer supported/.test(value))return 'UNSUPPORTED_APPROVAL_CONFIG';
 if(/Address already in use|address.*in use|EADDRINUSE/i.test(value))return 'SOCKET_IN_USE';
 if(/Permission denied|EACCES/i.test(value))return 'ACCESS_DENIED';
 if(/unexpected argument|unrecognized (?:option|argument)/i.test(value))return 'UNSUPPORTED_ARGUMENT';
 if(/No such file or directory|ENOENT/i.test(value))return 'FILE_NOT_FOUND';
 return '';
}
function launch(argv){
 const [endpoint,sandbox,approval,questions,language]=argv;let args;
 try{args=argumentsFor(endpoint,sandbox,approval,questions);}catch{process.stderr.write('Codex: INVALID_LAUNCH\n');process.exitCode=2;return;}
 let seenSocket=false;let spawnFailed=false;let child,stopping=false,timer,diagnostic='',tail='';const decoder=new StringDecoder('utf8');
 const descriptions={UNSUPPORTED_APPROVAL_CONFIG:['CLI отверг настройку approval_policy=untrusted.','The CLI rejected approval_policy=untrusted.'],SOCKET_IN_USE:['Сокет уже занят.','The socket is already in use.'],ACCESS_DENIED:['При запуске получен отказ в доступе.','Startup reported access denied.'],UNSUPPORTED_ARGUMENT:['CLI не поддерживает переданный параметр.','The CLI does not support a supplied argument.'],FILE_NOT_FOUND:['При запуске не найден необходимый файл.','Startup reported a missing file.'],CLI_NOT_FOUND:['Codex не найден в PATH этой среды.','Codex was not found in this environment PATH.'],SPAWN_FAILED:['Не удалось создать процесс Codex.','Could not create the Codex process.']};
 const emit=(code,status)=>process.stderr.write((language==='ru'?'Codex: процесс app-server завершился':'Codex: app-server process exited')+' ['+code+']'+(Number.isInteger(status)?' (exit '+status+')':'')+(descriptions[code]?' '+descriptions[code][language==='ru'?0:1]:'')+'\n');
 child=spawn('codex',args,{stdio:['ignore','ignore','pipe'],env:{...process.env,JOBS_RTK_OWNER_PID:String(process.pid)}});
 child.stderr.on('data',chunk=>{if(!seenSocket)try{seenSocket=fs.statSync(endpoint).isSocket();}catch{}if(seenSocket){tail='';diagnostic='';return;}const text=tail+decoder.write(chunk);diagnostic=reason(text)||diagnostic;tail=text.slice(-256);});
 child.once('error',error=>{spawnFailed=true;tail='';emit(error.code==='ENOENT'?'CLI_NOT_FOUND':'SPAWN_FAILED');process.exitCode=127;});
 const stop=()=>{if(stopping)return;stopping=true;if(child.exitCode===null&&child.signalCode===null){child.kill('SIGTERM');timer=setTimeout(()=>{if(child.exitCode===null&&child.signalCode===null)child.kill('SIGKILL');},8000);timer.unref();}};
 for(const signal of ['SIGINT','SIGTERM','SIGHUP'])process.on(signal,stop);
 child.once('close',(code,signal)=>{clearTimeout(timer);if(spawnFailed){process.exitCode=127;return;}tail='';for(const name of ['SIGINT','SIGTERM','SIGHUP'])process.removeListener(name,stop);if(!stopping&&code!==0)emit(diagnostic||'PROCESS_EXIT',code);process.exitCode=stopping?130:Number.isInteger(code)?code:signal?1:0;});
}
module.exports={argumentsFor,reason};
if(require.main===module)launch(process.argv.slice(2));
