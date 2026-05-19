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

/**
 * Global reactive state — flush queue, current computation, event
 * collectors. Migrated from `com.vaadin.client.flow.reactive.Reactive`.
 *
 * Java callers still see the Java `Reactive` API; this module owns the
 * actual state and is reached via the `NativeReactive` JsType shim. Reactive
 * value types (`Computation`, `ReactiveValueChangeEvent`, ...) remain
 * opaque to this module — they are just passed back to Java callbacks.
 *
 * `Reactive.runWhenDependenciesChange` is *not* in this module: it
 * constructs a Java `Computation` subclass, which can't be expressed from
 * TS. That helper stays on the Java side of `Reactive.java`.
 */

type FlushListener = () => void;
type ChangeListener = (event: unknown) => void;
type ComputationHandle = unknown;

let flushListeners: FlushListener[] = [];
let postFlushListeners: FlushListener[] = [];
const eventCollectors = new Set<ChangeListener>();
let currentComputation: ComputationHandle = null;
let flushing = false;

export const Reactive = {
  addFlushListener(listener: FlushListener): void {
    flushListeners.push(listener);
  },

  addPostFlushListener(listener: FlushListener): void {
    postFlushListeners.push(listener);
  },

  flush(): void {
    if (flushing) {
      return;
    }

    let flushIndex = 0;
    let postFlushIndex = 0;

    flushing = true;
    try {
      while (flushIndex < flushListeners.length || postFlushIndex < postFlushListeners.length) {
        // Drain all flush listeners first.
        while (flushIndex < flushListeners.length) {
          flushListeners[flushIndex]();
          flushIndex++;
        }
        // Drain one post-flush listener, then re-check whether new flush
        // listeners have appeared.
        if (postFlushIndex < postFlushListeners.length) {
          postFlushListeners[postFlushIndex]();
          postFlushIndex++;
        }
      }
    } finally {
      flushing = false;
      flushListeners.splice(0, flushIndex);
      postFlushListeners.splice(0, postFlushIndex);
    }
  },

  getCurrentComputation(): ComputationHandle {
    return currentComputation;
  },

  runWithComputation(computation: ComputationHandle, command: () => void): void {
    const old = currentComputation;
    currentComputation = computation;
    try {
      command();
    } finally {
      currentComputation = old;
    }
  },

  addEventCollector(listener: ChangeListener): () => void {
    eventCollectors.add(listener);
    return () => {
      eventCollectors.delete(listener);
    };
  },

  notifyEventCollectors(event: unknown): void {
    if (eventCollectors.size === 0) {
      return;
    }
    // Snapshot to tolerate mutations during dispatch (matches the Java behaviour).
    const snapshot = Array.from(eventCollectors);
    for (const listener of snapshot) {
      listener(event);
    }
  },

  reset(): void {
    flushListeners = [];
    postFlushListeners = [];
    eventCollectors.clear();
    currentComputation = null;
    flushing = false;
  }
};
