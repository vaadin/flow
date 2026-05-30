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

// Copies the hand-written ES-module entry points (`FlowClient.js`,
// `FlowBootstrap.js`, and their `.d.ts` companions) from the source tree into
// `target/classes/META-INF/frontend/`, sitting next to the tsc-emitted .js
// outputs so flow-server can pick up the complete entry surface. This used to
// live in `scripts/client.js`, which also wrapped the GWT-compiled bundle —
// after the bootstrap chain was migrated to TS, only the copy step remains.
const fs = require('fs');

const sourceDir = 'src/main/frontend/';
const targetDir = 'target/classes/META-INF/frontend/';

fs.mkdirSync(targetDir, { recursive: true });
fs.readdirSync(sourceDir)
  .filter((s) => s.endsWith('.d.ts') || s.endsWith('.js'))
  .forEach((file) => fs.copyFileSync(sourceDir + file, targetDir + file));
