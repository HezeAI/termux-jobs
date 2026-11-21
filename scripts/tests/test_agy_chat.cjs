'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const net = require('node:net');
const path = require('node:path');
const childProcess = require('node:child_process');
const { EventEmitter } = require('node:events');
const { PassThrough } = require('node:stream');
for (const name of ['spawn', 'spawnSync', 'exec', 'execFile', 'execSync', 'execFileSync', 'fork']) {
    childProcess[name] = () => { throw Error('Real subprocess blocked'); };
}
process.kill = () => { throw Error('Real signal blocked'); };

const { AntigravityChat, serve } = require('../../app/src/main/assets/agy-chat.cjs');
const { fixture } = require('./fixture-paths.cjs');
const root = fixture('agy');
const conversation = 'b7078b35-47df-44e0-92db-e5e8c9bbff8f';
const otherConversation = '8c44dcde-3504-4d26-9929-5ab05936064e';
const childConversation = '0be921ce-0542-4f2d-9381-56270bfe658f';
const secondChild = 'e4c61047-d37f-4ea3-a918-6332d07d26ee';
const session = 'app-chat-1';
const executable = '/verified/agy';
const q = (op, fields = {}) => ({op, session, ...fields});
const message = id => q('chat-send', {id: 'msg_' + id, text: 'example ' + id, kind: 'message'});

function fakeRuntime() {
    const children = [];
    const starts = new Map();
    const signals = [];
    const options = {
        ackTimeout: 25,
        identity: pid => starts.get(pid),
        spawn(file, argv, config) {
            const child = new EventEmitter();
            child.pid = 2048 + children.length;
            child.exitCode = child.signalCode = null;
            starts.set(child.pid, {parent: process.pid, start: String(child.pid * 4)});
            child.stdin = new PassThrough();
            child.stdout = new PassThrough();
            child.stderr = new PassThrough();
            const record = {child, argv, config, inputs: [], file};
            children.push(record);
            record.exit = (code = 0) => {
                child.exitCode = code;
                child.stdout.end();
                child.stderr.end();
                setImmediate(() => child.emit('close', code));
            };
            child.kill = signal => {
                signals.push([child.pid, signal]);
                record.exit();
                return true;
            };
            child.stdin.on('data', bytes => record.inputs.push(JSON.parse(bytes)));
            child.stdin.once('end', () => { if (!record.ignoreInputEnd) record.exit(); });
            queueMicrotask(() => child.emit('spawn'));
            return child;
        },
    };
    return {options, children, starts, signals};
}

const init = (id = conversation) => ({event: 'init', conversation_id: id,
    init: {permission_mode: 'request-review'}});
const step = (kind, text) => ({event: 'step_update', step_update: {
    conversation_id: conversation, step_type: kind, state: 'DONE', text_delta: text,
}});
const subagentStep = (index, state, subagents) => ({event: 'step_update', step_update: {
    conversation_id: conversation, step_type: 'tool', step_index: index, state,
    ...(subagents === undefined ? {} : {subagent_info: {subagents}}),
}});
const result = response => ({event: 'result', result: {
    conversation_id: conversation, status: 'SUCCESS', response,
}});
const frame = (worker, data) => worker.child.stdout.write(JSON.stringify(data) + '\n');
const settle = () => new Promise(resolve => setImmediate(resolve));

async function stream() {
    const rt = fakeRuntime();
    const file = path.join(root, 'stream.json');
    const chat = new AntigravityChat(file, session, executable, '', rt.options);
    assert.equal(chat.command(q('status')).nativeChat, true);
    assert.equal(rt.children.length, 0);
    assert.equal(chat.command(q('chat-send', {...message('bad'), kind: 'shell'})).error, 'UNSUPPORTED_INPUT');
    assert.equal(chat.command(q('chat-send', {...message('bad'), attachments: [{}]})).error, 'UNSUPPORTED_INPUT');
    assert.equal(chat.command(q('chat-send', {...message('bad'), session: 'foreign'})).error, 'STALE_SESSION');

    chat.startStream();
    const worker = rt.children[0];
    assert.equal(worker.file, executable);
    assert.deepEqual(worker.argv, ['--mode', 'default', '--input-format', 'stream-json', '--output-format', 'stream-json']);
    assert.equal(chat.command(q('status')).connected, false);
    assert.equal(chat.command(message('one')).error, 'BUSY');
    assert.deepEqual(worker.inputs, []);
    assert.equal(fs.existsSync(file), false);
    const encoded = JSON.stringify(init()) + '\n';
    worker.child.stdout.write(encoded.slice(0, 18));
    worker.child.stdout.write(encoded.slice(18));
    assert.equal(chat.command(q('status')).connected, true);
    const one = chat.command(message('one'));
    assert.deepEqual(worker.inputs, [{event: 'user', message: {content: 'example one'}}]);
    assert.equal(chat.command(message('one')), one);
    assert.equal(chat.command(message('two')).error, 'BUSY');
    frame(worker, step('user_input'));
    assert.equal((await one).ack, 'native-user-input');
    frame(worker, step('agent_response', 'first '));
    let snap = chat.command(q('chat-snapshot'));
    assert.equal(snap.rows[1].body, 'first ');
    assert.equal(snap.stopToken, 'msg_one');
    assert.equal(snap.requestTiming.request, 'msg_one');
    frame(worker, step('agent_response', 'response'));
    frame(worker, result('first response'));
    assert.equal(chat.command(q('chat-snapshot')).rows[1].body, 'first response');
    assert.ok(chat.command(q('chat-snapshot')).rows[1].requestSummary.completedAt > 0);

    for (let i = 0; i < 12; i++) {
        const next = chat.command(message(String(i)));
        frame(worker, step('user_input'));
        frame(worker, result('x'.repeat(28000)));
        assert.equal((await next).accepted, true);
    }
    snap = chat.command(q('chat-snapshot'));
    assert.ok(snap.older);
    const older = chat.command(q('chat-snapshot', {cursor: snap.older}));
    assert.equal(older.rows[0].message, 'msg_one');
    assert.equal(older.older, '');
    const detail = chat.command(q('chat-detail', {message: 'msg_11', part: 'text'}));
    assert.equal(detail.body.length, 24000);
    assert.deepEqual(chat.command(q('chat-detail', {message: 'msg_11', part: 'text', offset: detail.next})),
        {body: 'x'.repeat(4000), next: null});

    const running = chat.command(message('cancel'));
    frame(worker, step('user_input'));
    assert.equal((await running).accepted, true);
    assert.equal(chat.command(q('chat-interrupt', {stopToken: 'msg_one'})).error, 'STALE_REQUEST');
    const identity = rt.starts.get(worker.child.pid);
    rt.starts.set(worker.child.pid, {...identity, start: 'replacement'});
    assert.equal(chat.command(q('chat-interrupt', {stopToken: 'msg_cancel'})).error, 'STOP_UNCONFIRMED');
    assert.deepEqual(rt.signals, []);
    rt.starts.set(worker.child.pid, identity);
    assert.equal(chat.command(q('chat-interrupt', {stopToken: 'msg_cancel'})).interrupted, true);
    assert.deepEqual(rt.signals, [[worker.child.pid, 'SIGINT']]);
    await chat.worker.closed;

    const reopened = new AntigravityChat(file, session, executable, 'model-test', rt.options);
    assert.equal(reopened.command(message('one')).accepted, true);
    assert.equal(reopened.command(message('new')).error, 'BUSY');
    const resumed = rt.children.at(-1);
    assert.deepEqual(resumed.argv, ['--mode', 'default', '--conversation', conversation, '--model', 'model-test',
        '--input-format', 'stream-json', '--output-format', 'stream-json']);
    frame(resumed, init(otherConversation));
    assert.deepEqual(resumed.inputs, []);
    assert.deepEqual(rt.signals.at(-1), [resumed.child.pid, 'SIGTERM']);
    assert.equal(reopened.state.conversation, conversation);
}

async function authAndTerminal() {
    const rt = fakeRuntime();
    const chat = new AntigravityChat(path.join(root, 'auth.json'), session, executable, '', rt.options);
    chat.startStream();
    const unauthenticated = rt.children[0];
    unauthenticated.ignoreInputEnd = true;
    unauthenticated.child.stderr.write('Authentication requ');
    unauthenticated.child.stderr.write('ired. Private fixture diagnostic');
    let snap = chat.command(q('chat-snapshot'));
    assert.equal(unauthenticated.child.exitCode, null);
    assert.equal(snap.needsTerminal, true);
    assert.equal(snap.authRequired, true);
    assert.deepEqual(snap.rows, []);
    assert.equal(JSON.stringify(snap).includes('diagnostic'), false);
    assert.equal(chat.command(message('signin')).error, 'AUTH_REQUIRED');
    assert.deepEqual(unauthenticated.inputs, []);
    assert.deepEqual(await chat.command(q('terminal-open')), {opened: true});
    const terminal = rt.children.at(-1);
    assert.equal(terminal.config.stdio, 'inherit');
    assert.deepEqual(terminal.argv, ['--mode', 'default']);
    assert.equal(chat.command(message('signin')).error, 'BUSY');
    assert.equal(chat.command(q('chat-snapshot')).authRequired, true);
    assert.deepEqual(await chat.command(q('terminal-close')), {closed: true});
    assert.deepEqual(rt.signals, [[unauthenticated.child.pid, 'SIGTERM'], [terminal.child.pid, 'SIGTERM']]);
    assert.equal(chat.command(q('chat-snapshot')).authRequired, true);
    const worker = rt.children.at(-1);
    assert.deepEqual(worker.inputs, []);
    frame(worker, init());
    const retry = chat.command(message('signin'));
    frame(worker, step('user_input'));
    frame(worker, result('done'));
    assert.equal((await retry).accepted, true);
    snap = chat.command(q('chat-snapshot'));
    assert.equal(snap.authRequired, false);
    assert.equal(snap.connected, true);
    assert.equal(chat.command(q('stop')).stopped, true);
    assert.equal(rt.signals.length, 2);
    await chat.worker.closed;
}

async function ambiguousRecovery() {
    const rt = fakeRuntime();
    const file = path.join(root, 'ambiguous.json');
    const chat = new AntigravityChat(file, session, executable, '', rt.options);
    chat.startStream();
    const worker = rt.children[0];
    frame(worker, init());
    const first = chat.command(message('once'));
    assert.equal((await first).delivery, 'unconfirmed');
    worker.exit(1);
    await chat.worker.closed;
    const reopen = new AntigravityChat(file, session, executable, '', rt.options);
    assert.equal(reopen.command(message('once')).delivery, 'unconfirmed');
    assert.equal(rt.children.length, 1);
    assert.equal(reopen.command({...message('once'), text: 'different'}).error, 'CHANGED_MESSAGE');
    assert.throws(() => reopen.command(q('chat-snapshot', {cursor: 'foreign:1'})));
}

async function subagentDetails() {
    const rt = fakeRuntime();
    const file = path.join(root, 'subagents.json');
    const chat = new AntigravityChat(file, session, executable, '', rt.options);
    chat.startStream();
    const worker = rt.children[0];
    frame(worker, init());
    const receipt = chat.command(message('delegate'));
    const nativeChild = {conversation_id: childConversation, type_name: 'code reviewer', role: 'review',
        log_uri: 'file:///fixture-private-log', workspace_uris: ['https://invalid.example/fixture-private-workspace'],
        prompt: 'undocumented fixture-private-task'};
    frame(worker, subagentStep(1, 'ACTIVE', [nativeChild]));
    assert.deepEqual(chat.command(q('chat-snapshot')).rows, []);
    frame(worker, step('user_input'));
    assert.equal((await receipt).accepted, true);
    frame(worker, subagentStep(-1, 'DONE', [nativeChild]));
    frame(worker, subagentStep(2, 'DONE', [{conversation_id: conversation.toUpperCase()}, {conversation_id: '../fixture'}]));
    frame(worker, subagentStep(3, 'DONE', [{conversation_id: secondChild, status: 'running'}]));
    frame(worker, subagentStep(4, 'ACTIVE', [nativeChild]));

    let rows = chat.command(q('chat-snapshot')).rows;
    assert.deepEqual(rows.map(row => row.part), ['user', 'subagent-3', 'subagent-4']);
    const launch = rows[2];
    assert.equal(launch.kind, 'tool');
    assert.equal(launch.toolType, 'collabAgentToolCall');
    assert.equal(launch.toolName, 'spawnAgent');
    assert.equal(launch.status, 'inProgress');
    const request = q('chat-detail', {message: launch.message, part: launch.part, offset: 0});
    assert.deepEqual(JSON.parse(chat.command(request).body), {
        tool: 'spawnAgent', senderThreadId: conversation, receiverThreadIds: [childConversation],
        agentParticipants: [{id: childConversation, type: 'code reviewer', role: 'review'}],
        sourceHistoryAvailable: false,
    });
    assert.equal(chat.command({...request, session: 'foreign'}).error, 'STALE_SESSION');
    assert.throws(() => chat.command({...request, message: 'msg_unrelated'}), /Unknown detail/);
    assert.throws(() => chat.command({...request, part: 'subagent-2'}), /Unknown detail/);
    assert.equal(rt.children.length, 1);
    assert.equal(worker.inputs.length, 1);

    frame(worker, subagentStep(4, 'ACTIVE', [{conversation_id: childConversation.toUpperCase()},
        {conversation_id: secondChild, type_name: 'agent', role: 'research'}]));
    frame(worker, subagentStep(4, 'DONE'));
    frame(worker, subagentStep(4, 'ACTIVE', [nativeChild]));
    rows = chat.command(q('chat-snapshot')).rows;
    assert.equal(rows[2].key, launch.key);
    assert.equal(rows[2].status, 'completed');
    assert.equal(rows.length, 3);
    const body = JSON.parse(chat.command(request).body);
    assert.deepEqual(body.receiverThreadIds, [childConversation, secondChild]);
    assert.deepEqual(body.agentParticipants, [{id: childConversation, type: 'code reviewer', role: 'review'},
        {id: secondChild, type: 'agent', role: 'research'}]);
    assert.equal(body.sourceHistoryAvailable, false);
    assert.equal(body.agentsStates, undefined);
    assert.equal(body.agentHistory, undefined);
    frame(worker, result('parent response'));
    const saved = fs.readFileSync(file, 'utf8');
    assert.equal(saved.includes('fixture-private-'), false);
    assert.equal(saved.includes('workspace_uris'), false);
    const reopened = new AntigravityChat(file, session, executable, '', rt.options);
    assert.deepEqual(reopened.command(request), chat.command(request));
    assert.equal(reopened.command(q('chat-snapshot')).rows.at(-1).body, 'parent response');
    assert.equal(rt.children.length, 1);
    const closed = chat.worker.closed;
    assert.equal(chat.command(q('stop')).stopped, true);
    await closed;
}

function request(socket, query, halfClose = false) {
    return new Promise((resolve, reject) => {
        const data = JSON.stringify(query);
        const connection = net.connect(socket, () => {
            if (halfClose) connection.end(data);
            else connection.write(data + '\n');
        });
        connection.setTimeout(1000, () => connection.destroy(Error('Socket timeout')));
        connection.on('error', reject);
        let bytes = '';
        connection.on('data', chunk => { bytes += chunk; });
        connection.on('end', () => resolve(JSON.parse(bytes)));
    });
}

async function socketProtocol() {
    const state = path.join(root, 'state');
    if (Buffer.byteLength(path.join(root, 'one.sock')) >= 108) throw Error('Use a short private FORK_TEST_TMPDIR');
    const socket = path.join(root, 'one.sock');
    const rt = fakeRuntime();
    const finished = serve(state, socket, session, process.execPath, '', rt.options);
    await new Promise((resolve, reject) => {
        let attempts = 0;
        const tick = () => {
            if (fs.existsSync(socket)) return resolve();
            if (++attempts > 100) return reject(Error('Server did not start'));
            setTimeout(tick, 10);
        };
        tick();
    });
    assert.equal(fs.statSync(socket).mode & 0o777, 0o700);
    const initial = await request(socket, q('chat-snapshot'));
    assert.equal(initial.session, session);
    assert.equal(initial.connected, false);
    assert.equal(rt.children.length, 1);
    assert.deepEqual(rt.children[0].inputs, []);
    frame(rt.children[0], {event: 'result', result: {status: 'ERROR', error: 'Authentication required'}});
    assert.equal((await request(socket, q('chat-snapshot'))).authRequired, true);
    assert.equal((await request(socket, q('permission'), true)).permission, null);
    await assert.rejects(serve(state, path.join(root, 'two.sock'), session, process.execPath), {code: 'EADDRINUSE'});
    assert.equal((await request(socket, q('stop'))).stopped, true);
    await finished;
    await settle();
    assert.equal(fs.existsSync(socket), false);
}

(async () => {
    await stream();
    await authAndTerminal();
    await ambiguousRecovery();
    await subagentDetails();
    await socketProtocol();
    console.log('PASS Antigravity stream, auth handoff, durable history, subagent details, socket framing, and owned process lifecycle fixtures');
})().catch(error => {
    console.error(error);
    process.exitCode = 1;
});
