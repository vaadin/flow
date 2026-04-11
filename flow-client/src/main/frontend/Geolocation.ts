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

/**
 * Coordinates data sent to the server, matching the Java GeolocationCoordinates record.
 */
interface VaadinGeolocationCoordinates {
  latitude: number;
  longitude: number;
  accuracy: number;
  altitude: number | null;
  altitudeAccuracy: number | null;
  heading: number | null;
  speed: number | null;
}

/**
 * Position data sent to the server, matching the Java GeolocationPosition record.
 */
interface VaadinGeolocationPosition {
  coords: VaadinGeolocationCoordinates;
  timestamp: number;
}

/**
 * Error data sent to the server, matching the Java GeolocationError record.
 */
interface VaadinGeolocationError {
  code: number;
  message: string;
}

/**
 * Result of a one-shot geolocation request. Contains either a position or an error.
 */
interface VaadinGeolocationGetResult {
  position?: VaadinGeolocationPosition;
  error?: VaadinGeolocationError;
}

/**
 * Options for geolocation requests, matching the standard PositionOptions.
 */
interface VaadinGeolocationOptions {
  enableHighAccuracy?: boolean;
  timeout?: number;
  maximumAge?: number;
}

function copyCoords(c: GeolocationCoordinates): VaadinGeolocationCoordinates {
  return {
    latitude: c.latitude,
    longitude: c.longitude,
    accuracy: c.accuracy,
    altitude: c.altitude,
    altitudeAccuracy: c.altitudeAccuracy,
    heading: c.heading,
    speed: c.speed
  };
}

const watches = new Map<string, number>();

(window as any).Vaadin = (window as any).Vaadin || {};
(window as any).Vaadin.Flow = (window as any).Vaadin.Flow || {};
(window as any).Vaadin.Flow.geolocation = {
  get(options?: VaadinGeolocationOptions): Promise<VaadinGeolocationGetResult> {
    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (p) => {
          resolve({
            position: {
              coords: copyCoords(p.coords),
              timestamp: p.timestamp
            }
          });
        },
        (e) => {
          resolve({
            error: { code: e.code, message: e.message }
          });
        },
        options || undefined
      );
    });
  },

  watch(element: HTMLElement, options: VaadinGeolocationOptions | undefined, watchKey: string): void {
    if (watches.has(watchKey)) {
      navigator.geolocation.clearWatch(watches.get(watchKey)!);
    }
    watches.set(
      watchKey,
      navigator.geolocation.watchPosition(
        (p) => {
          element.dispatchEvent(
            new CustomEvent('vaadin-geolocation-position', {
              detail: {
                coords: copyCoords(p.coords),
                timestamp: p.timestamp
              }
            })
          );
        },
        (e) => {
          element.dispatchEvent(
            new CustomEvent('vaadin-geolocation-error', {
              detail: { code: e.code, message: e.message }
            })
          );
        },
        options || undefined
      )
    );
  },

  clearWatch(watchKey: string): void {
    if (watches.has(watchKey)) {
      navigator.geolocation.clearWatch(watches.get(watchKey)!);
      watches.delete(watchKey);
    }
  },

  isSupported(): boolean {
    if (!window.isSecureContext) {
      return false;
    }
    // Chromium exposes document.featurePolicy; Firefox and Safari do not
    // expose any feature-policy introspection API, so the check is only
    // possible on Chromium. When absent, assume geolocation is allowed.
    const doc = document as any;
    if (doc.featurePolicy && typeof doc.featurePolicy.allowsFeature === 'function') {
      try {
        if (!doc.featurePolicy.allowsFeature('geolocation')) {
          return false;
        }
      } catch (_e) {
        // Ignore and assume allowed
      }
    }
    return true;
  },

  async queryPermission(): Promise<'GRANTED' | 'DENIED' | 'PROMPT' | 'UNKNOWN'> {
    try {
      const status = await navigator.permissions.query({ name: 'geolocation' as PermissionName });
      switch (status.state) {
        case 'granted':
          return 'GRANTED';
        case 'denied':
          return 'DENIED';
        case 'prompt':
          return 'PROMPT';
        default:
          return 'UNKNOWN';
      }
    } catch (_e) {
      // Safari rejects the 'geolocation' permission name with a TypeError
      return 'UNKNOWN';
    }
  }
};

// Empty export to ensure TypeScript emits this as an ES module,
// which is required for Vite to load it via import.
export {};
