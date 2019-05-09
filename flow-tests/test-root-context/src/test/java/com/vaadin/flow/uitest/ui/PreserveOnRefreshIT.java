package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.uitest.ui.PreserveOnRefreshView.*;

public class PreserveOnRefreshIT extends ChromeBrowserTest {

    @Test
    public void refresh_componentAndUiChildrenReused() {
        open();
        final String componentId = getString(COMPONENT_ID);
        final String notificationId = getString(NOTIFICATION_ID);

        open();
        final String newComponentId = getString(COMPONENT_ID);
        final String newNotificationId = getString(NOTIFICATION_ID);
        final int attachCount = getInt(ATTACHCOUNTER_ID);

        Assert.assertEquals("Component contents expected identical",
                componentId, newComponentId);
        Assert.assertEquals("Notification contents expected identical",
                notificationId, newNotificationId);
        Assert.assertEquals("Expected two attaches",
                2, attachCount);
    }

    @Test
    public void navigateToNonRefreshing_refreshInDifferentWindow_componentIsRecreated() {
        open();
        final String componentId = getString(COMPONENT_ID);
        final String notificationId = getString(NOTIFICATION_ID);

        // navigate to some other page in between
        getDriver().get(getRootURL() +
                "/view/com.vaadin.flow.uitest.ui.PageView");

        open();
        final String newComponentId = getString(COMPONENT_ID);
        final String newNotificationId = getString(NOTIFICATION_ID);
        final int attachCount = getInt(ATTACHCOUNTER_ID);

        Assert.assertNotEquals("Component contents expected different",
                componentId, newComponentId);
        Assert.assertNotEquals("Notification contents expected different",
                notificationId, newNotificationId);
        Assert.assertEquals("Expected one attach",
                1, attachCount);
    }

    @Test
    public void refreshInDifferentWindow_componentIsRecreated() {
        open();
        final String firstWin = getDriver().getWindowHandle();

        final String componentId = getString(COMPONENT_ID);
        final String notificationId = getString(NOTIFICATION_ID);

        ((JavascriptExecutor) getDriver()).executeScript(
                "window.open('" + getTestURL() + "','_blank');");

        final String secondWin = getDriver().getWindowHandles().stream()
                .filter(windowId -> !windowId.equals(firstWin))
                .findFirst()
                .get();
        driver.switchTo().window(secondWin);

        final String newComponentId = getString(COMPONENT_ID);
        final String newNotificationId = getString(NOTIFICATION_ID);
        final int attachCount = getInt(ATTACHCOUNTER_ID);

        Assert.assertNotEquals("Component contents expected different",
                componentId, newComponentId);
        Assert.assertNotEquals("Notification contents expected different",
                notificationId, newNotificationId);
        Assert.assertEquals("Expected one attach",
                1, attachCount);
    }

    private String getString(String id) {
        waitForElementPresent(By.id(id));
        return findElement(By.id(id)).getText();
    }

    private int getInt(String id) {
        return Integer.parseInt(getString(id));

    }
}
