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
