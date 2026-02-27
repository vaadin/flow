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
package com.vaadin.flow.component.page;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;

public class ClipboardTest {

    @Tag("button")
    private static class TestButton extends Component {
    }

    @Tag("input")
    private static class TestTextField extends Component {
    }

    @Tag("div")
    private static class TestDiv extends Component {
    }

    private MockUI ui;

    @Before
    public void setUp() {
        ui = new MockUI();
    }

    @After
    public void tearDown() {
        ui = null;
    }

    // --- copyOnClick(Component, String) ---

    @Test
    public void copyOnClick_setsPropertyAndInstallsHandler() {
        TestButton button = new TestButton();
        ui.add(button);

        ClipboardCopy handle = Clipboard.copyOnClick(button, "test text");

        Assert.assertNotNull(handle);
        Assert.assertEquals("test text", button.getElement()
                .getProperty(ClipboardCopy.CLIPBOARD_TEXT_PROPERTY));

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assert.assertFalse("Should have JS invocations", invocations.isEmpty());

        String js = invocations.get(invocations.size() - 1).getInvocation()
                .getExpression();
        Assert.assertTrue("Should call setupCopyOnClick",
                js.contains("clipboard.setupCopyOnClick"));
    }

    @Test
    public void copyOnClick_nullText_setsEmptyProperty() {
        TestButton button = new TestButton();
        ui.add(button);

        Clipboard.copyOnClick(button, (String) null);

        Assert.assertEquals("", button.getElement()
                .getProperty(ClipboardCopy.CLIPBOARD_TEXT_PROPERTY));
    }

    @Test(expected = NullPointerException.class)
    public void copyOnClick_nullTrigger_throws() {
        Clipboard.copyOnClick(null, "text");
    }

    @Test
    public void copyOnClick_remove_executesCleanupJs() {
        TestButton button = new TestButton();
        ui.add(button);

        ClipboardCopy handle = Clipboard.copyOnClick(button, "text");
        // Drain setup invocations
        ui.dumpPendingJsInvocations();

        handle.remove();

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assert.assertFalse("Should have cleanup JS invocations",
                invocations.isEmpty());
        String js = invocations.get(0).getInvocation().getExpression();
        Assert.assertTrue("Should call cleanupCopyOnClick",
                js.contains("clipboard.cleanupCopyOnClick"));
    }

    @Test
    public void copyOnClick_updateValue_changesProperty() {
        TestButton button = new TestButton();
        ui.add(button);

        ClipboardCopy handle = Clipboard.copyOnClick(button, "initial");

        Assert.assertEquals("initial", button.getElement()
                .getProperty(ClipboardCopy.CLIPBOARD_TEXT_PROPERTY));

        handle.setValue("updated");

        Assert.assertEquals("updated", button.getElement()
                .getProperty(ClipboardCopy.CLIPBOARD_TEXT_PROPERTY));
    }

    // --- copyOnClick(Component, String, Command, Command) ---

    @Test
    public void copyOnClickWithCallbacks_installsHandlerWithChannels() {
        TestButton button = new TestButton();
        ui.add(button);

        AtomicBoolean successCalled = new AtomicBoolean(false);
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        ClipboardCopy handle = Clipboard.copyOnClick(button, "text",
                () -> successCalled.set(true), () -> errorCalled.set(true));

        Assert.assertNotNull(handle);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assert.assertFalse(invocations.isEmpty());

        // Find the invocation that sets up the click handler
        boolean foundClipboardSetup = invocations.stream()
                .anyMatch(inv -> inv.getInvocation().getExpression()
                        .contains("clipboard.setupCopyOnClickWithCallbacks"));
        Assert.assertTrue("Should call setupCopyOnClickWithCallbacks",
                foundClipboardSetup);
    }

    // --- copyOnClick(Component, Component) ---

    @Test
    public void copyOnClickWithSource_installsHandler() {
        TestButton button = new TestButton();
        TestTextField source = new TestTextField();
        ui.add(button);
        ui.add(source);

        ClipboardCopy handle = Clipboard.copyOnClick(button, source);

        Assert.assertNotNull(handle);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assert.assertFalse(invocations.isEmpty());

        boolean foundSourceSetup = invocations.stream()
                .anyMatch(inv -> inv.getInvocation().getExpression()
                        .contains("clipboard.setupCopyOnClickFromSource"));
        Assert.assertTrue("Should call setupCopyOnClickFromSource",
                foundSourceSetup);
    }

    @Test(expected = NullPointerException.class)
    public void copyOnClickWithSource_nullSource_throws() {
        TestButton button = new TestButton();
        Clipboard.copyOnClick(button, (Component) null);
    }

    // --- writeText ---

    @Test
    public void writeText_executesJs() {
        PendingJavaScriptResult result = Clipboard.writeText(ui, "hello");

        Assert.assertNotNull(result);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assert.assertFalse(invocations.isEmpty());

        PendingJavaScriptInvocation invocation = invocations
                .get(invocations.size() - 1);
        String js = invocation.getInvocation().getExpression();
        Assert.assertTrue("Should call clipboard.writeText",
                js.contains("clipboard.writeText($0)"));
    }

    // --- readText ---

    @Test
    public void readText_executesJs() {
        AtomicReference<String> result = new AtomicReference<>();
        Clipboard.readText(ui, result::set);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assert.assertFalse(invocations.isEmpty());

        boolean foundReadText = invocations.stream()
                .anyMatch(inv -> inv.getInvocation().getExpression()
                        .contains("clipboard.readText()"));
        Assert.assertTrue("Should call readText", foundReadText);
    }

    @Test(expected = NullPointerException.class)
    public void readText_nullCallback_throws() {
        Clipboard.readText(ui, null);
    }

    // --- writeImage ---

    @Test
    public void writeImage_withUrl_executesJs() {
        PendingJavaScriptResult result = Clipboard.writeImage(ui,
                "/images/chart.png");

        Assert.assertNotNull(result);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assert.assertFalse(invocations.isEmpty());

        boolean foundWriteImage = invocations.stream()
                .anyMatch(inv -> inv.getInvocation().getExpression()
                        .contains("clipboard.writeImage($0)"));
        Assert.assertTrue("Should call clipboard.writeImage", foundWriteImage);
    }

    // --- addCopyListener ---

    @Test
    public void addCopyListener_registersEventListener() {
        TestDiv div = new TestDiv();
        ui.add(div);

        AtomicReference<ClipboardEvent> eventRef = new AtomicReference<>();
        Registration reg = Clipboard.addCopyListener(div, eventRef::set);

        Assert.assertNotNull(reg);
    }

    @Test
    public void addCutListener_registersEventListener() {
        TestDiv div = new TestDiv();
        ui.add(div);

        AtomicReference<ClipboardEvent> eventRef = new AtomicReference<>();
        Registration reg = Clipboard.addCutListener(div, eventRef::set);

        Assert.assertNotNull(reg);
    }

    @Test(expected = NullPointerException.class)
    public void addCopyListener_nullTarget_throws() {
        Clipboard.addCopyListener(null, event -> {
        });
    }

    @Test(expected = NullPointerException.class)
    public void addCopyListener_nullListener_throws() {
        TestDiv div = new TestDiv();
        Clipboard.addCopyListener(div, null);
    }

    // --- addPasteListener ---
    // Note: Full integration tests for addPasteListener require a session
    // with a real StreamResourceRegistry (needed by setAttribute with
    // ElementRequestHandler). The PasteState coordination and null-argument
    // validation are tested below.

    @Test(expected = NullPointerException.class)
    public void addPasteListener_nullTarget_throws() {
        Clipboard.addPasteListener(null, event -> {
        });
    }

    @Test(expected = NullPointerException.class)
    public void addPasteListener_nullListener_throws() {
        TestDiv div = new TestDiv();
        Clipboard.addPasteListener(div, null);
    }

    // --- PasteState coordination ---

    @Test
    public void pasteState_noFiles_dispatchesImmediately() {
        AtomicReference<ClipboardEvent> eventRef = new AtomicReference<>();
        Clipboard.PasteState state = new Clipboard.PasteState(eventRef::set);

        state.setTextData("hello", "<b>hello</b>", 0);

        Assert.assertNotNull(eventRef.get());
        Assert.assertEquals("paste", eventRef.get().getType());
        Assert.assertEquals("hello", eventRef.get().getText());
        Assert.assertEquals("<b>hello</b>", eventRef.get().getHtml());
        Assert.assertTrue(eventRef.get().getFiles().isEmpty());
    }

    @Test
    public void pasteState_withFiles_waitsForAllFiles() {
        AtomicReference<ClipboardEvent> eventRef = new AtomicReference<>();
        Clipboard.PasteState state = new Clipboard.PasteState(eventRef::set);

        state.setTextData("text", null, 2);
        Assert.assertNull("Should not dispatch yet", eventRef.get());

        state.addFile(new ClipboardFile("file1.png", "image/png", 100,
                new byte[100]));
        Assert.assertNull("Should not dispatch yet", eventRef.get());

        state.addFile(new ClipboardFile("file2.jpg", "image/jpeg", 200,
                new byte[200]));
        Assert.assertNotNull("Should dispatch after all files arrived",
                eventRef.get());
        Assert.assertEquals(2, eventRef.get().getFiles().size());
        Assert.assertEquals("text", eventRef.get().getText());
    }

    @Test
    public void pasteState_filesArriveBefore_textData() {
        AtomicReference<ClipboardEvent> eventRef = new AtomicReference<>();
        Clipboard.PasteState state = new Clipboard.PasteState(eventRef::set);

        // Files arrive first (before setTextData is called)
        state.addFile(
                new ClipboardFile("f.txt", "text/plain", 10, new byte[10]));
        Assert.assertNull("Should not dispatch without text data",
                eventRef.get());

        // Text data arrives, but it resets the file list
        state.setTextData("text", null, 1);

        // The file was added before setTextData, which clears the list,
        // so it's not counted. We need to add it again.
        Assert.assertNull("File added before setTextData was cleared",
                eventRef.get());

        state.addFile(
                new ClipboardFile("f.txt", "text/plain", 10, new byte[10]));
        Assert.assertNotNull("Should dispatch now", eventRef.get());
    }
}
