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

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class SetParameterForwardToIT extends ChromeBrowserTest {

    @Test
    public void testForwardingToViewWithLocation_setParameter() {
        testForwardingToView("location");
    }

    @Test
    public void testForwardingToViewWithRouteParameter_setParameter() {
        testForwardingToView("locationRouteParameter");
    }

    @Test
    public void testForwardingToViewWithRouteParameterList_setParameter() {
        testForwardingToView("locationRouteParameterList");
    }

    @Test
    public void testForwardingToViewWithQueryParameter_setParameter() {
        testForwardingToView("locationQueryParams");
    }

    private void testForwardingToView(String location) {
        final String baseLoc = "/view/com.vaadin.flow.uitest.ui.SetParameterForwardToView";
        final String expectedParameter = location + "Two";
        final String expectedLoc = "/" + expectedParameter;
        getDriver().get(getRootURL() + baseLoc + "/" + location);
        waitForDevServer();

        waitForElementPresent(By.id(SetParameterForwardToView.LOCATION_ID));
        final String locationId = findElement(
                By.id(SetParameterForwardToView.LOCATION_ID)).getText();
        waitForElementPresent(By.id(SetParameterForwardToView.PARAMETER_ID));
        final String parameterValue = findElement(
                By.id(SetParameterForwardToView.PARAMETER_ID)).getText();

        Assert.assertTrue("should redirect to " + baseLoc + expectedLoc,
                locationId.endsWith(expectedLoc));
        Assert.assertTrue("should update the URL",
                getDriver().getCurrentUrl().endsWith(baseLoc + expectedLoc));
        Assert.assertEquals("forwarded parameter", expectedParameter,
                parameterValue);
    }

}
