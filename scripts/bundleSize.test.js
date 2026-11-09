// Run with: node --test scripts/bundleSize.test.js
const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { measure, compare, comment, inputDifferences, readReport } = require('./bundleSize');

const BASE = {
  chunks: [
    { name: 'VAADIN/build/frontend.js', minified: 30000, brotli: 10000 },
    { name: 'VAADIN/build/FlowClient.js', minified: 4000, brotli: 1400 },
    { name: 'sw.js', minified: 8000, brotli: 3700 }
  ],
  inputs: { 'Flow.ts': 20000, 'Shortcut.ts': 1000, 'FlowClient.js': 4000, 'internal/Timer.ts': 8000 }
};

test('reports the files and modules that changed', () => {
  const head = {
    chunks: [
      { name: 'VAADIN/build/frontend.js', minified: 30500, brotli: 10100 },
      { name: 'VAADIN/build/FlowClient.js', minified: 4000, brotli: 1400 },
      { name: 'sw.js', minified: 7900, brotli: 3650 }
    ],
    inputs: { 'Flow.ts': 20000, 'Shortcut.ts': 1500, 'FlowClient.js': 4000, 'internal/Timer.ts': 7900 }
  };

  assert.strictEqual(compare(BASE, head).changed, true);
  const body = comment(BASE, head, 'abc1234');
  assert.match(body, /^<!-- flow-bundle-size -->/);
  assert.match(body, /Compared with the base commit abc1234/);
  assert.ok(
    body.includes(
      '| `VAADIN/build/frontend.js` | 30,500 B | $\\color{red}\\textbf{+500 B}$ (+1.7%) | 10,100 B | $\\color{red}\\textbf{+100 B}$ (+1.0%) |'
    )
  );
  assert.ok(body.includes('| `VAADIN/build/FlowClient.js` | 4,000 B | no change | 1,400 B | no change |'));
  assert.ok(
    body.includes(
      '| **Total** | 42,400 B | $\\color{red}\\textbf{+400 B}$ (+1.0%) | 15,150 B | $\\color{red}\\textbf{+50 B}$ (+0.3%) |'
    )
  );
  assert.match(body, /\| `Shortcut.ts` \| \+500 B \|\n\| `internal\/Timer.ts` \| −100 B \|/);
  assert.doesNotMatch(body, /\| `Flow.ts` \| [+−]/);
});

test('reports no change without a table when no minified size differs', () => {
  // The compressed size alone does not count, as it follows the minified one
  const same = {
    chunks: BASE.chunks.map((chunk) => ({ ...chunk, brotli: chunk.brotli + 1 })),
    inputs: { ...BASE.inputs }
  };

  assert.strictEqual(compare(BASE, same).changed, false);
  const body = comment(BASE, same, 'abc1234');
  assert.match(body, /No change compared with the base commit abc1234/);
  assert.doesNotMatch(body, /\| Chunk \|/);
});

test('reports files that only one side has', () => {
  const head = {
    chunks: [
      { name: 'VAADIN/build/frontend.js', minified: 30000, brotli: 10000 },
      { name: 'VAADIN/build/Lazy.js', minified: 500, brotli: 300 }
    ],
    inputs: { 'Flow.ts': 30000, 'Lazy.ts': 500 }
  };

  const body = comment(BASE, head, 'abc1234');
  assert.match(body, /\| `VAADIN\/build\/Lazy.js` \| 500 B \| new \| 300 B \| new \|/);
  assert.match(body, /\| `VAADIN\/build\/FlowClient.js` \| — \| removed \| — \| removed \|/);
  assert.deepStrictEqual(inputDifferences(BASE.inputs, head.inputs).slice(0, 2), [
    { name: 'Flow.ts', bytes: 10000 },
    { name: 'internal/Timer.ts', bytes: -8000 }
  ]);
});

test('lists the largest module differences only', () => {
  const inputs = (bytes) => Object.fromEntries(Array.from({ length: 20 }, (_, i) => [`File${i}.ts`, bytes(i)]));
  const base = {
    chunks: [{ name: 'VAADIN/build/frontend.js', minified: 1000, brotli: 500 }],
    inputs: inputs(() => 100)
  };
  const head = {
    chunks: [{ name: 'VAADIN/build/frontend.js', minified: 1210, brotli: 550 }],
    inputs: inputs((i) => 100 + i + 1)
  };

  const body = comment(base, head, 'abc1234');
  assert.match(body, /\| `File19.ts` \| \+20 B \|/);
  assert.match(body, /\| `File5.ts` \| \+6 B \|/);
  assert.doesNotMatch(body, /`File4.ts`/);
  assert.match(body, /\| …and 5 more \| \|/);
});

test('reports the head alone when the base has nothing to compare with', () => {
  assert.strictEqual(compare(undefined, BASE).changed, true);
  const body = comment(undefined, BASE, 'abc1234');
  assert.match(body, /The base commit abc1234 has no bundle to compare with/);
  assert.match(body, /\| `VAADIN\/build\/frontend.js` \| 30,000 B \| — \| 10,000 B \| — \|/);
  assert.doesNotMatch(body, /by source file/);
});

function writeBundle(dir, flowHash, frontendHash, revision) {
  const write = (name, content) => {
    fs.mkdirSync(path.dirname(path.join(dir, name)), { recursive: true });
    fs.writeFileSync(path.join(dir, name), content);
  };
  write(`webapp/VAADIN/build/FlowClient-${flowHash}.js`, 'export const flow = 1;');
  write(`webapp/VAADIN/build/frontend-${frontendHash}.js`, `import('./FlowClient-${flowHash}.js');`);
  write(`webapp/VAADIN/build/frontend-${frontendHash}.css`, 'html{}');
  write('webapp/sw.js', `[{url:\`VAADIN/build/FlowClient-${flowHash}.js\`,revision:\`${revision}\`}]`);
  write('webapp/index.html', '<html></html>');
  const data = {
    nodeParts: { p1: { renderedLength: 30 }, p2: { renderedLength: 12 }, p3: { renderedLength: 5 } },
    nodeMetas: {
      m1: { id: '/src/main/frontend/generated/jar-resources/FlowClient.js', moduleParts: { a: 'p1' } },
      m2: { id: '/node_modules/lit/index.js', moduleParts: { a: 'p2', b: 'p3' } }
    }
  };
  write('config/bundle-size.html', `<script>\nconst data = ${JSON.stringify(data)};\n</script>`);
}

test('measures the files of the bundle, without their content hashes', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'bundle-size-'));
  try {
    writeBundle(path.join(root, 'a'), 'CLdr9-o5', '0t8eFnqu', 'a'.repeat(32));
    writeBundle(path.join(root, 'b'), 'Zz_9-AbC', '1u9fGovv', 'b'.repeat(32));

    const report = measure(path.join(root, 'a'));
    assert.deepStrictEqual(
      report.chunks.map((chunk) => chunk.name),
      ['sw.js', 'VAADIN/build/FlowClient.js', 'VAADIN/build/frontend.css', 'VAADIN/build/frontend.js']
    );
    assert.ok(report.chunks.every((chunk) => chunk.minified > 0 && chunk.brotli > 0));
    assert.deepStrictEqual(report.inputs, {
      'src/main/frontend/generated/jar-resources/FlowClient.js': 30,
      'node_modules/lit/index.js': 17
    });
    // Other hashes and revisions are no change
    assert.deepStrictEqual(measure(path.join(root, 'b')), report);

    const file = path.join(root, 'report.json');
    fs.writeFileSync(file, JSON.stringify(report));
    assert.deepStrictEqual(readReport(file), report);
    assert.strictEqual(readReport(path.join(root, 'missing.json')), undefined);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('rejects a report without chunks', () => {
  const file = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'bundle-size-')), 'report.json');
  fs.writeFileSync(file, JSON.stringify({ chunks: [], inputs: {} }));
  assert.throws(() => readReport(file), /needs chunks/);
});
