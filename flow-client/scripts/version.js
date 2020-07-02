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

const fs = require('fs');

// maven-resources-plugin writes the project version into this file
// when it copies resources from `src/main/resources` into `target/classes`
const versionFile = 'target/classes/version.txt';
const replaceVersionInFile = 'src/main/resources/META-INF/resources/frontend/form/index.ts';

const version = fs.readFileSync(versionFile, 'utf8');

const sources = fs.readFileSync(replaceVersionInFile, 'utf8')
  .replace(/\/\* updated-by-script \*\/ '(\w|[\-${}.])+'/,
    `/* updated-by-script */ '${version}'`);

fs.writeFileSync(replaceVersionInFile, sources, 'utf8');
