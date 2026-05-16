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

public class JsEscapeHatchTest {

    @Test
    public void jsTriggerActionAndArgument_encodeExpressionsAndArgumentIds() {
        TagComponent host = new TagComponent("div");

        JsArgument<String> answer = new JsArgument<>(String.class,
                "return 'forty-two';");
        new JsTrigger(host, "this.addEventListener('dblclick', trigger);")
                .triggers(new JsAction(
                        "this.querySelector('#out').textContent = argument(0);",
                        answer));

        ObjectNode snapshot = TriggerSupport.on(host).snapshotForTest();

        JsonNode trigger = snapshot.get("triggers").get("0");
        Assert.assertEquals(JsTrigger.TYPE_ID, trigger.get("type").asString());
        Assert.assertEquals("this.addEventListener('dblclick', trigger);",
                trigger.get("config").get("expression").asString());

        JsonNode action = snapshot.get("actions").get("0");
        Assert.assertEquals(JsAction.TYPE_ID, action.get("type").asString());
        Assert.assertEquals(
                "this.querySelector('#out').textContent = argument(0);",
                action.get("config").get("expression").asString());
        JsonNode arguments = action.get("config").get("arguments");
        Assert.assertEquals(1, arguments.size());
        Assert.assertEquals(0, arguments.get(0).asInt());

        JsonNode argument = snapshot.get("arguments").get("0");
        Assert.assertEquals(JsArgument.TYPE_ID,
                argument.get("type").asString());
        Assert.assertEquals("return 'forty-two';",
                argument.get("config").get("expression").asString());
    }

    @Test
    public void jsAction_dedupsSharedArgumentAcrossMixedTypes() {
        TagComponent host = new TagComponent("div");
        TagComponent field = new TagComponent("input");

        // One argument reused across two actions of different types: a built-in
        // ClipboardCopyAction and a JsAction. The argument pool should still
        // contain exactly one entry, and both actions should reference id 0.
        Argument<String> value = new PropertyArgument<>(field, "value",
                String.class);
        new JsTrigger(host, "this.addEventListener('input', trigger);")
                .triggers(new ClipboardCopyAction(value),
                        new JsAction("alert(argument(0));", value));

        ObjectNode snapshot = TriggerSupport.on(host).snapshotForTest();
        Assert.assertEquals(1, snapshot.get("arguments").size());

        JsonNode clipboard = snapshot.get("actions").get("0");
        JsonNode js = snapshot.get("actions").get("1");
        Assert.assertEquals(0, clipboard.get("config").get("text").asInt());
        Assert.assertEquals(1, js.get("config").get("arguments").size());
        Assert.assertEquals(0,
                js.get("config").get("arguments").get(0).asInt());
    }
}
