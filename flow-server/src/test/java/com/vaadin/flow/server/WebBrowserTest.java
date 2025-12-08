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
package com.vaadin.flow.server;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class WebBrowserTest {

    private WebBrowser browser = new WebBrowser();

    @Test
    public void isLinux_noDetails_returnsFalse() {
        Assert.assertFalse(browser.isLinux());
    }

    @Test
    public void isMacOSX_noDetails_returnsFalse() {
        Assert.assertFalse(browser.isMacOSX());
    }

    @Test
    public void isWindows_noDetails_returnsFalse() {
        Assert.assertFalse(browser.isWindows());
    }

    @Test
    public void isWindowsPhone_noDetails_returnsFalse() {
        Assert.assertFalse(browser.isWindowsPhone());
    }

    @Test
    public void isAndroid_noDetails_returnsFalse() {
        Assert.assertFalse(browser.isAndroid());
    }

    @Test
    public void isIPhone_noDetails_returnsFalse() {
        Assert.assertFalse(browser.isIPhone());
    }

    @Test
    public void isChromeOS_noDetails_returnsFalse() {
        Assert.assertFalse(browser.isChromeOS());
    }

    @Test
    public void isSafariOnMac_userDetails_returnsTrue() {
        VaadinRequest request = initRequest(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_6_2) AppleWebKit/611.3.10.1.5 (KHTML, like Gecko) Version/14.1.2 Safari/611.3.10.1.5");

        browser = new WebBrowser(request);
        Assert.assertTrue(browser.isSafari());
        Assert.assertTrue(browser.isMacOSX());
    }

    @Test
    public void isChromeOnWindows_userDetails_returnsTrue() {
        VaadinRequest request = initRequest(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");

        browser = new WebBrowser(request);
        Assert.assertTrue(browser.isChrome());
        Assert.assertTrue(browser.isWindows());
    }

    @Test
    public void isOperaOnWindows_userDetails_returnsTrue() {
        VaadinRequest request = initRequest(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 OPR/115.0.0.0");

        browser = new WebBrowser(request);
        Assert.assertTrue(browser.isOpera());
        Assert.assertTrue(browser.isWindows());
    }

    @Test
    public void isFirefoxOnAndroid_userDetails_returnsTrue() {
        VaadinRequest request = initRequest(
                "Mozilla/5.0 (Android; Tablet; rv:33.0) Gecko/33.0 Firefox/33.0");

        browser = new WebBrowser(request);
        Assert.assertTrue(browser.isFirefox());
        Assert.assertTrue(browser.isAndroid());
    }

    private static VaadinRequest initRequest(String userAgent) {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request.getLocale()).thenReturn(Locale.ENGLISH);
        Mockito.when(request.getRemoteAddr()).thenReturn("0.0.0.0");
        Mockito.when(request.isSecure()).thenReturn(false);
        Mockito.when(request.getHeader("User-Agent")).thenReturn(userAgent);
        return request;
    }
}
