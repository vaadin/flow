// Run with: node --test scripts/formatterSuggestions.test.js
const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { run, parseDiff, parsePatchRanges, collectSuggestions, summaryComment } = require('./formatterSuggestions');

const SUGGESTION = 'Formatting suggestion:\n';
const PATH = 'flow-server/src/main/java/com/vaadin/flow/Foo.java';

// What `git diff` prints after spotless:apply reformatted a file
const DIFF = `diff --git a/${PATH} b/${PATH}
index 8058f4f..e4f99f8 100644
--- a/${PATH}
+++ b/${PATH}
@@ -1,8 +1,10 @@
 package com.vaadin.flow;
+
 class Foo {
-  void f( ) {
-  }
     int x;
+    int y;

     int z;
-}
\\ No newline at end of file
+}
`;

const suggestions = (diff, prFiles, max) => collectSuggestions(parseDiff(diff), new Map(prFiles), max);

test('parseDiff splits hunks into changed blocks numbered on the old file', () => {
  const [file] = parseDiff(DIFF);
  assert.strictEqual(file.path, PATH);
  assert.deepStrictEqual(
    file.blocks.map(({ oldStart, removed, added }) => ({ oldStart, removed, added })),
    [
      { oldStart: 2, removed: [], added: [''] },
      { oldStart: 3, removed: ['  void f( ) {', '  }'], added: [] },
      { oldStart: 6, removed: [], added: ['    int y;'] },
      { oldStart: 8, removed: ['}'], added: ['}'] }
    ]
  );
});

test('parsePatchRanges reads the new file lines of each hunk', () => {
  assert.deepStrictEqual(parsePatchRanges('@@ -1,3 +1,4 @@\n a\n+b\n@@ -10 +11 @@\n-c\n+d\n@@ -20,2 +22,0 @@\n-e'), [
    [1, 4],
    [11, 11]
  ]);
  assert.deepStrictEqual(parsePatchRanges(undefined), []);
});

test('changes within the pull request diff become suggestions', () => {
  const { comments, unsuggested } = suggestions(DIFF, [[PATH, [[1, 10]]]]);
  assert.deepStrictEqual(comments, [
    // An added line is suggested together with the line before it
    { path: PATH, line: 1, side: 'RIGHT', body: SUGGESTION + '```suggestion\npackage com.vaadin.flow;\n\n```' },
    // Removed lines are suggested as an empty replacement of all of them
    { path: PATH, line: 4, side: 'RIGHT', start_line: 3, start_side: 'RIGHT', body: SUGGESTION + '```suggestion\n```' },
    { path: PATH, line: 5, side: 'RIGHT', body: SUGGESTION + '```suggestion\n    int x;\n    int y;\n```' }
  ]);
  // A missing newline at the end of the file can't be suggested
  assert.deepStrictEqual([...unsuggested], [[PATH, 1]]);
});

test('changes outside the pull request diff and in other files are not suggested', () => {
  const { comments, unsuggested } = suggestions(DIFF, [[PATH, [[3, 3]]]]);
  assert.deepStrictEqual(comments, []);
  assert.deepStrictEqual([...unsuggested], [[PATH, 4]]);

  const other = suggestions(DIFF, [['Other.java', [[1, 10]]]]);
  assert.deepStrictEqual(other.comments, []);
  assert.deepStrictEqual([...other.unsuggested], []);
});

test('suggestions past the limit are counted instead', () => {
  const { comments, unsuggested } = suggestions(DIFF, [[PATH, [[1, 10]]]], 1);
  assert.strictEqual(comments.length, 1);
  assert.deepStrictEqual([...unsuggested], [[PATH, 3]]);
});

test('lines added at the start of a file are suggested with the line after them', () => {
  const diff = `diff --git a/${PATH} b/${PATH}\n--- a/${PATH}\n+++ b/${PATH}\n@@ -1 +1,3 @@\n+/* header */\n+\n package a;\n`;
  const { comments } = suggestions(diff, [[PATH, [[1, 3]]]]);
  assert.deepStrictEqual(comments, [
    { path: PATH, line: 1, side: 'RIGHT', body: SUGGESTION + '```suggestion\n/* header */\n\npackage a;\n```' }
  ]);
});

test('a changed last line is suggested, and its missing newline counted', () => {
  const diff = `diff --git a/${PATH} b/${PATH}\n--- a/${PATH}\n+++ b/${PATH}\n@@ -1,2 +1,2 @@\n class Foo {\n-  }\n\\ No newline at end of file\n+}\n`;
  const { comments, unsuggested } = suggestions(diff, [[PATH, [[1, 2]]]]);
  assert.deepStrictEqual(comments, [
    { path: PATH, line: 2, side: 'RIGHT', body: SUGGESTION + '```suggestion\n}\n```' }
  ]);
  assert.deepStrictEqual([...unsuggested], [[PATH, 1]]);
});

test('suggestions containing a code fence use a longer one', () => {
  const diff = `diff --git a/${PATH} b/${PATH}\n--- a/${PATH}\n+++ b/${PATH}\n@@ -1 +1 @@\n-  /** \`\`\`x\`\`\` */\n+/** \`\`\`x\`\`\` */\n`;
  const { comments } = suggestions(diff, [[PATH, [[1, 1]]]]);
  assert.strictEqual(comments[0].body, SUGGESTION + '````suggestion\n/** ```x``` */\n````');
});

test('summaryComment points at the suggestions and at what could not be suggested', () => {
  const body = summaryComment({
    files: [PATH],
    suggested: 3,
    unsuggested: new Map([[PATH, 1]]),
    runUrl: 'https://github.com/vaadin/flow/actions/runs/1'
  });
  assert.match(body, /^<!-- tc-formatter -->\n/);
  assert.match(body, /\*\*1 files\*\* with format errors/);
  assert.match(body, /3 formatting changes are suggested/);
  assert.match(
    body,
    /1 formatting changes could not be suggested.*`flow-server\/src\/main\/java\/com\/vaadin\/flow\/Foo.java` \(1\)/
  );
  assert.match(body, /\[differences artifact\]\(https:\/\/github.com\/vaadin\/flow\/actions\/runs\/1\)/);
});

const HEAD = 'abc123';
const PATCH = '@@ -1,8 +1,10 @@';

/**
 * Runs run() against a stub of the GitHub API that records the calls
 * changing anything. `state` holds what the API returns.
 */
async function runWith(diff, state = {}) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'formatter-'));
  const diffFile = path.join(dir, 'formatter-diff.txt');
  if (diff !== null) {
    fs.writeFileSync(diffFile, diff);
  }
  const calls = [];
  const record = (name, result) => async (params) => {
    calls.push({ name, params });
    if (result instanceof Error) {
      throw result;
    }
  };
  const lists = new Map();
  const list = (items) => {
    const fn = async () => {};
    lists.set(fn, items);
    return fn;
  };
  const github = {
    paginate: async (fn) => lists.get(fn),
    graphql: async (query, variables) => {
      if (query.startsWith('mutation')) {
        calls.push({ name: 'resolveReviewThread', params: variables });
        return {};
      }
      const nodes = state.threads ?? [];
      return { repository: { pullRequest: { reviewThreads: { pageInfo: { hasNextPage: false }, nodes } } } };
    },
    rest: {
      pulls: {
        list: list(state.pulls ?? [{ number: 7, head: { sha: HEAD } }]),
        listFiles: list([{ filename: PATH, patch: PATCH }]),
        createReview: record('createReview', state.createReviewError)
      },
      issues: {
        listComments: list(state.comments ?? []),
        createComment: record('createComment'),
        updateComment: record('updateComment'),
        deleteComment: record('deleteComment')
      }
    }
  };
  const context = {
    repo: { owner: 'vaadin', repo: 'flow' },
    payload: {
      workflow_run: {
        head_sha: HEAD,
        head_branch: 'fix',
        head_repository: { owner: { login: 'someone' } },
        html_url: 'https://github.com/vaadin/flow/actions/runs/1'
      }
    }
  };
  const core = {
    info: () => {},
    warning: () => {},
    setFailed: (message) => calls.push({ name: 'setFailed', message })
  };
  try {
    await run({ github, context, core, diffFile });
  } finally {
    fs.rmSync(dir, { recursive: true });
  }
  return calls;
}

const STICKY = { id: 42, user: { login: 'github-actions[bot]' }, body: '<!-- tc-formatter -->\nold' };

test('run posts the suggestions in a review and the summary in a comment', async () => {
  const calls = await runWith(DIFF);
  assert.deepStrictEqual(
    calls.map((call) => call.name),
    ['createReview', 'createComment']
  );
  assert.strictEqual(calls[0].params.commit_id, HEAD);
  assert.strictEqual(calls[0].params.comments.length, 3);
  assert.match(calls[1].params.body, /3 formatting changes are suggested/);
});

/** A review thread as GraphQL returns it. */
const thread = (id, { line, startLine = null, body, resolved = false, replies = [] }) => ({
  id,
  isResolved: resolved,
  path: PATH,
  line,
  startLine,
  comments: {
    nodes: [
      { author: { login: 'github-actions' }, body },
      ...replies.map((login) => ({ author: { login }, body: 'ok' }))
    ]
  }
});
const REPEATED = SUGGESTION + '```suggestion\n    int x;\n    int y;\n```';

test('run neither posts again nor resolves a suggestion it still makes', async () => {
  const threads = [thread('T1', { line: 5, body: REPEATED })];
  const calls = await runWith(DIFF, { threads, comments: [STICKY] });
  assert.deepStrictEqual(
    calls.map((call) => call.name),
    ['createReview', 'updateComment']
  );
  assert.strictEqual(calls[0].params.comments.length, 2);
  assert.match(calls[1].params.body, /3 formatting changes are suggested/);
});

test('run resolves the suggestions it no longer makes, unless someone replied', async () => {
  const threads = [
    thread('outdated', { line: null, body: REPEATED }),
    thread('changed', { line: 5, body: SUGGESTION + '```suggestion\nother\n```' }),
    thread('replied', { line: null, body: REPEATED, replies: ['author'] }),
    thread('resolved', { line: null, body: REPEATED, resolved: true }),
    thread('other', { line: null, body: 'Not a formatting suggestion' })
  ];
  const calls = await runWith(DIFF, { threads });
  assert.deepStrictEqual(
    calls.filter((call) => call.name === 'resolveReviewThread').map((call) => call.params.id),
    ['outdated', 'changed']
  );
});

test('run counts the suggestions as not suggested when the review is refused', async () => {
  const calls = await runWith(DIFF, { createReviewError: new Error('Unprocessable Entity') });
  const body = calls.find((call) => call.name === 'createComment').params.body;
  assert.doesNotMatch(body, /are suggested/);
  assert.match(body, /4 formatting changes could not be suggested/);
});

test('run resolves the suggestions and deletes the comment once the format is fixed', async () => {
  const calls = await runWith('', { comments: [STICKY], threads: [thread('T1', { line: 5, body: REPEATED })] });
  assert.deepStrictEqual(calls, [
    { name: 'resolveReviewThread', params: { id: 'T1' } },
    { name: 'deleteComment', params: { owner: 'vaadin', repo: 'flow', comment_id: 42 } }
  ]);
});

test('run rejects an oversized diff', async () => {
  const calls = await runWith('x'.repeat(1024 * 1024 + 1));
  assert.deepStrictEqual(
    calls.map((call) => call.name),
    ['setFailed']
  );
});

test('run does nothing when no open pull request has the commit as its head', async () => {
  const calls = await runWith(DIFF, { pulls: [{ number: 7, head: { sha: 'newer' } }] });
  assert.deepStrictEqual(calls, []);
});
