/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.vitelogout;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Integration test for issue #20819: Vite page reload should not cancel
 * server-initiated redirect when session is invalidated.
 * <p>
 * Test scenario:
 * <ol>
 * <li>Navigate to login page</li>
 * <li>Submit login form (POST to login route)</li>
 * <li>Filter intercepts, sets authenticated flag, redirects to logout-test
 * route</li>
 * <li>Click logout button which sets location to session-ended route and
 * invalidates session</li>
 * <li>Verify user lands on session-ended page (not reloaded by Vite)</li>
 * </ol>
 */
public class ViteLogoutRedirectIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/view/com.vaadin.flow.uitest.ui.vitelogout.LoginView";
    }

    @Test
    public void logoutRedirect_sessionInvalidated_redirectsToSessionEndedPage() {
        // Navigate to login page
        open();

        // Submit the login form
        $(NativeButtonElement.class).id("login-button").click();

        // Verify redirected to logout-test page
        waitUntil(driver -> $(SpanElement.class).id("logout-test-marker")
                .isDisplayed());

        // Click logout button
        $(NativeButtonElement.class).id("logout-button").click();

        // Verify redirected to session-ended page (not reloaded by Vite)
        waitUntil(driver -> $(SpanElement.class).id("session-ended-marker")
                .isDisplayed());

        // Verify we're on the correct URL
        Assert.assertTrue("Should be on SessionEndedView page",
                getDriver().getCurrentUrl().contains(
                        "/view/com.vaadin.flow.uitest.ui.vitelogout.SessionEndedView"));
    }
}
