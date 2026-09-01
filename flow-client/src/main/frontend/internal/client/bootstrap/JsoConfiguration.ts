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
//
// getAtmosphereJSVersion reads the Atmosphere version off the window rather than
// through AtmospherePushConnection: Java reaches that class's private static
// isAtmosphereLoaded through JSNI, which ignores visibility, and TypeScript has
// no equivalent.

import type { ValueMap } from '../ValueMap';

// The bootstrap configuration object exposes a getConfig(name) accessor over the
// values the server injected into the page. This is the JavaScript object the
// Java JSO overlays, so a caller passes it where Java calls a method on the JSO.
export interface ConfigObject {
  getConfig(name: string): unknown;
}

/**
 * Reads a configuration parameter as a string. Please note that the
 * javascript value of the parameter should also be a string, or else an
 * undefined exception may be thrown.
 *
 * @param name - name of the configuration parameter
 * @returns value of the configuration parameter, or `null` if not defined
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
 * @returns value of the configuration parameter, or `null`if not defined
 */
export function getConfigValueMap(config: ConfigObject, name: string): ValueMap | null {
  return (config.getConfig(name) as ValueMap | undefined) ?? null;
}

/**
 * Reads a configuration parameter as a String array.
 *
 * @param name - name of the configuration parameter
 * @returns value of the configuration parameter, or `null`if not defined
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
 * Reads a configuration parameter as an integer object. Please note that
 * the javascript value of the parameter should also be an integer, or else
 * an undefined exception may be thrown.
 *
 * @param config - the bootstrap configuration object
 * @param name - name of the configuration parameter
 * @returns integer value of the configuration parameter, or `null` if no value
 *          is defined
 */
export function getConfigInteger(config: ConfigObject, name: string): number | null {
  const value = config.getConfig(name);
  if (value === null || value === undefined) {
    return null;
  }
  // Java hands the value to Integer.valueOf(int), which truncates it.
  return Math.trunc(Number(value));
}

/**
 * Reads a configuration parameter as an `ErrorMessage` object. Please
 * note that the javascript value of the parameter should also be an object
 * with appropriate fields, or else an undefined exception may be thrown
 * when calling this method or when calling methods on the returned object.
 *
 * @param name - name of the configuration parameter
 * @returns error message with the given name, or `null` if no value is defined
 */
export function getConfigError(config: ConfigObject, name: string): unknown {
  return config.getConfig(name) ?? null;
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

/**
 * Gets the JS version used in the Atmosphere framework.
 *
 * @returns a string with the version
 */
export function getAtmosphereJSVersion(): string | null {
  const atmosphere = (window as unknown as { vaadinPush?: { atmosphere?: { version?: string } } }).vaadinPush
    ?.atmosphere;
  return atmosphere?.version ?? null;
}

/**
 * Gets the initial UIDL from the bootstrap page.
 *
 * @param config - the bootstrap configuration object
 * @returns the initial UIDL
 */
export function getUIDL(config: ConfigObject): ValueMap | null {
  return getConfigValueMap(config, 'uidl');
}
