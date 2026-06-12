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
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.installFns;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerTest {

    @Test
    void multipleActions_eachInstalledAsItsOwnListener() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent target = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), target.getElement());

        new DomEventTrigger(button, "click").triggers(
                new SetPropertyAction<>(target, "disabled", true),
                new SetPropertyAction<>(target, "value", "x"));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Two actions → two install registrations, each with one action as
        // its $0. Order matches the declaration order.
        List<JsFunction> installs = installFns(ui);
        assertEquals(2, installs.size());
        assertEquals("disabled",
                actionOf(installs.get(0)).getCaptures().get(1));
        assertEquals("value", actionOf(installs.get(1)).getCaptures().get(1));
    }

    @Test
    void sameElementCapturedIndependentlyPerAction() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent target = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), target.getElement());

        new DomEventTrigger(button, "click").triggers(
                new SetPropertyAction<>(target, "disabled", true),
                new SetPropertyAction<>(target, "value", "x"));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Each action is self-contained — same target element captured by
        // both, not de-duplicated. Trades a duplicate wire reference for
        // simpler per-action rendering.
        List<JsFunction> installs = installFns(ui);
        assertSame(target.getElement(),
                actionOf(installs.get(0)).getCaptures().get(0));
        assertSame(target.getElement(),
                actionOf(installs.get(1)).getCaptures().get(0));
    }

    @Test
    void hostAsActionTargetIsCapturedTheSameWayAsAnyOtherElement() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new SetPropertyAction<>(button, "disabled", true));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // No "this" special-case: the host is captured as $0 just like any
        // other element.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertSame(button.getElement(), action.getCaptures().get(0));
    }

    @Test
    void multipleTriggersCalls_eachActionGetsItsOwnInitializer() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent target = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), target.getElement());

        DomEventTrigger trigger = new DomEventTrigger(button, "click");
        trigger.triggers(new SetPropertyAction<>(target, "value", "a"));
        trigger.triggers(new SetPropertyAction<>(target, "value", "b"));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        List<JsFunction> installs = installFns(ui);
        assertEquals(2, installs.size());

        // Each install has its own action JsFunction; the literal value lives
        // on the source-input function captured by the action.
        Object v0 = ((JsFunction) actionOf(installs.get(0)).getCaptures()
                .get(2)).getCaptures().get(0);
        Object v1 = ((JsFunction) actionOf(installs.get(1)).getCaptures()
                .get(2)).getCaptures().get(0);
        assertEquals("a", v0);
        assertEquals("b", v1);
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

}
