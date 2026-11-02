'use strict';
// Diagnostic probe: actual CLI + synthetic loopback provider; no real auth or shared route.
const fs=require('node:fs'),path=require('node:path'),http=require('node:http'),readline=require('node:readline'),assert=require('node:assert/strict'),{spawn}=require('node:child_process'),{once}=require('node:events');
const root=require('./fixture-paths.cjs').fixture('cxnet'),assets=path.resolve('app/src/main/assets');
for(const f of ['codex-chat.cjs','agent-receipts.cjs','agent-action-preview.cjs','agent-completion-times.cjs'])fs.copyFileSync(path.join(assets,f),path.join(root,f));
fs.copyFileSync(path.join(assets,'codex-control.js'),path.join(root,'codex-wire-v0.2.22.js'));
const {CodexChat}=require(path.join(root,'codex-chat.cjs')),pause=ms=>new Promise(r=>setTimeout(r,ms));
const started=Date.now(),events=[],record=(type,data={})=>{const value={ms:Date.now()-started,type,...data};events.push(value);fs.appendFileSync(path.join(root,'events.jsonl'),JSON.stringify(value)+'\n');};
async function until(fn,label,ms=15000){const deadline=Date.now()+ms;while(Date.now()<deadline){if(await fn())return;await pause(50);}throw Error('Timeout: '+label);}
const home=path.join(root,'home'),project=path.join(root,'project');fs.mkdirSync(home);fs.mkdirSync(project);
let blocked=false,requests=0,firstResponse,controller,child,rpc,log,sampler;
const pending=new Map();let serial=0;
const exhaust=process.argv.includes('--exhaust'),cancel=process.argv.includes('--cancel');
const server=http.createServer((req,res)=>{let raw='';req.on('data',b=>raw+=b);req.on('end',()=>{
 const q=JSON.parse(raw);const n=++requests;record('provider.request',{n,blocked,input:(q.input||[]).map(x=>({type:x.type,role:x.role,content:x.content})),previous_response_id:q.previous_response_id});
 if(blocked){record('provider.reset',{n});res.destroy();return;}
 res.writeHead(200,{'Content-Type':'text/event-stream'});
 const emit=(type,body)=>res.write('event: '+type+'\ndata: '+JSON.stringify({type,...body})+'\n\n');
 emit('response.created',{response:{id:'response_'+n,status:'in_progress'}});
 if(n===1){firstResponse=res;record('provider.holding',{n});return;}
 const item={id:'assistant_'+n,type:'message',status:'completed',role:'assistant',content:[{type:'output_text',text:'RECOVERED-SYNTHETIC-RESPONSE-'+n,annotations:[]}]};
 emit('response.output_item.added',{output_index:0,item});emit('response.output_item.done',{output_index:0,item});
 emit('response.completed',{response:{id:'response_'+n,status:'completed',output:[item],usage:{input_tokens:10,output_tokens:5,total_tokens:15}}});res.end();record('provider.completed',{n});
 });});
(async()=>{
 try{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 fs.writeFileSync(path.join(home,'config.toml'),'model="gpt-6.1-sol"\nmodel_provider="fixture"\n[model_providers.fixture]\nname="Local fixture"\nbase_url="http://127.0.0.1:'+server.address().port+'/v1"\nwire_api="responses"\nrequires_openai_auth=false\n[projects."/root/termux-fork"]\ntrust_level="trusted"\n[projects."'+project+'"]\ntrust_level="trusted"\n');
 const exe='/usr/local/lib/node_modules/@openai/codex/node_modules/@openai/codex-linux-arm64/vendor/aarch64-unknown-linux-musl/bin/codex';
 log=fs.openSync(path.join(root,'native.log'),'w');
 child=spawn(exe,['--no-daemon','--enable','default_mode_request_user_input','app-server'],{cwd:project,env:{PATH:process.env.PATH,CODEX_HOME:home,TMPDIR:path.resolve('.tools/tmp')},stdio:['pipe','pipe',log]});
 record('fixture.child',{pid:child.pid,exe});
 rpc={send(m){child.stdin.write(JSON.stringify(m)+'\n');},call(method,params){record('rpc.call',{method,params});return new Promise((resolve,reject)=>{const id=++serial,timer=setTimeout(()=>{pending.delete(id);reject(Error('RPC timeout '+method));},15000);pending.set(id,{resolve,reject,timer,method});this.send({id,method,params});});},async initialize(){await this.call('initialize',{clientInfo:{name:'jobs_network_fixture',version:'1'},capabilities:{experimentalApi:true}});this.send({method:'initialized'});},close(){child.stdin.end();}};
 readline.createInterface({input:child.stdout}).on('line',line=>{const m=JSON.parse(line),p=pending.get(m.id);record('native',{message:m});if(p&&!m.method){clearTimeout(p.timer);pending.delete(m.id);m.error?p.reject(Error(JSON.stringify(m.error))):p.resolve(m.result);}rpc.onMessage?.(m);});
 controller=new CodexChat(rpc,{endpoint:path.join(root,'s'),access:'full',approval:'never',cwd:project,chat:'new',thread:'',confirmed:'',language:'en'});await controller.start();
 sampler=setInterval(()=>{controller.snapshot().then(s=>record('snapshot',{connected:s.connected,busy:s.busy,stopToken:s.stopToken,rows:s.rows.map(x=>({key:x.key,message:x.message,kind:x.kind,body:x.body}))})).catch(e=>record('snapshot.error',{error:e.message}));},500);
 const q={op:'chat-send',id:'msg_'+'a'.repeat(32),session:controller.thread,kind:'message',text:'Initial synthetic request'};
 record('send.initial',{result:await controller.send(q)});await until(()=>firstResponse,'initial open response');
 const turn=controller.turn;blocked=true;firstResponse.destroy();record('fault.enabled',{only:'fixture HTTP response sockets',turn});
 await pause(800);
 const steering={...q,id:'msg_'+'b'.repeat(32),text:'Synthetic clarification during broken upstream'};
 const ack=await controller.send(steering);record('send.steer',{result:ack});assert(ack.accepted);assert.equal(ack.delivery,'current');
 const replay=await controller.send(steering);record('send.sameID',{result:replay});assert.equal(replay.replayed,true);
 await pause(3500);const during=await controller.snapshot();record('fault.snapshot',{snapshot:during});
 if(cancel){record('send.interrupt',{result:await controller.handle({op:'chat-interrupt',session:controller.thread,stopToken:turn})});await until(()=>!controller.busy,'fixture interruption');}
 if(exhaust)await until(()=>!controller.busy,'retry exhaustion',150000);
 const beforeRestore=await controller.snapshot();record('fault.beforeRestore',{snapshot:beforeRestore});
 blocked=false;record('fault.restored');
 await until(()=>!controller.busy,'native turn completion after restored provider',65000);
 const final=await controller.snapshot();record('final.snapshot',{snapshot:final});assert.equal(final.busy,false);assert.equal(final.connected,true);assert.equal(final.rows.some(r=>r.body.includes('RECOVERED-SYNTHETIC')),!exhaust&&!cancel);if(cancel)assert.equal(final.modelConnection,null,'interrupted turn cannot keep reconnect banner');for(const id of [q.id,steering.id])assert(final.rows.some(r=>r.deliveryReceipt?.clientId===id),'native clientId receipt '+id);if(exhaust)assert.equal(final.rows.filter(r=>r.kind==='error').length,1,'same native terminal error is shown once');
 let explicitNext=null;
 if(exhaust||cancel){await pause(1500);assert.equal(controller.busy,false);const count=requests;record('send.explicitNext',{result:await controller.send({...q,id:'msg_'+'c'.repeat(32),text:'Explicit next synthetic request after restore'})});await until(()=>!controller.busy&&requests>count,'explicit next request');explicitNext=await controller.snapshot();record('next.snapshot',{snapshot:explicitNext});}
 const native=events.filter(e=>e.type==='native').map(e=>e.message);
 const summary={root,exhaust,cancel,elapsedMs:Date.now()-started,upstream:'synthetic loopback HTTP only',productionTunnelTouched:false,providerRequests:requests,initialTurn:turn,steer:ack,sameIDReplay:replay,steerRPCCalls:events.filter(e=>e.type==='rpc.call'&&e.method==='turn/steer').length,during:{busy:during.busy,connected:during.connected,errorRows:during.rows.filter(r=>r.kind==='error')},beforeRestore:{busy:beforeRestore.busy,connected:beforeRestore.connected},final:{busy:final.busy,connected:final.connected,rows:final.rows},explicitNext,nativeErrors:native.filter(m=>m.method==='error').map(m=>m.params),completed:native.filter(m=>m.method==='turn/completed').map(m=>m.params),restoredResponse:final.rows.some(r=>r.body.includes('RECOVERED-SYNTHETIC')),limitations:['Synthetic provider cannot establish the failure cause on another phone','HTTP stream reset is not a test of Hysteria QUIC, remote proxy, real model, or Android UI']};
 fs.writeFileSync(path.join(root,'result.json'),JSON.stringify(summary,null,2)+'\n');console.log(JSON.stringify(summary,null,2));
 }catch(e){record('failure',{error:e.stack});console.error(e);process.exitCode=1;}
 finally{clearInterval(sampler);if(controller)await controller.close();else if(rpc)rpc.close();if(child&&child.exitCode===null&&child.signalCode===null){const done=once(child,'exit');child.kill('SIGTERM');await done;}for(const p of pending.values())clearTimeout(p.timer);if(log!==undefined)fs.closeSync(log);server.closeAllConnections();server.close();console.log('Evidence: '+root);}
})();
