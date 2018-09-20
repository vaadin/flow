package com.vaadin.flow.contexttest.ui;

import org.junit.Assert;
import org.openqa.selenium.By;

public class SubContextIT extends AbstractContextIT {

    @Override
    protected String getAppContext() {
        return "/sub-context/";
    }

    @Override
    protected void verifyCorrectUI() {
        Assert.assertNotNull(findElement(By.id("sub")));
    }

}
