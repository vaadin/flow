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

/** A listener invoked on the next Reactive.flush(); mirrors FlushListener. */
export type FlushListener = () => void;

/** A reactive value whose reads can be tracked as computation dependencies. */
export interface ReactiveValue {
  addReactiveValueChangeListener(listener: ReactiveValueChangeListener): EventRemover;
}

/** Base reactive change event carrying its source; mirrors ReactiveValueChangeEvent. */
export class ReactiveValueChangeEvent {
  private readonly source: ReactiveValue;

  constructor(source: ReactiveValue) {
    this.source = source;
  }

  getSource(): ReactiveValue {
    return this.source;
  }
}

/** Listener for reactive change events; mirrors ReactiveValueChangeListener. */
export type ReactiveValueChangeListener = (event: ReactiveValueChangeEvent) => void;

/** Event fired when a computation is invalidated; mirrors InvalidateEvent. */
export class InvalidateEvent {
  private readonly source: Computation;

  constructor(source: Computation) {
    this.source = source;
  }

  getSource(): Computation {
    return this.source;
  }
}

/** Listener for computation invalidation; mirrors InvalidateListener. */
export type InvalidateListener = (event: InvalidateEvent) => void;

// Reactive global state (static fields in Reactive.java).
let flushListeners: FlushListener[] = [];
let postFlushListeners: FlushListener[] = [];
let eventCollectors = new Set<ReactiveValueChangeListener>();
let currentComputation: Computation | null = null;
let flushing = false;

/**
 * Automatically reruns its recompute command whenever any reactive value used
 * by it changes. Mirrors Computation.java (in its decoupled, callback form).
 */
export class Computation {
  private invalidated = false;

  private stopped = false;

  private readonly dependencies: EventRemover[] = [];

  private invalidateListeners = new Set<InvalidateListener>();

  private readonly recomputeCommand: () => void;

  constructor(recomputeCommand: () => void) {
    this.recomputeCommand = recomputeCommand;
    // Make sure a recompute is scheduled
    this.invalidate();
  }

  addDependency(dependency: ReactiveValue): void {
    if (!this.stopped) {
      const remover = dependency.addReactiveValueChangeListener((event) => this.onValueChange(event));
      this.dependencies.push(remover);
    }
  }

  onValueChange(_changeEvent: ReactiveValueChangeEvent): void {
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

    // Fire invalidate events
    if (this.invalidateListeners.size !== 0) {
      const oldListeners = this.invalidateListeners;
      this.invalidateListeners = new Set();

      const invalidateEvent = new InvalidateEvent(this);

      oldListeners.forEach((listener) => listener(invalidateEvent));
    }
  }

  private clearDependencies(): void {
    while (this.dependencies.length > 0) {
      const remover = this.dependencies.shift();
      remover?.remove();
    }
  }

  stop(): void {
    this.stopped = true;

    this.invalidate();

    // Prevent firing more events
    this.invalidateListeners.clear();

    // Release memory
    this.clearDependencies();
  }

  isInvalidated(): boolean {
    return this.invalidated;
  }

  recompute(): void {
    if (this.invalidated && !this.stopped) {
      try {
        Reactive.runWithComputation(this, () => this.recomputeCommand());
      } finally {
        this.invalidated = false;
      }
    }
  }

  onNextInvalidate(listener: InvalidateListener): void {
    if (!this.stopped) {
      this.invalidateListeners.add(listener);
    }
  }
}

/**
 * Global reactivity coordinator: the lazy flush cycle, the current computation,
 * and reactive event collectors. Mirrors the static Reactive.java.
 */
export const Reactive = {
  addFlushListener(flushListener: FlushListener): void {
    flushListeners.push(flushListener);
  },

  addPostFlushListener(postFlushListener: FlushListener): void {
    postFlushListeners.push(postFlushListener);
  },

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

  getCurrentComputation(): Computation | null {
    return currentComputation;
  },

  runWithComputation(computation: Computation | null, command: () => void): void {
    const oldComputation = currentComputation;
    currentComputation = computation;
    try {
      command();
    } finally {
      currentComputation = oldComputation;
    }
  },

  addEventCollector(reactiveValueChangeListener: ReactiveValueChangeListener): EventRemover {
    eventCollectors.add(reactiveValueChangeListener);
    return {
      remove: () => {
        eventCollectors.delete(reactiveValueChangeListener);
      }
    };
  },

  notifyEventCollectors(event: ReactiveValueChangeEvent): void {
    const copy = new Set(eventCollectors);
    copy.forEach((listener) => listener(event));
  },

  runWhenDependenciesChange(command: () => void): Computation {
    return new Computation(command);
  },

  reset(): void {
    flushListeners = [];
    eventCollectors = new Set();
    currentComputation = null;
    postFlushListeners = [];
  }
};

/**
 * Routes events to listeners and integrates with the reactive computation
 * tracking. Mirrors ReactiveEventRouter.java (in its decoupled, callback form).
 */
export class ReactiveEventRouter<L, E extends ReactiveValueChangeEvent> {
  private readonly listeners = new Set<L>();

  private readonly reactiveValue: ReactiveValue;

  private readonly wrapper: (listener: ReactiveValueChangeListener) => L;

  private readonly dispatcher: (listener: L, event: E) => void;

  constructor(
    reactiveValue: ReactiveValue,
    wrapper: (listener: ReactiveValueChangeListener) => L,
    dispatcher: (listener: L, event: E) => void
  ) {
    this.reactiveValue = reactiveValue;
    this.wrapper = wrapper;
    this.dispatcher = dispatcher;
  }

  addListener(listener: L): EventRemover {
    this.listeners.add(listener);
    const remover: EventRemover = {
      remove: () => {
        this.listeners.delete(listener);
      }
    };

    const computation = Reactive.getCurrentComputation();
    if (computation !== null) {
      computation.onNextInvalidate(() => remover.remove());
    }

    return remover;
  }

  addReactiveListener(reactiveValueChangeListener: ReactiveValueChangeListener): EventRemover {
    return this.addListener(this.wrapper(reactiveValueChangeListener));
  }

  fireEvent(event: E): void {
    const copy = new Set(this.listeners);
    copy.forEach((listener) => this.dispatcher(listener, event));
    Reactive.notifyEventCollectors(event);
  }

  registerRead(): void {
    const computation = Reactive.getCurrentComputation();
    if (computation !== null) {
      computation.addDependency(this.reactiveValue);
    }
  }

  getReactiveValue(): ReactiveValue {
    return this.reactiveValue;
  }
}
