#!/usr/bin/env node
/**
 * Compares the memory footprint measured on the base and the head of a pull
 * request, and writes the pull request comment that reports it.
 *
 * The measurements are the JSON files UIMemoryFootprintTest (in
 * flow-html-components) writes to target/memory-footprint, one per scenario:
 * the bytes a UI retains, the size of a serialized session with one UI and the
 * retained instances and bytes per class. They are exact, so any difference is
 * caused by the change and not by noise.
 *
 * Usage: node scripts/memoryFootprint.js <base dir> <head dir> <base commit>
 *
 * Outputs:
 * - `changed=true|false` appended to $GITHUB_OUTPUT (printed if not set):
 *   whether any measurement differs, or the base has none to compare with
 * - memory-comment.md: the sticky pull request comment body
 */
const fs = require('fs');
const path = require('path');

// Starts the pull request comment, so that each run updates the one comment
// instead of adding another
const COMMENT_MARKER = '<!-- flow-memory-footprint -->';

// Caps the classes listed per scenario, which a large change could otherwise
// grow past what anyone reads
const MAX_CLASSES = 15;

const METRICS = [
  { key: 'retainedBytesPerUI', title: 'Retained per UI' },
  { key: 'serializedSessionBytes', title: 'Serialized session' }
];

function readMeasurements(dir) {
  if (!fs.existsSync(dir)) {
    return {};
  }
  const measurements = {};
  for (const file of fs.readdirSync(dir).filter((name) => name.endsWith('.json')).sort()) {
    measurements[path.basename(file, '.json')] = JSON.parse(fs.readFileSync(path.join(dir, file), 'utf8'));
  }
  return measurements;
}

function formatBytes(bytes) {
  return `${bytes.toLocaleString('en-US')} B`;
}

function formatDifference(base, head) {
  if (base === undefined) {
    return formatBytes(head);
  }
  if (head === undefined) {
    return `${formatBytes(base)} → removed`;
  }
  if (base === head) {
    return `${formatBytes(head)} (no change)`;
  }
  const difference = head - base;
  const sign = difference > 0 ? '+' : '−';
  const percent = ((Math.abs(difference) / base) * 100).toFixed(1);
  return `${formatBytes(base)} → ${formatBytes(head)} (**${sign}${formatBytes(Math.abs(difference))}**, ${sign}${percent}%)`;
}

/**
 * The classes whose retained instances or bytes differ, the largest
 * difference in bytes first.
 */
function classDifferences(base, head) {
  const names = new Set([...Object.keys(base), ...Object.keys(head)]);
  const differences = [];
  for (const name of names) {
    const before = base[name] || { count: 0, bytes: 0 };
    const after = head[name] || { count: 0, bytes: 0 };
    if (before.count !== after.count || before.bytes !== after.bytes) {
      differences.push({ name, count: after.count - before.count, bytes: after.bytes - before.bytes });
    }
  }
  return differences.sort((a, b) => Math.abs(b.bytes) - Math.abs(a.bytes) || a.name.localeCompare(b.name));
}

function signed(number) {
  return (number > 0 ? '+' : number < 0 ? '−' : '') + Math.abs(number).toLocaleString('en-US');
}

function compare(base, head) {
  const scenarios = [...new Set([...Object.keys(head), ...Object.keys(base)])];
  const hasBase = Object.keys(base).length > 0;
  const changed =
    !hasBase ||
    scenarios.some((scenario) =>
      METRICS.some(({ key }) => base[scenario]?.[key] !== head[scenario]?.[key])
    );
  return { scenarios, hasBase, changed };
}

function comment(base, head, baseCommit) {
  const { scenarios, hasBase, changed } = compare(base, head);
  const lines = [COMMENT_MARKER, '', '## Memory footprint', ''];
  if (!hasBase) {
    lines.push(`The base commit ${baseCommit} has no memory measurements to compare with.`, '');
  } else if (!changed) {
    lines.push(`No change compared with the base commit ${baseCommit}.`, '');
  } else {
    lines.push(`Compared with the base commit ${baseCommit}.`, '');
  }
  lines.push(`| Scenario | ${METRICS.map(({ title }) => title).join(' | ')} |`);
  lines.push(`| --- |${' --- |'.repeat(METRICS.length)}`);
  for (const scenario of scenarios) {
    const cells = METRICS.map(({ key }) =>
      formatDifference(hasBase ? base[scenario]?.[key] : undefined, head[scenario]?.[key])
    );
    lines.push(`| \`${scenario}\` | ${cells.join(' | ')} |`);
  }
  if (hasBase) {
    for (const scenario of scenarios.filter((name) => base[name] && head[name])) {
      const differences = classDifferences(base[scenario].classes, head[scenario].classes);
      if (differences.length === 0) {
        continue;
      }
      lines.push('', `<details><summary>Retained per UI by class: <code>${scenario}</code></summary>`, '');
      lines.push('| Class | Instances | Bytes |', '| --- | ---: | ---: |');
      for (const { name, count, bytes } of differences.slice(0, MAX_CLASSES)) {
        lines.push(`| \`${name}\` | ${signed(count)} | ${signed(bytes)} |`);
      }
      if (differences.length > MAX_CLASSES) {
        lines.push(`| …and ${differences.length - MAX_CLASSES} more | | |`);
      }
      lines.push('', '</details>');
    }
  }
  lines.push(
    '',
    '_Measured by `UIMemoryFootprintTest`: the bytes a UI retains on the heap, ' +
      'and the size of a serialized session with one UI open. The measurements are exact, ' +
      'so any difference comes from the change._',
    ''
  );
  return lines.join('\n');
}

function main() {
  const [baseDir, headDir, baseCommit] = process.argv.slice(2);
  if (!baseDir || !headDir || !baseCommit) {
    console.error('Usage: node scripts/memoryFootprint.js <base dir> <head dir> <base commit>');
    process.exit(1);
  }
  const base = readMeasurements(baseDir);
  const head = readMeasurements(headDir);
  if (Object.keys(head).length === 0) {
    console.error(`No measurements found in ${headDir}`);
    process.exit(1);
  }
  fs.writeFileSync('memory-comment.md', comment(base, head, baseCommit));
  const output = `changed=${compare(base, head).changed}\n`;
  if (process.env.GITHUB_OUTPUT) {
    fs.appendFileSync(process.env.GITHUB_OUTPUT, output);
  } else {
    process.stdout.write(output);
  }
}

if (require.main === module) {
  main();
}

module.exports = { compare, comment, classDifferences };
