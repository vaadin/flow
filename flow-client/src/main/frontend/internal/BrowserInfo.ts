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
// parsing itself lives in the shared BrowserDetails class.

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

// User-agent-based browser-family probes. The canonical parsing lives in the
// shared BrowserDetails (Java); these approximate the predicates the client
// needs for browser-specific workarounds (e.g. ResourceLoader stylesheet load
// detection on Safari/Opera).

/** Whether the browser is Safari (and not a Chromium-family browser). */
export function isSafari(): boolean {
  const ua = getBrowserString();
  return /safari/i.test(ua) && !/chrome|chromium|crios|android/i.test(ua);
}

/** Whether the browser is Safari or running on iOS. */
export function isSafariOrIOS(): boolean {
  return isSafari() || isIos();
}

/** Whether the browser is Opera (Presto or Chromium-based OPR). */
export function isOpera(): boolean {
  return /opr\/|opera/i.test(getBrowserString());
}

// The following predicates mirror BrowserDetails.parseBrowserName(): a single
// browser name is selected by a fixed priority order (Edge before Chrome,
// Chrome excluding Opera, IE via MSIE/Trident), so the predicates are made
// mutually exclusive to match the Java accessors isEdge()/isChrome()/isIE().

/** Whether the browser is (Chromium- or legacy-) Edge. */
export function isEdge(): boolean {
  const ua = getBrowserString().toLowerCase();
  return ua.includes(' edge/') || ua.includes(' edg/') || ua.includes(' edga/') || ua.includes(' edgios/');
}

/** Whether the browser is Chrome (excludes Edge and Opera). */
export function isChrome(): boolean {
  if (isEdge()) {
    return false;
  }
  const ua = getBrowserString().toLowerCase();
  return (
    (ua.includes(' chrome/') || ua.includes(' crios/') || ua.includes(' headlesschrome/')) && !ua.includes(' opr/')
  );
}

/** Whether the browser is Internet Explorer (MSIE, or Trident-based IE 11). */
export function isIE(): boolean {
  if (isEdge() || isChrome() || isOpera()) {
    return false;
  }
  const ua = getBrowserString().toLowerCase();
  return (ua.includes('msie') && !ua.includes('webtv')) || ua.includes('trident/');
}

/** Whether the browser is WebKit-based (excludes legacy Edge). */
export function isWebkit(): boolean {
  const ua = getBrowserString();
  return /applewebkit/i.test(ua) && !/edge\//i.test(ua);
}
