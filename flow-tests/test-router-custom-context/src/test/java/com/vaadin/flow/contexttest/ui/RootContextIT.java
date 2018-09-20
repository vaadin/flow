package com.vaadin.flow.contexttest.ui;

import org.junit.Assert;
import org.openqa.selenium.By;

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
