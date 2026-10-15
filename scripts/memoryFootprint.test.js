// Run with: node --test scripts/memoryFootprint.test.js
const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { compare, comment, classDifferences, readMeasurements } = require('./memoryFootprint');

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
    'empty-ui': report('Empty UI', 9876, 22160, BASE['empty-ui'].metrics[0].breakdown),
    'rows-ui': report('Rows UI', 927048, 519703, {
      'java.util.HashMap': { count: 15, bytes: 720 },
      '[Ljava.lang.String;': { count: 1000, bytes: 32000 }
    })
  };

  assert.strictEqual(compare(BASE, head).changed, true);
  const body = comment(BASE, head, 'abc1234');
  assert.match(body, /^<!-- flow-memory-footprint -->/);
  assert.match(body, /Compared with the base commit abc1234/);
  assert.match(body, /\| Scenario \| Metric \| Base \| Head \| Change \|/);
  assert.ok(
    body.includes('| Rows UI | Heap retained per UI | 1,010,240 B | 927,048 B | $\\color{green}\\textbf{−83,192 B}$ (−8.2%) |')
  );
  assert.match(body, /\| Rows UI \| Serialized session \| 519,703 B \| 519,703 B \| no change \|/);
  assert.match(body, /\| Empty UI \| Heap retained per UI \| 9,880 B \| 9,876 B \| −4 B \(ignored\) \|/);
  assert.ok(body.includes('| Empty UI | Serialized session | 22,153 B | 22,160 B | $\\color{red}\\textbf{+7 B}$ (+0.0%) |'));
  assert.match(body, /<summary>Rows UI: Heap retained per UI by class<\/summary>/);
  assert.doesNotMatch(body, /<summary>Empty UI:/);
  assert.match(body, /- \*\*Rows UI\*\* \(`rows-ui`\): Rows UI description/);
  assert.deepStrictEqual(classDifferences(BASE['rows-ui'].metrics[0].breakdown, head['rows-ui'].metrics[0].breakdown), [
    { name: 'java.util.HashMap$Node', count: -2008, bytes: -64256 },
    { name: 'java.util.HashMap', count: -1000, bytes: -48000 },
    { name: '[Ljava.lang.String;', count: 1000, bytes: 32000 }
  ]);
});

test('reports no change without a table when every metric differs by less than 5 bytes', () => {
  const almostSame = {
    'empty-ui': report('Empty UI', 9876, 22154, BASE['empty-ui'].metrics[0].breakdown),
    'rows-ui': report('Rows UI', 1010240, 519703, BASE['rows-ui'].metrics[0].breakdown)
  };
  for (const head of [BASE, almostSame]) {
    assert.strictEqual(compare(BASE, head).changed, false);
    const body = comment(BASE, head, 'abc1234');
    assert.match(body, /No change compared with the base commit abc1234/);
    assert.doesNotMatch(body, /\| Scenario \|/);
    assert.doesNotMatch(body, /by class/);
  }
});

test('reports the head metrics when the base has none', () => {
  assert.strictEqual(compare({}, BASE).changed, true);
  const body = comment({}, BASE, 'abc1234');
  assert.match(body, /The base commit abc1234 has no memory measurements/);
  assert.match(body, /\| Rows UI \| Heap retained per UI \| — \| 1,010,240 B \| — \|/);
});

test('reports scenarios and metrics that only the base or the head has', () => {
  const rows = report('Rows UI', 1010240, 519703, BASE['rows-ui'].metrics[0].breakdown);
  rows.metrics[1] = { id: 'requests', title: 'Requests', value: 3 };
  const head = { 'form-ui': report('Form UI', 50000, 30000), 'rows-ui': rows };

  assert.strictEqual(compare(BASE, head).changed, true);
  const body = comment(BASE, head, 'abc1234');
  assert.match(body, /\| Form UI \| Heap retained per UI \| — \| 50,000 B \| new \|/);
  assert.match(body, /\| Rows UI \| Requests \| — \| 3 \| new \|/);
  assert.match(body, /\| Rows UI \| Serialized session \| 519,703 B \| — \| removed \|/);
  assert.match(body, /\| Empty UI \| Heap retained per UI \| 9,880 B \| — \| removed \|/);
  assert.doesNotMatch(body, /by class/);
});

test('lists at most 15 classes per breakdown', () => {
  const classes = {};
  for (let i = 0; i < 17; i++) {
    classes[`Class${String(i).padStart(2, '0')}`] = { count: 1, bytes: 100 - i };
  }
  const head = {
    ...BASE,
    'rows-ui': report('Rows UI', 1011770, 519703, { ...BASE['rows-ui'].metrics[0].breakdown, ...classes })
  };

  const body = comment(BASE, head, 'abc1234');
  assert.match(body, /`Class14`/);
  assert.doesNotMatch(body, /`Class15`/);
  assert.match(body, /\| …and 2 more \| \| \|/);
});

test('rejects a report that does not have the expected shape', (t) => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'memory-footprint-'));
  t.after(() => fs.rmSync(dir, { recursive: true }));
  fs.writeFileSync(path.join(dir, 'rows-ui.json'), JSON.stringify(BASE['rows-ui']));
  assert.deepStrictEqual(readMeasurements(dir), { 'rows-ui': BASE['rows-ui'] });

  fs.writeFileSync(path.join(dir, 'rows-ui.json'), JSON.stringify({ title: 'Rows UI', results: [] }));
  assert.throws(() => readMeasurements(dir), /rows-ui\.json needs a title and metrics/);
  fs.writeFileSync(path.join(dir, 'rows-ui.json'), JSON.stringify({ title: 'Rows UI', metrics: [{ id: 'heap', title: 'Heap', value: '1' }] }));
  assert.throws(() => readMeasurements(dir), /rows-ui\.json needs a title and metrics/);
  fs.writeFileSync(path.join(dir, 'rows-ui.json'), JSON.stringify({ name: 'Rows UI', metrics: BASE['rows-ui'].metrics }));
  assert.throws(() => readMeasurements(dir), /rows-ui\.json needs a title and metrics/);
  const untitled = { ...BASE['rows-ui'], metrics: [{ id: 'heap', value: 1 }] };
  fs.writeFileSync(path.join(dir, 'rows-ui.json'), JSON.stringify(untitled));
  assert.throws(() => readMeasurements(dir), /rows-ui\.json needs a title and metrics/);
});
