'use strict';
const assert=require('node:assert/strict');
const {preview,files,trimPreviews}=require('../../app/src/main/assets/agent-action-preview.cjs');
const changes=[{path:'/project/main.py',kind:{type:'update'},diff:'--- a/main.py\n+++ b/main.py\n@@ -1 +1 @@\n-old\n+new\n+extra\n'},{path:'/project/new.txt',kind:{type:'add'},diff:'+hello\n'},{path:'/project/old.txt',kind:{type:'delete'},diff:'-bye\n'}];
const f=files(changes);assert.equal(f.count,3);assert.deepEqual(f.files.map(f=>f.operation),['modified','created','deleted']);assert.equal(f.files[0].added,2);assert.equal(f.files[0].removed,1);
assert.equal(files([{path:'/x',kind:{type:'update',move_path:'/y'}}]).files[0].operation,'renamed');
const long=files(Array.from({length:150},(_,i)=>({path:'/p/'+i,kind:'update',diff:'+a\n'.repeat(4000)})));assert.equal(long.count,150);assert.equal(long.files.length,100);assert(long.filesTruncated);assert(long.files[0].diffTruncated);assert(long.files.reduce((n,f)=>n+f.diff.length,0)<=16000);
assert.equal(files([{no:'path'}]),null);
assert.equal(preview({type:'imageView',path:'/sdcard/Pictures/a.jpg'}).image.path,'/sdcard/Pictures/a.jpg');
assert.equal(preview({type:'imageView',path:'relative.jpg'}),null);assert.equal(preview({type:'imageView',path:'/private/key.txt'}),null);
assert.equal(preview(null,'Read',{file_path:'/images/a.png'}).kind,'image');assert.equal(preview(null,'Read',{file_path:'/project/code.py'}).kind,'fileTool');
assert.equal(preview({type:'webSearch',action:{type:'search',query:'build Android APK'}}).query,'build Android APK');
const open=preview({type:'webSearch',action:{type:'openPage',url:'https://example.org/docs'},title:'Example docs'});assert.equal(open.operation,'open');assert.equal(open.title,'Example docs');assert.equal(open.url,'https://example.org/docs');
assert.equal(preview({type:'webSearch',action:{type:'findInPage',url:'https://example.org/',pattern:'setup'}}).operation,'find');
assert.equal(preview(null,'WebFetch',{url:'javascript:alert(1)'}).url,'');assert.equal(preview(null,'WebFetch',{url:'https://user:secret@example.org/'}).url,'');
assert.equal(preview({type:'unknown'}),null);
const rows=Array.from({length:50},(_,i)=>({key:String(i),actionPreview:{kind:'web',query:'x'.repeat(10000)}}));const trimmed=trimPreviews(rows);assert.equal(rows[0].actionPreview.query.length,10000);assert(trimmed.reduce((n,r)=>n+(r.actionPreview?JSON.stringify(r.actionPreview).length:0),0)<=60000);
console.log('PASS bounded action metadata: file operations/diff counts, original image paths, web queries/pages/find, safe URLs, unknown fallback, projection budget');

assert.equal(require('../../app/src/main/assets/agent-action-preview.cjs').outputTail('old\n\u001b[32mnew progress\u001b[0m\n'),'new progress');
assert.equal(require('../../app/src/main/assets/agent-action-preview.cjs').outputTail(null),'');
assert.equal(require('../../app/src/main/assets/agent-action-preview.cjs').outputTail('download 1%\rdownload 2%'),'download 2%');

{const helper=require('../../app/src/main/assets/agent-action-preview.cjs');const rows=helper.trimPreviews(Array.from({length:100},()=>({outputTail:'x'.repeat(320)})));assert(rows.reduce((n,r)=>n+(r.outputTail||'').length,0)<=12000);assert(rows.at(-1).outputTail);}
