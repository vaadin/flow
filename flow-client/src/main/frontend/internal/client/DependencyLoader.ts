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

import { Console } from './Console';
import type { ResourceLoadEvent, ResourceLoadListener, ResourceLoader } from './ResourceLoader';

// Mirrors com.vaadin.flow.shared.ui.Dependency.Type enum values. The strings
// match `name()` so JSON payloads coming from the server are matched directly.
const TYPE_STYLESHEET = 'STYLESHEET';
const TYPE_JAVASCRIPT = 'JAVASCRIPT';
const TYPE_JS_MODULE = 'JS_MODULE';
const TYPE_DYNAMIC_IMPORT = 'DYNAMIC_IMPORT';

// Mirrors com.vaadin.flow.shared.ui.LoadMode enum values.
const LOAD_MODE_EAGER = 'EAGER';
const LOAD_MODE_LAZY = 'LAZY';
const LOAD_MODE_INLINE = 'INLINE';

// Mirrors com.vaadin.flow.shared.ui.Dependency string keys for JSON payloads.
const KEY_TYPE = 'type';
const KEY_URL = 'url';
const KEY_CONTENTS = 'contents';
const KEY_ID = 'id';

type DependencyJson = Record<string, unknown>;

/**
 * Loader function: takes a payload (URL or inline contents) and a listener.
 * Used internally to dispatch into the right ResourceLoader method based on
 * the dependency type and load mode.
 */
type Loader = (payload: string, listener: ResourceLoadListener) => void;

/** Minimal URI resolver shape — a thin slice of the URIResolver TS facade. */
interface UriResolverLike {
  resolveVaadinUri(uri: string): string;
}

// Module-level state that mirrors the static fields of the Java DependencyLoader.
const callbacks: Array<() => void> = [];
let eagerDependenciesLoading = 0;

const EAGER_RESOURCE_LOAD_LISTENER: ResourceLoadListener = {
  onLoad(_event: ResourceLoadEvent): void {
    // Call start for next before calling end for current
    endEagerDependencyLoading();
  },
  onError(event: ResourceLoadEvent): void {
    Console.error(`'${event.getResourceData()}' could not be loaded.`);
    // The show must go on
    endEagerDependencyLoading();
  }
};

const LAZY_RESOURCE_LOAD_LISTENER: ResourceLoadListener = {
  onLoad(_event: ResourceLoadEvent): void {
    // Do nothing on success, simply continue loading
  },
  onError(event: ResourceLoadEvent): void {
    Console.error(`${event.getResourceData()} could not be loaded.`);
  }
};

/**
 * Adds a command to be run when all eager dependencies have finished loading.
 * <p>
 * If no eager dependencies are currently being loaded, runs the command immediately.
 */
function runWhenEagerDependenciesLoaded(command: () => void): void {
  if (eagerDependenciesLoading === 0) {
    command();
  } else {
    callbacks.push(command);
  }
}

/** Marks that loading of a dependency has started. */
function startEagerDependencyLoading(): void {
  eagerDependenciesLoading++;
}

/**
 * Marks that loading of a dependency has ended. If all pending dependencies
 * have been loaded, calls any callback registered using
 * {@link runWhenEagerDependenciesLoaded}.
 */
function endEagerDependencyLoading(): void {
  eagerDependenciesLoading--;
  if (eagerDependenciesLoading === 0 && callbacks.length !== 0) {
    try {
      for (const cmd of callbacks) {
        cmd();
      }
    } finally {
      callbacks.length = 0;
    }
  }
}

function getDependencyType(dependencyJson: DependencyJson): string {
  return dependencyJson[KEY_TYPE] as string;
}

function getDependencyId(dependencyJson: DependencyJson): string | null {
  const id = dependencyJson[KEY_ID];
  return typeof id === 'string' ? id : null;
}

/**
 * Handles loading of dependencies (stylesheets and scripts) in the application.
 *
 * Migrated from {@code com.vaadin.client.DependencyLoader}.
 */
export class DependencyLoader {
  private readonly uriResolver: UriResolverLike;
  private readonly resourceLoader: ResourceLoader;

  constructor(uriResolver: UriResolverLike, resourceLoader: ResourceLoader) {
    this.uriResolver = uriResolver;
    this.resourceLoader = resourceLoader;
  }

  private loadLazyDependency(dependencyUrl: string, loader: Loader): void {
    loader(dependencyUrl, LAZY_RESOURCE_LOAD_LISTENER);
  }

  private loadDependencyEagerly(data: string, loader: Loader): void {
    startEagerDependencyLoading();
    loader(data, EAGER_RESOURCE_LOAD_LISTENER);
  }

  /**
   * Triggers loading of the given dependencies.
   *
   * @param clientDependencies the map of dependencies to load, divided into
   *                           groups by load mode, not {@code null}.
   *                           Keys are {@link LoadMode#name()} strings.
   */
  loadDependencies(clientDependencies: Map<string, unknown[]>): void {
    const lazyDependencies = new Map<string, Loader>();

    clientDependencies.forEach((dependencies, mode) => {
      const extracted = this.extractLazyDependenciesAndLoadOthers(mode, dependencies);
      extracted.forEach((loader, url) => lazyDependencies.set(url, loader));
    });

    // Postpone load dependencies execution after the browser event loop to
    // make it possible to execute all other commands that should be run after
    // the eager dependencies, so that lazy dependencies don't block those
    // commands.
    if (lazyDependencies.size !== 0) {
      runWhenEagerDependenciesLoaded(() =>
        setTimeout(() => {
          Console.debug('Finished loading eager dependencies, loading lazy.');
          lazyDependencies.forEach((loader, url) => this.loadLazyDependency(url, loader));
        }, 0)
      );
    }
  }

  private extractLazyDependenciesAndLoadOthers(loadMode: string, dependencies: unknown[]): Map<string, Loader> {
    const lazyDependencies = new Map<string, Loader>();
    for (const entry of dependencies) {
      const dependencyJson = entry as DependencyJson;
      const type = getDependencyType(dependencyJson);
      const dependencyId = getDependencyId(dependencyJson);
      const resourceLoader = this.getResourceLoaderFn(type, loadMode, dependencyId);

      if (type === TYPE_DYNAMIC_IMPORT) {
        this.loadDependencyEagerly(dependencyJson[KEY_URL] as string, resourceLoader);
      } else {
        switch (loadMode) {
          case LOAD_MODE_EAGER:
            this.loadDependencyEagerly(this.getDependencyUrl(dependencyJson), resourceLoader);
            break;
          case LOAD_MODE_LAZY:
            lazyDependencies.set(this.getDependencyUrl(dependencyJson), resourceLoader);
            break;
          case LOAD_MODE_INLINE:
            this.loadDependencyEagerly(dependencyJson[KEY_CONTENTS] as string, resourceLoader);
            break;
          default:
            throw new Error(`Unknown load mode = ${loadMode}`);
        }
      }
    }
    return lazyDependencies;
  }

  private getDependencyUrl(dependencyJson: DependencyJson): string {
    return this.uriResolver.resolveVaadinUri(dependencyJson[KEY_URL] as string);
  }

  private getResourceLoaderFn(resourceType: string, loadMode: string, dependencyId: string | null): Loader {
    const resourceLoader = this.resourceLoader;
    const inline = loadMode === LOAD_MODE_INLINE;

    switch (resourceType) {
      case TYPE_STYLESHEET:
        if (inline) {
          return (data, listener) =>
            resourceLoader.inlineStyleSheet(
              data,
              (e) => listener.onLoad(e),
              (e) => listener.onError(e),
              dependencyId
            );
        }
        return (url, listener) =>
          resourceLoader.loadStylesheet(
            url,
            (e) => listener.onLoad(e),
            (e) => listener.onError(e),
            dependencyId
          );
      case TYPE_JAVASCRIPT:
        if (inline) {
          return (data, listener) =>
            resourceLoader.inlineScript(
              data,
              (e) => listener.onLoad(e),
              (e) => listener.onError(e)
            );
        }
        return (scriptUrl, listener) =>
          resourceLoader.loadScriptAsyncDefer(
            scriptUrl,
            (e) => listener.onLoad(e),
            (e) => listener.onError(e),
            false,
            true
          );
      case TYPE_JS_MODULE:
        if (inline) {
          throw new Error('Inline load mode is not supported for JsModule.');
        }
        return (scriptUrl, listener) =>
          resourceLoader.loadJsModule(
            scriptUrl,
            (e) => listener.onLoad(e),
            (e) => listener.onError(e),
            false,
            true
          );
      case TYPE_DYNAMIC_IMPORT:
        return (expression, listener) =>
          resourceLoader.loadDynamicImport(
            expression,
            (e) => listener.onLoad(e),
            (e) => listener.onError(e)
          );
      default:
        throw new Error(`Unknown dependency type ${resourceType}`);
    }
  }

  /**
   * Prevents eager dependencies from being considered as loaded until
   * {@code HTMLImports.whenReady} has been run.
   */
  requireHtmlImportsReady(): void {
    startEagerDependencyLoading();
    this.resourceLoader.runWhenHtmlImportsReady(() => endEagerDependencyLoading());
  }

  /** Static entry point matching the Java {@code DependencyLoader#runWhenEagerDependenciesLoaded}. */
  static runWhenEagerDependenciesLoaded(command: () => void): void {
    runWhenEagerDependenciesLoaded(command);
  }
}
