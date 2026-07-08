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

// TypeScript port of com.vaadin.client.communication.Heartbeat, built alongside
// the Java version. It periodically POSTs a heartbeat request to keep the
// server-side UI alive, rescheduling after each response and disabling itself
// when the UI terminates. The GWT Timer maps to setTimeout; the
// Registry/ApplicationConfiguration/UILifecycle/ConnectionStateHandler are
// contracts satisfied at cutover.

import { addGetParameter } from '../SharedUtil';

// com.vaadin.flow.shared.ApplicationConstants
const REQUEST_TYPE_PARAMETER = 'v-r';
const REQUEST_TYPE_HEARTBEAT = 'heartbeat';
const UI_ID_PARAMETER = 'v-uiId';

/** The slice of Registry that Heartbeat uses. */
interface HeartbeatRegistry {
  getApplicationConfiguration(): { getHeartbeatInterval(): number; getServiceUrl(): string; getUIId(): number };
  getUILifecycle(): {
    addHandler(handler: (event: { getUiLifecycle(): { isTerminated(): boolean } }) => void): void;
  };
  getConnectionStateHandler(): {
    heartbeatOk(): void;
    heartbeatInvalidStatusCode(xhr: XMLHttpRequest): void;
    heartbeatException(xhr: XMLHttpRequest, error: unknown): void;
  };
}

// One-shot reschedulable timer; mirrors the GWT Timer used by Heartbeat.
class HeartbeatTimer {
  private handle: ReturnType<typeof setTimeout> | null = null;

  private readonly task: () => void;

  constructor(task: () => void) {
    this.task = task;
  }

  schedule(ms: number): void {
    this.cancel();
    this.handle = setTimeout(() => {
      this.handle = null;
      this.task();
    }, ms);
  }

  cancel(): void {
    if (this.handle !== null) {
      clearTimeout(this.handle);
      this.handle = null;
    }
  }
}

/** Sends heartbeats to the server and reacts to the response; mirrors Heartbeat.java. */
export class Heartbeat {
  private readonly timer = new HeartbeatTimer(() => this.send());

  private uri = '';

  private interval = -1;

  private readonly registry: HeartbeatRegistry;

  constructor(registry: HeartbeatRegistry) {
    this.registry = registry;
    const configuration = registry.getApplicationConfiguration();
    this.setInterval(configuration.getHeartbeatInterval());

    let uri = configuration.getServiceUrl();
    uri = addGetParameter(uri, REQUEST_TYPE_PARAMETER, REQUEST_TYPE_HEARTBEAT);
    uri = addGetParameter(uri, UI_ID_PARAMETER, configuration.getUIId());
    this.uri = uri;

    registry.getUILifecycle().addHandler((event) => {
      if (event.getUiLifecycle().isTerminated()) {
        this.setInterval(-1);
      }
    });
  }

  /** Sends a heartbeat request to the server. */
  send(): void {
    this.timer.cancel();
    if (this.interval < 0) {
      console.debug('Heartbeat terminated, skipping request');
      return;
    }

    console.debug('Sending heartbeat request...');

    const xhr = new XMLHttpRequest();
    xhr.open('POST', this.uri, true);
    // Mirror Java's Xhr.post, which always sends credentials so cross-origin/CORS
    // deployments keep their cookies and authentication headers.
    xhr.withCredentials = true;
    xhr.onreadystatechange = () => {
      if (xhr.readyState === XMLHttpRequest.DONE) {
        if (xhr.status === 200) {
          this.registry.getConnectionStateHandler().heartbeatOk();
        } else if (this.interval < 0) {
          // Heartbeat terminated before response processing (likely a session
          // expiration already handled elsewhere).
          console.debug('Heartbeat terminated, ignoring failure.');
        } else {
          this.registry.getConnectionStateHandler().heartbeatInvalidStatusCode(xhr);
        }
        this.schedule();
      }
    };
    xhr.onerror = () => {
      this.registry.getConnectionStateHandler().heartbeatException(xhr, new Error('Heartbeat request failed'));
      this.schedule();
    };
    xhr.send();
  }

  /** The heartbeat interval in seconds. */
  getInterval(): number {
    return this.interval;
  }

  /** Reschedules the heartbeat to match the interval; a negative interval disables it. */
  schedule(): void {
    if (this.interval > 0) {
      console.debug(`Scheduling heartbeat in ${this.interval} seconds`);
      this.timer.schedule(this.interval * 1000);
    } else {
      console.debug('Disabling heartbeat');
      this.timer.cancel();
    }
  }

  /** Changes the heartbeat interval (seconds) at runtime and applies it. */
  setInterval(heartbeatInterval: number): void {
    console.debug(`Setting heartbeat interval to ${heartbeatInterval}sec.`);
    this.interval = heartbeatInterval;
    this.schedule();
  }
}
