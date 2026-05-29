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
package com.vaadin.flow.component.fullscreen;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        // enter(component) can resolve it.
        ui.getInternals().setFullAppId("test");
        ui.getInternals().createWrapperElement();
        UI.setCurrent(ui);
    }

    // --- entry point ----------------------------------------------------

    @Test
    void onClick_installsClickTrigger() {
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        Fullscreen.onClick(button).enter();

        // DomEventTrigger emits "this.addEventListener($1, $0); ..." where
        // $1 is the event name capture; the actual "click" string lives in
        // the install JsFunction's captures, not its body.
        JsFunction installFn = singleInstallFn(ui);
        assertTrue(installFn.getCaptures().contains("click"),
                "Expected install captures to include the event name: "
                        + installFn.getCaptures());
    }

    // --- enter actions --------------------------------------------------

    @Test
    void enter_fireAndForget_actionFnCallsRequestPageFullscreen() {
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        Fullscreen.onClick(button).enter();

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(
                "return window.Vaadin.Flow.fullscreen.requestPageFullscreen()",
                action.getBody());
    }

    @Test
    void enter_withCallbacks_wrapsWithObserver() {
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        Fullscreen.onClick(button).enter(() -> {
        }, err -> {
        });

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("$0($1(event), $2)", action.getBody());

        JsFunction inner = (JsFunction) action.getCaptures().get(1);
        assertEquals(
                "return window.Vaadin.Flow.fullscreen.requestPageFullscreen()",
                inner.getBody());
    }

    @Test
    void enterComponent_fireAndForget_actionFnCallsRequestComponentFullscreenWithTargetAndWrapper() {
        TestButton button = new TestButton();
        TestPanel panel = new TestPanel();
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        Fullscreen.onClick(button).enter(panel);

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(
                "return window.Vaadin.Flow.fullscreen.requestComponentFullscreen($0, $1)",
                action.getBody());
        assertSame(panel.getElement(), action.getCaptures().get(0));
        assertSame(ui.getInternals().getWrapperElement(),
                action.getCaptures().get(1));
    }

    @Test
    void enterComponent_withCallbacks_wrapsWithObserver() {
        TestButton button = new TestButton();
        TestPanel panel = new TestPanel();
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        Fullscreen.onClick(button).enter(panel, () -> {
        }, err -> {
        });

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("$0($1(event), $2)", action.getBody());

        JsFunction inner = (JsFunction) action.getCaptures().get(1);
        assertEquals(
                "return window.Vaadin.Flow.fullscreen.requestComponentFullscreen($0, $1)",
                inner.getBody());
        assertSame(panel.getElement(), inner.getCaptures().get(0));
        assertSame(ui.getInternals().getWrapperElement(),
                inner.getCaptures().get(1));
    }

    @Test
    void enterComponent_detachedComponent_defersUntilAttach() {
        TestButton button = new TestButton();
        TestPanel panel = new TestPanel();
        ui.getElement().appendChild(button.getElement());

        // Wire the binding before the target is attached — must not throw,
        // and must not install any JS yet.
        Fullscreen.onClick(button).enter(panel);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        long installsBeforeAttach = ui.getInternals()
                .dumpPendingJavaScriptInvocations().stream()
                .filter(p -> p.getInvocation().getExpression()
                        .contains("registerInitializer"))
                .count();
        assertEquals(0, installsBeforeAttach,
                "No install JS expected before target attaches");

        // Attach the target — the deferred bind() runs and installs the JS.
        ui.getElement().appendChild(panel.getElement());
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(
                "return window.Vaadin.Flow.fullscreen.requestComponentFullscreen($0, $1)",
                action.getBody());
        assertSame(panel.getElement(), action.getCaptures().get(0));
    }

    // --- exit -----------------------------------------------------------

    @Test
    void exit_executesConnectorJs() {
        Fullscreen.exit();

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, pending.size(),
                "Expected exactly one pending JS for exit()");
        assertEquals("window.Vaadin.Flow.fullscreen.exitFullscreen()",
                pending.get(0).getInvocation().getExpression());
    }

    @Test
    void exit_withoutCurrentUi_throws() {
        UI.setCurrent(null);
        assertThrows(IllegalStateException.class, Fullscreen::exit);
    }

    // --- helpers --------------------------------------------------------

    private static JsFunction singleInstallFn(UI ui) {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, pending.size(), "Expected exactly one pending JS");
        JavaScriptInvocation invocation = pending.get(0).getInvocation();
        Object o = invocation.getParameters().get(2);
        assertTrue(o instanceof JsFunction,
                "Expected install param $2 to be a JsFunction");
        return (JsFunction) o;
    }

    private static JsFunction actionOf(JsFunction installFn) {
        Object o = installFn.getCaptures().get(0);
        assertTrue(o instanceof JsFunction,
                "Expected install $0 to be the action JsFunction");
        return (JsFunction) o;
    }
}
