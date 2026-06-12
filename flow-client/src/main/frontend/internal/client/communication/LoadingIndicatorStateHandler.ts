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
import { ConnectionIndicator } from '../ConnectionIndicator';

// Mirrors com.vaadin.flow.shared.JsonConstants.RPC_TYPE_EVENT.
const RPC_TYPE_EVENT = 'event';

// High-frequency events whose RPC requests should not show the loading
// indicator (to avoid visual noise). Lifted verbatim from the Java original.
const SILENT_EVENT_TYPES: ReadonlySet<string> = new Set([
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
 * Manages the loading-indicator state based on active RPC requests, event
 * types, and lifecycle events. Migrated from
 * `com.vaadin.client.communication.LoadingIndicatorStateHandler`.
 *
 * Construction takes a `hasActiveRequest` callback that dispatches into
 * `RequestResponseTracker.hasActiveRequest()`, rather than the full Registry,
 * so the TS class does not reach back through the Java facade.
 */
export class LoadingIndicatorStateHandler {
  private readonly hasActiveRequest: () => boolean;
  private loading = false;
  private showLoading = false;

  constructor(hasActiveRequest: () => boolean) {
    this.hasActiveRequest = hasActiveRequest;
  }

  startLoading(): void {
    if (!this.showLoading) {
      // The next request is muted; don't show the loading indicator.
      return;
    }
    this.update();
  }

  stopLoading(): void {
    if (this.hasActiveRequest()) {
      // Some request is still in progress, skip the stop.
      return;
    }
    this.showLoading = false;
    // Defer (setTimeout 0 matches GWT's Scheduler.scheduleDeferred) so that
    // a follow-up request scheduled in the same task can re-arm
    // `showLoading` before this update runs.
    setTimeout(() => this.update(), 0);
  }

  processMessage(rpcType: string, eventType: string | null): void {
    // At least one non-silent message must occur for the next request to
    // show the loading indicator.
    const silent = rpcType === RPC_TYPE_EVENT && eventType !== null && SILENT_EVENT_TYPES.has(eventType);
    if (!silent) {
      this.showLoading = true;
    }
  }

  private update(): void {
    if (this.showLoading === this.loading) {
      return;
    }
    this.loading = this.showLoading;
    // Use loadingStarted/loadingFinished rather than setState, so the
    // Flow-router and Hilla loading parties aren't interfered with.
    if (this.loading) {
      ConnectionIndicator.loadingStarted();
    } else {
      ConnectionIndicator.loadingFinished();
    }
  }
}
