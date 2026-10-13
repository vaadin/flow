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
 * optional. The measurements are exact, so any difference is caused by the
 * change and not by noise.
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

// Caps the classes listed per breakdown, which a large change could otherwise
// grow past what anyone reads
const MAX_CLASSES = 15;

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

function formatValue(value, unit) {
  const number = value.toLocaleString('en-US');
  if (unit === 'bytes') {
    return `${number} B`;
  }
  return unit ? `${number} ${unit}` : number;
}

function formatDifference(base, head, unit) {
  if (base === undefined) {
    return formatValue(head, unit);
  }
  if (head === undefined) {
    return `${formatValue(base, unit)} → removed`;
  }
  if (base === head) {
    return `${formatValue(head, unit)} (no change)`;
  }
  const difference = head - base;
  const sign = difference > 0 ? '+' : '−';
  const percent = ((Math.abs(difference) / base) * 100).toFixed(1);
  return `${formatValue(base, unit)} → ${formatValue(head, unit)} (**${sign}${formatValue(Math.abs(difference), unit)}**, ${sign}${percent}%)`;
}

function metricsById(report) {
  return Object.fromEntries((report?.metrics || []).map((metric) => [metric.id, metric]));
}

/**
 * The scenarios of the head followed by the ones only the base has, each with
 * its metrics in the same order and the base and head value of each.
 */
function rows(base, head) {
  const scenarios = [...new Set([...Object.keys(head), ...Object.keys(base)])];
  return scenarios.map((id) => {
    const baseMetrics = metricsById(base[id]);
    const headMetrics = metricsById(head[id]);
    const metricIds = [...new Set([...Object.keys(headMetrics), ...Object.keys(baseMetrics)])];
    return {
      id,
      title: head[id]?.title || base[id]?.title || id,
      description: head[id]?.description || base[id]?.description,
      metrics: metricIds.map((metricId) => ({
        id: metricId,
        base: baseMetrics[metricId],
        head: headMetrics[metricId],
        title: headMetrics[metricId]?.title || baseMetrics[metricId]?.title || metricId,
        unit: headMetrics[metricId]?.unit || baseMetrics[metricId]?.unit
      }))
    };
  });
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
  const hasBase = Object.keys(base).length > 0;
  const changed =
    !hasBase ||
    rows(base, head).some((scenario) =>
      scenario.metrics.some((metric) => metric.base?.value !== metric.head?.value)
    );
  return { hasBase, changed };
}

function comment(base, head, baseCommit) {
  const { hasBase, changed } = compare(base, head);
  const scenarios = rows(base, head);
  const lines = [COMMENT_MARKER, '', '## Memory footprint', ''];
  if (!hasBase) {
    lines.push(`The base commit ${baseCommit} has no memory measurements to compare with.`, '');
  } else if (!changed) {
    lines.push(`No change compared with the base commit ${baseCommit}.`, '');
  } else {
    lines.push(`Compared with the base commit ${baseCommit}.`, '');
  }
  lines.push('| Scenario | Metric | Value |', '| --- | --- | --- |');
  for (const scenario of scenarios) {
    for (const metric of scenario.metrics) {
      const value = formatDifference(hasBase ? metric.base?.value : undefined, metric.head?.value, metric.unit);
      lines.push(`| ${scenario.title} | ${metric.title} | ${value} |`);
    }
  }
  if (hasBase) {
    for (const scenario of scenarios) {
      for (const metric of scenario.metrics.filter((m) => m.base?.breakdown && m.head?.breakdown)) {
        const differences = classDifferences(metric.base.breakdown, metric.head.breakdown);
        if (differences.length === 0) {
          continue;
        }
        lines.push('', `<details><summary>${scenario.title}: ${metric.title} by class</summary>`, '');
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
  }
  lines.push('', '<details><summary>Scenarios</summary>', '');
  for (const scenario of scenarios) {
    lines.push(`- **${scenario.title}** (\`${scenario.id}\`)${scenario.description ? `: ${scenario.description}` : ''}`);
  }
  lines.push(
    '',
    '</details>',
    '',
    '_Measured by `UIMemoryFootprintTest`. The measurements are exact, so any difference comes from the change._',
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
