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

// TypeScript port of com.vaadin.client.communication.LoadingIndicatorStateHandler,
// built alongside the Java version. It shows/hides the loading indicator based
// on active RPC requests and message types, muting it for high-frequency UI
// events (mousemove etc.). The Registry/RequestResponseTracker are contracts
// satisfied at cutover; the connection-indicator calls go through the ported
// ConnectionIndicator.

import { loadingFinished, loadingStarted } from '../ConnectionIndicator';
import { JsonConstants } from '../JsonConstants';

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

/** The slice of Registry that LoadingIndicatorStateHandler uses. */
interface LoadingIndicatorRegistry {
  getRequestResponseTracker(): { hasActiveRequest(): boolean };
}

/** Manages the loading indicator from RPC activity; mirrors LoadingIndicatorStateHandler.java. */
export class LoadingIndicatorStateHandler {
  private readonly registry: LoadingIndicatorRegistry;

  private loading = false;

  private showLoading = false;

  constructor(registry: LoadingIndicatorRegistry) {
    this.registry = registry;
  }

  /** Shows loading when a non-silent request starts. */
  startLoading(): void {
    if (!this.showLoading) {
      // The next request is muted, do not show loading.
      return;
    }
    this.update();
  }

  /** Hides loading when no requests remain active (debounced). */
  stopLoading(): void {
    if (this.registry.getRequestResponseTracker().hasActiveRequest()) {
      // Some request is in progress, skip the current stop.
      return;
    }
    this.showLoading = false;
    // Debounce the update so a follow-up request keeps the indicator shown.
    setTimeout(() => this.update(), 0);
  }

  /**
   * Processes an RPC message to decide whether the next request should show the
   * loading indicator (muted for high-frequency event requests).
   */
  processMessage(rpcType: string | null, eventType: string | null): void {
    const silent = rpcType === JsonConstants.RPC_TYPE_EVENT && eventType !== null && SILENT_EVENT_TYPES.has(eventType);
    if (!silent) {
      this.showLoading = true;
    }
  }

  private update(): void {
    if (this.showLoading === this.loading) {
      return;
    }
    this.loading = this.showLoading;
    // loadingStarted/loadingFinished are preferred over setState so as not to
    // interfere with other loading parties (Flow router, Hilla requests).
    if (this.loading) {
      loadingStarted();
    } else {
      loadingFinished();
    }
  }
}
