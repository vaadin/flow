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
import { Console } from '../Console';

type UILifecycleEvent = { getUiLifecycle(): { isTerminated(): boolean } };

type UILifecycleLike = {
  addHandler(handler: (event: UILifecycleEvent) => void): unknown;
};

type HeartbeatCallbacks = {
  onOk(): void;
  onInvalidStatusCode(xhr: XMLHttpRequest): void;
  onException(xhr: XMLHttpRequest, message: string): void;
};

/**
 * Sends heartbeat POSTs to the server at a configured interval. Migrated from
 * `com.vaadin.client.communication.Heartbeat`.
 *
 * The connection-state callbacks (ok, invalid status, exception) are wired
 * at construction so the TS class does not need to dispatch back through
 * `Registry.getConnectionStateHandler()`. The exception payload is reduced
 * to a string because Java's `Exception` shape doesn't survive the JS
 * boundary cleanly — the only consumer (`heartbeatException` logging) reads
 * `getMessage()`, so a string is sufficient.
 *
 * GWT's `Timer` is replaced with `setTimeout`/`clearTimeout`; the original
 * `Xhr.post(uri, null, null, callback)` is replaced with a native
 * `XMLHttpRequest` (matching the original "no Content-type" semantics).
 */
export class Heartbeat {
  private readonly callbacks: HeartbeatCallbacks;
  private readonly uri: string;
  private interval = -1;
  private timerId: ReturnType<typeof setTimeout> | null = null;

  constructor(uri: string, heartbeatInterval: number, uiLifecycle: UILifecycleLike, callbacks: HeartbeatCallbacks) {
    this.uri = uri;
    this.callbacks = callbacks;
    this.setInterval(heartbeatInterval);
    uiLifecycle.addHandler((event) => {
      if (event.getUiLifecycle().isTerminated()) {
        this.setInterval(-1);
      }
    });
  }

  /** Sends a heartbeat to the server now. */
  send(): void {
    this.cancelTimer();
    if (this.interval < 0) {
      Console.debug('Heartbeat terminated, skipping request');
      return;
    }
    Console.debug('Sending heartbeat request...');

    const xhr = new XMLHttpRequest();
    xhr.withCredentials = true;
    xhr.onreadystatechange = (): void => {
      if (xhr.readyState !== XMLHttpRequest.DONE) {
        return;
      }
      // Clear handler so the listener can be garbage-collected.
      xhr.onreadystatechange = null;
      if (xhr.status === 200) {
        this.callbacks.onOk();
        this.schedule();
        return;
      }
      if (this.interval < 0) {
        // Heartbeat was terminated before the response arrived (typically
        // because the session expired and another component handled it).
        Console.debug('Heartbeat terminated, ignoring failure.');
      } else {
        this.callbacks.onInvalidStatusCode(xhr);
      }
      this.schedule();
    };

    try {
      xhr.open('POST', this.uri);
      xhr.send();
    } catch (e) {
      Console.error(e);
      const message = e instanceof Error ? e.message : String(e);
      this.callbacks.onException(xhr, message);
      xhr.onreadystatechange = null;
      this.schedule();
    }
  }

  /** Gets the heartbeat interval in seconds. */
  getInterval(): number {
    return this.interval;
  }

  /** Updates the schedule to match the configured interval. */
  schedule(): void {
    if (this.interval > 0) {
      Console.debug(`Scheduling heartbeat in ${this.interval} seconds`);
      this.cancelTimer();
      this.timerId = setTimeout(() => this.send(), this.interval * 1000);
    } else {
      Console.debug('Disabling heartbeat');
      this.cancelTimer();
    }
  }

  /** Sets the heartbeat interval (in seconds) and reschedules. */
  setInterval(heartbeatInterval: number): void {
    Console.debug(`Setting heartbeat interval to ${heartbeatInterval}sec.`);
    this.interval = heartbeatInterval;
    this.schedule();
  }

  private cancelTimer(): void {
    if (this.timerId !== null) {
      clearTimeout(this.timerId);
      this.timerId = null;
    }
  }
}
