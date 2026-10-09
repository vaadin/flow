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

import { TypedEventTarget } from '../TypedEventTarget';
import type { VaadinRequest } from './VaadinRequest';

/**
 * How the client handled a {@link VaadinServerMessage}.
 *
 * - `applied`: the client handled the message.
 * - `discarded`: the client dropped the message without handling it: it could
 *   not be parsed, it repeated a message already handled, it was dropped while
 *   waiting for an earlier message, or the application had stopped.
 */
export type VaadinServerMessageOutcome = 'applied' | 'discarded';

/**
 * The events a {@link VaadinServerMessage} dispatches, by type.
 *
 * - `parsed`: the message was parsed and identified;
 *   {@link VaadinServerMessage.request} tells which request it replies to.
 * - `start`: the client starts handling the message, in order and when no
 *   other work holds handling back, before it loads the dependencies the
 *   message brings.
 * - `end`: {@link VaadinServerMessage.outcome} says how the message was
 *   handled. Dispatched exactly once, as the last event.
 */
export interface VaadinServerMessageEventMap {
  parsed: Event;
  start: Event;
  end: Event;
}

// What a message records about its handling. It is kept outside the message,
// where only the functions below can change it, so the message itself stays
// read-only for page scripts.
interface MessageState {
  request?: VaadinRequest;
  outcome?: VaadinServerMessageOutcome;
}

const states = new WeakMap<VaadinServerMessage, MessageState>();

/**
 * One message from the server, tracked from the moment its raw data arrives
 * until the client has handled it. It covers both replies to a
 * {@link VaadinRequest} and messages the server pushes on its own.
 *
 * The client announces each message as the detail of a `vaadin-server-message`
 * event on `window.Vaadin.Flow.clients[appId].events`, before parsing it, and
 * then dispatches the events of {@link VaadinServerMessageEventMap} on it.
 *
 * The properties are read-only. Events dispatched on a message from outside
 * the client reach its listeners but change nothing in the message or the
 * client. To keep data of your own for a message, store it in a `WeakMap` keyed
 * by the message, so that it is released with the message.
 *
 * Listeners run synchronously inside the client's own work: keep them cheap
 * and do not call back into the client from them.
 */
export class VaadinServerMessage extends TypedEventTarget<VaadinServerMessageEventMap> {
  constructor() {
    super();
    states.set(this, {});
  }

  /**
   * The request this message replies to, set before `parsed`. `undefined` if
   * the server sent the message on its own, or if it repeats a reply to a
   * request the client no longer knows.
   */
  get request(): VaadinRequest | undefined {
    return states.get(this)!.request;
  }

  /** How the message was handled, set before `end`; `undefined` until then. */
  get outcome(): VaadinServerMessageOutcome | undefined {
    return states.get(this)!.outcome;
  }
}

// The functions below change a message on behalf of the client. Each does
// nothing once the message has ended, so `end` is always the last event.

/** The state of a message that has not ended yet, or `undefined` once it has. */
function getOpenState(message: VaadinServerMessage): MessageState | undefined {
  const state = states.get(message)!;
  return state.outcome === undefined ? state : undefined;
}

/**
 * Records the request the message replies to and dispatches `parsed`.
 *
 * @param message - the parsed message
 * @param request - the request the message replies to, or `undefined` if none
 */
export function dispatchParsed(message: VaadinServerMessage, request: VaadinRequest | undefined): void {
  const state = getOpenState(message);
  if (state) {
    state.request = request;
    message.dispatchEvent(new Event('parsed'));
  }
}

/**
 * Dispatches `start` as the client starts handling the message.
 *
 * @param message - the message being handled
 */
export function dispatchStart(message: VaadinServerMessage): void {
  if (getOpenState(message)) {
    message.dispatchEvent(new Event('start'));
  }
}

/**
 * Settles the message with the given outcome and dispatches `end`.
 *
 * @param message - the message to end
 * @param outcome - how the message was handled
 */
export function dispatchMessageEnd(message: VaadinServerMessage, outcome: VaadinServerMessageOutcome): void {
  const state = getOpenState(message);
  if (state) {
    state.outcome = outcome;
    message.dispatchEvent(new Event('end'));
  }
}
