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

// TypeScript port of com.vaadin.client.UILifecycle, built alongside the Java
// version. It manages a UI's lifecycle state, which may only advance forward
// INITIALIZING -> RUNNING -> TERMINATED, firing a state-change event on each
// transition. The GWT EventBus is replaced by a listener set.

import type { EventRemover } from './reactive/reactive';

/** The lifecycle state of a UI; the order defines the allowed forward transitions. */
export const UIState = {
  INITIALIZING: 'INITIALIZING',
  RUNNING: 'RUNNING',
  TERMINATED: 'TERMINATED'
} as const;

export type UIState = (typeof UIState)[keyof typeof UIState];

// Ordinals matching the Java enum order, used to enforce single-step forward
// transitions.
const ORDINAL: Record<UIState, number> = {
  INITIALIZING: 0,
  RUNNING: 1,
  TERMINATED: 2
};

/** Event fired when the lifecycle state changes; mirrors UILifecycle.StateChangeEvent. */
export interface StateChangeEvent {
  getUiLifecycle(): UILifecycle;
}

/** A listener for UI lifecycle state changes; mirrors StateChangeHandler. */
export type StateChangeHandler = (event: StateChangeEvent) => void;

/** Manages the lifecycle state of a UI; mirrors UILifecycle.java. */
export class UILifecycle {
  private state: UIState = UIState.INITIALIZING;

  private readonly handlers = new Set<StateChangeHandler>();

  /** The current lifecycle state. */
  getState(): UIState {
    return this.state;
  }

  /**
   * Advances the state. Only single-step forward transitions
   * (INITIALIZING -> RUNNING -> TERMINATED) are allowed; fires a state-change
   * event.
   */
  setState(state: UIState): void {
    if (ORDINAL[state] !== ORDINAL[this.state] + 1) {
      throw new Error(`Tried to move from state ${this.state} to ${state} which is not allowed`);
    }
    this.state = state;
    const event: StateChangeEvent = { getUiLifecycle: () => this };
    this.handlers.forEach((handler) => handler(event));
  }

  /** Whether the state is RUNNING. */
  isRunning(): boolean {
    return this.state === UIState.RUNNING;
  }

  /** Whether the state is TERMINATED. */
  isTerminated(): boolean {
    return this.state === UIState.TERMINATED;
  }

  /** Adds a state-change handler, returning a remover. */
  addHandler(handler: StateChangeHandler): EventRemover {
    this.handlers.add(handler);
    return {
      remove: () => {
        this.handlers.delete(handler);
      }
    };
  }
}
