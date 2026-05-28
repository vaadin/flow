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

public class TriggerWriteToClipboardIT extends ChromeBrowserTest {

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

        // The ClipboardItem the browser would have received carries the
        // current input value as text/plain.
        Object written = waitUntil(d -> ((JavascriptExecutor) d)
                .executeScript("return window.__written;"));
        Assert.assertEquals("hello clipboard",
                ((java.util.Map<?, ?>) written).get("text/plain"));

        // onCopied receives the same string back from the resolved promise.
        waitUntil(d -> "ok:hello clipboard".equals(status.getText()));
    }

    @Test
    public void clickCopiesStaticString_andTheLiteralRoundTripsVerbatim() {
        open();
        installResolvingClipboardShim();

        WebElement button = findElement(By.id("copy-static"));

        button.click();

        // The Java-side literal (`hello "world"\n`) is JSON-encoded into the
        // emitted JS at build time, so the value the browser receives back
        // through the write shim is exactly the original String — quotes
        // and newline intact.
        Object written = waitUntil(d -> ((JavascriptExecutor) d)
                .executeScript("return window.__written;"));
        Assert.assertEquals(TriggerWriteToClipboardView.STATIC_TEXT,
                ((java.util.Map<?, ?>) written).get("text/plain"));
        // STATIC_TEXT ends with `\n`; read raw textContent (Selenium's
        // getText() collapses/trims whitespace and would drop it).
        waitUntil(d -> ("ok:" + TriggerWriteToClipboardView.STATIC_TEXT)
                .equals(((JavascriptExecutor) d).executeScript(
                        "return document.getElementById('status').textContent;")));
    }

    @Test
    public void clickCopiesTextAndHtml_intoOneClipboardItem_resolvingWithText() {
        open();
        installResolvingClipboardShim();

        WebElement button = findElement(By.id("copy-multi"));
        WebElement status = findElement(By.id("status"));

        button.click();

        // The single ClipboardItem carries both MIME types.
        Object written = waitUntil(d -> ((JavascriptExecutor) d)
                .executeScript("return window.__written;"));
        java.util.Map<?, ?> entries = (java.util.Map<?, ?>) written;
        Assert.assertEquals(TriggerWriteToClipboardView.MULTI_TEXT,
                entries.get("text/plain"));
        Assert.assertEquals(TriggerWriteToClipboardView.MULTI_HTML,
                entries.get("text/html"));

        // onCopied resolves with the text/plain value (text wins over html).
        waitUntil(d -> ("ok:" + TriggerWriteToClipboardView.MULTI_TEXT)
                .equals(status.getText()));
    }

    @Test
    public void clickCopiesSignalValue_andTracksSignalAfterChange() {
        open();
        installResolvingClipboardShim();

        WebElement copySignal = findElement(By.id("copy-signal"));
        WebElement changeSignal = findElement(By.id("change-signal"));
        WebElement signalDisplay = findElement(By.id("signal-value"));
        WebElement status = findElement(By.id("status"));

        // The bound span renders the signal value via bindText; wait for the
        // initial render to land so we know the SignalInput's mirror has
        // also reached the client.
        waitUntil(d -> TriggerWriteToClipboardView.SIGNAL_INITIAL_TEXT
                .equals(signalDisplay.getText()));

        copySignal.click();

        Object written = waitUntil(d -> ((JavascriptExecutor) d)
                .executeScript("return window.__written;"));
        Assert.assertEquals(TriggerWriteToClipboardView.SIGNAL_INITIAL_TEXT,
                ((java.util.Map<?, ?>) written).get("text/plain"));
        waitUntil(d -> ("ok:" + TriggerWriteToClipboardView.SIGNAL_INITIAL_TEXT)
                .equals(status.getText()));

        // Mutate the signal from the server. Both the bound span and the
        // SignalInput's mirrored property update from the same effect pass,
        // so the span's text is a deterministic sync point for the next
        // copy click.
        ((JavascriptExecutor) getDriver())
                .executeScript("window.__written = null;");
        changeSignal.click();
        waitUntil(d -> TriggerWriteToClipboardView.SIGNAL_UPDATED_TEXT
                .equals(signalDisplay.getText()));

        copySignal.click();

        Object writtenAgain = waitUntil(d -> ((JavascriptExecutor) d)
                .executeScript("return window.__written;"));
        Assert.assertEquals(TriggerWriteToClipboardView.SIGNAL_UPDATED_TEXT,
                ((java.util.Map<?, ?>) writtenAgain).get("text/plain"));
        waitUntil(d -> ("ok:" + TriggerWriteToClipboardView.SIGNAL_UPDATED_TEXT)
                .equals(status.getText()));
    }

    @Test
    public void writeRejection_propagatesAsFailureWithNameAndMessage() {
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
        // ClipboardItem is stubbed to record its entries map (the WriteAction
        // emits string values for text/plain and text/html). navigator
        // .clipboard.write resolves immediately and stores the first item's
        // entries on window.__written for the assertions to read.
        ((JavascriptExecutor) getDriver())
                .executeScript("window.__written = null;"
                        + "window.ClipboardItem = function(items) { return { items: items }; };"
                        + "Object.defineProperty(navigator, 'clipboard', {"
                        + "  configurable: true, value: {"
                        + "    write: items => { window.__written = items[0].items; return Promise.resolve(); }"
                        + "  }" + "});");
    }

    private void installRejectingClipboardShim() {
        ((JavascriptExecutor) getDriver()).executeScript(
                "window.ClipboardItem = function(items) { return { items: items }; };"
                        + "Object.defineProperty(navigator, 'clipboard', {"
                        + "  configurable: true, value: {"
                        + "    write: items => Promise.reject("
                        + "      new DOMException('DeniedByTest', 'NotAllowedError'))"
                        + "  }" + "});");
    }
}
