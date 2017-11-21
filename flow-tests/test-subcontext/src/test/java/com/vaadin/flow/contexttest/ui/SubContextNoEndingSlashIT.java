package com.vaadin.flow.contexttest.ui;

import static org.junit.Assert.assertNotNull;

import org.openqa.selenium.By;

public class SubContextNoEndingSlashIT extends AbstractContextIT {

    @Override
    protected String getAppContext() {
        return "/SubContext";
    }

    @Override
    protected void verifyCorrectUI() {
        assertNotNull(findElement(By.id("sub")));
    }

}
