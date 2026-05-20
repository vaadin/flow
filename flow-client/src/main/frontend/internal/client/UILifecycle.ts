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

const STATES = ['INITIALIZING', 'RUNNING', 'TERMINATED'] as const;
type State = (typeof STATES)[number];

interface HandlerRegistration {
  removeHandler(): void;
}

type StateChangeHandler = (event: { getUiLifecycle(): UILifecycle }) => void;

/**
 * Manages the lifecycle of a UI. Migrated from
 * `com.vaadin.client.UILifecycle`. The state is exposed as the string name
 * of the matching {@code UIState} enum constant; the Java facade adapts
 * between {@code UIState} and the string on each side of the JsInterop
 * boundary.
 */
export class UILifecycle {
  private state: State = 'INITIALIZING';
  private readonly handlers = new Set<StateChangeHandler>();

  getStateName(): string {
    return this.state;
  }

  setStateName(state: string): void {
    const currentOrdinal = STATES.indexOf(this.state);
    const nextOrdinal = STATES.indexOf(state as State);
    if (nextOrdinal !== currentOrdinal + 1) {
      throw new Error(`Tried to move from state ${this.state} to ${state} which is not allowed`);
    }
    this.state = state as State;
    const event = { getUiLifecycle: (): UILifecycle => this };
    const snapshot = Array.from(this.handlers);
    for (const handler of snapshot) {
      handler(event);
    }
  }

  isRunning(): boolean {
    return this.state === 'RUNNING';
  }

  isTerminated(): boolean {
    return this.state === 'TERMINATED';
  }

  addHandler(handler: StateChangeHandler): HandlerRegistration {
    this.handlers.add(handler);
    return {
      removeHandler: () => {
        this.handlers.delete(handler);
      }
    };
  }
}
