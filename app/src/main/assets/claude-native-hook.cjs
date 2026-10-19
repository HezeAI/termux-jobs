'use strict';
// Notification only: never grants a native permission or emits model instructions.
const net=require('node:net');let input='';process.stdin.setEncoding('utf8');
process.stdin.on('data',data=>{input+=data;if(Buffer.byteLength(input)>1048576)process.exit(1);});
process.stdin.on('end',()=>{try{const value=JSON.parse(input),socket=process.env.JOBS_CLAUDE_EVENT_SOCKET,token=process.env.JOBS_CLAUDE_EVENT_TOKEN;if(!socket||!token)return;const c=net.createConnection(socket);c.setTimeout(1500,()=>c.destroy());c.on('error',()=>{});c.on('connect',()=>c.end(JSON.stringify({op:'native-event',token,event:value.hook_event_name,session:value.session_id,cwd:value.cwd})+'\n'));}catch{}});
