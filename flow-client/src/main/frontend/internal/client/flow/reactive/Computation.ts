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
import { Reactive } from './Reactive';

interface InvalidateListener {
  onInvalidate(event: { getSource(): Computation }): void;
}

/**
 * Automatically reruns the constructor-supplied `recompute` callback whenever
 * any reactive value used by it changes. Migrated from
 * `com.vaadin.client.flow.reactive.Computation`.
 *
 * <p>The Java side is the orchestrator for adding dependencies — Java's
 * `ReactiveEventRouter.registerRead` calls `addReactiveListener(...)` itself
 * and passes the resulting remover to {@link addDependencyRemover}, so this
 * TS class never has to dispatch back into Java method names by string.
 *
 * <p>{@link onValueChange} is the listener method invoked by Java when one of
 * the registered dependencies fires a change event.
 */
export class Computation {
  private invalidated = false;
  private stopped = false;
  private removers: Array<() => void> = [];
  private invalidateListeners = new Set<InvalidateListener>();
  private readonly recomputeFn: () => void;

  constructor(recompute: () => void) {
    this.recomputeFn = recompute;
    // Schedule an initial recompute.
    this.invalidate();
  }

  /**
   * Stores the dependency-removal callback returned by Java when this
   * computation was registered as a listener on a reactive value. Java does
   * the actual `addReactiveValueChangeListener` call.
   */
  addDependencyRemover(remover: () => void): void {
    if (!this.stopped) {
      this.removers.push(remover);
    }
  }

  /** Receives reactive change events from any registered dependency. */
  onValueChange(_changeEvent: unknown): void {
    if (this.invalidated || this.stopped) {
      return;
    }
    this.invalidate();
  }

  private invalidate(): void {
    this.invalidated = true;
    this.clearDependencies();

    if (!this.stopped) {
      Reactive.addFlushListener(() => this.recompute());
    }

    if (this.invalidateListeners.size !== 0) {
      const oldListeners = this.invalidateListeners;
      this.invalidateListeners = new Set();
      const event = { getSource: (): Computation => this };
      oldListeners.forEach((listener) => listener.onInvalidate(event));
    }
  }

  private clearDependencies(): void {
    while (this.removers.length > 0) {
      const remover = this.removers.shift();
      remover?.();
    }
  }

  /** Stops this computation, so that it will no longer be recomputed. */
  stop(): void {
    this.stopped = true;
    this.invalidate();
    this.invalidateListeners.clear();
    this.clearDependencies();
  }

  /**
   * Checks whether this computation is invalidated. An invalidated computation
   * will eventually be recomputed (unless it has also been stopped).
   */
  isInvalidated(): boolean {
    return this.invalidated;
  }

  /** Recomputes this computation. */
  recompute(): void {
    if (this.invalidated && !this.stopped) {
      try {
        Reactive.runWithComputation(this, () => this.recomputeFn());
      } finally {
        this.invalidated = false;
      }
    }
  }

  /**
   * Adds an invalidate listener that will be invoked the next time this
   * computation is invalidated.
   */
  onNextInvalidate(listener: InvalidateListener): void {
    if (!this.stopped) {
      this.invalidateListeners.add(listener);
    }
  }
}
