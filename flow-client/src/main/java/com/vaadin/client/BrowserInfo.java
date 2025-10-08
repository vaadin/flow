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
package com.vaadin.client;

import com.vaadin.flow.shared.BrowserDetails;

/**
 * Provides a way to query information about web browser.
 *
 * Browser details are detected only once and those are stored in this singleton
 * class.
 *
 * @since 1.0
 */
public class BrowserInfo {

    private static final String BROWSER_OPERA = "op";
    private static final String BROWSER_IE = "ie";
    private static final String BROWSER_EDGE = "edge";
    private static final String BROWSER_FIREFOX = "ff";
    private static final String BROWSER_SAFARI = "sa";

    public static final String ENGINE_GECKO = "gecko";
    public static final String ENGINE_WEBKIT = "webkit";
    public static final String ENGINE_PRESTO = "presto";
    public static final String ENGINE_TRIDENT = "trident";

    private static final String OS_WINDOWS = "win";
    private static final String OS_LINUX = "lin";
    private static final String OS_MACOSX = "mac";
    private static final String OS_ANDROID = "android";

    private static BrowserInfo instance;

    private String cssClass = null;

    private BrowserDetails browserDetails;
    private boolean touchDevice;

    private BrowserInfo() {
        browserDetails = new BrowserDetails(getBrowserString());

        touchDevice = checkForTouchDevice();
    }

    /**
     * Singleton method to get BrowserInfo object.
     *
     * @return instance of BrowserInfo object
     */
    public static BrowserInfo get() {
        if (instance == null) {
            instance = new BrowserInfo();
        }
        return instance;
    }

    private native boolean checkForTouchDevice()
    /*-{
        if (navigator && "maxTouchPoints" in navigator) {
            return navigator.maxTouchPoints > 0;
        } else if (navigator && "msMaxTouchPoints" in navigator) {
            return navigator.msMaxTouchPoints > 0;
        } else {
            var mQ = $wnd.matchMedia && matchMedia("(pointer:coarse)");
            if (mQ && mQ.media === "(pointer:coarse)") {
                return !!mQ.matches;
            }
        }
        try {
            $doc.createEvent("TouchEvent");
            return true;
        } catch(e){
            return false;
        }
    }-*/;

    /**
     * Checks if the browser is IE.
     *
     * @return true if the browser is IE, false otherwise
     * @deprecated use a parsing library like ua-parser-js to parse the user
     *             agent from {@link #getBrowserString()}
     */
    @Deprecated
    public boolean isIE() {
        return browserDetails.isIE();
    }

    /**
     * Checks if the browser is Edge.
     *
     * @return true if the browser is Edge, false otherwise
     * @deprecated use a parsing library like ua-parser-js to parse the user
     *             agent from {@link #getBrowserString()}
     */
    @Deprecated
    public boolean isEdge() {
        return browserDetails.isEdge();
    }

    /**
     * Checks if the browser is Firefox.
     *
     * @return true if the browser is Firefox, false otherwise
     * @deprecated use a parsing library like ua-parser-js to parse the user
     *             agent from {@link #getBrowserString()}
     */
    @Deprecated
    public boolean isFirefox() {
        return browserDetails.isFirefox();
    }

    /**
     * Checks if the browser is Safari.
     *
     * @return true if the browser is Safari, false otherwise
     * @deprecated use a parsing library like ua-parser-js to parse the user
     *             agent from {@link #getBrowserString()}
     */
    @Deprecated
    public boolean isSafari() {
        return browserDetails.isSafari();
    }

    /**
     * Checks if the browser is Safari or runs on IOS (covering also Chrome on
     * iOS).
     *
     * @return true if the browser is Safari or running on IOS, false otherwise
     * @deprecated use a parsing library like ua-parser-js to parse the user
     *             agent from {@link #getBrowserString()}
     */
    @Deprecated
    public boolean isSafariOrIOS() {
        return isSafari() || isIos();
    }

    /**
     * Checks if the browser is Chrome.
     *
     * @return true if the browser is Chrome, false otherwise
     * @deprecated use a parsing library like ua-parser-js to parse the user
     *             agent from {@link #getBrowserString()}
     */
    @Deprecated
    public boolean isChrome() {
        return browserDetails.isChrome();
    }

    /**
     * Checks if the browser using the Gecko engine.
     *
     * @return true if the browser is using Gecko, false otherwise
     * @deprecated use a parsing library like ua-parser-js to parse the user
     *             agent from {@link #getBrowserString()}
     */
    @Deprecated
    public boolean isGecko() {
        return browserDetails.isGecko();
    }

    /**
     * Checks if the browser using the Webkit engine.
     *
     * @return true if the browser is using Webkit, false otherwise
     * @deprecated use a parsing library like ua-parser-js to parse the user
     *             agent from {@link #getBrowserString()}
     */
    @Deprecated
    public boolean isWebkit() {
        return browserDetails.isWebKit();
    }

    /**
     * Returns the Gecko version if the browser is Gecko based. The Gecko
     * version for Firefox 2 is 1.8 and 1.9 for Firefox 3.
     *
     * @return The Gecko version or -1 if the browser is not Gecko based
     * @deprecated use a parsing library like ua-parser-js to parse the user
     *             agent from {@link #getBrowserString()}
     */
    @Deprecated
    public float getGeckoVersion() {
        if (!browserDetails.isGecko()) {
            return -1;
        }

        return browserDetails.getBrowserEngineVersion();
    }

    /**
     * Returns the WebKit version if the browser is WebKit based. The WebKit
     * version returned is the major version e.g., 523.
     *
     * @return The WebKit version or -1 if the browser is not WebKit based
     * @deprecated use a parsing library like ua-parser-js to parse the user
     *             agent from {@link #getBrowserString()}
     */
    @Deprecated
    public float getWebkitVersion() {
        if (!browserDetails.isWebKit()) {
            return -1;
        }

        return browserDetails.getBrowserEngineVersion();
    }

    /**
     * Checks if the browser is Opera.
     *
     * @return true if the browser is Opera, false otherwise
     * @deprecated use a parsing library like ua-parser-js to parse the user
     *             agent from {@link #getBrowserString()}
     */
    @Deprecated
    public boolean isOpera() {
        return browserDetails.isOpera();
    }

    private static native String getBrowserString()
    /*-{
        return $wnd.navigator.userAgent;
    }-*/;

    private static native boolean isIos()
    /*-{
        return (/iPad|iPhone|iPod/.test(navigator.platform) ||
            (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1));
    }-*/;

    /**
     * Checks if the browser runs on a touch capable device.
     *
     * @return true if the browser runs on a touch based device, false otherwise
     */
    public boolean isTouchDevice() {
        return touchDevice;
    }

    /**
     * Checks if the browser is run on Android.
     *
     * @return true if the browser is run on Android, false otherwise
     * @deprecated use a parsing library like ua-parser-js to parse the user
     *             agent from {@link #getBrowserString()}
     */
    @Deprecated
    public boolean isAndroid() {
        return browserDetails.isAndroid();
    }

    /**
     * Tests if this is an Android devices with a broken scrollTop
     * implementation.
     *
     * @return true if scrollTop cannot be trusted on this device, false
     *         otherwise
     * @deprecated use a parsing library like ua-parser-js to parse the user
     *             agent from {@link #getBrowserString()} and check version
     *             against known issues.
     */
    @Deprecated
    public boolean isAndroidWithBrokenScrollTop() {
        return isAndroid() && (getOperatingSystemMajorVersion() == 3
                || getOperatingSystemMajorVersion() == 4);
    }

    private int getOperatingSystemMajorVersion() {
        return browserDetails.getOperatingSystemMajorVersion();
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
     * @deprecated use a parsing library like ua-parser-js to parse the user
     *             agent from {@link #getBrowserString()}
     */
    @Deprecated
    public int getBrowserMajorVersion() {
        return browserDetails.getBrowserMajorVersion();
    }

    /**
     * Returns the browser minor version e.g., 5 for Firefox 3.5.
     *
     * @see #getBrowserMajorVersion()
     *
     * @return The minor version of the browser, or -1 if not known/parsed.
     * @deprecated use a parsing library like ua-parser-js to parse the user
     *             agent from {@link #getBrowserString()}
     */
    @Deprecated
    public int getBrowserMinorVersion() {
        return browserDetails.getBrowserMinorVersion();
    }

}
