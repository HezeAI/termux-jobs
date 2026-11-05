'use strict';
// Only the native get_usage plan windows: API response headers/session token costs are unrelated.
function window(value){
 if(!value||typeof value!=='object'||typeof value.utilization!=='number'||!Number.isFinite(value.utilization)||value.utilization<0||value.utilization>100)return null;
 const reset=typeof value.resets_at==='string'?Date.parse(value.resets_at):NaN;
 return {usedPercent:value.utilization,...(Number.isFinite(reset)&&reset>0?{resetsAt:reset}:{})};
}
function parse(value,now){
 const available=value?.rate_limits_available===true;
 const plan=typeof value?.subscription_type==='string'&&['pro','max','ultra','team','enterprise'].includes(value.subscription_type)?value.subscription_type:'';
 return {source:'native-get-usage',available,plan,updatedAt:now,stale:false,
  fiveHour:available?window(value?.rate_limits?.five_hour):null,
  sevenDay:available?window(value?.rate_limits?.seven_day):null};
}
module.exports={parse};
