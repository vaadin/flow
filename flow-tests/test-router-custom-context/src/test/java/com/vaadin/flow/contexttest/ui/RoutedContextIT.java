package com.vaadin.flow.contexttest.ui;

import org.junit.Assert;
import org.openqa.selenium.By;

public class RoutedContextIT extends AbstractContextIT {

    @Override
    protected String getAppContext() {
        return "/routed";
    }

    @Override
    protected void verifyCorrectUI() {
        Assert.assertNotNull(findElement(By.id("routed")));
    }

}
