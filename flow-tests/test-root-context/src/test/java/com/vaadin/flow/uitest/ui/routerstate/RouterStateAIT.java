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
package com.vaadin.flow.uitest.ui.routerstate;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * IT for {@code UI.routerStateSignal()}: a shared {@link RouterStateLayout}
 * subscribes to the signal and renders the active path, target class, route
 * parameters and an update counter; navigating between {@link RouterStateAView}
 * and {@link RouterStateBView} (with and without route parameters) must update
 * the layout's display, while UI mutations unrelated to navigation (locale
 * change) must not.
 */
public class RouterStateAIT extends ChromeBrowserTest {

    @Test
    public void layoutFollowsRouterStateAcrossNavigations() {
        open();

        // Initial navigation to View A
        waitForPath("RouterStateAView");
        Assert.assertEquals("RouterStateAView",
                text(RouterStateLayout.TARGET_ID));
        Assert.assertEquals("", text(RouterStateLayout.PARAMS_ID));
        int updatesAfterA = parseUpdates();
        Assert.assertTrue(
                "Update counter should be > 0 after initial navigation",
                updatesAfterA > 0);

        // Navigate to B/42 — leaf class changes, route parameter populated
        click(RouterStateAView.LINK_TO_B_WITH_ID);
        waitForPath("RouterStateBView/42");
        Assert.assertEquals("RouterStateBView",
                text(RouterStateLayout.TARGET_ID));
        Assert.assertEquals("id=42", text(RouterStateLayout.PARAMS_ID));
        int updatesAfterB42 = parseUpdates();
        Assert.assertTrue("Update counter should grow after navigation",
                updatesAfterB42 > updatesAfterA);

        // Self-navigation to a different :id — same view class, different param
        click("router-state-link-b-id-99");
        waitForPath("RouterStateBView/99");
        Assert.assertEquals("RouterStateBView",
                text(RouterStateLayout.TARGET_ID));
        Assert.assertEquals("id=99", text(RouterStateLayout.PARAMS_ID));
        int updatesAfterB99 = parseUpdates();
        Assert.assertTrue(
                "Self-navigation with new route parameter must update the signal",
                updatesAfterB99 > updatesAfterB42);

        // Locale change — unrelated UI mutation must NOT touch the router
        // signal
        click(RouterStateBView.SET_LOCALE_BUTTON);
        // Path/target/params must still match the last navigation
        Assert.assertEquals("RouterStateBView/99",
                text(RouterStateLayout.PATH_ID));
        Assert.assertEquals("RouterStateBView",
                text(RouterStateLayout.TARGET_ID));
        Assert.assertEquals("id=99", text(RouterStateLayout.PARAMS_ID));
        Assert.assertEquals(
                "Non-navigation UI mutation must not increment router signal updates",
                updatesAfterB99, parseUpdates());

        // Navigate back to A — leaf class changes again, params clear
        click(RouterStateBView.LINK_TO_A);
        waitForPath("RouterStateAView");
        Assert.assertEquals("RouterStateAView",
                text(RouterStateLayout.TARGET_ID));
        Assert.assertEquals("", text(RouterStateLayout.PARAMS_ID));
        Assert.assertTrue("Update counter should grow after navigating back",
                parseUpdates() > updatesAfterB99);
    }

    private void waitForPath(String pathSuffix) {
        waitUntil(
                driver -> text(RouterStateLayout.PATH_ID).endsWith(pathSuffix));
    }

    private int parseUpdates() {
        return Integer.parseInt(text(RouterStateLayout.UPDATES_ID));
    }

    private String text(String id) {
        WebElement el = findElement(By.id(id));
        return el.getText();
    }

    private void click(String id) {
        findElement(By.id(id)).click();
    }
}
