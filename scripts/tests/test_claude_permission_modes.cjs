'use strict';
const assert=require('node:assert/strict'),path=require('node:path'),{EventEmitter}=require('node:events');
const {modes,reason}=require('../../app/src/main/assets/claude-permissions.cjs');
const {argsFor}=require('../../app/src/main/assets/claude-control.js');
const {ClaudeChat}=require('../../app/src/main/assets/claude-chat.cjs');
const root=require('./fixture-paths.cjs').fixture('claude-modes');process.env.CLAUDE_CONFIG_DIR=path.join(root,'empty-config');
const id='12345678-1234-4234-8234-123456789abc';
const kill=process.kill,getuid=process.getuid;process.kill=()=>{throw Error('Real signals forbidden in this fixture');};
function backend(mode){const b=new ClaudeChat({socket:path.join(root,id+'.sock'),binary:'/fake/claude',model:'sonnet',mode,chat:'new',thread:id});b.terminate=async()=>{};return b;}
function child(){const c=new EventEmitter();c.stdout=new EventEmitter();c.stdout.setEncoding=()=>{};c.stderr=new EventEmitter();c.stdin={writable:true,write(){},on(){}};c.exitCode=null;c.signalCode=null;return c;}
(async()=>{
 for(const mode of modes)assert.equal(argsFor('',mode,'new',id)[1],mode);
 assert.match(reason('bypassPermissions','"bypassPermissions"',0),/root/);assert.match(reason('auto','',1000),/not confirm/);assert.equal(reason('auto','"auto"',0),'');assert.equal(reason('bypassPermissions','"bypassPermissions"',1000),'');
 process.getuid=()=>0;const rootBypass=backend('bypassPermissions');rootBypass.owned=()=>{throw Error('Must refuse before any spawn');};await rootBypass.startSDK();assert.equal(rootBypass.ready,false);assert.match(rootBypass.failure,/root/);
 const cancelled=backend('auto');cancelled.checkPermissionMode=async()=>{cancelled.closed=true;};cancelled.owned=()=>{throw Error('No spawn after cancellation');};await cancelled.startSDK();assert.equal(cancelled.worker,null);
 for(const accepted of [true,false]){
  const b=backend('auto'),calls=[];b.owned=args=>{calls.push(args);const c=child();if(args[0]==='--help')queueMicrotask(()=>{c.stdout.emit('data','"auto" "bypassPermissions"');c.exitCode=0;c.emit('exit',0);});return c;};
  b.control=async(kind,fields)=>{calls.push([kind,fields]);if(kind==='set_permission_mode'&&!accepted)throw Error('Rejected');return {};};await b.startSDK();assert.equal(b.ready,accepted);assert(calls.some(x=>x[0]==='set_permission_mode'&&x[1].mode==='auto'));assert(calls.some(x=>x.includes('--permission-mode')&&x.includes('auto')));if(!accepted)assert.match(b.failure,/Auto is unavailable/);
  if(accepted){b.event({type:'system',subtype:'init',session_id:id,permissionMode:'default'});assert.equal(b.snapshot().mode,'manual');assert.match(b.failure,/instead of requested auto/);}b.ready=true;b.workerKind='sdk';b.mode='manual';b.control=async()=>{throw Error('Native refusal');};await assert.rejects(()=>b.command({op:'set-mode',mode:'auto'}));assert.equal(b.mode,'manual');assert.equal(b.setting,false);
 }
 console.log('PASS mode allowlists, root refusal before spawn, CLI support, explicit Auto ACK, refusal preserves mode; synthetic controls only');
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(()=>{process.kill=kill;process.getuid=getuid;});
