#!/usr/bin/env node
// Finds the integration test views a pull request touches, for pr-preview.yml.
// Takes a file with the changed paths, one per line, and prints one line per
// view: the module directory, the view class and its route, separated by tabs.
//
//   node .github/scripts/pr-preview-views.mjs changed.txt
//
// A view counts when the pull request changes it, or when it changes the
// integration test that goes with it by the naming convention of
// ViewOrUITest: FooIT in src/test/java tests FooView in src/main/java.

import fs from 'node:fs';
import path from 'node:path';

const SOURCE = /^(flow-tests\/.+?)\/src\/(main|test)\/java\/(.+)\.java$/;

const changed = fs.readFileSync(process.argv[2], 'utf8').split('\n').filter(Boolean);
const views = new Map();

for (const file of changed) {
  const match = file.match(SOURCE);
  if (!match) {
    continue;
  }
  const [, module, sourceSet, className] = match;
  const viewClass = sourceSet === 'main' ? className : className.replace(/IT$/, 'View');
  if (sourceSet === 'test' && viewClass === className) {
    continue;
  }
  const viewFile = path.join(module, 'src/main/java', `${viewClass}.java`);
  const route = fs.existsSync(viewFile) && readRoute(viewFile, path.basename(viewClass));
  if (route !== false && route !== undefined) {
    views.set(viewFile, [module, viewClass.replaceAll('/', '.'), route]);
  }
}

for (const view of views.values()) {
  console.log(view.join('\t'));
}

// Returns the route of a class annotated with @Route, or undefined for any
// other class. Without an explicit value the route is derived the way Flow
// derives it: the class name without a View suffix, lower case, and the empty
// route for Main and MainView.
function readRoute(file, simpleName) {
  const source = fs.readFileSync(file, 'utf8');
  const annotation = source.match(/^@Route\b(?:\s*\(([^)]*)\))?/m);
  if (!annotation) {
    return undefined;
  }
  const value = (annotation[1] ?? '').match(/^\s*(?:value\s*=\s*)?"([^"]*)"/);
  if (value) {
    return value[1];
  }
  const name = simpleName.replace(/View$/, '');
  return name === 'Main' ? '' : name.toLowerCase();
}
