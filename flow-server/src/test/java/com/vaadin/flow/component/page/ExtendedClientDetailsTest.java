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
package com.vaadin.flow.component.page;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtendedClientDetailsTest {

    /**
     * Slack for the time that passes between constructing the details and
     * reading the time back.
     */
    private static final long TIME_TOLERANCE_MS = 1000L;

    @Test
    void initializeWithClientValues_gettersReturnExpectedValues() {
        final ExtendedClientDetails details = new ExtendBuilder()
                .buildDetails();

        assertEquals(2560, details.getScreenWidth());
        assertEquals(1450, details.getScreenHeight());
        assertEquals(-1, details.getWindowInnerWidth());
        assertEquals(-1, details.getWindowInnerHeight());
        assertEquals(1600, details.getBodyClientWidth());
        assertEquals(1360, details.getBodyClientHeight());
        assertEquals(16200000, details.getTimezoneOffset());
        assertEquals("Asia/Tehran", details.getTimeZoneId());
        assertEquals(12600000, details.getRawTimezoneOffset());
        assertEquals(3600000, details.getDSTSavings());
        assertEquals(true, details.isDSTInEffect());
        assertEquals(false, details.isTouchDevice());
        assertEquals(2.0D, details.getDevicePixelRatio(), 0.0);
        assertEquals("ROOT-1234567-0.1234567", details.getWindowName());
        assertFalse(details.isIPad());
        assertEquals(ColorScheme.Value.LIGHT, details.getColorScheme());
        assertEquals("aura", details.getThemeName());

        // Don't test getCurrentDate() and time delta due to the dependency on
        // server-side time
    }

    @Test
    void differentNavigatorPlatformDetails_isIPadReturnsExpectedValue() {
        ExtendBuilder detailsBuilder = new ExtendBuilder();

        ExtendedClientDetails details = detailsBuilder.buildDetails();
        assertFalse(details.isIPad(), "Linux is not an iPad");

        detailsBuilder.setNavigatorPlatform("iPad");
        details = detailsBuilder.buildDetails();

        assertTrue(details.isIPad(), "'iPad' is an iPad");

        // See https://github.com/vaadin/flow/issues/14517
        detailsBuilder.setNavigatorPlatform("MacIntel");
        details = detailsBuilder.buildDetails();
        assertFalse(details.isIPad(),
                "MacIntel on non touch device is not an iPad");

        // See https://github.com/vaadin/flow/issues/14517
        detailsBuilder.setTouchDevice("true");
        details = detailsBuilder.buildDetails();
        assertTrue(details.isIPad(), "MacIntel on touch device is an iPad");
    }

    @Test
    void differentNavigatorPlatformDetails_isIOSReturnsExpectedValue() {
        ExtendBuilder detailsBuilder = new ExtendBuilder();

        assertFalse(detailsBuilder.buildDetails().isIOS(), "Linux is not iOS");

        detailsBuilder.setNavigatorPlatform("iPhone");
        assertTrue(detailsBuilder.buildDetails().isIOS(), "'iPhone' is iOS");

        detailsBuilder.setNavigatorPlatform("iPod touch");
        assertTrue(detailsBuilder.buildDetails().isIOS(),
                "'iPod touch' is iOS");

        detailsBuilder.setNavigatorPlatform("iPad");
        assertTrue(detailsBuilder.buildDetails().isIOS(), "an iPad is iOS");
    }

    @Test
    void clientClockAheadOfServer_getBrowserTimeReturnsClientTime() {
        long clientTime = System.currentTimeMillis() + 60_000;
        final ExtendedClientDetails details = new ExtendBuilder()
                .setClientServerTimeDelta(Long.toString(clientTime))
                .buildDetails();

        assertEquals(clientTime, details.getBrowserTime().toEpochMilli(),
                TIME_TOLERANCE_MS,
                "getBrowserTime() should follow the clock of the browser");
        assertEquals(details.getBrowserTime().toEpochMilli(),
                details.getCurrentDate().getTime(), TIME_TOLERANCE_MS,
                "the deprecated getCurrentDate() should agree with getBrowserTime()");
    }

    @Test
    void getNavigatorPlatform_returnsValueReportedByBrowser() {
        ExtendBuilder detailsBuilder = new ExtendBuilder();

        assertEquals("Linux i686",
                detailsBuilder.buildDetails().getNavigatorPlatform());

        detailsBuilder.setNavigatorPlatform(null);
        assertNull(detailsBuilder.buildDetails().getNavigatorPlatform());
    }

    /**
     * Builder to create modified extended details. Default values apply.
     */
    private class ExtendBuilder {
        private String screenWidth = "2560";
        private String screenHeight = "1450";
        private String windowInnerWidth = "2400";
        private String windowInnerHeight = "1400";
        private String bodyClientWidth = "1600";
        private String bodyClientHeight = "1360";
        private String timezoneOffset = "-270"; // minutes from UTC
        private String rawTimezoneOffset = "-210"; // minutes from UTC without
        // DST
        private String dstSavings = "60"; // dist shift amount
        private String dstInEffect = "true";
        private String timeZoneId = "Asia/Tehran";
        private String clientServerTimeDelta = "1555000000000"; // Apr 11 2019
        private String touchDevice = "false";
        private String devicePixelRatio = "2.0";
        private String windowName = "ROOT-1234567-0.1234567";
        private String navigatorPlatform = "Linux i686";
        private String colorScheme = "light";
        private String themeName = "aura";

        public ExtendedClientDetails buildDetails() {
            return new ExtendedClientDetails(null, screenWidth, screenHeight,
                    windowInnerWidth, windowInnerHeight, bodyClientWidth,
                    bodyClientHeight, timezoneOffset, rawTimezoneOffset,
                    dstSavings, dstInEffect, timeZoneId, clientServerTimeDelta,
                    touchDevice, devicePixelRatio, windowName,
                    navigatorPlatform, colorScheme, themeName);
        }

        public ExtendBuilder setScreenWidth(String screenWidth) {
            this.screenWidth = screenWidth;
            return this;
        }

        public ExtendBuilder setScreenHeight(String screenHeight) {
            this.screenHeight = screenHeight;
            return this;
        }

        public ExtendBuilder setWindowInnerWidth(String windowInnerWidth) {
            this.windowInnerWidth = windowInnerWidth;
            return this;
        }

        public ExtendBuilder setWindowInnerHeight(String windowInnerHeight) {
            this.windowInnerHeight = windowInnerHeight;
            return this;
        }

        public ExtendBuilder setBodyClientWidth(String bodyClientWidth) {
            this.bodyClientWidth = bodyClientWidth;
            return this;
        }

        public ExtendBuilder setBodyClientHeight(String bodyClientHeight) {
            this.bodyClientHeight = bodyClientHeight;
            return this;
        }

        public ExtendBuilder setTimezoneOffset(String timezoneOffset) {
            this.timezoneOffset = timezoneOffset;
            return this;
        }

        public ExtendBuilder setRawTimezoneOffset(String rawTimezoneOffset) {
            this.rawTimezoneOffset = rawTimezoneOffset;
            return this;
        }

        public ExtendBuilder setDstSavings(String dstSavings) {
            this.dstSavings = dstSavings;
            return this;
        }

        public ExtendBuilder setDstInEffect(String dstInEffect) {
            this.dstInEffect = dstInEffect;
            return this;
        }

        public ExtendBuilder setTimeZoneId(String timeZoneId) {
            this.timeZoneId = timeZoneId;
            return this;
        }

        public ExtendBuilder setClientServerTimeDelta(
                String clientServerTimeDelta) {
            this.clientServerTimeDelta = clientServerTimeDelta;
            return this;
        }

        public ExtendBuilder setTouchDevice(String touchDevice) {
            this.touchDevice = touchDevice;
            return this;
        }

        public ExtendBuilder setDevicePixelRatio(String devicePixelRatio) {
            this.devicePixelRatio = devicePixelRatio;
            return this;
        }

        public ExtendBuilder setWindowName(String windowName) {
            this.windowName = windowName;
            return this;
        }

        public ExtendBuilder setNavigatorPlatform(String navigatorPlatform) {
            this.navigatorPlatform = navigatorPlatform;
            return this;
        }
    }
}
