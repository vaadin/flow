/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.shared;

import java.io.Serializable;

/**
 * Parses the user agent string from the browser and provides information about
 * the browser.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class BrowserDetails implements Serializable {
    private static final String CHROME = " chrome/";
    private static final String HEADLESSCHROME = " headlesschrome/";
    private static final String OS_MAJOR = "OS major";
    private static final String OS_MINOR = "OS minor";
    private static final String BROWSER_MAJOR = "Browser major";
    private static final String BROWSER_MINOR = "Browser minor";

    /**
     * Detected operating systems.
     */
    public enum OperatingSystem {
        UNKNOWN, WINDOWS, MACOSX, LINUX, IOS, ANDROID, CHROMEOS
    }

    private boolean isGecko;
    private boolean isWebKit;
    private boolean isPresto;
    private boolean isTrident;

    private boolean isSafari;
    private boolean isChrome;
    private boolean isFirefox;
    private boolean isOpera;
    private boolean isIE;
    private boolean isEdge;

    private boolean isWindowsPhone;
    private boolean isIPad;
    private boolean isIPhone;
    private boolean isChromeOS;

    private OperatingSystem os = OperatingSystem.UNKNOWN;

    private float browserEngineVersion = -1.0f;
    private int browserMajorVersion = -1;
    private int browserMinorVersion = -1;

    private int osMajorVersion = -1;
    private int osMinorVersion = -1;

    /**
     * Create an instance based on the given user agent.
     *
     * @param userAgent
     *            User agent as provided by the browser.
     */
    public BrowserDetails(String userAgent) {
        userAgent = userAgent.toLowerCase();

        // browser engine name
        isGecko = userAgent.contains("gecko") && !userAgent.contains("webkit")
                && !userAgent.contains("trident/");
        isPresto = userAgent.contains(" presto/");
        isTrident = userAgent.contains("trident/");
        isWebKit = !isTrident && userAgent.contains("applewebkit");

        // browser name
        isChrome = userAgent.contains(CHROME) || userAgent.contains(" crios/")
                || userAgent.contains(HEADLESSCHROME);
        isOpera = userAgent.contains("opera");
        isIE = userAgent.contains("msie") && !isOpera
                && !userAgent.contains("webtv");
        // IE 11 no longer contains MSIE in the user agent
        isIE = isIE || isTrident;

        isSafari = !isChrome && !isIE && userAgent.contains("safari");
        isFirefox = userAgent.contains(" firefox/");
        if (userAgent.contains(" edge/")) {
            isEdge = true;
            isChrome = false;
            isOpera = false;
            isIE = false;
            isSafari = false;
            isFirefox = false;
            isWebKit = false;
            isGecko = false;
        }

        // Rendering engine version
        try {
            if (isGecko) {
                int rvPos = userAgent.indexOf("rv:");
                if (rvPos >= 0) {
                    String tmp = userAgent.substring(rvPos + 3);
                    tmp = tmp.replaceFirst("(\\.[0-9]+).+", "$1");
                    browserEngineVersion = Float.parseFloat(tmp);
                }
            } else if (isWebKit) {
                String tmp = userAgent
                        .substring(userAgent.indexOf("webkit/") + 7);
                tmp = tmp.replaceFirst("([0-9]+\\.[0-9]+).*", "$1");
                browserEngineVersion = Float.parseFloat(tmp);
            } else if (isTrident) {
                String tmp = userAgent
                        .substring(userAgent.indexOf("trident/") + 8);
                tmp = tmp.replaceFirst("([0-9]+\\.[0-9]+).*", "$1");
                browserEngineVersion = Float.parseFloat(tmp);
                if (browserEngineVersion > 7) {
                    // Windows 10 on launch reported Trident/8.0, now it does
                    // not
                    // Due to Edge there shouldn't ever be an Trident 8.0 or
                    // IE12
                    browserEngineVersion = 7;
                }
            } else if (isEdge) {
                browserEngineVersion = 0;
            }
        } catch (Exception e) {
            // Browser engine version parsing failed
            log("Browser engine version parsing failed for: " + userAgent, e);
        }

        // Browser version
        try {
            if (isIE) {
                if (!userAgent.contains("msie")) {
                    // IE 11+
                    int rvPos = userAgent.indexOf("rv:");
                    if (rvPos >= 0) {
                        String tmp = userAgent.substring(rvPos + 3);
                        tmp = tmp.replaceFirst("(\\.[0-9]+).+", "$1");
                        parseVersionString(tmp);
                    }
                } else if (isTrident) {
                    // potentially IE 11 in compatibility mode
                    // See
                    // https://docs.microsoft.com/en-us/previous-versions/windows/internet-explorer/ie-developer/compatibility/ms537503(v=vs.85)#trident-token
                    browserMajorVersion = 4 + (int) browserEngineVersion;
                    browserMinorVersion = 0;
                } else {
                    String ieVersionString = userAgent
                            .substring(userAgent.indexOf("msie ") + 5);
                    ieVersionString = safeSubstring(ieVersionString, 0,
                            ieVersionString.indexOf(';'));
                    parseVersionString(ieVersionString);
                }
            } else if (isFirefox) {
                int i = userAgent.indexOf(" firefox/") + 9;
                parseVersionString(safeSubstring(userAgent, i, i + 5));
            } else if (isChrome) {
                parseChromeVersion(userAgent);
            } else if (isSafari) {
                int i = userAgent.indexOf(" version/");
                if (i >= 0) {
                    i += 9;
                    parseVersionString(safeSubstring(userAgent, i, i + 5));
                } else {
                    int engineVersion = (int) (browserEngineVersion * 10);
                    if (engineVersion >= 6010 && engineVersion < 6015) {
                        browserMajorVersion = 9;
                        browserMinorVersion = 0;
                    } else if (engineVersion >= 6015 && engineVersion < 6018) {
                        browserMajorVersion = 9;
                        browserMinorVersion = 1;
                    } else if (engineVersion >= 6020 && engineVersion < 6030) {
                        browserMajorVersion = 10;
                        browserMinorVersion = 0;
                    } else if (engineVersion >= 6030 && engineVersion < 6040) {
                        browserMajorVersion = 10;
                        browserMinorVersion = 1;
                    } else if (engineVersion >= 6040 && engineVersion < 6050) {
                        browserMajorVersion = 11;
                        browserMinorVersion = 0;
                    } else if (engineVersion >= 6050 && engineVersion < 6060) {
                        browserMajorVersion = 11;
                        browserMinorVersion = 1;
                    } else if (engineVersion >= 6060 && engineVersion < 6070) {
                        browserMajorVersion = 12;
                        browserMinorVersion = 0;
                    } else if (engineVersion >= 6070) {
                        browserMajorVersion = 12;
                        browserMinorVersion = 1;
                    }
                }
            } else if (isOpera) {
                int i = userAgent.indexOf(" version/");
                if (i != -1) {
                    // Version present in Opera 10 and newer
                    i += 9; // " version/".length
                } else {
                    i = userAgent.indexOf("opera/") + 6;
                }
                parseVersionString(safeSubstring(userAgent, i, i + 5));
            } else if (isEdge) {
                int i = userAgent.indexOf(" edge/") + 6;
                parseVersionString(safeSubstring(userAgent, i, i + 8));
            }
        } catch (Exception e) {
            // Browser version parsing failed
            log("Browser version parsing failed for: " + userAgent, e);

        }

        // Operating system
        if (userAgent.contains("windows ")) {
            os = OperatingSystem.WINDOWS;
            isWindowsPhone = userAgent.contains("windows phone");
        } else if (userAgent.contains("android")) {
            os = OperatingSystem.ANDROID;
            parseAndroidVersion(userAgent);
        } else if (userAgent.contains("linux")) {
            os = OperatingSystem.LINUX;
        } else if (userAgent.contains("macintosh")
                || userAgent.contains("mac osx")
                || userAgent.contains("mac os x")) {
            isIPad = userAgent.contains("ipad");
            isIPhone = userAgent.contains("iphone");
            if (isIPad || userAgent.contains("ipod") || isIPhone) {
                os = OperatingSystem.IOS;
                parseIOSVersion(userAgent);
            } else {
                os = OperatingSystem.MACOSX;
            }
        } else if (userAgent.contains("; cros ")) {
            os = OperatingSystem.CHROMEOS;
            isChromeOS = true;
            parseChromeOSVersion(userAgent);
        }
    }

    // (X11; CrOS armv7l 6946.63.0)
    private void parseChromeOSVersion(String userAgent) {
        int start = userAgent.indexOf("; cros ");
        if (start == -1) {
            return;
        }
        int end = userAgent.indexOf(')', start);
        if (end == -1) {
            return;
        }
        int cur = end;
        while (cur >= start && userAgent.charAt(cur) != ' ') {
            cur--;
        }
        if (cur == start) {
            return;
        }
        String osVersionString = userAgent.substring(cur + 1, end);
        String[] parts = osVersionString.split("\\.");
        parseChromeOsVersionParts(parts);
    }

    private void parseChromeOsVersionParts(String[] parts) {
        osMajorVersion = -1;
        osMinorVersion = -1;

        if (parts.length > 2) {
            osMajorVersion = parseVersionPart(parts[0], OS_MAJOR);
            osMinorVersion = parseVersionPart(parts[1], OS_MINOR);
        }
    }

    private void parseChromeVersion(String userAgent) {
        int i = userAgent.indexOf(" crios/");
        if (i == -1) {
            i = userAgent.indexOf(CHROME);
            if (i == -1) {
                i = userAgent.indexOf(HEADLESSCHROME) + HEADLESSCHROME.length();
            } else {
                i += CHROME.length();
            }

            parseVersionString(safeSubstring(userAgent, i, i + 5));
        } else {
            i += 7;
            parseVersionString(safeSubstring(userAgent, i, i + 6));
        }
    }

    private void parseAndroidVersion(String userAgent) {
        // Android 5.1;
        if (!userAgent.contains("android")) {
            return;
        }

        String osVersionString = safeSubstring(userAgent,
                userAgent.indexOf("android ") + "android ".length(),
                userAgent.length());
        osVersionString = safeSubstring(osVersionString, 0,
                osVersionString.indexOf(";"));
        String[] parts = osVersionString.split("\\.");
        parseOsVersion(parts);
    }

    private void parseIOSVersion(String userAgent) {
        // OS 5_1 like Mac OS X
        if (!userAgent.contains("os ") || !userAgent.contains(" like mac")) {
            return;
        }

        String osVersionString = safeSubstring(userAgent,
                userAgent.indexOf("os ") + 3, userAgent.indexOf(" like mac"));
        String[] parts = osVersionString.split("_");
        parseOsVersion(parts);
    }

    private void parseOsVersion(String[] parts) {
        osMajorVersion = -1;
        osMinorVersion = -1;

        if (parts.length >= 1) {
            osMajorVersion = parseVersionPart(parts[0], OS_MAJOR);
        }
        if (parts.length >= 2) {
            // Some Androids report version numbers as "2.1-update1"
            int dashIndex = parts[1].indexOf('-');
            if (dashIndex > -1) {
                String dashlessVersion = parts[1].substring(0, dashIndex);
                osMinorVersion = parseVersionPart(dashlessVersion, OS_MINOR);
            } else {
                osMinorVersion = parseVersionPart(parts[1], OS_MINOR);
            }
        }
    }

    private void parseVersionString(String versionString) {
        int idx = versionString.indexOf('.');
        if (idx < 0) {
            idx = versionString.length();
        }
        String majorVersionPart = safeSubstring(versionString, 0, idx);
        browserMajorVersion = parseVersionPart(majorVersionPart, BROWSER_MAJOR);

        int idx2 = versionString.indexOf('.', idx + 1);
        if (idx2 < 0) {
            idx2 = versionString.length();
        }
        String minorVersionPart = safeSubstring(versionString, idx + 1, idx2)
                .replaceAll("[^0-9].*", "");
        browserMinorVersion = parseVersionPart(minorVersionPart, BROWSER_MINOR);
    }

    private static String safeSubstring(String string, int beginIndex,
            int endIndex) {
        int trimmedStart, trimmedEnd;
        if (beginIndex < 0) {
            trimmedStart = 0;
        } else {
            trimmedStart = beginIndex;
        }

        if (endIndex < 0 || endIndex > string.length()) {
            trimmedEnd = string.length();
        } else {
            trimmedEnd = endIndex;
        }
        return string.substring(trimmedStart, trimmedEnd);
    }

    private int parseVersionPart(String versionString, String partName) {
        try {
            return Integer.parseInt(versionString);
        } catch (Exception e) {
            log(partName + " version parsing failed for: " + versionString, e);
        }
        return -1;
    }

    /**
     * Tests if the browser is Firefox.
     *
     * @return true if it is Firefox, false otherwise
     */
    public boolean isFirefox() {
        return isFirefox;
    }

    /**
     * Tests if the browser is using the Gecko engine.
     *
     * @return true if it is Gecko, false otherwise
     */
    public boolean isGecko() {
        return isGecko;
    }

    /**
     * Tests if the browser is using the WebKit engine.
     *
     * @return true if it is WebKit, false otherwise
     */
    public boolean isWebKit() {
        return isWebKit;
    }

    /**
     * Tests if the browser is using the Presto engine.
     *
     * @return true if it is Presto, false otherwise
     */
    public boolean isPresto() {
        return isPresto;
    }

    /**
     * Tests if the browser is using the Trident engine.
     *
     * @return true if it is Trident, false otherwise
     */
    public boolean isTrident() {
        return isTrident;
    }

    /**
     * Tests if the browser is Safari.
     *
     * @return true if it is Safari, false otherwise
     */
    public boolean isSafari() {
        return isSafari;
    }

    /**
     * Tests if the browser is Safari or runs on IOS (covering also Chrome on
     * iOS).
     *
     * @return true if it is Safari or running on IOS, false otherwise
     */
    public boolean isSafariOrIOS() {
        return isSafari() || isIOS();
    }

    /**
     * Tests if the browser is Chrome.
     *
     * @return true if it is Chrome, false otherwise
     */
    public boolean isChrome() {
        return isChrome;
    }

    /**
     * Tests if the browser is Opera.
     *
     * @return true if it is Opera, false otherwise
     */
    public boolean isOpera() {
        return isOpera;
    }

    /**
     * Tests if the browser is Internet Explorer.
     *
     * @return true if it is Internet Explorer, false otherwise
     */
    public boolean isIE() {
        return isIE;
    }

    /**
     * Tests if the browser is Edge.
     *
     * @return true if it is Edge, false otherwise
     */
    public boolean isEdge() {
        return isEdge;
    }

    /**
     * Returns the version of the browser engine. For WebKit this is an integer
     * e.g., 532.0. For gecko it is a float e.g., 1.8 or 1.9.
     *
     * @return The version of the browser engine
     */
    public float getBrowserEngineVersion() {
        return browserEngineVersion;
    }

    /**
     * Returns the browser major version e.g., 3 for Firefox 3.5, 4 for Chrome
     * 4, 8 for Internet Explorer 8.
     * <p>
     * Note that Internet Explorer 8 and newer will return the document mode so
     * IE8 rendering as IE7 will return 7.
     * </p>
     *
     * @return The major version of the browser.
     */
    public final int getBrowserMajorVersion() {
        return browserMajorVersion;
    }

    /**
     * Returns the browser minor version e.g., 5 for Firefox 3.5.
     *
     * @see #getBrowserMajorVersion()
     *
     * @return The minor version of the browser, or -1 if not known/parsed.
     */
    public final int getBrowserMinorVersion() {
        return browserMinorVersion;
    }

    /**
     * Tests if the browser is run on Windows.
     *
     * @return true if run on Windows, false otherwise
     */
    public boolean isWindows() {
        return os == OperatingSystem.WINDOWS;
    }

    /**
     * Tests if the browser is run on Windows Phone.
     *
     * @return true if run on Windows Phone, false otherwise
     */
    public boolean isWindowsPhone() {
        return isWindowsPhone;
    }

    /**
     * Tests if the browser is run on Mac OSX.
     *
     * @return true if run on Mac OSX, false otherwise
     */
    public boolean isMacOSX() {
        return os == OperatingSystem.MACOSX;
    }

    /**
     * Tests if the browser is run on Linux.
     *
     * @return true if run on Linux, false otherwise
     */
    public boolean isLinux() {
        return os == OperatingSystem.LINUX;
    }

    /**
     * Tests if the browser is run on Android.
     *
     * @return true if run on Android, false otherwise
     */
    public boolean isAndroid() {
        return os == OperatingSystem.ANDROID;
    }

    /**
     * Tests if the browser is run in iOS.
     *
     * @return true if run in iOS, false otherwise
     */
    public boolean isIOS() {
        return os == OperatingSystem.IOS;
    }

    /**
     * Tests if the browser is run on iPhone.
     *
     * @return true if run on iPhone, false otherwise
     */
    public boolean isIPhone() {
        return isIPhone;
    }

    /**
     * Tests if the browser is run on Chrome OS (e.g. a Chromebook).
     *
     * @return true if run on Chrome OS, false otherwise
     */
    public boolean isChromeOS() {
        return isChromeOS;
    }

    /**
     * Tests if the browser is run on iPad.
     *
     * @return true if run on iPad, false otherwise
     */
    public boolean isIPad() {
        return isIPad;
    }

    /**
     * Returns the major version of the operating system. Currently only
     * supported for mobile devices (iOS/Android)
     *
     * @return The major version or -1 if unknown
     */
    public int getOperatingSystemMajorVersion() {
        return osMajorVersion;
    }

    /**
     * Returns the minor version of the operating system. Currently only
     * supported for mobile devices (iOS/Android)
     *
     * @return The minor version or -1 if unknown
     */
    public int getOperatingSystemMinorVersion() {
        return osMinorVersion;
    }

    /**
     * Checks if the browser is so old that it simply won't work.
     *
     * @return true if the browser won't work, false if not the browser is
     *         supported or might work
     */
    public boolean isTooOldToFunctionProperly() {
        // Check Trident version to detect compatibility mode
        if (isIE() && getBrowserMajorVersion() < 11) {
            return true;
        }
        // Safari 9+
        if (isSafari() && getBrowserMajorVersion() < 9) {
            return true;
        }
        // Firefox 43+ for now
        if (isFirefox() && getBrowserMajorVersion() < 43) {
            return true;
        }
        // Opera 34+ for now
        if (isOpera() && getBrowserMajorVersion() < 34) {
            return true;
        }
        // Chrome 47+ for now
        if (isChrome() && getBrowserMajorVersion() < 47) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the browser supports ECMAScript 6, based on the user agent. The
     * main features required to consider the browser ES6 compatible are ES6
     * Classes, let/const support and arrow functions.
     *
     * @return <code>true</code> if the browser supports ES6, <code>false</code>
     *         otherwise.
     */
    public boolean isEs6Supported() {

        if (isEs5AdapterNeeded()) {
            return false;
        }

        if (isWebKit() && getBrowserEngineVersion() >= 604) {
            // Covers Safari 11+ and all kind of webkit views on iOS 11+
            return true;
        }

        // Safari 10+.
        if (isSafari() && getBrowserMajorVersion() >= 10) {
            return true;
        }
        // Firefox 51+
        if (isFirefox() && getBrowserMajorVersion() >= 51) {
            return true;
        }
        // Opera 36+
        if (isOpera() && getBrowserMajorVersion() >= 36) {
            return true;
        }
        // Chrome 49+
        if (isChrome() && getBrowserMajorVersion() >= 49) {
            return true;
        }
        // Edge 15.15063+
        if (isEdge() && (getBrowserMajorVersion() > 15
                || (getBrowserMajorVersion() == 15
                        && getBrowserMinorVersion() >= 15063))) {
            return true;
        }

        return false;
    }

    /**
     * Checks if the browser needs `custom-elements-es5-adapter.js` to be
     * loaded.
     * <p>
     * This adapter file is needed when the browser has some ES6 capabilities,
     * but a ES5 files are served instead. This happens when the browser doesn't
     * support all ES6 features needed for Flow to work properly, or when some
     * ES6 features have bugs under conditions used by the application.
     *
     * @return <code>true</code> if the browser needs the adapter,
     *         <code>false</code> otherwise.
     */
    public boolean isEs5AdapterNeeded() {
        // Safari 10 / IOS 10 has a known issue on
        // https://caniuse.com/#feat=let, which
        // needs a separate Es5 adapter for production mode
        if (isIOS() && getOperatingSystemMajorVersion() == 10) {
            return true;
        }
        if (isSafari() && getBrowserMajorVersion() == 10) {
            return true;
        }
        return false;
    }

    private static void log(String error, Exception e) {
        // "Logs" to stdout so the problem can be found but does not prevent
        // using the app. As this class is shared, we do not use
        // java.util.logging
        System.err.println(error + ' ' + e.getMessage());
    }

}
