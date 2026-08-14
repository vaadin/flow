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

// Browser-environment probes migrated from BrowserInfo.java. The user-agent
// parsing itself lives in the shared BrowserDetails class. Functions that were
// private in BrowserInfo.java (getBrowserString, checkForTouchDevice, isIos)
// stay module-local here; only the members that were public in Java are
// exported.

/** Returns the browser's user-agent string. */
function getBrowserString(): string {
  return window.navigator.userAgent;
}

/** Detects whether the browser runs on a touch-capable device. */
function checkForTouchDevice(): boolean {
  const nav = navigator as unknown as { maxTouchPoints?: number; msMaxTouchPoints?: number };
  if ('maxTouchPoints' in nav) {
    return (nav.maxTouchPoints ?? 0) > 0;
  } else if ('msMaxTouchPoints' in nav) {
    return (nav.msMaxTouchPoints ?? 0) > 0;
  }
  const mediaQuery = window.matchMedia && window.matchMedia('(pointer:coarse)');
  if (mediaQuery && mediaQuery.media === '(pointer:coarse)') {
    return !!mediaQuery.matches;
  }
  try {
    document.createEvent('TouchEvent');
    return true;
  } catch {
    return false;
  }
}

/** Detects whether the browser runs on iOS (including iPadOS desktop mode). */
function isIos(): boolean {
  return (
    /iPad|iPhone|iPod/.test(navigator.platform) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
  );
}

/** Checks if the browser runs on a touch capable device. */
export function isTouchDevice(): boolean {
  return checkForTouchDevice();
}

// User-agent-based browser-family probes. The canonical parsing lives in the
// shared BrowserDetails (Java); these approximate the predicates the client
// needs for browser-specific workarounds (e.g. ResourceLoader stylesheet load
// detection on Safari/Opera).

/**
 * Whether the browser is Safari (and not a Chromium-family browser).
 *
 * @deprecated use a parsing library like ua-parser-js to parse the user agent
 */
export function isSafari(): boolean {
  const ua = getBrowserString();
  return /safari/i.test(ua) && !/chrome|chromium|crios|android/i.test(ua);
}

/**
 * Whether the browser is Safari or running on iOS.
 *
 * @deprecated use a parsing library like ua-parser-js to parse the user agent
 */
export function isSafariOrIOS(): boolean {
  // Mirrors BrowserInfo.isSafariOrIOS in Java, which delegates to the
  // (also deprecated) isSafari check.
  // eslint-disable-next-line @typescript-eslint/no-deprecated
  return isSafari() || isIos();
}

/**
 * Whether the browser is Opera (Presto or Chromium-based OPR).
 *
 * @deprecated use a parsing library like ua-parser-js to parse the user agent
 */
export function isOpera(): boolean {
  return /opr\/|opera/i.test(getBrowserString());
}

/**
 * Whether the browser is WebKit-based (excludes legacy Edge).
 *
 * @deprecated use a parsing library like ua-parser-js to parse the user agent
 */
export function isWebkit(): boolean {
  const ua = getBrowserString();
  return /applewebkit/i.test(ua) && !/edge\//i.test(ua);
}
