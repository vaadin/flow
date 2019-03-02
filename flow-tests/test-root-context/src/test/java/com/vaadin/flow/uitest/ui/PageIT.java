package com.vaadin.flow.uitest.ui;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.WebDriver;

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
        $(InputTextElement.class).id("input").setValue(title);
        findElement(By.id("button")).click();
    }

    @Test
    public void testReload() {
        open();

        InputTextElement input = $(InputTextElement.class).id("input");
        input.setValue("foo");
        Assert.assertEquals("foo", input.getPropertyString("value"));
        findElement(By.id("reload")).click();
        input = $(InputTextElement.class).id("input");
        Assert.assertEquals("", input.getValue());
    }

    @Test
    public void testSetLocation() {
        open();

        findElement(By.id("setLocation")).click();
        Assert.assertThat(getDriver().getCurrentUrl(),
                Matchers.startsWith("https://www.google.com"));
    }

    @Test
    public void testOpenUrl() {
        open();

        findElement(By.id("open")).click();
        Assert.assertThat(
                getDriver().switchTo().window("secondwindow").getCurrentUrl(),
                Matchers.startsWith("https://www.google.com")
        );
    }

    @Test
    public void testOpenUrlWithSizedWindow() {
        open();

        findElement(By.id("openWithSize")).click();
        WebDriver newWindow = getDriver().switchTo().window("sizewindow");
        Assert.assertThat(newWindow.getCurrentUrl(),
                Matchers.startsWith("https://www.google.com"));

        Assert.assertEquals(newWindow.manage().window().getSize().getWidth(), 400);
        // if not headless driver, window will have extra 28px for the address bar.
        Assert.assertEquals(newWindow.manage().window().getSize().getHeight(), isHeadless() ? 400 : 428);
    }
}
