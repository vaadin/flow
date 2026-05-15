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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.trigger.internal.ServerCallbackAction;
import com.vaadin.flow.component.trigger.internal.TriggerSupport;
import com.vaadin.flow.dom.Element;

public class ServerCallbackTest {

    @Test
    public void triggers_runnable_addsServerCallbackToSnapshot() {
        Element button = new Element("button");
        AtomicInteger calls = new AtomicInteger();

        new ClickTrigger(button).triggers(() -> calls.incrementAndGet());

        ObjectNode snapshot = TriggerSupport.on(button).snapshotForTest();
        JsonNode action = snapshot.get("actions").get("0");
        Assert.assertEquals(ServerCallbackAction.TYPE_ID,
                action.get("type").asString());
        Assert.assertEquals(0, calls.get());
    }

    @Test
    public void dispatchMirror_runsWrappedRunnable() {
        Element button = new Element("button");
        AtomicInteger calls = new AtomicInteger();

        new ClickTrigger(button).triggers(() -> calls.incrementAndGet());

        TriggerSupport.on(button).dispatchMirrorForTest(0);

        Assert.assertEquals(1, calls.get());
    }
}
