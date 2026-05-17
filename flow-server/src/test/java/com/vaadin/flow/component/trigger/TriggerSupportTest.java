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
package com.vaadin.flow.component.trigger;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.trigger.internal.TriggerSupport;
import com.vaadin.flow.internal.JacksonUtils;

public class TriggerSupportTest {

    /** Test-only callback payload — a record so Jackson can deserialise it. */
    public record RecordingPayload(String label, int count) {
    }

    /** Test-only action that records mirror-channel invocations. */
    private static final class RecordingAction
            extends AbstractCallbackAction<RecordingPayload> {
        static final String TYPE_ID = "test:recording";
        final List<RecordingPayload> received = new ArrayList<>();

        RecordingAction() {
            super(TYPE_ID, RecordingPayload.class);
        }

        @Override
        public void applyServerSideEffect(RecordingPayload payload) {
            received.add(payload);
        }
    }

    @Test
    public void snapshot_includesTriggerActionArgumentAndBinding() {
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");

        DomEventTrigger trigger = new DomEventTrigger(button, "click");
        trigger.triggers(new SetPropertyAction<>(field, "value", ""));
        // Register an unrelated argument too so the arguments map is
        // exercised (SetPropertyAction itself takes no arguments).
        TriggerSupport.on(button).registerArgumentForTest(
                new PropertyArgument<>(field, "value", String.class));

        TriggerSupport support = TriggerSupport.on(button);
        ObjectNode snapshot = support.snapshotForTest();

        JsonNode triggers = snapshot.get("triggers");
        Assert.assertEquals(1, triggers.size());
        JsonNode triggerEntry = triggers.get("0");
        Assert.assertEquals(DomEventTrigger.TYPE_ID,
                triggerEntry.get("type").asString());
        Assert.assertEquals("click",
                triggerEntry.get("config").get("eventName").asString());

        JsonNode actions = snapshot.get("actions");
        Assert.assertEquals(1, actions.size());
        JsonNode actionEntry = actions.get("0");
        Assert.assertEquals(SetPropertyAction.TYPE_ID,
                actionEntry.get("type").asString());
        Assert.assertEquals("value",
                actionEntry.get("config").get("property").asString());
        Assert.assertEquals(1,
                actionEntry.get("config").get("element").asInt());
        Assert.assertEquals("",
                actionEntry.get("config").get("value").asString());

        JsonNode arguments = snapshot.get("arguments");
        Assert.assertEquals(1, arguments.size());
        JsonNode argEntry = arguments.get("0");
        Assert.assertEquals(PropertyArgument.TYPE_ID,
                argEntry.get("type").asString());
        Assert.assertEquals("value",
                argEntry.get("config").get("property").asString());
        // The field was already referenced as element index 1 by the action,
        // so the argument reuses that index.
        Assert.assertEquals(1, argEntry.get("config").get("element").asInt());

        JsonNode bindings = snapshot.get("bindings");
        Assert.assertEquals(1, bindings.size());
        Assert.assertEquals(0, bindings.get(0).get("trigger").asInt());
        Assert.assertEquals(0, bindings.get(0).get("actions").get(0).asInt());
    }

    @Test
    public void setProperty_serialisesBooleanAndNullValues() {
        TagComponent host = new TagComponent("div");
        TagComponent target = new TagComponent("button");
        new DomEventTrigger(host, "click").triggers(
                new SetPropertyAction<>(target, "disabled", true),
                new SetPropertyAction<>(target, "label", null));

        ObjectNode snapshot = TriggerSupport.on(host).snapshotForTest();
        JsonNode actions = snapshot.get("actions");
        Assert.assertTrue(
                actions.get("0").get("config").get("value").asBoolean());
        Assert.assertTrue(actions.get("1").get("config").get("value").isNull());
    }

    @Test
    public void sharedAction_dedupedById_acrossMultipleBindings() {
        TagComponent button = new TagComponent("button");
        SetPropertyAction<Boolean> disable = new SetPropertyAction<>(button,
                "disabled", true);

        DomEventTrigger t1 = new DomEventTrigger(button, "click");
        DomEventTrigger t2 = new DomEventTrigger(button, "keydown");
        t1.triggers(disable);
        t2.triggers(disable);

        ObjectNode snapshot = TriggerSupport.on(button).snapshotForTest();
        Assert.assertEquals(2, snapshot.get("triggers").size());
        Assert.assertEquals("shared action gets a single entry", 1,
                snapshot.get("actions").size());
        Assert.assertEquals(2, snapshot.get("bindings").size());
    }

    @Test
    public void remove_dropsTriggerAndBindings() {
        TagComponent button = new TagComponent("button");
        SetPropertyAction<Boolean> disable = new SetPropertyAction<>(button,
                "disabled", true);
        DomEventTrigger t1 = new DomEventTrigger(button, "click");
        DomEventTrigger t2 = new DomEventTrigger(button, "keydown");
        t1.triggers(disable);
        t2.triggers(disable);

        t1.remove();

        ObjectNode snapshot = TriggerSupport.on(button).snapshotForTest();
        Assert.assertEquals(1, snapshot.get("triggers").size());
        Assert.assertEquals(1, snapshot.get("bindings").size());
        Assert.assertEquals(t2.getTriggerId(),
                snapshot.get("bindings").get(0).get("trigger").asInt());
    }

    @Test(expected = IllegalArgumentException.class)
    public void bind_emptyActionsRejected() {
        TagComponent button = new TagComponent("button");
        new DomEventTrigger(button, "click").triggers(new Action[0]);
    }

    @Test
    public void mirrorDispatch_deserialisesPayloadIntoCallbackPayloadType() {
        TagComponent button = new TagComponent("button");
        RecordingAction action = new RecordingAction();
        new DomEventTrigger(button, "click").triggers(action);

        TriggerSupport support = TriggerSupport.on(button);
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(0); // action id
        ObjectNode payload = JacksonUtils.createObjectNode();
        payload.put("label", "hello");
        payload.put("count", 42);
        args.add(payload);
        support.dispatchMirrorForTest(args);

        Assert.assertEquals(1, action.received.size());
        RecordingPayload received = action.received.get(0);
        Assert.assertEquals("hello", received.label());
        Assert.assertEquals(42, received.count());
    }

    @Test
    public void mirrorDispatch_ignoresNonCallbackActions() {
        TagComponent host = new TagComponent("div");
        TagComponent target = new TagComponent("button");
        // SetPropertyAction is not a callback action.
        new DomEventTrigger(host, "click")
                .triggers(new SetPropertyAction<>(target, "disabled", true));

        TriggerSupport support = TriggerSupport.on(host);
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(0);
        args.add("ignored");
        // Should not throw — non-callback actions silently skip mirror.
        support.dispatchMirrorForTest(args);
    }
}
