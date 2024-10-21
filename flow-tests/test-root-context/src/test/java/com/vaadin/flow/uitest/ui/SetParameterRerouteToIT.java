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
