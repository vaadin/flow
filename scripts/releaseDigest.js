#!/usr/bin/env node
/**
 * Posts the "which Flow branches need a release" digest to Slack.
 *
 * Reads the `report.json` written by `checkReleases.js`, asks Claude whether
 * each branch's unreleased commits actually warrant a release, and renders the
 * result as plain text — the Slack Workflow Builder trigger it posts through
 * inserts a variable's value without parsing Slack markup.
 *
 * The model's recommendation is advisory. It is shown next to the verdict the
 * commit types alone give, and where the two disagree the digest says so rather
 * than hiding one behind the other, because a `fix:` the model reads as
 * test-only is exactly the call a human should confirm. Without
 * `ANTHROPIC_API_KEY`, or if the call fails, the digest still uses commit types
 * alone.
 *
 * The digest goes to stdout whether or not it is posted, so a run can be read
 * in the Actions job summary before any Slack target is configured. Everything
 * else the script says goes to stderr.
 *
 * Usage:
 *   ./scripts/releaseDigest.js [--in=release-check] [--no-post]
 *
 * Environment:
 *   ANTHROPIC_API_KEY   Enables the release-worthiness judgement (optional).
 *   SLACK_TRIGGER_URL   The Workflow Builder webhook trigger to post to.
 *   SLACK_TRIGGER_VARIABLE
 *                       The workflow's text variable name (default `digest`).
 */

const fs = require('fs');

const MODEL = 'claude-opus-5';
const MAX_COMMITS_SHOWN = 8;
// A Workflow Builder message step accepts 4000 characters.
const TEXT_LIMIT = 3800;

// Literal emoji rather than `:rocket:` shortcodes, which a variable's value is
// not guaranteed to be scanned for.
const VERDICT_ICON = { release: '🚀', consider: '🟡', skip: '⚪' };

function parseArgs(argv) {
  const opts = { inDir: 'release-check', post: true };
  for (const arg of argv) {
    if (arg.startsWith('--in=')) opts.inDir = arg.slice('--in='.length);
    else if (arg === '--no-post') opts.post = false;
  }
  return opts;
}

/**
 * One recommendation per branch. Structured outputs guarantee the enum and the
 * shape, so the renderer never has to defend against a malformed verdict.
 */
const RECOMMENDATION_SCHEMA = {
  type: 'object',
  additionalProperties: false,
  required: ['branches'],
  properties: {
    branches: {
      type: 'array',
      description: 'One entry per branch given, in the same order.',
      items: {
        type: 'object',
        additionalProperties: false,
        required: ['branch', 'recommendation', 'rationale'],
        properties: {
          branch: {
            type: 'string',
            description: 'The branch name, copied exactly from the input.',
          },
          recommendation: {
            type: 'string',
            enum: ['release', 'consider', 'skip'],
            description:
              'release: ship it now. consider: defensible either way, a human should decide. skip: nothing here is worth a release.',
          },
          rationale: {
            type: 'string',
            description:
              'One sentence, under 200 characters, naming the specific change that drives the recommendation. No preamble.',
          },
        },
      },
    },
  },
};

const SYSTEM_PROMPT = `You advise the Vaadin Flow team on which release branches are worth cutting a release for this week.

Flow ships patch releases from maintained branches (25.2, 24.10, ...) and pre-releases from main. A release costs real time: CI, smoke testing, release notes, and downstream platform work. Recommend one only when users get something from it.

Judge each branch on what its unreleased commits actually do:
- User-visible bug fixes and features are worth releasing.
- A security-relevant dependency bump is worth releasing.
- Routine dependency bumps are worth releasing in a batch, rarely on their own.
- Refactoring, test-only changes, CI and build changes, and release mechanics (version bumps, changelog edits) are not worth releasing on their own, whatever their commit type prefix says.

Commit messages are conventional-commit prefixed but the prefix is not always accurate. Read the subject, not just the type: a "fix:" that only touches tests or CI is not a user-visible fix. Older maintained branches serve users who upgrade cautiously, so the bar for disrupting them is higher.

Be decisive and specific. Name the change that drives your call rather than restating the counts.`;

/** Compact per-branch input — subjects carry the signal, hashes do not. */
function buildPrompt(branches) {
  const blocks = branches.map((b) => {
    const commits = b.commits.length
      ? b.commits.map((c) => `  - ${c.subject}`).join('\n')
      : '  (no commits since the last release)';
    const lastReleased = b.lastTag
      ? `${b.lastTag}${b.lastTagDate ? ` on ${b.lastTagDate}` : ''}`
      : 'never';
    return (
      `Branch ${b.branch} (last released ${lastReleased}, ` +
      `next would be ${b.next || 'unknown'})\n${commits}`
    );
  });
  return (
    'For each branch below, decide whether its unreleased commits warrant a release now.\n\n' +
    blocks.join('\n\n')
  );
}

/** Asks Claude to judge each branch; returns a Map keyed by branch name. */
async function judge(branches) {
  const { Anthropic } = require('@anthropic-ai/sdk');

  const response = await new Anthropic().messages.create({
    model: MODEL,
    max_tokens: 16000,
    system: SYSTEM_PROMPT,
    output_config: {
      effort: 'medium',
      format: { type: 'json_schema', schema: RECOMMENDATION_SCHEMA },
    },
    messages: [{ role: 'user', content: buildPrompt(branches) }],
  });

  const text = response.content
    .filter((block) => block.type === 'text')
    .map((block) => block.text)
    .join('');
  return new Map(JSON.parse(text).branches.map((entry) => [entry.branch, entry]));
}

/**
 * The digest as Slack will show it: literal emoji, bare URLs and indentation,
 * because a variable's value is inserted without `*bold*` or `<url|label>`
 * being parsed.
 *
 * A branch is only collapsed into the closing line when the commit types and
 * the model agree there is nothing to ship — otherwise a model-written "skip"
 * would bury both the feat/fix commits and the reasoning for dismissing them.
 * main always appears; an overlooked pre-release is the common failure.
 */
function buildDigest(report, adviceByBranch) {
  const verdictOf = (b) => adviceByBranch.get(b.branch)?.recommendation || b.verdict;
  const shown = report.branches.filter(
    (b) => b.branch === 'main' || verdictOf(b) !== 'skip' || b.verdict !== 'skip'
  );
  const quiet = report.branches.filter((b) => !shown.includes(b));
  const due = shown.filter((b) => verdictOf(b) === 'release').length;

  const lines = [
    due
      ? `Flow releases to do — ${due} branch${due === 1 ? '' : 'es'} ` +
        `look${due === 1 ? 's' : ''} ready to release (${report.generated})`
      : `Flow releases to do — nothing looks urgent this week (${report.generated})`,
  ];

  for (const b of shown) {
    const advice = adviceByBranch.get(b.branch);
    const counts = [
      [b.counts.releasable, 'feat/fix'],
      [b.counts.deps, 'deps'],
      [b.counts.internal, 'other'],
    ]
      .filter(([n]) => n > 0)
      .map(([n, label]) => `${n} ${label}`);

    lines.push('');
    lines.push(`${VERDICT_ICON[verdictOf(b)]} ${b.branch} → ${b.next || 'next version unknown'}`);
    const since = b.lastTag
      ? `${b.lastTag}${b.lastTagDate ? ` (${b.lastTagDate})` : ''}`
      : 'the start of the branch';
    lines.push(`    ${counts.join(', ') || 'nothing'} since ${since}`);
    if (advice) {
      lines.push(`    ${advice.rationale}`);
      if (advice.recommendation !== b.verdict) {
        lines.push(`    (commit types alone would say ${b.verdict})`);
      }
    }

    // Lead with the commits that justify a release; the rest are context.
    const commits = [
      ...b.commits.filter((c) => c.category === 'releasable'),
      ...b.commits.filter((c) => c.category !== 'releasable'),
    ];
    for (const c of commits.slice(0, MAX_COMMITS_SHOWN)) {
      lines.push(`    • ${c.subject}`);
    }
    if (commits.length > MAX_COMMITS_SHOWN) {
      lines.push(`    • …and ${commits.length - MAX_COMMITS_SHOWN} more`);
    }
    lines.push(`    ${b.compareUrl}`);
  }

  if (quiet.length) {
    lines.push('');
    lines.push(`${VERDICT_ICON.skip} Nothing to release on ${quiet.map((b) => b.branch).join(', ')}.`);
  }

  const length = () => lines.join('\n').length;
  if (length() > TEXT_LIMIT) {
    // Drop whole lines, so that no commit subject is left half-written.
    const note = '…truncated; the rest is in the workflow run.';
    while (length() + note.length + 1 > TEXT_LIMIT) lines.pop();
    lines.push(note);
  }
  return lines.join('\n');
}

/** Posts to the Workflow Builder trigger, which takes one text variable. */
async function post(digest) {
  const url = process.env.SLACK_TRIGGER_URL;
  if (!url) {
    throw new Error('Set SLACK_TRIGGER_URL to the Slack workflow trigger to post to');
  }

  const response = await fetch(url, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ [process.env.SLACK_TRIGGER_VARIABLE || 'digest']: digest }),
  });
  if (!response.ok) {
    throw new Error(`Slack returned ${response.status}: ${await response.text()}`);
  }
  // A payload whose keys do not match the workflow's variables is rejected in
  // the body, with a 200.
  const body = await response.json();
  if (!body.ok) {
    throw new Error(`Slack rejected the digest: ${body.error || 'unknown error'}`);
  }
}

async function main() {
  const opts = parseArgs(process.argv.slice(2));
  const report = JSON.parse(fs.readFileSync(`${opts.inDir}/report.json`, 'utf8'));

  let adviceByBranch = new Map();
  if (process.env.ANTHROPIC_API_KEY) {
    try {
      adviceByBranch = await judge(report.branches);
    } catch (e) {
      // A missing judgement costs nuance, not the digest.
      console.error(`Release-worthiness judgement unavailable (${e.message}); using commit types.`);
    }
  } else {
    console.error('No ANTHROPIC_API_KEY; judging by commit types alone.');
  }

  const digest = buildDigest(report, adviceByBranch);
  console.log(digest);

  if (opts.post) {
    await post(digest);
    console.error(`Posted the digest, covering ${report.branches.length} branches.`);
  }
}

main().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
