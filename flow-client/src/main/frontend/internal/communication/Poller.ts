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

// com.vaadin.flow.component.PollEvent.DOM_EVENT_NAME
const POLL_DOM_EVENT_NAME = 'ui-poll';

/** The slice of StateTree that Poller uses. */
interface PollerTree {
  getRootNode(): unknown;
  sendEventToServer(node: unknown, eventType: string, eventData: unknown): void;
}

/** The slice of Registry that Poller uses. */
interface PollerRegistry {
  getUILifecycle(): {
    addHandler(handler: (event: { getUiLifecycle(): { isTerminated(): boolean } }) => void): void;
  };
  getStateTree(): PollerTree;
}

/** Polls the server with a given interval; mirrors Poller.java. */
export class Poller {
  private pollHandle: ReturnType<typeof setInterval> | null = null;

  private readonly registry: PollerRegistry;

  constructor(registry: PollerRegistry) {
    this.registry = registry;
    registry.getUILifecycle().addHandler((event) => {
      if (event.getUiLifecycle().isTerminated()) {
        this.stop();
      }
    });
  }

  private stop(): void {
    if (this.pollHandle !== null) {
      clearInterval(this.pollHandle);
      this.pollHandle = null;
    }
  }

  /**
   * Sets the polling interval (ms). A non-negative interval (re)starts polling;
   * a negative interval stops it. Mirrors setInterval.
   */
  setInterval(interval: number): void {
    this.stop();
    if (interval >= 0) {
      this.pollHandle = setInterval(() => this.poll(), interval);
    }
  }

  /** Polls the server for changes by sending a poll event on the root node. */
  poll(): void {
    const stateTree = this.registry.getStateTree();
    stateTree.sendEventToServer(stateTree.getRootNode(), POLL_DOM_EVENT_NAME, null);
  }
}
