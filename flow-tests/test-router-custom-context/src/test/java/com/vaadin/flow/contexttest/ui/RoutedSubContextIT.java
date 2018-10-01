package com.vaadin.flow.contexttest.ui;

import org.junit.Assert;
import org.openqa.selenium.By;

public class RoutedSubContextIT extends AbstractContextIT {

    @Override
    protected String getAppContext() {
        return "/routed/path-sub-context/";
    }

    @Override
    protected void verifyCorrectUI() {
        Assert.assertNotNull(findElement(By.id("routed-sub")));
    }

}
