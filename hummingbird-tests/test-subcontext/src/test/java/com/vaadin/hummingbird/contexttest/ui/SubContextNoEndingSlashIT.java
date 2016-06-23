package com.vaadin.hummingbird.contexttest.ui;

import org.junit.Assert;
import org.openqa.selenium.By;

public class SubContextNoEndingSlashIT extends AbstractContextIT {

    @Override
    protected String getAppContext() {
        return "/SubContext";
    }

    @Override
    protected void verifyCorrectUI() {
        Assert.assertNotNull(findElement(By.id("sub")));
    }

}
