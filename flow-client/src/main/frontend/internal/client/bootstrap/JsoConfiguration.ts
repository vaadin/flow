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

import { AtmospherePushConnection } from '../communication/AtmospherePushConnection';

interface ErrorMessageLike {
  caption?: string | null;
  message?: string | null;
  url?: string | null;
  querySelector?: string | null;
}

interface VersionInfoLike {
  vaadinVersion?: string;
  atmosphereVersion?: string;
}

/**
 * Lightweight reader over the bootstrap configuration object the server emits
 * in the HTML page (or in the AppInitResponse). Migrated from
 * `com.vaadin.client.bootstrap.JsoConfiguration`.
 *
 * The wrapped object exposes a `getConfig(name)` accessor (defined in
 * `FlowBootstrap.js`); the methods below normalize each return value into a
 * typed shape the bootstrapper can feed straight into ApplicationConfiguration.
 */
export class JsoConfiguration {
  private readonly raw: { getConfig(name: string): unknown };

  constructor(raw: unknown) {
    this.raw = raw as { getConfig(name: string): unknown };
  }

  /** Reads a configuration parameter as a string, or null when undefined. */
  getConfigString(name: string): string | null {
    const value = this.raw.getConfig(name);
    if (value == null) {
      return null;
    }
    return String(value);
  }

  /** Reads a configuration parameter as a string array. */
  getConfigStringArray(name: string): string[] | null {
    const value = this.raw.getConfig(name);
    if (value == null) {
      return null;
    }
    return value as string[];
  }

  /** Reads a configuration parameter as a boolean (false when undefined). */
  getConfigBoolean(name: string): boolean {
    const value = this.raw.getConfig(name);
    if (value == null) {
      return false;
    }
    return Boolean(value);
  }

  /** Reads a configuration parameter as an integer, or null when undefined. */
  getConfigInteger(name: string): number | null {
    const value = this.raw.getConfig(name);
    if (value == null) {
      return null;
    }
    return Math.trunc(Number(value));
  }

  /** Reads a configuration parameter as an error-message struct. */
  getConfigError(name: string): ErrorMessageLike | null {
    const value = this.raw.getConfig(name);
    if (value == null) {
      return null;
    }
    return value as ErrorMessageLike;
  }

  /** Gets the Vaadin framework version reported by the server. */
  getVaadinVersion(): string | null {
    const info = this.raw.getConfig('versionInfo') as VersionInfoLike | null | undefined;
    return info?.vaadinVersion ?? null;
  }

  /** Gets the Atmosphere framework version reported by the server. */
  getAtmosphereVersion(): string | null {
    const info = this.raw.getConfig('versionInfo') as VersionInfoLike | null | undefined;
    return info?.atmosphereVersion ?? null;
  }

  /** Gets the Atmosphere JS-side version, if atmosphere has been loaded. */
  getAtmosphereJSVersion(): string | null {
    if (AtmospherePushConnection.isAtmosphereLoaded()) {
      return AtmospherePushConnection.getAtmosphereJSVersion();
    }
    return null;
  }

  /** Gets the initial UIDL message from the bootstrap page, if present. */
  getUIDL(): unknown {
    return this.raw.getConfig('uidl');
  }
}
