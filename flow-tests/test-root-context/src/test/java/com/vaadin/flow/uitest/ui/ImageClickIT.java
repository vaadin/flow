package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.WebElement;

public class ImageClickIT extends ChromeBrowserTest {
    @Test
    public void testClickOnImage() {
        open();

        WebElement message = findElement(By.id("message"));
        WebElement message2 = findElement(By.id("message2"));
        WebElement message3 = findElement(By.id("message3"));
        WebElement image = findElement(By.id("image"));

        Assert.assertEquals("Before click", message.getText());

        image.click();

        Assert.assertEquals("After click 1", message.getText());
        Assert.assertEquals("Single click 1", message2.getText());
        Assert.assertEquals("", message3.getText());
    }

    @Test
    public void testDoubleClickOnImage() {
        open();

        Actions act = new Actions(getDriver());

        WebElement message = findElement(By.id("message"));
        WebElement message2 = findElement(By.id("message2"));
        WebElement message3 = findElement(By.id("message3"));
        WebElement image = findElement(By.id("image"));

        Assert.assertEquals("Before click", message.getText());

        act.doubleClick(image).perform();

        Assert.assertEquals("After click 2", message.getText());
        Assert.assertEquals("Single click 1", message2.getText());
        Assert.assertEquals("Double click 1", message3.getText());
    }

}
