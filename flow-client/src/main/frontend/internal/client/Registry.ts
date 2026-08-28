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

import { assert } from '../assert';

// TypeScript port of the container mechanism of com.vaadin.client.Registry.
// The Registry is a holder of singleton services (MessageSender, StateTree, ...)
// looked up by a key; some are resettable via a supplier so the whole set can be
// recreated. The original Java class keyed by Class<?>; here a service is keyed
// by an opaque token (a string, symbol or constructor) since some TS services
// are functions, not classes.
//
// Registry.java also declares 24 typed getters (getMessageSender, getStateTree,
// ...). They are not ported yet because 14 of their return types are not: the
// modules that need a service therefore still declare a local interface naming
// the getters they call. Those stand-ins collapse into this class once the
// remaining services land.

/** A token identifying a registered service (its class/constructor, a symbol, or a name). */
export type ServiceKey = unknown;

/** A holder of singleton services looked up by key; mirrors Registry.java's container. */
export class Registry {
  readonly #lookupTable = new Map<ServiceKey, unknown>();

  readonly #resettable = new Map<ServiceKey, () => unknown>();

  /**
   * Stores an instance of the given type.
   *
   * @param type - the type to store
   * @param instance - the instance to store
   * @typeParam T - the type
   */
  protected set<T>(type: ServiceKey, instance: T): void {
    assert(!this.#lookupTable.has(type), 'Registry already has a class of this type registered');
    this.#lookupTable.set(type, instance);
  }

  /**
   * Stores an instance created by the given supplier and remembers the supplier
   * so the instance can be recreated by reset(). Mirrors Registry.set(Class,
   * Supplier).
   */
  protected setResettable<T>(type: ServiceKey, instanceSupplier: () => T): void {
    this.set(type, instanceSupplier());
    this.#resettable.set(type, instanceSupplier);
  }

  /**
   * Gets the instance registered for the given type. Throws if none has been
   * registered. Mirrors Registry.get(Class).
   */
  protected get<T>(type: ServiceKey): T {
    assert(this.#lookupTable.has(type), 'Tried to look up a type but no instance has been registered');
    return this.#lookupTable.get(type) as T;
  }

  /** Whether an instance is registered for the given type. */
  protected has(type: ServiceKey): boolean {
    return this.#lookupTable.has(type);
  }

  /**
   * Deletes and recreates resettable instances of registry singletons.
   */
  reset(): void {
    this.#resettable.forEach((supplier, key) => {
      this.#lookupTable.delete(key);
      this.#lookupTable.set(key, supplier());
    });
  }
}
