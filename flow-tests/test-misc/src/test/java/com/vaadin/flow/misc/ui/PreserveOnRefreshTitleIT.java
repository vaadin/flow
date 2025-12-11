package com.vaadin.flow.misc.ui;

import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PreserveOnRefreshTitleIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/preserve-on-refresh-title-view";
    }

    @Test
    public void preserveOnRefresh_updatesTitle_titlePreservedOnRefresh() {
        open();

        findElement(By.id("update-title")).click();

        waitUntil(driver -> "Updated Title".equals(driver.getTitle()));

        getDriver().navigate().refresh();

        waitUntil(driver -> "Updated Title".equals(driver.getTitle()));
    }
}
