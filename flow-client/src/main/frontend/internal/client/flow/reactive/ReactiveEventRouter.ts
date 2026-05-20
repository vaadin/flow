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

interface EventRemover {
  remove(): void;
}

interface ReactiveValueChangeListenerLike {
  onValueChange(event: unknown): void;
}

interface ComputationLike {
  onValueChange(event: unknown): void;
  addDependencyRemover(remover: () => void): void;
  onNextInvalidate(listener: { onInvalidate(event: { getSource(): unknown }): void }): void;
}

/**
 * Event router providing integration with reactive features. Migrated from
 * `com.vaadin.client.flow.reactive.ReactiveEventRouter`.
 *
 * <p>The Java abstract-class shape with overridable `wrap` and `dispatchEvent`
 * methods is replaced by constructor-callback parameters of the same shape, so
 * Java sites that previously used anonymous subclasses (`new ReactiveEventRouter
 * <L,E>(reactiveValue) { ... }`) now pass the same logic as two function
 * arguments.
 */
export class ReactiveEventRouter {
  private readonly listeners = new Set<unknown>();
  private readonly reactiveValue: unknown;
  private readonly wrapFn: (listener: ReactiveValueChangeListenerLike) => unknown;
  private readonly dispatchFn: (listener: unknown, event: unknown) => void;

  constructor(
    reactiveValue: unknown,
    wrapFn: (listener: ReactiveValueChangeListenerLike) => unknown,
    dispatchFn: (listener: unknown, event: unknown) => void
  ) {
    this.reactiveValue = reactiveValue;
    this.wrapFn = wrapFn;
    this.dispatchFn = dispatchFn;
  }

  addListener(listener: unknown): EventRemover {
    this.listeners.add(listener);
    const remover: EventRemover = {
      remove: () => {
        this.listeners.delete(listener);
      }
    };

    const computation = Reactive.getCurrentComputation() as ComputationLike | null;
    if (computation) {
      computation.onNextInvalidate({
        onInvalidate: () => remover.remove()
      });
    }

    return remover;
  }

  addReactiveListener(listener: ReactiveValueChangeListenerLike): EventRemover {
    return this.addListener(this.wrapFn(listener));
  }

  fireEvent(event: unknown): void {
    // Snapshot to tolerate mutations during dispatch (matches Java semantics).
    const snapshot = Array.from(this.listeners);
    for (const listener of snapshot) {
      this.dispatchFn(listener, event);
    }
    Reactive.notifyEventCollectors(event);
  }

  registerRead(): void {
    const computation = Reactive.getCurrentComputation() as ComputationLike | null;
    if (computation) {
      const remover = this.addReactiveListener({
        onValueChange: (event) => computation.onValueChange(event)
      });
      computation.addDependencyRemover(() => remover.remove());
    }
  }

  getReactiveValue(): unknown {
    return this.reactiveValue;
  }
}
