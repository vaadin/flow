package com.vaadin.hummingbird.uitest.ui;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.uitest.PhantomJSTest;

public class BasicElementIT extends PhantomJSTest {

    @Test
    public void ensureDomUpdatesAndEventsDoSomething() {
        open();

        Assert.assertEquals(0, getThankYouCount());

        getDriver().findElement(By.tagName("input")).click();

        Assert.assertEquals(1, getThankYouCount());

        // Clicking removes the element
        getThankYouElements().get(0).click();

        Assert.assertEquals(0, getThankYouCount());

        WebElement helloElement = getDriver().findElement(By.id("hello-world"));

        Assert.assertEquals("Hello world", helloElement.getText());
        Assert.assertEquals("hello", helloElement.getAttribute("class"));

        helloElement.click();

        Assert.assertEquals("Stop touching me!", helloElement.getText());
        Assert.assertEquals("", helloElement.getAttribute("class"));
    }

    private int getThankYouCount() {
        return getThankYouElements().size();
    }

    private List<WebElement> getThankYouElements() {
        return getDriver().findElements(By.cssSelector(".thankYou"));
    }

}
