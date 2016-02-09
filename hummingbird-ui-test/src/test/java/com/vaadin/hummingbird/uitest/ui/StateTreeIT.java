package com.vaadin.hummingbird.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.hummingbird.uitest.PhantomJSTest;

public class StateTreeIT extends PhantomJSTest {

    @Test
    public void ensureDomUpdatesAndEventsDoSomething() {
        open();

        Assert.assertFalse(getDriver().getPageSource().contains("Thank you"));

        getDriver().findElement(By.tagName("input")).click();

        Assert.assertTrue(getDriver().getPageSource().contains("Thank you"));
    }

}
