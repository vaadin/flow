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
package com.vaadin.flow.component.trigger.internal;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShareActionTest {

    @Test
    void fireAndForget_handlerCallsNavigatorShareWithAllSlots() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click").triggers(new ShareAction(
                new LiteralInput<>("Hi"), new LiteralInput<>("World"),
                new LiteralInput<>("https://vaadin.com")));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertEquals(
                "navigator.share({title:\"Hi\",text:\"World\",url:\"https://vaadin.com\"});",
                handlerOf(singleInstallFn(ui)).getBody());
    }

    @Test
    void fireAndForget_onlyUrl_emitsObjectWithSingleField() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click").triggers(new ShareAction(null,
                null, new LiteralInput<>("https://vaadin.com")));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertEquals("navigator.share({url:\"https://vaadin.com\"});",
                handlerOf(singleInstallFn(ui)).getBody());
    }

    @Test
    void withCallbacks_handlerCallsObserverWithNavigatorSharePromise() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click").triggers(
                new ShareAction(new LiteralInput<>("Hi"), null, null, () -> {
                }, err -> {
                }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // $0 = OBSERVE_PROMISE JsFunction, $1 = return channel; the
        // .then/.catch glue lives inside $0, not in the action's expression.
        assertEquals("$0(navigator.share({title:\"Hi\"}), $1);",
                handlerOf(singleInstallFn(ui)).getBody());
    }

    @Test
    void allInputsNull_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new ShareAction(null, null, null));
    }

    private static JsFunction singleInstallFn(UI ui) {
        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, pending.size(), "Expected exactly one pending JS");
        return installFn(pending.get(0).getInvocation());
    }

    private static JsFunction installFn(JavaScriptInvocation invocation) {
        Object o = invocation.getParameters().get(2);
        assertTrue(o instanceof JsFunction, "Expected $2 to be a JsFunction");
        return (JsFunction) o;
    }

    private static JsFunction handlerOf(JsFunction installFn) {
        Object o = installFn.getCaptures().get(0);
        assertTrue(o instanceof JsFunction,
                "Expected install $0 to be the handler JsFunction");
        return (JsFunction) o;
    }
}
