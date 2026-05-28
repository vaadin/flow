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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        // PropertyInput renders as `return target[propertyName]` and
        // captures (element, "value").
        JsFunction textInput = (JsFunction) actionFn(ui).getCaptures().get(0);
        assertEquals(
                "let propertyName=$1;let target=$0;return target[propertyName]",
                textInput.getBody());
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
    void write_contentTextFromHasValue_emitsPropertyInputForValue() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        TestField field = new TestField();
        ui.getElement().appendChild(button.getElement(), field.getElement());

        Clipboard.onClick(button).write(ClipboardContent.create().text(field));

        JsFunction textInput = (JsFunction) actionFn(ui).getCaptures().get(0);
        assertEquals(
                "let propertyName=$1;let target=$0;return target[propertyName]",
                textInput.getBody());
        assertEquals("value", textInput.getCaptures().get(1));
    }

    @Test
    void write_emptyContent_throws() {
        TestButton button = new TestButton();
        assertThrows(IllegalArgumentException.class, () -> Clipboard
                .onClick(button).write(ClipboardContent.create()));
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
