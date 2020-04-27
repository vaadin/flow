/**
 * NOTICE: this is an auto-generated file
 *
 * This file has been generated for `pnpm install` task.
 * It is used to pin client side dependencies.
 * This file will be overwritten on every run.
 */

const fs = require('fs');

const packageJson = JSON.parse(fs.readFileSync('./package.json', 'utf-8'));

const versionsFile = '[to-be-generated-by-flow]';

if (!fs.existsSync(versionsFile)) {
  return;
}
const versions = JSON.parse(fs.readFileSync(versionsFile, 'utf-8'));

module.exports = {
  hooks: {
    readPackage
  }
};

function readPackage(pkg) {
  const {dependencies} = pkg;

  if (dependencies) {
    for (let key in versions) {
      if (!dependencies[key]) {
        pkg.dependencies[key] = versions[key];
      }
    }
  }

  return pkg;
}
