package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PreserveOnRefreshIT extends ChromeBrowserTest {

    private final static String DIV_ID = "contents";

    @Test
    public void refresh_componentIsReused() {
        open();
        waitForElementPresent((By.id(DIV_ID)));
        final String initialContents = findElement(By.id(DIV_ID)).getText();

        open();
        waitForElementPresent((By.id(DIV_ID)));
        final String newContents = findElement(By.id(DIV_ID)).getText();

        Assert.assertEquals("Component contents expected identical",
                initialContents, newContents);
    }

    @Test
    public void navigateToNonRefreshing_refreshInDifferentWindow_componentIsRecreated() {
        open();
        waitForElementPresent((By.id(DIV_ID)));
        final String initialContents = findElement(By.id(DIV_ID)).getText();

        // navigate to some other page in between
        getDriver().get(getRootURL() +
                "/view/com.vaadin.flow.uitest.ui.PageView");

        open();
        waitForElementPresent((By.id(DIV_ID)));
        final String newContents = findElement(By.id(DIV_ID)).getText();

        Assert.assertNotEquals("Component contents expected different",
                initialContents, newContents);
    }

    @Test
    public void refreshInDifferentWindow_componentIsRecreated() {
        open();
        final String firstWin = getDriver().getWindowHandle();
        waitForElementPresent((By.id(DIV_ID)));
        final String initialContents = findElement(By.id(DIV_ID)).getText();

        ((JavascriptExecutor) getDriver()).executeScript(
                "window.open('" + getTestURL() + "','_blank');");

        final String secondWin = getDriver().getWindowHandles().stream()
                .filter(windowId -> !windowId.equals(firstWin))
                .findFirst()
                .get();
        driver.switchTo().window(secondWin);
        waitForElementPresent((By.id(DIV_ID)));
        final String newContents = findElement(By.id(DIV_ID)).getText();

        Assert.assertNotEquals("Component contents expected different",
                initialContents, newContents);
    }
}
