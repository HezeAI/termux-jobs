'use strict';
// Keep fixtures private to this checkout (or its containing workspace archive).
// Real paths avoid Unix socket limits introduced by nested .tools symlinks.
const fs=require('node:fs'),path=require('node:path');
const source=path.resolve(__dirname,'../..');let workspace=source;
for(let p=source;;p=path.dirname(p)){
 if(fs.existsSync(path.join(p,'.git'))){workspace=p;break;}
 if(path.dirname(p)===p)break;
}
const marker=path.join(workspace,'.git');if(fs.statSync(marker).isFile()){const git=path.resolve(workspace,fs.readFileSync(marker,'utf8').trim().replace(/^gitdir:\s*/,''));const common=path.join(git,'commondir');if(fs.existsSync(common))workspace=path.dirname(path.resolve(git,fs.readFileSync(common,'utf8').trim()));}
const base=path.join(fs.realpathSync(workspace),'.verification','t');fs.mkdirSync(base,{recursive:true});
function fixture(label){return fs.mkdtempSync(path.join(base,label.replace(/[^a-z0-9]/gi,'').slice(0,5)+'-'));}
module.exports={fixture};
