#!/usr/bin/env node
/**
 * Measures the size of the Flow client as an application's production build
 * ships it, and compares the sizes measured on the base and the head of a pull
 * request in the pull request comment that reports them.
 *
 * The client ships as TypeScript sources that the application's Vite build
 * bundles. This bundles the same sources with esbuild, minified and split at
 * the dynamic imports the way a production build splits them, starting from
 * Flow.ts, which the application imports, and the modules Flow.ts loads
 * dynamically. Bare imports such as Lit are left out, as the application
 * bundles them for its own code anyway. The sizes are close to, not the same
 * as, what Vite produces; the difference between base and head is what
 * counts.
 *
 * Usage:
 *   node scripts/bundleSize.js measure <client sources dir> <report file>
 *   node scripts/bundleSize.js compare <base report> <head report> <base commit>
 *
 * The report written by measure:
 *
 *   {
 *     "chunks": [{ "name": "Flow.ts", "minified": 123, "gzip": 45 }],
 *     "inputs": { "<source file>": <bytes it adds to the minified bundle> }
 *   }
 *
 * A missing base report means the base has nothing to compare with.
 *
 * Outputs of compare:
 * - `changed=true|false` appended to $GITHUB_OUTPUT (printed if not set):
 *   whether the minified size of any chunk differs, or the base has nothing
 *   to compare with
 * - bundle-size-comment.md: the sticky pull request comment body
 */
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

// Starts the pull request comment, so that each run updates the one comment
// instead of adding another
const COMMENT_MARKER = '<!-- flow-bundle-size -->';

// Modules that are loaded, statically or dynamically, when an application
// starts Flow. Flow.ts imports the engine through the vaadin-flow-client
// specifier, which the generated Vite config aliases to FlowClient.js.
const ENTRY_POINTS = ['Flow.ts', 'FlowBootstrap.js', 'FlowClient.js'];

// Chunks that more than one entry point or dynamic import share have a name
// made of a content hash, so they are reported together
const SHARED_CHUNKS = 'Shared chunks';

// Caps the source files listed, which a large change could otherwise grow
// past what anyone reads
const MAX_FILES = 15;

function measure(sourcesDir) {
  // The head's esbuild measures both sides, so that both are bundled alike
  const esbuild = require(path.join(__dirname, '..', 'flow-client', 'node_modules', 'esbuild'));
  const result = esbuild.buildSync({
    absWorkingDir: path.resolve(sourcesDir),
    entryPoints: ENTRY_POINTS.filter((entry) => fs.existsSync(path.join(sourcesDir, entry))),
    alias: { 'vaadin-flow-client': './FlowClient.js' },
    packages: 'external',
    bundle: true,
    splitting: true,
    format: 'esm',
    target: 'es2023',
    minify: true,
    legalComments: 'none',
    outdir: 'out',
    entryNames: '[name]',
    chunkNames: 'chunk-[hash]',
    write: false,
    metafile: true,
    logLevel: 'error'
  });
  const contents = new Map(
    result.outputFiles.map((file) => [path.relative(path.resolve(sourcesDir), file.path), file])
  );
  const chunks = new Map();
  const inputs = {};
  for (const [output, meta] of Object.entries(result.metafile.outputs)) {
    const name = meta.entryPoint || SHARED_CHUNKS;
    const gzip = zlib.gzipSync(contents.get(output).contents, { level: 9 }).length;
    const chunk = chunks.get(name) || { name, minified: 0, gzip: 0 };
    chunk.minified += meta.bytes;
    chunk.gzip += gzip;
    chunks.set(name, chunk);
    for (const [input, { bytesInOutput }] of Object.entries(meta.inputs)) {
      inputs[input] = (inputs[input] || 0) + bytesInOutput;
    }
  }
  const order = (chunk) => (chunk.name === SHARED_CHUNKS ? 1 : 0);
  return {
    chunks: [...chunks.values()].sort((a, b) => order(a) - order(b) || a.name.localeCompare(b.name)),
    inputs
  };
}

function readReport(file) {
  if (!fs.existsSync(file)) {
    return undefined;
  }
  const report = JSON.parse(fs.readFileSync(file, 'utf8'));
  const validChunk = (c) => typeof c.name === 'string' && Number.isFinite(c.minified) && Number.isFinite(c.gzip);
  if (!report.chunks?.length || !report.chunks.every(validChunk) || typeof report.inputs !== 'object') {
    throw new Error(`${file} needs chunks with a name, a minified and a gzip size, and inputs`);
  }
  return report;
}

function total(report) {
  return report.chunks.reduce(
    (sum, chunk) => ({ minified: sum.minified + chunk.minified, gzip: sum.gzip + chunk.gzip }),
    { minified: 0, gzip: 0 }
  );
}

function formatBytes(value) {
  return `${value.toLocaleString('en-US')} B`;
}

/**
 * The difference from base to head, in bold and colored green when the
 * client shrinks and red when it grows.
 */
function formatChange(base, head) {
  if (base === undefined) {
    return 'new';
  }
  if (head === undefined) {
    return 'removed';
  }
  if (base === head) {
    return 'no change';
  }
  const difference = head - base;
  const sign = difference > 0 ? '+' : '−';
  const percent = ((Math.abs(difference) / base) * 100).toFixed(1);
  const color = difference > 0 ? 'red' : 'green';
  // GitHub colors text only in math, where \textbf keeps the spaces. The
  // percentage stays outside, as Markdown would unescape the \% math needs
  return `$\\color{${color}}\\textbf{${sign}${formatBytes(Math.abs(difference))}}$ (${sign}${percent}%)`;
}

/**
 * The source files whose bytes in the bundle differ, the largest difference
 * first.
 */
function inputDifferences(base, head) {
  const differences = [];
  for (const name of new Set([...Object.keys(base), ...Object.keys(head)])) {
    const bytes = (head[name] || 0) - (base[name] || 0);
    if (bytes !== 0) {
      differences.push({ name, bytes });
    }
  }
  return differences.sort((a, b) => Math.abs(b.bytes) - Math.abs(a.bytes) || a.name.localeCompare(b.name));
}

/**
 * One row per chunk, with the head's chunks first and then the ones only the
 * base has.
 */
function rows(base, head) {
  const result = new Map();
  for (const [side, report] of [
    ['head', head],
    ['base', base]
  ]) {
    for (const chunk of report?.chunks || []) {
      result.set(chunk.name, { name: chunk.name, ...result.get(chunk.name), [side]: chunk });
    }
  }
  return [...result.values()];
}

function compare(base, head) {
  const hasBase = base !== undefined;
  const changed = !hasBase || rows(base, head).some((row) => row.base?.minified !== row.head?.minified);
  return { hasBase, changed };
}

function comment(base, head, baseCommit) {
  const { hasBase, changed } = compare(base, head);
  const lines = [COMMENT_MARKER, '', '## Client bundle size', ''];
  if (!hasBase) {
    lines.push(`The base commit ${baseCommit} has no client to compare with.`, '');
  } else if (!changed) {
    lines.push(`No change compared with the base commit ${baseCommit}.`, '');
    return lines.join('\n');
  } else {
    lines.push(`Compared with the base commit ${baseCommit}.`, '');
  }
  lines.push('| Chunk | Minified | Change | Gzip | Change |', '| --- | ---: | ---: | ---: | ---: |');
  const row = (name, before, after) => {
    const value = (side) => (side === undefined ? '—' : formatBytes(side));
    const change = (key) => (hasBase ? formatChange(before?.[key], after?.[key]) : '—');
    return `| ${name} | ${value(after?.minified)} | ${change('minified')} | ${value(after?.gzip)} | ${change('gzip')} |`;
  };
  for (const { name, base: before, head: after } of rows(base, head)) {
    lines.push(row(`\`${name}\``, before, after));
  }
  lines.push(row('**Total**', hasBase ? total(base) : undefined, total(head)));
  const differences = hasBase ? inputDifferences(base.inputs, head.inputs) : [];
  if (differences.length > 0) {
    lines.push('', '<details><summary>Minified size by source file</summary>', '');
    lines.push('| Source file | Change |', '| --- | ---: |');
    for (const { name, bytes } of differences.slice(0, MAX_FILES)) {
      const sign = bytes > 0 ? '+' : '−';
      lines.push(`| \`${name}\` | ${sign}${formatBytes(Math.abs(bytes))} |`);
    }
    if (differences.length > MAX_FILES) {
      lines.push(`| …and ${differences.length - MAX_FILES} more | |`);
    }
    lines.push('', '</details>');
  }
  lines.push(
    '',
    '_The `flow-client` sources bundled with esbuild, minified and split at dynamic imports, from `Flow.ts` and the ' +
      'modules it loads. Lit and other packages are left out, as the application bundles them anyway. Shared chunks ' +
      'are those more than one of these modules loads._',
    ''
  );
  return lines.join('\n');
}

function main() {
  const [command, ...args] = process.argv.slice(2);
  if (command === 'measure' && args.length === 2) {
    fs.writeFileSync(args[1], `${JSON.stringify(measure(args[0]), null, 2)}\n`);
    return;
  }
  if (command === 'compare' && args.length === 3) {
    const [baseFile, headFile, baseCommit] = args;
    const head = readReport(headFile);
    if (head === undefined) {
      console.error(`No report found in ${headFile}`);
      process.exit(1);
    }
    const base = readReport(baseFile);
    fs.writeFileSync('bundle-size-comment.md', comment(base, head, baseCommit));
    const output = `changed=${compare(base, head).changed}\n`;
    if (process.env.GITHUB_OUTPUT) {
      fs.appendFileSync(process.env.GITHUB_OUTPUT, output);
    } else {
      process.stdout.write(output);
    }
    return;
  }
  console.error(
    'Usage: node scripts/bundleSize.js measure <client sources dir> <report file>\n' +
      '       node scripts/bundleSize.js compare <base report> <head report> <base commit>'
  );
  process.exit(1);
}

if (require.main === module) {
  main();
}

module.exports = { measure, compare, comment, inputDifferences, readReport };
