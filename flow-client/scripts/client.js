/*
 * Copyright 2000-2026 Vaadin Ltd.
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

const sourceDir = 'src/main/frontend/';
const targetDir = 'target/classes/META-INF/frontend/';
const toFile = 'FlowClient.js';

// FlowClient.init() is the client entry point that Flow.ts imports and calls. It
// runs the TypeScript bootstrap (onModuleLoad): it registers the widgetset start
// callback so the server bootstrap can start the application, which assembles and
// starts the TypeScript engine (ApplicationConnection). This replaces the former
// GWT engine bundle that used to be inlined here.
const clientSource = `import { onModuleLoad } from './internal/Bootstrapper';

export function init() {
  onModuleLoad();
}
`;

// Write to source
fs.writeFileSync(sourceDir + toFile, clientSource, 'utf8');

// Write to target (copy '.d.ts' and '.js' files from sourceDir)
fs.mkdirSync(targetDir, { recursive: true });
fs.readdirSync(sourceDir)
  .filter((s) => s.endsWith('.d.ts') || s.endsWith('.js'))
  .forEach((file) => fs.copyFileSync(sourceDir + file, targetDir + file));
