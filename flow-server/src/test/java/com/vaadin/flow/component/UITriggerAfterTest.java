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
package com.vaadin.flow.component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.server.communication.ReturnChannelHandler;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UITriggerAfterTest {
    private final MockUI ui = new MockUI();

    @Test
    void triggerAfter_channelInvoked_runsTask() {
        AtomicInteger count = new AtomicInteger();

        ui.triggerAfter(Duration.ofMillis(100), count::incrementAndGet);

        handleMessage(onlyChannelId());

        assertEquals(1, count.get(),
                "Task should have been run when the channel was invoked");
    }

    @Test
    void triggerAfter_negativeDelay_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ui.triggerAfter(Duration.ofMillis(-1), () -> {
                }));
    }

    /**
     * Finds the id of the single channel that {@code triggerAfter} registered
     * on the UI node, simulating the client round trip that the timer would
     * otherwise make.
     */
    private int onlyChannelId() {
        ReturnChannelMap map = ui.getElement().getNode()
                .getFeature(ReturnChannelMap.class);
        for (int id = 0; id < 100; id++) {
            if (map.get(id) != null) {
                return id;
            }
        }
        throw new AssertionError("No channel was registered");
    }

    private void handleMessage(int channelId) {
        ObjectNode invocationJson = JacksonUtils.createObjectNode();
        invocationJson.put(JsonConstants.RPC_NODE,
                ui.getElement().getNode().getId());
        invocationJson.put(JsonConstants.RPC_CHANNEL, channelId);
        invocationJson.set(JsonConstants.RPC_CHANNEL_ARGUMENTS,
                JacksonUtils.createArrayNode());

        new ReturnChannelHandler().handle(ui, invocationJson);
    }
}
