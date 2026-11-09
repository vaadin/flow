// Run with: node --test scripts/bundleSize.test.js
const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { measure, compare, comment, inputDifferences, readReport } = require('./bundleSize');

const BASE = {
  chunks: [
    { name: 'Flow.ts', minified: 30000, gzip: 10000 },
    { name: 'FlowClient.js', minified: 4000, gzip: 1400 },
    { name: 'Shared chunks', minified: 8000, gzip: 3700 }
  ],
  inputs: { 'Flow.ts': 20000, 'Shortcut.ts': 1000, 'FlowClient.js': 4000, 'internal/Timer.ts': 8000 }
};

test('reports the chunks and source files that changed', () => {
  const head = {
    chunks: [
      { name: 'Flow.ts', minified: 30500, gzip: 10100 },
      { name: 'FlowClient.js', minified: 4000, gzip: 1400 },
      { name: 'Shared chunks', minified: 7900, gzip: 3650 }
    ],
    inputs: { 'Flow.ts': 20000, 'Shortcut.ts': 1500, 'FlowClient.js': 4000, 'internal/Timer.ts': 7900 }
  };

  assert.strictEqual(compare(BASE, head).changed, true);
  const body = comment(BASE, head, 'abc1234');
  assert.match(body, /^<!-- flow-bundle-size -->/);
  assert.match(body, /Compared with the base commit abc1234/);
  assert.ok(
    body.includes(
      '| `Flow.ts` | 30,500 B | $\\color{red}\\textbf{+500 B}$ (+1.7%) | 10,100 B | $\\color{red}\\textbf{+100 B}$ (+1.0%) |'
    )
  );
  assert.ok(body.includes('| `FlowClient.js` | 4,000 B | no change | 1,400 B | no change |'));
  assert.ok(
    body.includes(
      '| **Total** | 42,400 B | $\\color{red}\\textbf{+400 B}$ (+1.0%) | 15,150 B | $\\color{red}\\textbf{+50 B}$ (+0.3%) |'
    )
  );
  assert.match(body, /\| `Shortcut.ts` \| \+500 B \|\n\| `internal\/Timer.ts` \| −100 B \|/);
  assert.doesNotMatch(body, /\| `Flow.ts` \| [+−]/);
});

test('reports no change without a table when no minified size differs', () => {
  // The gzipped size alone does not count, as it follows the minified one
  const same = { chunks: BASE.chunks.map((chunk) => ({ ...chunk, gzip: chunk.gzip + 1 })), inputs: { ...BASE.inputs } };

  assert.strictEqual(compare(BASE, same).changed, false);
  const body = comment(BASE, same, 'abc1234');
  assert.match(body, /No change compared with the base commit abc1234/);
  assert.doesNotMatch(body, /\| Chunk \|/);
});

test('reports chunks that only one side has', () => {
  const head = {
    chunks: [
      { name: 'Flow.ts', minified: 30000, gzip: 10000 },
      { name: 'Lazy.ts', minified: 500, gzip: 300 }
    ],
    inputs: { 'Flow.ts': 30000, 'Lazy.ts': 500 }
  };

  const body = comment(BASE, head, 'abc1234');
  assert.match(body, /\| `Lazy.ts` \| 500 B \| new \| 300 B \| new \|/);
  assert.match(body, /\| `FlowClient.js` \| — \| removed \| — \| removed \|/);
  assert.deepStrictEqual(inputDifferences(BASE.inputs, head.inputs).slice(0, 2), [
    { name: 'Flow.ts', bytes: 10000 },
    { name: 'internal/Timer.ts', bytes: -8000 }
  ]);
});

test('lists the largest source file differences only', () => {
  const inputs = (bytes) => Object.fromEntries(Array.from({ length: 20 }, (_, i) => [`File${i}.ts`, bytes(i)]));
  const base = { chunks: [{ name: 'Flow.ts', minified: 1000, gzip: 500 }], inputs: inputs(() => 100) };
  const head = { chunks: [{ name: 'Flow.ts', minified: 1210, gzip: 550 }], inputs: inputs((i) => 100 + i + 1) };

  const body = comment(base, head, 'abc1234');
  assert.match(body, /\| `File19.ts` \| \+20 B \|/);
  assert.match(body, /\| `File5.ts` \| \+6 B \|/);
  assert.doesNotMatch(body, /`File4.ts`/);
  assert.match(body, /\| …and 5 more \| \|/);
});

test('reports the head alone when the base has nothing to compare with', () => {
  assert.strictEqual(compare(undefined, BASE).changed, true);
  const body = comment(undefined, BASE, 'abc1234');
  assert.match(body, /The base commit abc1234 has no client to compare with/);
  assert.match(body, /\| `Flow.ts` \| 30,000 B \| — \| 10,000 B \| — \|/);
  assert.doesNotMatch(body, /by source file/);
});

test('bundles the entry points, the engine alias and dynamic imports, leaving packages out', () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'bundle-size-'));
  try {
    const write = (name, content) => fs.writeFileSync(path.join(dir, name), content);
    write(
      'Flow.ts',
      "import { html } from 'lit';\nimport { share } from './Shared';\nexport const flow = async () => [html, share(), await import('vaadin-flow-client')];\n"
    );
    write(
      'FlowClient.js',
      "import { share } from './Shared';\nexport const init = () => import('./Lazy').then((m) => m.lazy(share()));\n"
    );
    write('Lazy.ts', 'export const lazy = (value: unknown) => [value, 42];\n');
    write('Shared.ts', "export const share = () => 'shared between the entry points';\n");

    const report = measure(dir);
    assert.deepStrictEqual(
      report.chunks.map((chunk) => chunk.name),
      ['Flow.ts', 'FlowClient.js', 'Lazy.ts', 'Shared chunks']
    );
    assert.ok(report.chunks.every((chunk) => chunk.minified > 0 && chunk.gzip > 0));
    assert.deepStrictEqual(Object.keys(report.inputs).sort(), ['Flow.ts', 'FlowClient.js', 'Lazy.ts', 'Shared.ts']);

    const file = path.join(dir, 'report.json');
    fs.writeFileSync(file, JSON.stringify(report));
    assert.deepStrictEqual(readReport(file), report);
    assert.strictEqual(readReport(path.join(dir, 'missing.json')), undefined);
  } finally {
    fs.rmSync(dir, { recursive: true, force: true });
  }
});

test('rejects a report without chunks', () => {
  const file = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'bundle-size-')), 'report.json');
  fs.writeFileSync(file, JSON.stringify({ chunks: [], inputs: {} }));
  assert.throws(() => readReport(file), /needs chunks/);
});
