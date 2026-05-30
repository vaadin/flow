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

// Thin ES-module entry point that drives the migrated TS Bootstrapper.
// `Flow.ts` dynamic-imports this module after publishing the TS modules under
// `window.Vaadin.Flow.internal.*` (see `internal/bridge.ts`), so the namespace
// lookup is guaranteed to be populated by the time `init()` runs.
//
// The previous incarnation was the 128 KB GWT-compiled ClientEngine bundle
// produced by `gwt-maven-plugin`. After the bootstrap chain was migrated to
// TS, the GWT compile is no longer required, and this file is the entire
// runtime contract between `Flow.ts` and the migrated client engine.
export function init() {
  window.Vaadin.Flow.internal.client.bootstrap.Bootstrapper.initModule();
}
