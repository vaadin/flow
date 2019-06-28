package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class KeyboardEventIT extends ChromeBrowserTest {
    @Test
    public void verify_that_keys_are_received_correctly() {
        open();

        // make sure both elements are present
        Assert.assertTrue(isElementPresent(By.id("input")));
        Assert.assertTrue(isElementPresent(By.id("paragraph")));

        WebElement input = findElement(By.id("input"));
        WebElement paragraph = findElement(By.id("paragraph"));

        input.sendKeys("q");

        Assert.assertEquals(
                "q:KeyQ",
                paragraph.getText()
        );

        input.sendKeys("%");

        Assert.assertEquals(
                "%:Digit5",
                paragraph.getText()
        );
        // next tests rely on
        // https://github.com/SeleniumHQ/selenium/blob/master/javascript/node/selenium-webdriver/lib/input.js#L52

        // arrow right
        input.sendKeys("\uE014");

        Assert.assertEquals(
                "ArrowRight:ArrowRight",
                paragraph.getText()
        );

        // physical * key
        input.sendKeys("\uE024");

        Assert.assertEquals(
                "*:NumpadMultiply",
                paragraph.getText()
        );
    }

    @Test // #5989
    public void verify_that_invalid_keyup_event_is_ignored() {
        open();

        WebElement input = findElement(By.id("input"));
        WebElement sendInvalidKeyUp = findElement(By.id("sendInvalidKeyUp"));
        WebElement paragraph = findElement(By.id("keyUpParagraph"));

        input.sendKeys("q");
        Assert.assertEquals("q", paragraph.getText());

        sendInvalidKeyUp.click();

        Assert.assertEquals("q", paragraph.getText());
    }
}
