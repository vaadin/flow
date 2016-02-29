package com.vaadin.hummingbird.contexttest.ui;

import org.junit.Assert;

import com.vaadin.testbench.By;

public class RootContextIT extends AbstractContextIT {

    @Override
    protected String getAppContext() {
        return "";
    }

    @Override
    protected void verifyCorrectUI() {
        Assert.assertNotNull(findElement(By.id("root")));
    }

}
