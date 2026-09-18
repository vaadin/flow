/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

const testUserAgent = (regexp) => regexp.test(navigator.userAgent);

const testPlatform = (regexp) => regexp.test(navigator.platform);

const testVendor = (regexp) => regexp.test(navigator.vendor);

export const isAndroid = testUserAgent(/Android/u);

export const isChrome = testUserAgent(/Chrome/u) && testVendor(/Google Inc/u);

export const isFirefox = testUserAgent(/Firefox/u);

// IPadOS 13 lies and says it's a Mac, but we can distinguish by detecting touch support.
export const isIPad = testPlatform(/^iPad/u) || (testPlatform(/^Mac/u) && navigator.maxTouchPoints > 1);

export const isIPhone = testPlatform(/^iPhone/u);

export const isIOS = isIPhone || isIPad;

export const isSafari = testUserAgent(/^((?!chrome|android).)*safari/iu);

export const isTouch = (() => {
  try {
    document.createEvent('TouchEvent');
    return true;
  } catch (_) {
    return false;
  }
})();

export const supportsAdoptingStyleSheets =
  window.ShadowRoot && 'adoptedStyleSheets' in Document.prototype && 'replace' in CSSStyleSheet.prototype;
