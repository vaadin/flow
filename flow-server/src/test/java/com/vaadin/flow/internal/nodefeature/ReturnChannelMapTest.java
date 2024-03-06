/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
