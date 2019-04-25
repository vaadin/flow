package com.vaadin.flow.component.page;

import java.io.Serializable;
import java.util.Date;
import java.util.TimeZone;

/**
 * Provides extended information about the web browser, such as screen
 * resolution and time zone.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ExtendedClientDetails implements Serializable {
    private int screenHeight = -1;
    private int screenWidth = -1;
    private int timezoneOffset = 0;
    private int rawTimezoneOffset = 0;
    private int dstSavings;
    private boolean dstInEffect;
    private String timeZoneId;
    private boolean touchDevice;
    private long clientServerTimeDelta;
    private String windowName;

    /**
     * For internal use only. Updates all properties in the class according to
     * the given information.
     *
     * @param sw
     *            Screen width
     * @param sh
     *            Screen height
     * @param tzo
     *            TimeZone offset in minutes from GMT
     * @param rtzo
     *            raw TimeZone offset in minutes from GMT (w/o DST adjustment)
     * @param dstSavings
     *            the difference between the raw TimeZone and DST in minutes
     * @param dstInEffect
     *            is DST currently active in the region or not?
     * @param tzId
     *            time zone id
     * @param curDate
     *            the current date in milliseconds since the epoch
     * @param touchDevice
     *            whether browser responds to touch events
     * @param windowName
     *            a unique browser window name which persists on reload
     */
    ExtendedClientDetails(String sw, String sh, String tzo, String rtzo,
                          String dstSavings, String dstInEffect, String tzId,
                          String curDate, String touchDevice, String windowName) {
        if (sw != null) {
            try {
                screenWidth = Integer.parseInt(sw);
                screenHeight = Integer.parseInt(sh);
            } catch (final NumberFormatException e) {
                screenHeight = screenWidth = -1;
            }
        }
        if (tzo != null) {
            try {
                // browser->java conversion: min->ms, reverse sign
                timezoneOffset = -Integer.parseInt(tzo) * 60 * 1000;
            } catch (final NumberFormatException e) {
                timezoneOffset = 0; // default gmt+0
            }
        }
        if (rtzo != null) {
            try {
                // browser->java conversion: min->ms, reverse sign
                rawTimezoneOffset = -Integer.parseInt(rtzo) * 60 * 1000;
            } catch (final NumberFormatException e) {
                rawTimezoneOffset = 0; // default gmt+0
            }
        }
        if (dstSavings != null) {
            try {
                // browser->java conversion: min->ms
                this.dstSavings = Integer.parseInt(dstSavings) * 60 * 1000;
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

        this.windowName = windowName;
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
     * </p>
     * <p>
     * If {@link #getRawTimezoneOffset()} and {@link #getTimezoneOffset()}
     * returns the same value, the browser is either in a zone that does not
     * currently have daylight saving time, or in a zone that never has daylight
     * saving time.
     * </p>
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
     * Returns a unique browser window identifier. For internal use only.
     *
     * @return An id which persists if the UI is reloaded in the same browser
     * window/tab.
     */
    public String getWindowName() {
        return windowName;
    }

}
