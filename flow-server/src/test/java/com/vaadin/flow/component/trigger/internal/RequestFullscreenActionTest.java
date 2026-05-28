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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
        // The target element is exposed by name and is the only capture.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("let target=$0;return target.requestFullscreen()",
                action.getBody());
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
        // OBSERVE_PROMISE and routes through a return channel; all three are
        // exposed by name.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("let channel=$2;let inner=$1;let observer=$0;"
                + "observer(inner(event), channel)", action.getBody());

        JsFunction inner = (JsFunction) action.getCaptures().get(1);
        assertEquals("let target=$0;return target.requestFullscreen()",
                inner.getBody());
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
        JsFunction action = actionOf(singleInstallFn(ui));
        Element captured = (Element) action.getCaptures().get(0);
        assertSame(button.getElement(), captured);
    }

}
