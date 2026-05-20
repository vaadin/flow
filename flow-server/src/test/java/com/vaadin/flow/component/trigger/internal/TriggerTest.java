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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerTest {

    @Test
    void domEventTrigger_setProperty_emitsAddEventListenerOnAttach() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), field.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new SetPropertyAction<>(field, "value", ""));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction install = singleInstallFn(ui);
        // Install JS references the handler at $0 — no user content leaks
        // into the install string.
        assertEquals("this.addEventListener(\"click\", $0);"
                + "return () => this.removeEventListener(\"click\", $0);",
                install.getBody());

        JsFunction handler = handlerOf(install);
        assertEquals(List.of("event"), handler.getArgumentNames());
        assertEquals("$0[\"value\"] = \"\";", handler.getBody());
        assertEquals(List.of(field.getElement()), handler.getCaptures());
    }

    @Test
    void multipleActions_runInOrder() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent target = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), target.getElement());

        new DomEventTrigger(button, "click").triggers(
                new SetPropertyAction<>(target, "disabled", true),
                new SetPropertyAction<>(target, "value", "x"));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        String body = handlerOf(singleInstallFn(ui)).getBody();
        int disabledIdx = body.indexOf("[\"disabled\"]");
        int valueIdx = body.indexOf("[\"value\"]");
        assertTrue(disabledIdx >= 0 && valueIdx > disabledIdx,
                "Both assignments should appear in order: " + body);
    }

    @Test
    void sameElementReusesParameterIndex() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent target = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), target.getElement());

        new DomEventTrigger(button, "click").triggers(
                new SetPropertyAction<>(target, "disabled", true),
                new SetPropertyAction<>(target, "value", "x"));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction handler = handlerOf(singleInstallFn(ui));
        String body = handler.getBody();
        assertTrue(body.contains("$0[\"disabled\"]"), body);
        assertTrue(body.contains("$0[\"value\"]"), body);
        assertEquals(List.of(target.getElement()), handler.getCaptures());
    }

    @Test
    void hostAsActionTargetIsReferencedAsThis() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new SetPropertyAction<>(button, "disabled", true));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction handler = handlerOf(singleInstallFn(ui));
        assertEquals("this[\"disabled\"] = true;", handler.getBody());
        // No element captures — the host is `this`.
        assertEquals(List.of(), handler.getCaptures());
    }

    @Test
    void multipleTriggersCalls_eachEmitOwnInitializer() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent target = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), target.getElement());

        DomEventTrigger trigger = new DomEventTrigger(button, "click");
        trigger.triggers(new SetPropertyAction<>(target, "value", "a"));
        trigger.triggers(new SetPropertyAction<>(target, "value", "b"));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(2, pending.size());
        String h0 = handlerOf(installFn(pending.get(0).getInvocation()))
                .getBody();
        String h1 = handlerOf(installFn(pending.get(1).getInvocation()))
                .getBody();
        assertNotEquals(h0, h1,
                "Each registration should produce a distinct handler body");
    }

    @Test
    void remove_emitsDisposeInvocations() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), field.getElement());

        DomEventTrigger trigger = new DomEventTrigger(button, "click");
        trigger.triggers(new SetPropertyAction<>(field, "value", ""));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().dumpPendingJavaScriptInvocations();
        ui.getInternals().getStateTree().collectChanges(c -> {
        });

        trigger.remove();
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, pending.size());
        assertTrue(
                pending.get(0).getInvocation().getExpression()
                        .contains("disposeInitializer"),
                "Removal should emit the dispose invocation");
    }

    @Test
    void triggers_emptyActionsRejected() {
        TagComponent button = new TagComponent("button");
        DomEventTrigger trigger = new DomEventTrigger(button, "click");
        assertThrows(IllegalArgumentException.class,
                () -> trigger.triggers(new Action[0]));
    }

    @Test
    void clickTrigger_screenCoordinates_renderEventProperties() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent xField = new TagComponent("input");
        TagComponent yField = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), xField.getElement(),
                yField.getElement());

        ClickTrigger click = new ClickTrigger(button);
        click.triggers(
                new SetPropertyAction<>(xField, "value", click.screenX()),
                new SetPropertyAction<>(yField, "value", click.screenY()));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction handler = handlerOf(singleInstallFn(ui));
        String body = handler.getBody();
        assertTrue(body.contains("$0[\"value\"] = event[\"screenX\"]"), body);
        assertTrue(body.contains("$1[\"value\"] = event[\"screenY\"]"), body);
        assertEquals(List.of(xField.getElement(), yField.getElement()),
                handler.getCaptures());
    }

    @Test
    void argumentFromOtherTrigger_isRejectedAtBuildTime() {
        TagComponent button1 = new TagComponent("button");
        TagComponent button2 = new TagComponent("button");
        TagComponent field = new TagComponent("input");

        ClickTrigger click1 = new ClickTrigger(button1);
        ClickTrigger click2 = new ClickTrigger(button2);

        // Argument made by click1 cannot be used in click2's handler.
        assertThrows(IllegalArgumentException.class, () -> click2.triggers(
                new SetPropertyAction<>(field, "value", click1.screenX())));
    }

    @Test
    void argumentUsedInOwningTrigger_acceptedAcrossMultipleTriggersCalls() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent xField = new TagComponent("input");
        TagComponent yField = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), xField.getElement(),
                yField.getElement());

        ClickTrigger click = new ClickTrigger(button);
        // Same Argument instance used across two separate triggers() calls on
        // its owning trigger.
        Argument<Integer> x = click.screenX();
        click.triggers(new SetPropertyAction<>(xField, "value", x));
        click.triggers(new SetPropertyAction<>(yField, "value", x));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(2, pending.size());
    }

    private static JsFunction singleInstallFn(UI ui) {
        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, pending.size(), "Expected exactly one pending JS");
        return installFn(pending.get(0).getInvocation());
    }

    /**
     * addJsInitializer wrapper parameters: [element, installFn, initializerId].
     * The installFn is the trigger's install JsFunction.
     */
    private static JsFunction installFn(JavaScriptInvocation invocation) {
        Object o = invocation.getParameters().get(1);
        assertTrue(o instanceof JsFunction, "Expected $1 to be a JsFunction");
        return (JsFunction) o;
    }

    /**
     * Inside the install JsFunction, the handler is captured at $0.
     */
    private static JsFunction handlerOf(JsFunction installFn) {
        Object o = installFn.getCaptures().get(0);
        assertTrue(o instanceof JsFunction,
                "Expected install $0 to be the handler JsFunction");
        return (JsFunction) o;
    }
}
