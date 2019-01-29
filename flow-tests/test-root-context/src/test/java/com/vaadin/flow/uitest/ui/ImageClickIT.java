package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ImageClickIT extends ChromeBrowserTest {
    @Test
    public void testClickOnImage() {
        open();

        WebElement message = findElement(By.id("message"));
        WebElement image = findElement(By.id("image"));

        Assert.assertEquals("Before click", message.getText());

        image.click();

        Assert.assertEquals("After click", message.getText());

    }
}
