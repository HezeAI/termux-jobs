'use strict';
const fs=require('node:fs'),crypto=require('node:crypto');
const {clean}=require('./agent-receipts.cjs');

const validId=value=>typeof value==='string'&&/^[a-zA-Z0-9_-]{1,160}$/.test(value);
const isSubagent=block=>block?.type==='tool_use'&&validId(block.id)&&['Agent','Task'].includes(block.name);
const blockText=(value,limit)=>typeof value==='string'?clean(value.slice(0,limit)).slice(0,limit):'';
const statusValues=new Set(['running','completed','failed','stopped']);
const byteLimit=512*1024;
const messageLimit=16000;

/** Only text directly linked by Claude's native stream to a known parent tool call. */
class ClaudeSubagents {
 constructor(file,session){
  this.file=file;this.session=session;this.calls=new Set();this.records=new Map();
  if(!file)return;
  let fd;
  try{
   fd=fs.openSync(file,fs.constants.O_RDONLY|fs.constants.O_NOFOLLOW|fs.constants.O_NONBLOCK);
   const stat=fs.fstatSync(fd);
   if(!stat.isFile()||stat.nlink!==1||stat.size>byteLimit)return;
   const bytes=Buffer.alloc(stat.size);
   if(fs.readSync(fd,bytes,0,bytes.length,0)!==bytes.length)return;
   const records=JSON.parse(bytes.toString('utf8'));
   if(!Array.isArray(records))return;
   for(const [id,entry] of records.filter(row=>Array.isArray(row)&&row.length===2).slice(-24)){
    if(!validId(id)||!entry||!Array.isArray(entry.messages))continue;
    const messages=entry.messages.filter(row=>row&&validId(row.id)&&
     ['userMessage','agentMessage'].includes(row.role)&&typeof row.text==='string'&&row.text.length<=messageLimit).slice(-32);
    this.records.set(id,{taskId:validId(entry.taskId)?entry.taskId:'',
     status:statusValues.has(entry.status)&&entry.status!=='running'?entry.status:'',
     summary:blockText(entry.summary,messageLimit),messages});
   }
  }catch{}finally{if(fd!==undefined)fs.closeSync(fd);}
 }
 remember(block){
  if(!isSubagent(block))return;
  this.calls.delete(block.id);this.calls.add(block.id);
  if(this.calls.size>128)this.calls.delete(this.calls.values().next().value);
 }
 capture(event){
  if(!this.session||event.session_id!==this.session)return;
  const parent=event.parent_tool_use_id;
  if(parent){
   if(!validId(parent)||!this.calls.has(parent)||!['assistant','user'].includes(event.type))return;
   const id=event.message?.id||event.uuid;
   if(!validId(id))return;
   const blocks=event.message?.content;
   const value=typeof blocks==='string'?blocks:Array.isArray(blocks)?
    blocks.filter(block=>block?.type==='text').map(block=>blockText(block.text,messageLimit)).join('\n'):'';
   const text=blockText(value,messageLimit);
   if(!text)return;
   const record=this.entry(parent);
   const role=event.type==='assistant'?'agentMessage':'userMessage';
   const index=record.messages.findIndex(row=>row.id===id&&row.role===role);
   if(index>=0&&record.messages[index].text===text)return;
   if(index>=0)record.messages[index]={id,role,text};
   else record.messages.push({id,role,text});
   while(record.messages.length>32||Buffer.byteLength(JSON.stringify(record.messages))>128*1024)record.messages.shift();
   this.save(parent,record);
   return;
  }
  if(event.type==='assistant'&&Array.isArray(event.message?.content)){
   for(const block of event.message.content)this.remember(block);
  }else if(event.type==='stream_event'&&event.event?.type==='content_block_start'){
   this.remember(event.event.content_block);
  }else if(event.type==='system'&&validId(event.task_id)){
   this.captureStatus(event);
  }
 }
 captureStatus(event){
  const start=event.subtype==='task_started';
  const finish=event.subtype==='task_notification'&&['completed','failed','stopped'].includes(event.status);
  if(!start&&!finish)return;
  let parent=event.tool_use_id;
  if(!parent&&!start)parent=[...this.records].find(([,record])=>record.taskId===event.task_id)?.[0];
  if(!validId(parent)||!this.calls.has(parent)&&!this.records.has(parent))return;
  const record=this.entry(parent);
  if(finish&&record.taskId&&record.taskId!==event.task_id)return;
  record.taskId=event.task_id;
  record.status=start?'running':event.status;
  record.summary=finish?blockText(event.summary,messageLimit):'';
  this.save(parent,record);
 }
 entry(parent){return this.records.get(parent)||{taskId:'',status:'',summary:'',messages:[]};}
 disconnect(){for(const record of this.records.values())if(record.status==='running')record.status='';}
 save(parent,record){
  this.records.delete(parent);this.records.set(parent,record);
  let data=JSON.stringify([...this.records]);
  while(this.records.size>24||Buffer.byteLength(data)>byteLimit){
   this.records.delete(this.records.keys().next().value);
   data=JSON.stringify([...this.records]);
  }
  if(!this.file)return;
  const temp=this.file+'.'+crypto.randomBytes(8).toString('hex');
  try{fs.writeFileSync(temp,data,{flag:'wx',mode:0o600});fs.renameSync(temp,this.file);}
  catch{}finally{try{fs.unlinkSync(temp);}catch{}}
 }
 detail(block,result){
  const record=this.entry(block.id);
  const id=record.taskId||block.id;
  const prompt=blockText(block.input?.prompt,messageLimit);
  const output=blockText(result?.body||record.summary,messageLimit);
  const status=record.status;
  const type=blockText(block.input?.subagent_type,120);
  return {
   tool:'spawnAgent',...(prompt?{prompt}:{}),receiverThreadIds:[id],sourceHistoryAvailable:false,
   ...(type?{agentParticipants:[{id,type}]}:{}),
   agentsStates:{[id]:{...(status?{status}:{}),...(output?{message:output}:{})}},
   agentHistory:record.messages.length?{[id]:{relationVerified:true,complete:false,messages:record.messages}}:{}
  };
 }
 rowStatus(block,result){return this.records.get(block.id)?.status||result?.status||'';}
}

module.exports={ClaudeSubagents,isSubagent};
