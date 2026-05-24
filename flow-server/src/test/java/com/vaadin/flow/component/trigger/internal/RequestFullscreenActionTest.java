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
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestFullscreenActionTest {

    @Test
    void fireAndForget_actionFnCallsRequestFullscreenOnTargetCapture() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent panel = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new RequestFullscreenAction(panel));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Fire-and-forget collapses to the inner promise function directly.
        // $0 is the target element captured by JsFunction.
        JsFunction action = actionOf(handlerOf(singleInstallFn(ui)), 0);
        assertEquals("return $0.requestFullscreen()", action.getBody());
        assertSame(panel.getElement(), action.getCaptures().get(0));
    }

    @Test
    void withCallbacks_actionFnWrapsInnerWithObserverAndChannel() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent panel = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new RequestFullscreenAction(panel, () -> {
                }, err -> {
                }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // With callbacks, PromiseAction wraps the inner function with
        // OBSERVE_PROMISE; the inner still captures the target as $0.
        JsFunction action = actionOf(handlerOf(singleInstallFn(ui)), 0);
        assertEquals("$0($1(event), $2)", action.getBody());

        JsFunction inner = (JsFunction) action.getCaptures().get(1);
        assertEquals("return $0.requestFullscreen()", inner.getBody());
        assertSame(panel.getElement(), inner.getCaptures().get(0));
    }

    @Test
    void targetEqualsHost_capturedTheSameWayAsAnyOtherElement() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new RequestFullscreenAction(button));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // No "this" special-case: the host element is captured the same way
        // as any other element. The DOM ref is resolved on the client from
        // the JsFunction capture.
        JsFunction action = actionOf(handlerOf(singleInstallFn(ui)), 0);
        Element captured = (Element) action.getCaptures().get(0);
        assertSame(button.getElement(), captured);
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

    private static JsFunction actionOf(JsFunction handler, int index) {
        Object o = handler.getCaptures().get(index);
        assertTrue(o instanceof JsFunction,
                "Expected handler capture " + index + " to be a JsFunction");
        return (JsFunction) o;
    }
}
