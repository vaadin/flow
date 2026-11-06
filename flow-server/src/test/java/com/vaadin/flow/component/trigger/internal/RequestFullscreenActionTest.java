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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.snippetOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestFullscreenActionTest {

    private UI ui;

    @BeforeEach
    void setUp() {
        ui = new MockUI();
        // MockUI skips UI.doInit which creates the wrapper in real usage;
        // seed an app id and create the wrapper explicitly so component-mode
        // actions can resolve it.
        ui.getInternals().setFullAppId("test");
        ui.getInternals().createWrapperElement();
    }

    @Test
    void pageMode_fireAndForget_callsRequestPageFullscreen() {
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new RequestFullscreenAction());

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Fire-and-forget: action function is the inner promise function.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("requestPageFullscreen", snippetOf(action));
    }

    @Test
    void pageMode_withCallbacks_wrapsRequestPageFullscreenWithObserver() {
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new RequestFullscreenAction(() -> {
                }, err -> {
                }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // With callbacks: outer function reports the outcome; the inner
        // function is captured at $0 and makes the actual fullscreen call.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("reportOutcome", snippetOf(action));

        JsFunction inner = (JsFunction) action.getCaptures().get(0);
        assertEquals("requestPageFullscreen", snippetOf(inner));
    }

    @Test
    void componentMode_fireAndForget_callsRequestComponentFullscreenWithTargetAndWrapper() {
        TagComponent button = new TagComponent("button");
        TagComponent panel = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new RequestFullscreenAction(panel));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // $0 = target element, $1 = wrapper element from UIInternals.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("requestComponentFullscreen", snippetOf(action));
        assertSame(panel.getElement(), action.getCaptures().get(0));
        assertSame(ui.getInternals().getWrapperElement(),
                action.getCaptures().get(1));
    }

    @Test
    void componentMode_withCallbacks_wrapsRequestComponentFullscreenWithObserver() {
        TagComponent button = new TagComponent("button");
        TagComponent panel = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new RequestFullscreenAction(panel, () -> {
                }, err -> {
                }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("reportOutcome", snippetOf(action));

        JsFunction inner = (JsFunction) action.getCaptures().get(0);
        assertEquals("requestComponentFullscreen", snippetOf(inner));
        assertSame(panel.getElement(), inner.getCaptures().get(0));
        assertSame(ui.getInternals().getWrapperElement(),
                inner.getCaptures().get(1));
    }

    @Test
    void componentMode_detachedComponent_throws() {
        TagComponent detached = new TagComponent("div");
        assertThrows(IllegalStateException.class,
                () -> new RequestFullscreenAction(detached));
    }
}
