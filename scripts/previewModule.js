#!/usr/bin/env node
/**
 * Picks the test module a pull request preview deployment serves, and writes
 * the pull request comment that links to it.
 *
 * Only one test module is deployed per pull request. flow-tests/test-default
 * is preferred: it is deployed whenever the pull request touches it, and also
 * when the pull request touches no deployable test module at all, as for a
 * change to the framework itself. Otherwise the deployable test module with
 * the most changed files is deployed, ties broken by name so the choice is
 * stable between pushes. Which modules are deployable is read from their
 * poms, see deployment().
 *
 * Links are generated to the views the pull request adds or changes, and to
 * the ones the integration tests it adds or changes open, resolved the way
 * the tests resolve them: a getTestPath() returning a literal; otherwise the
 * view named by @TestFor or by the IT to View naming convention, served at
 * /view/<class name> in the modules that have ViewTestServlet (from
 * flow-test-common) and at its @Route elsewhere.
 *
 * Usage: node scripts/previewModule.js <preview url> < <changed files>
 *
 * The changed files are read from standard input, one repository path per
 * line, e.g. from `git diff --name-only origin/main...`.
 *
 * Outputs:
 * - `module=<path>` and `context-path=<path>` appended to $GITHUB_OUTPUT
 *   (printed if not set)
 * - preview-comment.md: the sticky pull request comment body
 */
const fs = require('fs');

const DEFAULT_MODULE = 'flow-tests/test-default';

// Starts the pull request comment, so that each deployment updates the one
// comment instead of adding another
const COMMENT_MARKER = '<!-- flow-pr-preview -->';

// Caps the links listed in the comment, which a large refactoring could
// otherwise grow past what anyone reads.
const MAX_LINKS = 20;

// The flow-tests profiles the workflow builds the picked module with, next to
// the modules listed outside any profile. Kept to the ones the pull request
// validation runs, so that modules left to other builds, like the nightly
// one, are not deployed either. The install step in pr-preview.yml activates
// the same ones.
const REACTOR_PROFILES = ['it-test-modules', 'it-shared-modules'];

/** Names of the flow-tests modules in the reactor the workflow builds. */
function reactorModules(readSource) {
  const parent = readSource('flow-tests/pom.xml') || '';
  const profiles = parent.match(/<profile>[\s\S]*?<\/profile>/g) || [];
  const included = [parent.replace(/<profiles>[\s\S]*<\/profiles>/, '')].concat(
    profiles.filter((profile) => REACTOR_PROFILES.includes(profile.match(/<id>([^<]*)<\/id>/)?.[1]))
  );
  return included.flatMap((part) => [...part.matchAll(/<module>([^<]*)<\/module>/g)].map((match) => match[1]));
}

/**
 * How a test module is deployed, `{ module, contextPath }`, or null if it
 * can't be. Read from the poms, so that a new test module is picked up
 * without listing it anywhere: it has to be a module of flow-tests in the
 * reactor the workflow builds (see REACTOR_PROFILES), and a single application the
 * preview image can start - a war that its ITs run on Jetty, at a context
 * path the pom spells out, or a Spring Boot application. Anything else
 * (several wars, an application server, an aggregator) is left to the ITs.
 */
function deployment(module, readSource) {
  const name = module.slice('flow-tests/'.length);
  const pom = readSource(`${module}/pom.xml`);
  if (!pom || !reactorModules(readSource).includes(name)) {
    return null;
  }
  if (/<packaging>war<\/packaging>/.test(pom) && /<artifactId>jetty(-ee\d+)?-maven-plugin<\/artifactId>/.test(pom)) {
    const contextPath = (pom.match(/<contextPath>([^<]*)<\/contextPath>/) || [null, ''])[1].trim();
    // A context path set from a property differs between the builds of the
    // module, and the plain one is not necessarily what the ITs expect
    return contextPath.includes('${') ? null : { module, contextPath: contextPath.replace(/\/+$/, '') };
  }
  if (/<artifactId>spring-boot-maven-plugin<\/artifactId>/.test(pom)) {
    return { module, contextPath: '' };
  }
  return null;
}

/**
 * The deployment for the given changed files: test-default, unless they
 * change only other deployable test modules.
 */
function selectDeployment(changedFiles, readSource) {
  const counts = new Map();
  for (const file of changedFiles) {
    const module = file.match(/^flow-tests\/[^/]+(?=\/)/)?.[0];
    if (module) {
      counts.set(module, (counts.get(module) || 0) + 1);
    }
  }
  const candidates = [...counts.entries()]
    .filter(([module]) => deployment(module, readSource))
    .sort(([a, countA], [b, countB]) => countB - countA || a.localeCompare(b));
  const module = candidates.length === 0 || counts.has(DEFAULT_MODULE) ? DEFAULT_MODULE : candidates[0][0];
  return deployment(module, readSource) || { module: DEFAULT_MODULE, contextPath: '' };
}

/** Fully qualified class name of a Java source file, or null. */
function classNameOf(file) {
  const match = file.match(/\/src\/(?:main|test)\/java\/(.+)\.java$/);
  return match ? match[1].replace(/\//g, '.') : null;
}

/** The value of a Java string literal, or null if it is not one. */
function stringLiteral(expression) {
  return /^"(?:[^"\\]|\\.)*"$/.test(expression) ? JSON.parse(expression) : null;
}

/**
 * The path a view is served at, relative to the context path, or null if the
 * source is not a view or its path can't be told from the source.
 * `readSource(file)` returns the source or null.
 */
function viewPath(viewFile, viewServlet, readSource) {
  const source = readSource(viewFile);
  if (source === null || !(/@Route\b/.test(source) || /View\.java$/.test(viewFile))) {
    return null;
  }
  if (viewServlet) {
    // ViewTestServlet serves any view by its class name, which is also where
    // ChromeBrowserTest opens it
    return '/view/' + classNameOf(viewFile);
  }
  const route = source.match(/@Route\s*\(\s*(?:value\s*=\s*)?([^,)]+)/);
  if (!route) {
    return null;
  }
  let value = route[1].trim();
  // A route held in a constant of the view itself, like LoginView.ROUTE
  const constant = value.match(/^(?:\w+\.)?([A-Z_][A-Z0-9_]*)$/);
  if (constant) {
    const declaration = source.match(new RegExp(`\\b${constant[1]}\\s*=\\s*("[^"]*")`));
    value = declaration ? declaration[1] : '';
  }
  const literal = stringLiteral(value);
  return literal === null ? null : '/' + literal.replace(/^\//, '');
}

/**
 * The path an integration test opens, including the context path, when it
 * overrides getTestPath() with a string literal; null otherwise.
 */
function testPath(source) {
  const match = source.match(/String\s+getTestPath\s*\(\s*\)\s*\{\s*return\s+([^;]+);/);
  const literal = match ? stringLiteral(match[1].trim()) : null;
  return literal === null ? null : '/' + literal.replace(/^\//, '');
}

/** The source file of the view an integration test opens. */
function viewOfTest(testFile, source) {
  const module = testFile.slice(0, testFile.indexOf('/src/test/java/'));
  const testFor = source.match(/@TestFor\s*\(\s*(?:value\s*=\s*)?(\w+)\.class/);
  if (testFor) {
    const imported = source.match(new RegExp(`import\\s+([\\w.]+\\.${testFor[1]})\\s*;`));
    const className = imported ? imported[1] : classNameOf(testFile).replace(/\w+$/, testFor[1]);
    return `${module}/src/main/java/${className.replace(/\./g, '/')}.java`;
  }
  return testFile.replace('/src/test/java/', '/src/main/java/').replace(/IT\.java$/, 'View.java');
}

/**
 * Paths, including the context path, of the views worth linking to: the ones
 * the pull request changes and the ones its changed integration tests open.
 */
function viewPaths({ module, contextPath }, changedFiles, readSource) {
  const viewServlet = (readSource(`${module}/pom.xml`) || '').includes('<artifactId>flow-test-common</artifactId>');
  const paths = new Set();
  const addView = (view) => {
    const path = viewPath(view, viewServlet, readSource);
    if (path) {
      paths.add(contextPath + path);
    }
  };
  for (const file of changedFiles) {
    if (!file.startsWith(module + '/') || !file.endsWith('.java')) {
      continue;
    }
    if (file.includes('/src/main/java/')) {
      addView(file);
    } else if (file.endsWith('IT.java')) {
      const source = readSource(file);
      if (source !== null) {
        const path = testPath(source);
        if (path) {
          paths.add(path);
        } else {
          addView(viewOfTest(file, source));
        }
      }
    }
  }
  return [...paths].sort();
}

/** The pull request comment body. */
function comment({ module, contextPath }, paths, previewUrl) {
  const root = previewUrl + contextPath + '/';
  const lines = [COMMENT_MARKER, '', '## 🚀 Preview deployment', '', `Deployed \`${module}\`: ${root}`];
  if (paths.length > 0) {
    lines.push('', 'Views changed in this pull request:', '');
    for (const viewUrl of paths.slice(0, MAX_LINKS)) {
      lines.push(`- ${previewUrl}${viewUrl.replace(/ /g, '%20')}`);
    }
    if (paths.length > MAX_LINKS) {
      lines.push(`- …and ${paths.length - MAX_LINKS} more`);
    }
  }
  lines.push(
    '',
    '_Only one test module is deployed per pull request: `test-default`, ' +
      'unless the pull request changes only other test modules._',
    ''
  );
  return lines.join('\n');
}

function main() {
  const previewUrl = (process.argv[2] || '').replace(/\/+$/, '');
  const changedFiles = fs
    .readFileSync(0, 'utf8')
    .split('\n')
    .map((file) => file.trim())
    .filter(Boolean);
  const readSource = (file) => (fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : null);

  const selected = selectDeployment(changedFiles, readSource);
  const paths = viewPaths(selected, changedFiles, readSource);
  fs.writeFileSync('preview-comment.md', comment(selected, paths, previewUrl));
  const output = `module=${selected.module}\n` + `context-path=${selected.contextPath}\n`;
  if (process.env.GITHUB_OUTPUT) {
    fs.appendFileSync(process.env.GITHUB_OUTPUT, output);
  } else {
    process.stdout.write(output);
  }
}

if (require.main === module) {
  main();
}

module.exports = { selectDeployment, viewPaths, comment, DEFAULT_MODULE };
