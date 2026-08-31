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

// TypeScript port of com.vaadin.client.communication.Poller, built alongside the
// Java version. It polls the server at a configured interval by sending a poll
// event on the root state node (the GWT repeating Timer maps to setInterval) and
// stops when the UI lifecycle terminates. The Registry/StateTree are contracts
// satisfied at cutover.

import type { Registry } from '../Registry';
import type { StateTree } from '../flow/StateTree';
import type { UILifecycle } from '../UILifecycle';

// com.vaadin.flow.component.PollEvent.DOM_EVENT_NAME
const POLL_DOM_EVENT_NAME = 'ui-poll';

/** The slice of {@link Registry} that Poller uses. */
interface PollerRegistry {
  getUILifecycle(): Pick<UILifecycle, 'addHandler'>;
  getStateTree(): StateTree;
}

/**
 * Handles polling the server with a given interval.
 */
export class Poller {
  #pollHandle: ReturnType<typeof setInterval> | null = null;

  readonly #registry: PollerRegistry;

  /**
   * Creates a new instance using the given registry.
   *
   * @param registry - the registry
   */
  constructor(registry: PollerRegistry) {
    this.#registry = registry;
    registry.getUILifecycle().addHandler((event) => {
      if (event.getUiLifecycle().isTerminated()) {
        this.#stop();
      }
    });
  }

  #stop(): void {
    if (this.#pollHandle !== null) {
      clearInterval(this.#pollHandle);
      this.#pollHandle = null;
    }
  }

  /**
   * Sets the polling interval.
   *
   * Changing the polling interval will stop any current polling and schedule a
   * new poll to happen after the given interval.
   *
   * @param interval - The interval to use
   */
  setInterval(interval: number): void {
    this.#stop();
    if (interval >= 0) {
      if (interval === 0) {
        // GWT's Timer.scheduleRepeating(0) throws, so a zero interval never
        // starts a timer in Java either; setInterval(fn, 0) would busy-poll.
        throw new Error('must be positive');
      }
      this.#pollHandle = setInterval(() => this.poll(), interval);
    }
  }

  /** Polls the server for changes by sending a poll event on the root node. */
  poll(): void {
    const stateTree = this.#registry.getStateTree();
    stateTree.sendEventToServer(stateTree.getRootNode(), POLL_DOM_EVENT_NAME, null);
  }
}
