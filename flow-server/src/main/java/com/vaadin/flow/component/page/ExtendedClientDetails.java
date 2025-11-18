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
package com.vaadin.flow.component.page;

import java.io.Serializable;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Function;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.server.VaadinSession;

/**
 * Provides extended information about the web browser, such as screen
 * resolution and time zone.
 * <p>
 * Browser details are automatically fetched on the first call to
 * {@link Page#getExtendedClientDetails()} and cached for the lifetime of the
 * UI. The fetch happens asynchronously, so the first call may return
 * {@code null} while the data is being retrieved. To update the cached values
 * with fresh data from the browser, use {@link #refresh(Consumer)}.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class ExtendedClientDetails implements Serializable {
    private final UI ui;
    private int screenWidth = -1;
    private int screenHeight = -1;
    private int windowInnerWidth = -1;
    private int windowInnerHeight = -1;
    private int bodyClientWidth = -1;
    private int bodyClientHeight = -1;
    private int timezoneOffset = 0;
    private int rawTimezoneOffset = 0;
    private int dstSavings;
    private boolean dstInEffect;
    private String timeZoneId;
    private long clientServerTimeDelta;
    private boolean touchDevice;
    private double devicePixelRatio = -1.0D;
    private String windowName;
    private String navigatorPlatform;

    /**
     * For internal use only. Updates all properties in the class according to
     * the given information.
     *
     * @param ui
     *            the UI instance that owns this ExtendedClientDetails
     * @param screenWidth
     *            Screen width
     * @param screenHeight
     *            Screen height
     * @param windowInnerWidth
     *            Window width
     * @param windowInnerHeight
     *            Window height
     * @param bodyClientWidth
     *            Body element width
     * @param bodyClientHeight
     *            Body element height
     * @param tzOffset
     *            TimeZone offset in minutes from GMT
     * @param rawTzOffset
     *            raw TimeZone offset in minutes from GMT (w/o DST adjustment)
     * @param dstShift
     *            the difference between the raw TimeZone and DST in minutes
     * @param dstInEffect
     *            is DST currently active in the region or not?
     * @param tzId
     *            time zone id
     * @param curDate
     *            the current date in milliseconds since the epoch
     * @param touchDevice
     *            whether browser responds to touch events
     * @param devicePixelRatio
     *            the ratio of the display's resolution in physical pixels to
     *            the resolution in CSS pixels
     * @param windowName
     *            a unique browser window name which persists on reload
     * @param navigatorPlatform
     *            navigation platform received from the browser
     */
    public ExtendedClientDetails(UI ui, String screenWidth, String screenHeight,
            String windowInnerWidth, String windowInnerHeight,
            String bodyClientWidth, String bodyClientHeight, String tzOffset,
            String rawTzOffset, String dstShift, String dstInEffect,
            String tzId, String curDate, String touchDevice,
            String devicePixelRatio, String windowName,
            String navigatorPlatform) {
        this.ui = ui;
        if (screenWidth != null) {
            try {
                this.screenWidth = Integer.parseInt(screenWidth);
                this.screenHeight = Integer.parseInt(screenHeight);
            } catch (final NumberFormatException e) {
                this.screenHeight = this.screenWidth = -1;
            }
        }
        if (bodyClientHeight != null) {
            try {
                this.bodyClientHeight = Integer.parseInt(bodyClientHeight);
                this.bodyClientWidth = Integer.parseInt(bodyClientWidth);
            } catch (final NumberFormatException e) {
                this.bodyClientHeight = this.bodyClientWidth = -1;
            }
        }
        if (windowInnerHeight != null) {
            try {
                this.windowInnerHeight = Integer.parseInt(windowInnerHeight);
                this.windowInnerWidth = Integer.parseInt(windowInnerWidth);
            } catch (final NumberFormatException e) {
                this.windowInnerHeight = this.windowInnerWidth = -1;
            }
        }
        if (tzOffset != null) {
            try {
                // browser->java conversion: min->ms, reverse sign
                timezoneOffset = -Integer.parseInt(tzOffset) * 60 * 1000;
            } catch (final NumberFormatException e) {
                timezoneOffset = 0; // default gmt+0
            }
        }
        if (rawTzOffset != null) {
            try {
                // browser->java conversion: min->ms, reverse sign
                rawTimezoneOffset = -Integer.parseInt(rawTzOffset) * 60 * 1000;
            } catch (final NumberFormatException e) {
                rawTimezoneOffset = 0; // default gmt+0
            }
        }
        if (dstShift != null) {
            try {
                // browser->java conversion: min->ms
                this.dstSavings = Integer.parseInt(dstShift) * 60 * 1000;
            } catch (final NumberFormatException e) {
                this.dstSavings = 0; // default no savings
            }
        }
        if (dstInEffect != null) {
            this.dstInEffect = Boolean.parseBoolean(dstInEffect);
        }

        if (tzId == null || "undefined".equals(tzId)) {
            this.timeZoneId = null;
        } else {
            this.timeZoneId = tzId;
        }

        if (curDate != null) {
            try {
                long curTime = Long.parseLong(curDate);
                clientServerTimeDelta = curTime - new Date().getTime();
            } catch (final NumberFormatException e) {
                clientServerTimeDelta = 0;
            }
        }
        if (touchDevice != null) {
            this.touchDevice = Boolean.parseBoolean(touchDevice);
        }
        if (devicePixelRatio != null) {
            this.devicePixelRatio = Double.parseDouble(devicePixelRatio);
        }

        this.windowName = windowName;
        this.navigatorPlatform = navigatorPlatform;
    }

    /**
     * Gets the width of the screen in pixels. This is the full screen
     * resolution and not the width available for the application.
     *
     * @return the width of the screen in pixels.
     */
    public int getScreenWidth() {
        return screenWidth;
    }

    /**
     * Gets the height of the screen in pixels. This is the full screen
     * resolution and not the height available for the application.
     *
     * @return the height of the screen in pixels.
     */
    public int getScreenHeight() {
        return screenHeight;
    }

    /**
     * Gets the inner height of the browser window {@code window.innerHeight} in
     * pixels. This includes the scrollbar, if it is visible.
     *
     * @return the browser window inner height in pixels
     */
    public int getWindowInnerHeight() {
        return windowInnerHeight;
    }

    /**
     * Gets the inner width of the browser window {@code window.innerWidth} in
     * pixels. This includes the scrollbar, if it is visible.
     *
     * @return the browser window inner width in pixels
     */
    public int getWindowInnerWidth() {
        return windowInnerWidth;
    }

    /**
     * Gets the height of the body element in the document in pixels.
     *
     * @return the height of the body element
     */
    public int getBodyClientHeight() {
        return bodyClientHeight;
    }

    /**
     * Gets the width of the body element in the document in pixels.
     *
     * @return the width of the body element
     */
    public int getBodyClientWidth() {
        return bodyClientWidth;
    }

    /**
     * Returns the browser-reported TimeZone offset in milliseconds from GMT.
     * This includes possible daylight saving adjustments, to figure out which
     * TimeZone the user actually might be in, see
     * {@link #getRawTimezoneOffset()}.
     *
     * @see ExtendedClientDetails#getRawTimezoneOffset()
     * @return timezone offset in milliseconds, 0 if not available
     */
    public int getTimezoneOffset() {
        return timezoneOffset;
    }

    /**
     * Returns the TimeZone Id (like "Europe/Helsinki") provided by the browser
     * (if the browser supports this feature).
     *
     * @return the TimeZone Id if provided by the browser, null otherwise.
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/DateTimeFormat/resolvedOptions">Intl.DateTimeFormat.prototype.resolvedOptions()</a>
     */
    public String getTimeZoneId() {
        return timeZoneId;
    }

    /**
     * Returns the browser-reported TimeZone offset in milliseconds from GMT
     * ignoring possible daylight saving adjustments that may be in effect in
     * the browser.
     * <p>
     * You can use this to figure out which TimeZones the user could actually be
     * in by calling {@link TimeZone#getAvailableIDs(int)}.
     * <p>
     * If {@link #getRawTimezoneOffset()} and {@link #getTimezoneOffset()}
     * returns the same value, the browser is either in a zone that does not
     * currently have daylight saving time, or in a zone that never has daylight
     * saving time.
     *
     * @return timezone offset in milliseconds excluding DST, 0 if not available
     */
    public int getRawTimezoneOffset() {
        return rawTimezoneOffset;
    }

    /**
     * Returns the offset in milliseconds between the browser's GMT TimeZone and
     * DST.
     *
     * @return the number of milliseconds that the TimeZone shifts when DST is
     *         in effect
     */
    public int getDSTSavings() {
        return dstSavings;
    }

    /**
     * Returns whether daylight saving time (DST) is currently in effect in the
     * region of the browser or not.
     *
     * @return true if the browser resides at a location that currently is in
     *         DST
     */
    public boolean isDSTInEffect() {
        return dstInEffect;
    }

    /**
     * Returns the current date and time of the browser. This will not be
     * entirely accurate due to varying network latencies, but should provide a
     * close-enough value for most cases. Also note that the returned Date
     * object uses servers default time zone, not the clients.
     * <p>
     * To get the actual date and time shown in the end users computer, you can
     * do something like:
     *
     * <pre>
     * WebBrowser browser = ...;
     * SimpleTimeZone timeZone = new SimpleTimeZone(browser.getTimezoneOffset(), "Fake client time zone");
     * DateFormat format = DateFormat.getDateTimeInstance();
     * format.setTimeZone(timeZone);
     * myLabel.setValue(format.format(browser.getCurrentDate()));
     * </pre>
     *
     * @return the current date and time of the browser.
     * @see #isDSTInEffect()
     * @see #getDSTSavings()
     * @see #getTimezoneOffset()
     */
    public Date getCurrentDate() {
        return new Date(new Date().getTime() + clientServerTimeDelta);
    }

    /**
     * Checks if the browser supports touch events.
     *
     * @return true if the browser is detected to support touch events, false
     *         otherwise
     */
    public boolean isTouchDevice() {
        return touchDevice;
    }

    /**
     * Gets the device pixel ratio, {@code window.devicePixelRatio}. See more
     * from <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/API/Window/devicePixelRatio">MDN
     * web docs</a>.
     * <p>
     * A value of -1 indicates that the value was not reported by the browser
     * correctly.
     *
     * @return double-precision floating-point value indicating the ratio of the
     *         display's resolution in physical pixels to the resolution in CSS
     *         pixels
     */
    public double getDevicePixelRatio() {
        return devicePixelRatio;
    }

    /**
     * Returns a unique browser window identifier. For internal use only.
     *
     * @return An id which persists if the UI is reloaded in the same browser
     *         window/tab.
     */
    public String getWindowName() {
        return windowName;
    }

    /**
     * Check if the browser is run on IPad.
     *
     * @return true if run on IPad false if the user is not using IPad or if no
     *         information from the browser is present
     */
    public boolean isIPad() {
        return navigatorPlatform != null && (navigatorPlatform
                .startsWith("iPad")
                || (navigatorPlatform.equals("MacIntel") && isTouchDevice()));
    }

    /**
     * Check if the browser is run on IOS.
     *
     * @return {@code true} if run on IOS , {@code false} if the user is not
     *         using IOS or if no information from the browser is present
     */
    public boolean isIOS() {
        return isIPad() || VaadinSession.getCurrent().getBrowser().isIPhone()
                || (navigatorPlatform != null
                        && navigatorPlatform.startsWith("iPod"));
    }

    /**
     * Creates an ExtendedClientDetails instance from browser details JSON
     * object. This is intended for internal use when browser details are
     * provided as JSON (e.g., during UI initialization or refresh).
     * <p>
     * For internal use only.
     *
     * @param ui
     *            the UI instance that owns this ExtendedClientDetails
     * @param json
     *            the JSON object containing browser details parameters
     * @return a new ExtendedClientDetails instance
     * @throws RuntimeException
     *             if the JSON is not a valid object
     */
    public static ExtendedClientDetails fromJson(UI ui, JsonNode json) {
        if (!(json instanceof ObjectNode)) {
            throw new RuntimeException("Expected a JSON object");
        }
        final ObjectNode jsonObj = (ObjectNode) json;

        // Note that JSON returned is a plain string -> string map, the actual
        // parsing of the fields happens in ExtendedClient's constructor. If a
        // field is missing or the wrong type, pass on null for default.
        final Function<String, String> getStringElseNull = key -> {
            final JsonNode jsValue = jsonObj.get(key);
            if (jsValue != null
                    && JsonNodeType.STRING.equals(jsValue.getNodeType())) {
                return jsValue.asString();
            } else {
                return null;
            }
        };

        return new ExtendedClientDetails(ui, getStringElseNull.apply("v-sw"),
                getStringElseNull.apply("v-sh"),
                getStringElseNull.apply("v-ww"),
                getStringElseNull.apply("v-wh"),
                getStringElseNull.apply("v-bw"),
                getStringElseNull.apply("v-bh"),
                getStringElseNull.apply("v-tzo"),
                getStringElseNull.apply("v-rtzo"),
                getStringElseNull.apply("v-dstd"),
                getStringElseNull.apply("v-dston"),
                getStringElseNull.apply("v-tzid"),
                getStringElseNull.apply("v-curdate"),
                getStringElseNull.apply("v-td"),
                getStringElseNull.apply("v-pr"),
                getStringElseNull.apply("v-wn"),
                getStringElseNull.apply("v-np"));
    }

    /**
     * Refreshes the browser details by fetching updated values from the
     * browser. The refresh happens asynchronously. The cached values in this
     * instance will be updated when the browser responds, and then the provided
     * callback will be invoked with the updated details.
     *
     * @param callback
     *            a callback that will be invoked with the updated
     *            ExtendedClientDetails when the refresh is complete
     */
    public void refresh(SerializableConsumer<ExtendedClientDetails> callback) {
        final String js = "return Vaadin.Flow.getBrowserDetailsParameters();";
        final SerializableConsumer<JsonNode> resultHandler = json -> {
            ExtendedClientDetails details = fromJson(ui, json);
            ui.getInternals().setExtendedClientDetails(details);
            if (callback != null) {
                callback.accept(details);
            }
        };
        final SerializableConsumer<String> errorHandler = err -> {
            throw new RuntimeException("Unable to retrieve extended "
                    + "client details. JS error is '" + err + "'");
        };
        ui.getPage().executeJs(js).then(resultHandler, errorHandler);
    }
}
