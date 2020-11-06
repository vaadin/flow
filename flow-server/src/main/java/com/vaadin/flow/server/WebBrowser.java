/*
 * Copyright 2000-2020 Vaadin Ltd.
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

package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.Locale;

import com.vaadin.flow.shared.BrowserDetails;

/**
 * Provides information about the web browser the user is using that is directly
 * available in the request, for instance browser name and version and IP
 * address.
 *
 * Note! browser details rely on the user agent from the browser and thus the
 * details are not always correct.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class WebBrowser implements Serializable {

    private String browserApplication = null;
    private Locale locale = null;
    private String address = null;
    private boolean secureConnection = false;

    private BrowserDetails browserDetails = null;

    /**
     * For internal use only. Configures all properties for the initial empty state.
     */
    WebBrowser() {}

    /**
     * For internal use only. Configures all properties in the class according to
     * the given information.
     *
     * @param request
     *            the Vaadin request to read the information from
     */
    WebBrowser(VaadinRequest request) {
        locale = request.getLocale();
        address = request.getRemoteAddr();
        secureConnection = request.isSecure();
        // Headers are case insensitive according to the specification but are
        // case sensitive in Weblogic portal...
        String agent = request.getHeader("User-Agent");

        if (agent != null) {
            browserApplication = agent;
            browserDetails = new BrowserDetails(agent);
        }
    }

    /**
     * Get the browser user-agent string.
     *
     * @return The raw browser userAgent string
     */
    public String getBrowserApplication() {
        return browserApplication;
    }

    /**
     * Gets the IP-address of the web browser, if available.
     *
     * @return IP-address in 1.12.123.123 -format or null if the address is not
     *         available
     */
    public String getAddress() {
        return address;
    }

    /**
     * Gets the locale reported by the browser.
     *
     * @return the browser reported locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Checks if the connection was established using HTTPS.
     *
     * @return true if HTTPS was used, false otherwise
     */
    public boolean isSecureConnection() {
        return secureConnection;
    }

    /**
     * Tests whether the user is using Firefox.
     *
     * @return true if the user is using Firefox, false if the user is not using
     *         Firefox or if no information on the browser is present
     */
    public boolean isFirefox() {
        if (browserDetails == null) {
            return false;
        }

        return browserDetails.isFirefox();
    }

    /**
     * Tests whether the user is using Internet Explorer.
     *
     * @return true if the user is using Internet Explorer, false if the user is
     *         not using Internet Explorer or if no information on the browser
     *         is present
     */
    public boolean isIE() {
        if (browserDetails == null) {
            return false;
        }

        return browserDetails.isIE();
    }

    /**
     * Tests whether the user is using Edge.
     *
     * @return true if the user is using Edge, false if the user is not using
     *         Edge or if no information on the browser is present
     */
    public boolean isEdge() {
        if (browserDetails == null) {
            return false;
        }

        return browserDetails.isEdge();
    }

    /**
     * Tests whether the user is using Safari. Note that Chrome on iOS is not
     * detected as Safari but as Chrome although the underlying browser engine
     * is the same.
     *
     * @return true if the user is using Safari, false if the user is not using
     *         Safari or if no information on the browser is present
     */
    public boolean isSafari() {
        if (browserDetails == null) {
            return false;
        }

        return browserDetails.isSafari();
    }

    /**
     * Tests whether the user is using Opera.
     *
     * @return true if the user is using Opera, false if the user is not using
     *         Opera or if no information on the browser is present
     */
    public boolean isOpera() {
        if (browserDetails == null) {
            return false;
        }

        return browserDetails.isOpera();
    }

    /**
     * Tests whether the user is using Chrome.
     *
     * @return true if the user is using Chrome, false if the user is not using
     *         Chrome or if no information on the browser is present
     */
    public boolean isChrome() {
        if (browserDetails == null) {
            return false;
        }

        return browserDetails.isChrome();
    }

    /**
     * Gets the major version of the browser the user is using.
     *
     * <p>
     * Note that Internet Explorer in IE7 compatibility mode might return 8 in
     * some cases even though it should return 7.
     * </p>
     *
     * @return The major version of the browser or -1 if not known.
     */
    public int getBrowserMajorVersion() {
        if (browserDetails == null) {
            return -1;
        }

        return browserDetails.getBrowserMajorVersion();
    }

    /**
     * Gets the minor version of the browser the user is using.
     *
     * @see #getBrowserMajorVersion()
     *
     * @return The minor version of the browser or -1 if not known.
     */
    public int getBrowserMinorVersion() {
        if (browserDetails == null) {
            return -1;
        }

        return browserDetails.getBrowserMinorVersion();
    }

    /**
     * Tests whether the user is using Linux.
     *
     * @return true if the user is using Linux, false if the user is not using
     *         Linux or if no information on the browser is present
     */
    public boolean isLinux() {
        if (browserDetails == null) {
            return false;
        }
        return browserDetails.isLinux();
    }

    /**
     * Tests whether the user is using Mac OS X.
     *
     * @return true if the user is using Mac OS X, false if the user is not
     *         using Mac OS X or if no information on the browser is present
     */
    public boolean isMacOSX() {
        if (browserDetails == null) {
            return false;
        }
        return browserDetails.isMacOSX();
    }

    /**
     * Tests whether the user is using Windows.
     *
     * @return true if the user is using Windows, false if the user is not using
     *         Windows or if no information on the browser is present
     */
    public boolean isWindows() {
        if (browserDetails == null) {
            return false;
        }
        return browserDetails.isWindows();
    }

    /**
     * Tests whether the user is using Windows Phone.
     *
     * @return true if the user is using Windows Phone, false if the user is not
     *         using Windows Phone or if no information on the browser is
     *         present
     */
    public boolean isWindowsPhone() {
        if (browserDetails == null) {
            return false;
        }
        return browserDetails.isWindowsPhone();
    }

    /**
     * Tests if the browser is run on Android.
     *
     * @return true if run on Android false if the user is not using Android or
     *         if no information on the browser is present
     */
    public boolean isAndroid() {
        if (browserDetails == null) {
            return false;
        }
        return browserDetails.isAndroid();
    }

    /**
     * Tests if the browser is run on IPhone.
     *
     * @return true if run on IPhone false if the user is not using IPhone or if
     *         no information on the browser is present
     */
    public boolean isIPhone() {
        if (browserDetails == null) {
            return false;
        }
        return browserDetails.isIPhone();
    }

    /**
     * Tests if the browser is run on ChromeOS (e.g. a Chromebook).
     *
     * @return true if run on ChromeOS false if the user is not using ChromeOS
     *         or if no information on the browser is present
     */
    public boolean isChromeOS() {
        if (browserDetails == null) {
            return false;
        }
        return browserDetails.isChromeOS();
    }

    /**
     * Checks if the browser is so old that it simply won't work with a Vaadin
     * application. Can be used to redirect to an alternative page, show
     * alternative content or similar.
     *
     * When this method returns true chances are very high that the browser
     * won't work and it does not make sense to direct the user to the Vaadin
     * application.
     *
     * @return true if the browser won't work, false if not the browser is
     *         supported or might work
     */
    public boolean isTooOldToFunctionProperly() {
        if (browserDetails == null) {
            // Don't know, so assume it will work
            return false;
        }

        return browserDetails.isTooOldToFunctionProperly();
    }

}
