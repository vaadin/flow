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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.component.trigger.internal.DomEventTrigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullscreenTest {

    @Tag("test-button")
    static final class TestButton extends Component
            implements ClickNotifier<TestButton> {
    }

    @Tag("test-panel")
    static final class TestPanel extends Component {
    }

    private UI ui;

    @BeforeEach
    void setUp() {
        ui = new MockUI();
        // MockUI skips UI.doInit which creates the wrapper in real usage;
        // seed an app id and create the wrapper explicitly so
        // requestComponent(...) can resolve it.
        ui.getInternals().setFullAppId("test");
        ui.getInternals().createWrapperElement();
    }

    // --- entry points ---------------------------------------------------

    @Test
    void on_clickNotifier_installsClickTrigger() {
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        Fullscreen.on(button).requestPage();

        String install = installBody(ui);
        assertTrue(install.contains("\"click\""),
                "click trigger install JS: " + install);
    }

    @Test
    void on_trigger_usesProvidedTrigger() {
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        Fullscreen.on(new DomEventTrigger(button, "keydown")).requestPage();

        String install = installBody(ui);
        assertTrue(install.contains("\"keydown\""),
                "keydown trigger install JS: " + install);
    }

    @Test
    void on_nullClickNotifier_throws() {
        assertThrows(NullPointerException.class,
                () -> Fullscreen.on((ClickNotifier<?>) null));
    }

    // --- request verbs --------------------------------------------------

    @Test
    void requestPage_fireAndForget_emitsRequestPageFullscreen() {
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        Fullscreen.on(button).requestPage();

        assertEquals("window.Vaadin.Flow.fullscreen.requestPageFullscreen();",
                handlerBody(ui));
    }

    @Test
    void requestPage_withCallbacks_emitsObservedPromise() {
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        Fullscreen.on(button).requestPage(() -> {
        }, err -> {
        });

        // $0 = OBSERVE_PROMISE, $1 = channel; observe wraps the request.
        assertEquals(
                "$0(window.Vaadin.Flow.fullscreen.requestPageFullscreen(), $1);",
                handlerBody(ui));
    }

    @Test
    void requestComponent_fireAndForget_emitsRequestComponentFullscreen() {
        TestButton button = new TestButton();
        TestPanel panel = new TestPanel();
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        Fullscreen.on(button).requestComponent(panel);

        // $0 = panel, $1 = wrapper element from UIInternals.
        assertEquals(
                "window.Vaadin.Flow.fullscreen.requestComponentFullscreen($0, $1);",
                handlerBody(ui));
    }

    @Test
    void requestComponent_withCallbacks_emitsObservedPromise() {
        TestButton button = new TestButton();
        TestPanel panel = new TestPanel();
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        Fullscreen.on(button).requestComponent(panel, () -> {
        }, err -> {
        });

        // $0 = OBSERVE_PROMISE, $1 = channel, $2 = panel, $3 = wrapper.
        assertEquals(
                "$0(window.Vaadin.Flow.fullscreen.requestComponentFullscreen($2, $3), $1);",
                handlerBody(ui));
    }

    @Test
    void requestComponent_detachedComponent_throws() {
        TestButton button = new TestButton();
        TestPanel detached = new TestPanel();
        ui.getElement().appendChild(button.getElement());
        assertThrows(IllegalStateException.class,
                () -> Fullscreen.on(button).requestComponent(detached));
    }

    @Test
    void request_returnRemoves_thenNoMoreInstallJs() {
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        FullscreenRequest registration = Fullscreen.on(button).requestPage();
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().dumpPendingJavaScriptInvocations();

        registration.remove();
        // Removing the binding should leave no further install JS pending,
        // only the JS that detaches the listener.
        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertTrue(
                pending.stream()
                        .noneMatch(p -> p.getInvocation().getExpression()
                                .contains("addEventListener")),
                "Expected no new addEventListener installs after remove()");
    }

    // --- helpers --------------------------------------------------------

    private static String installBody(UI ui) {
        // The wrapper expression on the invocation is framework-owned init
        // glue; the trigger's actual install JS is the body of the JsFunction
        // passed as the third parameter.
        return singleInstallFn(ui).getBody();
    }

    private static String handlerBody(UI ui) {
        return handlerOf(singleInstallFn(ui)).getBody();
    }

    private static JavaScriptInvocation singleInstall(UI ui) {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, pending.size(), "Expected exactly one pending JS");
        return pending.get(0).getInvocation();
    }

    private static JsFunction singleInstallFn(UI ui) {
        Object o = singleInstall(ui).getParameters().get(2);
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
