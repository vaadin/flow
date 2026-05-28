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
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalInputTest {

    private static final Pattern PROPERTY_NAME = Pattern
            .compile("__vTriggerSignalInput_\\d+");

    private MockVaadinServletService service;
    private VaadinSession session;
    private MockUI ui;

    @BeforeEach
    void setUp() {
        service = new MockVaadinServletService();
        VaadinService.setCurrent(service);
        session = new MockVaadinSession(service);
        session.lock();
        ui = new MockUI(session);
    }

    @AfterEach
    void tearDown() {
        session.unlock();
        CurrentInstance.clearAll();
        service.destroy();
    }

    @Test
    void constructor_hasNoSideEffects_noEffectInstalledUntilRendered() {
        TagComponent owner = new TagComponent("div");
        ui.getElement().appendChild(owner.getElement());

        new SignalInput<>(owner, new ValueSignal<>("hello"));

        // Just creating the input must not queue any JS — the effect is
        // installed lazily when the input is wired into a trigger.
        assertEquals(List.of(), dumpExecuteJsCalls(),
                "Constructor must not queue any executeJs");
    }

    @Test
    void handlerJs_readsUniquePropertyOnOwnerElement() {
        TagComponent button = new TagComponent("button");
        TagComponent owner = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), owner.getElement());

        ValueSignal<String> signal = new ValueSignal<>("hello");
        new DomEventTrigger(button, "click").triggers(
                new WriteToClipboardAction(new SignalInput<>(owner, signal),
                        null, null));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // SignalInput renders as `return $0[$1]` with the property name as
        // capture $1. Reach into the action's captures to find the text
        // input's JsFunction, then verify the generated property-name shape.
        JsFunction action = actionOf(singleInstallFn(ui));
        JsFunction textInput = (JsFunction) action.getCaptures().get(0);
        assertEquals("return $0[$1]", textInput.getBody());
        String property = (String) textInput.getCaptures().get(1);
        assertTrue(PROPERTY_NAME.matcher(property).matches(),
                "Unexpected property name: " + property);
    }

    @Test
    void signalChange_pushesValueToOwnerElementProperty() {
        TagComponent button = new TagComponent("button");
        TagComponent owner = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), owner.getElement());

        ValueSignal<String> signal = new ValueSignal<>("first");
        new DomEventTrigger(button, "click").triggers(
                new WriteToClipboardAction(new SignalInput<>(owner, signal),
                        null, null));

        // After triggers(), the effect has been installed and the initial
        // pass queued an executeJs that mirrors the signal to the owner.
        // Drop the install JsFunction; what's left is just the mirror call.
        JavaScriptInvocation initial = onlyMirrorCall(dumpExecuteJsCalls());
        assertTrue(initial.getExpression().contains("this[$0]=$1"),
                "Unexpected expression: " + initial.getExpression());
        String property = (String) initial.getParameters().get(0);
        assertTrue(PROPERTY_NAME.matcher(property).matches(),
                "Unexpected property name: " + property);
        assertEquals("first", initial.getParameters().get(1));

        signal.set("second");

        JavaScriptInvocation update = onlyMirrorCall(dumpExecuteJsCalls());
        assertEquals(property, update.getParameters().get(0));
        assertEquals("second", update.getParameters().get(1));
    }

    @Test
    void multipleSignalInputs_getDistinctPropertyNames() {
        TagComponent owner = new TagComponent("div");
        ui.getElement().appendChild(owner.getElement());

        SignalInput<String> a = new SignalInput<>(owner,
                new ValueSignal<>("a"));
        SignalInput<String> b = new SignalInput<>(owner,
                new ValueSignal<>("b"));

        // Property name is the second capture of the rendered JsFunction
        // (`return $0[$1]`, where $1 is the propertyName literal).
        JsFunction fnA = a.toJs(new DomEventTrigger(owner, "click"));
        JsFunction fnB = b.toJs(new DomEventTrigger(owner, "click"));

        assertNotEquals(fnA.getCaptures().get(1), fnB.getCaptures().get(1),
                "Each SignalInput must use its own property name");
    }

    private List<JavaScriptInvocation> dumpExecuteJsCalls() {
        // executeJs goes through the element queue, flushed on the next
        // before-client-response pass — same pattern as
        // MockUI.dumpPendingJsInvocations.
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return ui.getInternals().dumpPendingJavaScriptInvocations().stream()
                .map(PendingJavaScriptInvocation::getInvocation)
                .filter(inv -> inv.getParameters().stream()
                        .noneMatch(p -> p instanceof JsFunction))
                .toList();
    }

    private static JavaScriptInvocation onlyMirrorCall(
            List<JavaScriptInvocation> calls) {
        assertEquals(1, calls.size(),
                "Expected exactly one mirror-update executeJs");
        return calls.get(0);
    }

    private static JsFunction singleInstallFn(UI ui) {
        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        JsFunction installFn = null;
        for (PendingJavaScriptInvocation inv : pending) {
            List<Object> params = inv.getInvocation().getParameters();
            if (params.size() >= 3 && params.get(2) instanceof JsFunction) {
                assertTrue(installFn == null,
                        "Expected exactly one install JsFunction");
                installFn = (JsFunction) params.get(2);
            }
        }
        assertTrue(installFn != null, "Expected an install JsFunction");
        return installFn;
    }

}
