/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.navigation.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BrowserNavigationServerRoundTripIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/first";
    }

    @Test
    public void testForwardingToViewInSetParameter() {
        final String baseLoc = "/first";
        getDriver().get(getRootURL() + baseLoc + "/1?query=foo");
        waitForDevServer();

        WebElement button = findElement(By.id(FirstView.PARAM_NAV_BUTTON_ID));
        button.click();

        final String queryValue0 = findElement(By.id(FirstView.QUERY_LABEL_ID))
                .getText();
        Assert.assertEquals("should have received query parameter value 'bar'",
                "query=bar", queryValue0);

        getDriver().navigate().back();

        final String queryValue1 = findElement(By.id(FirstView.QUERY_LABEL_ID))
                .getText();
        Assert.assertEquals("should have received query parameter value 'foo'",
                "query=foo", queryValue1);
    }

    @Test
    public void backAndForwardBrowserButton_triggerServerSideRoundTrip() {
        open();
        waitForDevServer();

        WebElement button = findElement(By.id(FirstView.NAV_BUTTON_ID));
        button.click();

        waitForElementPresent(By.id(SecondView.BUTTON_ID));

        getDriver().navigate().back();

        waitForElementPresent(By.id(FirstView.NAV_BUTTON_ID));

        getDriver().navigate().forward();

        waitForElementPresent(By.id(SecondView.BUTTON_ID));
    }
}
