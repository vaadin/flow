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
 * An `EventTarget` whose `addEventListener` and `removeEventListener` check the
 * event types listed in an event map, and the event their listeners get.
 *
 * It is a plain `EventTarget` otherwise, so options such as `once` and
 * `signal` work, and an error thrown by a listener is reported like any other
 * uncaught error without stopping the code that dispatched the event or the
 * other listeners.
 *
 * @typeParam M - the event types, mapped to the event their listeners get
 */
export class TypedEventTarget<M extends { [K in keyof M]: Event }> extends EventTarget {
  /**
   * Adds a listener for an event type.
   *
   * @param type - the event type
   * @param listener - the listener, called with the event
   * @param options - the standard `addEventListener` options
   * @typeParam K - the event type
   */
  override addEventListener<K extends keyof M & string>(
    type: K,
    listener: (event: M[K]) => void,
    options?: boolean | AddEventListenerOptions
  ): void;
  override addEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject | null,
    options?: boolean | AddEventListenerOptions
  ): void;
  override addEventListener(
    type: string,
    listener: ((event: never) => void) | EventListenerObject | null,
    options?: boolean | AddEventListenerOptions
  ): void {
    // The typed overload narrows the event its listener gets, which a plain
    // EventListener does not; the target passes the event of the type anyway.
    super.addEventListener(type, listener as EventListenerOrEventListenerObject | null, options);
  }

  /**
   * Removes a listener added with {@link TypedEventTarget.addEventListener}.
   *
   * @param type - the event type
   * @param listener - the listener to remove
   * @param options - the standard `removeEventListener` options
   * @typeParam K - the event type
   */
  override removeEventListener<K extends keyof M & string>(
    type: K,
    listener: (event: M[K]) => void,
    options?: boolean | EventListenerOptions
  ): void;
  override removeEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject | null,
    options?: boolean | EventListenerOptions
  ): void;
  override removeEventListener(
    type: string,
    listener: ((event: never) => void) | EventListenerObject | null,
    options?: boolean | EventListenerOptions
  ): void {
    // The typed overload narrows the event its listener gets, which a plain
    // EventListener does not; the target passes the event of the type anyway.
    super.removeEventListener(type, listener as EventListenerOrEventListenerObject | null, options);
  }
}
