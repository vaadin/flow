package com.vaadin.hummingbird.uitest.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.uitest.PhantomJSTest;

public class StateTreeIT extends PhantomJSTest {

    @Test
    public void ensureDomContainsSomething() throws InterruptedException {
        open();
        Assert.assertTrue(getDriver().getPageSource().contains("Hello world"));
    }

}
