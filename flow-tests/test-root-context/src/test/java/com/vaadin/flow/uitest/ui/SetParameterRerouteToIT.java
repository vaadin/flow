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

public class SetParameterRerouteToIT extends ChromeBrowserTest {

    @Test
    public void testReroutingToViewWithLocation_setParameter() {
        testReroutingToView("location");
    }

    @Test
    public void testReroutingToViewWithRouteParameter_setParameter() {
        testReroutingToView("locationRouteParameter");
    }

    @Test
    public void testReroutingToViewWithRouteParameterList_setParameter() {
        testReroutingToView("locationRouteParameterList");
    }

    @Test
    public void testReroutingToViewWithQueryParameter_setParameter() {
        testReroutingToView("locationQueryParams");
    }

    private void testReroutingToView(String location) {
        final String baseLoc = "/view/com.vaadin.flow.uitest.ui.SetParameterRerouteToView";
        final String expectedParameter = location + "Two";
        final String expectedLoc = "/" + expectedParameter;
        final String originalLoc = baseLoc + "/" + location;
        getDriver().get(getRootURL() + originalLoc);
        waitForDevServer();

        waitForElementPresent(By.id(SetParameterForwardToView.LOCATION_ID));
        final String locationId = findElement(
                By.id(SetParameterForwardToView.LOCATION_ID)).getText();
        waitForElementPresent(By.id(SetParameterForwardToView.PARAMETER_ID));
        final String parameterValue = findElement(
                By.id(SetParameterForwardToView.PARAMETER_ID)).getText();

        Assert.assertTrue("should redirect to " + baseLoc + expectedLoc,
                locationId.endsWith(expectedLoc));
        Assert.assertTrue("should not update the URL",
                getDriver().getCurrentUrl().endsWith(originalLoc));
        Assert.assertEquals("rerouted parameter", expectedParameter,
                parameterValue);
    }

}
