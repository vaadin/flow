/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
const args = require('minimist')(process.argv.slice(2));
const path = require('path');

const currentDir = process.cwd() + '/';

let targetDir;
/* Since we cannot get the target directory from maven as a parameter,
 * need to hard code it here when the flow-components script is triggered. */
if (args.flow_components) {
  targetDir = path.join(currentDir, '/../flow-generated-components/json_metadata/');
} else {
  targetDir = currentDir + (args.targetDir || 'generated_json').replace(/,+$/, "");
}
console.log(`Using target dir: ${targetDir}`);

/* This is either a custom bower resources directory or the default directory where to load WC files from. */
const bowerSrcDir = currentDir + (args.resourcesDir || 'bower_components').replace(/,+$/, "");
const skipInheritedAPI = args.skipInheritedAPI || true;
/* This is the file where the webcomponent dependencies are declared */
const dependenciesFile = currentDir + (args.dependenciesFile || 'bower.json').replace(/,+$/, "");

module.exports = {
  currentDir: currentDir,
  targetDir: targetDir,
  bowerSrcDir: bowerSrcDir,
  bowerPackages: args.package ? args.package.split(/[, ]+/) : null,
  skipInheritedAPI: skipInheritedAPI,
  dependenciesFile: dependenciesFile
};
