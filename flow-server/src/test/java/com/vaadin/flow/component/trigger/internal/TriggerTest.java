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

        JsFunction install = singleInstallFn(ui);
        // Install JS references the handler at $0 — no user content leaks
        // into the install string.
        assertEquals("this.addEventListener(\"click\", $0);"
                + "return () => this.removeEventListener(\"click\", $0);",
                install.getBody());

        // The handler just invokes each action's JsFunction.
        JsFunction handler = handlerOf(install);
        assertEquals(List.of("event"), handler.getArgumentNames());
        assertEquals("$0(event);", handler.getBody());
        assertEquals(1, handler.getCaptures().size());

        // SetPropertyAction body shape; target captured as $0, property name
        // string capture at $1, source JsFunction invoked as $2(event).
        JsFunction action = actionOf(handler, 0);
        assertEquals("$0[$1] = $2(event)", action.getBody());
        assertSame(field.getElement(), action.getCaptures().get(0));
        assertEquals("value", action.getCaptures().get(1));
    }

    @Test
    void multipleActions_invokedInOrderAsSeparateCaptures() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent target = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), target.getElement());

        new DomEventTrigger(button, "click").triggers(
                new SetPropertyAction<>(target, "disabled", true),
                new SetPropertyAction<>(target, "value", "x"));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Two actions → handler invokes them positionally in declaration
        // order.
        JsFunction handler = handlerOf(singleInstallFn(ui));
        assertEquals("$0(event);$1(event);", handler.getBody());
        assertEquals("disabled", actionOf(handler, 0).getCaptures().get(1));
        assertEquals("value", actionOf(handler, 1).getCaptures().get(1));
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
        JsFunction handler = handlerOf(singleInstallFn(ui));
        assertSame(target.getElement(),
                actionOf(handler, 0).getCaptures().get(0));
        assertSame(target.getElement(),
                actionOf(handler, 1).getCaptures().get(0));
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
        JsFunction action = actionOf(handlerOf(singleInstallFn(ui)), 0);
        assertSame(button.getElement(), action.getCaptures().get(0));
    }

    @Test
    void multipleTriggersCalls_eachEmitOwnInitializerWithDistinctCaptures() {
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

        // Top-level handler bodies are identical ($0(event);), but each
        // wraps its own action JsFunction whose source-input function
        // captures the distinct value.
        JsFunction h0Action = actionOf(
                handlerOf(installFn(pending.get(0).getInvocation())), 0);
        JsFunction h1Action = actionOf(
                handlerOf(installFn(pending.get(1).getInvocation())), 0);
        Object v0 = ((JsFunction) h0Action.getCaptures().get(2)).getCaptures()
                .get(0);
        Object v1 = ((JsFunction) h1Action.getCaptures().get(2)).getCaptures()
                .get(0);
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

        // HandlerInput renders as a JsFunction taking `event` and returning
        // the property read.
        JsFunction handler = handlerOf(singleInstallFn(ui));
        JsFunction action0 = actionOf(handler, 0);
        JsFunction source0 = (JsFunction) action0.getCaptures().get(2);
        assertEquals(List.of("event"), source0.getArgumentNames());
        assertEquals("return event[\"screenX\"]", source0.getBody());

        JsFunction action1 = actionOf(handler, 1);
        JsFunction source1 = (JsFunction) action1.getCaptures().get(2);
        assertEquals("return event[\"screenY\"]", source1.getBody());
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
        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, pending.size(), "Expected exactly one pending JS");
        return installFn(pending.get(0).getInvocation());
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
     * Inside the install JsFunction, the handler is captured at $0.
     */
    private static JsFunction handlerOf(JsFunction installFn) {
        Object o = installFn.getCaptures().get(0);
        assertTrue(o instanceof JsFunction,
                "Expected install $0 to be the handler JsFunction");
        return (JsFunction) o;
    }

    private static JsFunction actionOf(JsFunction handler, int index) {
        Object o = handler.getCaptures().get(index);
        assertTrue(o instanceof JsFunction,
                "Expected handler capture " + index + " to be a JsFunction");
        return (JsFunction) o;
    }
}
