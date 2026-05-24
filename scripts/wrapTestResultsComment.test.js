#!/usr/bin/env node
const test = require('node:test');
const assert = require('node:assert/strict');

const {
  formatDuration,
  buildSummary,
  wrapBody,
  isEnricoMiComment,
  TIME_WARN_THRESHOLD_PCT,
} = require('./wrapTestResultsComment.js');

function swd({ failures = 0, errors = 0, testDelta = 0, current = 4820, delta = 26 } = {}) {
  return {
    tests: { number: 10000, delta: testDelta },
    tests_fail: { number: failures, delta: 0 },
    tests_error: { number: errors, delta: 0 },
    duration: { number: current, delta },
  };
}

test('formatDuration omits zero components but keeps seconds when nothing else', () => {
  assert.equal(formatDuration(4820), '1h 20m 20s');
  assert.equal(formatDuration(3600), '1h');
  assert.equal(formatDuration(75), '1m 15s');
  assert.equal(formatDuration(0), '0s');
});

test('all-pass summary uses :white_check_mark: across the board', () => {
  const lines = buildSummary(swd({ current: 4794, delta: -100 }));
  assert.deepEqual(lines, [
    ':white_check_mark: All tests pass',
    `:white_check_mark: ${Math.round(4794 / 4894 * 100)}% of reference time spent (1h 19m 54s)`,
    ':white_check_mark: No tests added or removed',
  ]);
});

test('failures flip the first line to :x:', () => {
  const [first] = buildSummary(swd({ failures: 4, errors: 1 }));
  assert.equal(first, ':x: 5 tests failed');
});

test('1 failure uses singular "test"', () => {
  const [first] = buildSummary(swd({ failures: 1 }));
  assert.equal(first, ':x: 1 test failed');
});

test('added/removed lines reflect the test count delta sign', () => {
  assert.equal(buildSummary(swd({ testDelta: 5 }))[2], ':white_check_mark: 5 tests added');
  assert.equal(buildSummary(swd({ testDelta: -2 }))[2], ':warning: 2 tests removed');
});

test(`time line warns above ${TIME_WARN_THRESHOLD_PCT}%`, () => {
  // 124% of 4000 = 4960, delta = 960
  const warn = buildSummary(swd({ current: 4960, delta: 960 }))[1];
  assert.match(warn, /^:warning: 124% of reference time spent/);

  // 109% of 4000 = 4360, delta = 360 — under threshold
  const ok = buildSummary(swd({ current: 4360, delta: 360 }))[1];
  assert.match(ok, /^:white_check_mark: 109% of reference time spent/);
});

test('wrapBody keeps "## Test Results\\n" at the start and "Results for commit" inside', () => {
  const original = '## Test Results\n\n| stat | value |\n| --- | --- |\n\nResults for commit abc1234.';
  const wrapped = wrapBody(original, [
    ':white_check_mark: All tests pass',
    ':white_check_mark: 98% of reference time spent (1h 19m 54s)',
    ':white_check_mark: No tests added or removed',
  ]);
  assert.ok(wrapped.startsWith('## Test Results\n'),
    'wrapped body must start with the EnricoMi heading');
  assert.match(wrapped, /\nResults for commit /,
    'wrapped body must still contain the commit indicator');
  assert.match(wrapped, /<details>\n<summary>Full report<\/summary>/);
  assert.match(wrapped, /:white_check_mark: All tests pass/);
});

test('isEnricoMiComment matches the heading + commit indicator', () => {
  assert.ok(isEnricoMiComment('## Test Results\n\nfoo\nResults for commit abc.'));
  assert.ok(isEnricoMiComment('## Test Results\n\nfoo\nresults for commit abc.'));
  assert.ok(!isEnricoMiComment('## Something Else\nResults for commit abc.'));
  assert.ok(!isEnricoMiComment('## Test Results\n\nno indicator here'));
});
