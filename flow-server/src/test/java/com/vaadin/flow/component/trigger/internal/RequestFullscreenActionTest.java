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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestFullscreenActionTest {

    private UI ui;

    @BeforeEach
    void setUp() {
        ui = new MockUI();
        // MockUI skips UI.doInit which is what creates the wrapper in real
        // usage; seed an app id and create the wrapper explicitly so
        // component-mode actions can resolve it.
        ui.getInternals().setFullAppId("test");
        ui.getInternals().createWrapperElement();
    }

    @Test
    void pageMode_fireAndForget_handlerCallsRequestPageFullscreen() {
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new RequestFullscreenAction());

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertEquals("window.Vaadin.Flow.fullscreen.requestPageFullscreen();",
                handlerOf(singleInstallFn(ui)).getBody());
    }

    @Test
    void pageMode_withCallbacks_handlerObservesRequestPageFullscreenPromise() {
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new RequestFullscreenAction(() -> {
                }, err -> {
                }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // $0 = OBSERVE_PROMISE JsFunction, $1 = return channel; the .then
        // /.catch glue lives inside $0, not in the handler body.
        assertEquals(
                "$0(window.Vaadin.Flow.fullscreen.requestPageFullscreen(), $1);",
                handlerOf(singleInstallFn(ui)).getBody());
    }

    @Test
    void componentMode_fireAndForget_handlerCallsRequestComponentFullscreen() {
        TagComponent button = new TagComponent("button");
        TagComponent panel = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new RequestFullscreenAction(panel));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // $0 = panel, $1 = wrapper element from UIInternals.
        assertEquals(
                "window.Vaadin.Flow.fullscreen.requestComponentFullscreen($0, $1);",
                handlerOf(singleInstallFn(ui)).getBody());
    }

    @Test
    void componentMode_withCallbacks_handlerObservesRequestComponentFullscreenPromise() {
        TagComponent button = new TagComponent("button");
        TagComponent panel = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new RequestFullscreenAction(panel, () -> {
                }, err -> {
                }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // $0 = OBSERVE_PROMISE, $1 = return channel, $2 = panel, $3 = wrapper.
        assertEquals(
                "$0(window.Vaadin.Flow.fullscreen.requestComponentFullscreen($2, $3), $1);",
                handlerOf(singleInstallFn(ui)).getBody());
    }

    @Test
    void componentMode_detachedComponent_throws() {
        TagComponent detached = new TagComponent("div");
        assertThrows(IllegalStateException.class,
                () -> new RequestFullscreenAction(detached));
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
