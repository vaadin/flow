/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.internal.StateNode;

import elemental.json.Json;
import elemental.json.JsonArray;

public class ReturnChannelMapTest {
    private StateNode node = new StateNode(ReturnChannelMap.class);
    private ReturnChannelMap returnChannelMap = node
            .getFeature(ReturnChannelMap.class);

    @Test
    public void registerHandler_regstrationHasCorrectData() {
        ReturnChannelRegistration registration = returnChannelMap
                .registerChannel((arguments, channel) -> {
                });

        Assert.assertEquals("Node id should match", node.getId(),
                registration.getStateNodeId());
        Assert.assertSame("Registration should be findable based on id",
                registration,
                returnChannelMap.get(registration.getChannelId()));
        Assert.assertEquals(
                "Default disabled update mode should be to allow when enabled",
                DisabledUpdateMode.ONLY_WHEN_ENABLED,
                registration.getDisabledUpdateMode());
    }

    @Test
    public void twoArgsHandler_invoked() {
        AtomicReference<JsonArray> observedArguments = new AtomicReference<>();
        AtomicReference<ReturnChannelRegistration> observedRegistration = new AtomicReference<>();

        ReturnChannelRegistration registration = returnChannelMap
                .registerChannel((arguments, channel) -> {
                    Assert.assertNotNull("Arguments should not be null",
                            arguments);
                    Assert.assertNull("There should be no previous arguments",
                            observedArguments.getAndSet(arguments));
                    Assert.assertNull("There should be no previous channel",
                            observedRegistration.getAndSet(channel));
                });

        Assert.assertNull("Handler should not yet be invoked",
                observedArguments.get());

        JsonArray arguments = Json.createArray();
        registration.invoke(arguments);

        Assert.assertSame(arguments, observedArguments.get());
        Assert.assertSame(registration, observedRegistration.get());
    }

    @Test
    public void shorthandHandler_invoked() {
        AtomicReference<JsonArray> observedArguments = new AtomicReference<>();

        ReturnChannelRegistration registration = returnChannelMap
                .registerChannel(arguments -> {
                    Assert.assertNotNull("Arguments should not be null",
                            arguments);
                    Assert.assertNull("There should be no previous arguments",
                            observedArguments.getAndSet(arguments));
                });

        Assert.assertNull("Handler should not yet be invoked",
                observedArguments.get());

        JsonArray arguments = Json.createArray();
        registration.invoke(arguments);

        Assert.assertSame(arguments, observedArguments.get());
    }

}
