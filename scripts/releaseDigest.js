#!/usr/bin/env node
/**
 * Posts the "which Flow branches need a release" digest to Slack.
 *
 * Reads the `report.json` written by `checkReleases.js`, asks Claude whether
 * each branch's unreleased commits actually warrant a release, and renders the
 * result as a Slack Block Kit message.
 *
 * The model's recommendation is advisory and shown next to the commit counts it
 * was derived from. Where it disagrees with the deterministic verdict the
 * disagreement is rendered explicitly rather than silently resolved, because a
 * `fix:` the model judges test-only is exactly the call a human should confirm.
 * Without `ANTHROPIC_API_KEY` — or if the call fails — the digest still posts
 * using the deterministic verdict alone.
 *
 * Usage:
 *   ./scripts/releaseDigest.js [--in=release-check] [--dry-run] [--no-ai] [--text]
 *                              [--no-post] [--save=<file>]
 *
 * `--no-post` renders without posting, and `--save` writes the plain-text
 * rendering to a file, so a run can be inspected — in the Actions job summary,
 * say — before or without any Slack target being configured.
 *
 * Environment:
 *   ANTHROPIC_API_KEY   Enables the release-worthiness judgement (optional).
 *   SLACK_TRIGGER_URL   Workflow Builder webhook trigger. Its "Send a message"
 *                       step inserts a variable as text and cannot render Block
 *                       Kit, so this target gets the plain-text digest.
 *   SLACK_TRIGGER_VARIABLE
 *                       The workflow's text variable name (default `digest`).
 *   SLACK_BOT_TOKEN     Bot token; posts via chat.postMessage to SLACK_CHANNEL.
 *   SLACK_CHANNEL       Channel id, e.g. C6RAXJATF for #flow-dev.
 *   SLACK_WEBHOOK_URL   Incoming webhook; used when no bot token is set.
 *   GITHUB_SERVER_URL / GITHUB_REPOSITORY / GITHUB_RUN_ID
 *                       Used to link the digest back to the workflow run.
 */

const fs = require('fs');

const MODEL = 'claude-opus-5';
const MAX_COMMITS_SHOWN = 8;
// Slack rejects a section whose text exceeds 3000 characters.
const SECTION_LIMIT = 2900;
// A Workflow Builder message step accepts 4000; leave room for the closing line.
const TEXT_LIMIT = 3800;

const VERDICT_EMOJI = {
  release: ':rocket:',
  consider: ':large_yellow_circle:',
  skip: ':white_circle:',
};

// Literal emoji rather than `:rocket:` shortcodes, because a workflow variable's
// value is not guaranteed to be scanned for them.
const VERDICT_ICON = {
  release: '\u{1F680}',
  consider: '\u{1F7E1}',
  skip: '\u{26AA}',
};

function parseArgs(argv) {
  const opts = {
    inDir: 'release-check',
    dryRun: false,
    ai: true,
    text: false,
    post: true,
    save: null,
  };
  for (const arg of argv) {
    if (arg.startsWith('--in=')) opts.inDir = arg.slice('--in='.length);
    else if (arg.startsWith('--save=')) opts.save = arg.slice('--save='.length);
    else if (arg === '--dry-run') opts.dryRun = true;
    else if (arg === '--no-ai') opts.ai = false;
    else if (arg === '--text') opts.text = true;
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
    const header =
      `Branch ${b.branch} (last released ${b.lastTag || 'never'}, ` +
      `next would be ${b.next || 'unknown'}${b.prerelease ? ', pre-release line' : ''})`;
    const commits = b.commits.length
      ? b.commits.map((c) => `  - ${c.subject}`).join('\n')
      : '  (no commits since the last release)';
    return `${header}\n${commits}`;
  });
  return (
    'For each branch below, decide whether its unreleased commits warrant a release now.\n\n' +
    blocks.join('\n\n')
  );
}

/** Asks Claude to judge each branch; returns a Map, empty if unavailable. */
async function judge(branches) {
  const { Anthropic } = require('@anthropic-ai/sdk');
  const client = new Anthropic();

  const response = await client.messages.create({
    model: MODEL,
    max_tokens: 16000,
    system: SYSTEM_PROMPT,
    output_config: {
      effort: 'medium',
      format: { type: 'json_schema', schema: RECOMMENDATION_SCHEMA },
    },
    messages: [{ role: 'user', content: buildPrompt(branches) }],
  });

  if (response.stop_reason === 'refusal') {
    throw new Error('the model declined to answer');
  }
  if (response.stop_reason === 'max_tokens') {
    throw new Error('the response was truncated before the JSON was complete');
  }

  const text = response.content
    .filter((block) => block.type === 'text')
    .map((block) => block.text)
    .join('');
  const parsed = JSON.parse(text);
  return new Map(parsed.branches.map((entry) => [entry.branch, entry]));
}

function escape(text) {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

/** Commits that justify a release first; the rest are context. */
function orderedCommits(b) {
  return [
    ...b.commits.filter((c) => c.category === 'releasable'),
    ...b.commits.filter((c) => c.category !== 'releasable'),
  ];
}

/**
 * What both renderings show, and in what order.
 *
 * A branch is only collapsed into the closing line when the commit types and
 * the model agree there is nothing to ship — otherwise a model-written "skip"
 * would bury both the feat/fix commits and the reasoning for dismissing them.
 * main always appears; an overlooked pre-release is the common failure.
 */
function selectBranches(report, adviceByBranch) {
  const verdictOf = (b) => {
    const advice = adviceByBranch.get(b.branch);
    return advice ? advice.recommendation : b.verdict;
  };
  const shown = report.branches.filter(
    (b) => b.branch === 'main' || verdictOf(b) !== 'skip' || b.verdict !== 'skip'
  );
  const quiet = report.branches.filter((b) => !shown.includes(b));
  const dueCount = shown.filter((b) => verdictOf(b) === 'release').length;
  return { shown, quiet, dueCount, verdictOf };
}

/** The Slack section for one branch: what to release, why, and what changed. */
function branchSection(b, advice) {
  const verdict = advice ? advice.recommendation : b.verdict;
  const target = b.next ? `\`${b.next}\`` : 'next version unknown';
  const lines = [];

  lines.push(`${VERDICT_EMOJI[verdict]}  *${escape(b.branch)}* → ${target}`);

  const parts = [`since \`${b.lastTag || 'start of branch'}\``];
  if (b.counts.releasable) parts.push(`*${b.counts.releasable}* feat/fix`);
  if (b.counts.deps) parts.push(`${b.counts.deps} deps`);
  if (b.counts.internal) parts.push(`${b.counts.internal} other`);
  lines.push(parts.join(' · '));

  if (advice) {
    lines.push(`_${escape(advice.rationale)}_`);
    if (advice.recommendation !== b.verdict) {
      lines.push(`:information_source: commit types alone would say *${b.verdict}*.`);
    }
  }

  const ordered = orderedCommits(b);
  for (const c of ordered.slice(0, MAX_COMMITS_SHOWN)) {
    lines.push(`• ${escape(c.subject)}`);
  }
  const hidden = ordered.length - MAX_COMMITS_SHOWN;
  if (hidden > 0) {
    lines.push(`• _…and ${hidden} more_`);
  }
  lines.push(`<${b.compareUrl}|Full diff on GitHub>`);

  let text = lines.join('\n');
  if (text.length > SECTION_LIMIT) {
    text = `${text.slice(0, SECTION_LIMIT)}\n_(truncated)_`;
  }
  return { type: 'section', text: { type: 'mrkdwn', text } };
}

function buildMessage(report, adviceByBranch) {
  const { shown, quiet, dueCount, verdictOf } = selectBranches(report, adviceByBranch);

  const blocks = [
    {
      type: 'header',
      text: { type: 'plain_text', text: 'Flow releases to do', emoji: true },
    },
  ];

  blocks.push({
    type: 'context',
    elements: [
      {
        type: 'mrkdwn',
        text: dueCount
          ? `*${dueCount}* branch${dueCount === 1 ? '' : 'es'} look${dueCount === 1 ? 's' : ''} ready to release · ${report.generated}`
          : `Nothing looks urgent this week · ${report.generated}`,
      },
    ],
  });
  blocks.push({ type: 'divider' });

  for (const b of shown) {
    blocks.push(branchSection(b, adviceByBranch.get(b.branch)));
  }

  if (quiet.length) {
    blocks.push({
      type: 'context',
      elements: [
        {
          type: 'mrkdwn',
          text: `Nothing to release on ${quiet.map((b) => `\`${b.branch}\``).join(', ')}.`,
        },
      ],
    });
  }

  const { GITHUB_SERVER_URL, GITHUB_REPOSITORY, GITHUB_RUN_ID } = process.env;
  if (GITHUB_SERVER_URL && GITHUB_REPOSITORY && GITHUB_RUN_ID) {
    blocks.push({
      type: 'context',
      elements: [
        {
          type: 'mrkdwn',
          text: `<${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}|Workflow run> · branches come from \`validation.yml\``,
        },
      ],
    });
  }

  return {
    // Shown in notifications and by clients that cannot render blocks.
    text: dueCount
      ? `Flow releases to do: ${shown
          .filter((b) => verdictOf(b) === 'release')
          .map((b) => b.next || b.branch)
          .join(', ')}`
      : 'Flow releases to do: nothing urgent this week',
    blocks,
  };
}

/**
 * Plain-text rendering for a Workflow Builder trigger, whose message step
 * inserts a variable's value without parsing Slack markup: literal emoji, bare
 * URLs and indentation survive that, `*bold*` and `<url|label>` would show
 * their own syntax instead.
 */
function buildText(report, adviceByBranch) {
  const { shown, quiet, dueCount, verdictOf } = selectBranches(report, adviceByBranch);
  const lines = [];

  lines.push(
    dueCount
      ? `Flow releases to do \u2014 ${dueCount} branch${dueCount === 1 ? '' : 'es'} ` +
          `look${dueCount === 1 ? 's' : ''} ready to release (${report.generated})`
      : `Flow releases to do \u2014 nothing looks urgent this week (${report.generated})`
  );

  for (const b of shown) {
    const advice = adviceByBranch.get(b.branch);
    lines.push('');
    lines.push(
      `${VERDICT_ICON[verdictOf(b)]} ${b.branch} \u2192 ${b.next || 'next version unknown'}`
    );

    const counts = [];
    if (b.counts.releasable) counts.push(`${b.counts.releasable} feat/fix`);
    if (b.counts.deps) counts.push(`${b.counts.deps} deps`);
    if (b.counts.internal) counts.push(`${b.counts.internal} other`);
    lines.push(
      `    ${counts.length ? counts.join(', ') : 'nothing'} since ` +
        `${b.lastTag || 'the start of the branch'}`
    );

    if (advice) {
      lines.push(`    ${advice.rationale}`);
      if (advice.recommendation !== b.verdict) {
        lines.push(`    (commit types alone would say ${b.verdict})`);
      }
    }

    const ordered = orderedCommits(b);
    for (const c of ordered.slice(0, MAX_COMMITS_SHOWN)) {
      lines.push(`    \u2022 ${c.subject}`);
    }
    const hidden = ordered.length - MAX_COMMITS_SHOWN;
    if (hidden > 0) {
      lines.push(`    \u2022 \u2026and ${hidden} more`);
    }
    lines.push(`    ${b.compareUrl}`);
  }

  if (quiet.length) {
    lines.push('');
    lines.push(`${VERDICT_ICON.skip} Nothing to release on ${quiet.map((b) => b.branch).join(', ')}.`);
  }

  const text = lines.join('\n');
  if (text.length <= TEXT_LIMIT) return text;
  // Cut at a line boundary so no commit subject is left half-written.
  const cut = text.lastIndexOf('\n', TEXT_LIMIT);
  return `${text.slice(0, cut > 0 ? cut : TEXT_LIMIT)}\n\u2026truncated; see the workflow run for the rest.`;
}

/**
 * Posts to the first target configured: a Workflow Builder trigger, which takes
 * the plain text, or a bot token or incoming webhook, which take Block Kit.
 */
async function post(report, adviceByBranch) {
  const { SLACK_TRIGGER_URL, SLACK_BOT_TOKEN, SLACK_CHANNEL, SLACK_WEBHOOK_URL } = process.env;

  if (SLACK_TRIGGER_URL) {
    const variable = process.env.SLACK_TRIGGER_VARIABLE || 'digest';
    const response = await fetch(SLACK_TRIGGER_URL, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ [variable]: buildText(report, adviceByBranch) }),
    });
    // An unknown or unpublished trigger fails by status code; a payload whose
    // keys do not match the workflow's variables fails in the body.
    const body = await response.text();
    if (!response.ok) {
      throw new Error(`Slack trigger returned ${response.status}: ${body}`);
    }
    let parsed = null;
    try {
      parsed = JSON.parse(body);
    } catch {
      // A non-JSON 200 is a success the trigger did not bother to describe.
    }
    if (parsed && parsed.ok === false) {
      throw new Error(`Slack rejected the trigger payload: ${parsed.error || body}`);
    }
    return;
  }

  const message = buildMessage(report, adviceByBranch);

  if (SLACK_BOT_TOKEN) {
    if (!SLACK_CHANNEL) {
      throw new Error('SLACK_BOT_TOKEN is set but SLACK_CHANNEL is missing');
    }
    const response = await fetch('https://slack.com/api/chat.postMessage', {
      method: 'POST',
      headers: {
        authorization: `Bearer ${SLACK_BOT_TOKEN}`,
        'content-type': 'application/json; charset=utf-8',
      },
      body: JSON.stringify({ channel: SLACK_CHANNEL, ...message }),
    });
    // chat.postMessage reports failures in the body, not the status code.
    const body = await response.json();
    if (!body.ok) {
      throw new Error(`Slack rejected the message: ${body.error}`);
    }
    return;
  }

  if (!SLACK_WEBHOOK_URL) {
    throw new Error(
      'Set SLACK_TRIGGER_URL, SLACK_WEBHOOK_URL, or SLACK_BOT_TOKEN with SLACK_CHANNEL'
    );
  }
  const response = await fetch(SLACK_WEBHOOK_URL, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(message),
  });
  if (!response.ok) {
    throw new Error(`Slack webhook returned ${response.status}: ${await response.text()}`);
  }
}

async function main() {
  const opts = parseArgs(process.argv.slice(2));
  const report = JSON.parse(fs.readFileSync(`${opts.inDir}/report.json`, 'utf8'));

  let adviceByBranch = new Map();
  if (opts.ai && process.env.ANTHROPIC_API_KEY) {
    try {
      adviceByBranch = await judge(report.branches);
    } catch (e) {
      // A missing judgement costs nuance, not the digest.
      console.warn(`Release-worthiness judgement unavailable (${e.message}); using commit types.`);
    }
  } else {
    console.log('Skipping release-worthiness judgement; using commit types.');
  }

  // Saved for the job summary: the text a trigger target receives verbatim, and
  // a faithful reading of what a Block Kit target receives.
  if (opts.save) {
    fs.writeFileSync(opts.save, buildText(report, adviceByBranch) + '\n');
    console.log(`Wrote the digest to ${opts.save}.`);
  }

  // A trigger target gets plain text; --text previews it without one set.
  if (opts.dryRun) {
    const asText = opts.text || Boolean(process.env.SLACK_TRIGGER_URL);
    console.log(
      asText
        ? buildText(report, adviceByBranch)
        : JSON.stringify(buildMessage(report, adviceByBranch), null, 2)
    );
    return;
  }
  if (!opts.post) {
    console.log('Not posting (--no-post).');
    return;
  }
  await post(report, adviceByBranch);
  console.log(`Posted digest covering ${report.branches.length} branches.`);
}

if (require.main === module) {
  main().catch((e) => {
    console.error(e.message);
    process.exit(1);
  });
} else {
  module.exports = { buildPrompt, buildMessage, buildText, RECOMMENDATION_SCHEMA };
}
