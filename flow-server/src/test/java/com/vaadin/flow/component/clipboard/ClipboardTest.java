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
package com.vaadin.flow.component.clipboard;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipboardTest {

    @Tag("test-button")
    static final class TestButton extends Component
            implements ClickNotifier<TestButton> {
    }

    @Tag("test-field")
    static final class TestField extends AbstractField<TestField, String> {
        TestField() {
            super("");
        }

        @Override
        protected void setPresentationValue(String newPresentationValue) {
            // not exercised in these tests
        }
    }

    @Tag("img")
    static final class TestImage extends Component {
    }

    @Test
    void onClick_installsClickTrigger() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        Clipboard.onClick(button).writeText("Hello");

        // ClickTrigger captures the event name as install $1.
        assertEquals("click", installFn(ui).getCaptures().get(1));
    }

    @Test
    void writeText_literal_capturesLiteralAsTextInput() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        Clipboard.onClick(button).writeText("Hello");

        // Action calls the TS helper; literal is captured by the text input
        // JsFunction at action $0.
        JsFunction action = actionFn(ui);
        assertTrue(action.getBody().contains("writePayload"), action.getBody());
        JsFunction textInput = (JsFunction) action.getCaptures().get(0);
        assertEquals("Hello", textInput.getCaptures().get(0));
    }

    @Test
    void writeText_hasValue_emitsPropertyInputForValue() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        TestField field = new TestField();
        ui.getElement().appendChild(button.getElement(), field.getElement());

        Clipboard.onClick(button).writeText(field);

        // PropertyInput renders as `return $0[$1]` and captures (element,
        // "value").
        JsFunction textInput = (JsFunction) actionFn(ui).getCaptures().get(0);
        assertEquals("return $0[$1]", textInput.getBody());
        assertSame(field.getElement(), textInput.getCaptures().get(0));
        assertEquals("value", textInput.getCaptures().get(1));
    }

    @Test
    void writeHtml_literal_capturesLiteralAsHtmlInput() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        Clipboard.onClick(button).writeHtml("<b>Hi</b>");

        // The html input is at action $1.
        JsFunction htmlInput = (JsFunction) actionFn(ui).getCaptures().get(1);
        assertEquals("<b>Hi</b>", htmlInput.getCaptures().get(0));
    }

    @Test
    void write_multiFormat_packsBothTextAndHtml() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        Clipboard.onClick(button).write(
                ClipboardContent.create().text("plain").html("<b>html</b>"));

        JsFunction action = actionFn(ui);
        JsFunction textInput = (JsFunction) action.getCaptures().get(0);
        JsFunction htmlInput = (JsFunction) action.getCaptures().get(1);
        assertEquals("plain", textInput.getCaptures().get(0));
        assertEquals("<b>html</b>", htmlInput.getCaptures().get(0));
    }

    @Test
    void writeImage_capturesImageElementAsImageInput() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        TestImage image = new TestImage();
        ui.getElement().appendChild(button.getElement(), image.getElement());

        Clipboard.onClick(button).writeImage(image);

        // ImageBlobInput renders as `return $0` and captures the image
        // element. The action puts it at slot 2.
        JsFunction imageInput = (JsFunction) actionFn(ui).getCaptures().get(2);
        assertEquals("return $0", imageInput.getBody());
        assertSame(image.getElement(), imageInput.getCaptures().get(0));
    }

    @Test
    void write_multiFormat_packsAllThreeSlots() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        TestImage image = new TestImage();
        ui.getElement().appendChild(button.getElement(), image.getElement());

        Clipboard.onClick(button).write(ClipboardContent.create().text("plain")
                .html("<b>html</b>").image(image));

        JsFunction action = actionFn(ui);
        assertEquals("plain", ((JsFunction) action.getCaptures().get(0))
                .getCaptures().get(0));
        assertEquals("<b>html</b>", ((JsFunction) action.getCaptures().get(1))
                .getCaptures().get(0));
        assertSame(image.getElement(),
                ((JsFunction) action.getCaptures().get(2)).getCaptures()
                        .get(0));
    }

    @Test
    void write_contentTextFromHasValue_emitsPropertyInputForValue() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        TestField field = new TestField();
        ui.getElement().appendChild(button.getElement(), field.getElement());

        Clipboard.onClick(button).write(ClipboardContent.create().text(field));

        JsFunction textInput = (JsFunction) actionFn(ui).getCaptures().get(0);
        assertEquals("return $0[$1]", textInput.getBody());
        assertEquals("value", textInput.getCaptures().get(1));
    }

    @Test
    void write_emptyContent_throws() {
        TestButton button = new TestButton();
        assertThrows(IllegalArgumentException.class, () -> Clipboard
                .onClick(button).write(ClipboardContent.create()));
    }

    @Test
    void writeImage_nonImgComponent_throws() {
        TestButton button = new TestButton();
        // TestButton's root is <test-button>, not <img>.
        assertThrows(IllegalArgumentException.class,
                () -> Clipboard.onClick(button).writeImage(button));
    }

    @Test
    void onPaste_dispatchesPasteEventWithTextAndHtml() {
        UI ui = new MockUI();
        TestButton target = new TestButton();
        ui.getElement().appendChild(target.getElement());

        AtomicReference<PasteEvent> received = new AtomicReference<>();
        Clipboard.onPaste(target, received::set);

        // Must match the JS expressions in PasteEventDispatcher byte-for-byte
        // — DomListenerRegistration uses the expression string as the JSON key
        // under which the evaluated result appears in DomEvent#getEventData().
        String textExpr = "window.Vaadin.Flow.clipboard.pasteEventText(event)";
        String htmlExpr = "window.Vaadin.Flow.clipboard.pasteEventHtml(event)";
        ObjectNode data = JacksonUtils.createObjectNode();
        data.put(textExpr, "plain");
        data.put(htmlExpr, "<b>html</b>");

        target.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(target.getElement(), "paste", data));

        PasteEvent event = received.get();
        assertNotNull(event);
        assertSame(target, event.getSource());
        assertEquals("plain", event.getText());
        assertEquals("<b>html</b>", event.getHtml());
        assertTrue(event.hasText());
        assertTrue(event.hasHtml());
        // No target mapping in the fabricated event data, so the resolver
        // returns null. The IT verifies real target resolution end-to-end.
        assertNull(event.getTargetElement());
    }

    @Test
    void onPaste_options_attachesToHostElement_withoutCurrentUI() {
        // The Component-accepting onPaste must not rely on UI.getCurrent(),
        // so callers from a background thread can pass the UI (or any
        // component) directly. MockUI's constructor sets UI.setCurrent as a
        // side effect; clear it so the test only exercises the explicit
        // component path. PasteOptions.includingInputFields() skips the
        // editable-target filter, so the fabricated event doesn't need to
        // carry the filter key — the filter behaviour is covered end-to-end
        // by TriggerPasteIT.
        UI ui = new MockUI();
        UI.setCurrent(null);

        AtomicReference<PasteEvent> received = new AtomicReference<>();
        Clipboard.onPaste(ui, PasteOptions.includingInputFields(),
                received::set);

        String textExpr = "window.Vaadin.Flow.clipboard.pasteEventText(event)";
        ObjectNode data = JacksonUtils.createObjectNode();
        data.put(textExpr, "from-bg");

        ui.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(ui.getElement(), "paste", data));

        PasteEvent event = received.get();
        assertNotNull(event);
        assertSame(ui, event.getSource());
        assertEquals("from-bg", event.getText());
    }

    @Test
    void read_emitsReadFromClipboardAction() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        Clipboard.onClick(button).read(p -> {
        }, err -> {
        });

        // Observed PromiseAction wraps the inner call; the inner JsFunction
        // delegates to the TS readPayload helper.
        JsFunction action = actionFn(ui);
        JsFunction inner = (JsFunction) action.getCaptures().get(1);
        assertEquals("return window.Vaadin.Flow.clipboard.readPayload()",
                inner.getBody());
    }

    @Test
    void readText_adaptsPayloadConsumerToTextField() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        List<@Nullable String> received = new ArrayList<>();
        Clipboard.onClick(button).readText(received::add, err -> {
        });

        // Capture the channel once before the install JS gets drained, then
        // exercise both the payload-present and the null-payload paths.
        ReturnChannelRegistration channel = returnChannel(ui);

        invokeSuccess(channel, "hello", "<b>hello</b>");
        assertEquals(List.of("hello"), received);

        received.clear();
        invokeSuccessNull(channel);
        assertEquals(1, received.size());
        assertNull(received.get(0));
    }

    @Test
    void readHtml_adaptsPayloadConsumerToHtmlField() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        List<@Nullable String> received = new ArrayList<>();
        Clipboard.onClick(button).readHtml(received::add, err -> {
        });

        ReturnChannelRegistration channel = returnChannel(ui);

        invokeSuccess(channel, "hello", "<b>hello</b>");
        assertEquals(List.of("<b>hello</b>"), received);

        received.clear();
        invokeSuccessNull(channel);
        assertEquals(1, received.size());
        assertNull(received.get(0));
    }

    private static void invokeSuccess(ReturnChannelRegistration channel,
            String text, String html) {
        ObjectNode payload = JacksonUtils.createObjectNode();
        payload.put("text", text);
        payload.put("html", html);
        ObjectNode outcome = JacksonUtils.createObjectNode();
        outcome.put("ok", true);
        outcome.set("value", payload);
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(outcome);
        channel.invoke(args);
    }

    private static void invokeSuccessNull(ReturnChannelRegistration channel) {
        ObjectNode outcome = JacksonUtils.createObjectNode();
        outcome.put("ok", true);
        outcome.set("value", JacksonUtils.nullNode());
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(outcome);
        channel.invoke(args);
    }

    /**
     * The action's captures include the single return-channel registration (the
     * third arg of the observed wrapper). Pull it out so the test can
     * synthesise an outcome and verify the user-supplied consumer. Drains the
     * pending JS invocations as a side effect — call once per test.
     */
    private static ReturnChannelRegistration returnChannel(UI ui) {
        List<ReturnChannelRegistration> channels = actionFn(ui).getCaptures()
                .stream().filter(o -> o instanceof ReturnChannelRegistration)
                .map(o -> (ReturnChannelRegistration) o).toList();
        assertEquals(1, channels.size(),
                "Expected exactly one captured return channel");
        return channels.get(0);
    }

    /**
     * Returns the action JsFunction for a fire-and-forget binding: the install
     * JsFunction's $0 capture, which in fire-and-forget mode is the JsFunction
     * that calls {@code window.Vaadin.Flow.clipboard.writePayload}.
     */
    private static JsFunction actionFn(UI ui) {
        Object action = installFn(ui).getCaptures().get(0);
        assertTrue(action instanceof JsFunction,
                "install $0 is the action JsFunction");
        return (JsFunction) action;
    }

    private static JsFunction installFn(UI ui) {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, pending.size(),
                "Expected exactly one pending JS invocation");
        JavaScriptInvocation invocation = pending.get(0).getInvocation();
        Object o = invocation.getParameters().get(2);
        assertTrue(o instanceof JsFunction, "Expected $2 to be a JsFunction");
        return (JsFunction) o;
    }
}
