'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const MAX_BYTES = 8 * 1024 * 1024;
const MODEL = /^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$/;
const TIER = /^[A-Za-z][A-Za-z0-9_-]{0,127}$/;

function readModel(model, home) {
    if (typeof model !== 'string' || !MODEL.test(model)) return null;
    let fd;
    try {
        const cache = path.join(home || process.env.CODEX_HOME || path.join(os.homedir(), '.codex'), 'models_cache.json');
        const record = fs.lstatSync(cache);
        if (!record.isFile()) return null;
        fd = fs.openSync(cache, fs.constants.O_RDONLY | fs.constants.O_NOFOLLOW | fs.constants.O_NONBLOCK);
        const stat = fs.fstatSync(fd);
        if (!stat.isFile() || stat.dev !== record.dev || stat.ino !== record.ino ||
            stat.size === 0 || stat.size > MAX_BYTES) return null;
        const bytes = Buffer.alloc(stat.size + 1);
        let offset = 0;
        while (offset < bytes.length) {
            const count = fs.readSync(fd, bytes, offset, bytes.length - offset, null);
            if (count === 0) break;
            offset += count;
        }
        if (offset !== stat.size) return null;
        const models = JSON.parse(bytes.toString('utf8', 0, offset))?.models;
        return Array.isArray(models) ? models.find(item => item?.slug === model) || null : null;
    } catch {
        return null;
    } finally {
        if (fd !== undefined) fs.closeSync(fd);
    }
}

function fastTier(model, home) {
    const tiers = readModel(model, home)?.service_tiers;
    if (!Array.isArray(tiers)) return '';
    const fast = tiers.find(tier => typeof tier?.name === 'string' && tier.name.toLowerCase() === 'fast' &&
        typeof tier.id === 'string' && TIER.test(tier.id) && tier.id !== 'default');
    return fast?.id || '';
}

function speedOptions(model, home) {
    return {model, fastAvailable: fastTier(model, home) !== '', supported: true};
}

function serviceTierForTurn(model, speed, home) {
    if (speed === undefined) return undefined;
    if (speed === 'normal') return 'default';
    if (speed !== 'fast') throw Error('INVALID_SPEED');
    const tier = fastTier(model, home);
    if (!tier) throw Error('FAST_UNAVAILABLE');
    return tier;
}

function count(value) {
    return Number.isSafeInteger(value) && value > 0 ? value : 0;
}

function contextOptions(model, home) {
    const info = readModel(model, home);
    const defaultWindow = count(info?.context_window);
    const explicitMaximum = info?.max_context_window != null;
    const limit = explicitMaximum ? count(info.max_context_window) : defaultWindow;
    const maximumWindow = defaultWindow <= limit ? limit : 0;
    return {model, defaultWindow, maximumWindow, explicitMaximum};
}

module.exports = {contextOptions, speedOptions, serviceTierForTurn};
