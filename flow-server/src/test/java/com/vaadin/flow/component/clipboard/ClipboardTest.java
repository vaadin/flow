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

        String install = installBody(ui);
        assertTrue(install.contains("\"click\""),
                "click trigger install JS: " + install);
    }

    @Test
    void writeText_literal_emitsClipboardItemWithTextPlain() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        Clipboard.onClick(button).writeText("Hello");

        String body = handlerBody(ui);
        assertTrue(body.contains("navigator.clipboard.write"), body);
        assertTrue(body.contains("\"text/plain\":t"), body);
        assertTrue(body.contains("\"Hello\""), body);
    }

    @Test
    void writeText_hasValue_emitsPropertyInputForValue() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        TestField field = new TestField();
        ui.getElement().appendChild(button.getElement(), field.getElement());

        Clipboard.onClick(button).writeText(field);

        String body = handlerBody(ui);
        assertTrue(body.contains("$0[\"value\"]"), body);
    }

    @Test
    void writeHtml_literal_emitsClipboardItemWithTextHtml() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        Clipboard.onClick(button).writeHtml("<b>Hi</b>");

        String body = handlerBody(ui);
        assertTrue(body.contains("\"text/html\":h"), body);
        assertTrue(body.contains("\"<b>Hi</b>\""), body);
    }

    @Test
    void write_multiFormat_packsBothTextAndHtml() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        Clipboard.onClick(button).write(
                ClipboardContent.create().text("plain").html("<b>html</b>"));

        String body = handlerBody(ui);
        assertTrue(body.contains("\"text/plain\":t"), body);
        assertTrue(body.contains("\"text/html\":h"), body);
        assertTrue(body.contains("\"plain\""), body);
        assertTrue(body.contains("\"<b>html</b>\""), body);
    }

    @Test
    void write_emptyContent_throws() {
        TestButton button = new TestButton();
        assertThrows(IllegalArgumentException.class, () -> Clipboard
                .onClick(button).write(ClipboardContent.create()));
    }

    private static String installBody(UI ui) {
        return installFn(ui).getBody();
    }

    private static String handlerBody(UI ui) {
        Object handler = installFn(ui).getCaptures().get(0);
        assertTrue(handler instanceof JsFunction,
                "install $0 is the handler JsFunction");
        return ((JsFunction) handler).getBody();
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
