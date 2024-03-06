/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication.rpc;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.server.communication.ReturnChannelHandler;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.tests.util.MockUI;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class ReturnChannelHandlerTest {
    private MockUI ui = new MockUI();

    private AtomicReference<JsonArray> observedArguments = new AtomicReference<>();
    private SerializableConsumer<JsonArray> observingConsumer = arguments -> {
        Assert.assertNotNull("Arguments should not be null", arguments);
        Assert.assertNull("There should be no previous arguments",
                observedArguments.getAndSet(arguments));
    };

    private JsonArray args = Json.createArray();

    @Test
    public void happyPath_everythingWorks() {
        ReturnChannelRegistration registration = registerUiChannel();

        handleMessage(registration);

        Assert.assertSame(
                "Handler should have been invoked with the given arguments.",
                args, observedArguments.get());
    }

    @Test
    public void noReturnChannelMap_invocationIgnored() {
        StateNode nodeWithoutMap = new StateNode();

        ui.getElement().getNode().getFeature(ElementChildrenList.class).add(0,
                nodeWithoutMap);

        handleMessage(nodeWithoutMap.getId(), 0);

        // Nothing to assert, just checking that no exception is thrown
    }

    @Test
    public void returnChannelMapNotInitialized_noInitializedAfterInvocation() {
        handleMessage(ui.getElement().getNode().getId(), 0);

        Assert.assertFalse("Feature should not be initialized",
                ui.getElement().getNode()
                        .getFeatureIfInitialized(ReturnChannelMap.class)
                        .isPresent());
    }

    @Test
    public void unregisteredChannel_invocationIgnored() {
        ReturnChannelRegistration registration = registerUiChannel();
        registration.remove();

        handleMessage(registration);

        Assert.assertNull("Channel handler should not be called",
                observedArguments.get());
    }

    @Test
    public void disabledElement_defaultRegistration_invocationIgnored() {
        ReturnChannelRegistration registration = registerUiChannel();

        ui.setEnabled(false);

        handleMessage(registration);

        Assert.assertNull("Channel handler should not be called",
                observedArguments.get());
    }

    @Test
    public void disabledElement_registrationAlwaysAllowed_invocationProcessed() {
        ReturnChannelRegistration registration = registerUiChannel();
        registration.setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        ui.setEnabled(false);

        handleMessage(registration);

        Assert.assertNotNull("Channel handler should be called",
                observedArguments.get());
    }

    @Test
    public void modalComponent_registrationExists_invocationProcessed() {
        ReturnChannelRegistration registration = registerUiChannel();

        Div modal = new Div();
        ui.addModal(modal);

        handleMessage(registration);

        Assert.assertNotNull("Channel handler should be called",
                observedArguments.get());
    }

    @Test
    public void modalComponent_unregisteredChannel_invocationIgnored() {
        ReturnChannelRegistration registration = registerUiChannel();
        registration.remove();

        Div modal = new Div();
        ui.addModal(modal);

        handleMessage(registration);

        Assert.assertNull("Channel handler should not be called",
                observedArguments.get());
    }

    private void handleMessage(ReturnChannelRegistration registration) {
        handleMessage(registration.getStateNodeId(),
                registration.getChannelId());
    }

    private ReturnChannelRegistration registerUiChannel() {
        ReturnChannelRegistration registration = ui.getElement().getNode()
                .getFeature(ReturnChannelMap.class)
                .registerChannel(observingConsumer);
        return registration;
    }

    private void handleMessage(int nodeId, int channelId) {
        JsonObject invocationJson = createInvocationJson(nodeId, channelId);

        new ReturnChannelHandler().handle(ui, invocationJson);
    }

    private JsonObject createInvocationJson(int stateNodeId, int channelId) {
        JsonObject invocationJson = Json.createObject();

        invocationJson.put(JsonConstants.RPC_NODE, stateNodeId);
        invocationJson.put(JsonConstants.RPC_CHANNEL, channelId);
        invocationJson.put(JsonConstants.RPC_CHANNEL_ARGUMENTS, args);

        return invocationJson;
    }

    @Tag("div")
    private class Div extends Component {
    }
}
