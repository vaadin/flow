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
 * Type contracts for the client API. `ApplicationConnection` is the public
 * surface of the running client engine; `ApplicationConfiguration` is the
 * application configuration read from the DOM at startup. `publishClient` exposes
 * an `ApplicationConnection` on `window.Vaadin.Flow.clients[appId]`.
 */

/** The application configuration read from the DOM at startup. */
export interface ApplicationConfiguration {
  getApplicationId(): string;
  getUIId(): number;
  isProductionMode(): boolean;
  isRequestTiming(): boolean;
  getServletVersion(): string;
  getExportedWebComponents(): string[];
}

/**
 * The client-API methods published on `window.Vaadin.Flow.clients[appId]` — the
 * public surface of the running client engine.
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
