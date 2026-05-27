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
import static org.junit.jupiter.api.Assertions.assertSame;
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

        // Install JS references action at $0 and event name at $1 — no user
        // content leaks into the body, both are captures.
        JsFunction install = singleInstallFn(ui);
        assertEquals(
                "this.addEventListener($1, $0);"
                        + "return () => this.removeEventListener($1, $0);",
                install.getBody());
        assertEquals("click", install.getCaptures().get(1));

        // The install $0 is the action's JsFunction directly — no intermediate
        // composed handler layer. The action is also the DOM event listener.
        JsFunction action = actionOf(install);
        assertEquals(List.of("event"), action.getArgumentNames());

        // SetPropertyAction body shape; target captured as $0, property name
        // string capture at $1, source JsFunction invoked as $2(event).
        assertEquals("$0[$1] = $2(event)", action.getBody());
        assertSame(field.getElement(), action.getCaptures().get(0));
        assertEquals("value", action.getCaptures().get(1));
    }

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

        // HandlerInput renders as a JsFunction taking `event` and capturing
        // the property name; the body itself is a constant.
        List<JsFunction> installs = installFns(ui);
        JsFunction source0 = (JsFunction) actionOf(installs.get(0))
                .getCaptures().get(2);
        assertEquals(List.of("event"), source0.getArgumentNames());
        assertEquals("return event[$0]", source0.getBody());
        assertEquals("screenX", source0.getCaptures().get(0));

        JsFunction source1 = (JsFunction) actionOf(installs.get(1))
                .getCaptures().get(2);
        assertEquals("return event[$0]", source1.getBody());
        assertEquals("screenY", source1.getCaptures().get(0));
    }

    @Test
    void argumentFromOtherTrigger_isRejectedAtBuildTime() {
        TagComponent button1 = new TagComponent("button");
        TagComponent button2 = new TagComponent("button");
        TagComponent field = new TagComponent("input");

        ClickTrigger click1 = new ClickTrigger(button1);
        ClickTrigger click2 = new ClickTrigger(button2);

        // Input made by click1 cannot be used in click2's handler.
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
        // Same Input instance used across two separate triggers() calls on
        // its owning trigger.
        Action.Input<Integer> x = click.screenX();
        click.triggers(new SetPropertyAction<>(xField, "value", x));
        click.triggers(new SetPropertyAction<>(yField, "value", x));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(2, pending.size());
    }

    private static JsFunction singleInstallFn(UI ui) {
        List<JsFunction> installs = installFns(ui);
        assertEquals(1, installs.size(), "Expected exactly one pending JS");
        return installs.get(0);
    }

    private static List<JsFunction> installFns(UI ui) {
        return ui.getInternals().dumpPendingJavaScriptInvocations().stream()
                .map(PendingJavaScriptInvocation::getInvocation)
                .map(TriggerTest::installFn).toList();
    }

    /**
     * addJsInitializer wrapper parameters: [element, initializerId, installFn].
     * The installFn is the trigger's install JsFunction.
     */
    private static JsFunction installFn(JavaScriptInvocation invocation) {
        Object o = invocation.getParameters().get(2);
        assertTrue(o instanceof JsFunction, "Expected $2 to be a JsFunction");
        return (JsFunction) o;
    }

    /**
     * DomEventTrigger captures the action at install $0 by convention — with
     * the per-action install model, that capture IS the action JsFunction (no
     * intermediate composed-handler layer).
     */
    private static JsFunction actionOf(JsFunction installFn) {
        Object o = installFn.getCaptures().get(0);
        assertTrue(o instanceof JsFunction,
                "Expected install $0 to be the action JsFunction");
        return (JsFunction) o;
    }
}
