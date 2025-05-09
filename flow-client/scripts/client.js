/*
 * Copyright 2000-2025 Vaadin Ltd.
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

const fs = require('fs');

const fromDir = "target/classes/META-INF/resources/VAADIN/static/client/";
const fromFileRegex = /^client-.*\.cache\.js$/;

const sourceDir = "src/main/frontend/";
const targetDir = "target/classes/META-INF/frontend/";
const toFile  = "FlowClient.js";

const fromFileName = fs.readdirSync(fromDir)
    .find(filename => fromFileRegex.exec(filename));

// Read from target
let clientSource = fs.readFileSync(fromDir + fromFileName, 'utf8');

// Wrap with ES module export
clientSource = `export function init() {
${clientSource}
};`;

// Write to source
fs.writeFileSync(sourceDir + toFile, clientSource, 'utf8');

// Write to target (copy '.d.ts' and '.js' files from sourceDir)
fs.mkdirSync(targetDir, {recursive: true});
fs.readdirSync(sourceDir)
  .filter(s => s.endsWith('.d.ts') || s.endsWith('.js'))
  .forEach(file => fs.copyFileSync(sourceDir + file, targetDir + file));
