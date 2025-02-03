package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class IFrameIT extends ChromeBrowserTest {

    @Test
    public void testIFrameReload() {
        open();

        waitForElementPresent(By.id("frame1"));
        getDriver().switchTo().frame("frame1");
        Assert.assertEquals("A", findElement(By.id("Friday")).getText());

        getDriver().switchTo().parentFrame();
        findElement(By.id("Reload")).click();

        getDriver().switchTo().frame("frame1");
        waitUntil(webDriver -> "B"
                .equals(findElement(By.id("Friday")).getText()));
    }

    @Test
    public void testIFrameWithDynamicResource() {
        open();

        waitForElementPresent(By.id("frame2"));
        getDriver().switchTo().frame("frame2");
        Assert.assertEquals("Dynamic", findElement(By.id("content")).getText());
    }
}
