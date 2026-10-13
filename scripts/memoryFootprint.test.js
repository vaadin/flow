// Run with: node --test scripts/memoryFootprint.test.js
const test = require('node:test');
const assert = require('node:assert');
const { compare, comment, classDifferences } = require('./memoryFootprint');

function measurement(retained, serialized, classes = {}) {
  return { retainedBytesPerUI: retained, serializedSessionBytes: serialized, classes };
}

const BASE = {
  emptyUI: measurement(9880, 22153, { 'java.util.HashMap': { count: 15, bytes: 720 } }),
  rowsView: measurement(1010240, 519703, {
    'java.util.HashMap': { count: 1015, bytes: 48720 },
    'java.util.HashMap$Node': { count: 2008, bytes: 64256 }
  })
};

test('reports the measurements and classes that changed', () => {
  const head = {
    emptyUI: BASE.emptyUI,
    rowsView: measurement(927048, 519703, {
      'java.util.HashMap': { count: 15, bytes: 720 },
      '[Ljava.lang.String;': { count: 1000, bytes: 32000 }
    })
  };

  assert.strictEqual(compare(BASE, head).changed, true);
  const body = comment(BASE, head, 'abc1234');
  assert.match(body, /^<!-- flow-memory-footprint -->/);
  assert.match(body, /Compared with the base commit abc1234/);
  assert.match(body, /\| `rowsView` \| 1,010,240 B → 927,048 B \(\*\*−83,192 B\*\*, −8\.2%\) \| 519,703 B \(no change\) \|/);
  assert.match(body, /\| `emptyUI` \| 9,880 B \(no change\) \| 22,153 B \(no change\) \|/);
  assert.match(body, /by class: <code>rowsView<\/code>/);
  assert.doesNotMatch(body, /by class: <code>emptyUI<\/code>/);
  assert.deepStrictEqual(classDifferences(BASE.rowsView.classes, head.rowsView.classes), [
    { name: 'java.util.HashMap$Node', count: -2008, bytes: -64256 },
    { name: 'java.util.HashMap', count: -1000, bytes: -48000 },
    { name: '[Ljava.lang.String;', count: 1000, bytes: 32000 }
  ]);
});

test('reports no change when every measurement is the same', () => {
  assert.strictEqual(compare(BASE, BASE).changed, false);
  const body = comment(BASE, BASE, 'abc1234');
  assert.match(body, /No change compared with the base commit abc1234/);
  assert.doesNotMatch(body, /<details>/);
});

test('reports the head measurements when the base has none', () => {
  assert.strictEqual(compare({}, BASE).changed, true);
  const body = comment({}, BASE, 'abc1234');
  assert.match(body, /The base commit abc1234 has no memory measurements/);
  assert.match(body, /\| `rowsView` \| 1,010,240 B \| 519,703 B \|/);
});
