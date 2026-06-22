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

// Browser-environment probes migrated from BrowserInfo.java, registered on
// window.Vaadin.Flow.internal.BrowserInfo by registerInternals; the Java methods
// delegate here. The user-agent parsing itself stays in the shared
// BrowserDetails Java class. Also bundled to ES5 for the HtmlUnit used by
// GwtTests.

/** Returns the browser's user-agent string. */
export function getBrowserString(): string {
  return window.navigator.userAgent;
}

/** Detects whether the browser runs on a touch-capable device. */
export function checkForTouchDevice(): boolean {
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
export function isIos(): boolean {
  return (
    /iPad|iPhone|iPod/.test(navigator.platform) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
  );
}
