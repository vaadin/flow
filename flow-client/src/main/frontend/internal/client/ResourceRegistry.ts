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

import type { Registry } from './Registry';
import type { ResourceLoader } from './ResourceLoader';
import { Console } from './Console';

// The resource bookkeeping + listener-fanout kernel of
// com.vaadin.client.ResourceLoader, extracted as a standalone, testable unit.
// It dedupes resources by key (URL or content), fans load/error notifications
// out to the listeners registered for a key, and supports removing a resource by
// its dependency id. The DOM element creation in ResourceLoader's
// loadScript/loadStylesheet/... composes this kernel.

/** Information about a (loaded or failed) resource; mirrors ResourceLoadEvent. */
export interface ResourceLoadEvent {
  /**
   * Gets the resource loader that has fired this event.
   *
   * @returns the resource loader
   */
  getResourceLoader(): ResourceLoader;

  /**
   * Gets the absolute url or content of the loaded resource or the JS expression
   * that imports the resource.
   *
   * @returns the absolute url or content of the loaded resource or the JS
   *          expression that imports the resource
   */
  getResourceData(): string;
}

/** Notified when a resource has loaded or failed; mirrors ResourceLoadListener. */
export interface ResourceLoadListener {
  /**
   * Notifies this ResourceLoadListener that a resource has been loaded. Some
   * browsers do not support any way of detecting load errors. In these cases,
   * onLoad will be called regardless of the status.
   *
   * @see {@link ResourceLoadEvent}
   *
   * @param event - a resource load event with information about the loaded
   *          resource
   */
  onLoad(event: ResourceLoadEvent): void;

  /**
   * Notifies this ResourceLoadListener that a resource could not be loaded, e.g.
   * because the file could not be found or because the server did not respond.
   * Some browsers do not support any way of detecting load errors. In these
   * cases, onLoad will be called regardless of the status.
   *
   * @see {@link ResourceLoadEvent}
   *
   * @param event - a resource load event with information about the resource
   *          that could not be loaded.
   */
  onError(event: ResourceLoadEvent): void;
}

/** Tracks loaded resources and their listeners; the dedup/fanout kernel of ResourceLoader. */
export class ResourceRegistry {
  readonly #loadedResources = new Set<string>();

  readonly #loadListeners = new Map<string, Array<ResourceLoadListener | null>>();

  // Maps a dependency id to its resource key (URL/content) for removal.
  readonly #dependencyIdToResourceKey = new Map<string, string>();

  readonly #registry: Registry;

  constructor(registry: Registry) {
    this.#registry = registry;
  }

  /** Whether the resource identified by the given key has finished loading. */
  isLoaded(key: string): boolean {
    return this.#loadedResources.has(key);
  }

  /** Marks a resource key as already loaded (e.g. discovered in the DOM). */
  markLoaded(key: string): void {
    this.#loadedResources.add(key);
  }

  /** Associates a dependency id with its resource key, for later removal. */
  registerDependencyId(dependencyId: string, resourceKey: string): void {
    this.#dependencyIdToResourceKey.set(dependencyId, resourceKey);
  }

  /**
   * Registers a load listener for a resource key, returning true if it is the
   * first listener (i.e. the caller should start loading the resource). Mirrors
   * ResourceLoader.addListener.
   */
  addListener(resourceId: string, listener: ResourceLoadListener | null): boolean {
    const listeners = this.#loadListeners.get(resourceId);
    if (listeners === undefined) {
      this.#loadListeners.set(resourceId, [listener]);
      return true;
    }
    listeners.push(listener);
    return false;
  }

  /** Marks the resource loaded and notifies (then clears) its listeners. Mirrors fireLoad. */
  fireLoad(event: ResourceLoadEvent): void {
    Console.debug(`Loaded ${event.getResourceData()}`);
    const resource = event.getResourceData();
    const listeners = this.#loadListeners.get(resource);
    this.#loadedResources.add(resource);
    this.#loadListeners.delete(resource);
    listeners?.forEach((listener) => listener?.onLoad(event));
  }

  /** Reports the error and notifies (then clears) the resource's listeners. Mirrors fireError. */
  fireError(event: ResourceLoadEvent): void {
    // Java resolves the handler through the registry at each failure, so a
    // registry reset is picked up rather than bound at construction.
    this.#registry.getSystemErrorHandler().handleError(`Error loading ${event.getResourceData()}`);
    const resource = event.getResourceData();
    const listeners = this.#loadListeners.get(resource);
    this.#loadListeners.delete(resource);
    listeners?.forEach((listener) => listener?.onError(event));
  }

  /** Clears a resource (loaded flag + listeners + mapping) by its dependency id. */
  clearLoadedResourceById(dependencyId: string): void {
    const resourceKey = this.#dependencyIdToResourceKey.get(dependencyId);
    if (resourceKey !== undefined) {
      this.#loadedResources.delete(resourceKey);
      this.#loadListeners.delete(resourceKey);
      this.#dependencyIdToResourceKey.delete(dependencyId);
    }
  }
}
