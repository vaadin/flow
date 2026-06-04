#!/usr/bin/env node
/**
 * Reports which maintained branches have unreleased changes worth a patch
 * release, producing one issue body per branch so releases can be tracked and
 * assigned individually.
 *
 * The maintained branches are read from `.github/workflows/validation.yml`
 * (the `on.push.branches` list) so this stays in sync with CI and there is no
 * second list to keep up to date.
 *
 * For each branch it finds the latest release tag on that version line and
 * lists the `feat:` and `fix:` commits merged since then. `chore:`,
 * `refactor:` and other prefixes are ignored because they don't, on their own,
 * justify a release.
 *
 * Usage:
 *   ./scripts/checkReleases.js [--out=release-check] [--no-fetch]
 *
 * Outputs, under the out directory (default `release-check`):
 *   - `<branch>.md`   Issue body for each branch with pending changes.
 *   - `manifest.json` { pending: [{branch,title,next,count,body}], clean: [branch] }
 *                     The workflow uses this to upsert one issue per pending
 *                     branch and to close issues for branches now up to date.
 *   - `summary.md`    Combined overview for the Actions job summary.
 *
 * The script fetches the maintained branches and tags itself, so the workflow
 * only needs a shallow checkout.
 */

const fs = require('fs');
const { execFileSync } = require('child_process');

const REMOTE = process.env.RELEASE_CHECK_REMOTE || 'origin';
const VALIDATION_YML = '.github/workflows/validation.yml';

function git(args) {
  return execFileSync('git', args, { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
}

function parseArgs(argv) {
  const opts = { outDir: 'release-check', fetch: true };
  for (const arg of argv) {
    if (arg.startsWith('--out=')) opts.outDir = arg.slice('--out='.length);
    else if (arg === '--no-fetch') opts.fetch = false;
  }
  return opts;
}

/**
 * Extracts the maintained branch list from the `on.push.branches` entry of
 * validation.yml. Done with a focused regex to avoid a YAML dependency, but
 * anchored to the `push:` block so it doesn't pick up `pull_request.branches`.
 */
function readMaintainedBranches() {
  const yml = fs.readFileSync(VALIDATION_YML, 'utf8');
  const pushMatch = yml.match(/\bpush:\s*\n(?:[^\n]*\n)*?\s*branches:\s*\[([^\]]*)\]/);
  if (!pushMatch) {
    throw new Error(`Could not find on.push.branches in ${VALIDATION_YML}`);
  }
  return pushMatch[1]
    .split(',')
    .map((s) => s.trim().replace(/^['"]|['"]$/g, ''))
    .filter(Boolean);
}

/** Parses a tag like `24.9.19` or `25.2.0-beta1` into a comparable structure. */
function parseVersion(tag) {
  const m = tag.match(/^(\d+)\.(\d+)\.(\d+)(?:[.-]([0-9A-Za-z.-]+))?$/);
  if (!m) return null;
  return {
    tag,
    major: Number(m[1]),
    minor: Number(m[2]),
    patch: Number(m[3]),
    pre: m[4] || null, // e.g. "beta1", "alpha13", "rc1"
  };
}

/** Semver-style comparison where a release outranks its pre-releases. */
function compareVersions(a, b) {
  if (a.major !== b.major) return a.major - b.major;
  if (a.minor !== b.minor) return a.minor - b.minor;
  if (a.patch !== b.patch) return a.patch - b.patch;
  if (a.pre === b.pre) return 0;
  if (!a.pre) return 1; // final release > pre-release
  if (!b.pre) return -1;
  return a.pre < b.pre ? -1 : 1; // lexical is good enough for alpha/beta/rc
}

function fetchRefs(branches) {
  const refspecs = branches.map((b) => `+${b}:refs/remotes/${REMOTE}/${b}`);
  // Tags first (so the version line is fully known), then the branch tips.
  git(['fetch', '--quiet', '--tags', REMOTE, ...refspecs]);
}

/** Latest tag (release or pre-release) on a branch's version line, or null. */
function latestTagFor(branch) {
  const prefix = `${branch}.`;
  const versions = git(['tag', '--list', `${prefix}*`])
    .split('\n')
    .map((t) => t.trim())
    .filter(Boolean)
    .map(parseVersion)
    .filter(Boolean);
  if (!versions.length) return null;
  versions.sort(compareVersions);
  return versions[versions.length - 1];
}

/** The patch version that the pending changes would be released as. */
function suggestNextVersion(version) {
  if (!version) return null;
  // Don't guess the next pre-release identifier; only suggest for finals.
  if (version.pre) return null;
  return `${version.major}.${version.minor}.${version.patch + 1}`;
}

const RELEASABLE = /^(feat|fix)(\([^)]*\))?!?:\s/i;

/** feat:/fix: commit subjects merged since `tag` on the branch. */
function pendingCommits(tag, branch) {
  const range = tag ? `${tag}..${REMOTE}/${branch}` : `${REMOTE}/${branch}`;
  const lines = git(['log', '--no-merges', '--format=%h\t%s', range])
    .split('\n')
    .filter(Boolean);
  return lines
    .map((line) => {
      const tab = line.indexOf('\t');
      return { sha: line.slice(0, tab), subject: line.slice(tab + 1) };
    })
    .filter((c) => RELEASABLE.test(c.subject));
}

/**
 * The issue title is intentionally branch-only (no version) so it is stable
 * across release cycles. That gives the workflow a reliable key to find and
 * update the existing open issue for a branch instead of opening duplicates,
 * which keeps any assignee attached. The target version lives in the body.
 */
function issueTitle(branch) {
  return `Release pending: ${branch}`;
}

/** Issue body for a single branch's pending release. */
function branchBody(r, now) {
  const target = r.next ? `\`${r.next}\`` : 'the next pre-release';
  const n = r.commits.length;
  const lines = [];
  lines.push(`Branch \`${r.branch}\` has **${n}** unreleased \`feat:\`/\`fix:\` commit${n === 1 ? '' : 's'} since \`${r.lastTag || 'the start of the branch'}\`.`);
  lines.push('');
  lines.push(`Suggested next release: ${target}`);
  lines.push('');
  lines.push('### Changes to release');
  lines.push('');
  for (const c of r.commits) {
    lines.push(`- ${c.subject} (${c.sha})`);
  }
  lines.push('');
  lines.push('---');
  lines.push(
    `_Auto-generated by \`scripts/checkReleases.js\` from \`${VALIDATION_YML}\`. ` +
      `Last updated ${now}. Assign this issue to whoever owns the \`${r.branch}\` release._`
  );
  return lines.join('\n');
}

/** Combined overview written to the Actions job summary. */
function buildSummary(results, now) {
  const pending = results.filter((r) => r.commits.length > 0);
  const lines = [];
  lines.push('## Maintained branch release check');
  lines.push('');
  lines.push(`_Generated ${now} from \`${VALIDATION_YML}\` maintained branches._`);
  lines.push('');
  if (!pending.length) {
    lines.push('No maintained branch has unreleased `feat:`/`fix:` changes. 🎉');
    return lines.join('\n');
  }
  lines.push('One issue is opened/updated per branch below.');
  lines.push('');
  lines.push('| Branch | Last release | Next | feat/fix commits |');
  lines.push('| --- | --- | --- | --- |');
  for (const r of pending) {
    lines.push(`| \`${r.branch}\` | ${r.lastTag || '—'} | ${r.next || '—'} | ${r.commits.length} |`);
  }
  const clean = results.filter((r) => r.commits.length === 0);
  if (clean.length) {
    lines.push('');
    lines.push(`Up to date: ${clean.map((r) => `\`${r.branch}\``).join(', ')}.`);
  }
  return lines.join('\n');
}

function main() {
  const opts = parseArgs(process.argv.slice(2));
  const branches = readMaintainedBranches();
  if (opts.fetch) {
    fetchRefs(branches);
  }

  const now = new Date().toISOString().slice(0, 10);
  const results = branches.map((branch) => {
    const version = latestTagFor(branch);
    const commits = pendingCommits(version && version.tag, branch);
    return {
      branch,
      lastTag: version && version.tag,
      next: suggestNextVersion(version),
      commits,
    };
  });

  fs.mkdirSync(opts.outDir, { recursive: true });

  const pending = [];
  for (const r of results) {
    if (r.commits.length === 0) continue;
    const bodyFile = `${opts.outDir}/${r.branch}.md`;
    fs.writeFileSync(bodyFile, branchBody(r, now) + '\n');
    pending.push({
      branch: r.branch,
      title: issueTitle(r.branch),
      next: r.next,
      count: r.commits.length,
      body: bodyFile,
    });
  }
  const clean = results.filter((r) => r.commits.length === 0).map((r) => issueTitle(r.branch));

  fs.writeFileSync(
    `${opts.outDir}/manifest.json`,
    JSON.stringify({ pending, clean }, null, 2) + '\n'
  );
  fs.writeFileSync(`${opts.outDir}/summary.md`, buildSummary(results, now) + '\n');

  console.log(
    pending.length
      ? `Pending releases on: ${pending.map((p) => p.next || p.branch).join(', ')}`
      : 'No pending patch releases'
  );
  for (const r of results) {
    console.log(`  ${r.branch}: ${r.commits.length} pending (last ${r.lastTag || 'none'})`);
  }

  if (process.env.GITHUB_OUTPUT) {
    fs.appendFileSync(process.env.GITHUB_OUTPUT, `has_pending=${pending.length > 0}\n`);
  }
}

main();
