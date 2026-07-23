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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.trigger.internal.Action.Input;
import com.vaadin.flow.component.trigger.internal.PromiseAction.Error;
import com.vaadin.flow.component.trigger.internal.Triggers.ArmingListener;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the server-side simulation seam (arming observation, event-name
 * introspection, input evaluation, outcome delivery) that lets a browserless
 * test harness fire triggers and observe their actions without a browser.
 */
class TriggerSimulationTest {

    private record Armed(Trigger trigger, List<Action> actions) {
    }

    // 1a — arming observation ------------------------------------------------

    @Test
    void armingListener_notifiedPerTriggersCall_thenOnRemove() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), field.getElement());

        List<Armed> armed = new ArrayList<>();
        AtomicReference<Trigger> disarmed = new AtomicReference<>();
        Registration registration = Triggers
                .addArmingListener(new ArmingListener() {
                    @Override
                    public void onArmed(Trigger trigger, List<Action> actions) {
                        armed.add(new Armed(trigger, actions));
                    }

                    @Override
                    public void onDisarmed(Trigger trigger) {
                        disarmed.set(trigger);
                    }
                });
        try {
            ClickTrigger click = new ClickTrigger(button);
            Action a = new SetPropertyAction<>(field, "value", "x");
            Action b = new SetPropertyAction<>(field, "disabled", true);
            click.triggers(a, b);

            assertEquals(1, armed.size(),
                    "one notification per triggers() call");
            assertSame(click, armed.get(0).trigger());
            assertEquals(List.of(a, b), armed.get(0).actions());

            // A second call on the same trigger notifies again with its batch.
            Action c = new SetPropertyAction<>(field, "value", "y");
            click.triggers(c);
            assertEquals(2, armed.size());
            assertEquals(List.of(c), armed.get(1).actions());

            click.remove();
            assertSame(click, disarmed.get());
        } finally {
            registration.remove();
        }

        // After removal the listener no longer fires.
        int before = armed.size();
        new ClickTrigger(button)
                .triggers(new SetPropertyAction<>(field, "value", "z"));
        assertEquals(before, armed.size());
    }

    @Test
    void armingRegistration_serializable_evenWithNonSerializableListener()
            throws Exception {
        // ArmingListener is not Serializable, so this anonymous instance is
        // not either. The registration must still serialize (the listener is
        // held transiently) rather than fail on it.
        Registration registration = Triggers
                .addArmingListener(new ArmingListener() {
                    @Override
                    public void onArmed(Trigger trigger, List<Action> actions) {
                    }
                });
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(registration);
            }
            Registration restored;
            try (ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(bytes.toByteArray()))) {
                restored = (Registration) in.readObject();
            }
            // A deserialized registration refers to no live listener; removing
            // it must be a harmless no-op.
            restored.remove();
        } finally {
            registration.remove();
        }
    }

    // 1b — event-name introspection -----------------------------------------

    @Test
    void domEventTrigger_reportsEventName() {
        TagComponent button = new TagComponent("button");
        assertEquals("click", new ClickTrigger(button).getEventName());
        assertEquals("dblclick", new DoubleClickTrigger(button).getEventName());
        assertEquals("input",
                new DomEventTrigger(button, "input").getEventName());
    }

    // 1c — server-side input evaluation -------------------------------------

    @Test
    void literalInput_evaluatesToItsValue() {
        assertEquals("hi", new LiteralInput<>("hi").evaluate(null).asString());
    }

    @Test
    void propertyInput_evaluatesToSyncedProperty() {
        TagComponent field = new TagComponent("input");
        field.getElement().setProperty("value", "abc");

        Input<String> input = new PropertyInput<>(field, "value", String.class);
        assertEquals("abc", input.evaluate(null).asString());
    }

    @Test
    void propertyInput_absentProperty_evaluatesToNull() {
        TagComponent field = new TagComponent("input");
        Input<String> input = new PropertyInput<>(field, "value", String.class);
        assertTrue(input.evaluate(null).isNull());
    }

    @Test
    void handlerInput_evaluatesFromEventData() {
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("screenX", 42);
        assertEquals(42,
                ClickTrigger.EventData.screenX.evaluate(eventData).asInt());
    }

    // 1d — outcome delivery --------------------------------------------------

    @Test
    void promiseAction_deliverSuccess_runsOnSuccess() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        AtomicReference<String> copied = new AtomicReference<>();
        AtomicReference<Error> failed = new AtomicReference<>();
        WriteToClipboardAction action = new WriteToClipboardAction(
                new LiteralInput<>("ignored"), null, copied::set, failed::set);
        ClickTrigger click = new ClickTrigger(button);
        click.triggers(action);

        action.deliverSuccess(click, JacksonUtils.writeValue("hello"));
        assertEquals("hello", copied.get());
        assertNull(failed.get());
    }

    @Test
    void promiseAction_deliverError_runsOnError() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        AtomicReference<String> copied = new AtomicReference<>();
        AtomicReference<Error> failed = new AtomicReference<>();
        WriteToClipboardAction action = new WriteToClipboardAction(
                new LiteralInput<>("ignored"), null, copied::set, failed::set);
        ClickTrigger click = new ClickTrigger(button);
        click.triggers(action);

        action.deliverError(click, new Error("NotAllowedError", "denied"));
        assertEquals("NotAllowedError", failed.get().name());
        assertEquals("denied", failed.get().message());
        assertNull(copied.get());
    }

    @Test
    void promiseAction_fireAndForget_deliverIsNoop() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        // No onCopied/onError => no channel; delivering must not throw.
        WriteToClipboardAction action = new WriteToClipboardAction(
                new LiteralInput<>("x"), null);
        ClickTrigger click = new ClickTrigger(button);
        click.triggers(action);

        action.deliverSuccess(click, JacksonUtils.writeValue("x"));
        action.deliverError(click, new Error("", ""));
    }

    @Test
    void callbackAction_deliver_runsCallback() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        AtomicReference<Integer> received = new AtomicReference<>();
        CallbackAction<Integer> action = new CallbackAction<>(Integer.class,
                received::set, ClickTrigger.EventData.screenX);
        ClickTrigger click = new ClickTrigger(button);
        click.triggers(action);

        action.deliver(click, JacksonUtils.writeValue(7));
        assertEquals(7, received.get());
    }

    @Test
    void callbackAction_valueless_deliverRunsRunnable() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        AtomicReference<Boolean> ran = new AtomicReference<>(false);
        CallbackAction<Void> action = new CallbackAction<>(() -> ran.set(true));
        ClickTrigger click = new ClickTrigger(button);
        click.triggers(action);

        action.deliver(click, null);
        assertTrue(ran.get());
    }
}
