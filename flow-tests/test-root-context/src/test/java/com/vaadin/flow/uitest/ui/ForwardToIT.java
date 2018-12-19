package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

public class ForwardToIT extends ChromeBrowserTest {

    @Test
    public void testForwardingToView() {
        String initUrl = getDriver().getCurrentUrl();
        open();

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
