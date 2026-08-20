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

// TypeScript port of the flow-client reactive core (Reactive, Computation,
// ReactiveEventRouter and the event/listener/value types), being built
// alongside the Java versions; once the consumers are wired to it and their
// tests ported to mocha, the Java reactive package is deleted in a cutover.
// Faithful to the Java semantics in com.vaadin.client.flow.reactive.

/** Mirrors elemental.events.EventRemover. */
export interface EventRemover {
  remove(): void;
}

/**
 * Listener that is invoked by {@link flush}.
 *
 * @see {@link addFlushListener}
 */
export type FlushListener = () => void;

/**
 * A reactive value fires reactive value change events when its value changes
 * and registers itself as dependent on the current computation when the value
 * is accessed.
 *
 * A reactive value typically uses a {@link ReactiveEventRouter} for keeping
 * track of listeners, firing events and registering the value as dependent to
 * the current computation.
 */
export interface ReactiveValue {
  /**
   * Adds a listener that has a dependency to this value, and should be
   * notified when this value changes.
   *
   * @param listener - the listener to add
   * @returns an event remover that can be used for removing the added listener
   */
  addReactiveValueChangeListener(listener: ReactiveValueChangeListener): EventRemover;
}

/**
 * Event fired when a reactive value has changed.
 */
export class ReactiveValueChangeEvent {
  readonly #source: ReactiveValue;

  /**
   * Creates a new event fired from a source.
   *
   * @param source - the reactive value that will fire the event
   */
  constructor(source: ReactiveValue) {
    this.#source = source;
  }

  /**
   * Gets the reactive value from which this event originates.
   *
   * @returns the event source
   */
  getSource(): ReactiveValue {
    return this.#source;
  }
}

/**
 * Listens to changes to a reactive value.
 *
 * @see {@link addReactiveValueChangeListener}
 */
export type ReactiveValueChangeListener = (event: ReactiveValueChangeEvent) => void;

/**
 * Event fired when a computation is invalidated.
 */
export class InvalidateEvent {
  readonly #source: Computation;

  /**
   * Creates a new event for computation.
   *
   * @param source - the invalidated computation
   */
  constructor(source: Computation) {
    this.#source = source;
  }

  /**
   * Gets the invalidated computation.
   *
   * @returns the invalidated computation
   */
  getSource(): Computation {
    return this.#source;
  }
}

/**
 * Listens to invalidate events fired by a computation.
 */
export type InvalidateListener = (event: InvalidateEvent) => void;

// Reactive global state (static fields in Reactive.java).
let flushListeners: FlushListener[] = [];
let postFlushListeners: FlushListener[] = [];
let eventCollectors = new Set<ReactiveValueChangeListener>();
let currentComputation: Computation | null = null;
let flushing = false;

/**
 * Automatically reruns the recompute command whenever any reactive value used
 * by it changes. The recompute command is invoked by the next invocation of
 * {@link flush}, unless it has been invoked manually before the global flush. A
 * computation is also scheduled to for an initial "recomputation" when it is
 * created.
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
   * {@link flush} is invoked.
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

/**
 * Handles global features related to reactivity, such as keeping track of the
 * current {@link Computation}, providing a lazy flush cycle and registering
 * reactive event collectors.
 *
 * With a reactive programming model, the dependencies needed for producing a
 * result are automatically registered when the result is computed. When any
 * dependency of a computation is changed, that computation is scheduled to be
 * recomputed. To reduce the number of recomputations performed when many
 * dependencies are updated, the recomputation is performed lazily the next time
 * {@link flush} is invoked.
 *
 * @see {@link Computation}
 */
export const Reactive = {
  /**
   * Adds a listener that will be invoked the next time {@link flush} is
   * invoked. A listener added during a flush will be invoked before that
   * flush finishes.
   *
   * @param flushListener - the flush listener to add
   */
  addFlushListener(flushListener: FlushListener): void {
    flushListeners.push(flushListener);
  },

  /**
   * Adds a listener that will be invoked during the next {@link flush},
   * after all regular flush listeners have been invoked. If a post flush
   * listener adds new flush listeners, those flush listeners will be invoked
   * before the next post flush listener is invoked.
   *
   * @param postFlushListener - the listener to add
   */
  addPostFlushListener(postFlushListener: FlushListener): void {
    postFlushListeners.push(postFlushListener);
  },

  /**
   * Flushes all flush listeners and post flush listeners. A listener is
   * discarded after it has been invoked once. This means that there will be
   * no listeners registered for the next flush at the time this method
   * returns.
   *
   * @see {@link addFlushListener}
   * @see {@link addPostFlushListener}
   */
  flush(): void {
    if (flushing) {
      return;
    }

    let flushListenerIndex = 0;
    let postFlushListenerIndex = 0;

    try {
      flushing = true;

      while (flushListenerIndex < flushListeners.length || postFlushListenerIndex < postFlushListeners.length) {
        // Purge all flush listeners
        while (flushListenerIndex < flushListeners.length) {
          flushListeners[flushListenerIndex]();
          flushListenerIndex++;
        }

        // Purge one post flush listener, then look for new flush listeners
        if (postFlushListenerIndex < postFlushListeners.length) {
          postFlushListeners[postFlushListenerIndex]();
          postFlushListenerIndex++;
        }
      }
    } finally {
      flushing = false;

      flushListeners.splice(0, flushListenerIndex);
      postFlushListeners.splice(0, postFlushListenerIndex);
    }
  },

  /**
   * Gets the currently active computation. Any reactive value that is
   * accessed when a computation is active should be added as a dependency to
   * that computation so that the computation will be invalidated if the value
   * changes.
   *
   * @returns the current computation, or `null` if there is no current
   *   computation.
   */
  getCurrentComputation(): Computation | null {
    return currentComputation;
  },

  /**
   * Runs a task with the given computation set as
   * {@link getCurrentComputation}. If another computation is set as the
   * current computation, it is temporarily replaced by the provided
   * computation, but restored again after the provided task has been run.
   *
   * @param computation - the computation to set as current
   * @param command - the command to run while the computation is set as current
   */
  runWithComputation(computation: Computation | null, command: () => void): void {
    const oldComputation = currentComputation;
    currentComputation = computation;
    try {
      command();
    } finally {
      currentComputation = oldComputation;
    }
  },

  /**
   * Adds a reactive change listener that will be invoked whenever a reactive
   * change event is fired from any reactive event router.
   *
   * @param reactiveValueChangeListener - the listener to add
   * @returns an event remover that can be used to remove the listener
   */
  addEventCollector(reactiveValueChangeListener: ReactiveValueChangeListener): EventRemover {
    eventCollectors.add(reactiveValueChangeListener);
    return {
      remove: () => {
        eventCollectors.delete(reactiveValueChangeListener);
      }
    };
  },

  /**
   * Fires a reactive change event to all registered event collectors.
   *
   * @see {@link addEventCollector}
   *
   * @param event - the fired event
   */
  notifyEventCollectors(event: ReactiveValueChangeEvent): void {
    const copy = new Set(eventCollectors);
    copy.forEach((listener) => listener(event));
  },

  /**
   * Evaluates the given command whenever there is a change in any
   * {@link ReactiveValue} used in the command.
   *
   * @param command - the command to run
   * @returns A {@link Computation} object which can be used to control the
   *   evaluation
   */
  runWhenDependenciesChange(command: () => void): Computation {
    return new Computation(command);
  },

  /**
   * Resets Reactive to the initial state.
   *
   * Intended for test cases to call in setup to avoid having tests affect
   * each other as Reactive state is static and shared.
   *
   * Should never be called from non-test code!
   */
  reset(): void {
    flushListeners = [];
    eventCollectors = new Set();
    currentComputation = null;
    postFlushListeners = [];
  }
};

/**
 * Event router providing integration with reactive features in {@link Reactive}
 * and {@link Computation}. Listeners can be added both for a specific event
 * type and for the generic value change. All events are fired to both types of
 * listeners, as well as to event collectors registered using
 * {@link addEventCollector}.
 *
 * Mirrors ReactiveEventRouter.java in its decoupled, callback form.
 *
 * @typeParam L - the listener type of this router
 * @typeParam E - the reactive event type of this router
 */
export class ReactiveEventRouter<L, E extends ReactiveValueChangeEvent> {
  readonly #listeners = new Set<L>();

  readonly #reactiveValue: ReactiveValue;

  readonly #wrapper: (listener: ReactiveValueChangeListener) => L;

  readonly #dispatcher: (listener: L, event: E) => void;

  /**
   * Creates a new event router for a reactive value.
   *
   * @param reactiveValue - the reactive value, not `null`
   * @param wrapper - callback for wrapping a generic reactive change listener
   *   to an instance of the listener type natively supported by this event
   *   router
   * @param dispatcher - callback for dispatching an event to a listener
   */
  constructor(
    reactiveValue: ReactiveValue,
    wrapper: (listener: ReactiveValueChangeListener) => L,
    dispatcher: (listener: L, event: E) => void
  ) {
    this.#reactiveValue = reactiveValue;
    this.#wrapper = wrapper;
    this.#dispatcher = dispatcher;
  }

  /**
   * Adds a listener to this event router.
   *
   * @param listener - the listener to add, not `null`
   * @returns an event remover that can be used for removing the added listener
   */
  addListener(listener: L): EventRemover {
    this.#listeners.add(listener);
    const remover: EventRemover = {
      remove: () => {
        this.#listeners.delete(listener);
      }
    };

    const computation = Reactive.getCurrentComputation();
    if (computation !== null) {
      computation.onNextInvalidate(() => remover.remove());
    }

    return remover;
  }

  /**
   * Adds a generic reactive change listener to this router.
   *
   * @param reactiveValueChangeListener - the change listener to add, not `null`
   * @returns an event remover that can be used for removing the added listener
   */
  addReactiveListener(reactiveValueChangeListener: ReactiveValueChangeListener): EventRemover {
    return this.addListener(this.#wrapper(reactiveValueChangeListener));
  }

  /**
   * Fires an event to all listeners added to this router using
   * {@link addListener} or {@link addReactiveListener} as well as all
   * global event collectors added using {@link addEventCollector}.
   *
   * @param event - the event to fire
   */
  fireEvent(event: E): void {
    const copy = new Set(this.#listeners);
    copy.forEach((listener) => this.#dispatcher(listener, event));
    Reactive.notifyEventCollectors(event);
  }

  /**
   * Registers access to the data for which this event router fires event.
   * This registers the event source of this event router to be set as a
   * dependency of the current computation if there is one.
   */
  registerRead(): void {
    const computation = Reactive.getCurrentComputation();
    if (computation !== null) {
      computation.addDependency(this.#reactiveValue);
    }
  }

  /**
   * Gets the reactive value for which this router fires event.
   *
   * @returns the reactive value
   */
  getReactiveValue(): ReactiveValue {
    return this.#reactiveValue;
  }
}
