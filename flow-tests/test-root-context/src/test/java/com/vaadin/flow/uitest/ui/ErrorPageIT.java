package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ErrorPageIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/view/abcd";
    };

    @Test
    public void testErrorViewOpened() {
        open();

        WebElement element = findElement(By.id("error-path"));
        Assert.assertNotNull(element);
        Assert.assertEquals("abcd", element.getText());

        getDriver().get(getTestURL() + "/foobar");

        element = findElement(By.id("error-path"));
        Assert.assertNotNull(element);
        Assert.assertEquals("abcd/foobar", element.getText());
    }
}
