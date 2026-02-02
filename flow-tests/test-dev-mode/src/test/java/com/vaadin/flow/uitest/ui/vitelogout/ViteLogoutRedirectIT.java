/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
