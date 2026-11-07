'use strict';
const modes=['manual','acceptEdits','plan','auto','bypassPermissions'];
function reason(mode,help,uid){
 if(!modes.includes(mode))return 'Invalid Claude permission mode.';
 if(mode==='bypassPermissions'&&uid===0)return 'Bypass permissions is unavailable: Claude refuses this mode with root/sudo privileges in Ubuntu/proot.';
 if(['auto','bypassPermissions'].includes(mode)&&!help.includes('"'+mode+'"'))return mode+' is unavailable: the installed Claude CLI did not confirm support.';
 return '';
}
module.exports={modes,reason};
