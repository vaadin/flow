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

// TypeScript port of com.vaadin.client.communication.LoadingIndicatorStateHandler.
// It shows/hides the loading indicator based on active RPC requests and message
// types, muting it for high-frequency UI events (mousemove etc.). The
// connection-indicator calls go through ConnectionIndicator.

import type { Registry } from '../Registry';
import { getScheduler } from '../TrackingScheduler';
import {
  CONNECTED,
  LOADING,
  addStateChangeListener,
  getState,
  loadingFinished,
  loadingStarted,
  setState
} from '../ConnectionIndicator';
import { JsonConstants } from '../../flow/shared/JsonConstants';

// High-frequency events whose RPC requests should not trigger loading
// indication.
const SILENT_EVENT_TYPES = new Set<string>([
  'keydown',
  'keypress',
  'keyup',
  'mousemove',
  'pointermove',
  'pointerrawupdate',
  'touchmove',
  'beforeinput',
  'input',
  'scroll',
  'wheel',
  'drag',
  'dragover'
]);

/**
 * Manages the state of loading indicator based on active RPC requests, event
 * types, and lifecycle events. This class ensures appropriate visual feedback
 * (e.g., loading bar) is shown or hidden according to the current network
 * conditions and request status. It is responsible for muting the loading
 * indication when RPC requests are triggered by high-frequency UI events
 * (mousemove and such) to avoid excessive visual noise in these cases.
 */
export class LoadingIndicatorStateHandler {
  readonly #registry: Registry;

  #loading = false;

  #showLoading = false;

  // Whether the connection state currently counts this handler's loading.
  // Any state change resets that count, so it clears this flag.
  #announced = false;

  // A state change, e.g. to reconnecting or offline, drops the loading
  // announced before it, whoever made the change.
  readonly #stateChangeListener = (): void => {
    this.#announced = false;
  };

  /**
   * Creates a new instance connected to the given registry.
   *
   * @param registry - the global registry
   */
  constructor(registry: Registry) {
    this.#registry = registry;
  }

  /** Shows loading when a non-silent request starts. */
  startLoading(): void {
    if (!this.#showLoading) {
      // The next request is muted, do not show loading.
      return;
    }
    this.#update();
  }

  /** Hides loading when no requests remain active (debounced). */
  stopLoading(): void {
    if (this.#registry.getRequestResponseTracker().hasActiveRequest()) {
      // Some request is in progress, skip the current stop.
      return;
    }
    // Reset the loading state
    this.#showLoading = false;
    // Debounce the update so a follow-up request keeps the indicator shown.
    // Through the shared TrackingScheduler (mirrors GWT's Scheduler.get()) so
    // the pending update keeps the application active for TestBench's
    // waitForVaadin, as ServerRpcQueue's deferred flush does.
    getScheduler().scheduleDeferred(() => this.#update());
  }

  /**
   * Restores the loading indication after a reconnect, which replaced it with
   * the reconnecting state. Re-announces an ongoing loading indication unless
   * a request started during the reconnect already announced it, moving the
   * state from reconnecting to loading in one transition, or adding to the
   * loading started by another party (Flow router, Hilla requests) during the
   * reconnect, then stops it if no request remains active. Otherwise shows
   * connected, also leaving a connection lost set by another party, unless
   * another party is loading.
   */
  restoreLoading(): void {
    if (!this.#loading) {
      // Nothing to stop. A message queued but not yet sent keeps its request
      // for loading indication.
      if (getState() !== LOADING) {
        setState(CONNECTED);
      }
      return;
    }
    if (!this.#announced) {
      this.#announce();
    }
    this.stopLoading();
  }

  /**
   * Processes an RPC message to determine if a loading indicator should be displayed.
   *
   * @param rpcType - the type of RPC request being processed
   * @param eventType - for event RPC requests, the name of the event, otherwise
   *          `null`
   */
  processMessage(rpcType: string | null, eventType: string | null): void {
    // Require at least one non-silent message to indicate loading for the next
    // request.
    const silent = rpcType === JsonConstants.RPC_TYPE_EVENT && eventType !== null && SILENT_EVENT_TYPES.has(eventType);
    if (!silent) {
      this.#showLoading = true;
    }
  }

  #update(): void {
    if (this.#showLoading === this.#loading) {
      return;
    }
    this.#loading = this.#showLoading;
    // loadingStarted/loadingFinished are preferred over setState so as not to
    // interfere with other loading parties (Flow router, Hilla requests).
    if (this.#loading) {
      this.#announce();
    } else if (this.#announced) {
      this.#announced = false;
      loadingFinished();
    }
  }

  #announce(): void {
    // Subscribing again is a no-op for the same store, and also covers a
    // store that did not exist yet when this handler was created.
    addStateChangeListener(this.#stateChangeListener);
    // After loadingStarted, as its change to loading clears the flag.
    loadingStarted();
    this.#announced = true;
  }
}
