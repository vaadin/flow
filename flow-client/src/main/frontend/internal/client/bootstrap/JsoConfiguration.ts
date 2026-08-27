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

/**
 * Reads a configuration parameter as a string. Please note that the
 * javascript value of the parameter should also be a string, or else an
 * undefined exception may be thrown.
 *
 * @param name - name of the configuration parameter
 * @returns value of the configuration parameter, or <code>null</code> if not defined
 */
export function getConfigString(config: ConfigObject, name: string): string | null {
  const value = config.getConfig(name);
  return value === null || value === undefined ? null : `${value as string}`;
}

/**
 * Reads a configuration parameter as a {@link ValueMap}. Please note that
 * the javascript value of the parameter should also be a javascript object,
 * or else an undefined exception may be thrown.
 *
 * @param name - name of the configuration parameter
 * @returns value of the configuration parameter, or <code>null</code>if not defined
 */
export function getConfigValueMap(config: ConfigObject, name: string): unknown {
  return config.getConfig(name);
}

/**
 * Reads a configuration parameter as a String array.
 *
 * @param name - name of the configuration parameter
 * @returns value of the configuration parameter, or <code>null</code>if not defined
 */
export function getConfigStringArray(config: ConfigObject, name: string): unknown {
  return config.getConfig(name);
}

/**
 * Reads a configuration parameter as a boolean.
 *
 * Please note that the javascript value of the parameter should also be a
 * boolean, or else an undefined exception may be thrown.
 *
 * @param name - name of the configuration parameter
 * @returns the boolean value of the configuration parameter, or <code>false</code> if no value is defined
 */
export function getConfigBoolean(config: ConfigObject, name: string): boolean {
  const value = config.getConfig(name);
  return value === null || value === undefined ? false : Boolean(value);
}

/**
 * Reads a configuration parameter as an {@link ErrorMessage} object. Please
 * note that the javascript value of the parameter should also be an object
 * with appropriate fields, or else an undefined exception may be thrown
 * when calling this method or when calling methods on the returned object.
 *
 * @param name - name of the configuration parameter
 * @returns error message with the given name, or <code>null</code> if no value is defined
 */
export function getConfigError(config: ConfigObject, name: string): unknown {
  return config.getConfig(name);
}

/**
 * Gets the version of the Vaadin framework used on the server.
 *
 * @returns a string with the version
 */
export function getVaadinVersion(config: ConfigObject): string | null {
  const info = config.getConfig('versionInfo') as { vaadinVersion?: string } | null;
  return info?.vaadinVersion ?? null;
}

/**
 * Gets the version of the Atmosphere framework.
 *
 * @returns a string with the version
 */
export function getAtmosphereVersion(config: ConfigObject): string | null {
  const info = config.getConfig('versionInfo') as { atmosphereVersion?: string } | null;
  return info?.atmosphereVersion ?? null;
}
