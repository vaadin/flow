/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Before;
import org.openqa.selenium.StaleElementReferenceException;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class AbstractReloadIT extends ChromeBrowserTest {

    // Tests in this module routinely recompile classes, which causes Spring
    // Boot DevTools to restart Tomcat. A previous test's restart can still be
    // in progress when the next test starts, so wait until the server is
    // reachable instead of failing on the first refused connection.
    @Before
    @Override
    public void checkIfServerAvailable() {
        waitUntil(driver -> {
            try {
                super.checkIfServerAvailable();
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    protected void reloadAndWait() {
        String viewId = getViewId();
        $("*").id(SessionValueView.TRIGGER_RELOAD_ID).click();
        waitUntil(driver -> {
            try {
                return !getViewId().equals(viewId);
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    protected String getViewId() {
        return $("*").id("viewId").getText();
    }
}
