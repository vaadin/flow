package com.vaadin.hummingbird.uitest.ui;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.testutil.PhantomJSTest;

public abstract class AbstractBasicElementComponentIT extends PhantomJSTest {

    @Test
    public void ensureDomUpdatesAndEventsDoSomething() {
        open();

        Assert.assertEquals(0, getThankYouCount());

        findElement(By.tagName(Tag.INPUT)).sendKeys("abc");
        findElement(By.tagName(Tag.BUTTON)).click();

        Assert.assertEquals(1, getThankYouCount());

        String buttonText = getThankYouElements().get(0).getText();
        String expected = "Thank you for clicking \"Click me\" at \\((\\d+),(\\d+)\\)! The field value is abc";
        Assert.assertTrue(
                "Expected '" + expected + "', was '" + buttonText + "'",
                buttonText.matches(expected));

        // Clicking removes the element
        getThankYouElements().get(0).click();

        Assert.assertEquals(0, getThankYouCount());

        WebElement helloElement = findElement(By.id("hello-world"));

        Assert.assertEquals("Hello world", helloElement.getText());
        Assert.assertEquals("hello", helloElement.getAttribute("class"));

        helloElement.click();

        Assert.assertEquals("Stop touching me!", helloElement.getText());
        Assert.assertEquals("", helloElement.getAttribute("class"));

        // Clicking again shouldn't have any effect
        helloElement.click();
        Assert.assertEquals("Stop touching me!", helloElement.getText());
    }

    private int getThankYouCount() {
        return getThankYouElements().size();
    }

    private List<WebElement> getThankYouElements() {
        return findElements(By.cssSelector(".thankYou"));
    }

}
