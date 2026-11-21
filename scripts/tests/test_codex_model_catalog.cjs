'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {test} = require('node:test');
const {fixture} = require('./fixture-paths.cjs');
const {contextOptions, speedOptions, serviceTierForTurn} = require('../../app/src/main/assets/codex-model-catalog.cjs');

function catalog(models) {
    const home = fixture('cxcat');
    fs.writeFileSync(path.join(home, 'models_cache.json'), JSON.stringify({models}));
    return home;
}

test('Fast uses its exact model-specific ID and Normal resets the turn to default', () => {
    const home = catalog([
        {slug: 'first', service_tiers: [{name: 'Flex', id: 'flex'}, {name: 'Fast', id: 'test-rush'}]},
        {slug: 'second', service_tiers: [{name: 'Fast', id: 'default'}, {name: 'Fast', id: '../private'}], additional_speed_tiers: ['fast']}
    ]);
    assert.deepEqual(speedOptions('first', home), {model: 'first', fastAvailable: true, supported: true});
    assert.equal(serviceTierForTurn('first', 'fast', home), 'test-rush');
    assert.equal(serviceTierForTurn('second', 'normal', home), 'default');
    assert.equal(serviceTierForTurn('first', undefined, home), undefined);
    assert.equal(speedOptions('second', home).fastAvailable, false);
    assert.throws(() => serviceTierForTurn('second', 'fast', home), {message: 'FAST_UNAVAILABLE'});
    assert.throws(() => serviceTierForTurn('first', 'test-rush', home), {message: 'INVALID_SPEED'});
});

test('context default, explicit maximum and conservative fallback stay separate', () => {
    const home = catalog([
        {slug: 'large', context_window: 272000, max_context_window: 872000},
        {slug: 'fallback', context_window: 256000, max_context_window: null},
        {slug: 'inconsistent', context_window: 256000, max_context_window: 16},
        {slug: 'malformed', context_window: 100, max_context_window: '1000'},
        {slug: 'fractional', context_window: 100.3}
    ]);
    assert.deepEqual(contextOptions('large', home), {model: 'large', defaultWindow: 272000, maximumWindow: 872000, explicitMaximum: true});
    assert.deepEqual(contextOptions('fallback', home), {model: 'fallback', defaultWindow: 256000, maximumWindow: 256000, explicitMaximum: false});
    for (const slug of ['inconsistent', 'malformed', 'fractional', 'unknown']) assert.equal(contextOptions(slug, home).maximumWindow, 0, slug);
});

test('missing, malformed and oversized caches fail closed', () => {
    const home = fixture('cxcat');
    const cache = path.join(home, 'models_cache.json');
    assert.equal(speedOptions('first', home).fastAvailable, false);
    fs.writeFileSync(cache, 'not json');
    assert.equal(contextOptions('first', home).maximumWindow, 0);
    fs.truncateSync(cache, 8 * 1024 * 1024 + 1);
    assert.throws(() => serviceTierForTurn('first', 'fast', home), {message: 'FAST_UNAVAILABLE'});
    fs.truncateSync(cache, 0);
});
