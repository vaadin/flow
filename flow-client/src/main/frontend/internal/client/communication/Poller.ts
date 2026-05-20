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

// Mirrors com.vaadin.flow.component.PollEvent.DOM_EVENT_NAME.
const POLL_DOM_EVENT_NAME = 'ui-poll';

type StateNodeLike = unknown;

type StateTreeLike = {
  getRootNode(): StateNodeLike;
  sendEventToServer(node: StateNodeLike, eventType: string, eventData: unknown): void;
};

type UILifecycleEvent = { getUiLifecycle(): { isTerminated(): boolean } };

type UILifecycleLike = {
  addHandler(handler: (event: UILifecycleEvent) => void): unknown;
};

/**
 * Polls the server with a given interval. Migrated from
 * `com.vaadin.client.communication.Poller`. Each tick sends a `ui-poll`
 * server event on the root state node; the timer auto-cancels when the UI
 * lifecycle transitions to TERMINATED.
 */
export class Poller {
  private readonly tree: StateTreeLike;
  private timerId: ReturnType<typeof setInterval> | null = null;

  constructor(tree: StateTreeLike, uiLifecycle: UILifecycleLike) {
    this.tree = tree;
    uiLifecycle.addHandler((event) => {
      if (event.getUiLifecycle().isTerminated()) {
        this.stop();
      }
    });
  }

  setInterval(interval: number): void {
    this.stop();
    if (interval >= 0) {
      this.timerId = setInterval(() => this.poll(), interval);
    }
  }

  poll(): void {
    this.tree.sendEventToServer(this.tree.getRootNode(), POLL_DOM_EVENT_NAME, null);
  }

  private stop(): void {
    if (this.timerId !== null) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }
}
