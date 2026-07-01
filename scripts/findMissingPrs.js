#!/usr/bin/env node
const readline = require('readline');
const execSync = require('child_process').execSync;

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

const mainPrs = findPrs(mainBranch);
const otherPrs = findPrs(otherBranch);

const ignoredTitlePatterns = [
  /Bump frontend dependencies/i,
  /Update frontend dependencies/i,
  /update default Node\.js version/i,
];

const missingPrs = Object.keys(mainPrs)
  .filter((pr) => !otherPrs[pr])
  .filter((pr) => !ignoredTitlePatterns.some((pattern) => pattern.test(mainPrs[pr])))
  .map((pr) => {
    const labels = findLabels(pr);
    const failedLabel = failedPickLabels.find((label) => labels.includes(label)) || null;
    const alreadyTargeted = labels.includes(targetLabel);
    return {
      pr,
      title: prTitle(mainPrs[pr]),
      failed: failedLabel !== null,
      failedLabel,
      alreadyTargeted,
    };
  });

if (missingPrs.length === 0) {
  console.log(`No PRs missing from ${otherBranch}`);
  process.exit(0);
}

selectPrs(missingPrs).then((selected) => {
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
});

// Strip the leading commit hash from a `git log --oneline` line.
function prTitle(line) {
  return line.replace(/^\S+\s+/, '');
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

function findLabels(pr) {
  const output = execSync(`gh api -H "Accept: application/vnd.github+json" -H "X-GitHub-Api-Version: 2022-11-28" /repos/vaadin/flow/pulls/${pr}`, {
    encoding: 'utf8',
    maxBuffer: 256 * 1024 * 1024,
  });
  const json = JSON.parse(output);
  return json.labels.map(label => label.name);
}

function findPrs(branch) {
  const output = execSync(`git log --oneline origin/${branch}`, {
    encoding: 'utf8',
    maxBuffer: 256 * 1024 * 1024,
  });
  const prAndLine = output.split('\n').flatMap((line) => {
    const all = [...line.matchAll(/\(#(\d+)\)/g)];
    return all.map((match) => ({ line, pr: match[1] }));
  });

  return Object.assign(...prAndLine.map((pr) => ({ [pr.pr]: pr.line })));
}
