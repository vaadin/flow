#!/usr/bin/env node
/**
 * Rewrites the PR comment posted by EnricoMi/publish-unit-test-result-action
 * so it leads with a compact pass/time/delta summary and tucks the original
 * verbose table inside a <details> block.
 *
 * Expected env:
 *   TEST_RESULTS_JSON     EnricoMi step `json` output
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

function buildSummary(swd) {
  const failures = (swd.tests_fail?.number ?? 0) + (swd.tests_error?.number ?? 0);
  const passLine = failures > 0
    ? `:x: ${plural(failures, 'test')} failed`
    : ':white_check_mark: All tests pass';

  const current = swd.duration?.number ?? 0;
  const delta = swd.duration?.delta ?? 0;
  const reference = current - delta;
  const pct = reference > 0 ? Math.round((current / reference) * 100) : 100;
  const timeIcon = pct > TIME_WARN_THRESHOLD_PCT ? ':warning:' : ':white_check_mark:';
  const timeLine = `${timeIcon} ${pct}% of reference time spent (${formatDuration(current)})`;

  const testDelta = swd.tests?.delta ?? 0;
  let deltaLine;
  if (testDelta > 0) {
    deltaLine = `:white_check_mark: ${plural(testDelta, 'test')} added`;
  } else if (testDelta < 0) {
    deltaLine = `:warning: ${plural(-testDelta, 'test')} removed`;
  } else {
    deltaLine = ':white_check_mark: No tests added or removed';
  }

  return [passLine, timeLine, deltaLine];
}

function wrapBody(originalBody, summaryLines) {
  // EnricoMi always writes the heading as the first line. Strip it so we can
  // rebuild a body that still starts with the same heading (for re-discovery)
  // but inserts the summary between the heading and the full report.
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
  const raw = process.env.TEST_RESULTS_JSON;
  if (!raw) {
    console.log('TEST_RESULTS_JSON is empty; nothing to wrap.');
    return;
  }
  let json;
  try {
    json = JSON.parse(raw);
  } catch (err) {
    console.log(`TEST_RESULTS_JSON could not be parsed: ${err.message}`);
    return;
  }
  const swd = json.stats_with_delta;
  if (!swd) {
    console.log('No stats_with_delta in EnricoMi output; nothing to wrap.');
    return;
  }
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

  const summary = buildSummary(swd);
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
  formatDuration,
  buildSummary,
  wrapBody,
  isEnricoMiComment,
  TIME_WARN_THRESHOLD_PCT,
};
