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

// Bootstrap configuration readers migrated from JsoConfiguration.java. They read
// from the configuration object (the value returned by
// $wnd.Vaadin.Flow.getApp(appId), which exposes a getConfig(name) accessor).

// The bootstrap configuration object exposes a getConfig(name) accessor over the
// values the server injected into the page.
interface ConfigObject {
  getConfig(name: string): unknown;
}

/** Reads a configuration parameter as a string, or null if not defined. */
export function getConfigString(config: ConfigObject, name: string): string | null {
  const value = config.getConfig(name);
  return value === null || value === undefined ? null : `${value as string}`;
}

/** Reads a configuration parameter as a ValueMap-like object. */
export function getConfigValueMap(config: ConfigObject, name: string): unknown {
  return config.getConfig(name);
}

/** Reads a configuration parameter as a string array. */
export function getConfigStringArray(config: ConfigObject, name: string): unknown {
  return config.getConfig(name);
}

/** Reads a configuration parameter as a boolean, or false if not defined. */
export function getConfigBoolean(config: ConfigObject, name: string): boolean {
  const value = config.getConfig(name);
  return value === null || value === undefined ? false : Boolean(value);
}

/** Reads a configuration parameter as an error-message object. */
export function getConfigError(config: ConfigObject, name: string): unknown {
  return config.getConfig(name);
}

/** The Vaadin framework version reported by the server. */
export function getVaadinVersion(config: ConfigObject): string | null {
  const info = config.getConfig('versionInfo') as { vaadinVersion?: string } | null;
  return info?.vaadinVersion ?? null;
}

/** The Atmosphere framework version reported by the server. */
export function getAtmosphereVersion(config: ConfigObject): string | null {
  const info = config.getConfig('versionInfo') as { atmosphereVersion?: string } | null;
  return info?.atmosphereVersion ?? null;
}
