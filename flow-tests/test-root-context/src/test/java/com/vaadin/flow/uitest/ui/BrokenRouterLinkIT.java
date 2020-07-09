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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BrokenRouterLinkIT extends ChromeBrowserTest {

    // https://github.com/vaadin/flow/issues/8544
    @Test
    public void testRouterLink_linkIsBroken_urlIsUpdated() {
        // enable after https://github.com/vaadin/vaadin-router/issues/43 is fixed
        Assume.assumeFalse(isClientRouter());

        open();

        WebElement link = findElement(By.id(BrokenRouterLinkView.LINK_ID));

        String href = link.getAttribute("href");

        link.click();

        Assert.assertTrue(getDriver().getCurrentUrl().endsWith(href));
    }

    // https://github.com/vaadin/flow/issues/8693
    @Test
    public void testRouterLink_visitBrokenLinkAndBack_scrollPositionIsRetained() {
        // enable after https://github.com/vaadin/vaadin-router/issues/43 is fixed
        Assume.assumeFalse(isClientRouter());

        open();

        executeScript("window.scrollTo(0,100)");

        WebElement link = findElement(By.id(BrokenRouterLinkView.LINK_ID));
        link.click();

        long y0 = (Long) executeScript("return window.scrollY");
        Assert.assertEquals(0L, y0);

        getDriver().navigate().back();

        long y1 = (Long) executeScript("return window.scrollY");
        Assert.assertEquals(100L, y1);
    }
}
