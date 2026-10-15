#!/usr/bin/env node
/**
 * Compares the memory footprint measured on the base and the head of a pull
 * request, and writes the pull request comment that reports it.
 *
 * The measurements are the JSON reports UIMemoryFootprintTest (in
 * flow-html-components) writes to target/memory-footprint, one per scenario,
 * named after the id of the scenario:
 *
 *   {
 *     "title": "...", "description": "...",
 *     "metrics": [{ "id": "...", "title": "...", "unit": "bytes", "value": 123,
 *                   "breakdown": { "<class>": { "count": 1, "bytes": 16 } } }]
 *   }
 *
 * The scenarios and metrics are defined by the test alone: this script
 * matches them by id and reports whatever it finds. The breakdown is
 * optional. Differences smaller than MIN_CHANGE are not counted as changes.
 *
 * Usage: node scripts/memoryFootprint.js <base dir> <head dir> <base commit>
 *
 * Outputs:
 * - `changed=true|false` appended to $GITHUB_OUTPUT (printed if not set):
 *   whether any measurement differs by at least MIN_CHANGE, or the base has
 *   none to compare with
 * - memory-comment.md: the sticky pull request comment body
 */
const fs = require('fs');
const path = require('path');

// Starts the pull request comment, so that each run updates the one comment
// instead of adding another
const COMMENT_MARKER = '<!-- flow-memory-footprint -->';

// Caps the classes listed per breakdown, which a large change could otherwise
// grow past what anyone reads
const MAX_CLASSES = 15;

// The response kept for resending is stored compressed, and the order of its
// constants can differ between runs, so the serialized size can vary by a
// byte or so without any change
const MIN_CHANGE = 5;

/**
 * Whether a metric changed from base to head by at least MIN_CHANGE, or is
 * there on only one side.
 */
function isChange(base, head) {
  if (base === undefined || head === undefined) {
    return base !== head;
  }
  return Math.abs(head - base) >= MIN_CHANGE;
}

function readMeasurements(dir) {
  if (!fs.existsSync(dir)) {
    return {};
  }
  const measurements = {};
  for (const file of fs.readdirSync(dir).filter((name) => name.endsWith('.json')).sort()) {
    const report = JSON.parse(fs.readFileSync(path.join(dir, file), 'utf8'));
    // A change to what the test writes has to fail here, rather than make
    // every metric look unchanged
    const validMetric = (m) => typeof m.id === 'string' && typeof m.title === 'string' && Number.isFinite(m.value);
    if (typeof report.title !== 'string' || !report.metrics?.length || !report.metrics.every(validMetric)) {
      throw new Error(`${file} needs a title and metrics with an id, a title and a numeric value`);
    }
    measurements[path.basename(file, '.json')] = report;
  }
  return measurements;
}

function formatValue(value, unit) {
  const number = value.toLocaleString('en-US');
  if (unit === 'bytes') {
    return `${number} B`;
  }
  return unit ? `${number} ${unit}` : number;
}

/**
 * The difference from base to head, in bold and colored green when the
 * footprint shrinks and red when it grows.
 */
function formatChange(hasBase, base, head, unit) {
  if (!hasBase) {
    return '—';
  }
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
  if (!isChange(base, head)) {
    return `${sign}${formatValue(Math.abs(difference), unit)} (ignored)`;
  }
  const percent = ((Math.abs(difference) / base) * 100).toFixed(1);
  const color = difference > 0 ? 'red' : 'green';
  // GitHub colors text only in math, where \textbf keeps the spaces. The
  // percentage stays outside, as Markdown would unescape the \% math needs
  return `$\\color{${color}}\\textbf{${sign}${formatValue(Math.abs(difference), unit)}}$ (${sign}${percent}%)`;
}

/**
 * One row per metric of each scenario, with the metric as measured on the base
 * and on the head. The scenarios and metrics of the head come first, then the
 * ones only the base has.
 */
function scenarioIds(base, head) {
  return [...new Set([...Object.keys(head), ...Object.keys(base)])];
}

function rows(base, head) {
  const result = [];
  for (const id of scenarioIds(base, head)) {
    const metrics = new Map();
    for (const [side, report] of [['head', head[id]], ['base', base[id]]]) {
      for (const metric of report?.metrics || []) {
        metrics.set(metric.id, { ...metrics.get(metric.id), [side]: metric });
      }
    }
    for (const { base: before, head: after } of metrics.values()) {
      const metric = after || before;
      result.push({ scenario: head[id] || base[id], title: metric.title, unit: metric.unit, base: before, head: after });
    }
  }
  return result;
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

function compare(base, head, allRows = rows(base, head)) {
  const hasBase = Object.keys(base).length > 0;
  const changed = !hasBase || allRows.some((row) => isChange(row.base?.value, row.head?.value));
  return { hasBase, changed };
}

function comment(base, head, baseCommit) {
  const allRows = rows(base, head);
  const { hasBase, changed } = compare(base, head, allRows);
  const lines = [COMMENT_MARKER, '', '## Memory footprint', ''];
  if (!hasBase) {
    lines.push(`The base commit ${baseCommit} has no memory measurements to compare with.`, '');
  } else if (!changed) {
    lines.push(`No change compared with the base commit ${baseCommit}.`, '');
    return lines.join('\n');
  } else {
    lines.push(`Compared with the base commit ${baseCommit}.`, '');
  }
  lines.push('| Scenario | Metric | Base | Head | Change |', '| --- | --- | ---: | ---: | ---: |');
  for (const row of allRows) {
    const before = hasBase ? row.base?.value : undefined;
    const after = row.head?.value;
    const baseValue = before === undefined ? '—' : formatValue(before, row.unit);
    const headValue = after === undefined ? '—' : formatValue(after, row.unit);
    const change = formatChange(hasBase, before, after, row.unit);
    lines.push(`| ${row.scenario.title} | ${row.title} | ${baseValue} | ${headValue} | ${change} |`);
  }
  for (const row of allRows.filter((r) => r.base?.breakdown && r.head?.breakdown)) {
    const differences = classDifferences(row.base.breakdown, row.head.breakdown);
    if (differences.length === 0) {
      continue;
    }
    lines.push('', `<details><summary>${row.scenario.title}: ${row.title} by class</summary>`, '');
    lines.push('| Class | Instances | Bytes |', '| --- | ---: | ---: |');
    for (const { name, count, bytes } of differences.slice(0, MAX_CLASSES)) {
      lines.push(`| \`${name}\` | ${signed(count)} | ${signed(bytes)} |`);
    }
    if (differences.length > MAX_CLASSES) {
      lines.push(`| …and ${differences.length - MAX_CLASSES} more | | |`);
    }
    lines.push('', '</details>');
  }
  lines.push('', '<details><summary>Scenarios</summary>', '');
  for (const id of scenarioIds(base, head)) {
    const scenario = head[id] || base[id];
    lines.push(`- **${scenario.title}** (\`${id}\`)${scenario.description ? `: ${scenario.description}` : ''}`);
  }
  lines.push(
    '',
    '</details>',
    '',
    `_Measured by \`UIMemoryFootprintTest\`. Differences under ${MIN_CHANGE} B are ignored._`,
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

module.exports = { compare, comment, classDifferences, readMeasurements };
