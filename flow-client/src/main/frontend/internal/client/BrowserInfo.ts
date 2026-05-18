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

interface MsTouchNavigator extends Navigator {
  msMaxTouchPoints?: number;
}

/**
 * Browser-detection helpers migrated from `com.vaadin.client.BrowserInfo`.
 * Reached from GWT code via the `NativeBrowserInfo` JsType shim. The
 * `BrowserDetails`-backed query methods (`isFirefox`, `isMacOSX`, etc.) stay
 * in `BrowserInfo.java` because they wrap the shared parsing library.
 */
export const BrowserInfo = {
  checkForTouchDevice(): boolean {
    const nav = navigator as MsTouchNavigator;
    if (nav && 'maxTouchPoints' in nav) {
      return (nav.maxTouchPoints ?? 0) > 0;
    }
    if (nav && 'msMaxTouchPoints' in nav) {
      return ((nav as MsTouchNavigator).msMaxTouchPoints ?? 0) > 0;
    }
    const mq = typeof matchMedia === 'function' ? matchMedia('(pointer:coarse)') : null;
    if (mq && mq.media === '(pointer:coarse)') {
      return mq.matches;
    }
    try {
      document.createEvent('TouchEvent');
      return true;
    } catch {
      return false;
    }
  },

  getBrowserString(): string {
    return navigator.userAgent;
  },

  isIos(): boolean {
    return (
      /iPad|iPhone|iPod/.test(navigator.platform) ||
      (navigator.platform === 'MacIntel' && (navigator.maxTouchPoints ?? 0) > 1)
    );
  }
};
