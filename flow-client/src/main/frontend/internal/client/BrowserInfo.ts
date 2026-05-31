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

interface ParsedUa {
  isFirefox: boolean;
  isChrome: boolean;
  isSafari: boolean;
  isOpera: boolean;
  isEdge: boolean;
  isIE: boolean;
  isGecko: boolean;
  isWebkit: boolean;
  isAndroid: boolean;
  browserMajor: number;
  browserMinor: number;
  engineVersion: number;
  osMajor: number;
}

function parseUa(ua: string): ParsedUa {
  const lower = ua.toLowerCase();

  const isOpera = lower.includes(' opr/') || lower.includes('opera');
  const isEdge = lower.includes(' edge/') || lower.includes(' edg/');
  const isIE = !isEdge && (lower.includes('msie') || lower.includes('trident/'));
  const isFirefox = !isOpera && !isEdge && lower.includes('firefox');
  const isAndroid = lower.includes('android');
  // Safari excludes Chrome, Edge, Opera, and Android browsers — they all carry
  // "Safari" in their UA.
  const isSafari =
    !isOpera &&
    !isEdge &&
    !isAndroid &&
    !isFirefox &&
    lower.includes('safari') &&
    !lower.includes('chrome') &&
    !lower.includes('crios');
  const isChrome =
    !isOpera && !isEdge && !isIE && (lower.includes('chrome') || lower.includes('crios') || lower.includes('chromium'));

  const isGecko = isFirefox;
  const isWebkit = isSafari || isChrome || isOpera || isEdge;

  const browserMajor = extractMajor(lower, isChrome, isFirefox, isSafari, isEdge, isOpera, isIE);
  const browserMinor = extractMinor(lower);
  const engineVersion = extractEngineVersion(lower, isWebkit, isGecko);
  const osMajor = extractAndroidMajor(lower);

  return {
    isFirefox,
    isChrome,
    isSafari,
    isOpera,
    isEdge,
    isIE,
    isGecko,
    isWebkit,
    isAndroid,
    browserMajor,
    browserMinor,
    engineVersion,
    osMajor
  };
}

// eslint-disable-next-line @typescript-eslint/max-params
function extractMajor(
  ua: string,
  isChrome: boolean,
  isFirefox: boolean,
  isSafari: boolean,
  isEdge: boolean,
  isOpera: boolean,
  isIE: boolean
): number {
  let m: RegExpMatchArray | null = null;
  if (isEdge) {
    m = /(?:edge|edg)\/(\d+)/.exec(ua);
  } else if (isOpera) {
    m = /(?:opr|opera)\/(\d+)/.exec(ua);
  } else if (isFirefox) {
    m = /firefox\/(\d+)/.exec(ua);
  } else if (isChrome) {
    m = /(?:chrome|crios|chromium)\/(\d+)/.exec(ua);
  } else if (isSafari) {
    m = /version\/(\d+)/.exec(ua);
  } else if (isIE) {
    m = /(?:msie |rv:)(\d+)/.exec(ua);
  }
  return m ? Number.parseInt(m[1], 10) : -1;
}

function extractMinor(ua: string): number {
  const m = /\/(\d+)\.(\d+)/.exec(ua);
  return m ? Number.parseInt(m[2], 10) : -1;
}

function extractEngineVersion(ua: string, isWebkit: boolean, isGecko: boolean): number {
  if (isWebkit) {
    const m = /applewebkit\/(\d+)/.exec(ua);
    return m ? Number.parseInt(m[1], 10) : -1;
  }
  if (isGecko) {
    const m = /rv:(\d+)\.(\d+)/.exec(ua);
    return m ? Number.parseFloat(`${m[1]}.${m[2]}`) : -1;
  }
  return -1;
}

function extractAndroidMajor(ua: string): number {
  const m = /android (\d+)/.exec(ua);
  return m ? Number.parseInt(m[1], 10) : -1;
}

let instance: BrowserInfo | null = null;

/**
 * Browser-detection helpers. Migrated from `com.vaadin.client.BrowserInfo`.
 */
export class BrowserInfo {
  private readonly ua: ParsedUa;
  private readonly touchDevice: boolean;

  private constructor() {
    this.ua = parseUa(navigator.userAgent);
    this.touchDevice = checkForTouchDevice();
  }

  static get(): BrowserInfo {
    if (instance === null) {
      instance = new BrowserInfo();
    }
    return instance;
  }

  isIE(): boolean {
    return this.ua.isIE;
  }

  isEdge(): boolean {
    return this.ua.isEdge;
  }

  isFirefox(): boolean {
    return this.ua.isFirefox;
  }

  isSafari(): boolean {
    return this.ua.isSafari;
  }

  isSafariOrIOS(): boolean {
    return this.ua.isSafari || isIos();
  }

  isChrome(): boolean {
    return this.ua.isChrome;
  }

  isGecko(): boolean {
    return this.ua.isGecko;
  }

  isWebkit(): boolean {
    return this.ua.isWebkit;
  }

  isOpera(): boolean {
    return this.ua.isOpera;
  }

  isAndroid(): boolean {
    return this.ua.isAndroid;
  }

  isTouchDevice(): boolean {
    return this.touchDevice;
  }

  isAndroidWithBrokenScrollTop(): boolean {
    return this.ua.isAndroid && (this.ua.osMajor === 3 || this.ua.osMajor === 4);
  }

  getBrowserMajorVersion(): number {
    return this.ua.browserMajor;
  }

  getBrowserMinorVersion(): number {
    return this.ua.browserMinor;
  }

  getGeckoVersion(): number {
    return this.ua.isGecko ? this.ua.engineVersion : -1;
  }

  getWebkitVersion(): number {
    return this.ua.isWebkit ? this.ua.engineVersion : -1;
  }
}

function checkForTouchDevice(): boolean {
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
}

function isIos(): boolean {
  return (
    /iPad|iPhone|iPod/.test(navigator.platform) ||
    (navigator.platform === 'MacIntel' && (navigator.maxTouchPoints ?? 0) > 1)
  );
}
