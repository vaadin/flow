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

// TypeScript port of com.vaadin.client.DependencyLoader, built alongside the Java
// version. It loads stylesheet/script dependencies grouped by load mode (eager
// load before lazy; inline counts as eager), routing each to the right
// ResourceLoader method by dependency type, and resolves Vaadin URIs via the
// URIResolver. Composes the ported EagerDependencyTracker (eager-load gate) and
// ResourceLoader. The Registry members are contracts satisfied at cutover.

import {
  endEagerDependencyLoading,
  runWhenEagerDependenciesLoaded,
  startEagerDependencyLoading
} from './EagerDependencyTracker';
import type { ResourceLoadEvent, ResourceLoadListener } from './ResourceRegistry';

// com.vaadin.flow.shared.ui.Dependency
const KEY_URL = 'url';
const KEY_TYPE = 'type';
const KEY_CONTENTS = 'contents';
const KEY_ID = 'id';

// com.vaadin.flow.shared.ui.Dependency.Type / LoadMode
type DependencyType = 'STYLESHEET' | 'JAVASCRIPT' | 'JS_MODULE' | 'DYNAMIC_IMPORT';
type LoadMode = 'INLINE' | 'EAGER' | 'LAZY';

type Dependency = Record<string, unknown>;
type Loader = (data: string, listener: ResourceLoadListener) => void;

/** The ResourceLoader methods DependencyLoader drives. */
interface DependencyResourceLoader {
  loadScript(scriptUrl: string, listener: ResourceLoadListener | null, async?: boolean, defer?: boolean): void;
  loadJsModule(scriptUrl: string, listener: ResourceLoadListener | null, async?: boolean, defer?: boolean): void;
  inlineScript(scriptContents: string, listener: ResourceLoadListener | null): void;
  loadStylesheet(stylesheetUrl: string, listener: ResourceLoadListener | null, dependencyId?: string | null): void;
  inlineStyleSheet(
    styleSheetContents: string,
    listener: ResourceLoadListener | null,
    dependencyId?: string | null
  ): void;
  loadDynamicImport(expression: string, listener: ResourceLoadListener): void;
  runWhenHtmlImportsReady(task: () => void): void;
}

/** The slice of Registry DependencyLoader uses. */
interface DependencyLoaderRegistry {
  getURIResolver(): { resolveVaadinUri(uri: string): string | null };
  getResourceLoader(): DependencyResourceLoader;
}

/** Loads stylesheet/script dependencies; mirrors DependencyLoader.java. */
export class DependencyLoader {
  private readonly registry: DependencyLoaderRegistry;

  // Loads the next eager dependency when the current one completes.
  private readonly eagerListener: ResourceLoadListener = {
    onLoad: () => endEagerDependencyLoading(),
    onError: (event: ResourceLoadEvent) => {
      console.error(`'${event.getResourceData()}' could not be loaded.`);
      // The show must go on.
      endEagerDependencyLoading();
    }
  };

  private readonly lazyListener: ResourceLoadListener = {
    onLoad: () => {
      // Nothing to do on success; simply continue loading.
    },
    onError: (event: ResourceLoadEvent) => {
      console.error(`${event.getResourceData()} could not be loaded.`);
    }
  };

  constructor(registry: DependencyLoaderRegistry) {
    this.registry = registry;
  }

  private loadLazyDependency(dependencyUrl: string, loader: Loader): void {
    loader(dependencyUrl, this.lazyListener);
  }

  private loadDependencyEagerly(data: string, loader: Loader): void {
    startEagerDependencyLoading();
    loader(data, this.eagerListener);
  }

  /** Triggers loading of the given dependencies, grouped by load mode. */
  loadDependencies(clientDependencies: Map<LoadMode, Dependency[]>): void {
    const lazyDependencies = new Map<string, Loader>();
    clientDependencies.forEach((dependencies, mode) => {
      this.extractLazyDependenciesAndLoadOthers(mode, dependencies).forEach((loader, url) => {
        lazyDependencies.set(url, loader);
      });
    });

    // Postpone lazy dependencies until after the eager ones (and the browser
    // event loop) so they don't block commands queued after the eager load.
    if (lazyDependencies.size > 0) {
      runWhenEagerDependenciesLoaded(() => {
        setTimeout(() => {
          lazyDependencies.forEach((loader, url) => this.loadLazyDependency(url, loader));
        }, 0);
      });
    }
  }

  private extractLazyDependenciesAndLoadOthers(loadMode: LoadMode, dependencies: Dependency[]): Map<string, Loader> {
    const lazyDependencies = new Map<string, Loader>();
    for (const dependency of dependencies) {
      const type = dependency[KEY_TYPE] as DependencyType;
      const dependencyId = KEY_ID in dependency ? (dependency[KEY_ID] as string) : null;
      const resourceLoader = this.getResourceLoader(type, loadMode, dependencyId);

      if (type === 'DYNAMIC_IMPORT') {
        this.loadDependencyEagerly(dependency[KEY_URL] as string, resourceLoader);
      } else {
        switch (loadMode) {
          case 'EAGER':
            this.loadDependencyEagerly(this.getDependencyUrl(dependency), resourceLoader);
            break;
          case 'LAZY':
            lazyDependencies.set(this.getDependencyUrl(dependency), resourceLoader);
            break;
          case 'INLINE':
            this.loadDependencyEagerly(dependency[KEY_CONTENTS] as string, resourceLoader);
            break;
          default:
            throw new Error(`Unknown load mode = ${loadMode}`);
        }
      }
    }
    return lazyDependencies;
  }

  private getDependencyUrl(dependency: Dependency): string {
    return this.registry.getURIResolver().resolveVaadinUri(dependency[KEY_URL] as string) ?? '';
  }

  private getResourceLoader(resourceType: DependencyType, loadMode: LoadMode, dependencyId: string | null): Loader {
    const resourceLoader = this.registry.getResourceLoader();
    const inline = loadMode === 'INLINE';

    switch (resourceType) {
      case 'STYLESHEET':
        if (inline) {
          return (data, listener) => resourceLoader.inlineStyleSheet(data, listener, dependencyId);
        }
        return (url, listener) => resourceLoader.loadStylesheet(url, listener, dependencyId);
      case 'JAVASCRIPT':
        if (inline) {
          return (data, listener) => resourceLoader.inlineScript(data, listener);
        }
        return (scriptUrl, listener) => resourceLoader.loadScript(scriptUrl, listener, false, true);
      case 'JS_MODULE':
        if (inline) {
          throw new Error('Inline load mode is not supported for JsModule.');
        }
        return (scriptUrl, listener) => resourceLoader.loadJsModule(scriptUrl, listener, false, true);
      case 'DYNAMIC_IMPORT':
        return (expression, listener) => resourceLoader.loadDynamicImport(expression, listener);
      default:
        throw new Error(`Unknown dependency type ${resourceType}`);
    }
  }

  /** Prevents eager dependencies from being considered loaded until HTML imports are ready. */
  requireHtmlImportsReady(): void {
    startEagerDependencyLoading();
    this.registry.getResourceLoader().runWhenHtmlImportsReady(() => endEagerDependencyLoading());
  }
}
