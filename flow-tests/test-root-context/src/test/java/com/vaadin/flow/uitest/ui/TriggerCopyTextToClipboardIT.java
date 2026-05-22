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

public class TriggerCopyTextToClipboardIT extends ChromeBrowserTest {

    @Test
    public void clickCopiesValue_andServerReceivesCopiedTextOnSuccess() {
        open();
        installResolvingClipboardShim();

        WebElement field = findElement(By.id("source"));
        WebElement button = findElement(By.id("copy"));
        WebElement status = findElement(By.id("status"));

        ((JavascriptExecutor) getDriver()).executeScript(
                "arguments[0].value = 'hello clipboard';", field);

        button.click();

        Object copied = waitUntil(d -> ((JavascriptExecutor) d)
                .executeScript("return window.__copied;"));
        Assert.assertEquals("hello clipboard", copied);

        // onSuccess receives the copied string back from the resolved
        // promise — the server learns what actually reached the clipboard
        // even though the value came from a client-side PropertyInput.
        waitUntil(d -> "ok:hello clipboard".equals(status.getText()));
    }

    @Test
    public void clickCopiesStaticString_andTheLiteralRoundTripsVerbatim() {
        open();
        installResolvingClipboardShim();

        WebElement button = findElement(By.id("copy-static"));
        WebElement status = findElement(By.id("status"));

        button.click();

        // The Java-side literal (`hello "world"\n`) is JSON-encoded into the
        // emitted JS at build time, so the value the browser receives back
        // through the writeText shim is exactly the original String — quotes
        // and newline intact.
        Object copied = waitUntil(d -> ((JavascriptExecutor) d)
                .executeScript("return window.__copied;"));
        Assert.assertEquals(TriggerCopyTextToClipboardView.STATIC_TEXT, copied);
        waitUntil(d -> ("ok:" + TriggerCopyTextToClipboardView.STATIC_TEXT)
                .equals(status.getText()));
    }

    @Test
    public void writeTextRejection_propagatesAsFailureWithNameAndMessage() {
        open();
        installRejectingClipboardShim();

        WebElement button = findElement(By.id("copy"));
        WebElement status = findElement(By.id("status"));

        button.click();

        // The shim throws a DOMException with name "NotAllowedError" so the
        // server-side onError consumer sees both fields populated.
        waitUntil(d -> status.getText() != null
                && status.getText().startsWith("err:"));
        Assert.assertEquals("err:NotAllowedError:DeniedByTest",
                status.getText());
    }

    private void installResolvingClipboardShim() {
        ((JavascriptExecutor) getDriver())
                .executeScript("window.__copied = null;"
                        + "Object.defineProperty(navigator, 'clipboard', {"
                        + "  configurable: true, value: {"
                        + "    writeText: t => { window.__copied = t; return Promise.resolve(); }"
                        + "  }" + "});");
    }

    private void installRejectingClipboardShim() {
        ((JavascriptExecutor) getDriver())
                .executeScript("Object.defineProperty(navigator, 'clipboard', {"
                        + "  configurable: true, value: {"
                        + "    writeText: t => Promise.reject("
                        + "      new DOMException('DeniedByTest', 'NotAllowedError'))"
                        + "  }" + "});");
    }
}
