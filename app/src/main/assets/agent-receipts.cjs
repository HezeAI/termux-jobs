'use strict';
// Private per-chat approval receipts. No authentication data or scalar-status fields.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const clean=v=>String(v??'').replace(/[\x00-\x08\x0b\x0c\x0e-\x1f\x7f\u202a-\u202e\u2066-\u2069]/g,c=>'\\u'+c.charCodeAt(0).toString(16).padStart(4,'0'));
class ReceiptStore {
 constructor(file,agent){this.file=file;this.agent=agent;this.rows=[];try{if(fs.statSync(file).size<=2097152){const rows=JSON.parse(fs.readFileSync(file,'utf8'));if(Array.isArray(rows))this.rows=rows.filter(r=>this.valid(r)).slice(-200);}}catch{}}
 valid(r){return r&&/^[a-f0-9]{8}$/.test(r.token)&&typeof r.key==='string'&&r.key.length<=256&&['proceed_once','proceed_always','cancel'].includes(r.outcome)&&typeof r.title==='string'&&r.title.length<=160&&typeof r.body==='string'&&r.body.length<=40000;}
 get(key){return this.rows.find(r=>r.key===key);}
 token(token){return this.rows.find(r=>r.token===token);}
 add(key,outcome,title,body){if(typeof key!=='string'||!key||key.length>256||!['proceed_once','proceed_always','cancel'].includes(outcome))return null;const old=this.get(key);if(old)return old;
  let token;do{token=crypto.randomBytes(4).toString('hex');}while(this.token(token));
  const row={key,token,outcome,title:clean(title).slice(0,160),body:clean(body).slice(0,40000)};this.rows.push(row);
  while(this.rows.length>200||Buffer.byteLength(JSON.stringify(this.rows))>2097152)this.rows.shift();
  if(this.file){const dir=path.dirname(this.file);fs.mkdirSync(dir,{recursive:true,mode:0o700});const temp=this.file+'.'+crypto.randomBytes(8).toString('hex');try{fs.writeFileSync(temp,JSON.stringify(this.rows),{mode:0o600,flag:'wx'});fs.renameSync(temp,this.file);}finally{try{fs.unlinkSync(temp);}catch{}}}return row;
 }
 label(row,ru){if(!row)return '';const outcome=row.outcome==='cancel'?(ru?'Отклонено':'Denied'):row.outcome==='proceed_always'?(ru?(this.agent==='O'?'Разрешено для проекта':'Разрешено на сессию'):(this.agent==='O'?'Allowed for project':'Allowed for session')):(ru?'Разрешено разово':'Allowed once');return 'ⓘ '+outcome+' ['+this.agent+':'+row.token+']';}
}
module.exports={ReceiptStore,clean};
