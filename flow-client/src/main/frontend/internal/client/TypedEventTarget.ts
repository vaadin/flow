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
 * An `EventTarget` whose `addEventListener`, `removeEventListener` and
 * `dispatchEvent` check the event types listed in an event map, and the event
 * their listeners get.
 *
 * It is only a type over a plain `EventTarget`, so options such as `once` and
 * `signal` work, and an error thrown by a listener is reported like any other
 * uncaught error without stopping the code that dispatched the event or the
 * other listeners.
 *
 * @typeParam M - the event types, mapped to the event their listeners get
 */
export interface TypedEventTarget<M extends { [K in keyof M]: Event }> extends EventTarget {
  /**
   * Adds a listener for an event type.
   *
   * @param type - the event type
   * @param listener - the listener, called with the event
   * @param options - the standard `addEventListener` options
   * @typeParam K - the event type
   */
  addEventListener<K extends keyof M & string>(
    type: K,
    listener: (event: M[K]) => void,
    options?: boolean | AddEventListenerOptions
  ): void;
  addEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject | null,
    options?: boolean | AddEventListenerOptions
  ): void;

  /**
   * Removes a listener added with {@link TypedEventTarget.addEventListener}.
   *
   * @param type - the event type
   * @param listener - the listener to remove
   * @param options - the standard `removeEventListener` options
   * @typeParam K - the event type
   */
  removeEventListener<K extends keyof M & string>(
    type: K,
    listener: (event: M[K]) => void,
    options?: boolean | EventListenerOptions
  ): void;
  removeEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject | null,
    options?: boolean | EventListenerOptions
  ): void;

  /**
   * Dispatches an event of one of the types in the event map to the listeners
   * of its type.
   *
   * @param event - the event to dispatch
   * @returns `false` if the event is cancelable and a listener canceled it,
   *          `true` otherwise
   */
  dispatchEvent(event: M[keyof M]): boolean;
}

/**
 * The constructor of a {@link TypedEventTarget}: the plain `EventTarget`
 * constructor, typed, so the typing adds nothing at runtime.
 */
export const TypedEventTarget = EventTarget as unknown as new <
  M extends { [K in keyof M]: Event }
>() => TypedEventTarget<M>;
