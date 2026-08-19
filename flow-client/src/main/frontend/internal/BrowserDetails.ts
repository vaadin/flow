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

// User-agent parser migrated from com.vaadin.flow.shared.BrowserDetails. This is
// a dependency-free leaf utility: it inspects only the lower-cased user-agent
// string. The parsing is intentionally lazy (each aspect is parsed on first
// access) to mirror the Java original exactly. Members that were private in Java
// use the JS-native `#` private prefix here; the enums and the browser/OS/engine
// predicates that were public are exported. As in Java, diagnostics on parse failure are written
// straight to the console rather than through the engine logging facade, so the
// module stays a leaf with no imports.

/** Detected operating systems. */
export enum OperatingSystem {
  UNKNOWN = 0,
  WINDOWS = 1,
  MACOSX = 2,
  LINUX = 3,
  IOS = 4,
  ANDROID = 5,
  CHROMEOS = 6
}

/**
 * Detected browser families.
 */
export enum BrowserName {
  UNKNOWN = 0,
  SAFARI = 1,
  CHROME = 2,
  FIREFOX = 3,
  OPERA = 4,
  IE = 5,
  EDGE = 6
}

/**
 * Detected browser rendering engines.
 */
export enum BrowserEngine {
  UNKNOWN = 0,
  GECKO = 1,
  WEBKIT = 2,
  PRESTO = 3,
  TRIDENT = 4
}

const CHROME = ' chrome/';
const HEADLESSCHROME = ' headlesschrome/';
const OS_MAJOR = 'OS major';
const OS_MINOR = 'OS minor';
const BROWSER_MAJOR = 'Browser major';
const BROWSER_MINOR = 'Browser minor';

// Sentinel matching Java's -10 "not parsed yet" markers.
const NOT_PARSED = -10;

/**
 * Splits a string like Java's `String.split(regex)` with the default limit,
 * which drops trailing empty strings (JavaScript's split keeps them).
 */
function splitDroppingTrailingEmpty(value: string, separator: string): string[] {
  const parts = value.split(separator);
  while (parts.length > 0 && parts[parts.length - 1] === '') {
    parts.pop();
  }
  return parts;
}

/**
 * Substring that clamps out-of-range indices instead of throwing, mirroring the
 * Java {@code safeSubstring} helper. Uses slice (which never swaps arguments) so
 * a start past the end yields an empty string, matching how the Java version's
 * downstream parsing degrades.
 */
function safeSubstring(value: string, beginIndex: number, endIndex: number): string {
  const trimmedStart = beginIndex < 0 ? 0 : beginIndex;
  const trimmedEnd = endIndex < 0 || endIndex > value.length ? value.length : endIndex;
  return value.slice(trimmedStart, trimmedEnd);
}

/**
 * Gets the length of the version substring up to the next space.
 *
 * @param userAgent
 *            user agent string
 * @param startIndex
 *            index for version string start
 * @return length of version number
 */
function getVersionStringLength(userAgent: string, startIndex: number): number {
  const versionSubString = userAgent.substring(startIndex);
  let versionBreak = versionSubString.indexOf(' ');
  if (versionBreak === -1) {
    versionBreak = versionSubString.length;
  }
  return versionBreak;
}

/**
 * Parses the user agent string from the browser and provides information about
 * the browser.
 *
 * @deprecated For browser information users should parse the user-agent using a
 *             parsing library like ua-parser/uap-java
 */
export class BrowserDetails {
  #browserName?: BrowserName;

  #browserEngine?: BrowserEngine;

  #windowsPhone = false;

  #iPad = false;

  #iPhone = false;

  #chromeOS = false;

  #os?: OperatingSystem;

  #browserEngineVersion = -10.0;

  #browserMajorVersion = NOT_PARSED;

  #browserMinorVersion = NOT_PARSED;

  #osMajorVersion = NOT_PARSED;

  #osMinorVersion = NOT_PARSED;

  readonly #userAgent: string;

  /**
   * Create an instance based on the given user agent.
   *
   * @param userAgentString
   *            User agent as provided by the browser.
   */
  constructor(userAgentString: string) {
    this.#userAgent = userAgentString.toLowerCase();
  }

  #parseBrowserEngine(): void {
    const ua = this.#userAgent;
    // browser engine name
    if (ua.includes('gecko') && !ua.includes('webkit') && !ua.includes('trident/')) {
      this.#browserEngine = BrowserEngine.GECKO;
    } else if (ua.includes(' presto/')) {
      this.#browserEngine = BrowserEngine.PRESTO;
    } else if (ua.includes('trident/')) {
      this.#browserEngine = BrowserEngine.TRIDENT;
    } else if (!ua.includes('trident/') && ua.includes('applewebkit')) {
      this.#browserEngine = BrowserEngine.WEBKIT;
    } else {
      this.#browserEngine = BrowserEngine.UNKNOWN;
    }
  }

  #parseBrowserName(): void {
    const ua = this.#userAgent;
    // browser name
    if (ua.includes(' edge/') || ua.includes(' edg/') || ua.includes(' edga/') || ua.includes(' edgios/')) {
      this.#browserName = BrowserName.EDGE;
    } else if (
      (ua.includes(CHROME) || ua.includes(' crios/') || ua.includes(HEADLESSCHROME)) &&
      !ua.includes(' opr/')
    ) {
      this.#browserName = BrowserName.CHROME;
    } else if (ua.includes('opera') || ua.includes(' opr/')) {
      this.#browserName = BrowserName.OPERA;
    } else if ((ua.includes('msie') && !ua.includes('webtv')) || ua.includes('trident/')) {
      // check trident engine as IE 11 no longer contains MSIE in the user agent
      this.#browserName = BrowserName.IE;
    } else if (ua.includes(' firefox/') || ua.includes('fxios/')) {
      this.#browserName = BrowserName.FIREFOX;
    } else if (ua.includes('safari')) {
      this.#browserName = BrowserName.SAFARI;
    } else {
      this.#browserName = BrowserName.UNKNOWN;
    }
  }

  #parseEngineVersion(): void {
    const ua = this.#userAgent;
    // Rendering engine version
    if (this.#browserEngine === undefined) {
      this.#parseBrowserEngine();
    }
    try {
      if (this.#browserEngine === BrowserEngine.GECKO) {
        const rvPos = ua.indexOf('rv:');
        if (rvPos >= 0) {
          let tmp = ua.substring(rvPos + 3);
          tmp = tmp.replace(/(\.[0-9]+).+/, '$1');
          this.#browserEngineVersion = this.#parseFloatOrThrow(tmp);
        }
      } else if (this.#browserEngine === BrowserEngine.WEBKIT) {
        let tmp = ua.substring(ua.indexOf('webkit/') + 7);
        tmp = tmp.replace(/([0-9]+\.[0-9]+).*/, '$1');
        this.#browserEngineVersion = this.#parseFloatOrThrow(tmp);
      } else if (this.#browserEngine === BrowserEngine.TRIDENT) {
        let tmp = ua.substring(ua.indexOf('trident/') + 8);
        tmp = tmp.replace(/([0-9]+\.[0-9]+).*/, '$1');
        this.#browserEngineVersion = this.#parseFloatOrThrow(tmp);
        if (this.#browserEngineVersion > 7) {
          // Windows 10 on launch reported Trident/8.0, now it does not
          // Due to Edge there shouldn't ever be an Trident 8.0 or IE12
          this.#browserEngineVersion = 7;
        }
      } else if (this.#browserName === BrowserName.EDGE) {
        this.#browserEngineVersion = 0;
      } else {
        this.#browserEngineVersion = -1.0;
      }
    } catch (e) {
      // Browser engine version parsing failed
      this.log(`Browser engine version parsing failed for: ${ua}`, e);
    }
  }

  #parseBrowserVersion(): void {
    const ua = this.#userAgent;
    this.#browserMajorVersion = -1;
    this.#browserMinorVersion = -1;

    if (this.#browserName === undefined) {
      this.#parseBrowserName();
    }
    if (this.#browserEngine === undefined) {
      this.#parseBrowserEngine();
    }
    // Browser version
    try {
      if (this.#browserName === BrowserName.IE) {
        if (!ua.includes('msie')) {
          // IE 11+
          const rvPos = ua.indexOf('rv:');
          if (rvPos >= 0) {
            let tmp = ua.substring(rvPos + 3);
            tmp = tmp.replace(/(\.[0-9]+).+/, '$1');
            this.#parseVersionString(tmp, ua);
          }
        } else if (this.#browserEngine === BrowserEngine.TRIDENT) {
          // potentially IE 11 in compatibility mode
          this.#browserMajorVersion = 4 + Math.trunc(this.#browserEngineVersion);
          this.#browserMinorVersion = 0;
        } else {
          let ieVersionString = ua.substring(ua.indexOf('msie ') + 5);
          ieVersionString = safeSubstring(ieVersionString, 0, ieVersionString.indexOf(';'));
          this.#parseVersionString(ieVersionString, ua);
        }
      } else if (this.#browserName === BrowserName.FIREFOX) {
        let i = ua.indexOf(' fxios/');
        if (i !== -1) {
          i = ua.indexOf(' fxios/') + 7;
        } else {
          i = ua.indexOf(' firefox/') + 9;
        }
        this.#parseVersionString(safeSubstring(ua, i, i + getVersionStringLength(ua, i)), ua);
      } else if (this.#browserName === BrowserName.CHROME) {
        this.#parseChromeVersion(ua);
      } else if (this.#browserName === BrowserName.SAFARI) {
        let i = ua.indexOf(' version/');
        if (i >= 0) {
          i += 9;
          this.#parseVersionString(safeSubstring(ua, i, i + getVersionStringLength(ua, i)), ua);
        } else {
          if (this.#browserEngineVersion === NOT_PARSED) {
            this.#parseEngineVersion();
          }
          const engineVersion = Math.trunc(this.#browserEngineVersion * 10);
          if (engineVersion >= 6010 && engineVersion < 6015) {
            this.#browserMajorVersion = 9;
            this.#browserMinorVersion = 0;
          } else if (engineVersion >= 6015 && engineVersion < 6018) {
            this.#browserMajorVersion = 9;
            this.#browserMinorVersion = 1;
          } else if (engineVersion >= 6020 && engineVersion < 6030) {
            this.#browserMajorVersion = 10;
            this.#browserMinorVersion = 0;
          } else if (engineVersion >= 6030 && engineVersion < 6040) {
            this.#browserMajorVersion = 10;
            this.#browserMinorVersion = 1;
          } else if (engineVersion >= 6040 && engineVersion < 6050) {
            this.#browserMajorVersion = 11;
            this.#browserMinorVersion = 0;
          } else if (engineVersion >= 6050 && engineVersion < 6060) {
            this.#browserMajorVersion = 11;
            this.#browserMinorVersion = 1;
          } else if (engineVersion >= 6060 && engineVersion < 6070) {
            this.#browserMajorVersion = 12;
            this.#browserMinorVersion = 0;
          } else if (engineVersion >= 6070) {
            this.#browserMajorVersion = 12;
            this.#browserMinorVersion = 1;
          }
        }
      } else if (this.#browserName === BrowserName.OPERA) {
        let i = ua.indexOf(' version/');
        if (i !== -1) {
          // Version present in Opera 10 and newer
          i += 9; // " version/".length
        } else if (ua.includes(' opr/')) {
          i = ua.indexOf(' opr/') + 5;
        } else {
          i = ua.indexOf('opera/') + 6;
        }
        this.#parseVersionString(safeSubstring(ua, i, i + getVersionStringLength(ua, i)), ua);
      } else if (this.#browserName === BrowserName.EDGE) {
        let i = ua.indexOf(' edge/') + 6;
        if (ua.includes(' edg/')) {
          i = ua.indexOf(' edg/') + 5;
        } else if (ua.includes(' edga/')) {
          i = ua.indexOf(' edga/') + 6;
        } else if (ua.includes(' edgios/')) {
          i = ua.indexOf(' edgios/') + 8;
        }
        this.#parseVersionString(safeSubstring(ua, i, i + getVersionStringLength(ua, i)), ua);
      }
    } catch (e) {
      // Browser version parsing failed
      this.log(`Browser version parsing failed for: ${ua}`, e);
    }
  }

  #parseOperatingSystem(): void {
    const ua = this.#userAgent;
    // Operating system
    if (ua.includes('windows ')) {
      this.#os = OperatingSystem.WINDOWS;
      this.#windowsPhone = ua.includes('windows phone');
    } else if (ua.includes('android')) {
      this.#os = OperatingSystem.ANDROID;
      this.#parseAndroidVersion(ua);
    } else if (ua.includes('linux')) {
      this.#os = OperatingSystem.LINUX;
    } else if (ua.includes('macintosh') || ua.includes('mac osx') || ua.includes('mac os x')) {
      this.#iPad = ua.includes('ipad');
      this.#iPhone = ua.includes('iphone');
      if (this.#iPad || this.#iPhone) {
        this.#os = OperatingSystem.IOS;
        this.#parseIOSVersion(ua);
      } else {
        this.#os = OperatingSystem.MACOSX;
      }
    } else if (ua.includes('; cros ')) {
      this.#os = OperatingSystem.CHROMEOS;
      this.#chromeOS = true;
      this.#parseChromeOSVersion(ua);
    } else {
      this.#os = OperatingSystem.UNKNOWN;
    }
  }

  // (X11; CrOS armv7l 6946.63.0)
  #parseChromeOSVersion(userAgent: string): void {
    const start = userAgent.indexOf('; cros ');
    if (start === -1) {
      return;
    }
    const end = userAgent.indexOf(')', start);
    if (end === -1) {
      return;
    }
    let cur = end;
    while (cur >= start && userAgent.charAt(cur) !== ' ') {
      cur--;
    }
    if (cur === start) {
      return;
    }
    const osVersionString = userAgent.substring(cur + 1, end);
    const parts = splitDroppingTrailingEmpty(osVersionString, '.');
    this.#parseChromeOsVersionParts(parts, userAgent);
  }

  #parseChromeOsVersionParts(parts: string[], userAgent: string): void {
    this.#osMajorVersion = -1;
    this.#osMinorVersion = -1;

    if (parts.length > 2) {
      this.#osMajorVersion = this.#parseVersionPart(parts[0], OS_MAJOR, userAgent);
      this.#osMinorVersion = this.#parseVersionPart(parts[1], OS_MINOR, userAgent);
    }
  }

  #parseChromeVersion(userAgent: string): void {
    const crios = ' crios/';
    let i = userAgent.indexOf(crios);
    if (i === -1) {
      i = userAgent.indexOf(CHROME);
      if (i === -1) {
        i = userAgent.indexOf(HEADLESSCHROME) + HEADLESSCHROME.length;
      } else {
        i += CHROME.length;
      }
      const versionBreak = getVersionStringLength(userAgent, i);
      this.#parseVersionString(safeSubstring(userAgent, i, i + versionBreak), userAgent);
    } else {
      i += crios.length; // move index to version string start
      const versionBreak = getVersionStringLength(userAgent, i);
      this.#parseVersionString(safeSubstring(userAgent, i, i + versionBreak), userAgent);
    }
  }

  #parseAndroidVersion(userAgent: string): void {
    // Android 5.1;
    if (!userAgent.includes('android ')) {
      this.#osMajorVersion = -1;
      this.#osMinorVersion = -1;
      return;
    }

    if (userAgent.includes('ddg_android/')) {
      const startIndex = userAgent.indexOf('ddg_android/');
      const osVersionString = safeSubstring(
        userAgent,
        startIndex + 'ddg_android/'.length,
        userAgent.indexOf(' ', startIndex)
      );
      const parts = splitDroppingTrailingEmpty(osVersionString, '.');
      this.#parseOsVersion(parts, userAgent);
      return;
    }

    if (userAgent.includes('callpod keeper for android')) {
      const token = '; android ';
      const startIndex = userAgent.indexOf(token) + token.length;
      const endIndex = userAgent.indexOf(';', startIndex);
      const osVersionString = safeSubstring(userAgent, startIndex, endIndex);
      const parts = splitDroppingTrailingEmpty(osVersionString, '.');
      this.#parseOsVersion(parts, userAgent);
      return;
    }

    let osVersionString = safeSubstring(userAgent, userAgent.indexOf('android ') + 'android '.length, userAgent.length);
    const semicolonIndex = osVersionString.indexOf(';');
    const bracketIndex = osVersionString.indexOf(')');
    const endIndex = semicolonIndex !== -1 && semicolonIndex < bracketIndex ? semicolonIndex : bracketIndex;
    osVersionString = safeSubstring(osVersionString, 0, endIndex);
    const parts = splitDroppingTrailingEmpty(osVersionString, '.');
    this.#parseOsVersion(parts, userAgent);
  }

  #parseIOSVersion(userAgent: string): void {
    // OS 5_1 like Mac OS X
    if (!userAgent.includes('os ') || !userAgent.includes(' like mac')) {
      this.#osMajorVersion = -1;
      this.#osMinorVersion = -1;
      return;
    }

    const osVersionString = safeSubstring(userAgent, userAgent.indexOf('os ') + 3, userAgent.indexOf(' like mac'));
    const parts = splitDroppingTrailingEmpty(osVersionString, '_');
    this.#parseOsVersion(parts, userAgent);
  }

  #parseOsVersion(parts: string[], userAgent: string): void {
    this.#osMajorVersion = -1;
    this.#osMinorVersion = -1;

    if (parts.length >= 1) {
      this.#osMajorVersion = this.#parseVersionPart(parts[0], OS_MAJOR, userAgent);
    }
    if (parts.length >= 2) {
      // Some Androids report version numbers as "2.1-update1"
      const dashIndex = parts[1].indexOf('-');
      if (dashIndex > -1) {
        const dashlessVersion = parts[1].substring(0, dashIndex);
        this.#osMinorVersion = this.#parseVersionPart(dashlessVersion, OS_MINOR, userAgent);
      } else {
        this.#osMinorVersion = this.#parseVersionPart(parts[1], OS_MINOR, userAgent);
      }
    }
  }

  #parseVersionString(versionString: string, userAgent: string): void {
    let idx = versionString.indexOf('.');
    if (idx < 0) {
      idx = versionString.length;
    }
    const majorVersionPart = safeSubstring(versionString, 0, idx);
    this.#browserMajorVersion = this.#parseVersionPart(majorVersionPart, BROWSER_MAJOR, userAgent);

    if (this.#browserMajorVersion === -1) {
      // no need to scan for minor if major version scanning failed.
      return;
    }

    let idx2 = versionString.indexOf('.', idx + 1);
    if (idx2 < 0) {
      // If string only contains major version, set minor to 0.
      if (versionString.substring(idx).length === 0) {
        this.#browserMinorVersion = 0;
        return;
      }
      idx2 = versionString.length;
    }
    const minorVersionPart = safeSubstring(versionString, idx + 1, idx2).replace(/[^0-9].*/, '');
    this.#browserMinorVersion = this.#parseVersionPart(minorVersionPart, BROWSER_MINOR, userAgent);
  }

  /** Parses a float, throwing (like Java's Float.parseFloat) when not numeric. */
  #parseFloatOrThrow(value: string): number {
    const parsed = Number.parseFloat(value);
    if (Number.isNaN(parsed)) {
      throw new Error(`Not a number: "${value}"`);
    }
    return parsed;
  }

  #parseVersionPart(versionString: string, partName: string, userAgent: string): number {
    // Mirror the strictness of Java's Integer.parseInt, which throws (rather
    // than parsing a prefix like the lenient JS parseInt) on non-integer input.
    if (/^[+-]?[0-9]+$/.test(versionString)) {
      return Number.parseInt(versionString, 10);
    }
    this.log(`${partName} version parsing failed for: "${versionString}"\nWith userAgent: ${userAgent}`, undefined);
    return -1;
  }

  /**
   * Tests if the browser is Firefox.
   *
   * @return true if it is Firefox, false otherwise
   */
  isFirefox(): boolean {
    if (this.#browserName === undefined) {
      this.#parseBrowserName();
    }
    return this.#browserName === BrowserName.FIREFOX;
  }

  /**
   * Tests if the browser is using the Gecko engine.
   *
   * @return true if it is Gecko, false otherwise
   */
  isGecko(): boolean {
    if (this.#browserEngine === undefined) {
      this.#parseBrowserEngine();
    }
    return this.#browserEngine === BrowserEngine.GECKO;
  }

  /**
   * Tests if the browser is using the WebKit engine.
   *
   * @return true if it is WebKit, false otherwise
   */
  isWebKit(): boolean {
    if (this.#browserEngine === undefined) {
      this.#parseBrowserEngine();
    }
    return this.#browserEngine === BrowserEngine.WEBKIT;
  }

  /**
   * Tests if the browser is using the Presto engine.
   *
   * @return true if it is Presto, false otherwise
   */
  isPresto(): boolean {
    if (this.#browserEngine === undefined) {
      this.#parseBrowserEngine();
    }
    return this.#browserEngine === BrowserEngine.PRESTO;
  }

  /**
   * Tests if the browser is using the Trident engine.
   *
   * @return true if it is Trident, false otherwise
   */
  isTrident(): boolean {
    if (this.#browserEngine === undefined) {
      this.#parseBrowserEngine();
    }
    return this.#browserEngine === BrowserEngine.TRIDENT;
  }

  /**
   * Tests if the browser is Safari.
   *
   * @return true if it is Safari, false otherwise
   */
  isSafari(): boolean {
    if (this.#browserName === undefined) {
      this.#parseBrowserName();
    }
    return this.#browserName === BrowserName.SAFARI;
  }

  /**
   * Tests if the browser is Chrome.
   *
   * @return true if it is Chrome, false otherwise
   */
  isChrome(): boolean {
    if (this.#browserName === undefined) {
      this.#parseBrowserName();
    }
    return this.#browserName === BrowserName.CHROME;
  }

  /**
   * Tests if the browser is Opera.
   *
   * @return true if it is Opera, false otherwise
   */
  isOpera(): boolean {
    if (this.#browserName === undefined) {
      this.#parseBrowserName();
    }
    return this.#browserName === BrowserName.OPERA;
  }

  /**
   * Tests if the browser is Internet Explorer.
   *
   * @return true if it is Internet Explorer, false otherwise
   */
  isIE(): boolean {
    if (this.#browserName === undefined) {
      this.#parseBrowserName();
    }
    return this.#browserName === BrowserName.IE;
  }

  /**
   * Tests if the browser is Edge.
   *
   * @return true if it is Edge, false otherwise
   */
  isEdge(): boolean {
    if (this.#browserName === undefined) {
      this.#parseBrowserName();
    }
    return this.#browserName === BrowserName.EDGE;
  }

  /**
   * Returns the version of the browser engine. For WebKit this is an integer
   * e.g., 532.0. For gecko it is a float e.g., 1.8 or 1.9.
   *
   * @return The version of the browser engine
   */
  getBrowserEngineVersion(): number {
    if (this.#browserEngineVersion === NOT_PARSED) {
      this.#parseEngineVersion();
    }
    return this.#browserEngineVersion;
  }

  /**
   * Returns the browser major version e.g., 3 for Firefox 3.5, 4 for Chrome
   * 4, 8 for Internet Explorer 8.
   *
   * @return The major version of the browser.
   */
  getBrowserMajorVersion(): number {
    if (this.#browserMajorVersion === NOT_PARSED) {
      this.#parseBrowserVersion();
    }
    return this.#browserMajorVersion;
  }

  /**
   * Returns the browser minor version e.g., 5 for Firefox 3.5.
   *
   * @see {@link getBrowserMajorVersion}
   *
   * @return The minor version of the browser, or -1 if not known/parsed.
   */
  getBrowserMinorVersion(): number {
    if (this.#browserMinorVersion === NOT_PARSED) {
      this.#parseBrowserVersion();
    }
    return this.#browserMinorVersion;
  }

  #getOs(): OperatingSystem {
    if (this.#os === undefined) {
      this.#parseOperatingSystem();
    }
    // parseOperatingSystem always assigns os; the fallback only satisfies the
    // type checker.
    return this.#os ?? OperatingSystem.UNKNOWN;
  }

  /**
   * Tests if the browser is run on Windows.
   *
   * @return true if run on Windows, false otherwise
   */
  isWindows(): boolean {
    return this.#getOs() === OperatingSystem.WINDOWS;
  }

  /**
   * Tests if the browser is run on Windows Phone.
   *
   * @return true if run on Windows Phone, false otherwise
   */
  isWindowsPhone(): boolean {
    return this.#windowsPhone;
  }

  /**
   * Tests if the browser is run on Mac OSX.
   *
   * @return true if run on Mac OSX, false otherwise
   */
  isMacOSX(): boolean {
    return this.#getOs() === OperatingSystem.MACOSX;
  }

  /**
   * Tests if the browser is run on Linux.
   *
   * @return true if run on Linux, false otherwise
   */
  isLinux(): boolean {
    return this.#getOs() === OperatingSystem.LINUX;
  }

  /**
   * Tests if the browser is run on Android.
   *
   * @return true if run on Android, false otherwise
   */
  isAndroid(): boolean {
    return this.#getOs() === OperatingSystem.ANDROID;
  }

  /**
   * Tests if the browser is run on iPhone.
   *
   * @return true if run on iPhone, false otherwise
   */
  isIPhone(): boolean {
    if (this.#os === undefined) {
      this.#parseOperatingSystem();
    }
    return this.#iPhone;
  }

  /**
   * Tests if the browser is run on iPad.
   *
   * @return true if run on iPad, false otherwise
   */
  isIPad(): boolean {
    if (this.#os === undefined) {
      this.#parseOperatingSystem();
    }
    return this.#iPad;
  }

  /**
   * Tests if the browser is run on Chrome OS (e.g. a Chromebook).
   *
   * @return true if run on Chrome OS, false otherwise
   */
  isChromeOS(): boolean {
    if (this.#os === undefined) {
      this.#parseOperatingSystem();
    }
    return this.#chromeOS;
  }

  /**
   * Returns the major version of the operating system. Currently only
   * supported for mobile devices (iOS/Android)
   *
   * @return The major version or -1 if unknown
   */
  getOperatingSystemMajorVersion(): number {
    if (this.#os === undefined) {
      this.#parseOperatingSystem();
    }
    return this.#osMajorVersion;
  }

  /**
   * Returns the minor version of the operating system. Currently only
   * supported for mobile devices (iOS/Android)
   *
   * @return The minor version or -1 if unknown
   */
  getOperatingSystemMinorVersion(): number {
    if (this.#os === undefined) {
      this.#parseOperatingSystem();
    }
    return this.#osMinorVersion;
  }

  /**
   * Checks if the browser is so old that it simply won't work.
   *
   * @return true if the browser won't work, false if not the browser is
   *         supported or might work
   */
  isTooOldToFunctionProperly(): boolean {
    // IE is not supported
    if (this.isIE()) {
      return true;
    }
    // Only ChromeEdge is supported
    if (this.isEdge() && this.getBrowserMajorVersion() < 79) {
      return true;
    }
    // Safari 14+
    if (this.isSafari() && this.getBrowserMajorVersion() < 14) {
      if (
        this.isIPhone() &&
        (this.getOperatingSystemMajorVersion() > 14 ||
          (this.getOperatingSystemMajorVersion() === 14 && this.getOperatingSystemMinorVersion() >= 7))
      ) {
        // #11654
        return false;
      }
      return true;
    }
    // Firefox 78+ for now
    if (this.isFirefox() && this.getBrowserMajorVersion() < 78) {
      return true;
    }
    // Opera 58+ for now
    if (this.isOpera() && this.getBrowserMajorVersion() < 58) {
      return true;
    }
    // Chrome 71+ for now
    if (this.isChrome() && this.getBrowserMajorVersion() < 71) {
      return true;
    }
    return false;
  }

  protected log(error: string, e: unknown): void {
    // "Logs" to the console so the problem can be found but does not prevent
    // using the app. As this is a shared leaf utility with no dependencies, it
    // writes directly rather than going through the engine logging facade.
    const message = e instanceof Error ? `${error} ${e.message}` : error;
    // eslint-disable-next-line no-console
    console.error(message);
  }
}
