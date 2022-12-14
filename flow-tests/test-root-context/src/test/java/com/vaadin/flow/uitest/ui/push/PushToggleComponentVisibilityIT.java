package com.vaadin.flow.uitest.ui.push;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Category(PushTests.class)
public class PushToggleComponentVisibilityIT extends ChromeBrowserTest {

    private static final String HIDE = "hide";

    @Test
    public void ensureComponentVisible() {
        open();

        $(TestBenchElement.class).id(HIDE).click();
        Assert.assertEquals("Please wait",
                $(TestBenchElement.class).id("label").getText());

        waitUntil(driver -> isElementPresent(By.id(HIDE)));
        $(TestBenchElement.class).id(HIDE).click();
        Assert.assertEquals("Please wait",
                $(TestBenchElement.class).id("label").getText());
    }
}
