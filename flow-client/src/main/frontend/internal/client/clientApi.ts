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

import type { publishClient } from './publishClient';
import type { ValueMap } from './ValueMap';

/**
 * Type contracts for the client API. {@link ApplicationConnection} is the public
 * surface of the running client engine; {@link ApplicationConfiguration} is the
 * application configuration read from the DOM at startup. {@link publishClient}
 * exposes an {@link ApplicationConnection} on `window.Vaadin.Flow.clients[appId]`.
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
 * What {@link publishClient} needs from the running engine to build
 * `window.Vaadin.Flow.clients[appId]`. The published keys are the ones the JSNI
 * blocks in ApplicationConnection.java define, so two of them (`getByNodeId`,
 * `addDomBindingListener`) differ from the engine method they call.
 */
export interface ApplicationConnection {
  isActive(): boolean;
  getDomElementByNodeId(nodeId: number): Node | null;
  getNodeId(element: Element): number;
  addDomSetListener(nodeId: number, callback: () => void): void;
  poll(): void;
  resolveUri(uri: string): string | null;
  sendEventMessage(nodeId: number, eventType: string, eventData: unknown): void;
  getUIId(): number;
  connectWebComponent(eventData: unknown): void;
  debug(): unknown;
  getJavaClass(nodeId: number): string | null;
  isHiddenByServer(nodeId: number): boolean;
  getElementStyleProperties(nodeId: number): Record<string, unknown>;
  getProfilingData(): number[];
  start(initialUidl: ValueMap | null): void;
}
