package com.vaadin.flow.uitest.ui.push;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.parallel.TestCategory;

@TestCategory("push")
@Category(PushTests.class)
public class EnableDisablePushIT extends ChromeBrowserTest {
    @Test
    public void testEnablePushWhenUsingPolling() throws Exception {
        open();

        Assert.assertEquals("1. Push enabled", getLogRow(0));

        $(TestBenchElement.class).id("disable-push").click();
        Assert.assertEquals("3. Push disabled", getLogRow(0));

        $(TestBenchElement.class).id("enable-poll").click();
        Assert.assertEquals("5. Poll enabled", getLogRow(0));

        $(TestBenchElement.class).id("enable-push").click();
        Assert.assertEquals("7. Push enabled", getLogRow(0));

        $(TestBenchElement.class).id("disable-poll").click();
        Assert.assertEquals("9. Poll disabled", getLogRow(0));

        $(TestBenchElement.class).id("thread-re-enable-push").click();
        Thread.sleep(2500);
        Assert.assertEquals("16. Polling disabled, push enabled", getLogRow(0));

        $(TestBenchElement.class).id("disable-push").click();
        Assert.assertEquals("18. Push disabled", getLogRow(0));
    }

    private String getLogRow(int index) {
        return findElements(By.className("log")).get(0).getText();
    }

}