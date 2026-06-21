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
 * Typed contracts for the Java classes that the GWT-compiled flow-client bundle
 * exports to JavaScript through JsInterop.
 *
 * Exported classes are published under `window.Vaadin.Flow.internal.<Name>` and
 * only exist after the bundle's `init()` (see `FlowClient.js`) has run. As the
 * client engine is migrated from Java to TypeScript (see
 * `MIGRATION_STRATEGY.md`), the new TypeScript calls *into* these exports — the
 * boundary is always TypeScript → GWT, never the reverse. These interfaces type
 * the exported instances so callers never reach into `window` untyped.
 */

/**
 * Mirrors the getters exported from `com.vaadin.client.ApplicationConfiguration`
 * — the translated application configuration read from the DOM at startup.
 */
export interface ApplicationConfiguration {
  getApplicationId(): string;
  getUIId(): number;
  isProductionMode(): boolean;
  isRequestTiming(): boolean;
  getServletVersion(): string;
  getExportedWebComponents(): string[];
}

/**
 * Mirrors the client-API methods exported from
 * `com.vaadin.client.ApplicationConnection` — the operations published on
 * `window.Vaadin.Flow.clients[appId]`.
 *
 * Note: the constructor is intentionally not exported (`@JsIgnore` on the Java
 * side) — exporting a member whose body builds the engine pulls a `GWT.create`
 * rebind into the JsInterop export pass, which the GWT compiler cannot resolve.
 * The instance is therefore created on the GWT side; TypeScript only consumes
 * the exported methods below.
 */
export interface ApplicationConnection {
  isActive(): boolean;
  getByNodeId(nodeId: number): Node | null;
  getNodeId(element: Element): number;
  addDomBindingListener(nodeId: number, callback: () => void): void;
  poll(): void;
  resolveUri(uri: string): string;
  sendEventMessage(nodeId: number, eventType: string, eventData: object | null): void;
  getUIId(): number;
  connectWebComponent(eventData: object): void;
  debug(): object;
  getJavaClass(nodeId: number): string | null;
  isHiddenByServer(nodeId: number): boolean;
  getElementStyleProperties(nodeId: number): object;
  getProfilingData(): unknown[];
  start(initialUidl: object | null): void;
}
