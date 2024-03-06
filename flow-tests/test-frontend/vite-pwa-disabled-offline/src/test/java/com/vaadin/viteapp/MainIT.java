/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.testutil.ChromeDeviceTest;

public class MainIT extends ChromeDeviceTest {
    @Before
    public void init() {
        open();
    }

    @Test
    public void noAttemptsToRegisterServiceWorker() {
        checkLogsForErrors();
    }

    @Test
    public void tryToRegisterServiceWorker_serviceWorkerIsNotAvailable() {
        Assert.assertFalse("The service worker should not be available",
                (Boolean) ((JavascriptExecutor) getDriver()).executeAsyncScript(
                        "const done = arguments[arguments.length - 1];"
                                + "navigator.serviceWorker.register('sw.js')"
                                + "   .then(() => done(true))"
                                + "   .catch(() => done(false))"));
    }

    @Test
    public void registerFakeServiceWorker_reload_serviceWorkerIsUnregistered() {
        ((JavascriptExecutor) getDriver()).executeAsyncScript(
                "const done = arguments[arguments.length - 1];"
                        + "navigator.serviceWorker.register('fake-sw.js').then(done);");
        reload();

        Assert.assertFalse("The service worker should be unregistered",
                (Boolean) ((JavascriptExecutor) getDriver()).executeAsyncScript(
                        "const done = arguments[arguments.length - 1];"
                                + "navigator.serviceWorker.getRegistration('fake-sw.js').then((registration) => {"
                                + "  done(!!registration);" + "});"));
    }

    @Override
    protected String getTestPath() {
        return "";
    }

    private void reload() {
        executeScript("window.location.reload();");
        waitUntil(webDriver -> ((JavascriptExecutor) driver)
                .executeScript("return document.readyState")
                .equals("complete"));
    }
}
