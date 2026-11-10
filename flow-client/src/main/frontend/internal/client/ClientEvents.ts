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

import { TypedEventTarget } from './TypedEventTarget';
import type { VaadinRequest } from './communication/VaadinRequest';
import type { VaadinServerMessage } from './communication/VaadinServerMessage';

/**
 * The events fired through {@link ClientEvents}, by type.
 *
 * - `vaadin-request`: a request to the server is announced, when the first
 *   invocation for it is queued, or right before it is first sent if it has no
 *   invocations (such as a resynchronization). The {@link VaadinRequest} in the
 *   detail then tracks its delivery.
 * - `vaadin-server-message`: raw data arrived from the server, before it is
 *   parsed. The {@link VaadinServerMessage} in the detail then tracks how the
 *   client handles it. It covers both replies and messages the server pushes on
 *   its own.
 */
export interface ClientEventMap {
  'vaadin-request': CustomEvent<VaadinRequest>;
  'vaadin-server-message': CustomEvent<VaadinServerMessage>;
}

/**
 * The events of one client engine, published as
 * `window.Vaadin.Flow.clients[appId].events` so that page scripts can follow
 * the requests the client sends and the messages it receives, for example to
 * measure them:
 *
 * ```js
 * client.events.addEventListener('vaadin-request', ({ detail: request }) => {
 *   let sentAt;
 *   request.addEventListener('sent', () => (sentAt = performance.now()));
 *   request.addEventListener('response', () => console.log(`round trip ${performance.now() - sentAt} ms`));
 *   request.addEventListener('end', () => console.log(`request ${request.outcome}`));
 * });
 * ```
 *
 * It is a full `EventTarget`, so page scripts can also dispatch events of their
 * own on it, for example synthetic `vaadin-request` or `vaadin-server-message`
 * events to unit-test their listeners. The engine coordinates its own work through direct calls and never
 * listens to these events, so dispatching one does not make the engine do
 * anything.
 *
 * Listeners run synchronously inside the engine's work: keep them cheap and do
 * not call back into the client from them. Heartbeats and the overall state of
 * the client are not covered.
 */
export class ClientEvents extends TypedEventTarget<ClientEventMap> {}
