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

public class JsEscapeHatchTest {

    @Test
    public void jsTriggerActionAndOutput_encodeExpressionsAndOutputIds() {
        Element host = new Element("div");

        JsOutput<String> answer = new JsOutput<>(String.class,
                "return 'forty-two';");
        new JsTrigger(host, "this.addEventListener('dblclick', trigger);")
                .triggers(new JsAction(
                        "this.querySelector('#out').textContent = output(0);",
                        answer));

        ObjectNode snapshot = TriggerSupport.on(host).snapshotForTest();

        JsonNode trigger = snapshot.get("triggers").get("0");
        Assert.assertEquals(JsTrigger.TYPE_ID, trigger.get("type").asString());
        Assert.assertEquals("this.addEventListener('dblclick', trigger);",
                trigger.get("config").get("expression").asString());

        JsonNode action = snapshot.get("actions").get("0");
        Assert.assertEquals(JsAction.TYPE_ID, action.get("type").asString());
        Assert.assertEquals(
                "this.querySelector('#out').textContent = output(0);",
                action.get("config").get("expression").asString());
        JsonNode outputs = action.get("config").get("outputs");
        Assert.assertEquals(1, outputs.size());
        Assert.assertEquals(0, outputs.get(0).asInt());

        JsonNode output = snapshot.get("outputs").get("0");
        Assert.assertEquals(JsOutput.TYPE_ID, output.get("type").asString());
        Assert.assertEquals("return 'forty-two';",
                output.get("config").get("expression").asString());
    }

    @Test
    public void jsAction_dedupsSharedOutputAcrossMixedTypes() {
        Element host = new Element("div");
        Element field = new Element("input");

        // One output reused across two actions of different types: a built-in
        // ClipboardCopyAction and a JsAction. The output pool should still
        // contain exactly one entry, and both actions should reference id 0.
        Output<String> value = new PropertyOutput<>(field, "value",
                String.class);
        new JsTrigger(host, "this.addEventListener('input', trigger);")
                .triggers(new ClipboardCopyAction(value),
                        new JsAction("alert(output(0));", value));

        ObjectNode snapshot = TriggerSupport.on(host).snapshotForTest();
        Assert.assertEquals(1, snapshot.get("outputs").size());

        JsonNode clipboard = snapshot.get("actions").get("0");
        JsonNode js = snapshot.get("actions").get("1");
        Assert.assertEquals(0,
                clipboard.get("config").get("textOutput").asInt());
        Assert.assertEquals(1, js.get("config").get("outputs").size());
        Assert.assertEquals(0, js.get("config").get("outputs").get(0).asInt());
    }
}
