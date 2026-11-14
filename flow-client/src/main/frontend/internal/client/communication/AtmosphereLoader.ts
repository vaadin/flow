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

// Loads vaadinPush.js, the ES module build of atmosphere.js that flow-push
// serves. Kept apart from AtmospherePushConnection so that Flow.ts can start
// the load during bootstrap without pulling the connection into its chunk, and
// both end up with the same module.

let atmosphereModule: Promise<unknown> | null = null;

let loadedModule: unknown = null;

/**
 * Loads the atmosphere.js module from the given url, unless a load has already
 * been started, in which case that one is returned. A failed load is forgotten
 * so that a later call tries again.
 *
 * @param url - the url of vaadinPush.js, relative to the document base url
 * @returns the module
 */
export async function loadAtmosphere(url: string): Promise<unknown> {
  if (atmosphereModule === null) {
    // A relative specifier would be resolved against this module, not the page
    const moduleUrl = new URL(url, document.baseURI).href;
    setAtmosphereModule(import(/* @vite-ignore */ moduleUrl));
  }
  return atmosphereModule!;
}

/**
 * Uses the given module as the atmosphere.js module.
 *
 * @param module - the module, or null to load it again on the next use
 */
export function setAtmosphereModule(module: Promise<unknown> | null): void {
  atmosphereModule = module;
  loadedModule = null;
  module?.then(
    (loaded) => {
      if (atmosphereModule === module) {
        loadedModule = loaded;
      }
    },
    () => {
      if (atmosphereModule === module) {
        atmosphereModule = null;
      }
    }
  );
}

/**
 * Gets the atmosphere.js module if it has been loaded.
 *
 * @returns the module, or null if it has not been loaded (yet)
 */
export function getLoadedAtmosphere(): unknown {
  return loadedModule;
}
