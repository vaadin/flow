#!/usr/bin/env node
/**
 * Measures the production bundle of a default Flow application, and compares
 * the sizes measured on the base and the head of a pull request in the pull
 * request comment that reports them.
 *
 * The bundle is the one the production build of
 * flow-tests/test-express-build/test-prod-bundle writes, an application with
 * no code of its own. It holds what Flow adds to every application: the
 * client, the files Flow generates, such as the functions of every
 * JsDefinition, and the npm packages Flow brings in, bundled and minified by
 * the application's Vite build.
 *
 * Usage:
 *   node scripts/bundleSize.js measure <prod bundle dir> <report file>
 *   node scripts/bundleSize.js compare <base report> <head report> <base commit>
 *
 * The prod bundle dir holds the files the browser loads under webapp, and the
 * module sizes the build reports in config/bundle-size.html. The report
 * written by measure:
 *
 *   {
 *     "chunks": [{ "name": "VAADIN/build/FlowClient.js", "minified": 123, "brotli": 45 }],
 *     "inputs": { "<module>": <bytes it renders to before minification> }
 *   }
 *
 * A missing base report means the base has nothing to compare with.
 *
 * Outputs of compare:
 * - `changed=true|false` appended to $GITHUB_OUTPUT (printed if not set):
 *   whether the minified size of any file differs, or the base has nothing to compare
 *   with
 * - bundle-size-comment.md: the sticky pull request comment body
 */
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const { formatValue, formatDifference } = require('./prCommentFormat');

// Starts the pull request comment, so that each run updates the one comment
// instead of adding another
const COMMENT_MARKER = '<!-- flow-bundle-size -->';

// Vite names a chunk in VAADIN/build after its first module and a content
// hash, which changes with every change and would make every chunk look new.
// Files outside it, such as the service worker, have no hash.
const CONTENT_HASH = /^(VAADIN\/build\/.+)(-[\w-]{8})(?=\.(js|css)$)/;

// Where the module sizes are in the report rollup-plugin-visualizer writes
const VISUALIZER_DATA = /const data = (\{.*?\});\n/s;

// Caps the modules listed, which a large change could otherwise grow past
// what anyone reads
const MAX_FILES = 15;

function filesUnder(dir) {
  return fs
    .readdirSync(dir, { withFileTypes: true, recursive: true })
    .filter((entry) => entry.isFile())
    .map((entry) => path.join(entry.parentPath, entry.name));
}

/**
 * The size each module renders to in the bundle, before minification, as
 * the build reports it. Only tells which modules changed: the minified sizes
 * of single modules are not known.
 */
function moduleSizes(reportFile) {
  const match = fs.readFileSync(reportFile, 'utf8').match(VISUALIZER_DATA);
  if (!match) {
    throw new Error(`${reportFile} has no module sizes`);
  }
  const { nodeParts, nodeMetas } = JSON.parse(match[1]);
  const sizes = {};
  for (const meta of Object.values(nodeMetas)) {
    const name = meta.id.replace(/^\//, '');
    sizes[name] = Object.values(meta.moduleParts).reduce((sum, uid) => sum + nodeParts[uid].renderedLength, 0);
  }
  return sizes;
}

/**
 * The content of a file with the content hashes of the bundle and the
 * revisions in the service worker's precache manifest replaced. They change
 * with every change to the file they name, which changes how well another
 * file that names it compresses, while its length stays the same.
 */
function withoutHashes(content, hashes) {
  let result = content.replace(/(revision:\W?)[0-9a-f]{32}/g, (_, prefix) => prefix + '0'.repeat(32));
  for (const hash of hashes) {
    result = result.replaceAll(hash, 'x'.repeat(hash.length));
  }
  return result;
}

function measure(bundleDir) {
  const webapp = path.join(bundleDir, 'webapp');
  const files = filesUnder(webapp).filter((name) => /\.(js|css)$/.test(name));
  if (files.length === 0) {
    throw new Error(`No bundle found in ${webapp}`);
  }
  const names = files.map((file) => path.relative(webapp, file).split(path.sep).join('/'));
  const hashes = names.map((name) => name.match(CONTENT_HASH)?.[2]).filter(Boolean);
  const chunks = new Map();
  for (const [index, file] of files.entries()) {
    const name = names[index].replace(CONTENT_HASH, '$1');
    const content = withoutHashes(fs.readFileSync(file, 'utf8'), hashes);
    const chunk = chunks.get(name) || { name, minified: 0, brotli: 0 };
    chunk.minified += Buffer.byteLength(content);
    // What the build precompresses the bundle with, and what a browser gets
    chunk.brotli += zlib.brotliCompressSync(content, {
      params: { [zlib.constants.BROTLI_PARAM_QUALITY]: zlib.constants.BROTLI_MAX_QUALITY }
    }).length;
    chunks.set(name, chunk);
  }
  return {
    chunks: [...chunks.values()].sort((a, b) => a.name.localeCompare(b.name)),
    inputs: moduleSizes(path.join(bundleDir, 'config', 'bundle-size.html'))
  };
}

function readReport(file) {
  if (!fs.existsSync(file)) {
    return undefined;
  }
  const report = JSON.parse(fs.readFileSync(file, 'utf8'));
  const validChunk = (c) => typeof c.name === 'string' && Number.isFinite(c.minified) && Number.isFinite(c.brotli);
  if (!report.chunks?.length || !report.chunks.every(validChunk) || typeof report.inputs !== 'object') {
    throw new Error(`${file} needs chunks with a name, a minified and a brotli size, and inputs`);
  }
  return report;
}

function total(report) {
  return report.chunks.reduce(
    (sum, chunk) => ({ minified: sum.minified + chunk.minified, brotli: sum.brotli + chunk.brotli }),
    { minified: 0, brotli: 0 }
  );
}

function formatBytes(value) {
  return formatValue(value, 'bytes');
}

function formatChange(base, head) {
  if (base === undefined) {
    return 'new';
  }
  if (head === undefined) {
    return 'removed';
  }
  return base === head ? 'no change' : formatDifference(base, head, 'bytes');
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
  const lines = [COMMENT_MARKER, '', '## Bundle size', ''];
  if (!hasBase) {
    lines.push(`The base commit ${baseCommit} has no bundle to compare with.`, '');
  } else if (!changed) {
    lines.push(`No change compared with the base commit ${baseCommit}.`, '');
    return lines.join('\n');
  } else {
    lines.push(`Compared with the base commit ${baseCommit}.`, '');
  }
  lines.push('| File | Minified | Change | Brotli | Change |', '| --- | ---: | ---: | ---: | ---: |');
  const row = (name, before, after) => {
    const value = (side) => (side === undefined ? '—' : formatBytes(side));
    const change = (key) => (hasBase ? formatChange(before?.[key], after?.[key]) : '—');
    return `| ${name} | ${value(after?.minified)} | ${change('minified')} | ${value(after?.brotli)} | ${change('brotli')} |`;
  };
  for (const { name, base: before, head: after } of rows(base, head)) {
    lines.push(row(`\`${name}\``, before, after));
  }
  lines.push(row('**Total**', hasBase ? total(base) : undefined, total(head)));
  const differences = hasBase ? inputDifferences(base.inputs, head.inputs) : [];
  if (differences.length > 0) {
    lines.push('', '<details><summary>Size by module, before minification</summary>', '');
    lines.push('| Module | Change |', '| --- | ---: |');
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
    '_The production bundle of `test-prod-bundle`, an application with no code of its own: the Flow client, the ' +
      'files Flow generates and the npm packages it brings in. Brotli is what the build precompresses the files with._',
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
    'Usage: node scripts/bundleSize.js measure <prod bundle dir> <report file>\n' +
      '       node scripts/bundleSize.js compare <base report> <head report> <base commit>'
  );
  process.exit(1);
}

if (require.main === module) {
  main();
}

module.exports = { measure, withoutHashes, compare, comment, inputDifferences, readReport };
