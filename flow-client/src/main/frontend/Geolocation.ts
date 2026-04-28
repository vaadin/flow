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

type VaadinGeolocationAvailability = 'GRANTED' | 'DENIED' | 'PROMPT' | 'UNKNOWN' | 'UNSUPPORTED';

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
 * Result of a one-shot geolocation request. Contains either a position or an error,
 * plus the availability state updated by this response.
 */
interface VaadinGeolocationGetResult {
  position?: VaadinGeolocationPosition;
  error?: VaadinGeolocationError;
  availability: VaadinGeolocationAvailability;
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

// The cached availability for the current page. Populated on first
// queryAvailability() call, refreshed from each get()/watch() outcome, and
// kept current by a permissionchange listener (where supported).
let cachedAvailability: VaadinGeolocationAvailability | null = null;
let permissionChangeListenerInstalled = false;

function publishAvailability(next: VaadinGeolocationAvailability): void {
  if (cachedAvailability === next) {
    return;
  }
  cachedAvailability = next;
  // Dispatch on document.body so the server-side Geolocation facade (listening
  // on the UI element, which is body) can update its cached value.
  document.body.dispatchEvent(
    new CustomEvent('vaadin-geolocation-availability-change', {
      detail: { availability: next }
    })
  );
}

// Applies a single get()/watch() outcome to the cached availability and
// returns the value to report in the response. Never overwrites
// UNSUPPORTED, which is session-stable. TIMEOUT and POSITION_UNAVAILABLE
// don't reveal the permission state, so the previous cached value is
// returned unchanged.
function getAndCacheAvailabilityFromResult(
  position: VaadinGeolocationPosition | undefined,
  error: VaadinGeolocationError | undefined
): VaadinGeolocationAvailability {
  if (cachedAvailability !== 'UNSUPPORTED') {
    if (position) {
      publishAvailability('GRANTED');
    } else if (error?.code === 1) {
      publishAvailability('DENIED');
    }
  }
  return cachedAvailability ?? 'UNKNOWN';
}

async function resolveAvailability(): Promise<VaadinGeolocationAvailability> {
  if (!window.isSecureContext) {
    return 'UNSUPPORTED';
  }
  // Chromium exposes document.featurePolicy; Firefox and Safari do not
  // expose any feature-policy introspection API, so the check is only
  // possible on Chromium. When absent, assume geolocation is allowed.
  const doc = document as any;
  if (doc.featurePolicy && typeof doc.featurePolicy.allowsFeature === 'function') {
    try {
      if (!doc.featurePolicy.allowsFeature('geolocation')) {
        return 'UNSUPPORTED';
      }
    } catch (_e) {
      // Ignore and assume allowed
    }
  }
  try {
    const status = await navigator.permissions.query({ name: 'geolocation' as PermissionName });
    if (!permissionChangeListenerInstalled) {
      permissionChangeListenerInstalled = true;
      status.addEventListener('change', () => {
        publishAvailability(stateToAvailability(status.state));
      });
    }
    return stateToAvailability(status.state);
  } catch (_e) {
    // Safari rejects the 'geolocation' permission name with a TypeError
    return 'UNKNOWN';
  }
}

function stateToAvailability(state: string): VaadinGeolocationAvailability {
  switch (state) {
    case 'granted':
      return 'GRANTED';
    case 'denied':
      return 'DENIED';
    case 'prompt':
      return 'PROMPT';
    default:
      return 'UNKNOWN';
  }
}

const $wnd = window as any;
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
$wnd.Vaadin.Flow.geolocation = {
  get(options?: VaadinGeolocationOptions): Promise<VaadinGeolocationGetResult> {
    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (p) => {
          const position = { coords: copyCoords(p.coords), timestamp: p.timestamp };
          resolve({ position, availability: getAndCacheAvailabilityFromResult(position, undefined) });
        },
        (e) => {
          const error = { code: e.code, message: e.message };
          resolve({ error, availability: getAndCacheAvailabilityFromResult(undefined, error) });
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
          const position = { coords: copyCoords(p.coords), timestamp: p.timestamp };
          getAndCacheAvailabilityFromResult(position, undefined);
          element.dispatchEvent(
            new CustomEvent('vaadin-geolocation-position', {
              detail: position
            })
          );
        },
        (e) => {
          const error = { code: e.code, message: e.message };
          getAndCacheAvailabilityFromResult(undefined, error);
          element.dispatchEvent(
            new CustomEvent('vaadin-geolocation-error', {
              detail: error
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

  async queryAvailability(): Promise<VaadinGeolocationAvailability> {
    const value = await resolveAvailability();
    // publish without dispatching a change event — there is no previous
    // cached value to compare against when cachedAvailability is null and
    // the bootstrap consumer just wants the initial answer.
    cachedAvailability = value;
    return value;
  }
};

// Empty export to ensure TypeScript emits this as an ES module,
// which is required for Vite to load it via import.
export {};
