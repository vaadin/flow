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
import com.vaadin.flow.signals.local.ValueSignal;

public class SignalOutputTest {

    @Test
    public void buildClientConfig_shipsCurrentSignalValue() {
        Element button = new Element("button");
        ValueSignal<String> signal = new ValueSignal<>("alpha");

        new ClickTrigger(button).triggers(new ClipboardCopyAction(
                new SignalOutput<>(String.class, signal)));

        ObjectNode snapshot = TriggerSupport.on(button).snapshotForTest();
        JsonNode output = snapshot.get("outputs").get("0");
        Assert.assertEquals(SignalOutput.TYPE_ID,
                output.get("type").asString());
        Assert.assertEquals("alpha",
                output.get("config").get("value").asString());
    }

    @Test
    public void buildClientConfig_reflectsSignalUpdates() {
        Element button = new Element("button");
        ValueSignal<String> signal = new ValueSignal<>("alpha");
        new ClickTrigger(button).triggers(new ClipboardCopyAction(
                new SignalOutput<>(String.class, signal)));

        // First snapshot
        ObjectNode first = TriggerSupport.on(button).snapshotForTest();
        Assert.assertEquals("alpha", first.get("outputs").get("0").get("config")
                .get("value").asString());

        // Mutate the signal, then rebuild
        signal.set("beta");
        ObjectNode second = TriggerSupport.on(button).snapshotForTest();
        Assert.assertEquals("beta", second.get("outputs").get("0").get("config")
                .get("value").asString());
    }
}
