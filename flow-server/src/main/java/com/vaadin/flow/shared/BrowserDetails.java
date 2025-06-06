/*
 * Copyright 2000-2025 Vaadin Ltd.
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
        isChrome = (userAgent.contains(CHROME) || userAgent.contains(" crios/")
                || userAgent.contains(HEADLESSCHROME))
                && !userAgent.contains(" opr/");
        isOpera = userAgent.contains("opera") || userAgent.contains(" opr/");
        isIE = userAgent.contains("msie") && !isOpera
                && !userAgent.contains("webtv");
        // IE 11 no longer contains MSIE in the user agent
        isIE = isIE || isTrident;

        isSafari = !isChrome && !isIE && !isOpera
                && userAgent.contains("safari");
        isFirefox = userAgent.contains(" firefox/")
                || userAgent.contains("fxios/");
        if (userAgent.contains(" edge/") || userAgent.contains(" edg/")
                || userAgent.contains(" edga/")
                || userAgent.contains(" edgios/")) {
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
                        parseVersionString(tmp, userAgent);
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
                    parseVersionString(ieVersionString, userAgent);
                }
            } else if (isFirefox) {
                int i = userAgent.indexOf(" fxios/");
                if (i != -1) {
                    // Version present in Opera 10 and newer
                    i = userAgent.indexOf(" fxios/") + 7;
                } else {
                    i = userAgent.indexOf(" firefox/") + 9;
                }
                parseVersionString(
                        safeSubstring(userAgent, i,
                                i + getVersionStringLength(userAgent, i)),
                        userAgent);
            } else if (isChrome) {
                parseChromeVersion(userAgent);
            } else if (isSafari) {
                int i = userAgent.indexOf(" version/");
                if (i >= 0) {
                    i += 9;
                    parseVersionString(
                            safeSubstring(userAgent, i,
                                    i + getVersionStringLength(userAgent, i)),
                            userAgent);
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
                } else if (userAgent.contains(" opr/")) {
                    i = userAgent.indexOf(" opr/") + 5;
                } else {
                    i = userAgent.indexOf("opera/") + 6;
                }
                parseVersionString(
                        safeSubstring(userAgent, i,
                                i + getVersionStringLength(userAgent, i)),
                        userAgent);
            } else if (isEdge) {
                int i = userAgent.indexOf(" edge/") + 6;
                if (userAgent.contains(" edg/")) {
                    i = userAgent.indexOf(" edg/") + 5;
                } else if (userAgent.contains(" edga/")) {
                    i = userAgent.indexOf(" edga/") + 6;
                } else if (userAgent.contains(" edgios/")) {
                    i = userAgent.indexOf(" edgios/") + 8;
                }

                parseVersionString(
                        safeSubstring(userAgent, i,
                                i + getVersionStringLength(userAgent, i)),
                        userAgent);
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
            if (isIPad || isIPhone) {
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
        parseChromeOsVersionParts(parts, userAgent);
    }

    private void parseChromeOsVersionParts(String[] parts, String userAgent) {
        osMajorVersion = -1;
        osMinorVersion = -1;

        if (parts.length > 2) {
            osMajorVersion = parseVersionPart(parts[0], OS_MAJOR, userAgent);
            osMinorVersion = parseVersionPart(parts[1], OS_MINOR, userAgent);
        }
    }

    private void parseChromeVersion(String userAgent) {
        final String crios = " crios/";
        int i = userAgent.indexOf(crios);
        if (i == -1) {
            i = userAgent.indexOf(CHROME);
            if (i == -1) {
                i = userAgent.indexOf(HEADLESSCHROME) + HEADLESSCHROME.length();
            } else {
                i += CHROME.length();
            }
            int versionBreak = getVersionStringLength(userAgent, i);
            parseVersionString(safeSubstring(userAgent, i, i + versionBreak),
                    userAgent);
        } else {
            i += crios.length(); // move index to version string start
            int versionBreak = getVersionStringLength(userAgent, i);
            parseVersionString(safeSubstring(userAgent, i, i + versionBreak),
                    userAgent);
        }
    }

    /**
     * Get the full version string until space.
     *
     * @param userAgent
     *            user agent string
     * @param startIndex
     *            index for version string start
     * @return length of version number
     */
    private static int getVersionStringLength(String userAgent,
            int startIndex) {
        final String versionSubString = userAgent.substring(startIndex);
        int versionBreak = versionSubString.indexOf(" ");
        if (versionBreak == -1) {
            versionBreak = versionSubString.length();
        }
        return versionBreak;
    }

    private void parseAndroidVersion(String userAgent) {
        // Android 5.1;
        if (!userAgent.contains("android ")) {
            return;
        }

        if (userAgent.contains("ddg_android/")) {
            int startIndex = userAgent.indexOf("ddg_android/");
            String osVersionString = safeSubstring(userAgent,
                    startIndex + "ddg_android/".length(),
                    userAgent.indexOf(' ', startIndex));
            String[] parts = osVersionString.split("\\.");
            parseOsVersion(parts, userAgent);
            return;
        }

        String osVersionString = safeSubstring(userAgent,
                userAgent.indexOf("android ") + "android ".length(),
                userAgent.length());
        int semicolonIndex = osVersionString.indexOf(";");
        int bracketIndex = osVersionString.indexOf(")");
        int endIndex = semicolonIndex != -1 && semicolonIndex < bracketIndex
                ? semicolonIndex
                : bracketIndex;
        osVersionString = safeSubstring(osVersionString, 0, endIndex);
        String[] parts = osVersionString.split("\\.");
        parseOsVersion(parts, userAgent);
    }

    private void parseIOSVersion(String userAgent) {
        // OS 5_1 like Mac OS X
        if (!userAgent.contains("os ") || !userAgent.contains(" like mac")) {
            return;
        }

        String osVersionString = safeSubstring(userAgent,
                userAgent.indexOf("os ") + 3, userAgent.indexOf(" like mac"));
        String[] parts = osVersionString.split("_");
        parseOsVersion(parts, userAgent);
    }

    private void parseOsVersion(String[] parts, String userAgent) {
        osMajorVersion = -1;
        osMinorVersion = -1;

        if (parts.length >= 1) {
            osMajorVersion = parseVersionPart(parts[0], OS_MAJOR, userAgent);
        }
        if (parts.length >= 2) {
            // Some Androids report version numbers as "2.1-update1"
            int dashIndex = parts[1].indexOf('-');
            if (dashIndex > -1) {
                String dashlessVersion = parts[1].substring(0, dashIndex);
                osMinorVersion = parseVersionPart(dashlessVersion, OS_MINOR,
                        userAgent);
            } else {
                osMinorVersion = parseVersionPart(parts[1], OS_MINOR,
                        userAgent);
            }
        }
    }

    private void parseVersionString(String versionString, String userAgent) {
        int idx = versionString.indexOf('.');
        if (idx < 0) {
            idx = versionString.length();
        }
        String majorVersionPart = safeSubstring(versionString, 0, idx);
        browserMajorVersion = parseVersionPart(majorVersionPart, BROWSER_MAJOR,
                userAgent);

        if (browserMajorVersion == -1) {
            // no need to scan for minor if major version scanning failed.
            return;
        }

        int idx2 = versionString.indexOf('.', idx + 1);
        if (idx2 < 0) {
            // If string only contains major version, set minor to 0.
            if (versionString.substring(idx).length() == 0) {
                browserMinorVersion = 0;
                return;
            }
            idx2 = versionString.length();
        }
        String minorVersionPart = safeSubstring(versionString, idx + 1, idx2)
                .replaceAll("[^0-9].*", "");
        browserMinorVersion = parseVersionPart(minorVersionPart, BROWSER_MINOR,
                userAgent);
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

    private int parseVersionPart(String versionString, String partName,
            String userAgent) {
        try {
            return Integer.parseInt(versionString);
        } catch (Exception e) {
            log(partName + " version parsing failed for: \"" + versionString
                    + "\"\nWith userAgent: " + userAgent, e);
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
     * Tests if the browser is run on iPhone.
     *
     * @return true if run on iPhone, false otherwise
     */
    public boolean isIPhone() {
        return isIPhone;
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
     * Tests if the browser is run on Chrome OS (e.g. a Chromebook).
     *
     * @return true if run on Chrome OS, false otherwise
     */
    public boolean isChromeOS() {
        return isChromeOS;
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
        // IE is not supported
        if (isIE()) {
            return true;
        }
        // Only ChromeEdge is supported
        if (isEdge() && getBrowserMajorVersion() < 79) {
            return true;
        }
        // Safari 14+
        if (isSafari() && getBrowserMajorVersion() < 14) {
            if (isIPhone() && (getOperatingSystemMajorVersion() > 14
                    || (getOperatingSystemMajorVersion() == 14
                            && getOperatingSystemMinorVersion() >= 7))) {
                // #11654
                return false;
            }
            return true;
        }
        // Firefox 78+ for now
        if (isFirefox() && getBrowserMajorVersion() < 78) {
            return true;
        }
        // Opera 58+ for now
        if (isOpera() && getBrowserMajorVersion() < 58) {
            return true;
        }
        // Chrome 71+ for now
        if (isChrome() && getBrowserMajorVersion() < 71) {
            return true;
        }
        return false;
    }

    protected void log(String error, Exception e) {
        // "Logs" to stdout so the problem can be found but does not prevent
        // using the app. As this class is shared, we do not use
        // slf4j for logging as normal.
        System.err.println(error + ' ' + e.getMessage());
    }

}
