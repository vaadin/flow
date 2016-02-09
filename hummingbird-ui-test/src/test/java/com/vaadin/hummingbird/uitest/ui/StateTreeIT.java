package com.vaadin.hummingbird.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.hummingbird.uitest.PhantomJSTest;

public class StateTreeIT extends PhantomJSTest {

    @Test
    public void ensureDomUpdatesAndEventsDoSomething() {
        open();

        Assert.assertEquals(0, getThankYouCount());

        getDriver().findElement(By.tagName("input")).click();

        Assert.assertEquals(1, getThankYouCount());
    }

    private int getThankYouCount() {
        return getDriver().findElements(By.cssSelector(".thankYou")).size();
    }

}
