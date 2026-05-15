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

import org.junit.Assert;
import org.junit.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.trigger.internal.TriggerSupport;
import com.vaadin.flow.dom.Element;

public class TriggerSupportTest {

    @Test
    public void snapshot_includesTriggerActionOutputAndBinding() {
        Element button = new Element("button");
        Element field = new Element("input");

        Output<String> value = new PropertyOutput<>(field, "value",
                String.class);
        ClickTrigger trigger = new ClickTrigger(button);
        trigger.triggers(new ClipboardCopyAction(value));

        TriggerSupport support = TriggerSupport.on(button);
        ObjectNode snapshot = support.snapshotForTest();

        // Triggers pool has one click trigger keyed by its assigned id 0
        JsonNode triggers = snapshot.get("triggers");
        Assert.assertEquals(1, triggers.size());
        JsonNode triggerEntry = triggers.get("0");
        Assert.assertNotNull("trigger 0 in pool", triggerEntry);
        Assert.assertEquals(ClickTrigger.TYPE_ID,
                triggerEntry.get("type").asString());

        // Actions pool has one clipboard-copy referencing output id 0
        JsonNode actions = snapshot.get("actions");
        Assert.assertEquals(1, actions.size());
        JsonNode actionEntry = actions.get("0");
        Assert.assertNotNull("action 0 in pool", actionEntry);
        Assert.assertEquals(ClipboardCopyAction.TYPE_ID,
                actionEntry.get("type").asString());
        Assert.assertEquals(0,
                actionEntry.get("config").get("textOutput").asInt());

        // Outputs pool has one property output with element index 1 (host
        // is index 0, field is the first extra element)
        JsonNode outputs = snapshot.get("outputs");
        Assert.assertEquals(1, outputs.size());
        JsonNode outputEntry = outputs.get("0");
        Assert.assertNotNull("output 0 in pool", outputEntry);
        Assert.assertEquals(PropertyOutput.TYPE_ID,
                outputEntry.get("type").asString());
        Assert.assertEquals("value",
                outputEntry.get("config").get("property").asString());
        Assert.assertEquals(1,
                outputEntry.get("config").get("element").asInt());

        // Bindings list has one binding from trigger 0 to action 0
        JsonNode bindings = snapshot.get("bindings");
        Assert.assertEquals(1, bindings.size());
        Assert.assertEquals(0, bindings.get(0).get("trigger").asInt());
        Assert.assertEquals(1, bindings.get(0).get("actions").size());
        Assert.assertEquals(0, bindings.get(0).get("actions").get(0).asInt());

        // The field has been collected as a secondary element parameter
        Assert.assertArrayEquals(new Element[] { field },
                support.elementParamsForTest());
    }

    @Test
    public void sharedAction_dedupedById_acrossMultipleBindings() {
        Element button = new Element("button");
        Element field = new Element("input");
        ClipboardCopyAction copy = new ClipboardCopyAction(
                new PropertyOutput<>(field, "value", String.class));

        ClickTrigger t1 = new ClickTrigger(button);
        ClickTrigger t2 = new ClickTrigger(button);
        t1.triggers(copy);
        t2.triggers(copy);

        TriggerSupport support = TriggerSupport.on(button);
        ObjectNode snapshot = support.snapshotForTest();

        Assert.assertEquals(2, snapshot.get("triggers").size());
        Assert.assertEquals("shared action gets a single entry", 1,
                snapshot.get("actions").size());
        Assert.assertEquals(2, snapshot.get("bindings").size());
    }

    @Test
    public void remove_dropsTriggerAndBindings() {
        Element button = new Element("button");
        Element field = new Element("input");
        ClipboardCopyAction copy = new ClipboardCopyAction(
                new PropertyOutput<>(field, "value", String.class));
        ClickTrigger t1 = new ClickTrigger(button);
        ClickTrigger t2 = new ClickTrigger(button);
        t1.triggers(copy);
        t2.triggers(copy);

        t1.remove();

        ObjectNode snapshot = TriggerSupport.on(button).snapshotForTest();
        Assert.assertEquals(1, snapshot.get("triggers").size());
        Assert.assertEquals(1, snapshot.get("bindings").size());
        Assert.assertEquals(t2.getTriggerId(),
                snapshot.get("bindings").get(0).get("trigger").asInt());
    }

    @Test(expected = IllegalArgumentException.class)
    public void bind_emptyActionsRejected() {
        Element button = new Element("button");
        new ClickTrigger(button).triggers(new Action[0]);
    }
}
