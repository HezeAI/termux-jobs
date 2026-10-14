'use strict';
// Claude-only CLI owner. It does not use Codex/OpenCode servers or credentials.
const fs = require('node:fs');
const net = require('node:net');
const {spawn} = require('node:child_process');
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
function argsFor(model, mode, chat, thread) {
  if (!['', 'sonnet', 'opus', 'haiku'].includes(model) || !['manual', 'acceptEdits', 'plan'].includes(mode)
      || !['new', 'choose', 'id'].includes(chat) || !uuid.test(thread)) throw new Error('Invalid Claude profile');
  const args = ['--permission-mode', mode];
  if (model) args.push('--model', model);
  if (chat === 'new') args.push('--session-id', thread);
  if (chat === 'choose') args.push('--resume');
  if (chat === 'id') args.push('--resume', thread);
  return args;
}
async function run(socket, model, mode, chat, thread) {
  const args = argsFor(model, mode, chat, thread);
  if (!uuid.test(require('node:path').basename(socket, '.sock'))) throw new Error('Invalid control socket');
  // Unique run sockets: refuse to overwrite a controller that might still be live.
  if (fs.existsSync(socket)) throw new Error('Claude controller socket already exists');
  const binary = process.env.JOBS_CLAUDE_BINARY;
  if (!binary || !require('node:path').isAbsolute(binary)) throw new Error('Missing Claude runtime');
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
  await new Promise((resolve, reject) => {server.once('error', reject); server.listen(socket, resolve);});
  fs.chmodSync(socket, 0o600);
  for (const signal of ['SIGTERM','SIGHUP','SIGINT']) process.on(signal, stop);
  child = spawn(binary, args, {stdio:'inherit', env:{...process.env, DISABLE_AUTOUPDATER:'1', CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC:'1'}});
  child.once('error', () => {console.error('Cannot start Claude Code'); finish(1);});
  child.once('exit', code => finish(code));
  watch = setInterval(() => {if (process.ppid !== parent) stop();}, 500);
}
function stopProxy(socket) {
  return new Promise((resolve, reject) => {
    const client = net.createConnection(socket); let response = '';
    client.setTimeout(5500, () => client.destroy(new Error('Claude stop timeout')));
    client.on('connect', () => client.write('{"op":"stop"}\n'));
    client.on('data', data => {response += data; if (response.length > 1024) client.destroy(new Error('Oversized response'));});
    client.on('error', reject);
    client.on('end', () => {try {if (!JSON.parse(response).ok) throw new Error('Stop failed'); resolve();} catch(e) {reject(e);}});
  });
}
module.exports = {argsFor, run, stopProxy};
if (require.main === module) {
  const [op, socket, ...args] = process.argv.slice(2);
  (op === 'run' ? run(socket, ...args) : op === 'stop' ? stopProxy(socket) : Promise.reject(new Error('Unknown operation')))
    .catch(() => {console.error('Claude session controller unavailable'); process.exitCode=1;});
}
