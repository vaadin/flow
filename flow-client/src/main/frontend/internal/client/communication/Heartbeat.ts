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

import type { UILifecycle } from '../UILifecycle';
import type { ApplicationConfiguration } from '../ApplicationConfiguration';
import type { ConnectionStateHandler } from './ConnectionStateHandler';
import { addGetParameter } from '../../flow/shared/util/SharedUtil';
import { Console } from '../Console';

// com.vaadin.flow.shared.ApplicationConstants
const REQUEST_TYPE_PARAMETER = 'v-r';
const REQUEST_TYPE_HEARTBEAT = 'heartbeat';
const UI_ID_PARAMETER = 'v-uiId';

/** The slice of Registry that Heartbeat uses. */
interface HeartbeatRegistry {
  getApplicationConfiguration(): Pick<ApplicationConfiguration, 'getHeartbeatInterval' | 'getServiceUrl' | 'getUIId'>;
  getUILifecycle(): Pick<UILifecycle, 'addHandler'>;
  getConnectionStateHandler(): Pick<
    ConnectionStateHandler,
    'heartbeatOk' | 'heartbeatInvalidStatusCode' | 'heartbeatException'
  >;
}

// One-shot reschedulable timer; mirrors the GWT Timer used by Heartbeat.
class HeartbeatTimer {
  #handle: ReturnType<typeof setTimeout> | null = null;

  readonly #task: () => void;

  /**
   * Creates a new instance connected to the given registry.
   *
   * @param task - the global registry
   */
  constructor(task: () => void) {
    this.#task = task;
  }

  schedule(ms: number): void {
    this.cancel();
    this.#handle = setTimeout(() => {
      this.#handle = null;
      this.#task();
    }, ms);
  }

  cancel(): void {
    if (this.#handle !== null) {
      clearTimeout(this.#handle);
      this.#handle = null;
    }
  }
}

/** Sends heartbeats to the server and reacts to the response; mirrors Heartbeat.java. */
export class Heartbeat {
  readonly #timer = new HeartbeatTimer(() => this.send());

  #uri = '';

  #interval = -1;

  readonly #registry: HeartbeatRegistry;

  constructor(registry: HeartbeatRegistry) {
    this.#registry = registry;
    const configuration = registry.getApplicationConfiguration();
    this.setInterval(configuration.getHeartbeatInterval());

    let uri = configuration.getServiceUrl();
    uri = addGetParameter(uri, REQUEST_TYPE_PARAMETER, REQUEST_TYPE_HEARTBEAT);
    uri = addGetParameter(uri, UI_ID_PARAMETER, configuration.getUIId());
    this.#uri = uri;

    registry.getUILifecycle().addHandler((event) => {
      if (event.getUiLifecycle().isTerminated()) {
        this.setInterval(-1);
      }
    });
  }

  /** Sends a heartbeat request to the server. */
  send(): void {
    this.#timer.cancel();
    if (this.#interval < 0) {
      Console.debug('Heartbeat terminated, skipping request');
      return;
    }

    Console.debug('Sending heartbeat request...');

    const xhr = new XMLHttpRequest();

    // Mirrors Xhr.Callback.onFail: a null exception is a non-200 response, a
    // non-null one a synchronous throw from open()/send().
    const onFail = (error: Error | null): void => {
      if (error === null) {
        if (this.#interval < 0) {
          // Heartbeat terminated before response processing (likely a session
          // expiration already handled elsewhere).
          Console.debug('Heartbeat terminated, ignoring failure.');
        } else {
          this.#registry.getConnectionStateHandler().heartbeatInvalidStatusCode(xhr);
        }
      } else {
        this.#registry.getConnectionStateHandler().heartbeatException(xhr, error);
      }
      this.schedule();
    };

    // Cleared once fired, and no "error" listener: that event follows the DONE
    // ready-state change, so a network failure would be reported twice.
    xhr.onreadystatechange = () => {
      if (xhr.readyState === XMLHttpRequest.DONE) {
        if (xhr.status === 200) {
          this.#registry.getConnectionStateHandler().heartbeatOk();
          this.schedule();
          xhr.onreadystatechange = null;
          return;
        }
        onFail(null);
        xhr.onreadystatechange = null;
      }
    };
    try {
      xhr.open('POST', this.#uri, true);
      // Java's Xhr always sends credentials so cross-origin/CORS deployments
      // keep their cookies and authentication headers.
      xhr.withCredentials = true;
      xhr.send();
    } catch (error) {
      Console.error(error);
      onFail(error as Error);
      xhr.onreadystatechange = null;
    }
  }

  /**
   * Gets the heartbeat interval.
   *
   * @returns the interval at which heartbeat requests are sent.
   */
  getInterval(): number {
    return this.#interval;
  }

  /** Reschedules the heartbeat to match the interval; a negative interval disables it. */
  schedule(): void {
    if (this.#interval > 0) {
      Console.debug(`Scheduling heartbeat in ${this.#interval} seconds`);
      this.#timer.schedule(this.#interval * 1000);
    } else {
      Console.debug('Disabling heartbeat');
      this.#timer.cancel();
    }
  }

  /**
   * Changes the heartbeatInterval in runtime and applies it.
   *
   * @param heartbeatInterval - new interval in seconds.
   */
  setInterval(heartbeatInterval: number): void {
    Console.debug(`Setting heartbeat interval to ${heartbeatInterval}sec.`);
    this.#interval = heartbeatInterval;
    this.schedule();
  }
}
