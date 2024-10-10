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
