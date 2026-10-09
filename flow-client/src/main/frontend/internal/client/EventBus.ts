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
 * The events fired through an {@link EventBus}, by type.
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
export interface EventMap {
  'vaadin-request': CustomEvent<VaadinRequest>;
  'vaadin-server-message': CustomEvent<VaadinServerMessage>;
}

/**
 * The part of an {@link EventBus} that page scripts get: they can listen to the
 * engine's events, but not fire them.
 */
export type EventBusListeners = Pick<EventBus, 'addEventListener' | 'removeEventListener'>;

/**
 * An event bus for one client engine. The engine fires its events through it,
 * and its {@link EventBus.asListeners | listener methods} are published as
 * `window.Vaadin.Flow.clients[appId].eventBus` so that page scripts can follow
 * the requests the client sends and the messages it receives, for example to
 * measure them:
 *
 * ```js
 * client.eventBus.addEventListener('vaadin-request', ({ detail: request }) => {
 *   let sentAt;
 *   request.addEventListener('sent', () => (sentAt = performance.now()));
 *   request.addEventListener('response', () => console.log(`round trip ${performance.now() - sentAt} ms`));
 *   request.addEventListener('end', () => console.log(`request ${request.outcome}`));
 * });
 * ```
 *
 * The engine coordinates its own work through direct calls, never through
 * these events, so firing an event does not make the engine do anything.
 * Listeners run synchronously inside the engine's work: keep them cheap and do
 * not call back into the client from them. Heartbeats and the overall state of
 * the client are not covered.
 */
export class EventBus extends TypedEventTarget<EventMap> {
  /**
   * Fires an event to the listeners of its type.
   *
   * @param type - the event type
   * @param detail - the detail the event carries
   * @typeParam K - the event type
   */
  fireEvent<K extends keyof EventMap>(type: K, detail: EventMap[K]['detail']): void {
    this.dispatchEvent(new CustomEvent(type, { detail }));
  }

  /**
   * Gets a view of this bus that only adds and removes listeners, for code
   * outside the engine that must not fire the engine's events.
   *
   * @returns the listener methods of this bus
   */
  asListeners(): EventBusListeners {
    return {
      addEventListener: this.addEventListener.bind(this),
      removeEventListener: this.removeEventListener.bind(this)
    };
  }
}
