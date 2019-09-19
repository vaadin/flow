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
    private static final String OS_IOS = "ios";

    // Common CSS class for all touch devices
    private static final String UI_TOUCH = "touch";

    private static BrowserInfo instance;

    private String cssClass = null;

    private BrowserDetails browserDetails;
    private boolean touchDevice;

    private BrowserInfo() {
        browserDetails = new BrowserDetails(getBrowserString());

        if (browserDetails.isChrome()) {
            touchDevice = detectChromeTouchDevice();
        } else if (browserDetails.isIE()) {
            touchDevice = detectIETouchDevice();
        } else {
            // tests
            touchDevice = detectTouchDevice();
        }
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

    private native boolean detectTouchDevice()
    /*-{
        try { document.createEvent("TouchEvent");return true;} catch(e){return false;};
    }-*/;

    private native boolean detectChromeTouchDevice()
    /*-{
        return ("ontouchstart" in window);
    }-*/;

    private native boolean detectIETouchDevice()
    /*-{
        return !!navigator.msMaxTouchPoints;
    }-*/;

    /**
     * Checks if the browser is IE.
     *
     * @return true if the browser is IE, false otherwise
     */
    public boolean isIE() {
        return browserDetails.isIE();
    }

    /**
     * Checks if the browser is Edge.
     *
     * @return true if the browser is Edge, false otherwise
     */
    public boolean isEdge() {
        return browserDetails.isEdge();
    }

    /**
     * Checks if the browser is Firefox.
     *
     * @return true if the browser is Firefox, false otherwise
     */
    public boolean isFirefox() {
        return browserDetails.isFirefox();
    }

    /**
     * Checks if the browser is Safari.
     *
     * @return true if the browser is Safari, false otherwise
     */
    public boolean isSafari() {
        return browserDetails.isSafari();
    }

    /**
     * Checks if the browser is Safari or runs on IOS (covering also Chrome on
     * iOS).
     *
     * @return true if the browser is Safari or running on IOS, false otherwise
     */
    public boolean isSafariOrIOS() {
        return browserDetails.isSafariOrIOS();
    }

    /**
     * Checks if the browser is Chrome.
     *
     * @return true if the browser is Chrome, false otherwise
     */
    public boolean isChrome() {
        return browserDetails.isChrome();
    }

    /**
     * Checks if the browser using the Gecko engine.
     *
     * @return true if the browser is using Gecko, false otherwise
     */
    public boolean isGecko() {
        return browserDetails.isGecko();
    }

    /**
     * Checks if the browser using the Webkit engine.
     *
     * @return true if the browser is using Webkit, false otherwise
     */
    public boolean isWebkit() {
        return browserDetails.isWebKit();
    }

    /**
     * Returns the Gecko version if the browser is Gecko based. The Gecko
     * version for Firefox 2 is 1.8 and 1.9 for Firefox 3.
     *
     * @return The Gecko version or -1 if the browser is not Gecko based
     */
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
     */
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
     */
    public boolean isOpera() {
        return browserDetails.isOpera();
    }

    private static native String getBrowserString()
    /*-{
        return $wnd.navigator.userAgent;
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
     * Checks if the browser is run on iOS.
     *
     * @return true if the browser is run on iOS, false otherwise
     */
    public boolean isIOS() {
        return browserDetails.isIOS();
    }

    /**
     * Checks if the browser is run on Android.
     *
     * @return true if the browser is run on Android, false otherwise
     */
    public boolean isAndroid() {
        return browserDetails.isAndroid();
    }

    /**
     * Tests if this is an Android devices with a broken scrollTop
     * implementation.
     *
     * @return true if scrollTop cannot be trusted on this device, false
     *         otherwise
     */
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
     */
    public int getBrowserMajorVersion() {
        return browserDetails.getBrowserMajorVersion();
    }

    /**
     * Returns the browser minor version e.g., 5 for Firefox 3.5.
     *
     * @see #getBrowserMajorVersion()
     *
     * @return The minor version of the browser, or -1 if not known/parsed.
     */
    public int getBrowserMinorVersion() {
        return browserDetails.getBrowserMinorVersion();
    }

    /**
     * Checks if the browser supports ECMAScript 6, based on the user agent.
     *
     * @see com.vaadin.flow.shared.BrowserDetails#isEs6Supported()
     *
     * @return <code>true</code> if the browser supports ES6, <code>false</code>
     *         otherwise.
     */
    public boolean isEs6Supported() {
        return browserDetails.isEs6Supported();
    }

}
