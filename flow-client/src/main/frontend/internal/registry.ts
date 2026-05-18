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

/**
 * Publishes a TS implementation under `window.Vaadin.Flow.internal.<pkg>.<name>`
 * so GWT-compiled Java code can reach it via
 * `@JsType(isNative = true, namespace = "Vaadin.Flow.internal.<pkg>", name = "<name>")`.
 *
 * Migrated TS modules call this at module-evaluation time. The central
 * `bridge.ts` imports every such module, and `Flow.ts` imports `bridge.ts`
 * statically at the top, so the namespace is populated before the
 * GWT bundle (`FlowClient.js`) is dynamically imported and `init()` is called.
 *
 * The `pkg` argument mirrors the Java package below `com.vaadin.client`
 * (for example `client` for `com.vaadin.client`, or `client.flow.reactive`
 * for `com.vaadin.client.flow.reactive`).
 */
export function registerGwtBridge(pkg: string, name: string, impl: unknown): void {
  const win = window as unknown as Record<string, any>;
  const vaadin = (win.Vaadin = win.Vaadin ?? {});
  const flow = (vaadin.Flow = vaadin.Flow ?? {});
  const internal = (flow.internal = flow.internal ?? {});
  let node: Record<string, any> = internal;
  for (const segment of pkg.split('.')) {
    node = node[segment] = node[segment] ?? {};
  }
  node[name] = impl;
}
