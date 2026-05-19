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

        JavaScriptInvocation invocation = singleInvocation(ui);
        String expr = invocation.getExpression();
        assertTrue(expr.contains("this.addEventListener(\"click\", __h)"),
                expr);
        assertTrue(expr.contains("$0[\"value\"] = \"\""), expr);
        assertTrue(expr.contains("return () => this.removeEventListener"),
                expr);

        // Wrapper appends [host element, initializerId] after user params; the
        // single non-host element ($0) is the field.
        List<Object> params = invocation.getParameters();
        assertEquals(3, params.size(), "Expected [field, host, initId]");
        assertEquals(field.getElement(), params.get(0));
        assertEquals(button.getElement(), params.get(1));
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

        String expr = singleInvocation(ui).getExpression();
        int disabledIdx = expr.indexOf("[\"disabled\"]");
        int valueIdx = expr.indexOf("[\"value\"]");
        assertTrue(disabledIdx >= 0 && valueIdx > disabledIdx,
                "Both assignments should appear in order: " + expr);
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

        JavaScriptInvocation invocation = singleInvocation(ui);
        String expr = invocation.getExpression();
        assertTrue(expr.contains("$0[\"disabled\"]"), expr);
        assertTrue(expr.contains("$0[\"value\"]"), expr);
        assertEquals(target.getElement(), invocation.getParameters().get(0));
    }

    @Test
    void hostAsActionTargetIsReferencedAsThis() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new SetPropertyAction<>(button, "disabled", true));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JavaScriptInvocation invocation = singleInvocation(ui);
        String expr = invocation.getExpression();
        assertTrue(expr.contains("this[\"disabled\"] = true"), expr);
        // Only host + initId — no extra element parameters.
        assertEquals(2, invocation.getParameters().size(), expr);
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
        assertNotEquals(pending.get(0).getInvocation().getExpression(),
                pending.get(1).getInvocation().getExpression(),
                "Each registration should produce a distinct init expression");
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
                        .contains("initializers.dispose"),
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

        String expr = singleInvocation(ui).getExpression();
        assertTrue(expr.contains("$0[\"value\"] = event[\"screenX\"]"), expr);
        assertTrue(expr.contains("$1[\"value\"] = event[\"screenY\"]"), expr);
        assertTrue(expr.contains("this.addEventListener(\"click\", __h)"),
                expr);
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

    private static JavaScriptInvocation singleInvocation(UI ui) {
        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, pending.size(), "Expected exactly one pending JS");
        return pending.get(0).getInvocation();
    }
}
