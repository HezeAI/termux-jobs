'use strict';
// Use a short path even when a fixture runs from a staged copy or a worktree.
const fs=require('node:fs'),path=require('node:path');
const source=path.resolve(__dirname,'../..');
let workspace=source;
for(let parent=source;;parent=path.dirname(parent)){
 if(fs.existsSync(path.join(parent,'.git'))){workspace=parent;break;}
 if(path.dirname(parent)===parent)break;
}
const marker=path.join(workspace,'.git');
if(fs.existsSync(marker)&&fs.statSync(marker).isFile()){
 const git=path.resolve(workspace,fs.readFileSync(marker,'utf8').trim().replace(/^gitdir:\s*/,''));
 const common=path.join(git,'commondir');
 if(fs.existsSync(common))workspace=path.dirname(path.resolve(git,fs.readFileSync(common,'utf8').trim()));
}
const base=process.env.FORK_TEST_TMPDIR||path.join(fs.realpathSync(workspace),'.cx');
if(!path.isAbsolute(base))throw Error('FORK_TEST_TMPDIR must be an absolute directory');
fs.mkdirSync(base,{recursive:true,mode:0o700});
function fixture(label){return fs.mkdtempSync(path.join(fs.realpathSync(base),label.replace(/[^a-z0-9]/gi,'').slice(0,5)+'-'));}
module.exports={fixture};
