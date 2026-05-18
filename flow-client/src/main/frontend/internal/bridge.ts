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

// Central registry of TS modules migrated from the GWT codebase. Each entry
// gets published under `window.Vaadin.Flow.internal.<pkg>.<name>` so GWT code
// can reach it via `@JsType(isNative = true, namespace = ...)`. See
// MIGRATION.md for the full pattern.
import { ConnectionIndicator } from './client/ConnectionIndicator';
import { Console } from './client/Console';
import { ElementUtil } from './client/ElementUtil';
import { LitUtils } from './client/LitUtils';
import { PolymerUtils } from './client/PolymerUtils';
import { ReactUtils } from './client/ReactUtils';
import { WidgetUtil } from './client/WidgetUtil';
import { registerGwtBridge } from './registry';

/**
 * Publishes every migrated TS implementation into the `window.Vaadin.Flow.internal`
 * namespace. Idempotent and safe to call repeatedly; call before the GWT bundle
 * (`FlowClient.init()`) runs.
 *
 * `Flow.ts` calls this once at module load and again before each GWT init so
 * tests that wipe `window.Vaadin` between runs still see the bridge.
 */
export function installGwtBridge(): void {
  registerGwtBridge('client', 'ConnectionIndicator', ConnectionIndicator);
  registerGwtBridge('client', 'Console', Console);
  registerGwtBridge('client', 'ElementUtil', ElementUtil);
  registerGwtBridge('client', 'LitUtils', LitUtils);
  registerGwtBridge('client', 'PolymerUtils', PolymerUtils);
  registerGwtBridge('client', 'ReactUtils', ReactUtils);
  registerGwtBridge('client', 'WidgetUtil', WidgetUtil);
}

installGwtBridge();
