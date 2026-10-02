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

/** The detail of the event fired when the client tries to reconnect. */
export interface ReconnectionAttemptDetail {
  /** The number of the reconnection attempt, starting from 1. */
  readonly attempt: number;
}

/**
 * The events fired through an {@link EventBus}, by type.
 *
 * - `vaadin-request-start`: a request is sent to the server.
 * - `vaadin-response-start`: the client starts handling a message from the
 *   server, before it applies the changes.
 * - `vaadin-request-end`: the request is done: its response has been applied,
 *   or the request failed and the client has given up on it.
 * - `vaadin-reconnection-attempt`: the client tries to reach the server again
 *   after losing the connection.
 */
export interface EventMap {
  'vaadin-request-start': CustomEvent<undefined>;
  'vaadin-response-start': CustomEvent<undefined>;
  'vaadin-request-end': CustomEvent<undefined>;
  'vaadin-reconnection-attempt': CustomEvent<ReconnectionAttemptDetail>;
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
 * what the engine does:
 *
 * ```js
 * client.eventBus.addEventListener('vaadin-request-end', () => console.log('request done'));
 * ```
 *
 * It is a plain `EventTarget`, so listeners are added and removed with the
 * standard DOM methods and options such as `once` and `signal` work. An error
 * thrown by a listener is reported like any other uncaught error and does not
 * stop the engine or the other listeners. {@link EventMap} lists the event
 * types.
 */
export class EventBus extends EventTarget {
  /**
   * Adds a listener for an event type. The type and the event the listener
   * gets are checked for the types listed in {@link EventMap}.
   *
   * @param type - the event type
   * @param listener - the listener, called with the event
   * @param options - the standard `addEventListener` options
   * @typeParam K - the event type
   */
  override addEventListener<K extends keyof EventMap>(
    type: K,
    listener: (event: EventMap[K]) => void,
    options?: boolean | AddEventListenerOptions
  ): void;
  override addEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject | null,
    options?: boolean | AddEventListenerOptions
  ): void;
  override addEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject | null,
    options?: boolean | AddEventListenerOptions
  ): void {
    super.addEventListener(type, listener, options);
  }

  /**
   * Removes a listener added with {@link EventBus.addEventListener}.
   *
   * @param type - the event type
   * @param listener - the listener to remove
   * @param options - the standard `removeEventListener` options
   * @typeParam K - the event type
   */
  override removeEventListener<K extends keyof EventMap>(
    type: K,
    listener: (event: EventMap[K]) => void,
    options?: boolean | EventListenerOptions
  ): void;
  override removeEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject | null,
    options?: boolean | EventListenerOptions
  ): void;
  override removeEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject | null,
    options?: boolean | EventListenerOptions
  ): void {
    super.removeEventListener(type, listener, options);
  }

  /**
   * Fires an event to the listeners of its type.
   *
   * @param type - the event type
   * @param detail - the detail the event carries, if its type has one
   * @typeParam K - the event type
   */
  fireEvent<K extends keyof EventMap>(type: K, detail?: EventMap[K]['detail']): void {
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
