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

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.trigger.internal.TriggerSupport;

public class ShortcutAndMirrorTest {

    @Test
    public void shortcutTrigger_encodesKeyAndModifiers() {
        TagComponent form = new TagComponent("form");
        TagComponent submit = new TagComponent("button");

        new ShortcutTrigger(form, Key.ENTER, KeyModifier.CONTROL,
                KeyModifier.SHIFT).triggers(new ClickAction(submit));

        ObjectNode snapshot = TriggerSupport.on(form).snapshotForTest();
        JsonNode trigger = snapshot.get("triggers").get("0");
        Assert.assertEquals(ShortcutTrigger.TYPE_ID,
                trigger.get("type").asString());
        JsonNode cfg = trigger.get("config");
        Assert.assertEquals("Enter", cfg.get("key").asString());
        JsonNode mods = cfg.get("modifiers");
        Assert.assertEquals(2, mods.size());
        java.util.Set<String> values = new java.util.HashSet<>();
        mods.forEach(n -> values.add(n.asString()));
        Assert.assertEquals(java.util.Set.of("Control", "Shift"), values);
    }

    @Test
    public void setEnabledAction_encodesElementAndMirrorFlag() {
        TagComponent form = new TagComponent("form");
        TagComponent submit = new TagComponent("button");

        new ShortcutTrigger(form, Key.ENTER)
                .triggers(new SetEnabledAction(submit, false));

        ObjectNode snapshot = TriggerSupport.on(form).snapshotForTest();
        JsonNode action = snapshot.get("actions").get("0");
        Assert.assertEquals(SetEnabledAction.TYPE_ID,
                action.get("type").asString());
        JsonNode cfg = action.get("config");
        Assert.assertEquals(1, cfg.get("element").asInt());
        Assert.assertFalse(cfg.get("enabled").asBoolean());
        Assert.assertTrue(cfg.get("mirror").asBoolean());
    }

    @Test
    public void dispatchMirror_appliesSetEnabledServerSide() {
        TagComponent form = new TagComponent("form");
        TagComponent submit = new TagComponent("button");
        Assert.assertTrue("button starts enabled",
                submit.getElement().isEnabled());

        SetEnabledAction disable = new SetEnabledAction(submit, false);
        new ShortcutTrigger(form, Key.ENTER).triggers(disable);

        // Simulate the client reporting that action 0 fired locally.
        TriggerSupport.on(form).dispatchMirrorForTest(0);

        Assert.assertFalse("server-side button is now disabled",
                submit.getElement().isEnabled());
    }
}
