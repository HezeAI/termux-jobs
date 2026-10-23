// Version-pinned, in-memory instrumentation. Installed CLI files are never rewritten.
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
const core={
 'chunk-J7SXW2FI.js':'e2aca15290b02dc9845704f3fed44c577919652508624f340974f0ff588f206e',
 'chunk-NR7L3FXV.js':'d566e81b1df8d4d44a91bf7aeea96a58ec0e6c8934fa424315ef060507dcbb8c',
 'chunk-PZ4GDG24.js':'bf23ba86cf4525ce388a13969b9f85a8d88c63f26cb2a2da181f4b401124011e'};
const ui={
 'interactiveCli-2T27ZS4G.js':'855bffea56732b070204b95ee8fcd36f2916a98e6c9f71f071bc1090110f052f',
 'interactiveCli-66E5CZDR.js':'3c51144d6951cc0549c92acac3c37e0d6dbc5d5ff54481d6400fc39f26210c77',
 'interactiveCli-IX4FVS6G.js':'2b96b4c40b9e232db00c5f38ed717bdc9e97686629ef959c877cb2d4a5604f80'};
const renderer={
 'chunk-V7G7CIBW.js':'b6442d60b5ab0d02fd6531f5e177ef77c8e06634fdb37a6b4121309fe1a4ec58',
 'chunk-MA35JFUP.js':'5afb745a6015fcdd0108c6fa33112e8250bb93774e73634641f458e89f990985',
 'chunk-KJWA3D3G.js':'c2d3d5253df5d1ab2cdb6732336fd63cc39b6db2597fa1dc9b031f06a3845b40'};
export function instrument(source,name){
 const original=source;
 const wanted=core[name]||ui[name]||renderer[name];if(!wanted)return source;
 if(createHash('sha256').update(source).digest('hex')!==wanted)return 'globalThis.__jobsGeminiLive?.unsupported();\n'+source;
 const replace=(old,next)=>{if(source.split(old).length!==2)throw Error('UNSUPPORTED_GEMINI_LAYOUT');source=source.replace(old,next);};
 try{
  if(renderer[name]){
   // Frame the native log-update writes, including legacy main-screen redraws.
   // Do not wrap global stdout/stderr or change the selected Ink renderer.
   const begin=source.indexOf('var createStandard =');
   const end=source.indexOf('// node_modules/ink/build/instances.js',begin);
   if(begin<0||end<=begin)throw Error('UNSUPPORTED_GEMINI_LAYOUT');
   const block=source.slice(begin,end);
   if(!block.includes('var createStandard =')||!block.includes('var createIncremental ='))throw Error('UNSUPPORTED_GEMINI_LAYOUT');
   const helper='const jobsGeminiFrameWrite = (stream, text) => stream.write(text.includes("\\x1B[?2026h") ? text : "\\x1B[?2026h" + text + "\\x1B[?2026l");\n';
   replace(block,helper+block.replaceAll('stream.write(', 'jobsGeminiFrameWrite(stream, '));
  } else if(core[name]){
   replace('  getModel() {\n    return this.model;\n  }','  getModel() {\n    globalThis.__jobsGeminiLive?.attach(this);\n    return this.model;\n  }');
   replace('      coreEvents.emitModelChanged(newModel);','      coreEvents.emitModelChanged(newModel);\n      globalThis.__jobsGeminiLive?.attach(this);');
   replace('      this._activeModel = model;','      this._activeModel = model;\n      globalThis.__jobsGeminiLive?.attach(this);');
   replace('function logApiRequest(config2, event) {','function logApiRequest(config2, event) {\n  globalThis.__jobsGeminiLive?.api("request",config2,event);');
   replace('function logApiResponse(config2, event) {','function logApiResponse(config2, event) {\n  globalThis.__jobsGeminiLive?.api("response",config2,event);');
  } else {
   // Observe the persistent producer after a React commit, not a transient consumer
   // (e.g. ShowMoreLines) which can unmount before idle and leave stale state.
   const marker='  const streamingState = (0, import_react97.useMemo)(\n    () => calculateStreamingState(isResponding, toolCalls),\n    [isResponding, toolCalls]\n  );';
   replace(marker,marker+'\n  (0, import_react97.useEffect)(() => {globalThis.__jobsGeminiLive?.ui(streamingState);}, [streamingState]);');
   const permissionMarker='  const availableBodyContentHeight = (0, import_react35.useCallback)(() => {';
   replace(permissionMarker,'  (0, import_react35.useEffect)(() => globalThis.__jobsGeminiLive?.permission(callId,confirmationDetails,config,confirm,getOptions(),isFocused,toolName,!!deceptiveUrlWarningText), [callId,confirmationDetails,config,confirm,getOptions,isFocused,toolName,deceptiveUrlWarningText]);\n'+permissionMarker);
   replace('    async (callId, outcome, payload) => {','    async (callId, outcome, payload) => {\n      globalThis.__jobsGeminiLive?.consumePermission(callId);');
   // Native confirmation receipt is committed only after the native callback succeeds.
   replace('      const tool = toolCalls.find((t) => t.callId === callId);','      const jobsCommitReceipt = globalThis.__jobsGeminiLive?.prepareReceipt(callId,outcome);\n      const tool = toolCalls.find((t) => t.callId === callId);');
   replace('          payload\n        });\n        return;','          payload\n        });\n        jobsCommitReceipt?.();\n        return;');
   replace('        await details.onConfirm(outcome, payload);','        await details.onConfirm(outcome, payload);\n        jobsCommitReceipt?.();');
   const receiptComponent='var JobsPermissionReceipt = ({callId,borderColor}) => {const text=(0,import_react10.useSyncExternalStore)(globalThis.__jobsGeminiLive.receiptSubscribe,()=>globalThis.__jobsGeminiLive.receiptText(callId));return text?(0,import_jsx_runtime17.jsx)(Box_default,{borderStyle:"round",borderColor,paddingX:1,alignSelf:"flex-start",children:(0,import_jsx_runtime17.jsx)(Text,{children:text})}):null;};\n';
   replace('var ToolMessage = ({',receiptComponent+'var ToolMessage = ({');
   replace('                  isCompact ? /* @__PURE__ */','                  (0,import_jsx_runtime26.jsx)(JobsPermissionReceipt,{callId:tool.callId,borderColor}),\n                  isCompact ? /* @__PURE__ */');
   const shellMarker='  const streamingState = (0, import_react97.useMemo)(';
   replace(shellMarker,'  (0, import_react97.useEffect)(() => globalThis.__jobsGeminiLive?.chatShell(config, text => handleShellCommand(text, new AbortController().signal)), [config,handleShellCommand]);\n'+shellMarker);
   const chatMarker='  const uiActions = (0, import_react119.useMemo)(';
   const chatHook='  (0, import_react119.useEffect)(() => globalThis.__jobsGeminiLive?.chatUI(config,{history:historyManager.history,pending:pendingHistoryItems,thought,ready:isInputActive && isConfigInitialized && isMcpReady && !isCompressing && !isResuming,native:!!((uiState.activePtyId && uiState.embeddedShellFocused) || uiState.dialogsVisible || uiState.isAuthenticating || uiState.authError || uiState.isAuthDialogOpen || uiState.isAwaitingApiKeyInput || uiState.isAwaitingLoginRestart || uiState.isFolderTrustDialogOpen || uiState.permissionConfirmationRequest || uiState.commandConfirmationRequest || uiState.authConsentRequest || uiState.loopDetectionConfirmationRequest || uiState.initError || uiState.customDialog),authError:!!uiState.authError},handleFinalSubmit,shellModeActive,setShellModeActive), [config,historyManager.history,pendingHistoryItems,thought,isInputActive,isConfigInitialized,isMcpReady,isCompressing,isResuming,uiState,handleFinalSubmit,shellModeActive,setShellModeActive]);\n';
   replace(chatMarker,chatHook+chatMarker);
   const approvalMarker='  const [showApprovalMode, setApprovalMode] = (0, import_react106.useState)(currentConfigValue);';
   replace(approvalMarker,approvalMarker+'\n  (0, import_react106.useEffect)(() => globalThis.__jobsGeminiLive?.approval(config, (mode) => {config.setApprovalMode(mode);setApprovalMode(mode);return onApprovalModeChange?.(mode);}, allowPlanMode, isActive), [config,onApprovalModeChange,allowPlanMode,isActive]);');
  }
  return source;
 }catch{return 'globalThis.__jobsGeminiLive?.unsupported();\n'+original;}
}
export async function load(url,context,nextLoad){
 const result=await nextLoad(url,context);
 if(process.env.JOBS_GEMINI_LIVE!=='1'||!url.startsWith('file:')||result.format!=='module')return result;
 const file=fileURLToPath(url),base=process.env.JOBS_GEMINI_BINARY;
 if(!base||path.dirname(file)!==path.dirname(base))return result;
 const name=path.basename(file);if(!core[name]&&!ui[name]&&!renderer[name])return result;
 const source=typeof result.source==='string'?result.source:Buffer.from(result.source).toString('utf8');
 return {...result,source:instrument(source,name)};
}
