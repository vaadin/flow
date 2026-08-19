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

// Browser-environment probes migrated from BrowserInfo.java. Browser details are
// detected only once and stored in this singleton (accessed via BrowserInfo.get),
// mirroring the Java class. The actual user-agent parsing lives in BrowserDetails;
// this class only adds the touch-device probe and the iOS/Safari-or-iOS helpers.
// Functions that were private in BrowserInfo.java (getBrowserString,
// checkForTouchDevice, isIos) stay module-local here; only the members that were
// public in Java are part of the class surface, with @deprecated marks preserved.

import { BrowserDetails } from './BrowserDetails';

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

/**
 * Provides a way to query information about web browser.
 *
 * Browser details are detected only once and those are stored in this singleton
 * class.
 *
 * @since 1.0
 */
export class BrowserInfo {
  static readonly ENGINE_GECKO = 'gecko';

  static readonly ENGINE_WEBKIT = 'webkit';

  static readonly ENGINE_PRESTO = 'presto';

  static readonly ENGINE_TRIDENT = 'trident';

  static #instance?: BrowserInfo;

  readonly #browserDetails: BrowserDetails;

  readonly #touchDevice: boolean;

  private constructor() {
    this.#browserDetails = new BrowserDetails(getBrowserString());
    this.#touchDevice = checkForTouchDevice();
  }

  /**
   * Singleton method to get BrowserInfo object.
   *
   * @return instance of BrowserInfo object
   */
  static get(): BrowserInfo {
    if (BrowserInfo.#instance === undefined) {
      BrowserInfo.#instance = new BrowserInfo();
    }
    return BrowserInfo.#instance;
  }

  /**
   * Checks if the browser is IE.
   *
   * @return true if the browser is IE, false otherwise
   * @deprecated use a parsing library like ua-parser-js to parse the user agent
   */
  isIE(): boolean {
    return this.#browserDetails.isIE();
  }

  /**
   * Checks if the browser is Edge.
   *
   * @return true if the browser is Edge, false otherwise
   * @deprecated use a parsing library like ua-parser-js to parse the user agent
   */
  isEdge(): boolean {
    return this.#browserDetails.isEdge();
  }

  /**
   * Checks if the browser is Firefox.
   *
   * @return true if the browser is Firefox, false otherwise
   * @deprecated use a parsing library like ua-parser-js to parse the user agent
   */
  isFirefox(): boolean {
    return this.#browserDetails.isFirefox();
  }

  /**
   * Checks if the browser is Safari.
   *
   * @return true if the browser is Safari, false otherwise
   * @deprecated use a parsing library like ua-parser-js to parse the user agent
   */
  isSafari(): boolean {
    return this.#browserDetails.isSafari();
  }

  /**
   * Checks if the browser is Safari or runs on iOS (covering also Chrome on
   * iOS).
   *
   * @return true if the browser is Safari or running on iOS, false otherwise
   * @deprecated use a parsing library like ua-parser-js to parse the user agent
   */
  isSafariOrIOS(): boolean {
    return this.#browserDetails.isSafari() || isIos();
  }

  /**
   * Checks if the browser is Chrome.
   *
   * @return true if the browser is Chrome, false otherwise
   * @deprecated use a parsing library like ua-parser-js to parse the user agent
   */
  isChrome(): boolean {
    return this.#browserDetails.isChrome();
  }

  /**
   * Checks if the browser using the Gecko engine.
   *
   * @return true if the browser is using Gecko, false otherwise
   * @deprecated use a parsing library like ua-parser-js to parse the user agent
   */
  isGecko(): boolean {
    return this.#browserDetails.isGecko();
  }

  /**
   * Checks if the browser using the Webkit engine.
   *
   * @return true if the browser is using Webkit, false otherwise
   * @deprecated use a parsing library like ua-parser-js to parse the user agent
   */
  isWebkit(): boolean {
    return this.#browserDetails.isWebKit();
  }

  /**
   * Returns the Gecko version if the browser is Gecko based. The Gecko version
   * for Firefox 2 is 1.8 and 1.9 for Firefox 3.
   *
   * @return The Gecko version or -1 if the browser is not Gecko based
   * @deprecated use a parsing library like ua-parser-js to parse the user agent
   */
  getGeckoVersion(): number {
    if (!this.#browserDetails.isGecko()) {
      return -1;
    }
    return this.#browserDetails.getBrowserEngineVersion();
  }

  /**
   * Returns the WebKit version if the browser is WebKit based. The WebKit
   * version returned is the major version e.g., 523.
   *
   * @return The WebKit version or -1 if the browser is not WebKit based
   * @deprecated use a parsing library like ua-parser-js to parse the user agent
   */
  getWebkitVersion(): number {
    if (!this.#browserDetails.isWebKit()) {
      return -1;
    }
    return this.#browserDetails.getBrowserEngineVersion();
  }

  /**
   * Checks if the browser is Opera.
   *
   * @return true if the browser is Opera, false otherwise
   * @deprecated use a parsing library like ua-parser-js to parse the user agent
   */
  isOpera(): boolean {
    return this.#browserDetails.isOpera();
  }

  /**
   * Checks if the browser runs on a touch capable device.
   *
   * @return true if the browser runs on a touch based device, false otherwise
   */
  isTouchDevice(): boolean {
    return this.#touchDevice;
  }

  /**
   * Checks if the browser is run on Android.
   *
   * @return true if the browser is run on Android, false otherwise
   * @deprecated use a parsing library like ua-parser-js to parse the user agent
   */
  isAndroid(): boolean {
    return this.#browserDetails.isAndroid();
  }

  /**
   * Tests if this is an Android devices with a broken scrollTop
   * implementation.
   *
   * @return true if scrollTop cannot be trusted on this device, false otherwise
   * @deprecated use a parsing library like ua-parser-js to parse the user agent
   *             and check version against known issues.
   */
  isAndroidWithBrokenScrollTop(): boolean {
    return (
      this.#browserDetails.isAndroid() &&
      (this.#getOperatingSystemMajorVersion() === 3 || this.#getOperatingSystemMajorVersion() === 4)
    );
  }

  #getOperatingSystemMajorVersion(): number {
    return this.#browserDetails.getOperatingSystemMajorVersion();
  }

  /**
   * Returns the browser major version e.g., 3 for Firefox 3.5, 4 for Chrome
   * 4, 8 for Internet Explorer 8.
   *
   * @return The major version of the browser.
   * @deprecated use a parsing library like ua-parser-js to parse the user agent
   */
  getBrowserMajorVersion(): number {
    return this.#browserDetails.getBrowserMajorVersion();
  }

  /**
   * Returns the browser minor version e.g., 5 for Firefox 3.5.
   *
   * @return The minor version of the browser, or -1 if not known/parsed.
   * @deprecated use a parsing library like ua-parser-js to parse the user agent
   */
  getBrowserMinorVersion(): number {
    return this.#browserDetails.getBrowserMinorVersion();
  }
}
