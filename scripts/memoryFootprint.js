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
 *        node scripts/memoryFootprint.js paths <commit>
 *
 * The second form prints the paths the measurements of a commit depend on:
 * the root pom and the modules the test module is built with by `mvn -am`,
 * read from the poms of that commit. The workflow caches measurements under a
 * hash of what these paths hold, so it has no list of modules to keep up to
 * date.
 *
 * Outputs:
 * - `changed=true|false` appended to $GITHUB_OUTPUT (printed if not set):
 *   whether any measurement differs, or the base has none to compare with
 * - memory-comment.md: the sticky pull request comment body
 */
const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');

// Starts the pull request comment, so that each run updates the one comment
// instead of adding another
const COMMENT_MARKER = '<!-- flow-memory-footprint -->';

// The module UIMemoryFootprintTest is in
const TEST_MODULE = 'flow-html-components';

// Caps the classes listed per breakdown, which a large change could otherwise
// grow past what anyone reads
const MAX_CLASSES = 15;

function readMeasurements(dir) {
  if (!fs.existsSync(dir)) {
    return {};
  }
  const measurements = {};
  for (const file of fs.readdirSync(dir).filter((name) => name.endsWith('.json')).sort()) {
    const report = JSON.parse(fs.readFileSync(path.join(dir, file), 'utf8'));
    validateReport(file, report);
    measurements[path.basename(file, '.json')] = report;
  }
  return measurements;
}

/**
 * Throws when a report does not have the shape described above, so that a
 * change to what the test writes fails the comparison instead of making every
 * metric look unchanged.
 */
function validateReport(name, report) {
  const problems = [];
  if (typeof report?.title !== 'string') {
    problems.push('no title');
  }
  if (!Array.isArray(report?.metrics) || report.metrics.length === 0) {
    problems.push('no metrics');
  } else {
    report.metrics.forEach((metric, index) => {
      if (typeof metric?.id !== 'string' || typeof metric.title !== 'string' || !Number.isFinite(metric.value)) {
        problems.push(`metric ${index} needs a string id and title and a numeric value`);
      }
    });
  }
  if (problems.length > 0) {
    throw new Error(`Invalid memory footprint report ${name}: ${problems.join(', ')}`);
  }
}

function withoutBlocks(xml, ...tags) {
  return tags.reduce((result, tag) => result.replace(new RegExp(`<${tag}>[\\s\\S]*?</${tag}>`, 'g'), ''), xml);
}

function firstArtifactId(xml) {
  return /<artifactId>\s*([^<\s]+)\s*<\/artifactId>/.exec(xml)?.[1];
}

/**
 * The modules of the reactor, keyed by artifact id, read from the pom in the
 * given directory and, recursively, from the poms of the modules it lists
 * outside of profiles.
 */
function reactorModules(readFile, dir = '', modules = {}) {
  const source = readFile(dir ? `${dir}/pom.xml` : 'pom.xml');
  if (source === null) {
    return modules;
  }
  // Commented-out modules, dependencies and artifact ids are not part of the
  // build
  const pom = source.replace(/<!--[\s\S]*?-->/g, '');
  const own = withoutBlocks(pom, 'parent', 'dependencyManagement', 'dependencies', 'build', 'profiles', 'modules');
  modules[firstArtifactId(own)] = { dir, pom };
  const moduleList = withoutBlocks(pom, 'profiles');
  for (const [, module] of moduleList.matchAll(/<module>\s*([^<\s]+)\s*<\/module>/g)) {
    reactorModules(readFile, dir ? `${dir}/${module}` : module, modules);
  }
  return modules;
}

/**
 * The artifact ids a pom depends on, its parent included, leaving out
 * plugins, profiles and managed versions, which do not add a module to the
 * build.
 */
function dependencies(pom) {
  const parent = /<parent>([\s\S]*?)<\/parent>/.exec(pom);
  const ids = parent ? [firstArtifactId(parent[1])] : [];
  const declared = withoutBlocks(pom, 'exclusions', 'dependencyManagement', 'build', 'profiles');
  for (const [, dependency] of declared.matchAll(/<dependency>([\s\S]*?)<\/dependency>/g)) {
    ids.push(firstArtifactId(dependency));
  }
  return ids;
}

/**
 * The paths the measurements depend on: the root pom and the directory of
 * every module `mvn -pl <test module> -am` builds.
 */
function measuredPaths(readFile, testModule = TEST_MODULE) {
  const modules = reactorModules(readFile);
  const byDir = Object.fromEntries(Object.entries(modules).map(([id, module]) => [module.dir, id]));
  const reached = new Set();
  const pending = [byDir[testModule]];
  while (pending.length > 0) {
    const id = pending.pop();
    if (id === undefined || reached.has(id) || !modules[id]) {
      continue;
    }
    reached.add(id);
    pending.push(...dependencies(modules[id].pom));
  }
  if (reached.size === 0) {
    throw new Error(`No module ${testModule} in the reactor`);
  }
  return [...reached].map((id) => modules[id].dir || 'pom.xml').sort();
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
  if (process.argv[2] === 'paths') {
    const commit = process.argv[3];
    const readFile = (file) => {
      try {
        return execFileSync('git', ['show', `${commit}:${file}`], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] });
      } catch {
        return null;
      }
    };
    process.stdout.write(measuredPaths(readFile).join('\n') + '\n');
    return;
  }
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

module.exports = { compare, comment, classDifferences, validateReport, measuredPaths };
