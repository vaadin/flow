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

// TypeScript port of com.vaadin.client.flow.reactive.Computation.

import type { EventRemover } from '../../../EventRemover';
import { InvalidateEvent } from './InvalidateEvent';
import type { InvalidateListener } from './InvalidateListener';
import { Reactive } from './Reactive';
import type { ReactiveValue } from './ReactiveValue';
import type { ReactiveValueChangeEvent } from './ReactiveValueChangeEvent';

/**
 * Automatically reruns the recompute command whenever any reactive value used
 * by it changes. The recompute command is invoked by the next invocation of
 * {@link Reactive.flush}, unless it has been invoked manually before the global
 * flush. A computation is also scheduled to for an initial "recomputation" when
 * it is created.
 *
 * Mirrors Computation.java in its decoupled, callback form.
 */
export class Computation {
  #invalidated = false;

  #stopped = false;

  readonly #dependencies: EventRemover[] = [];

  #invalidateListeners = new Set<InvalidateListener>();

  readonly #recomputeCommand: () => void;

  /**
   * Creates a new computation.
   *
   * @param recomputeCommand - the command that does the actual recomputation.
   *   This command is run in a way that automatically registers dependencies
   *   to any reactive value accessed.
   */
  constructor(recomputeCommand: () => void) {
    this.#recomputeCommand = recomputeCommand;
    // Make sure a recompute is scheduled
    this.#invalidate();
  }

  /**
   * Adds a dependency to a reactive value. This computation is scheduled for
   * recomputation when any dependency fires a change event. All previous
   * dependencies are cleared before recomputing.
   *
   * This method is automatically called when a reactive value is used for
   * recomputing this computation. The developer is not expected to call this
   * method himself.
   *
   * @param dependency - the reactive value to depend on
   */
  addDependency(dependency: ReactiveValue): void {
    if (!this.#stopped) {
      const remover = dependency.addReactiveValueChangeListener((event) => this.onValueChange(event));
      this.#dependencies.push(remover);
    }
  }

  /**
   * Invoked when a reactive value has changed.
   *
   * @param _changeEvent - the change event
   */
  onValueChange(_changeEvent: ReactiveValueChangeEvent): void {
    if (this.#invalidated || this.#stopped) {
      return;
    }
    this.#invalidate();
  }

  #invalidate(): void {
    this.#invalidated = true;

    this.#clearDependencies();

    if (!this.#stopped) {
      Reactive.addFlushListener(() => this.recompute());
    }

    // Fire invalidate events
    if (this.#invalidateListeners.size !== 0) {
      const oldListeners = this.#invalidateListeners;
      this.#invalidateListeners = new Set();

      const invalidateEvent = new InvalidateEvent(this);

      oldListeners.forEach((listener) => listener(invalidateEvent));
    }
  }

  #clearDependencies(): void {
    while (this.#dependencies.length > 0) {
      const remover = this.#dependencies.shift();
      remover?.remove();
    }
  }

  /**
   * Stops this computation, so that it will no longer be recomputed.
   */
  stop(): void {
    this.#stopped = true;

    this.#invalidate();

    // Prevent firing more events
    this.#invalidateListeners.clear();

    // Release memory
    this.#clearDependencies();
  }

  /**
   * Checks whether this computation is invalidated. An invalidated
   * computation will eventually be recomputed (unless it has also been
   * stopped). Recomputation will happen the next time {@link recompute} or
   * {@link Reactive.flush} is invoked.
   *
   * @returns `true` if this computation is invalidated; otherwise `false`
   */
  isInvalidated(): boolean {
    return this.#invalidated;
  }

  /**
   * Recomputes this computation.
   */
  recompute(): void {
    if (this.#invalidated && !this.#stopped) {
      try {
        Reactive.runWithComputation(this, () => this.#recomputeCommand());
      } finally {
        this.#invalidated = false;
      }
    }
  }

  /**
   * Adds an invalidate listener that will be invoked the next time this
   * computation is invalidated.
   *
   * @param listener - the listener to run on the next invalidation
   */
  onNextInvalidate(listener: InvalidateListener): void {
    if (!this.#stopped) {
      this.#invalidateListeners.add(listener);
    }
  }
}
