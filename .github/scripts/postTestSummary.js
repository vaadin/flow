// Builds and posts the PR test summary comment from the JSON output of
// EnricoMi/publish-unit-test-result-action. Imported by validation.yml via
// actions/github-script, and runnable standalone for local previews:
//
//   node .github/scripts/postTestSummary.js --demo pass
//   node .github/scripts/postTestSummary.js --demo fail
//   node .github/scripts/postTestSummary.js path/to/results.json

const MARKER = '<!-- test-results-summary -->';

function buildBody(data) {
  const stats = data.stats || {};
  const delta = data.stats_with_delta || null;
  const checkUrl = data.check_url;

  const fmt = (n) => Number(n || 0).toLocaleString('en-US');
  const formatDuration = (sec) => {
    sec = Math.round(sec || 0);
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    const s = sec % 60;
    const parts = [];
    if (h > 0) parts.push(`${h}h`);
    if (m > 0 || h > 0) parts.push(`${m}m`);
    parts.push(`${s}s`);
    return parts.join(' ');
  };

  // stats_with_delta entries are {number, delta} for counts and
  // {duration, delta} for duration; only the delta side is used here.
  const dNum = (key) => (delta && delta[key] ? delta[key].delta || 0 : 0);
  const dDur = () => (delta && delta.duration ? delta.duration.delta || 0 : 0);
  const fmtDelta = (d) => {
    if (!d) return '';
    const sign = d > 0 ? '+' : '−';
    return ` (${sign}${fmt(Math.abs(d))})`;
  };

  const tests = stats.tests || 0;
  const passed = stats.tests_succ || 0;
  const skipped = stats.tests_skip || 0;
  const failed = (stats.tests_fail || 0) + (stats.tests_error || 0);

  const headlines = [];
  if (failed === 0 && tests > 0) {
    headlines.push(`### ✅ All ${fmt(passed)} tests pass`);
  } else if (failed > 0) {
    headlines.push(`### ❌ ${fmt(failed)} ${failed === 1 ? 'test' : 'tests'} failed`);
  } else {
    headlines.push(`### ⚠️ No test results`);
  }

  const testsDelta = dNum('tests');
  if (testsDelta > 0) {
    headlines.push(`✅ ${fmt(testsDelta)} ${testsDelta === 1 ? 'test' : 'tests'} added`);
  } else if (testsDelta < 0) {
    headlines.push(`❌ ${fmt(-testsDelta)} ${testsDelta === -1 ? 'test' : 'tests'} removed`);
  }

  const skipDelta = dNum('tests_skip');
  if (skipDelta > 0) {
    headlines.push(`💤 ${fmt(skipDelta)} ${skipDelta === 1 ? 'test' : 'tests'} newly skipped`);
  } else if (skipDelta < 0) {
    headlines.push(`▶️ ${fmt(-skipDelta)} ${skipDelta === -1 ? 'test' : 'tests'} no longer skipped`);
  }

  const failPlain = `${fmt(failed)}${fmtDelta(dNum('tests_fail') + dNum('tests_error'))}`;
  const runsFailed = (stats.runs_fail || 0) + (stats.runs_error || 0);
  const runsFailPlain = `${fmt(runsFailed)}${fmtDelta(dNum('runs_fail') + dNum('runs_error'))}`;

  const detailRows = [
    '| | Total | ✅ Pass | 💤 Skip | ❌ Fail |',
    '|---|---|---|---|---|',
    `| Tests | ${fmt(tests)}${fmtDelta(dNum('tests'))} | ${fmt(passed)}${fmtDelta(dNum('tests_succ'))} | ${fmt(skipped)}${fmtDelta(dNum('tests_skip'))} | ${failPlain} |`,
    `| Runs | ${fmt(stats.runs)}${fmtDelta(dNum('runs'))} | ${fmt(stats.runs_succ)}${fmtDelta(dNum('runs_succ'))} | ${fmt(stats.runs_skip)}${fmtDelta(dNum('runs_skip'))} | ${runsFailPlain} |`,
  ];

  const durationLine = `**${fmt(stats.files)}** files · **${fmt(stats.suites)}** suites · **${formatDuration(stats.duration)}**${fmtDelta(dDur())}`;
  const commitShort = (sha) => (sha ? sha.substring(0, 7) : '');
  const commitLine = delta && delta.commit
    ? `Results for commit \`${commitShort(delta.commit)}\`${delta.reference_commit ? `, compared against base \`${commitShort(delta.reference_commit)}\`` : ''}.`
    : (stats.commit ? `Results for commit \`${commitShort(stats.commit)}\`.` : '');

  return [
    MARKER,
    '## Test Results',
    '',
    headlines.join('\n'),
    '',
    '<details><summary>Details</summary>',
    '',
    ...detailRows,
    '',
    durationLine,
    '',
    commitLine,
    checkUrl ? `\n[View full check run →](${checkUrl})` : '',
    '',
    '</details>',
  ].filter((line) => line !== null && line !== undefined).join('\n');
}

async function post({ github, context, json }) {
  const data = JSON.parse(json || '{}');
  const body = buildBody(data);

  const { data: comments } = await github.rest.issues.listComments({
    owner: context.repo.owner,
    repo: context.repo.repo,
    issue_number: context.issue.number,
    per_page: 100,
  });
  const existing = comments.find((c) => c.body && c.body.includes(MARKER));

  if (existing) {
    await github.rest.issues.updateComment({
      owner: context.repo.owner,
      repo: context.repo.repo,
      comment_id: existing.id,
      body,
    });
  } else {
    await github.rest.issues.createComment({
      owner: context.repo.owner,
      repo: context.repo.repo,
      issue_number: context.issue.number,
      body,
    });
  }
}

const demoFixtures = {
  pass: {
    stats: {
      files: 1410, suites: 1410, duration: 4913,
      tests: 10163, tests_succ: 10094, tests_skip: 69, tests_fail: 0, tests_error: 0,
      runs: 10638, runs_succ: 10567, runs_skip: 71, runs_fail: 0, runs_error: 0,
      commit: 'ea899120000000000000000000000000',
    },
    stats_with_delta: {
      files: { number: 1410, delta: 0 },
      suites: { number: 1410, delta: 0 },
      duration: { duration: 4913, delta: 118 },
      tests: { number: 10163, delta: 3 },
      tests_succ: { number: 10094, delta: 3 },
      tests_skip: { number: 69, delta: 0 },
      tests_fail: { number: 0, delta: 0 },
      tests_error: { number: 0, delta: 0 },
      runs: { number: 10638, delta: 3 },
      runs_succ: { number: 10567, delta: 3 },
      runs_skip: { number: 71, delta: 0 },
      runs_fail: { number: 0, delta: 0 },
      runs_error: { number: 0, delta: 0 },
      commit: 'ea899120000000000000000000000000',
      reference_commit: '36972410000000000000000000000000',
    },
    check_url: 'https://github.com/vaadin/flow/runs/12345',
  },
  fail: {
    stats: {
      files: 1410, suites: 1410, duration: 4913,
      tests: 10158, tests_succ: 10077, tests_skip: 69, tests_fail: 10, tests_error: 2,
      runs: 10633, runs_succ: 10550, runs_skip: 71, runs_fail: 10, runs_error: 2,
      commit: 'ea899120000000000000000000000000',
    },
    stats_with_delta: {
      files: { number: 1410, delta: 0 },
      suites: { number: 1410, delta: 0 },
      duration: { duration: 4913, delta: 118 },
      tests: { number: 10158, delta: -5 },
      tests_succ: { number: 10077, delta: -14 },
      tests_skip: { number: 69, delta: 2 },
      tests_fail: { number: 10, delta: 10 },
      tests_error: { number: 2, delta: 2 },
      runs: { number: 10633, delta: -5 },
      runs_succ: { number: 10550, delta: -14 },
      runs_skip: { number: 71, delta: 2 },
      runs_fail: { number: 10, delta: 10 },
      runs_error: { number: 2, delta: 2 },
      commit: 'ea899120000000000000000000000000',
      reference_commit: '36972410000000000000000000000000',
    },
    check_url: 'https://github.com/vaadin/flow/runs/12345',
  },
};

module.exports = { buildBody, post, MARKER, demoFixtures };

if (require.main === module) {
  const args = process.argv.slice(2);
  let data;
  if (args[0] === '--demo') {
    const which = args[1] || 'pass';
    if (!demoFixtures[which]) {
      console.error(`Unknown demo "${which}". Available: ${Object.keys(demoFixtures).join(', ')}`);
      process.exit(1);
    }
    data = demoFixtures[which];
  } else if (args[0]) {
    data = JSON.parse(require('fs').readFileSync(args[0], 'utf8'));
  } else {
    console.error('Usage:');
    console.error('  node postTestSummary.js --demo pass|fail');
    console.error('  node postTestSummary.js <results.json>');
    process.exit(1);
  }
  process.stdout.write(buildBody(data) + '\n');
}
