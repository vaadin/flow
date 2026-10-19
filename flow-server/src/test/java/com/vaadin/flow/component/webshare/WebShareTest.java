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
package com.vaadin.flow.component.webshare;

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

class WebShareTest {

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

        WebShare.onClick(button).share(ShareContent.create().title("Hi"));

        // The event name is a capture of the install function, not inlined into
        // the body.
        JsFunction install = installFn(ui);
        assertTrue(install.getCaptures().contains("click"),
                "click trigger install captures: " + install.getCaptures());
    }

    @Test
    void share_literalSlots_emitsNavigatorShareWithAllFields() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        WebShare.onClick(button).share(ShareContent.create().title("Hi")
                .text("World").url("https://vaadin.com"));

        // Each slot value is produced by invoking its input JsFunction with the
        // event; the literals are captured inside those nested functions.
        JsFunction handler = handlerFn(ui);
        assertEquals(
                "return navigator.share({title:$0(event),text:$1(event),url:$2(event)})",
                handler.getBody());
        assertEquals("Hi", ((JsFunction) handler.getCaptures().get(0))
                .getCaptures().get(0));
        assertEquals("World", ((JsFunction) handler.getCaptures().get(1))
                .getCaptures().get(0));
        assertEquals("https://vaadin.com",
                ((JsFunction) handler.getCaptures().get(2)).getCaptures()
                        .get(0));
    }

    @Test
    void share_titleFromHasValue_emitsPropertyInputForValue() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        TestField field = new TestField();
        ui.getElement().appendChild(button.getElement(), field.getElement());

        WebShare.onClick(button).share(ShareContent.create().title(field));

        // The title slot reads the field's "value" property on the client; the
        // PropertyInput renders as its own JsFunction (return $0[$1]) with the
        // property name captured at $1.
        JsFunction handler = handlerFn(ui);
        assertEquals("return navigator.share({title:$0(event)})",
                handler.getBody());
        JsFunction titleInput = (JsFunction) handler.getCaptures().get(0);
        assertEquals("return $0[$1]", titleInput.getBody());
        assertEquals("value", titleInput.getCaptures().get(1));
    }

    @Test
    void share_emptyContent_throws() {
        TestButton button = new TestButton();
        assertThrows(IllegalArgumentException.class,
                () -> WebShare.onClick(button).share(ShareContent.create()));
    }

    @Test
    void supportSignal_initiallyUnknown() {
        UI ui = new MockUI();
        UI.setCurrent(ui);
        try {
            assertEquals(WebShareSupport.UNKNOWN,
                    WebShare.supportSignal().peek());
        } finally {
            UI.setCurrent(null);
        }
    }

    @Test
    void supportSignal_reflectsInternalsUpdate() {
        UI ui = new MockUI();
        ui.getInternals().setWebShareSupport(WebShareSupport.SUPPORTED);
        assertEquals(WebShareSupport.SUPPORTED,
                WebShare.supportSignal(ui).peek());
    }

    private static JsFunction handlerFn(UI ui) {
        Object handler = installFn(ui).getCaptures().get(0);
        assertTrue(handler instanceof JsFunction,
                "install $0 is the handler JsFunction");
        return (JsFunction) handler;
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
