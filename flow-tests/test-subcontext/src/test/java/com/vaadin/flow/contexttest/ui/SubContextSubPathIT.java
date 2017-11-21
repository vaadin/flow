package com.vaadin.flow.contexttest.ui;

import static org.junit.Assert.assertNotNull;

import org.openqa.selenium.By;

public class SubContextSubPathIT extends AbstractContextIT {

    @Override
    protected String getAppContext() {
        return "/SubContext/foo/bar";
    }

    @Override
    protected void verifyCorrectUI() {
        assertNotNull(findElement(By.id("sub")));
    }

}
