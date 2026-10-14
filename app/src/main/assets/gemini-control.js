'use strict';
// Gemini-only CLI owner. It does not use Codex/OpenCode servers or credentials.
const fs = require('node:fs');
const net = require('node:net');
const path = require('node:path');
const os = require('node:os');
const {spawn} = require('node:child_process');
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
function argsFor(model, mode, chat, thread) {
  if (!['', 'auto', 'flash', 'pro'].includes(model) || !['default', 'auto_edit', 'plan'].includes(mode)
      || !['new', 'choose', 'id'].includes(chat) || !uuid.test(thread)) throw new Error('Invalid Gemini profile');
  const args = ['--approval-mode', mode];
  if (model) args.push('--model', model);
  if (chat === 'new') args.push('--session-id', thread);
  if (chat === 'choose') args.push('--prompt-interactive', '/resume');
  if (chat === 'id') args.push('--resume', thread);
  return args;
}
// Explicit session preference uses the native enforcedType setting, never simulated keys.
function authEnvironment(mode, file, original = process.env) {
  if (!['auto','google','api'].includes(mode)) throw new Error('Invalid Gemini auth mode');
  const env = {...original};
  if (mode === 'auto') return {env, cleanup:()=>{}};
  // Preserve externally managed settings/policy discovery. Do not replace them.
  if (original.GEMINI_CLI_SYSTEM_SETTINGS_PATH || original.GEMINI_CLI_SYSTEM_DEFAULTS_PATH
      || fs.existsSync('/etc/gemini-cli')) {
    throw new Error('External Gemini system settings detected. Select terminal authentication mode.');
  }
  const type = mode === 'google' ? 'oauth-personal' : 'gemini-api-key';
  // Native Gemini rejects system settings with a world-writable ancestor (/tmp).
  // Keep only the control socket in /tmp; settings belong to the private runtime.
  const directory = path.dirname(file);
  fs.mkdirSync(directory, {recursive:true, mode:0o700});
  const info = fs.lstatSync(directory);
  if (!info.isDirectory() || info.isSymbolicLink() || (info.mode & 0o077)) {
    throw new Error('Gemini authentication directory is not private');
  }
  fs.writeFileSync(file, JSON.stringify({security:{auth:{selectedType:type,enforcedType:type}}}), {mode:0o600,flag:'wx'});
  env.GEMINI_CLI_SYSTEM_SETTINGS_PATH = file;
  env.GEMINI_CLI_SYSTEM_DEFAULTS_PATH = '/etc/gemini-cli/system-defaults.json';
  delete env.GEMINI_CLI_AUTH_OVERRIDE;
  return {env, cleanup:()=>{try{fs.unlinkSync(file);}catch{}}};
}
async function run(socket, model, mode, chat, thread, auth = 'auto') {
  const args = argsFor(model, mode, chat, thread);
  if (process.env.JOBS_GEMINI_DEBUG === '1') args.push('--debug');
  if (!uuid.test(require('node:path').basename(socket, '.sock'))) throw new Error('Invalid control socket');
  // Unique run sockets: refuse to overwrite a controller that might still be live.
  if (fs.existsSync(socket)) throw new Error('Gemini controller socket already exists');
  const binary = process.env.JOBS_GEMINI_BINARY;
  if (!binary || !require('node:path').isAbsolute(binary)) throw new Error('Missing Gemini runtime');
  const authRoot = process.env.JOBS_GEMINI_AUTH_DIR
    || path.join(os.homedir(), '.local/share/termux-jobs/gemini-cli/auth');
  if (!path.isAbsolute(authRoot)) throw new Error('Invalid Gemini authentication directory');
  const authConfig = authEnvironment(auth, path.join(authRoot, path.basename(socket, '.sock') + '.json'));
  let child, finished = false, stopping = false, timer, watch;
  const peers = new Set();
  const parent = process.ppid;
  const server = net.createServer(peer => {
    peers.add(peer); peer.on('close', () => peers.delete(peer)); peer.on('error', () => {});
    peer.setTimeout(6000, () => peer.destroy());
    let input = '';
    peer.on('data', data => {
      input += data; if (input.length > 1024) return peer.destroy();
      if (input.includes('\n')) {
        let request; try { request = JSON.parse(input.split('\n')[0]); } catch { return peer.destroy(); }
        if (request.op !== 'stop') return peer.end('{"ok":false}\n');
        stop(); // Acknowledge only after the owned child exits.
      }
    });
  });
  const finish = code => {
    if (finished) return; finished = true;
    clearTimeout(timer); clearInterval(watch);
    for (const peer of peers) peer.end('{"ok":true}\n');
    server.close();
    authConfig.cleanup();
    try { fs.unlinkSync(socket); } catch {}
    process.exitCode = Number.isInteger(code) ? code : (stopping ? 0 : 1);
  };
  const stop = () => {
    if (stopping || finished) return; stopping = true;
    if (!child || child.exitCode !== null || child.signalCode !== null) return finish(0);
    child.kill('SIGTERM');
    timer = setTimeout(() => {
      if (!finished && child.exitCode === null && child.signalCode === null) child.kill('SIGKILL');
    }, 3000);
  };
  try {await new Promise((resolve, reject) => {server.once('error', reject); server.listen(socket, resolve);});}
  catch(error){authConfig.cleanup();throw error;}
  fs.chmodSync(socket, 0o600);
  for (const signal of ['SIGTERM','SIGHUP','SIGINT']) process.on(signal, stop);
  let restarts = 0, lastRestart = 0, authOverride = process.env.GEMINI_CLI_AUTH_OVERRIDE;
  const launch = () => {
    const env = {...authConfig.env, GEMINI_CLI_NO_RELAUNCH:'1'};
    delete env.GEMINI_CLI_AUTH_OVERRIDE;
    if (auth === 'auto' && authOverride) env.GEMINI_CLI_AUTH_OVERRIDE = authOverride;
    const next = spawn(process.execPath, [binary, ...args], {stdio:['inherit','inherit','inherit','ipc'], env});
    child = next; authOverride = undefined;
    next.on('message', message => {
      if (message && message.type === 'auth-selected-type' && typeof message.authType === 'string'
          && /^[a-z-]{1,64}$/.test(message.authType)) authOverride = message.authType;
    });
    next.once('error', () => {console.error('Cannot start Gemini CLI'); finish(1);});
    next.once('exit', code => {
      // Official CLI uses 199 after trust/auth changes. Keep one owned control socket.
      if (code === 199 && !stopping && !finished) {
        const now = Date.now(); if (now-lastRestart > 60000) restarts = 0;
        lastRestart = now;
        if (++restarts <= 5) return launch();
        console.error('Gemini CLI restart limit reached');
      }
      finish(code);
    });
  };
  launch();
  watch = setInterval(() => {if (process.ppid !== parent) stop();}, 500);
}
function stopProxy(socket) {
  return new Promise((resolve, reject) => {
    const client = net.createConnection(socket); let response = '';
    client.setTimeout(5500, () => client.destroy(new Error('Gemini stop timeout')));
    client.on('connect', () => client.write('{"op":"stop"}\n'));
    client.on('data', data => {response += data; if (response.length > 1024) client.destroy(new Error('Oversized response'));});
    client.on('error', reject);
    client.on('end', () => {try {if (!JSON.parse(response).ok) throw new Error('Stop failed'); resolve();} catch(e) {reject(e);}});
  });
}
module.exports = {argsFor, run, stopProxy, authEnvironment};
if (require.main === module) {
  const [op, socket, ...args] = process.argv.slice(2);
  (op === 'run' ? run(socket, ...args) : op === 'stop' ? stopProxy(socket) : Promise.reject(new Error('Unknown operation')))
    .catch(error => {console.error(error.message === 'External Gemini system settings detected. Select terminal authentication mode.' ? error.message : 'Gemini session controller unavailable'); process.exitCode=1;});
}
