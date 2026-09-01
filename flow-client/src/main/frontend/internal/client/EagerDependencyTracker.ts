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

// The eager-dependency-loading gate of com.vaadin.client.DependencyLoader,
// extracted as a standalone, pure-logic unit. It counts in-flight eager
// dependency loads and runs queued commands once all of them have finished
// (e.g. MessageHandler defers processing the message until its eager
// dependencies are loaded). The resource loading itself lives in the rest of
// DependencyLoader, on ResourceLoader.

import type { Command } from './Command';

// Number of eager dependencies currently loading.
let eagerDependenciesLoading = 0;

// Commands to run once all eager dependencies have finished loading.
let callbacks: Array<() => void> = [];

/**
 * Adds a command to be run when all eager dependencies have finished loading.
 *
 * If no eager dependencies are currently being loaded, runs the command
 * immediately.
 *
 * @see {@link startEagerDependencyLoading}
 * @see {@link endEagerDependencyLoading}
 *
 * @param command - the command to run when eager dependencies have been loaded
 */
export function runWhenEagerDependenciesLoaded(command: Command): void {
  if (eagerDependenciesLoading === 0) {
    command();
  } else {
    callbacks.push(command);
  }
}

/**
 * Marks that loading of a dependency has started.
 *
 * @see {@link runWhenEagerDependenciesLoaded}
 * @see {@link endEagerDependencyLoading}
 */
export function startEagerDependencyLoading(): void {
  eagerDependenciesLoading++;
}

/**
 * Marks that loading of a dependency has ended.
 *
 * If all pending dependencies have been loaded, calls any callback registered
 * using {@link runWhenEagerDependenciesLoaded}. The callbacks are cleared even
 * if one of them throws.
 */
export function endEagerDependencyLoading(): void {
  eagerDependenciesLoading--;
  if (eagerDependenciesLoading === 0 && callbacks.length > 0) {
    try {
      // Index loop (re-reading length) so commands queued during the run are
      // also executed, mirroring the Java loop.
      // eslint-disable-next-line @typescript-eslint/prefer-for-of -- length is re-read so mid-run additions run too
      for (let i = 0; i < callbacks.length; i++) {
        callbacks[i]();
      }
    } finally {
      callbacks = [];
    }
  }
}
