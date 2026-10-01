// Run with: node --test scripts/formatterSuggestions.test.js
const test = require('node:test');
const assert = require('node:assert');
const { parseDiff, parsePatchRanges, collectSuggestions, summaryComment } = require('./formatterSuggestions');

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
    { path: PATH, line: 1, side: 'RIGHT', body: '```suggestion\npackage com.vaadin.flow;\n\n```' },
    // Removed lines are suggested as an empty replacement of all of them
    { path: PATH, line: 4, side: 'RIGHT', start_line: 3, start_side: 'RIGHT', body: '```suggestion\n```' },
    { path: PATH, line: 5, side: 'RIGHT', body: '```suggestion\n    int x;\n    int y;\n```' }
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
    { path: PATH, line: 1, side: 'RIGHT', body: '```suggestion\n/* header */\n\npackage a;\n```' }
  ]);
});

test('suggestions containing a code fence use a longer one', () => {
  const diff = `diff --git a/${PATH} b/${PATH}\n--- a/${PATH}\n+++ b/${PATH}\n@@ -1 +1 @@\n-  /** \`\`\`x\`\`\` */\n+/** \`\`\`x\`\`\` */\n`;
  const { comments } = suggestions(diff, [[PATH, [[1, 1]]]]);
  assert.strictEqual(comments[0].body, '````suggestion\n/** ```x``` */\n````');
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
