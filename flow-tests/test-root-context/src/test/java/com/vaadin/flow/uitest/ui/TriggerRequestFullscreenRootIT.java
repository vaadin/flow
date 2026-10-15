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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TriggerRequestFullscreenRootIT extends ChromeBrowserTest {

    @Test
    public void fullscreenViewRoot_doesNotCrash_andServerReceivesSuccess() {
        open();
        installResolvingFullscreenShim();

        WebElement button = findElement(By.id("go"));
        WebElement status = findElement(By.id("status"));

        button.click();

        // The fullscreened component is the wrapper's direct child (the view
        // root). The old firstChild-based logic threw a TypeError before
        // reaching document.documentElement.requestFullscreen(), so the shim
        // was never invoked and the success path never ran. Both assertions
        // therefore fail without the fix.
        Boolean called = (Boolean) ((JavascriptExecutor) getDriver())
                .executeScript("return window.__fsCalled === true;");
        Assert.assertTrue("requestFullscreen shim should have been invoked",
                called);

        // The view root stays visible here (there is nothing else to hide), so
        // the status text is reachable directly.
        waitUntil(d -> "ok".equals(status.getDomProperty("textContent")));
    }

    // Replace Element.prototype.requestFullscreen with a resolving shim so the
    // IT doesn't depend on the browser actually granting fullscreen (which
    // headless CI Chrome routinely denies).
    private void installResolvingFullscreenShim() {
        ((JavascriptExecutor) getDriver())
                .executeScript("window.__fsCalled = false;"
                        + "Element.prototype.requestFullscreen = function() {"
                        + "  window.__fsCalled = true;"
                        + "  return Promise.resolve();" + "};");
    }
}
