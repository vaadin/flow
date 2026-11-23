// Run with: node --test scripts/computeMatrix.test.js
const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { getSlowTestModules } = require('./computeMatrix');

const TEST_DIR = 'src/test/java/com/vaadin/flow';
const property = (groups) => `<properties><slowTestGroups>${groups}</slowTestGroups></properties>`;

// A reactor with flow-tests in it, written to a temporary directory
const FILES = {
  'pom.xml':
    '<modules><module>slow</module><module>plain</module><module>no-tests</module>' +
    '<!-- <module>commented</module> --><module>flow-tests</module></modules>' +
    property('com.vaadin.flow.testcategory.RootOnlyTests'),
  'flow-tests/pom.xml':
    '<modules><module>push</module><module>servlet-containers/tomcat9</module></modules>' +
    '<!-- <slowTestGroups>com.vaadin.flow.testcategory.CommentedTests</slowTestGroups> -->' +
    property('com.vaadin.flow.testcategory.SlowTests,\n    com.vaadin.flow.testcategory.PushTests,'),
  'slow/pom.xml': '',
  [`slow/${TEST_DIR}/ATest.java`]: '@Tag("com.vaadin.flow.testcategory.RootOnlyTests")',
  'plain/pom.xml': '',
  [`plain/${TEST_DIR}/BTest.java`]: 'import com.vaadin.flow.testcategory.CommentedTests;',
  'no-tests/pom.xml': '',
  'no-tests/src/main/java/C.java': 'import com.vaadin.flow.testcategory.SlowTests;',
  'commented/pom.xml': '',
  [`commented/${TEST_DIR}/DTest.java`]: 'import com.vaadin.flow.testcategory.SlowTests;',
  'flow-tests/push/pom.xml': '',
  [`flow-tests/push/${TEST_DIR}/EIT.java`]: 'import com.vaadin.flow.testcategory.PushTests;',
  // In globalExclusions, as the other test jobs leave it out
  'flow-tests/servlet-containers/tomcat9/pom.xml': '',
  [`flow-tests/servlet-containers/tomcat9/${TEST_DIR}/FIT.java`]: 'import com.vaadin.flow.testcategory.SlowTests;'
};

function inReactor(files, fn) {
  const cwd = process.cwd();
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'compute-matrix-'));
  try {
    for (const [file, content] of Object.entries(files)) {
      fs.mkdirSync(path.dirname(path.join(dir, file)), { recursive: true });
      fs.writeFileSync(path.join(dir, file), content);
    }
    process.chdir(dir);
    return fn();
  } finally {
    process.chdir(cwd);
    fs.rmSync(dir, { recursive: true, force: true });
  }
}

test('slow-tests lists the modules whose tests use a category of either slowTestGroups', () => {
  assert.deepStrictEqual(inReactor(FILES, getSlowTestModules), ['slow', 'flow-tests/push']);
});

for (const pom of ['pom.xml', 'flow-tests/pom.xml']) {
  test(`slow-tests fails naming ${pom} when it has no slowTestGroups`, () => {
    const files = { ...FILES, [pom]: '<modules><module>flow-tests</module><module>push</module></modules>' };
    const escaped = pom.replace(/[./]/g, '\\$&');
    assert.throws(() => inReactor(files, getSlowTestModules), new RegExp(`^Error: ${escaped}: no slowTestGroups`));
  });
}
