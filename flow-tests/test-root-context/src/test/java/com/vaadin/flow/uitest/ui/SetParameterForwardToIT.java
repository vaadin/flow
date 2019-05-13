package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class SetParameterForwardToIT extends ChromeBrowserTest {

    @Test
    public void testForwardingToViewInSetParameter() {
        open();

        waitForElementPresent(By.id("auto"));

        Assert.assertTrue("should update the URL",
                getDriver().getCurrentUrl()
                        .endsWith("/view/com.vaadin.flow.uitest.ui.SetParameterForwardToView/auto"));
    }
}
