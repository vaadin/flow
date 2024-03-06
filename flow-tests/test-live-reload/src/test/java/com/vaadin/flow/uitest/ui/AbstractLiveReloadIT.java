/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;

import com.vaadin.flow.testutil.ChromeDeviceTest;

public abstract class AbstractLiveReloadIT extends ChromeDeviceTest {

    private String initialAttachId = "";

    @Override
    protected String getTestPath() {
        return "/context" + super.getTestPath();
    }

    protected void open() {
        open((String[]) null);
        waitForServiceWorkerReady();
        waitForElementPresent(By.id(AbstractLiveReloadView.ATTACH_IDENTIFIER));
        initialAttachId = getAttachId();
    }

    protected String getAttachId() {
        return findElement(By.id(AbstractLiveReloadView.ATTACH_IDENTIFIER))
                .getText();
    }

    protected void waitForLiveReload() {
        waitUntil(d -> {
            try {
                final String newViewId = getAttachId();
                return !initialAttachId.equals(newViewId);
            } catch (StaleElementReferenceException ex) {
                return false;
            }
        });
    }

    public String getInitialAttachId() {
        return initialAttachId;
    }
}
