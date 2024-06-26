/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class RouterSessionExpirationIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/view/";
    }

    @Test
    public void navigationAfterSessionExpired() {
        openUrl("/new-router-session/NormalView");

        navigateToAnotherView();
        String sessionId = getSessionId();
        navigateToFirstView();
        Assert.assertEquals(sessionId, getSessionId());
        navigateToSesssionExpireView();
        // expired session causes page reload, after the page reload there will
        // be a new session
        Assert.assertNotEquals(sessionId, getSessionId());
        sessionId = getSessionId();
        navigateToFirstView();
        // session is preserved
        Assert.assertEquals(sessionId, getSessionId());
    }

    @Test
    public void navigationAfterInternalError() {
        openUrl("/new-router-session/NormalView");

        navigateToAnotherView();
        String sessionId = getSessionId();
        navigateToInternalErrorView();

        waitUntil(webDriver -> findElements(By.id("sessionId")).isEmpty());
        // Navigate back as we are on the error view.
        getDriver().navigate().back();
        Assert.assertEquals(sessionId, getSessionId());
    }

    private String getSessionId() {
        return findElement(By.id("sessionId")).getText();
    }

    private void navigateToFirstView() {
        navigateTo("NormalView");
    }

    private void navigateToAnotherView() {
        navigateTo("AnotherNormalView");
    }

    private void navigateToSesssionExpireView() {
        navigateTo("ViewWhichInvalidatesSession");
    }

    private void navigateToInternalErrorView() {
        findElement(By.linkText("ViewWhichCausesInternalException")).click();
        // Won't actually reach the view..
    }

    private void navigateTo(String linkText) {
        findElement(By.linkText(linkText)).click();
        Assert.assertNotNull(
                findElement(By.xpath("//strong[text()='" + linkText + "']")));

    }
}
