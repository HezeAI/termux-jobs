'use strict';

// Local bridge for the documented Antigravity CLI stream-json protocol.
const fs = require('node:fs');
const path = require('node:path');
const net = require('node:net');
const { spawn } = require('node:child_process');
const { createHash, randomUUID } = require('node:crypto');
const { RequestClock } = require('./agent-completion-times.cjs');
const delivery = require('./agent-delivery-status.cjs');
const { clean } = require('./agent-receipts.cjs');

const FRAME_LIMIT = 2 * 1024 * 1024;
const HISTORY_LIMIT = 12 * 1024 * 1024;
const TURN_LIMIT = 200;
const RESPONSE_LIMIT = 80000;
const SUBAGENT_STEP_LIMIT = 32;
const SUBAGENT_PARTICIPANT_LIMIT = 16;
const SUBAGENT_LABEL_LIMIT = 120;
const ACK_TIMEOUT = 12000;
const validSession = value => typeof value === 'string' && /^[A-Za-z0-9][A-Za-z0-9_-]{0,127}$/.test(value);
const nativeId = value => typeof value === 'string' && /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(value);
const authRequired = value => typeof value === 'string' &&
    /authentication (?:required|failed|expired)|not (?:authenticated|signed in)|(?:please |must )(?:log|sign)[ -]?in|unauthenticated/i.test(value);
const validStepIndex = value => Number.isSafeInteger(value) && value >= 0;
const validLabel = value => typeof value === 'string' && value.length <= SUBAGENT_LABEL_LIMIT;

function subagentLabel(value) {
    return typeof value === 'string' ? clean(value.slice(0, SUBAGENT_LABEL_LIMIT))
        .replace(/\s+/g, ' ').trim().slice(0, SUBAGENT_LABEL_LIMIT) : '';
}

function validSubagentSteps(steps, parent) {
    if (!Array.isArray(steps) || steps.length > SUBAGENT_STEP_LIMIT) return false;
    const indexes = new Set();
    return steps.every(step => {
        if (!validStepIndex(step?.index) || indexes.has(step.index) ||
            !['inProgress', 'completed'].includes(step.status) ||
            !Array.isArray(step.participants) || !step.participants.length ||
            step.participants.length > SUBAGENT_PARTICIPANT_LIMIT) return false;
        indexes.add(step.index);
        const ids = new Set();
        return step.participants.every(participant => {
            if (!nativeId(participant?.id) || participant.id.toLowerCase() === parent ||
                ids.has(participant.id.toLowerCase()) || !validLabel(participant.type) ||
                !validLabel(participant.role)) return false;
            ids.add(participant.id.toLowerCase());
            return true;
        });
    });
}

function trackSubagentStep(record, payload, parent) {
    if (!validStepIndex(payload.step_index) || !['ACTIVE', 'DONE'].includes(payload.state)) return;
    const steps = record.subagentSteps || [];
    const existing = steps.find(step => step.index === payload.step_index);
    const reported = payload.subagent_info?.subagents;
    if (!existing && (!Array.isArray(reported) || steps.length >= SUBAGENT_STEP_LIMIT)) return;
    const participants = new Map(existing?.participants.map(participant =>
        [participant.id.toLowerCase(), participant]));
    if (Array.isArray(reported)) for (const agent of reported) {
        const id = agent?.conversation_id;
        if (!nativeId(id) || id.toLowerCase() === parent) continue;
        const key = id.toLowerCase(), prior = participants.get(key);
        if (!prior && participants.size >= SUBAGENT_PARTICIPANT_LIMIT) continue;
        participants.set(key, {
            id: prior?.id || id,
            type: subagentLabel(agent.type_name) || prior?.type || '',
            role: subagentLabel(agent.role) || prior?.role || '',
        });
    }
    if (!participants.size) return;
    const status = payload.state === 'DONE' || existing?.status === 'completed' ? 'completed' : 'inProgress';
    if (existing) Object.assign(existing, {status, participants: Array.from(participants.values())});
    else {
        steps.push({index: payload.step_index, status, participants: Array.from(participants.values())});
        record.subagentSteps = steps;
    }
}

function processIdentity(pid) {
    if (!Number.isSafeInteger(pid) || pid <= 1) return null;
    try {
        const stat = fs.readFileSync('/proc/' + pid + '/stat', 'utf8');
        const fields = stat.slice(stat.lastIndexOf(')') + 2).split(' ');
        if (!/^\d+$/.test(fields[19])) return null;
        return { parent: Number(fields[1]), start: fields[19] };
    } catch {
        return null;
    }
}

function loadHistory(file) {
    let fd;
    try {
        fd = fs.openSync(file, fs.constants.O_RDONLY | fs.constants.O_NOFOLLOW);
        const stat = fs.fstatSync(fd);
        if (!stat.isFile() || stat.size > HISTORY_LIMIT) throw Error('Invalid history');
        const state = JSON.parse(fs.readFileSync(fd, 'utf8'));
        if (state.version !== 1 || !Array.isArray(state.turns) || state.turns.length > TURN_LIMIT ||
            !(state.conversation === '' || nativeId(state.conversation))) throw Error('Invalid history');
        let sequence = 0;
        const ids = new Set();
        for (const turn of state.turns) {
            if (!Number.isSafeInteger(turn.seq) || turn.seq <= sequence || !delivery.validId(turn.id) ||
                ids.has(turn.id) || typeof turn.text !== 'string' || typeof turn.response !== 'string' ||
                typeof turn.status !== 'string' || !Number.isSafeInteger(turn.started) ||
                !Number.isSafeInteger(turn.completed) || typeof turn.admitted !== 'boolean' ||
                (turn.subagentSteps !== undefined && (!turn.admitted || !state.conversation ||
                    !validSubagentSteps(turn.subagentSteps, state.conversation.toLowerCase())))) {
                throw Error('Invalid history');
            }
            sequence = turn.seq;
            ids.add(turn.id);
            if (turn.status === 'running') turn.status = 'disconnected';
        }
        return state;
    } catch (error) {
        if (error.code !== 'ENOENT') throw error;
        return {version: 1, conversation: '', turns: []};
    } finally {
        if (fd !== undefined) fs.closeSync(fd);
    }
}

function writeHistory(file, state) {
    let contents = JSON.stringify(state);
    while (Buffer.byteLength(contents) > HISTORY_LIMIT && state.turns.length > 1) {
        state.turns.shift();
        contents = JSON.stringify(state);
    }
    if (Buffer.byteLength(contents) > HISTORY_LIMIT) throw Error('History limit');
    const temp = file + '.' + randomUUID();
    const fd = fs.openSync(temp, 'wx', 0o600);
    try {
        fs.writeFileSync(fd, contents);
        fs.fsyncSync(fd);
        fs.renameSync(temp, file);
    } catch (error) {
        try { fs.unlinkSync(temp); } catch {}
        throw error;
    } finally {
        fs.closeSync(fd);
    }
}

function readLines(stream, onLine, onInvalid) {
    stream.setEncoding('utf8');
    let tail = '';
    stream.on('data', chunk => {
        tail += chunk;
        let split;
        while ((split = tail.indexOf('\n')) >= 0) {
            const line = tail.slice(0, split).trim();
            tail = tail.slice(split + 1);
            if (line.length > FRAME_LIMIT) onInvalid();
            else if (line) onLine(line);
        }
        if (tail.length > FRAME_LIMIT) {
            tail = '';
            onInvalid();
        }
    });
    stream.on('end', () => { if (tail.trim()) onLine(tail); });
}

class AntigravityChat {
    constructor(file, session, executable, model, options = {}) {
        this.file = file;
        this.session = session;
        this.executable = executable;
        this.model = model;
        this.spawn = options.spawn || spawn;
        this.identity = options.identity || processIdentity;
        this.ackTimeout = options.ackTimeout || ACK_TIMEOUT;
        this.state = loadHistory(file);
        this.clock = new RequestClock();
        this.authRequired = false;
        this.stopping = false;
        this.storageFailed = false;
    }

    save() {
        try {
            writeHistory(this.file, this.state);
        } catch (error) {
            this.storageFailed = true;
            throw error;
        }
    }

    startStream() {
        if (this.stopping || this.storageFailed) return null;
        try { return this.worker || this.launch(false); }
        catch { return null; }
    }

    launch(interactive) {
        const args = ['--mode', 'default'];
        if (this.state.conversation) args.push('--conversation', this.state.conversation);
        if (this.model) args.push('--model', this.model);
        if (!interactive) args.push('--input-format', 'stream-json', '--output-format', 'stream-json');

        const child = this.spawn(this.executable, args, {stdio: interactive ? 'inherit' : ['pipe', 'pipe', 'pipe']});
        const worker = {
            child, interactive, pid: child.pid, identity: this.identity(child.pid),
            error: '', stderr: '', stopping: false, signalPending: false, initialized: false,
        };
        this.worker = worker;
        worker.closed = new Promise(resolve => {
            child.once('close', () => { this.closed(worker); resolve(); });
        });
        worker.spawned = new Promise(resolve => {
            child.once('spawn', () => {
                if (!worker.identity) worker.identity = this.identity(worker.pid);
                resolve(true);
            });
            child.once('error', () => { worker.error = 'unavailable'; resolve(false); });
        });
        if (!interactive) {
            child.stdin.on('error', () => { if (!worker.error) worker.error = 'disconnected'; });
            readLines(child.stdout, line => this.event(worker, line), () => this.protocolError(worker));
            child.stderr.setEncoding('utf8');
            child.stderr.on('data', chunk => {
                // Classify diagnostics in memory. Never send raw stderr to the app or history.
                worker.stderr = (worker.stderr + chunk).slice(-2048);
                if (!worker.stopping && authRequired(worker.stderr)) {
                    worker.error = 'auth';
                    if (!worker.initialized) this.requireSignIn(worker);
                }
            });
        }
        return worker;
    }

    matches(worker) {
        if (!worker || worker !== this.worker || worker.child.pid !== worker.pid ||
            worker.child.exitCode !== null || worker.child.signalCode !== null) return false;
        const current = this.identity(worker.pid);
        return !!current && worker.identity?.parent === process.pid && current.parent === process.pid &&
            worker.identity.start === current.start;
    }

    terminate(worker, signal = 'SIGTERM') {
        if (!this.matches(worker)) return false;
        try {
            return worker.child.kill(signal);
        } catch {
            return false;
        }
    }

    signalStop(worker, signal) {
        if (!this.terminate(worker, signal)) return false;
        worker.stopping = true;
        worker.signalPending = true;
        // Recheck both the direct child and its kernel start time before escalation.
        const timer = setTimeout(() => this.terminate(worker, 'SIGKILL'), 3500);
        timer.unref();
        worker.closed.then(() => clearTimeout(timer));
        return true;
    }

    protocolError(worker) {
        if (this.worker !== worker || worker.stopping) return;
        worker.error = 'protocol';
        if (!this.signalStop(worker, 'SIGTERM')) {
            worker.stopping = true;
            worker.child.stdin.end();
        }
    }

    requireSignIn(worker) {
        this.authRequired = true;
        worker.error = 'auth';
        worker.stopping = true;
        worker.child.stdin.end();
    }

    event(worker, line) {
        if (this.worker !== worker || worker.stopping) return;
        let frame;
        try { frame = JSON.parse(line); } catch { this.protocolError(worker); return; }
        if (!frame || typeof frame !== 'object' || Array.isArray(frame)) return;

        const turn = this.active;
        const payload = frame[frame.event];
        if (!payload || typeof payload !== 'object') return;
        const id = frame.event === 'init' ? frame.conversation_id : payload.conversation_id;

        if (frame.event === 'result' && payload.status !== 'SUCCESS' && !worker.initialized) {
            if (authRequired(payload.error)) {
                this.requireSignIn(worker);
                return;
            }
            worker.error = 'unavailable';
            if (turn?.worker === worker) this.finish(turn, worker.error);
            worker.stopping = true;
            worker.child.stdin.end();
            return;
        }
        if (!['init', 'step_update', 'result'].includes(frame.event)) return;
        if (!nativeId(id) || (this.state.conversation && id !== this.state.conversation)) {
            this.protocolError(worker);
            return;
        }
        if (frame.event === 'init') {
            if (worker.initialized || payload.permission_mode === 'always-proceed') {
                this.protocolError(worker);
                return;
            }
            this.state.conversation = id;
            try { this.save(); } catch { this.protocolError(worker); return; }
            this.authRequired = false;
            worker.initialized = true;
            return;
        }
        if (!turn || turn.worker !== worker || !worker.initialized) {
            this.protocolError(worker);
            return;
        }

        if (frame.event === 'step_update') {
            if (payload.step_type === 'user_input' && payload.state === 'DONE') this.admit(turn);
            if (payload.step_type === 'agent_response' && typeof payload.text_delta === 'string' && turn.record.admitted) {
                turn.record.response = (turn.record.response + clean(payload.text_delta)).slice(0, RESPONSE_LIMIT);
            }
            if (turn.record.admitted && !['user_input', 'agent_response'].includes(payload.step_type)) {
                trackSubagentStep(turn.record, payload, id.toLowerCase());
            }
            return;
        }

        if (payload.status === 'SUCCESS') {
            if (!this.admit(turn)) return;
            if (typeof payload.response === 'string') turn.record.response = clean(payload.response).slice(0, RESPONSE_LIMIT);
            this.finish(turn, 'success');
            return;
        }

        if (authRequired(payload.error)) {
            worker.error = 'auth';
            this.authRequired = true;
        }
        this.finish(turn, worker.error || (['CANCELED', 'INTERRUPTED'].includes(payload.status) ?
            'cancelled' : payload.status === 'WAITING' ? 'terminal' : 'failed'));
        // An error can be followed by process exit. Reopen by explicit ID on the next request.
        worker.stopping = true;
        worker.child.stdin.end();
    }

    admit(turn) {
        if (turn.record.admitted) return true;
        turn.record.admitted = true;
        try { this.save(); } catch { this.protocolError(turn.worker); return false; }
        this.clock.start(turn.record.id);
        turn.confirm(this.sendReceipt(turn.record));
        return true;
    }

    finish(turn, status) {
        if (this.active !== turn) return;
        const record = turn.record;
        const rejected = !record.admitted && ['auth', 'unavailable'].includes(status);
        if (rejected) this.state.turns = this.state.turns.filter(item => item !== record);
        record.status = status;
        record.completed = Date.now();
        try { this.save(); } catch {}
        this.clock.stop(record.id);
        this.active = null;
        if (status === 'auth') this.authRequired = true;
        turn.confirm(record.admitted ? this.sendReceipt(record) :
            rejected ? this.rejected(status === 'auth' ? 'AUTH_REQUIRED' : 'AGENT_UNAVAILABLE') :
                this.uncertain(record.id));
    }

    closed(worker) {
        if (worker !== this.worker) return;
        this.worker = null;
        if (worker.interactive) return;
        if (worker.error === 'auth') this.authRequired = true;
        if (this.active?.worker === worker) {
            this.finish(this.active, worker.cancelled ? 'cancelled' : worker.error || 'disconnected');
        }
    }

    rejected(error) {
        return {ok: false, accepted: false, delivery: 'rejected', error};
    }

    uncertain(id) {
        return {ok: false, accepted: false, delivery: 'unconfirmed', id, error: 'DELIVERY_UNCONFIRMED'};
    }

    sendReceipt(record) {
        return {accepted: true, id: record.id, delivery: 'submitted', ack: 'native-user-input'};
    }

    send(q) {
        if (!delivery.validId(q.id) || typeof q.text !== 'string' || !q.text.trim() ||
            Buffer.byteLength(q.text) > 32000 || q.kind !== 'message' ||
            (q.attachments !== undefined && (!Array.isArray(q.attachments) || q.attachments.length))) {
            return this.rejected('UNSUPPORTED_INPUT');
        }
        const previous = this.state.turns.find(turn => turn.id === q.id);
        if (previous) {
            if (previous.text !== q.text) return this.rejected('CHANGED_MESSAGE');
            if (previous.admitted) return this.sendReceipt(previous);
            return this.active?.record === previous ? this.active.receipt : this.uncertain(q.id);
        }
        if (this.stopping || this.storageFailed) return this.rejected('AGENT_UNAVAILABLE');
        if (this.active || this.worker?.interactive) return this.rejected('BUSY');
        if (this.authRequired) return this.rejected('AUTH_REQUIRED');
        const worker = this.startStream();
        if (!worker) return this.rejected('AGENT_UNAVAILABLE');
        // Native stdin may be awaiting an authorization code during startup.
        if (!worker.initialized || worker.stopping) return this.rejected('BUSY');
        const record = {
            seq: (this.state.turns.at(-1)?.seq || 0) + 1, id: q.id, text: q.text, response: '',
            started: Date.now(), completed: 0, status: 'running', admitted: false,
        };
        this.state.turns.push(record);
        if (this.state.turns.length > TURN_LIMIT) this.state.turns.shift();
        try { this.save(); } catch { return this.rejected('HISTORY_UNAVAILABLE'); }

        let confirm;
        const receipt = new Promise(resolve => { confirm = resolve; });
        const timer = setTimeout(() => confirm(this.uncertain(record.id)), this.ackTimeout);
        const turn = {record, worker, receipt, confirm: value => { clearTimeout(timer); confirm(value); }};
        this.active = turn;
        worker.child.stdin.write(JSON.stringify({event: 'user', message: {content: q.text}}) + '\n');
        return receipt;
    }

    async waitForClose(worker, timeoutMs) {
        let timer;
        try {
            const timeout = new Promise(resolve => { timer = setTimeout(() => resolve(false), timeoutMs); });
            return await Promise.race([worker.closed.then(() => true), timeout]);
        } finally {
            clearTimeout(timer);
        }
    }

    async terminal(open) {
        if (this.stopping) return {ok: false, error: 'AGENT_UNAVAILABLE'};
        if (!open) {
            const worker = this.worker;
            if (!worker?.interactive) {
                if (!worker) this.startStream();
                return {closed: true};
            }
            if (!worker.signalPending && !this.signalStop(worker, 'SIGTERM')) {
                return {ok: false, error: 'STOP_UNCONFIRMED'};
            }
            if (!await this.waitForClose(worker, 4500)) return {ok: false, error: 'STOP_UNCONFIRMED'};
            this.startStream();
            return {closed: true};
        }
        if (this.worker?.interactive) {
            if (this.worker.stopping) return {ok: false, error: 'BUSY'};
            return {opened: await this.worker.spawned};
        }
        if (this.active) return {ok: false, error: 'BUSY'};
        if (this.worker) {
            const worker = this.worker;
            if (worker.error === 'auth' && !worker.signalPending) this.signalStop(worker, 'SIGTERM');
            if (!worker.stopping) {
                worker.stopping = true;
                worker.child.stdin.end();
            }
            // Wait for the idle stream to release the conversation before the
            // interactive process opens it.
            const closed = await this.waitForClose(worker, 3500);
            if (!closed || this.stopping) return {ok: false, error: 'BUSY'};
            if (this.worker?.interactive) return {opened: await this.worker.spawned};
        }
        const worker = this.launch(true);
        const opened = await worker.spawned;
        return opened ? {opened: true} : {ok: false, error: 'AGENT_UNAVAILABLE'};
    }

    stop() {
        if (this.stopping) return true;
        const worker = this.worker;
        if (worker?.interactive || this.active) {
            if (!worker.signalPending && !this.signalStop(worker, 'SIGTERM')) return false;
            if (this.active) worker.cancelled = true;
        } else if (worker && !worker.signalPending) {
            if (!worker.stopping) {
                worker.stopping = true;
                worker.child.stdin.end();
            }
            const timer = setTimeout(() => this.signalStop(worker, 'SIGTERM'), 3500);
            timer.unref();
            worker.closed.then(() => clearTimeout(timer));
        }
        this.stopping = true;
        return true;
    }

    turnRows(turn) {
        const rows = [];
        const add = (part, kind, body, extra = {}) => rows.push({
            key: turn.id + ':' + part, message: turn.id, part, kind, body,
            messageSender: kind === 'user' ? 'user' : 'agent',
            messageTime: kind === 'user' ? turn.started : turn.completed || turn.started, ...extra,
        });
        if (!turn.admitted) return rows;
        const receipt = delivery.receipt(this.sendReceipt(turn), turn.id);
        add('user', 'user', clean(turn.text), {deliveryReceipt: receipt});
        for (const step of turn.subagentSteps || []) {
            const body = {
                tool: 'spawnAgent', senderThreadId: this.state.conversation,
                receiverThreadIds: step.participants.map(participant => participant.id),
                agentParticipants: step.participants.map(({id, type, role}) => ({id, type, role})),
                // The CLI supplies launch metadata. No read-only child history API is used.
                sourceHistoryAvailable: false,
            };
            add('subagent-' + step.index, 'tool', JSON.stringify(body), {
                toolType: 'collabAgentToolCall', toolName: 'spawnAgent', status: step.status,
            });
        }
        if (turn.response) add('text', 'text', turn.response, turn.status === 'success' ? {
            requestSummary: {request: turn.id, completedAt: turn.completed, durationMs: Math.max(0, turn.completed - turn.started)},
        } : {});
        if (!['success', 'running', 'cancelled'].includes(turn.status)) {
            const ru = process.env.JOBS_AI_LANGUAGE === 'ru';
            add('notice', 'error', ru ? 'Запрос Antigravity прерван. Проверьте состояние агента в терминале.' :
                'Antigravity did not complete the request. Check the agent in the terminal.');
        }
        return rows;
    }

    snapshot(cursor) {
        const turns = this.state.turns.filter(turn => turn.admitted);
        const lastStatus = this.state.turns.at(-1)?.status;
        let end = turns.length;
        if (cursor !== undefined && cursor !== '') {
            const prefix = this.session + ':';
            if (typeof cursor !== 'string' || !cursor.startsWith(prefix) ||
                !/^\d{1,15}$/.test(cursor.slice(prefix.length))) throw Error('Invalid cursor');
            end = turns.findIndex(turn => turn.seq === Number(cursor.slice(prefix.length)));
            if (end < 0) return {...this.snapshot(), historyReset: true};
        }
        const start = Math.max(0, end - 12);
        const rows = turns.slice(start, end).flatMap(turn => this.turnRows(turn));
        const limit = Math.min(12000, Math.floor(48000 / Math.max(1, rows.length)));
        return {
            nativeChat: true, connected: this.worker?.initialized === true &&
                !this.worker.stopping && !this.authRequired && !this.storageFailed && !this.stopping,
            session: this.session, id: this.session, model: {id: this.model}, attachmentsSupported: false,
            busy: !!this.active, stopToken: this.active?.record.id || '', directShell: false,
            requestTiming: this.clock.snapshot(), authRequired: this.authRequired,
            needsTerminal: this.authRequired || lastStatus === 'terminal',
            agentError: !this.active && (this.storageFailed || ['failed', 'protocol', 'disconnected'].includes(lastStatus)),
            knownMessages: turns.map(turn => turn.id),
            rows: rows.map(row => ({...row, body: row.body.slice(0, limit), bodyLength: row.body.length,
                more: row.body.length > limit})),
            older: start ? this.session + ':' + turns[start].seq : '',
        };
    }

    command(q) {
        if (!q || typeof q !== 'object') throw Error('Invalid request');
        if (['chat-send', 'chat-interrupt', 'chat-detail'].includes(q.op) && q.session !== this.session) {
            return {ok: false, error: 'STALE_SESSION'};
        }
        switch (q.op) {
            case 'chat-send': return this.send(q);
            case 'chat-snapshot': return this.snapshot(q.cursor);
            case 'status': {
                const {rows, older, knownMessages, ...status} = this.snapshot();
                return status;
            }
            case 'terminal-open': return this.terminal(true);
            case 'terminal-close': return this.terminal(false);
            case 'permission': return {permission: null};
            case 'chat-detail': {
                if (!delivery.validId(q.message)) throw Error('Invalid detail');
                const turn = this.state.turns.find(item => item.id === q.message);
                const row = turn && this.turnRows(turn).find(item => item.part === q.part);
                if (!row) throw Error('Unknown detail');
                const offset = Number.isSafeInteger(q.offset) && q.offset >= 0 ? q.offset : 0;
                return {body: row.body.slice(offset, offset + 24000),
                    next: offset + 24000 < row.body.length ? offset + 24000 : null};
            }
            case 'chat-interrupt': {
                const turn = this.active;
                if (!turn || q.stopToken !== turn.record.id || turn.worker.stopping) {
                    return {ok: false, error: 'STALE_REQUEST'};
                }
                if (!this.signalStop(turn.worker, 'SIGINT')) return {ok: false, error: 'STOP_UNCONFIRMED'};
                turn.worker.cancelled = true;
                return {interrupted: true};
            }
            case 'stop': return this.stop() ? {stopped: true} : {ok: false, error: 'STOP_UNCONFIRMED'};
            default: return {ok: false, error: 'UNSUPPORTED_OPERATION'};
        }
    }
}

function readRequest(stream, limit) {
    return new Promise((resolve, reject) => {
        const chunks = [];
        let length = 0;
        let settled = false;
        const finish = () => {
            if (settled) return;
            settled = true;
            try { resolve(JSON.parse(Buffer.concat(chunks, length).toString('utf8'))); }
            catch (error) { reject(error); }
        };
        stream.on('data', chunk => {
            if (settled) return;
            const split = chunk.indexOf(10);
            const frame = split < 0 ? chunk : chunk.subarray(0, split);
            length += frame.length;
            if (length > limit) return stream.destroy(new Error('Oversized request'));
            chunks.push(frame);
            if (split >= 0) finish();
        });
        stream.once('error', reject);
        stream.once('end', finish);
    });
}

function listen(server, address) {
    return new Promise((resolve, reject) => {
        server.once('error', reject);
        server.listen(address, resolve);
    });
}

async function serve(root, sock, session, executable, model = '', options = {}) {
    if (![root, sock, executable].every(p => typeof p === 'string' && path.isAbsolute(p)) ||
        !validSession(session) || !(model === '' || /^[A-Za-z0-9][A-Za-z0-9_.-]{0,127}$/.test(model))) throw Error('Invalid configuration');
    fs.accessSync(executable, fs.constants.X_OK);
    process.umask(0o077);
    fs.mkdirSync(root, {recursive: true, mode: 0o700});
    const stateRoot = fs.realpathSync(root);
    const file = path.join(stateRoot, session + '.json');
    // A private per-run socket serves the UI. This separate kernel-held lease
    // prevents two adapters for the same conversation alias from writing it.
    const lease = net.createServer(connection => connection.destroy());
    const leaseName = '\0jobs-agy-' + createHash('sha256').update(file).digest('hex');
    await listen(lease, leaseName);
    try {
        const chat = new AntigravityChat(file, session, executable, model, options);
        const parent = process.ppid;
        const parentStart = processIdentity(parent)?.start;
        let server, watch;
        let closeStarted = false;
        const shutdown = () => {
            if (closeStarted) return;
            if (!chat.stop()) return;
            closeStarted = true;
            clearInterval(watch);
            server.close();
        };
        server = net.createServer({allowHalfOpen: true}, connection => {
            connection.setTimeout(18000, () => connection.destroy());
            connection.on('error', () => {});
            readRequest(connection, 65536).then(async q => {
                const result = await chat.command(q);
                connection.end(JSON.stringify({ok: true, ...result}) + '\n');
                if (result.stopped) shutdown();
            }).catch(() => connection.end(JSON.stringify({ok: false, error: 'AGENT_REQUEST_FAILED'}) + '\n'));
        });
        await listen(server, sock);
        const finished = new Promise(resolve => server.once('close', resolve));
        const interrupt = () => { if (!chat.worker?.interactive) shutdown(); };
        process.on('SIGINT', interrupt);
        process.on('SIGTERM', shutdown);
        process.on('SIGHUP', shutdown);
        watch = setInterval(() => {
            if (process.ppid !== parent || (parentStart && processIdentity(parent)?.start !== parentStart)) shutdown();
        }, 1000);
        watch.unref();
        chat.startStream();
        try {
            await finished;
            await chat.worker?.closed;
        } finally {
            clearInterval(watch);
            process.off('SIGINT', interrupt);
            process.off('SIGTERM', shutdown);
            process.off('SIGHUP', shutdown);
        }
    } finally {
        lease.close();
    }
}

async function main() {
    const [kind, ...args] = process.argv.slice(2);
    if (kind === 'run-chat') return serve(...args);
    if (kind === 'proxy') {
        const request = await readRequest(process.stdin, 65536);
        const connection = net.connect(args[0], () => connection.end(JSON.stringify(request)));
        connection.setTimeout(18000, () => connection.destroy(Error('Timeout')));
        const response = await readRequest(connection, FRAME_LIMIT);
        process.stdout.write(JSON.stringify(response));
        return;
    }
    throw Error('Invalid mode');
}

if (require.main === module) {
    main().catch(() => {
        if (process.argv[2] === 'proxy') {
            process.stdout.write(JSON.stringify({ok: false, error: 'AGENT_UNAVAILABLE'}));
        } else {
            process.stderr.write('Antigravity chat could not start.\n');
        }
        process.exitCode = 1;
    });
}

module.exports = {AntigravityChat, serve};
