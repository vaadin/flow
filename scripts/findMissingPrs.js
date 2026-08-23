#!/usr/bin/env node
const readline = require('readline');
const execSync = require('child_process').execSync;
const execFile = require('util').promisify(require('child_process').execFile);

const args = process.argv.slice(2);
if (args.length != 1) {
  console.log(`Usage: ${process.argv[1]} <version>`);
  process.exit(1);
}

const mainBranch = 'main';
const version = args[0];
const otherBranch = version;
const targetLabel = `target/${version}`;

// Labels the cherry-pick automation adds when an automated pick failed and the
// PR needs to be picked manually.
const failedPickLabels = [
  `need to pick manually ${version}`,
  `needs-manual-bp/${version}`,
];

// Only commits that landed on main after the target branch diverged are
// backport candidates; older commits are shared history and already present on
// both branches, so there is no need to scan them.
const mainPrs = findMainPrs(`origin/${otherBranch}..origin/${mainBranch}`);
// Numbers referenced by the target branch since it diverged. A backport commit
// keeps the original main PR number, so a candidate whose number appears here is
// already on the branch.
const backported = findReferencedNumbers(`origin/${mainBranch}..origin/${otherBranch}`);

const ignoredTitlePatterns = [
  /Bump frontend dependencies/i,
  /Update frontend dependencies/i,
  /update default Node\.js version/i,
];

const candidates = Object.keys(mainPrs)
  .filter((pr) => !backported.has(pr))
  .filter((pr) => !ignoredTitlePatterns.some((pattern) => pattern.test(mainPrs[pr])));

main();

async function main() {
  // A label lookup is one network round-trip per candidate PR and dominates the
  // runtime, so run the lookups concurrently instead of one at a time.
  const labelsByPr = await mapWithConcurrency(candidates, 16, findLabels);

  const missingPrs = candidates
    .map((pr, i) => {
      const labels = labelsByPr[i];
      // A null result means the number is not a PR (e.g. an issue reference that
      // happened to be the last `(#NNN)` on the line); skip it.
      if (labels === null) return null;
      const failedLabel = failedPickLabels.find((label) => labels.includes(label)) || null;
      const alreadyTargeted = labels.includes(targetLabel);
      return {
        pr,
        title: prTitle(mainPrs[pr]),
        failed: failedLabel !== null,
        failedLabel,
        alreadyTargeted,
      };
    })
    .filter(Boolean)
    // Group by commit type (fix, chore, feat, ...); titles with no recognizable
    // prefix sort last. The stable sort keeps the newest-first order from the
    // git log within each group.
    .sort((a, b) => {
      const ta = commitType(a.title);
      const tb = commitType(b.title);
      if (ta === tb) return 0;
      if (ta === null) return 1;
      if (tb === null) return -1;
      return ta.localeCompare(tb);
    });

  if (missingPrs.length === 0) {
    console.log(`No PRs missing from ${otherBranch}`);
    return;
  }

  const selected = await selectPrs(missingPrs);
  if (selected.length === 0) {
    console.log('\nNothing selected.');
    return;
  }

  console.log('\nRun this to apply the changes:\n');

  // Failed picks already carry target/<version>; selecting them means re-trigger
  // the pick by removing the failure label rather than re-adding the target.
  const toLabel = selected.filter((p) => !p.failed);
  const toUnblock = selected.filter((p) => p.failed);

  if (toLabel.length > 0) {
    console.log(
      `for pr in ${toLabel.map((p) => p.pr).join(' ')}; do gh pr edit "$pr" --repo vaadin/flow --add-label "${targetLabel}"; done`,
    );
  }

  // Failed PRs may carry different variants of the failure label; remove the
  // exact one each PR has.
  const byFailedLabel = {};
  for (const p of toUnblock) {
    (byFailedLabel[p.failedLabel] ||= []).push(p.pr);
  }
  for (const [failedLabel, prs] of Object.entries(byFailedLabel)) {
    console.log(
      `for pr in ${prs.join(' ')}; do gh pr edit "$pr" --repo vaadin/flow --remove-label "${failedLabel}"; done`,
    );
  }
}

// Runs `worker` over `items` with at most `limit` in flight at once, preserving
// input order in the returned results.
async function mapWithConcurrency(items, limit, worker) {
  const results = new Array(items.length);
  let next = 0;
  async function run() {
    while (next < items.length) {
      const i = next++;
      results[i] = await worker(items[i]);
    }
  }
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, run));
  return results;
}

// Strip the leading commit hash from a `git log --oneline` line.
function prTitle(line) {
  return line.replace(/^\S+\s+/, '');
}

// The conventional-commit type of a title, e.g. `fix` for `fix(server): ...`,
// or null when the title has no lowercase `type:` prefix.
function commitType(title) {
  const match = title.match(/^([a-z]+)(\([^)]*\))?!?:/);
  return match ? match[1] : null;
}

// Interactive multi-select list. Navigate with arrows or j/k, toggle with
// space, toggle all with `a`, confirm with enter, cancel with q / ctrl-c.
function selectPrs(items) {
  return new Promise((resolve) => {
    const selected = new Set();
    let cursor = 0;
    const stdin = process.stdin;

    if (!stdin.isTTY) {
      // No interactive terminal: fall back to PRs that aren't already targeted
      // and didn't fail a previous pick, i.e. the ones that still need a label.
      resolve(items.filter((item) => !item.alreadyTargeted && !item.failed));
      return;
    }

    readline.emitKeypressEvents(stdin);
    stdin.setRawMode(true);
    stdin.resume();

    const header = `Select PRs to backport to ${version} ` +
      `(space/enter=toggle, a=all, d=done, q=cancel)`;
    let rendered = false;

    function render() {
      if (rendered) {
        // Move cursor back up over the previously printed block.
        process.stdout.write(`\x1b[${items.length + 1}A`);
      }
      rendered = true;

      process.stdout.write(`\x1b[2K${header}\n`);
      items.forEach((item, i) => {
        const pointer = i === cursor ? '>' : ' ';
        const box = selected.has(i) ? '[x]' : '[ ]';
        let tag = '';
        if (item.failed) {
          tag = ` \x1b[31m[FAILED PICK]\x1b[0m`;
        } else if (item.alreadyTargeted) {
          tag = ` \x1b[33m[already ${targetLabel}]\x1b[0m`;
        }
        const line = `${pointer} ${box} #${item.pr} ${item.title}${tag}`;
        process.stdout.write(`\x1b[2K${line}\n`);
      });
    }

    function finish(result) {
      stdin.setRawMode(false);
      stdin.pause();
      stdin.removeListener('keypress', onKeypress);
      resolve(result);
    }

    function onKeypress(str, key) {
      if (!key) return;

      if (key.name === 'up' || key.name === 'k') {
        cursor = (cursor - 1 + items.length) % items.length;
      } else if (key.name === 'down' || key.name === 'j') {
        cursor = (cursor + 1) % items.length;
      } else if (key.name === 'space' || key.name === 'return') {
        if (selected.has(cursor)) selected.delete(cursor);
        else selected.add(cursor);
      } else if (key.name === 'a') {
        if (selected.size === items.length) selected.clear();
        else items.forEach((_, i) => selected.add(i));
      } else if (key.name === 'd') {
        finish([...selected].sort((a, b) => a - b).map((i) => items[i]));
        return;
      } else if (key.name === 'q' || (key.ctrl && key.name === 'c')) {
        finish([]);
        return;
      } else {
        return;
      }
      render();
    }

    stdin.on('keypress', onKeypress);
    render();
  });
}

// Returns the label names for a PR, or null if the number does not resolve to a
// PR. Issue and PR numbers share one namespace on GitHub, so a `(#NNN)` picked
// from a commit title may be an issue; the pulls endpoint then returns 404 and
// `gh` exits non-zero, which we treat as "not a PR" rather than a hard error.
async function findLabels(pr) {
  let output;
  try {
    ({ stdout: output } = await execFile('gh', [
      'api',
      '-H', 'Accept: application/vnd.github+json',
      '-H', 'X-GitHub-Api-Version: 2022-11-28',
      `/repos/vaadin/flow/pulls/${pr}`,
    ], { maxBuffer: 256 * 1024 * 1024 }));
  } catch {
    return null;
  }
  const json = JSON.parse(output);
  return json.labels.map(label => label.name);
}

// Maps PR number -> `git log --oneline` line for each commit in the range. A
// commit title may contain several `(#NNN)` references: the original PR title
// can mention issues (e.g. `... workaround (#15086) (#24902)`), while the
// squash-merge always appends the PR number last. Only the last match is the
// PR; earlier ones are issue references.
function findMainPrs(range) {
  const output = execSync(`git log --oneline ${range}`, {
    encoding: 'utf8',
    maxBuffer: 256 * 1024 * 1024,
  });
  const entries = output.split('\n').flatMap((line) => {
    const all = [...line.matchAll(/\(#(\d+)\)/g)];
    if (all.length === 0) return [];
    return [[all[all.length - 1][1], line]];
  });
  return Object.fromEntries(entries);
}

// Every `(#NNN)` referenced by any commit in the range, as a set. Used to tell
// whether a main PR is already on the target branch: a backport commit reuses
// the original PR number, but not necessarily as the last match (e.g.
// `... (#22968) (CP: 24.9) (#22978)`), so all matches must be collected.
function findReferencedNumbers(range) {
  const output = execSync(`git log --oneline ${range}`, {
    encoding: 'utf8',
    maxBuffer: 256 * 1024 * 1024,
  });
  const numbers = new Set();
  for (const line of output.split('\n')) {
    for (const match of line.matchAll(/\(#(\d+)\)/g)) {
      numbers.add(match[1]);
    }
  }
  return numbers;
}
