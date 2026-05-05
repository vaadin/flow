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
package com.vaadin.flow.internal.nodefeature;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ReturnChannelMapTest {
    private StateNode node = new StateNode(ReturnChannelMap.class);
    private ReturnChannelMap returnChannelMap = node
            .getFeature(ReturnChannelMap.class);

    @Test
    void registerHandler_regstrationHasCorrectData() {
        ReturnChannelRegistration registration = returnChannelMap
                .registerChannel((arguments, channel) -> {
                });

        assertEquals(node.getId(), registration.getStateNodeId(),
                "Node id should match");
        assertSame(registration,
                returnChannelMap.get(registration.getChannelId()),
                "Registration should be findable based on id");
        assertEquals(DisabledUpdateMode.ONLY_WHEN_ENABLED,
                registration.getDisabledUpdateMode(),
                "Default disabled update mode should be to allow when enabled");
    }

    @Test
    void twoArgsHandler_invoked() {
        AtomicReference<ArrayNode> observedArguments = new AtomicReference<>();
        AtomicReference<ReturnChannelRegistration> observedRegistration = new AtomicReference<>();

        ReturnChannelRegistration registration = returnChannelMap
                .registerChannel((arguments, channel) -> {
                    assertNotNull(arguments, "Arguments should not be null");
                    assertNull(observedArguments.getAndSet(arguments),
                            "There should be no previous arguments");
                    assertNull(observedRegistration.getAndSet(channel),
                            "There should be no previous channel");
                });

        assertNull(observedArguments.get(),
                "Handler should not yet be invoked");

        ArrayNode arguments = JacksonUtils.createArrayNode();
        registration.invoke(arguments);

        assertSame(arguments, observedArguments.get());
        assertSame(registration, observedRegistration.get());
    }

    @Test
    void shorthandHandler_invoked() {
        AtomicReference<ArrayNode> observedArguments = new AtomicReference<>();

        ReturnChannelRegistration registration = returnChannelMap
                .registerChannel(arguments -> {
                    assertNotNull(arguments, "Arguments should not be null");
                    assertNull(observedArguments.getAndSet(arguments),
                            "There should be no previous arguments");
                });

        assertNull(observedArguments.get(),
                "Handler should not yet be invoked");

        ArrayNode arguments = JacksonUtils.createArrayNode();
        registration.invoke(arguments);

        assertSame(arguments, observedArguments.get());
    }

}
