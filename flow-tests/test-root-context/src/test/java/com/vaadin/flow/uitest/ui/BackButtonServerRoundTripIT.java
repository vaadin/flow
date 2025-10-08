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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BackButtonServerRoundTripIT extends ChromeBrowserTest {
    @Test
    public void testForwardingToViewInSetParameter() {
        final String baseLoc = "/view/com.vaadin.flow.uitest.ui.BackButtonServerRoundTripView";
        getDriver().get(getRootURL() + baseLoc + "/1?query=foo");
        waitForDevServer();

        WebElement button = findElement(
                By.id(BackButtonServerRoundTripView.BUTTON_ID));
        button.click();

        final String queryValue0 = findElement(
                By.id(BackButtonServerRoundTripView.QUERY_LABEL_ID)).getText();
        Assert.assertTrue("should have received query parameter value 'bar'",
                queryValue0.equals("query=bar"));

        getDriver().navigate().back();

        final String queryValue1 = findElement(
                By.id(BackButtonServerRoundTripView.QUERY_LABEL_ID)).getText();
        Assert.assertTrue("should have received query parameter value 'foo'",
                queryValue1.equals("query=foo"));
    }
}
