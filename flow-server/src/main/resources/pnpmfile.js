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
      if (dependencies[key]) {
        console.log(" === " + key);
        if (compareVersions(dependencies[key], versions[key]) === -1) {
          // only rewrite to versions version if dependency older
          console.debug("Updating " + key + " to " + versions[key] + " from " + pkg.dependencies[key]);
          pkg.dependencies[key] = versions[key];
        } else if (dependencies[key].startsWith('~') || dependencies[key].startsWith('^')) {
          // Do not store versions with ^ or ~
          pkg.dependencies[key] = normalize(dependencies[key]);
        }
      }
    }
  }

  return pkg;
}

const semver = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$/i;

function indexOrEnd(str, q) {
  return str.indexOf(q) === -1 ? str.length : str.indexOf(q);
}

function normalize(version) {
  if (version.startsWith('~')) {
    return version.replace('~', '');
  }
  if (version.startsWith('^')) {
    return version.replace('^', '');
  }
  return version;
}

function split(version) {
  const cleaned = version.replace(/^v/, '').replace(/\+.*$/, '');
  const patchIndex = indexOrEnd(cleaned, '-');
  const arr = cleaned.substring(0, patchIndex).split('.');
  arr.push(cleaned.substring(patchIndex + 1));
  return arr;
}

function tryParse(v) {
  return isNaN(Number(v)) ? v : Number(v);
}

function validate(version) {
  if (typeof version !== 'string') {
    throw new TypeError('Invalid argument expected string got ' + version);
  }
  if (!semver.test(version)) {
    throw new Error('Invalid argument not valid semver (\'' + version + '\' received)');
  }
}

function compareVersions(version1, version2) {
  version1 = normalize(version1);
  version2 = normalize(version2);
  [version1, version2].forEach(validate);

  const v1Array = split(version1);
  const v2Array = split(version2);

  for (let i = 0; i < Math.max(v1Array.length - 1, v2Array.length - 1); i++) {
    const number1 = parseInt(v1Array[i] || 0, 10);
    const number2 = parseInt(v2Array[i] || 0, 10);

    if (number1 > number2) return 1;
    if (number2 > number1) return -1;
  }

  const sp1 = v1Array[v1Array.length - 1];
  const sp2 = v2Array[v2Array.length - 1];

  if (sp1 && sp2) {
    const p1 = sp1.split('.').map(tryParse);
    const p2 = sp2.split('.').map(tryParse);

    for (let i = 0; i < Math.max(p1.length, p2.length); i++) {
      if (p1[i] === undefined || typeof p2[i] === 'string' && typeof p1[i] === 'number') return -1;
      if (p2[i] === undefined || typeof p1[i] === 'string' && typeof p2[i] === 'number') return 1;

      if (p1[i] > p2[i]) return 1;
      if (p2[i] > p1[i]) return -1;
    }
  } else if (sp1 || sp2) {
    return sp1 ? -1 : 1;
  }

  return 0;
}
