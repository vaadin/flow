package com.vaadin.hummingbird.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

public class ErrorPageIT extends PhantomJSTest {

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
        Assert.assertNotNull(findElement(By.id("main-layout")));

        getDriver().get(getTestURL() + "/foobar");

        element = findElement(By.id("error-path"));
        Assert.assertNotNull(element);
        Assert.assertEquals("abcd/foobar", element.getText());
        Assert.assertNotNull(findElement(By.id("main-layout")));
    }
}
