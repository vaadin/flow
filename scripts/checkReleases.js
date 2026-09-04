#!/usr/bin/env node
/**
 * Reports which branches have unreleased changes worth a release, producing one
 * issue body per branch so releases can be tracked and assigned individually,
 * plus a structured report the Slack digest renders from.
 *
 * The maintained branches are read from `.github/workflows/validation.yml`
 * (the `on.push.branches` list) so this stays in sync with CI and there is no
 * second list to keep up to date. `--include-main` adds `main`, whose version
 * line comes from its POM version rather than its branch name because
 * pre-releases (`25.3.0-alphaN`) are cut from it.
 *
 * For each branch it finds the latest tag on that version line and classifies
 * every commit merged since then by its conventional-commit type, into the
 * verdict `release`, `consider` or `skip`.
 *
 * Usage:
 *   ./scripts/checkReleases.js [--out=release-check] [--no-fetch] [--include-main]
 *
 * Outputs, under the out directory (default `release-check`):
 *   - `<branch>.md`   Issue body for each branch with pending changes.
 *   - `manifest.json` { pending: [{branch,title,next,count,body}], clean: [branch] }
 *                     The workflow uses this to upsert one issue per pending
 *                     branch and to close issues for branches now up to date.
 *   - `report.json`   Full per-branch classification, consumed by the digest.
 *   - `summary.md`    Combined overview for the Actions job summary.
 *
 * `manifest.json` and the issue bodies stay about maintained-branch patch
 * releases: `main` is left out, and only the commits classified as releasable
 * are counted.
 *
 * The script fetches the branches and tags itself, so the workflow only needs a
 * shallow checkout.
 */

const fs = require('fs');
const { execFileSync } = require('child_process');

const REMOTE = process.env.RELEASE_CHECK_REMOTE || 'origin';
const VALIDATION_YML = '.github/workflows/validation.yml';
const MAIN_BRANCH = 'main';
const REPO_URL = 'https://github.com/vaadin/flow';

function git(args) {
  return execFileSync('git', args, { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
}

function parseArgs(argv) {
  const opts = { outDir: 'release-check', fetch: true, includeMain: false };
  for (const arg of argv) {
    if (arg.startsWith('--out=')) opts.outDir = arg.slice('--out='.length);
    else if (arg === '--no-fetch') opts.fetch = false;
    else if (arg === '--include-main') opts.includeMain = true;
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

/**
 * The version line `main` is currently building, taken from the `flow-project`
 * POM version (`25.3-SNAPSHOT` -> `25.3`). Unlike a maintained branch, `main`
 * is not named after its line, so its tags cannot be found from the branch
 * name alone.
 */
function readMainVersionLine() {
  const pom = git(['show', `${REMOTE}/${MAIN_BRANCH}:pom.xml`]);
  const m = pom.match(
    /<artifactId>flow-project<\/artifactId>\s*<version>(\d+\.\d+)[^<]*<\/version>/
  );
  if (!m) {
    throw new Error(`Could not read the flow-project version from ${MAIN_BRANCH}'s pom.xml`);
  }
  return m[1];
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

/** The date (YYYY-MM-DD) of the commit a tag points to, or null. */
function tagDate(tag) {
  if (!tag) return null;
  const date = git(['log', '-1', '--format=%as', tag]).trim();
  return date || null;
}

/** Latest tag (release or pre-release) on a version line, or null. */
function latestTagFor(line) {
  const prefix = `${line}.`;
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

/**
 * The version the pending changes would be released as: the next patch for a
 * final release, or the next iteration of the same pre-release series so `main`
 * gets a concrete `25.3.0-alpha8` rather than a placeholder.
 */
function suggestNextVersion(version) {
  if (!version) return null;
  if (!version.pre) {
    return `${version.major}.${version.minor}.${version.patch + 1}`;
  }
  const series = version.pre.match(/^(.*?)(\d+)$/);
  // An unnumbered pre-release identifier has no obvious successor.
  if (!series) return null;
  return `${version.major}.${version.minor}.${version.patch}-${series[1]}${Number(series[2]) + 1}`;
}

/**
 * Conventional-commit types grouped by what they mean for a release. Anything
 * unrecognised (including a subject with no type prefix) counts as internal, so
 * an unparseable subject can never on its own trigger a release recommendation.
 */
const RELEASABLE_TYPES = new Set(['feat', 'fix', 'perf', 'revert']);
const CONVENTIONAL = /^(\w+)(?:\(([^)]*)\))?(!)?:/;

/** The category a commit subject falls into. */
function classify(subject) {
  const m = subject.match(CONVENTIONAL);
  if (!m) return 'internal';
  const [, type, scope, breaking] = m;
  if (breaking || RELEASABLE_TYPES.has(type.toLowerCase())) return 'releasable';
  if (scope && /^deps/i.test(scope)) return 'deps';
  return 'internal';
}

/** Every non-merge commit since `tag`, classified. */
function commitsSince(tag, branch) {
  const range = tag ? `${tag}..${REMOTE}/${branch}` : `${REMOTE}/${branch}`;
  return git(['log', '--no-merges', '--format=%h\t%s', range])
    .split('\n')
    .filter(Boolean)
    .map((line) => {
      const tab = line.indexOf('\t');
      const subject = line.slice(tab + 1);
      return { sha: line.slice(0, tab), subject, category: classify(subject) };
    });
}

/**
 * Recommendation derived purely from commit types: `feat:`/`fix:` warrant a
 * release, dependency bumps alone are a judgement call, everything else waits.
 * The digest shows this next to any model-written recommendation so the two can
 * be compared rather than one silently overriding the other.
 */
function verdictFor(counts) {
  if (counts.releasable > 0) return 'release';
  if (counts.deps > 0) return 'consider';
  return 'skip';
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
  const releasable = r.commits.filter((c) => c.category === 'releasable');
  const n = releasable.length;
  const lines = [];
  const since = r.lastTag
    ? `\`${r.lastTag}\`${r.lastTagDate ? ` (released ${r.lastTagDate})` : ''}`
    : 'the start of the branch';
  lines.push(`Branch \`${r.branch}\` has **${n}** unreleased commit${n === 1 ? '' : 's'} worth releasing since ${since}.`);
  lines.push('');
  lines.push(`Suggested next release: ${target}`);
  lines.push('');
  lines.push('### Changes to release');
  lines.push('');
  for (const c of releasable) {
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
  const lines = [];
  lines.push('## Branch release check');
  lines.push('');
  lines.push(`_Generated ${now} from \`${VALIDATION_YML}\` maintained branches._`);
  lines.push('');
  const active = results.filter((r) => r.verdict !== 'skip');
  if (!active.length) {
    lines.push('No branch has unreleased changes worth a release. 🎉');
    return lines.join('\n');
  }
  lines.push('| Branch | Last release | Released | Next | feat/fix | deps | other | Verdict |');
  lines.push('| --- | --- | --- | --- | --- | --- | --- | --- |');
  for (const r of active) {
    const c = r.counts;
    lines.push(
      `| \`${r.branch}\` | ${r.lastTag || '—'} | ${r.lastTagDate || '—'} | ${r.next || '—'} | ` +
        `${c.releasable} | ${c.deps} | ${c.internal} | ${r.verdict} |`
    );
  }
  const quiet = results.filter((r) => r.verdict === 'skip');
  if (quiet.length) {
    lines.push('');
    lines.push(`Nothing to release: ${quiet.map((r) => `\`${r.branch}\``).join(', ')}.`);
  }
  return lines.join('\n');
}

function main() {
  const opts = parseArgs(process.argv.slice(2));
  const branches = readMaintainedBranches();
  // `main` leads the digest: its pre-release is the one people forget.
  if (opts.includeMain) branches.unshift(MAIN_BRANCH);
  if (opts.fetch) {
    fetchRefs(branches);
  }

  const now = new Date().toISOString().slice(0, 10);
  const results = branches.map((branch) => {
    const version = latestTagFor(branch === MAIN_BRANCH ? readMainVersionLine() : branch);
    const lastTag = version ? version.tag : null;
    const lastTagDate = tagDate(lastTag);
    const commits = commitsSince(lastTag, branch);
    const counts = {
      releasable: commits.filter((c) => c.category === 'releasable').length,
      deps: commits.filter((c) => c.category === 'deps').length,
      internal: commits.filter((c) => c.category === 'internal').length,
    };
    return {
      branch,
      lastTag,
      lastTagDate,
      next: suggestNextVersion(version),
      counts,
      verdict: verdictFor(counts),
      compareUrl: lastTag
        ? `${REPO_URL}/compare/${lastTag}...${branch}`
        : `${REPO_URL}/commits/${branch}`,
      commits,
    };
  });

  fs.mkdirSync(opts.outDir, { recursive: true });

  // `main` is deliberately absent from the manifest: the issue workflow tracks
  // patch releases of maintained branches, and pre-releases are not those.
  const tracked = results.filter((r) => r.branch !== MAIN_BRANCH);
  const pending = [];
  for (const r of tracked) {
    if (r.counts.releasable === 0) continue;
    const bodyFile = `${opts.outDir}/${r.branch}.md`;
    fs.writeFileSync(bodyFile, branchBody(r, now) + '\n');
    pending.push({
      branch: r.branch,
      title: issueTitle(r.branch),
      next: r.next,
      count: r.counts.releasable,
      body: bodyFile,
    });
  }
  const clean = tracked
    .filter((r) => r.counts.releasable === 0)
    .map((r) => issueTitle(r.branch));

  fs.writeFileSync(
    `${opts.outDir}/manifest.json`,
    JSON.stringify({ pending, clean }, null, 2) + '\n'
  );
  fs.writeFileSync(
    `${opts.outDir}/report.json`,
    JSON.stringify({ generated: now, branches: results }, null, 2) + '\n'
  );
  fs.writeFileSync(`${opts.outDir}/summary.md`, buildSummary(results, now) + '\n');

  console.log(
    pending.length
      ? `Pending releases on: ${pending.map((p) => p.next || p.branch).join(', ')}`
      : 'No pending patch releases'
  );
  for (const r of results) {
    const c = r.counts;
    console.log(
      `  ${r.branch}: ${r.verdict} — ${c.releasable} feat/fix, ${c.deps} deps, ` +
        `${c.internal} other (last ${r.lastTag || 'none'})`
    );
  }

  if (process.env.GITHUB_OUTPUT) {
    fs.appendFileSync(process.env.GITHUB_OUTPUT, `has_pending=${pending.length > 0}\n`);
  }
}

main();
