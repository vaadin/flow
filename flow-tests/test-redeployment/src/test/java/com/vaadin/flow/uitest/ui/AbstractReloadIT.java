/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.openqa.selenium.StaleElementReferenceException;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class AbstractReloadIT extends ChromeBrowserTest {

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
