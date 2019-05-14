package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class SetParameterForwardToIT extends ChromeBrowserTest {

    @Test
    public void testForwardingToViewInSetParameter() {
        final String baseLoc =
                "/view/com.vaadin.flow.uitest.ui.SetParameterForwardToView";
        getDriver().get(getRootURL() + baseLoc + "/one");

        waitForElementPresent(By.id(SetParameterForwardToView.LOCATION_ID));
        final String locationId = findElement(
                By.id(SetParameterForwardToView.LOCATION_ID)).getText();
        Assert.assertTrue("should redirect to "+ baseLoc + "/two",
                locationId.endsWith("/two"));
        Assert.assertTrue("should update the URL",
                getDriver().getCurrentUrl().endsWith(baseLoc + "/two"));
    }
}
