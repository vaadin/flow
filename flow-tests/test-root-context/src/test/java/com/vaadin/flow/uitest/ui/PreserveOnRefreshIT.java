package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PreserveOnRefreshIT extends ChromeBrowserTest {

    private final static String COMPONENT_ID = "contents";
    private final static String NOTIFICATION_ID = "notification";

    @Test
    public void refresh_componentAndUiChildrenReused() {
        open();
        final String componentId = getContents(COMPONENT_ID);
        final String notificationId = getContents(NOTIFICATION_ID);

        open();
        final String newComponentId = getContents(COMPONENT_ID);
        final String newNotificationId = getContents(NOTIFICATION_ID);

        Assert.assertEquals("Component contents expected identical",
                componentId, newComponentId);
        Assert.assertEquals("Notification contents expected identical",
                notificationId, newNotificationId);
    }

    @Test
    public void navigateToNonRefreshing_refreshInDifferentWindow_componentIsRecreated() {
        open();
        final String componentId = getContents(COMPONENT_ID);
        final String notificationId = getContents(NOTIFICATION_ID);

        // navigate to some other page in between
        getDriver().get(getRootURL() +
                "/view/com.vaadin.flow.uitest.ui.PageView");

        open();
        final String newComponentId = getContents(COMPONENT_ID);
        final String newNotificationId = getContents(NOTIFICATION_ID);

        Assert.assertNotEquals("Component contents expected different",
                componentId, newComponentId);
        Assert.assertNotEquals("Notification contents expected different",
                notificationId, newNotificationId);
    }

    @Test
    public void refreshInDifferentWindow_componentIsRecreated() {
        open();
        final String firstWin = getDriver().getWindowHandle();

        final String componentId = getContents(COMPONENT_ID);
        final String notificationId = getContents(NOTIFICATION_ID);

        ((JavascriptExecutor) getDriver()).executeScript(
                "window.open('" + getTestURL() + "','_blank');");

        final String secondWin = getDriver().getWindowHandles().stream()
                .filter(windowId -> !windowId.equals(firstWin))
                .findFirst()
                .get();
        driver.switchTo().window(secondWin);

        final String newComponentId = getContents(COMPONENT_ID);
        final String newNotificationId = getContents(NOTIFICATION_ID);

        Assert.assertNotEquals("Component contents expected different",
                componentId, newComponentId);
        Assert.assertNotEquals("Notification contents expected different",
                notificationId, newNotificationId);
    }

    private String getContents(String id) {
        waitForElementPresent(By.id(id));
        return findElement(By.id(COMPONENT_ID)).getText();
    }
}
