#!/usr/bin/env node
/**
 * Rewrites the PR comment posted by EnricoMi/publish-unit-test-result-action
 * so it leads with a compact pass/time/delta summary and tucks the original
 * verbose table inside a <details> block.
 *
 * Counts and deltas are parsed straight from the rendered comment body. The
 * action's `json` step output does NOT include base-commit deltas (only an
 * "earlier commit" comparison, which is empty on first pushes), but the
 * rendered comment text always carries them in a stable format.
 *
 * Expected env:
 *   GITHUB_TOKEN          token with pull-requests:write
 *   GITHUB_REPOSITORY     owner/repo
 *   PR_NUMBER             pull request number
 *
 * EnricoMi locates its previous comment by matching `## Test Results\n` at the
 * start of the body and `\nResults for commit ` somewhere inside, so both
 * markers must survive the rewrite for re-runs to keep updating in place.
 */

const TIME_WARN_THRESHOLD_PCT = 110;
const COMMENT_HEADING = '## Test Results';
const STOPWATCH = '⏱️'; // ⏱️
const FAIL_LABEL = '❌'; // ❌
const ERROR_LABEL = '\u{1F525}'; // 🔥

// EnricoMi pads numbers with U+2007 (figure space) and groups thousands with
// U+2008 (punctuation space). Treat both as no-ops when collecting digits.
function parseInteger(text) {
  const digits = text.replace(/[^\d-]/g, '');
  if (!digits) return null;
  return parseInt(digits, 10);
}

// Delta tokens come in three shapes (see as_delta in publish/__init__.py):
//   "±0"  -> 0
//   "+N"  -> +N
//   " - N" -> -N   (leading space, dash, space, then digits)
function parseDelta(text) {
  const t = text.trim();
  if (!t) return null;
  if (t.startsWith('±')) return parseInteger(t.slice(1)) ?? 0;
  if (t.startsWith('+')) return parseInteger(t.slice(1));
  if (t.startsWith('-')) {
    const n = parseInteger(t.slice(1));
    return n == null ? null : -n;
  }
  return null;
}

function parseDurationSeconds(text) {
  let total = 0;
  const m = text.matchAll(/(\d+)\s*([dhms])/g);
  for (const [, n, unit] of m) {
    const value = parseInt(n, 10);
    if (unit === 'd') total += value * 86400;
    else if (unit === 'h') total += value * 3600;
    else if (unit === 'm') total += value * 60;
    else total += value;
  }
  return total;
}

// Duration line example:
//   " 1 420 files  ±0   1 420 suites  ±0   1h 18m 52s ⏱️ - 4m 52s"
// The current duration is the time-token sequence immediately before ⏱️; the
// delta follows it (sign + value, or "±0s" when zero — see as_stat_duration).
function parseDurationLine(body) {
  const idx = body.indexOf(STOPWATCH);
  if (idx < 0) return null;
  const before = body.slice(0, idx);
  const after = body.slice(idx + STOPWATCH.length).split('\n', 1)[0];

  const durMatch = before.match(/((?:\d+\s*[dhms]\s*)+)\s*$/);
  if (!durMatch) return null;
  const current = parseDurationSeconds(durMatch[1]);

  const trimmedAfter = after.trim();
  if (!trimmedAfter) return { current, delta: 0 };
  if (trimmedAfter.startsWith('±')) return { current, delta: 0 };
  const signMatch = trimmedAfter.match(/^([+\-])\s*(.+)/);
  if (!signMatch) return { current, delta: 0 };
  const sign = signMatch[1] === '+' ? 1 : -1;
  return { current, delta: sign * parseDurationSeconds(signMatch[2]) };
}

// Inside a number, EnricoMi only uses U+2007 (figure-space padding) and U+2008
// (punctuation-space thousands grouping). Field separators are different
// whitespace runs (U+2002, U+2003, etc.), so a digit class excluding regular
// space is what keeps a delta capture from sliding into the next field.
const NUM_DIGIT = '[\\d\\u2007\\u2008]';

function signedValue(sign, value) {
  if (value == null) return null;
  if (sign === '+') return value;
  if (sign === '-') return -value;
  return 0; // ±
}

// Tests line example:
//   " 9 999 tests ±0   9 931 ✅ ±0  68 💤 ±0  0 ❌ ±0  3 🔥 +1"
// We need only the `tests` count delta and the count of failures + errors.
function parseTestsLine(body) {
  const lineMatch = body.match(/^[^\n]*\btests\b[^\n]*$/m);
  if (!lineMatch) return null;
  const line = lineMatch[0];

  const testsDeltaMatch = line.match(new RegExp(`tests\\s+([\\u00b1+\\-])\\s*(${NUM_DIGIT}+)`));
  if (!testsDeltaMatch) return null;
  const testsDelta = signedValue(testsDeltaMatch[1], parseInteger(testsDeltaMatch[2]));
  if (testsDelta == null) return null;

  const failCount = line.match(new RegExp(`(${NUM_DIGIT}+)\\s+${FAIL_LABEL}`));
  const errorCount = line.match(new RegExp(`(${NUM_DIGIT}+)\\s+${ERROR_LABEL}`));
  const failures = (failCount ? parseInteger(failCount[1]) ?? 0 : 0)
    + (errorCount ? parseInteger(errorCount[1]) ?? 0 : 0);

  return { failures, testsDelta };
}

function parseStats(body) {
  const duration = parseDurationLine(body);
  const tests = parseTestsLine(body);
  if (!duration || !tests) return null;
  return {
    duration: duration.current,
    durationDelta: duration.delta,
    failures: tests.failures,
    testsDelta: tests.testsDelta,
  };
}

function formatDuration(seconds) {
  const total = Math.max(0, Math.round(seconds));
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  const parts = [];
  if (h) parts.push(`${h}h`);
  if (m) parts.push(`${m}m`);
  if (s || parts.length === 0) parts.push(`${s}s`);
  return parts.join(' ');
}

function plural(n, word) {
  return `${n} ${word}${n === 1 ? '' : 's'}`;
}

function buildSummary(stats) {
  const passLine = stats.failures > 0
    ? `:x: ${plural(stats.failures, 'test')} failed`
    : ':white_check_mark: All tests pass';

  const reference = stats.duration - stats.durationDelta;
  const pct = reference > 0 ? Math.round((stats.duration / reference) * 100) : 100;
  // Use >= so the warning matches the displayed (rounded) percentage. With
  // strict >, a 109.7% run displays as "110%" yet stays green, which reads as
  // a UI bug to anyone seeing the threshold.
  const timeIcon = pct >= TIME_WARN_THRESHOLD_PCT ? ':warning:' : ':white_check_mark:';
  const timeLine = `${timeIcon} ${pct}% of reference time spent (${formatDuration(stats.duration)})`;

  let deltaLine;
  if (stats.testsDelta > 0) {
    deltaLine = `:white_check_mark: ${plural(stats.testsDelta, 'test')} added`;
  } else if (stats.testsDelta < 0) {
    deltaLine = `:warning: ${plural(-stats.testsDelta, 'test')} removed`;
  } else {
    deltaLine = ':white_check_mark: No tests added or removed';
  }

  return [passLine, timeLine, deltaLine];
}

function wrapBody(originalBody, summaryLines) {
  const headingPrefix = `${COMMENT_HEADING}\n`;
  let rest = originalBody.startsWith(headingPrefix)
    ? originalBody.slice(headingPrefix.length)
    : originalBody;
  rest = rest.replace(/^\n+/, '');

  return [
    COMMENT_HEADING,
    '',
    ...summaryLines,
    '',
    '<details>',
    '<summary>Full report</summary>',
    '',
    rest,
    '</details>',
    '',
  ].join('\n');
}

function isEnricoMiComment(body) {
  return typeof body === 'string'
    && body.startsWith(`${COMMENT_HEADING}\n`)
    && /\n[Rr]esults for commit /.test(body);
}

async function ghRequest(token, method, path, body) {
  const res = await fetch(`https://api.github.com${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'application/vnd.github+json',
      'X-GitHub-Api-Version': '2022-11-28',
      'User-Agent': 'flow-wrap-test-results',
      ...(body ? { 'Content-Type': 'application/json' } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`GitHub ${method} ${path} failed: ${res.status} ${text}`);
  }
  return res.json();
}

async function findEnricoMiComment(token, repo, prNumber) {
  let page = 1;
  let latest = null;
  while (true) {
    const batch = await ghRequest(
      token,
      'GET',
      `/repos/${repo}/issues/${prNumber}/comments?per_page=100&page=${page}`,
    );
    if (!Array.isArray(batch) || batch.length === 0) break;
    for (const c of batch) {
      if (isEnricoMiComment(c.body)) latest = c;
    }
    if (batch.length < 100) break;
    page += 1;
  }
  return latest;
}

async function main() {
  const token = process.env.GITHUB_TOKEN;
  const repo = process.env.GITHUB_REPOSITORY;
  const prNumber = process.env.PR_NUMBER;
  if (!token || !repo || !prNumber) {
    throw new Error('GITHUB_TOKEN, GITHUB_REPOSITORY and PR_NUMBER are required');
  }

  const comment = await findEnricoMiComment(token, repo, prNumber);
  if (!comment) {
    console.log('No EnricoMi comment found on this PR; nothing to wrap.');
    return;
  }

  const stats = parseStats(comment.body);
  if (!stats) {
    console.log(`Could not parse stats from comment ${comment.id}; skipping wrap.`);
    return;
  }

  const summary = buildSummary(stats);
  const newBody = wrapBody(comment.body, summary);
  if (newBody === comment.body) {
    console.log(`Comment ${comment.id} already wrapped; nothing to do.`);
    return;
  }
  await ghRequest(token, 'PATCH', `/repos/${repo}/issues/comments/${comment.id}`, {
    body: newBody,
  });
  console.log(`Wrapped comment ${comment.id}.`);
}

if (require.main === module) {
  main().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}

module.exports = {
  parseStats,
  parseDurationLine,
  parseTestsLine,
  parseDelta,
  parseDurationSeconds,
  formatDuration,
  buildSummary,
  wrapBody,
  isEnricoMiComment,
  TIME_WARN_THRESHOLD_PCT,
};
