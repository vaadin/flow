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
    public void should_HaveANewSessionId_when_NavigationAfterSessionExpired() {
        openUrl("/new-router-session/NormalView");

        navigateToAnotherView();
        String sessionId = getSessionId();
        navigateToFirstView();
        Assert.assertEquals(sessionId, getSessionId());

        navigateToSesssionExpireView();
        // expired session causes page reload, after the page reload there will
        // be a new session
        waitUntil(d -> !sessionId.equals(getSessionId()));

        String newSessionId = getSessionId();
        navigateToAnotherView();
        // session is preserved
        Assert.assertEquals(newSessionId, getSessionId());
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
        findElement(By.linkText("ViewWhichInvalidatesSession")).click();
    }

    private void navigateToInternalErrorView() {
        findElement(By.linkText("ViewWhichCausesInternalException")).click();
        // Won't actually reach the view..
    }

    private void navigateTo(String linkText) {
        findElement(By.linkText(linkText)).click();
        assertTextAvailableInView(linkText);

    }

    private void assertTextAvailableInView(String linkText) {
        Assert.assertNotNull(
                findElement(By.xpath("//strong[text()='" + linkText + "']")));
    }
}
