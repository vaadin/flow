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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtendedClientDetailsTest {

    @AfterEach
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Test
    public void initializeWithClientValues_gettersReturnExpectedValues() {
        final ExtendedClientDetails details = new ExtendBuilder()
                .buildDetails();

        assertEquals(2560, details.getScreenWidth());
        assertEquals(1450, details.getScreenHeight());
        assertEquals(2400, details.getWindowInnerWidth());
        assertEquals(1400, details.getWindowInnerHeight());
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
    public void differentNavigatorPlatformDetails_isIPadReturnsExpectedValue() {
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
    public void differentNavigatorPlatformDetails_Ipod_isIOSReturnsExpectedValue() {
        ExtendedClientDetails details = new ExtendBuilder()
                .setNavigatorPlatform("iPod ..").buildDetails();

        VaadinSession session = Mockito.mock(VaadinSession.class);
        CurrentInstance.setCurrent(session);
        WebBrowser browser = Mockito.mock(WebBrowser.class);
        Mockito.when(session.getBrowser()).thenReturn(browser);
        Mockito.when(browser.isIPhone()).thenReturn(false);

        assertTrue(details.isIOS());

        CurrentInstance.clearAll();
    }

    @Test
    public void isIOS_isIPad_returnsTrue() {
        ExtendedClientDetails details = Mockito
                .mock(ExtendedClientDetails.class);
        Mockito.doCallRealMethod().when(details).isIOS();
        Mockito.when(details.isIPad()).thenReturn(true);

        assertTrue(details.isIOS());
    }

    @Test
    public void isIOS_notIPadIsIPhone_returnsTrue() {
        ExtendedClientDetails details = Mockito
                .mock(ExtendedClientDetails.class);
        Mockito.doCallRealMethod().when(details).isIOS();

        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinSession.setCurrent(session);

        WebBrowser browser = Mockito.mock(WebBrowser.class);
        Mockito.when(session.getBrowser()).thenReturn(browser);

        Mockito.when(browser.isIPhone()).thenReturn(true);

        assertTrue(details.isIOS());
    }

    @Test
    public void isIOS_notIPad_notIsIPhone_returnsFalse() {
        ExtendedClientDetails details = Mockito
                .mock(ExtendedClientDetails.class);
        Mockito.doCallRealMethod().when(details).isIOS();

        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinSession.setCurrent(session);

        WebBrowser browser = Mockito.mock(WebBrowser.class);
        Mockito.when(session.getBrowser()).thenReturn(browser);
        Mockito.when(browser.isIPhone()).thenReturn(false);

        assertFalse(details.isIOS());
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
