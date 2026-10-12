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
import type { VaadinServerMessage } from './VaadinServerMessage';

/** Why an attempt to deliver a {@link VaadinRequest} failed. */
export interface VaadinRequestFailure {
  /**
   * - `network`: the attempt did not reach the server, or the connection was
   *   lost before a reply arrived.
   * - `timeout`: no reply arrived in time, so the client sends the request
   *   again.
   * - `http`: the server replied with an HTTP status other than 200, given in
   *   {@link VaadinRequestFailure.status}.
   * - `invalid-response`: the reply could not be parsed.
   */
  readonly reason: 'network' | 'timeout' | 'http' | 'invalid-response';
  /** The HTTP status of the reply, if there was one. */
  readonly status?: number;
}

/**
 * How the delivery of a {@link VaadinRequest} settled.
 *
 * - `acknowledged`: the server replied to the request.
 * - `rejected`: the server refused the request: it replied with 401, or with a
 *   message saying that the session has expired.
 * - `discarded`: the client dropped the request without a confirming reply.
 *   The server may or may not have executed it.
 */
export type VaadinRequestOutcome = 'acknowledged' | 'rejected' | 'discarded';

/**
 * The events a {@link VaadinRequest} dispatches, by type.
 *
 * - `sent`: an attempt goes out; {@link VaadinRequest.attempt} counts it.
 * - `error`: the attempt failed; {@link VaadinRequest.failure} says why.
 * - `response`: a reply to the attempt arrived and was identified. The time
 *   from `sent` includes parsing the reply.
 * - `end`: the delivery is settled; {@link VaadinRequest.outcome} says how.
 *   Dispatched exactly once, as the last event.
 */
export interface VaadinRequestEventMap {
  sent: Event;
  error: Event;
  response: Event;
  end: Event;
}

// What a request records about its delivery. It is kept outside the request,
// where only the functions below can change it, so the request itself stays
// read-only for page scripts.
interface RequestState {
  attempt: number;
  failure?: VaadinRequestFailure;
  outcome?: VaadinRequestOutcome;
}

const states = new WeakMap<VaadinRequest, RequestState>();

/**
 * One request from the client to the server, tracked from the moment the first
 * invocation for it is queued until its delivery is settled. Delivery only: how
 * the client then processes the reply is tracked by {@link VaadinServerMessage}.
 *
 * The client announces each request as the detail of a `vaadin-request` event
 * on `window.Vaadin.Flow.clients[appId].events`, and then dispatches the
 * events of {@link VaadinRequestEventMap} on it. The request is the same object
 * across every attempt to deliver it, whether it is sent again after a
 * reconnection or because no reply arrived in time.
 *
 * The properties are read-only. Events dispatched on a request from outside
 * the client reach its listeners but change nothing in the request or the
 * client. To keep data of your own for a request, store it in a `WeakMap` keyed
 * by the request, so that it is released with the request.
 *
 * Listeners run synchronously inside the client's own work, including while it
 * queues invocations: keep them cheap and do not call back into the client from
 * them.
 *
 * Heartbeats are not covered.
 */
export class VaadinRequest extends TypedEventTarget<VaadinRequestEventMap> {
  constructor() {
    super();
    states.set(this, { attempt: 0 });
  }

  /** The number of attempts sent so far: 0 while queued, incremented before each `sent`. */
  get attempt(): number {
    return states.get(this)!.attempt;
  }

  /** Why the latest failed attempt failed, set before `error`; `undefined` if none failed. */
  get failure(): VaadinRequestFailure | undefined {
    return states.get(this)!.failure;
  }

  /** How the delivery settled, set before `end`; `undefined` until then. */
  get outcome(): VaadinRequestOutcome | undefined {
    return states.get(this)!.outcome;
  }
}

// The functions below change a request on behalf of the client. Each does
// nothing once the request has ended, so `end` is always the last event.

/** The state of a request that has not ended yet, or `undefined` once it has. */
function getOpenState(request: VaadinRequest): RequestState | undefined {
  const state = states.get(request)!;
  return state.outcome === undefined ? state : undefined;
}

/**
 * Counts a new attempt of the request and dispatches `sent`.
 *
 * @param request - the request being sent
 */
export function dispatchSent(request: VaadinRequest): void {
  const state = getOpenState(request);
  if (state) {
    state.attempt++;
    request.dispatchEvent(new Event('sent'));
  }
}

/**
 * Records why the latest attempt of the request failed and dispatches `error`.
 *
 * @param request - the request whose attempt failed
 * @param failure - why the attempt failed
 */
export function dispatchError(request: VaadinRequest, failure: VaadinRequestFailure): void {
  const state = getOpenState(request);
  if (state) {
    state.failure = Object.freeze({ ...failure });
    request.dispatchEvent(new Event('error'));
  }
}

/**
 * Dispatches `response` for a reply to the request and then ends it with the
 * given outcome.
 *
 * @param request - the request that was replied to
 * @param outcome - how the reply settles the request
 */
export function dispatchResponse(request: VaadinRequest, outcome: 'acknowledged' | 'rejected'): void {
  if (getOpenState(request)) {
    request.dispatchEvent(new Event('response'));
    dispatchRequestEnd(request, outcome);
  }
}

/**
 * Settles the request with the given outcome and dispatches `end`.
 *
 * @param request - the request to end
 * @param outcome - how the request settled
 */
export function dispatchRequestEnd(request: VaadinRequest, outcome: VaadinRequestOutcome): void {
  const state = getOpenState(request);
  if (state) {
    state.outcome = outcome;
    request.dispatchEvent(new Event('end'));
  }
}
