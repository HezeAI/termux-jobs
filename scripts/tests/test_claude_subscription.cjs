'use strict';
const assert=require('node:assert/strict'),fs=require('fs'),path=require('path');
const root=require('./fixture-paths.cjs').fixture('clquota');
process.env.CLAUDE_CONFIG_DIR=path.join(root,'config');process.env.HOME=path.join(root,'home');fs.mkdirSync(process.env.HOME);fs.mkdirSync(process.env.CLAUDE_CONFIG_DIR);
const {parse}=require('../../app/src/main/assets/claude-subscription.cjs'),{ClaudeChat}=require('../../app/src/main/assets/claude-chat.cjs');
const raw={rate_limits_available:true,subscription_type:null,rate_limits:{five_hour:{utilization:12.5,resets_at:'2026-11-06T00:00:00Z'},seven_day:{utilization:87,resets_at:'2026-11-12T00:00:00Z'}},session:{private:'do not export'}};
assert.equal(parse(raw,1000).fiveHour.usedPercent,12.5);assert.equal(parse(raw,1000).sevenDay.usedPercent,87);assert.equal(parse(raw,1000).plan,'');assert(!JSON.stringify(parse(raw,1000)).includes('private'));
for(const invalid of [null,'20',-1,101,NaN,Infinity])assert.equal(parse({...raw,rate_limits:{five_hour:{utilization:invalid}}},1).fiveHour,null);
for(const absent of [null,{}, {five_hour:{utilization:0}}, {seven_day:{utilization:100}}]){let v=parse({...raw,rate_limits:absent},1);assert.equal(v.fiveHour?.usedPercent,absent?.five_hour?.utilization);assert.equal(v.sevenDay?.usedPercent,absent?.seven_day?.utilization);}
assert.equal(parse({...raw,rate_limits_available:false},1).fiveHour,null);
const cli=new ClaudeChat({socket:path.join(root,'11111111-1111-4111-8111-111111111111.sock'),binary:'/synthetic/no-cli',model:'sonnet',mode:'manual',chat:'new',thread:'22222222-2222-4222-8222-222222222222'});cli.ready=cli.auth=true;cli.workerKind='sdk';let calls=[],finish;
cli.control=(op,fields)=>{calls.push({op,fields});return new Promise(r=>finish=r)};
(async()=>{
 const initial=await cli.command({op:'status'});assert.equal(initial.subscription,null);assert.equal(calls.length,1);assert.deepEqual(calls[0],{op:'get_usage',fields:{skip_behaviors:true}});
 await cli.command({op:'status'});assert.equal(calls.length,1);finish(raw);await new Promise(setImmediate);let status=await cli.command({op:'status'});assert.equal(status.subscription.fiveHour.usedPercent,12.5);assert.equal(calls.length,1);
 cli.subscriptionCheck=0;cli.busy=true;await cli.command({op:'status'});assert.equal(calls.length,1);cli.busy=false;
 await cli.command({op:'status'});assert.equal(calls.length,2);cli.event({type:'auth_status',isAuthenticating:true});assert.equal(cli.subscription,null);finish(raw);await new Promise(setImmediate);assert.equal(cli.subscription,null,'late old-account response cannot refill new auth scope');
 await cli.command({op:'status'});finish({...raw,rate_limits_available:false});await new Promise(setImmediate);assert.equal(cli.subscription.available,false);assert.equal(cli.subscription.fiveHour,null);
 cli.clearWorker();assert.equal(cli.subscription,null);assert.equal(cli.ready,false);assert.equal((await cli.command({op:'status'})).subscription,null);
 await cli.stop();console.log('PASS native Claude get_usage mapping, both windows, 0/100/missing/invalid data, unknown plan, API isolation, TTL/in-flight/busy gating, auth-generation isolation and reconnect; no CLI/model/network');
})().catch(e=>{console.error(e);process.exitCode=1});
