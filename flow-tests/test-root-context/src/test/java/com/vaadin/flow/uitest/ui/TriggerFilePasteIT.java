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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TriggerFilePasteIT extends ChromeBrowserTest {

    @Test
    public void pasteTextFile_serverShowsPlainText() {
        open();
        dispatchPasteWithFiles(
                new SyntheticFile("hello.txt", "text/plain", "Hello, paste!"));
        waitFileCount(1);
        Assert.assertEquals("Hello, paste!",
                findElement(By.cssSelector("#file-status .file-slot"))
                        .getText());
    }

    @Test
    public void pasteSession_firesStartFileAndComplete() {
        // Verifies the full session shape end-to-end: onStart adds a
        // .paste-banner div, onFile renders each slot, and onComplete bumps
        // #completed-count. After two files in one paste, expect 1 banner,
        // 2 slots, 1 completed session.
        open();
        dispatchPasteWithFiles(
                new SyntheticFile("a.txt", "text/plain", "first"),
                new SyntheticFile("b.txt", "text/plain", "second"));
        waitFileCount(2);
        waitCompletedCount(1);
        Assert.assertEquals(1,
                findElements(By.cssSelector("#file-status .paste-banner"))
                        .size());
        Assert.assertEquals(2,
                findElements(By.cssSelector("#file-status .file-slot")).size());
    }

    @Test
    public void pasteHtmlFile_serverRendersHtml() {
        // The HTML body is wrapped in a single root element so the server-side
        // Html component accepts it. Verifying that <b> exists inside
        // #file-status with the right text content proves the bytes were
        // parsed as HTML rather than escaped as text.
        open();
        dispatchPasteWithFiles(new SyntheticFile("snippet.html", "text/html",
                "<div><b>bold</b> and <i>italic</i></div>"));
        waitFileCount(1);
        WebElement bold = findElement(By.cssSelector("#file-status b"));
        Assert.assertEquals("bold", bold.getText());
        WebElement italic = findElement(By.cssSelector("#file-status i"));
        Assert.assertEquals("italic", italic.getText());
    }

    @Test
    public void pasteImageFile_serverShowsImage() {
        // Any bytes will do — the view encodes them as a data URL and the IT
        // only checks the src prefix and the MIME type, not the pixels.
        open();
        dispatchPasteBinaryFile("pixel.png", "image/png",
                new int[] { 137, 80, 78, 71, 13, 10, 26, 10 });
        waitFileCount(1);
        WebElement img = findElement(
                By.cssSelector("#file-status .file-slot-image"));
        String src = img.getAttribute("src");
        Assert.assertTrue("expected data: URL, got " + src,
                src.startsWith("data:image/png;base64,"));
    }

    @Test
    public void twoSequentialPastes_bothSessionsComplete() {
        // Each paste runs its own session: the second paste must not cancel
        // the first. After two single-file pastes we expect two banners,
        // two slots, and two completed sessions; the framework never drops
        // files belonging to an earlier paste.
        open();
        dispatchPasteWithFiles(
                new SyntheticFile("first.txt", "text/plain", "FIRST"));
        waitCompletedCount(1);
        dispatchPasteWithFiles(
                new SyntheticFile("second.txt", "text/plain", "SECOND"));
        waitCompletedCount(2);

        Assert.assertEquals(2,
                findElements(By.cssSelector("#file-status .paste-banner"))
                        .size());
        List<String> rendered = findElements(
                By.cssSelector("#file-status .file-slot")).stream()
                .map(WebElement::getText).toList();
        Assert.assertEquals(Arrays.asList("FIRST", "SECOND"), rendered);
    }

    @Test
    public void pasteMultipleFiles_allFilesRendered() {
        // Each pasted file fires its own fetch POST and its own onFile
        // callback; the session handler accumulates them under one onStart
        // and fires onComplete once all have arrived. Completion order
        // isn't guaranteed, so the assertions read the slot set as a set.
        open();
        dispatchPasteWithFiles(new SyntheticFile("a.txt", "text/plain", "AAAA"),
                new SyntheticFile("b.txt", "text/plain", "BBBB"),
                new SyntheticFile("c.txt", "text/plain", "CCCC"));
        waitFileCount(3);
        List<String> rendered = findElements(
                By.cssSelector("#file-status .file-slot")).stream()
                .map(WebElement::getText).toList();
        Assert.assertEquals(3, rendered.size());
        Assert.assertEquals(
                new HashSet<>(Arrays.asList("AAAA", "BBBB", "CCCC")),
                new HashSet<>(rendered));
    }

    @Test
    public void pasteWithoutFiles_uploadHandlerNotInvoked() {
        // A plain text paste must not fire the file-paste filter's upload
        // code: the in-memory callback would bump the counter past zero.
        open();
        ((JavascriptExecutor) getDriver()).executeScript("""
                const el = document.getElementById('target');
                el.focus();
                const dt = new DataTransfer();
                dt.setData('text/plain', 'just text');
                el.dispatchEvent(new ClipboardEvent('paste', {
                    clipboardData: dt, bubbles: true, cancelable: true
                }));
                """);
        // No reliable positive signal to wait on — give the page a moment to
        // settle and then assert the counter never moved off zero.
        getCommandExecutor().waitForVaadin();
        Assert.assertEquals("0", findElement(By.id("file-count")).getText());
    }

    private void waitFileCount(int expected) {
        waitUntil(d -> {
            String text = findElement(By.id("file-count")).getText();
            return Integer.toString(expected).equals(text);
        });
    }

    private void waitCompletedCount(int expected) {
        waitUntil(d -> {
            String text = findElement(By.id("completed-count")).getText();
            return Integer.toString(expected).equals(text);
        });
    }

    // Dispatches a paste containing a single binary file built from the given
    // byte values. JavaScript's File ctor accepts a Uint8Array as the first
    // argument, so we hand the bytes over verbatim through executeScript and
    // wrap them client-side. Used by the image case to skip the
    // String-roundtrip that would otherwise mangle binary payloads.
    private void dispatchPasteBinaryFile(String name, String type,
            int[] bytes) {
        String script = """
                const bytes = new Uint8Array(arguments[0]);
                const el = document.getElementById('target');
                el.focus();
                const dt = new DataTransfer();
                dt.items.add(new File([bytes], arguments[1], { type: arguments[2] }));
                el.dispatchEvent(new ClipboardEvent('paste', {
                    clipboardData: dt, bubbles: true, cancelable: true
                }));
                """;
        // Selenium's executeScript serialises ints as numbers; the JS side
        // builds the Uint8Array from the resulting array.
        Object[] boxed = new Object[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            boxed[i] = bytes[i];
        }
        ((JavascriptExecutor) getDriver()).executeScript(script,
                java.util.Arrays.asList(boxed), name, type);
    }

    private record SyntheticFile(String name, String type, String body) {
    }

    // Builds a ClipboardEvent('paste') whose DataTransfer carries one
    // synthetic File per argument, and dispatches it on the #target div.
    // Files are constructed via the standard `new File([body], name, {type})`
    // browser API; DataTransfer.items.add(file) makes them visible on
    // event.clipboardData.files.
    private void dispatchPasteWithFiles(SyntheticFile... files) {
        StringBuilder script = new StringBuilder("""
                const el = document.getElementById('target');
                el.focus();
                const dt = new DataTransfer();
                """);
        for (int i = 0; i < files.length; i++) {
            script.append("dt.items.add(new File([arguments[").append(i * 3)
                    .append("]], arguments[").append(i * 3 + 1)
                    .append("], { type: arguments[").append(i * 3 + 2)
                    .append("] }));\n");
        }
        script.append("""
                el.dispatchEvent(new ClipboardEvent('paste', {
                    clipboardData: dt, bubbles: true, cancelable: true
                }));
                """);

        Object[] args = new Object[files.length * 3];
        for (int i = 0; i < files.length; i++) {
            args[i * 3] = files[i].body();
            args[i * 3 + 1] = files[i].name();
            args[i * 3 + 2] = files[i].type();
        }
        ((JavascriptExecutor) getDriver()).executeScript(script.toString(),
                args);
    }
}
