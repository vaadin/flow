/*
 * Copyright 2000-2025 Vaadin Ltd.
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
