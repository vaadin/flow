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

public class TriggerRequestFullscreenIT extends ChromeBrowserTest {

    @Test
    public void clickRequestsFullscreen_andServerReceivesSuccess() {
        open();
        installResolvingFullscreenShim();

        WebElement button = findElement(By.id("go"));
        WebElement status = findElement(By.id("status"));

        button.click();

        Boolean called = (Boolean) ((JavascriptExecutor) getDriver())
                .executeScript("return window.__fsCalled === true;");
        Assert.assertTrue("requestFullscreen shim should have been invoked",
                called);

        waitUntil(d -> "ok".equals(status.getText()));
    }

    @Test
    public void requestFullscreenRejection_propagatesAsFailureWithNameAndMessage() {
        open();
        installRejectingFullscreenShim();

        WebElement button = findElement(By.id("go"));
        WebElement status = findElement(By.id("status"));

        button.click();

        // The shim rejects with a DOMException(name="NotAllowedError",
        // message="DeniedByTest") — both fields reach the server via the
        // PromiseAction.Error record.
        waitUntil(d -> status.getText() != null
                && status.getText().startsWith("err:"));
        Assert.assertEquals("err:NotAllowedError:DeniedByTest",
                status.getText());
    }

    // Replace Element.prototype.requestFullscreen with a resolving shim so the
    // IT doesn't depend on the browser actually granting fullscreen (which
    // headless CI Chrome routinely denies). The wrapped action calls
    // document.documentElement.requestFullscreen() under the hood, so shimming
    // the prototype covers both component and page modes.
    private void installResolvingFullscreenShim() {
        ((JavascriptExecutor) getDriver())
                .executeScript("window.__fsCalled = false;"
                        + "Element.prototype.requestFullscreen = function() {"
                        + "  window.__fsCalled = true;"
                        + "  return Promise.resolve();" + "};");
    }

    private void installRejectingFullscreenShim() {
        ((JavascriptExecutor) getDriver()).executeScript(
                "Element.prototype.requestFullscreen = function() {"
                        + "  return Promise.reject("
                        + "    new DOMException('DeniedByTest', 'NotAllowedError'));"
                        + "};");
    }
}
