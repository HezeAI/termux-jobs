'use strict';
// The existing ownership/start-identity checked cleanup, used only for the child
// actually spawned here. No name-based process selection or broad signal.
const {spawn}=require('node:child_process');const {terminateTree}=require('./codex-chat.cjs');
const child=spawn(process.argv[2],process.argv.slice(3),{stdio:'inherit'});const parent=process.ppid;let closing=false;
const stop=async()=>{if(closing)return;closing=true;clearInterval(watch);await terminateTree(child);};
const watch=setInterval(()=>{if(process.ppid!==parent)stop();},250);watch.unref();
for(const s of ['SIGTERM','SIGINT','SIGHUP'])process.on(s,stop);
child.once('error',()=>{clearInterval(watch);process.exitCode=1;});child.once('exit',code=>{clearInterval(watch);process.exitCode=Number.isInteger(code)?code:1;});
