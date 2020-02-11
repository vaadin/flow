package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ForwardToIT extends ChromeBrowserTest {

    @Test
    public void testForwardingToView() {
        String initUrl = getDriver().getCurrentUrl();
        open();

        if (hasClientIssue("7578")) {
            return;
        }

        Assert.assertTrue("should forward to specified view",
                findElement(By.id("root")).isDisplayed());
        Assert.assertTrue("should update update the URL",
                getDriver().getCurrentUrl()
                        .endsWith("com.vaadin.flow.uitest.ui.BasicComponentView"));

        getDriver().navigate().back();
        Assert.assertEquals("should replace history state",
                getDriver().getCurrentUrl(), initUrl);
    }
}
