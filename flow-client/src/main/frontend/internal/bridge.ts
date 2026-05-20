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
import { ApplicationConnection } from './client/ApplicationConnection';
import { BrowserInfo } from './client/BrowserInfo';
import { ConnectionIndicator } from './client/ConnectionIndicator';
import { Console } from './client/Console';
import { ElementUtil } from './client/ElementUtil';
import { ExistingElementMap } from './client/ExistingElementMap';
import { ExecuteJavaScriptElementUtils } from './client/ExecuteJavaScriptElementUtils';
import { LitUtils } from './client/LitUtils';
import { PolymerUtils } from './client/PolymerUtils';
import { Profiler } from './client/Profiler';
import { ReactUtils } from './client/ReactUtils';
import { ResourceLoader } from './client/ResourceLoader';
import { SystemErrorHandler } from './client/SystemErrorHandler';
import { WidgetUtil } from './client/WidgetUtil';
import { Bootstrapper } from './client/bootstrap/Bootstrapper';
import { AtmospherePushConnection } from './client/communication/AtmospherePushConnection';
import { MessageHandler } from './client/communication/MessageHandler';
import { MessageSender } from './client/communication/MessageSender';
import { XhrConnection } from './client/communication/XhrConnection';
import { ExecuteJavaScriptProcessor } from './client/flow/ExecuteJavaScriptProcessor';
import { SimpleElementBindingStrategy } from './client/flow/binding/SimpleElementBindingStrategy';
import { UpdatableModelProperties } from './client/flow/model/UpdatableModelProperties';
import { Computation } from './client/flow/reactive/Computation';
import { Reactive } from './client/flow/reactive/Reactive';
import { ReactiveEventRouter } from './client/flow/reactive/ReactiveEventRouter';
import { ClientJsonCodec } from './client/flow/util/ClientJsonCodec';
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
  registerGwtBridge('client', 'ApplicationConnection', ApplicationConnection);
  registerGwtBridge('client', 'BrowserInfo', BrowserInfo);
  registerGwtBridge('client', 'ConnectionIndicator', ConnectionIndicator);
  registerGwtBridge('client', 'Console', Console);
  registerGwtBridge('client', 'ElementUtil', ElementUtil);
  registerGwtBridge('client', 'ExecuteJavaScriptElementUtils', ExecuteJavaScriptElementUtils);
  registerGwtBridge('client', 'ExistingElementMap', ExistingElementMap);
  registerGwtBridge('client', 'LitUtils', LitUtils);
  registerGwtBridge('client', 'PolymerUtils', PolymerUtils);
  registerGwtBridge('client', 'Profiler', Profiler);
  registerGwtBridge('client', 'ReactUtils', ReactUtils);
  registerGwtBridge('client', 'ResourceLoader', ResourceLoader);
  registerGwtBridge('client', 'SystemErrorHandler', SystemErrorHandler);
  registerGwtBridge('client', 'WidgetUtil', WidgetUtil);
  registerGwtBridge('client.bootstrap', 'Bootstrapper', Bootstrapper);
  registerGwtBridge('client.communication', 'AtmospherePushConnection', AtmospherePushConnection);
  registerGwtBridge('client.communication', 'MessageHandler', MessageHandler);
  registerGwtBridge('client.communication', 'MessageSender', MessageSender);
  registerGwtBridge('client.communication', 'XhrConnection', XhrConnection);
  registerGwtBridge('client.flow', 'ExecuteJavaScriptProcessor', ExecuteJavaScriptProcessor);
  registerGwtBridge('client.flow.binding', 'SimpleElementBindingStrategy', SimpleElementBindingStrategy);
  registerGwtBridge('client.flow.model', 'UpdatableModelProperties', UpdatableModelProperties);
  registerGwtBridge('client.flow.reactive', 'Computation', Computation);
  registerGwtBridge('client.flow.reactive', 'Reactive', Reactive);
  registerGwtBridge('client.flow.reactive', 'ReactiveEventRouter', ReactiveEventRouter);
  registerGwtBridge('client.flow.util', 'ClientJsonCodec', ClientJsonCodec);
}

installGwtBridge();
