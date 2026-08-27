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

// TypeScript port of com.vaadin.client.UILifecycle. It manages a UI's lifecycle
// state, which may only advance forward INITIALIZING -> RUNNING -> TERMINATED,
// firing a state-change event on each transition. The GWT EventBus is replaced by
// a listener set.

import type { EventRemover } from '../EventRemover';

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

/**
 * Event triggered when the lifecycle state of a UI is changed.
 *
 * To listen for the event add a {@link StateChangeHandler} using
 * {@link UILifecycle.addHandler}.
 */
export interface StateChangeEvent {
  getUiLifecycle(): UILifecycle;
}

/** A listener for UI lifecycle state changes; mirrors StateChangeHandler. */
export type StateChangeHandler = (event: StateChangeEvent) => void;

/** Manages the lifecycle state of a UI; mirrors UILifecycle.java. */
export class UILifecycle {
  #state: UIState = UIState.INITIALIZING;

  readonly #handlers = new Set<StateChangeHandler>();

  /**
   * Gets the state of the UI.
   *
   * @returns the current state of the UI
   */
  getState(): UIState {
    return this.#state;
  }

  /**
   * Sets the state of the UI to the given value.
   *
   * Only allows state changes in one direction: {@link UIState.INITIALIZING}
   * -\> {@link UIState.RUNNING} -\> {@link UIState.TERMINATED}.
   *
   * Changing the state fires a {@link StateChangeEvent}.
   *
   * @param state - the new UI state
   */
  setState(state: UIState): void {
    if (ORDINAL[state] !== ORDINAL[this.#state] + 1) {
      throw new Error(`Tried to move from state ${this.#state} to ${state} which is not allowed`);
    }
    this.#state = state;
    const event: StateChangeEvent = { getUiLifecycle: () => this };
    this.#handlers.forEach((handler) => handler(event));
  }

  /**
   * Check if the state is {@link UIState.RUNNING}.
   *
   * @returns `true` if the status is {@link UIState.RUNNING}, `false`
   *          otherwise
   */
  isRunning(): boolean {
    return this.#state === UIState.RUNNING;
  }

  /**
   * Check if the state is {@link UIState.TERMINATED}.
   *
   * @returns `true` if the status is {@link UIState.TERMINATED}, `false`
   *          otherwise
   */
  isTerminated(): boolean {
    return this.#state === UIState.TERMINATED;
  }

  /**
   * Adds a state change event handler.
   *
   * @param handler - the handler to add
   * @returns a handler registration object which can be used to remove the
   *          handler
   */
  addHandler(handler: StateChangeHandler): EventRemover {
    this.#handlers.add(handler);
    return {
      remove: () => {
        this.#handlers.delete(handler);
      }
    };
  }
}
