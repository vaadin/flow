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

public class TriggerPasteIT extends ChromeBrowserTest {

    @Test
    public void pasteWithTextOnly_serverReceivesText() {
        open();
        dispatchPaste("hello", null);
        WebElement status = waitForStatus();
        Assert.assertEquals("text=hello;html=null", status.getText());
    }

    @Test
    public void pasteWithHtmlOnly_serverReceivesHtml() {
        open();
        dispatchPaste(null, "<b>hi</b>");
        WebElement status = waitForStatus();
        Assert.assertEquals("text=null;html=<b>hi</b>", status.getText());
    }

    @Test
    public void pasteWithBoth_serverReceivesBoth() {
        open();
        dispatchPaste("hi", "<b>hi</b>");
        WebElement status = waitForStatus();
        Assert.assertEquals("text=hi;html=<b>hi</b>", status.getText());
    }

    @Test
    public void pasteWithNeither_serverReceivesBothNull() {
        open();
        dispatchPaste(null, null);
        WebElement status = waitForStatus();
        Assert.assertEquals("text=null;html=null", status.getText());
    }

    @Test
    public void pasteWithEmptyText_serverReceivesNull() {
        // The browser returns "" both when the MIME type is absent and when an
        // empty string was pasted. Clipboard.onPaste collapses "" into null at
        // the JS expression boundary, so an explicitly-empty paste must arrive
        // server-side as text=null, not text=.
        open();
        dispatchPaste("", null);
        WebElement status = waitForStatus();
        Assert.assertEquals("text=null;html=null", status.getText());
    }

    @Test
    public void pasteOnDiv_globalListenersAlsoFire() {
        // Pasting on the focusable target div fires the component-scoped
        // listener AND both global listeners; the default global skips
        // editable targets but a div is not editable, so it passes.
        open();
        dispatchPaste("hi", null);
        waitForStatus();
        Assert.assertEquals("target=target;text=hi",
                findElement(By.id("status-global")).getText());
        Assert.assertEquals("target=target;text=hi",
                findElement(By.id("status-global-include")).getText());
    }

    @Test
    public void pasteOnInput_defaultGlobalSkipped_includeGlobalFires() {
        // Pasting on the input bypasses the component listener (input is
        // outside the target div's subtree), is filtered out by the default
        // global listener (composed path contains <input>), and fires only on
        // the include-input global. Wait on the one that fires, then verify
        // the other two stayed empty in the same client roundtrip.
        open();
        dispatchPasteOn("input", "hi", null);
        waitUntil(d -> "target=input;text=hi"
                .equals(findElement(By.id("status-global-include")).getText()));
        Assert.assertEquals("", findElement(By.id("status-global")).getText());
        Assert.assertEquals("", findElement(By.id("status")).getText());
    }

    private WebElement waitForStatus() {
        WebElement status = findElement(By.id("status"));
        waitUntil(d -> status.getText() != null
                && status.getText().startsWith("text="));
        return status;
    }

    private void dispatchPaste(String text, String html) {
        dispatchPasteOn("target", text, html);
    }

    // Builds a synthetic ClipboardEvent('paste') with a populated DataTransfer
    // and dispatches it on the element with the given id. Java nulls map to JS
    // null via executeScript, matching the `!== null` guards.
    private void dispatchPasteOn(String targetId, String text, String html) {
        String script = """
                const el = document.getElementById(arguments[2]);
                el.focus();
                const dt = new DataTransfer();
                if (arguments[0] !== null) dt.setData('text/plain', arguments[0]);
                if (arguments[1] !== null) dt.setData('text/html',  arguments[1]);
                el.dispatchEvent(new ClipboardEvent('paste', {
                    clipboardData: dt, bubbles: true, cancelable: true
                }));
                """;
        ((JavascriptExecutor) getDriver()).executeScript(script, text, html,
                targetId);
    }
}
