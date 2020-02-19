package com.vaadin.flow.uitest.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import static com.vaadin.flow.uitest.ui.PreserveOnRefreshView.ATTACHCOUNTER_ID;
import static com.vaadin.flow.uitest.ui.PreserveOnRefreshView.COMPONENT_ID;
import static com.vaadin.flow.uitest.ui.PreserveOnRefreshView.NOTIFICATION_ID;

public class PreserveOnRefreshIT extends AbstractStreamResourceIT {

    @Test
    public void refresh_componentAndUiChildrenReused() throws IOException {
        open();
        if (hasClientIssue("7587")) {
            return;
        }

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
        Assert.assertEquals("Expected two attaches", 2, attachCount);

        assertDownloadedContent();
    }

    @Test
    public void navigateToNonRefreshing_refreshInDifferentWindow_componentIsRecreated() {
        open();
        if (hasClientIssue("7587")) {
            return;
        }

        final String componentId = getString(COMPONENT_ID);
        final String notificationId = getString(NOTIFICATION_ID);

        // navigate to some other page in between
        getDriver()
                .get(getRootURL() + "/view/com.vaadin.flow.uitest.ui.PageView");

        open();
        final String newComponentId = getString(COMPONENT_ID);
        final String newNotificationId = getString(NOTIFICATION_ID);
        final int attachCount = getInt(ATTACHCOUNTER_ID);

        Assert.assertNotEquals("Component contents expected different",
                componentId, newComponentId);
        Assert.assertNotEquals("Notification contents expected different",
                notificationId, newNotificationId);
        Assert.assertEquals("Expected one attach", 1, attachCount);
    }

    @Test
    public void refreshInDifferentWindow_componentIsRecreated()
            throws IOException {
        open();
        final String firstWin = getDriver().getWindowHandle();

        final String componentId = getString(COMPONENT_ID);
        final String notificationId = getString(NOTIFICATION_ID);

        ((JavascriptExecutor) getDriver())
                .executeScript("window.open('" + getTestURL() + "','_blank');");

        final String secondWin = getDriver().getWindowHandles().stream()
                .filter(windowId -> !windowId.equals(firstWin)).findFirst()
                .get();
        driver.switchTo().window(secondWin);

        final String newComponentId = getString(COMPONENT_ID);
        final String newNotificationId = getString(NOTIFICATION_ID);
        final int attachCount = getInt(ATTACHCOUNTER_ID);

        Assert.assertNotEquals("Component contents expected different",
                componentId, newComponentId);
        Assert.assertNotEquals("Notification contents expected different",
                notificationId, newNotificationId);
        Assert.assertEquals("Expected one attach", 1, attachCount);

        assertDownloadedContent();
    }

    private String getString(String id) {
        waitForElementPresent(By.id(id));
        return findElement(By.id(id)).getText();
    }

    private int getInt(String id) {
        return Integer.parseInt(getString(id));

    }

    private void assertDownloadedContent() throws IOException {
        WebElement link = findElement(By.id("link"));
        String url = link.getAttribute("href");

        getDriver().manage().timeouts().setScriptTimeout(15, TimeUnit.SECONDS);

        try (InputStream stream = download(url)) {
            List<String> lines = IOUtils.readLines(stream,
                    StandardCharsets.UTF_8);
            String text = lines.stream().collect(Collectors.joining());
            Assert.assertEquals("foo", text);
        }
    }
}
