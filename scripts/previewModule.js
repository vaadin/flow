#!/usr/bin/env node
/**
 * Picks the test module a pull request preview deployment serves, and writes
 * the pull request comment that links to it.
 *
 * Only one test module is deployed per pull request. flow-tests/test-default
 * is preferred: it is deployed whenever the pull request touches it, and also
 * when the pull request touches no deployable test module at all (a change to
 * the framework itself). Otherwise the deployable test module with the most
 * changed files is deployed, ties broken by name so the choice is stable
 * between pushes.
 *
 * Links are generated to the views the pull request adds or changes, and to
 * the views of the integration tests it adds or changes, the same way the
 * tests themselves resolve them: @TestFor, or the IT to View naming
 * convention; @Route if the view declares one, /view/<class name> otherwise.
 *
 * Usage: node scripts/previewModule.js <base ref> <preview url>
 *
 * Outputs:
 * - `module=<path>` appended to $GITHUB_OUTPUT (printed if not set)
 * - preview-comment.md: the sticky pull request comment body
 */
const fs = require('fs');
const { execFileSync } = require('child_process');

const DEFAULT_MODULE = 'flow-tests/test-default';

// Test modules that run as a single application with the default build, so
// the preview image can build and start them like the ITs do. Modules left
// out need a special setup (several wars, an application server, a custom
// frontend build) or only test the development tooling itself (live reload,
// redeployment, the dev loop), which a deployed preview cannot exercise.
const DEPLOYABLE_MODULES = {
  'flow-tests/test-default': { contextPath: '' },
  'flow-tests/test-root-context': { contextPath: '' },
  'flow-tests/test-ccdm': { contextPath: '/foo' },
  'flow-tests/test-ccdm-flow-navigation': { contextPath: '/context-path' },
  'flow-tests/test-client-queue': { contextPath: '' },
  'flow-tests/test-custom-route-registry': { contextPath: '' },
  'flow-tests/test-eager-bootstrap': { contextPath: '' },
  'flow-tests/test-legacy-frontend': { contextPath: '' },
  'flow-tests/test-misc': { contextPath: '' },
  'flow-tests/test-no-theme': { contextPath: '' },
  'flow-tests/test-pwa': { contextPath: '' },
  'flow-tests/test-pwa-disabled-offline': { contextPath: '' },
  'flow-tests/test-push-startup': { contextPath: '' },
  'flow-tests/test-react-adapter': { contextPath: '' },
  'flow-tests/test-router-custom-context': {
    contextPath: '/custom-context-router',
  },
  'flow-tests/test-servlet': { contextPath: '' },
  'flow-tests/test-tailwindcss': { contextPath: '' },
  'flow-tests/test-theme-no-polymer': { contextPath: '' },
  'flow-tests/test-themes': { contextPath: '' },
  'flow-tests/test-vaadin-router': { contextPath: '' },
  'flow-tests/test-webpush': { contextPath: '' },
};

// Caps the links listed in the comment, which a large refactoring could
// otherwise grow past what anyone reads.
const MAX_LINKS = 20;

/** The deployable test module a repository path belongs to, or null. */
function moduleOf(file) {
  return (
    Object.keys(DEPLOYABLE_MODULES).find((module) =>
      file.startsWith(module + '/'),
    ) || null
  );
}

/** The test module to deploy for the given changed files. */
function selectModule(changedFiles) {
  const counts = new Map();
  for (const file of changedFiles) {
    const module = moduleOf(file);
    if (module) {
      counts.set(module, (counts.get(module) || 0) + 1);
    }
  }
  if (counts.size === 0 || counts.has(DEFAULT_MODULE)) {
    return DEFAULT_MODULE;
  }
  return [...counts.entries()].sort(
    ([a, countA], [b, countB]) => countB - countA || a.localeCompare(b),
  )[0][0];
}

/** Fully qualified class name of a Java source file, or null. */
function classNameOf(file) {
  const match = file.match(/\/src\/(?:main|test)\/java\/(.+)\.java$/);
  return match ? match[1].replace(/\//g, '.') : null;
}

/**
 * The path a view is served at, relative to the context path, or null if the
 * source is not a view. `readSource(file)` returns the source or null.
 */
function viewPath(viewFile, readSource) {
  const source = readSource(viewFile);
  if (source === null || !/\bextends\s+\w+/.test(source)) {
    return null;
  }
  const route = source.match(/@Route\s*\(\s*(?:value\s*=\s*)?([^,)]+)/);
  if (route) {
    let value = route[1].trim();
    // A route held in a constant of the view itself, like LoginView.ROUTE
    const constant = value.match(/^(?:\w+\.)?([A-Z_][A-Z0-9_]*)$/);
    if (constant) {
      const declaration = source.match(
        new RegExp(`\\b${constant[1]}\\s*=\\s*("[^"]*")`),
      );
      value = declaration ? declaration[1] : null;
    }
    if (value && value.startsWith('"')) {
      return '/' + JSON.parse(value).replace(/^\//, '');
    }
  }
  if (/@Route\b/.test(source) && !route) {
    // @Route without a value is the class name without "View", which only
    // the Java side can derive reliably
    return null;
  }
  return '/view/' + classNameOf(viewFile);
}

/** The source file of the view an integration test opens, or null. */
function viewOfTest(testFile, readSource) {
  const source = readSource(testFile);
  if (source === null) {
    return null;
  }
  const module = testFile.slice(0, testFile.indexOf('/src/test/java/'));
  const testFor = source.match(/@TestFor\s*\(\s*(?:value\s*=\s*)?(\w+)\.class/);
  if (testFor) {
    const imported = source.match(
      new RegExp(`import\\s+([\\w.]+\\.${testFor[1]})\\s*;`),
    );
    const className = imported
      ? imported[1]
      : classNameOf(testFile).replace(/\w+$/, testFor[1]);
    return `${module}/src/main/java/${className.replace(/\./g, '/')}.java`;
  }
  return testFile
    .replace('/src/test/java/', '/src/main/java/')
    .replace(/IT\.java$/, 'View.java');
}

/** Paths (relative to the context path) of the views worth linking to. */
function viewPaths(module, changedFiles, readSource) {
  const views = new Set();
  for (const file of changedFiles) {
    if (!file.startsWith(module + '/') || !file.endsWith('.java')) {
      continue;
    }
    if (file.includes('/src/main/java/')) {
      views.add(file);
    } else if (file.endsWith('IT.java')) {
      const view = viewOfTest(file, readSource);
      if (view) {
        views.add(view);
      }
    }
  }
  const paths = new Set();
  for (const view of views) {
    const viewUrl = viewPath(view, readSource);
    if (viewUrl) {
      paths.add(viewUrl);
    }
  }
  return [...paths].sort();
}

/** The pull request comment body. */
function comment(module, paths, previewUrl) {
  const root = previewUrl + DEPLOYABLE_MODULES[module].contextPath;
  const lines = [
    '## 🚀 Preview deployment',
    '',
    `Deployed \`${module}\`: ${root}/`,
  ];
  if (paths.length > 0) {
    lines.push('', 'Views changed in this pull request:', '');
    for (const viewUrl of paths.slice(0, MAX_LINKS)) {
      lines.push(`- ${root}${encodeURI(viewUrl)}`);
    }
    if (paths.length > MAX_LINKS) {
      lines.push(`- …and ${paths.length - MAX_LINKS} more`);
    }
  }
  lines.push(
    '',
    '_Only one test module is deployed per pull request: `test-default`, ' +
      'unless the pull request changes only other test modules._',
    '',
  );
  return lines.join('\n');
}

function main() {
  const [baseRef = 'origin/main', previewUrl = ''] = process.argv.slice(2);
  const changedFiles = execFileSync(
    'git',
    ['diff', '--name-only', '--diff-filter=d', `${baseRef}...HEAD`],
    { encoding: 'utf8' },
  )
    .split('\n')
    .filter(Boolean);
  const readSource = (file) =>
    fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : null;

  const module = selectModule(changedFiles);
  const paths = viewPaths(module, changedFiles, readSource);
  fs.writeFileSync(
    'preview-comment.md',
    comment(module, paths, previewUrl.replace(/\/+$/, '')),
  );
  const output = `module=${module}\n`;
  if (process.env.GITHUB_OUTPUT) {
    fs.appendFileSync(process.env.GITHUB_OUTPUT, output);
  } else {
    process.stdout.write(output);
  }
}

if (require.main === module) {
  main();
}

module.exports = { selectModule, viewPaths, comment, DEFAULT_MODULE };
