#!/usr/bin/env node
const exec = require('util').promisify(require('child_process').exec);
const fs = require("fs");

/****************** START CONFIG */
const globalExclusions = ['flow-tests'];
// Set modules or tests weights and fixed slice position for better distribution
//  weight: can be time in minutes, default 1
//  pos:    from 1 to parallel (default fastest based in weigth)
const moduleWeights = {
  'flow-client': {weight: 9},
  'flow-server': {weight: 4},
  'vaadin-dev-server': {weight: 3},
  'fusion-end-point': {weight: 3},
  'flow-tests/test-mixed/pom-pnpm-production.xml': {pos: 1},
  'flow-tests/test-root-ui-context': {pos: 2},
  'flow-tests/test-mixed/pom-npm-production.xml': {pos: 2},
  'flow-tests/test-pwa/pom-production.xml': {pos: 2},
  'flow-tests/test-ccdm-flow-navigation': {pos: 2},
  'flow-tests/test-fusion-csrf-context': {pos: 3},
  'flow-tests/test-v14-bootstrap': {pos: 4},
  'flow-tests/test-fusion-csrf': {pos: 5, weight: 20},
  'flow-tests/test-ccdm': {pos: 5},
  'flow-tests/test-pwa' : {pos: 5},
  'flow-tests/test-ccdm/pom-production.xml': {pos: 6},
  
  'RemoveRoutersLayoutContentIT': {weight: 2},
  'BrowserWindowResizeIT': {weight: 2},
  'FragmentLinkIT': {weight: 2},
  'AttachListenerIT': {weight: 2},
  'template.ChildOrderIT': {weight: 2},
  'WaitForVaadinIT': {weight: 2},
  'ErrorPageIT': {weight: 2},
  'DomEventFilterIT': {weight: 2},
  'ShortcutsIT': {weight: 2},
  'JavaScriptReturnValueIT': {weight: 8},
}
// Set split number for modules with several tests
const moduleSplits = {
  'flow-tests/test-root-context': 3
}
/****************** END CONFIG */

// Using regex to avoid having to run `npm install` for xml libs.
const regexComment = /<!--[\s\S]+?-->/gm;
const regexModule = /([\s\S]*?)<module>([\s\S]*?)<\/module>([\s\S]*)/;
const regexVersion = '(<version>)(VERSION)(</version>)';

/**
 * 10 seconds faster than `mvn help:evaluate -Dexpression=project.modules`
 */
function getModules(pomFile, prefix) {
  prefix = prefix || '';
  const modules = [];
  
  const content = fs.readFileSync(prefix + pomFile).toString().replace(regexComment, '');
  let res = regexModule.exec(content);
  while(res) {
    modules.push(prefix + res[2]);
    res = regexModule.exec(res[3]);
  }
  return modules;
}

/**
 * Returns a list of files in a folder matching a pattern
 */
function getFiles(files, folder, pattern) {
  fs.readdirSync(folder).forEach(file => {
    file = folder + '/' + file;
    if (fs.lstatSync(file).isDirectory()) {
      files.concat(getFiles(files, file, pattern))
    } else if (pattern.test(file)) {
      files.push(file);
    }
  });
  return files;
}

/**
 * 30 seconds faster than `mvn versions:set -DnewVersion=...`
 */
function setVersion(newVersion) {
  const pomContent = fs.readFileSync('pom.xml').toString();
  const current = RegExp(regexVersion.replace('VERSION', '\\d+\\.\\d+\\-SNAPSHOT')).exec(pomContent)[2];
  if (current && current != newVersion) {
    const regexChangeVersion = RegExp(regexVersion.replace('VERSION', current), 'g');
    const files = getFiles([], '.', /.*\/pom.*\.xml$/);
    files.forEach(file => {
      console.log(`Replacing ${current} to ${newVersion} in ${file}`);
      const content = fs.readFileSync(file).toString();
      const newContent = content.replace(regexChangeVersion, '$1' + newVersion + '$3');
      fs.writeFileSync(file, newContent)
    });
  }
}

/**
 * return a list of file names removing path and extension
 */
function getTestFiles(folder, pattern) {
  return getFiles([], folder, pattern)
    .map(f => f.replace(/^.*\//, '').replace(/\..*?$/, '')).sort();
}

/**
 * remove excluded elements from array
 */
function grep(array, exclude) {
  return array.filter(item => !exclude.includes(item));
}

function sumWeights(items, slowMap) {
  return items.reduce((prev, curr) => prev + (slowMap[curr] && slowMap[curr].weight || 1), 0);
}

/**
 * return the slice with lower sum of weights
 */
function getFasterSlice(item, parts, slowMap) {
  return slowMap[item] && slowMap[item].pos && parts[slowMap[item].pos - 1]
    ? parts[slowMap[item].pos - 1]
    : parts.reduce((previous, current) =>
      sumWeights(previous, slowMap) < sumWeights(current, slowMap) ? previous : current,
      parts[0]);
}

/**
 * splits an array of modules in the desired slices based on weights defined in a map
 */
function splitArray(array, slices, slowMap) {
  const items = [...array];
  const nItems = items.length;
  slices = Math.min(slices, nItems);

  const parts = [...new Array(slices)].map(_ => []);

  const slows = Object.keys(slowMap)
    .sort((a,b) => slowMap[b].weight - slowMap[a].weight)
    .filter(e => items.includes(e));

  for (i = 0; i < slows.length; i++) {
    const item = slows[i];
    getFasterSlice(item, parts, slowMap).push(item);
    items.splice(items.indexOf(item), 1)
  }

  for (i = 0; i < items.length; i++) {
    const item = items[i];
    getFasterSlice(item, parts, slowMap).push(item);
  }
  return parts;
}

/**
 * convert the sliced data to a JS object
 */
function toObject(parts, suite, module, prevIdx) {
  const object = [];
  parts.forEach((items, idx, arr) => {
    const current = prevIdx + idx + 1;
    const total = arr.length;
    const name = `${suite} (${total}, ${current})`;
    const args = items.join(',');
    const weight = sumWeights(items, moduleWeights);
    const matrix = [arr.length, prevIdx + idx + 1]
    object.push({total, current, weight, suite, module, name, args, items, matrix});
  });
  return object;
}

/**
 * The main function to visit pom files and tests based on configuration and produce
 * the object json for actions
 */
function getParts(suite, prefix, slices) {
  let modules = getModules('pom.xml', prefix);
  const exclusions = Object.keys(moduleSplits).filter(module => modules.includes(module));
  modules = grep(modules, exclusions);

  const excSlices = exclusions.reduce((prev, module) => prev + moduleSplits[module], 0);
  const parts = splitArray(modules, slices - excSlices, moduleWeights);

  let object = toObject(parts, suite, '', 0);

  const testRegex = RegExp(`.*${suite == 'it-tests' ? 'IT' : 'Test'}.java`);

  exclusions.forEach(module => {
    const tests = getTestFiles(module + '/src/test/java', testRegex);
    const parts = splitArray(tests, moduleSplits[module], moduleWeights);
    object = [...object, ...toObject(parts, suite, module, object.length)];
  });
  return object;
}

/**
 * Produces a JSON string that works with GH actions
 */
function objectToString(object, keys) {
  if (keys && keys.length) {
    object.forEach(o => {
      Object.keys(o).forEach(k => {
        if (!keys.includes(k)) {
          delete o[k];
        }
      });
    });
  }
  return JSON.stringify({
    include: object
  }, null, 0)
}

function printStrategy(object) {
  const json = [];
  const o = object.map(o => {
    return {
      matrix: [o.suite, ...o.matrix, o.weight],
      module: o.module,
      items: o.module ? o.args : o.items
    }
  });
  console.error(o);
}

/*
 * The script entry-point
 */
async function main() {
  const versionRegx= /--version=(.*)/;
  const parallelRegx= /--parallel=(\d+)/;
  const program = process.argv[1].replace(/.*\//, ''); 
  const action = process.argv[2];
  const parameter = process.argv[3];
  const keys = process.argv.slice(4);

  if (action == 'set-version' && versionRegx.test(parameter)) {
    setVersion(parameter.replace(versionRegx, '$1'));
  } else if (action == 'unit-tests' && parallelRegx.test(parameter)) {
    const object = getParts(action, '', parameter.replace(parallelRegx, '$1'));
    printStrategy(object);
    const json = objectToString(object, keys);
    console.log(json);
  } else if (action == 'it-tests' && parallelRegx.test(parameter)) {
    const object = getParts(action, 'flow-tests/', parameter.replace(parallelRegx, '$1'));
    printStrategy(object);
    const json = objectToString(object, keys);
    console.log(json);
  } else {
    console.log(`
Usage:
  ${program} action parameters [keys]

Actions
  set-version        replace versions in all pom files of the project
  unit-tests         outputs the JSON matrix for unit-tests
  it-tests           outpurs the JSON matrix for it-tests

Parameters
  --version=xxx      the version to set
  --parallel=N       number of items for the matrix

Keys
  A comma separated list of keys for the matrix object.
  Valid keys are: current, suite, module, total, weight, name, args, items, matrix
   `);
    process.exit(1);
  }
}

main();


