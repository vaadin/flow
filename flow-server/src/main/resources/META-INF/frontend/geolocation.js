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

// Safari may report timestamps relative to the Apple epoch
// (2001-01-01) instead of Unix epoch (1970-01-01).
// The offset between the two is 978307200000 ms.
const APPLE_TO_UNIX_EPOCH_MS = 978307200000;
const MAX_REASONABLE_AGE_MS = 86400000000;

function fixTimestamp(ts) {
  if (Date.now() - ts > MAX_REASONABLE_AGE_MS) {
    return ts + APPLE_TO_UNIX_EPOCH_MS;
  }
  return ts;
}

function copyCoords(c) {
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

const watches = {};

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.geolocation = {
  get: function (options) {
    return new Promise(function (resolve) {
      navigator.geolocation.getCurrentPosition(
        function (p) {
          resolve({
            position: {
              coords: copyCoords(p.coords),
              timestamp: fixTimestamp(p.timestamp)
            }
          });
        },
        function (e) {
          resolve({
            error: { code: e.code, message: e.message }
          });
        },
        options || undefined
      );
    });
  },

  watch: function (element, options, watchKey) {
    if (watches[watchKey] != null) {
      navigator.geolocation.clearWatch(watches[watchKey]);
    }
    watches[watchKey] = navigator.geolocation.watchPosition(
      function (p) {
        element.dispatchEvent(
          new CustomEvent('vaadin-geolocation-position', {
            detail: {
              coords: copyCoords(p.coords),
              timestamp: fixTimestamp(p.timestamp)
            }
          })
        );
      },
      function (e) {
        element.dispatchEvent(
          new CustomEvent('vaadin-geolocation-error', {
            detail: { code: e.code, message: e.message }
          })
        );
      },
      options || undefined
    );
  },

  clearWatch: function (watchKey) {
    if (watches[watchKey] != null) {
      navigator.geolocation.clearWatch(watches[watchKey]);
      delete watches[watchKey];
    }
  }
};
