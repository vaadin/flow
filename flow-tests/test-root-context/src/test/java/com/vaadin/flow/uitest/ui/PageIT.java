package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class PageIT extends ChromeBrowserTest {

    @Test
    public void testPageTitleUpdates() {
        open();

        updateTitle("Page title 1");
        verifyTitle("Page title 1");

        updateTitle("FOObar");
        verifyTitle("FOObar");
    }

    @Test
    public void testOnlyMostRecentPageUpdate() {
        open();

        updateTitle("Page title 1");
        verifyTitle("Page title 1");

        findElement(By.id("input")).sendKeys("title 2" + Keys.TAB);
        findElement(By.id("override")).click();

        verifyTitle("OVERRIDDEN");
    }

    @Test
    public void testPageTitleClears() {
        open();

        findElement(By.id("override")).click();
        verifyTitle("OVERRIDDEN");

        updateTitle("");
        verifyTitle("");
    }

    private void verifyTitle(String title) {
        Assert.assertEquals("Page title does not match", title,
                getDriver().getTitle());
    }

    private void updateTitle(String title) {
        findElement(By.id("input")).clear();
        findElement(By.id("input")).sendKeys(title + Keys.TAB);
        findElement(By.id("button")).click();
    }

    @Test
    public void testReload() {
        open();

        TestBenchElement input = (TestBenchElement) findElement(By.id("input"));
        input.sendKeys("foo");
        Assert.assertEquals("foo", input.getPropertyString("value"));
        findElement(By.id("reload")).click();
        input = (TestBenchElement) findElement(By.id("input"));
        Assert.assertEquals("", input.getPropertyString("value"));
    }
}
