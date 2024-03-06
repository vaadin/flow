/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import org.junit.Assert;
import org.junit.Test;

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
}
