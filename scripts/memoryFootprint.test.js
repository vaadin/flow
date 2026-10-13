// Run with: node --test scripts/memoryFootprint.test.js
const test = require('node:test');
const assert = require('node:assert');
const { compare, comment, classDifferences } = require('./memoryFootprint');

function report(title, heap, serialized, breakdown = {}) {
  return {
    title,
    description: `${title} description`,
    metrics: [
      { id: 'heap', title: 'Heap retained per UI', unit: 'bytes', value: heap, breakdown },
      { id: 'serialized', title: 'Serialized session', unit: 'bytes', value: serialized }
    ]
  };
}

const BASE = {
  'empty-ui': report('Empty UI', 9880, 22153, { 'java.util.HashMap': { count: 15, bytes: 720 } }),
  'rows-ui': report('Rows UI', 1010240, 519703, {
    'java.util.HashMap': { count: 1015, bytes: 48720 },
    'java.util.HashMap$Node': { count: 2008, bytes: 64256 }
  })
};

test('reports the metrics and classes that changed', () => {
  const head = {
    'empty-ui': BASE['empty-ui'],
    'rows-ui': report('Rows UI', 927048, 519703, {
      'java.util.HashMap': { count: 15, bytes: 720 },
      '[Ljava.lang.String;': { count: 1000, bytes: 32000 }
    })
  };

  assert.strictEqual(compare(BASE, head).changed, true);
  const body = comment(BASE, head, 'abc1234');
  assert.match(body, /^<!-- flow-memory-footprint -->/);
  assert.match(body, /Compared with the base commit abc1234/);
  assert.match(body, /\| Rows UI \| Heap retained per UI \| 1,010,240 B → 927,048 B \(\*\*−83,192 B\*\*, −8\.2%\) \|/);
  assert.match(body, /\| Rows UI \| Serialized session \| 519,703 B \(no change\) \|/);
  assert.match(body, /\| Empty UI \| Heap retained per UI \| 9,880 B \(no change\) \|/);
  assert.match(body, /<summary>Rows UI: Heap retained per UI by class<\/summary>/);
  assert.doesNotMatch(body, /<summary>Empty UI:/);
  assert.match(body, /- \*\*Rows UI\*\* \(`rows-ui`\): Rows UI description/);
  assert.deepStrictEqual(classDifferences(BASE['rows-ui'].metrics[0].breakdown, head['rows-ui'].metrics[0].breakdown), [
    { name: 'java.util.HashMap$Node', count: -2008, bytes: -64256 },
    { name: 'java.util.HashMap', count: -1000, bytes: -48000 },
    { name: '[Ljava.lang.String;', count: 1000, bytes: 32000 }
  ]);
});

test('reports no change when every metric is the same', () => {
  assert.strictEqual(compare(BASE, BASE).changed, false);
  const body = comment(BASE, BASE, 'abc1234');
  assert.match(body, /No change compared with the base commit abc1234/);
  assert.doesNotMatch(body, /by class/);
});

test('reports the head metrics when the base has none', () => {
  assert.strictEqual(compare({}, BASE).changed, true);
  const body = comment({}, BASE, 'abc1234');
  assert.match(body, /The base commit abc1234 has no memory measurements/);
  assert.match(body, /\| Rows UI \| Heap retained per UI \| 1,010,240 B \|/);
});

test('reports scenarios and metrics that only the base or the head has', () => {
  const rows = report('Rows UI', 1010240, 519703, BASE['rows-ui'].metrics[0].breakdown);
  rows.metrics[1] = { id: 'requests', title: 'Requests', value: 3 };
  const head = { 'form-ui': report('Form UI', 50000, 30000), 'rows-ui': rows };

  assert.strictEqual(compare(BASE, head).changed, true);
  const body = comment(BASE, head, 'abc1234');
  assert.match(body, /\| Form UI \| Heap retained per UI \| 50,000 B \|/);
  assert.match(body, /\| Rows UI \| Requests \| 3 \|/);
  assert.match(body, /\| Rows UI \| Serialized session \| 519,703 B → removed \|/);
  assert.match(body, /\| Empty UI \| Heap retained per UI \| 9,880 B → removed \|/);
  assert.doesNotMatch(body, /by class/);
});

test('lists at most 15 classes per breakdown', () => {
  const classes = {};
  for (let i = 0; i < 17; i++) {
    classes[`Class${String(i).padStart(2, '0')}`] = { count: 1, bytes: 100 - i };
  }
  const head = {
    ...BASE,
    'rows-ui': report('Rows UI', 1010240, 519703, { ...BASE['rows-ui'].metrics[0].breakdown, ...classes })
  };

  const body = comment(BASE, head, 'abc1234');
  assert.match(body, /`Class14`/);
  assert.doesNotMatch(body, /`Class15`/);
  assert.match(body, /\| …and 2 more \| \| \|/);
});
