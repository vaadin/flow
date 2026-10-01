/**
 * Turns the formatter diff of a pull request into review suggestions, so that
 * the author can accept the formatting changes from the pull request page
 * instead of running the formatter locally.
 *
 * Runs from formatter-suggestions.yml, after formatter.yml has run
 * `mvn spotless:apply` on the pull request without any permissions and
 * uploaded the resulting `git diff`. The diff comes from code of the pull
 * request, which may be a fork, so it is only ever parsed as text: the pull
 * request is looked up from the workflow run rather than from the diff, and
 * only the files the pull request changes get suggestions.
 *
 * A change can only be suggested on lines that are part of the pull request
 * diff. Changes elsewhere, and ones a suggestion can't express like a missing
 * newline at the end of a file, are counted in the comment, which keeps
 * pointing at `mvn spotless:apply`.
 *
 * Usage, from actions/github-script:
 *   require('./scripts/formatterSuggestions.js').run({ github, context, core, diffFile })
 */
const fs = require('fs');

// Starts the pull request comment, so that each run updates the one comment
// instead of adding another. The same marker as before the suggestions were
// added, so that the comments already on open pull requests are reused.
const COMMENT_MARKER = '<!-- tc-formatter -->';

// Starts the body of the reviews holding the suggestions, so that a re-run for
// the same commit does not post them again
const REVIEW_MARKER = '<!-- formatter-suggestions -->';

// Caps the suggestions in one review. A pull request that is not formatted at
// all is better served by running the formatter than by accepting hundreds of
// suggestions one by one.
const MAX_SUGGESTIONS = 50;

// The diff is uploaded by a run of code from the pull request, so its size is
// not to be trusted either
const MAX_DIFF_BYTES = 1024 * 1024;

/**
 * Parses a unified diff into the changed blocks of each file. A block is a
 * run of removed and added lines between two context lines, with the line
 * number of its first removed line in the old file, and the context lines
 * around it.
 */
function parseDiff(diff) {
  const files = [];
  let file = null;
  let block = null;
  let oldLine = 0;
  let previous = null;
  const closeBlock = (next) => {
    if (block) {
      block.next = next;
      file.blocks.push(block);
      block = null;
    }
  };
  for (const line of diff.replace(/\n$/, '').split('\n')) {
    if (line.startsWith('diff --git ')) {
      closeBlock(null);
      file = { path: null, blocks: [] };
      files.push(file);
      oldLine = 0;
      continue;
    }
    if (!file) {
      continue;
    }
    const hunk = line.match(/^@@ -(\d+)(?:,\d+)? \+\d+(?:,\d+)? @@/);
    if (hunk) {
      closeBlock(null);
      oldLine = Number(hunk[1]);
      // A hunk that only adds lines to an empty file starts at line 0
      oldLine = oldLine === 0 ? 1 : oldLine;
      previous = null;
      continue;
    }
    if (!oldLine) {
      // Header lines before the first hunk
      const path = line.match(/^\+\+\+ b\/(.+)$/);
      if (path) {
        file.path = path[1];
      }
      continue;
    }
    if (line.startsWith('\\')) {
      // "\ No newline at end of file" applies to the line before
      if (block) {
        block.newlineChanged = true;
      }
      continue;
    }
    const kind = line[0];
    const text = line.slice(1);
    if (kind === '-' || kind === '+') {
      if (!block) {
        block = { oldStart: oldLine, removed: [], added: [], previous, next: null, newlineChanged: false };
      }
      if (kind === '-') {
        block.removed.push(text);
        oldLine++;
      } else {
        block.added.push(text);
      }
    } else if (kind === ' ' || line === '') {
      // An empty line is a blank context line whose leading space was trimmed
      closeBlock(text);
      previous = text;
      oldLine++;
    }
  }
  closeBlock(null);
  return files.filter((f) => f.path && f.blocks.length > 0);
}

/**
 * The lines of the new file that the hunks of a pull request file patch
 * cover, as [first, last] ranges. A review comment can only be placed within
 * one of them.
 */
function parsePatchRanges(patch) {
  const ranges = [];
  for (const match of (patch || '').matchAll(/^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@/gm)) {
    const start = Number(match[1]);
    const count = match[2] === undefined ? 1 : Number(match[2]);
    if (count > 0) {
      ranges.push([start, start + count - 1]);
    }
  }
  return ranges;
}

/** A fenced code block of the given kind, longer than any backtick run in the content. */
function fence(kind, lines) {
  const longest = Math.max(2, ...lines.map((line) => Math.max(0, ...(line.match(/`+/g) || []).map((m) => m.length))));
  const ticks = '`'.repeat(longest + 1);
  return [ticks + kind, ...lines, ticks].join('\n');
}

/**
 * The review comment suggesting a changed block, or null if a suggestion
 * can't express it. A suggestion replaces whole lines, so a block that only
 * adds lines is anchored on the line before or after it.
 */
function suggestionOf(path, block) {
  let start = block.oldStart;
  let end = block.oldStart + block.removed.length - 1;
  let replacement = block.added;
  if (block.removed.length === 0) {
    if (block.previous !== null) {
      start = end = block.oldStart - 1;
      replacement = [block.previous, ...block.added];
    } else if (block.next !== null) {
      start = end = block.oldStart;
      replacement = [...block.added, block.next];
    } else {
      return null;
    }
  } else if (block.newlineChanged && block.removed.join('\n') === block.added.join('\n')) {
    // Only the newline at the end of the file differs
    return null;
  }
  const comment = { path, line: end, side: 'RIGHT', body: fence('suggestion', replacement) };
  if (start < end) {
    Object.assign(comment, { start_line: start, start_side: 'RIGHT' });
  }
  return comment;
}

/**
 * Splits the formatter changes of the files a pull request changes into the
 * review comments suggesting them, and the number of changes per file that
 * can't be suggested. `prFiles` maps each file of the pull request to the
 * line ranges its patch covers.
 */
function collectSuggestions(diffFiles, prFiles, maxSuggestions = MAX_SUGGESTIONS) {
  const comments = [];
  const unsuggested = new Map();
  for (const { path, blocks } of diffFiles) {
    const ranges = prFiles.get(path);
    if (!ranges) {
      continue;
    }
    for (const block of blocks) {
      const comment = suggestionOf(path, block);
      const first = comment && (comment.start_line ?? comment.line);
      const inDiff = comment && ranges.some(([from, to]) => from <= first && comment.line <= to);
      if (inDiff && comments.length < maxSuggestions) {
        comments.push(comment);
      } else {
        unsuggested.set(path, (unsuggested.get(path) || 0) + 1);
      }
    }
  }
  return { comments, unsuggested };
}

/** The sticky pull request comment body. */
function summaryComment({ files, suggested, unsuggested, runUrl }) {
  const lines = [
    COMMENT_MARKER,
    '### Format Checker Report',
    '',
    '![BLOCKER][BLOCKER] There are **' + files.length + ' files** with format errors',
    '',
    "[BLOCKER]: https://sonarsource.github.io/sonar-github/severity-blocker.png 'Severity: BLOCKER'",
    ''
  ];
  if (suggested > 0) {
    lines.push(
      `- ${suggested} formatting changes are suggested in a review of this pull request. ` +
        'Accept them from the **Files changed** tab, with **Add suggestion to batch** and **Commit suggestions**.',
      ''
    );
  }
  const remaining = [...unsuggested.values()].reduce((sum, count) => sum + count, 0);
  if (remaining > 0) {
    lines.push(
      `- ${remaining} formatting changes could not be suggested, because they are outside the changed lines ` +
        'or too many to review one by one: ' +
        [...unsuggested.entries()].map(([path, count]) => `\`${path}\` (${count})`).join(', '),
      ''
    );
  }
  lines.push(
    `- To see a complete report of formatting issues, download the [differences artifact](${runUrl})`,
    '',
    '- To fix the build, please run `mvn spotless:apply` in your branch and commit the changes.',
    '',
    '- Optionally you might add the following line in your `.git/hooks/pre-commit` file:',
    '',
    '      mvn spotless:apply',
    '',
    'Here is the list of files with format issues in your PR:',
    '',
    fence('', files),
    ''
  );
  return lines.join('\n');
}

/** The open pull request whose head is the commit the workflow run checked. */
async function findPullRequest(github, context, run) {
  const pulls = await github.paginate(github.rest.pulls.list, {
    ...context.repo,
    state: 'open',
    head: `${run.head_repository.owner.login}:${run.head_branch}`,
    per_page: 100
  });
  return pulls.find((pull) => pull.head.sha === run.head_sha) || null;
}

async function run({ github, context, core, diffFile }) {
  const workflowRun = context.payload.workflow_run;
  const pull = await findPullRequest(github, context, workflowRun);
  if (!pull) {
    core.info(`No open pull request has ${workflowRun.head_sha} as its head, it was probably pushed to since`);
    return;
  }
  const issue = { ...context.repo, issue_number: pull.number };
  const existing = (await github.paginate(github.rest.issues.listComments, { ...issue, per_page: 100 })).find(
    (comment) => comment.user.login === 'github-actions[bot]' && comment.body.includes(COMMENT_MARKER)
  );

  const size = fs.existsSync(diffFile) ? fs.statSync(diffFile).size : 0;
  if (size > MAX_DIFF_BYTES) {
    core.setFailed(`The formatter diff is ${size} bytes, more than the ${MAX_DIFF_BYTES} accepted`);
    return;
  }
  const diffFiles = size > 0 ? parseDiff(fs.readFileSync(diffFile, 'utf8')) : [];
  if (diffFiles.length === 0) {
    if (existing) {
      await github.rest.issues.deleteComment({ ...context.repo, comment_id: existing.id });
    }
    return;
  }

  const prFiles = new Map(
    (
      await github.paginate(github.rest.pulls.listFiles, { ...context.repo, pull_number: pull.number, per_page: 100 })
    ).map((file) => [file.filename, parsePatchRanges(file.patch)])
  );
  let { comments, unsuggested } = collectSuggestions(diffFiles, prFiles);

  const reviews = await github.paginate(github.rest.pulls.listReviews, {
    ...context.repo,
    pull_number: pull.number,
    per_page: 100
  });
  const reviewed = reviews.some(
    (review) => review.commit_id === pull.head.sha && (review.body || '').includes(REVIEW_MARKER)
  );
  if (comments.length > 0 && !reviewed) {
    try {
      await github.rest.pulls.createReview({
        ...context.repo,
        pull_number: pull.number,
        commit_id: pull.head.sha,
        event: 'COMMENT',
        body: REVIEW_MARKER + '\nFormatting changes from `mvn spotless:apply`.',
        comments
      });
    } catch (error) {
      core.warning(`Could not post the formatting suggestions: ${error.message}`);
      for (const { path } of comments) {
        unsuggested.set(path, (unsuggested.get(path) || 0) + 1);
      }
      comments = [];
    }
  }

  const files = diffFiles.map((file) => file.path);
  const body = summaryComment({ files, suggested: comments.length, unsuggested, runUrl: workflowRun.html_url });
  if (existing) {
    await github.rest.issues.updateComment({ ...context.repo, comment_id: existing.id, body });
  } else {
    await github.rest.issues.createComment({ ...issue, body });
  }
}

module.exports = { run, parseDiff, parsePatchRanges, collectSuggestions, summaryComment };
