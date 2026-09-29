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

import type { EventRemover } from '../EventRemover';
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

/** Identifies the request a {@link RequestListener} is notified about. */
export interface RequestEvent {
  /**
   * The id of the request, unique within the UI. A request that is sent again,
   * because no response arrived in time, keeps its id.
   */
  readonly requestId: number;
}

/**
 * Listener for the requests the client sends to the server, registered through
 * `window.Vaadin.Flow.clients[appId].addRequestListener`. Every callback is
 * optional. The client sends one request at a time, but the next request may
 * start before the previous one is reported as ended; use
 * {@link RequestEvent.requestId} to pair the calls.
 */
export interface RequestListener {
  /** Called when a request is sent to the server. */
  requestStarted?(event: RequestEvent): void;
  /**
   * Called when the client starts handling the response to a request, before
   * it applies the changes. Messages the server sends on its own, e.g. through push, are not
   * reported.
   */
  responseReceived?(event: RequestEvent): void;
  /**
   * Called when the request is done: its response has been applied, or the
   * request failed and the client has given up on it.
   */
  requestEnded?(event: RequestEvent): void;
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
  addRequestListener(listener: RequestListener): EventRemover;
  start(initialUidl: ValueMap | null): void;
}
