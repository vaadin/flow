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
    public void clickCopiesImage_emitsImagePngBlobIntoClipboardItem() {
        open();
        installResolvingClipboardShim();

        // Wait for the source <img> to finish loading so naturalWidth/
        // naturalHeight are populated before the canvas converter runs.
        waitUntil(
                d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                        "var i = document.getElementById('source-image');"
                                + "return i && i.complete && i.naturalWidth > 0;")));

        WebElement button = findElement(By.id("copy-image"));
        WebElement status = findElement(By.id("status"));

        button.click();

        Object written = waitUntil(d -> ((JavascriptExecutor) d)
                .executeScript("return window.__written;"));
        java.util.Map<?, ?> entries = (java.util.Map<?, ?>) written;
        java.util.Map<?, ?> imageEntry = (java.util.Map<?, ?>) entries
                .get("image/png");
        Assert.assertNotNull("image/png entry expected", imageEntry);
        Assert.assertEquals("image/png", imageEntry.get("type"));
        Assert.assertTrue("blob should have non-zero size",
                ((Number) imageEntry.get("size")).longValue() > 0);

        // Image-only write resolves with null per the dedicated image
        // constructor's contract; onCopied receives "null".
        waitUntil(d -> "ok:null".equals(status.getText()));
    }

    @Test
    public void clickCopiesTextAndImage_packsBothIntoOneClipboardItem() {
        open();
        installResolvingClipboardShim();

        waitUntil(
                d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                        "var i = document.getElementById('source-image');"
                                + "return i && i.complete && i.naturalWidth > 0;")));

        WebElement button = findElement(By.id("copy-multi-image"));
        WebElement status = findElement(By.id("status"));

        button.click();

        Object written = waitUntil(d -> ((JavascriptExecutor) d)
                .executeScript("return window.__written;"));
        java.util.Map<?, ?> entries = (java.util.Map<?, ?>) written;
        Assert.assertEquals(TriggerWriteToClipboardView.MULTI_TEXT,
                entries.get("text/plain"));
        java.util.Map<?, ?> imageEntry = (java.util.Map<?, ?>) entries
                .get("image/png");
        Assert.assertNotNull("image/png entry expected", imageEntry);
        Assert.assertEquals("image/png", imageEntry.get("type"));

        // text wins over image as the onCopied value.
        waitUntil(d -> ("ok:" + TriggerWriteToClipboardView.MULTI_TEXT)
                .equals(status.getText()));
    }

    @Test
    public void clickCopiesImageViaDownloadHandler_emitsImagePngBlob() {
        open();
        installResolvingClipboardShim();

        // The binding has attached a hidden <img> child to the host button;
        // wait for that one to load too.
        waitUntil(
                d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                        "var b = document.getElementById('copy-image-handler');"
                                + "var i = b && b.querySelector('img');"
                                + "return i && i.complete && i.naturalWidth > 0;")));

        WebElement button = findElement(By.id("copy-image-handler"));
        WebElement status = findElement(By.id("status"));

        button.click();

        Object written = waitUntil(d -> ((JavascriptExecutor) d)
                .executeScript("return window.__written;"));
        java.util.Map<?, ?> entries = (java.util.Map<?, ?>) written;
        java.util.Map<?, ?> imageEntry = (java.util.Map<?, ?>) entries
                .get("image/png");
        Assert.assertNotNull("image/png entry expected", imageEntry);
        Assert.assertEquals("image/png", imageEntry.get("type"));
        Assert.assertTrue("blob should have non-zero size",
                ((Number) imageEntry.get("size")).longValue() > 0);

        waitUntil(d -> "ok:null".equals(status.getText()));
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
        // ClipboardItem is stubbed to record its entries map. The shim handles
        // three value shapes: string values (text/plain and text/html, emitted
        // verbatim by the action), Blob values, and Promise<Blob> values (the
        // image/png slot, which is fed as a promise so navigator.clipboard
        // .write stays synchronous inside the user gesture). Promises and
        // Blobs are normalised to {type, size} so the assertions can inspect
        // the resulting blob without dealing with binary content.
        ((JavascriptExecutor) getDriver())
                .executeScript("window.__written = null;"
                        + "window.ClipboardItem = function(items) { return { items: items }; };"
                        + "Object.defineProperty(navigator, 'clipboard', {"
                        + "  configurable: true, value: {"
                        + "    write: async items => {"
                        + "      const entries = items[0].items;"
                        + "      const resolved = {};"
                        + "      for (const k of Object.keys(entries)) {"
                        + "        const v = entries[k];"
                        + "        if (typeof v === 'string') { resolved[k] = v; }"
                        + "        else if (v instanceof Blob) { resolved[k] = {type: v.type, size: v.size}; }"
                        + "        else { const b = await v; resolved[k] = {type: b.type, size: b.size}; }"
                        + "      }" + "      window.__written = resolved;"
                        + "    }" + "  }" + "});");
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
