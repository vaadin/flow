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
package com.vaadin.flow.webcomponent;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.component.webshare.WebShareSupport;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class BrowserDetailsIT extends ChromeBrowserTest implements HasById {

    @Override
    protected String getTestPath() {
        return Constants.PAGE_CONTEXT + "/browserDetails.html";
    }

    @Test
    public void refresh_collectsAllBrowserDetails() {
        open();

        waitForElementVisible(By.id("browser-details"));
        byId("browser-details", "refresh").click();

        TestBenchElement screenWidth = byId("browser-details", "screen-width");
        waitUntil(driver -> !screenWidth.getText().isEmpty());
        Assert.assertEquals(
                String.valueOf(executeScript("return window.screen.width")),
                screenWidth.getText());
        // Only sent by the full browser details collection, so it stays
        // UNKNOWN unless refresh() used it
        Assert.assertNotEquals(WebShareSupport.UNKNOWN.name(),
                byId("browser-details", "web-share-support").getText());
    }
}
