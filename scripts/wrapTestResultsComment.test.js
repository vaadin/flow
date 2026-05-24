#!/usr/bin/env node
const test = require('node:test');
const assert = require('node:assert/strict');

const {
  parseStats,
  parseDurationLine,
  parseTestsLine,
  parseDelta,
  parseDurationSeconds,
  formatDuration,
  buildSummary,
  wrapBody,
  isEnricoMiComment,
  TIME_WARN_THRESHOLD_PCT,
} = require('./wrapTestResultsComment.js');

// EnricoMi uses U+2007 (figure space) as padding inside numbers and U+2008
// (punctuation space) as the thousands grouper. Between fields it emits a
// run of en/em spaces (U+2002 U+2003). Reproduce that mix here so the parser
// is exercised against the same character soup it sees in production.
const FS = ' ';
const PS = ' ';
const SEP = '  ';

function tableBody({ duration, durDelta, tests, testsDelta, fails = '0', failsDelta = '±0' }) {
  return [
    '## Test Results',
    `${FS}1${PS}420 files ±0${SEP}${FS}1${PS}420 suites ±0${SEP}${duration} ⏱️ ${durDelta}`,
    `${tests} tests ${testsDelta}${SEP}${FS}9${PS}931 ✅ ±0${SEP}68 💤 ±0${SEP}${fails} ❌ ${failsDelta}`,
    `10${PS}471 runs ±0${SEP}10${PS}402 ✅ ±0${SEP}69 💤 ±0${SEP}0 ❌ ±0`,
    '',
    'Results for commit abc1234. ± Comparison against base commit def5678.',
  ].join('\n');
}

test('formatDuration drops zero components but always emits seconds when nothing else fits', () => {
  assert.equal(formatDuration(4820), '1h 20m 20s');
  assert.equal(formatDuration(3600), '1h');
  assert.equal(formatDuration(75), '1m 15s');
  assert.equal(formatDuration(0), '0s');
});

test('parseDurationSeconds handles d/h/m/s combinations', () => {
  assert.equal(parseDurationSeconds('1h 20m 20s'), 4820);
  assert.equal(parseDurationSeconds('45s'), 45);
  assert.equal(parseDurationSeconds('4m 52s'), 292);
  assert.equal(parseDurationSeconds('1d 2h'), 93600);
});

test('parseDelta recognises ±, +, and the space-padded - form', () => {
  assert.equal(parseDelta('±0'), 0);
  assert.equal(parseDelta('+18'), 18);
  assert.equal(parseDelta(' - 5'), -5);
});

test('parseDurationLine extracts current + delta for negative deltas', () => {
  const body = tableBody({
    duration: '1h 18m 52s', durDelta: '- 4m 52s', tests: `${FS}9${PS}999`, testsDelta: '±0',
  });
  assert.deepEqual(parseDurationLine(body), { current: 4732, delta: -292 });
});

test('parseDurationLine extracts current + delta for positive deltas', () => {
  const body = tableBody({
    duration: '1h 20m 20s', durDelta: '+26s', tests: `10${PS}000`, testsDelta: '+18',
  });
  assert.deepEqual(parseDurationLine(body), { current: 4820, delta: 26 });
});

test('parseDurationLine extracts zero deltas', () => {
  const body = tableBody({
    duration: '1h 18m 52s', durDelta: '±0s', tests: `${FS}9${PS}999`, testsDelta: '±0',
  });
  assert.deepEqual(parseDurationLine(body), { current: 4732, delta: 0 });
});

test('parseTestsLine picks up positive tests delta and zero failures', () => {
  const body = tableBody({
    duration: '1h', durDelta: '+0s', tests: `10${PS}000`, testsDelta: '+18',
  });
  assert.deepEqual(parseTestsLine(body), { failures: 0, testsDelta: 18 });
});

test('parseTestsLine picks up negative tests delta and non-zero failures', () => {
  const body = tableBody({
    duration: '1h', durDelta: '+0s', tests: `${FS}9${PS}995`, testsDelta: ' - 5', fails: '5', failsDelta: '+5',
  });
  assert.deepEqual(parseTestsLine(body), { failures: 5, testsDelta: -5 });
});

test('parseStats walks the full body end to end', () => {
  const body = tableBody({
    duration: '1h 18m 52s', durDelta: '- 4m 52s', tests: `${FS}9${PS}999`, testsDelta: '±0',
  });
  assert.deepEqual(parseStats(body), {
    duration: 4732,
    durationDelta: -292,
    failures: 0,
    testsDelta: 0,
  });
});

test('all-pass summary uses :white_check_mark: across the board', () => {
  const lines = buildSummary({ duration: 4794, durationDelta: -100, failures: 0, testsDelta: 0 });
  assert.deepEqual(lines, [
    ':white_check_mark: All tests pass',
    `:white_check_mark: ${Math.round(4794 / 4894 * 100)}% of reference time spent (1h 19m 54s)`,
    ':white_check_mark: No tests added or removed',
  ]);
});

test('failures flip the first line to :x: with correct pluralisation', () => {
  const [first] = buildSummary({ duration: 1, durationDelta: 0, failures: 5, testsDelta: 0 });
  assert.equal(first, ':x: 5 tests failed');
  const [firstSingle] = buildSummary({ duration: 1, durationDelta: 0, failures: 1, testsDelta: 0 });
  assert.equal(firstSingle, ':x: 1 test failed');
});

test('test count delta drives the third summary line', () => {
  assert.equal(
    buildSummary({ duration: 1, durationDelta: 0, failures: 0, testsDelta: 5 })[2],
    ':white_check_mark: 5 tests added',
  );
  assert.equal(
    buildSummary({ duration: 1, durationDelta: 0, failures: 0, testsDelta: -2 })[2],
    ':warning: 2 tests removed',
  );
});

test(`time line warns above ${TIME_WARN_THRESHOLD_PCT}%`, () => {
  const warn = buildSummary({ duration: 4960, durationDelta: 960, failures: 0, testsDelta: 0 })[1];
  assert.match(warn, /^:warning: 124% of reference time spent/);
  const ok = buildSummary({ duration: 4360, durationDelta: 360, failures: 0, testsDelta: 0 })[1];
  assert.match(ok, /^:white_check_mark: 109% of reference time spent/);
});

test('wrapBody preserves the heading marker and the commit indicator', () => {
  const original = '## Test Results\n\n| stat | value |\n| --- | --- |\n\nResults for commit abc1234.';
  const wrapped = wrapBody(original, [
    ':white_check_mark: All tests pass',
    ':white_check_mark: 98% of reference time spent (1h 19m 54s)',
    ':white_check_mark: No tests added or removed',
  ]);
  assert.ok(wrapped.startsWith('## Test Results\n'));
  assert.match(wrapped, /\nResults for commit /);
  assert.match(wrapped, /<details>\n<summary>Full report<\/summary>/);
});

test('isEnricoMiComment matches the heading + commit indicator', () => {
  assert.ok(isEnricoMiComment('## Test Results\n\nfoo\nResults for commit abc.'));
  assert.ok(isEnricoMiComment('## Test Results\n\nfoo\nresults for commit abc.'));
  assert.ok(!isEnricoMiComment('## Something Else\nResults for commit abc.'));
  assert.ok(!isEnricoMiComment('## Test Results\n\nno indicator here'));
});

