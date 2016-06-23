package com.vaadin.hummingbird.contexttest.ui;

import org.junit.Assert;
import org.openqa.selenium.By;

public class SubContextSubPathIT extends AbstractContextIT {

    @Override
    protected String getAppContext() {
        return "/SubContext/foo/bar";
    }

    @Override
    protected void verifyCorrectUI() {
        Assert.assertNotNull(findElement(By.id("sub")));
    }

}
