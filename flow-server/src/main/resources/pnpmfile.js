/**
 * NOTICE: this is an auto-generated file
 *
 * This file has been generated for `pnpm install` task.
 * It is used to pin client side dependencies.
 * This file will be overwritten on every run.
 */

const fs = require('fs');

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
  const {dependencies, devDependencies} = pkg;
  
  if (dependencies) {
    for (let k in versions.dependencies) {
      const packageVersion = dependencies[k];
      const forcedVersion = versions.dependencies[k];
      if (packageVersion && packageVersion !== forcedVersion) {
        pkg.dependencies[k] = forcedVersion;
      }
    }
  }

  if (devDependencies) {
    for (let k in versions.devDependencies) {
      const packageVersion = devDependencies[k];
      const forcedVersion = versions.devDependencies[k];
      if (packageVersion && packageVersion !== forcedVersion) {
        pkg.devDependencies[k] = forcedVersion;
      }
    }
  }

  // Forcing chokidar version for now until new babel version is available
  // check out https://github.com/babel/babel/issues/11488
  if (pkg.dependencies.chokidar) {
    pkg.dependencies.chokidar = '^3.4.0';
  }

  return pkg;
}
