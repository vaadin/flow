/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.server.communication.rpc;

import java.util.Optional;
import java.util.function.Consumer;

import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.JsonConstants;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AbstractRpcInvocationHandlerTest {

    private static class TestRpcInvocationHandler
            extends AbstractRpcInvocationHandler {

        private StateNode node;

        @Override
        public String getRpcType() {
            return null;
        }

        @Override
        protected Optional<Runnable> handleNode(StateNode node,
                JsonNode invocationJson) {
            this.node = node;
            return Optional.of(() -> {
            });
        }
    }

    private TestRpcInvocationHandler handler = new TestRpcInvocationHandler();

    @Test
    public void handleVisibleAndEnabledNode_nodeIsHandled() {
        UI ui = new UI();

        Element element = createRpcInvocationData(ui, null);

        Assert.assertSame(element.getNode(), handler.node);
    }

    @Test
    public void handleInactiveNode_nodeIsNotHandled() {
        UI ui = new UI();

        createRpcInvocationData(ui, elem -> {
            elem.setVisible(false);
            elem.getNode().updateActiveState();

        });

        Assert.assertNull(handler.node);
    }

    @Test
    public void handleInertNode_nodeIsNotHandled() {
        UI ui = new UI();

        createRpcInvocationData(ui, elem -> {
            ElementUtil.setInert(elem, true);
            ui.getInternals().getStateTree().collectDirtyNodes()
                    .forEach(stateNode -> stateNode.collectChanges(change -> {
                    }));
        });

        Assert.assertNull(handler.node);
    }

    @Test
    public void inertUI_passingNoPollingPayload_ignoresPollingInvocation() {

        UI ui = createInertUIWithPollInterval();
        JsonNode invocationJson = createNonPollingRpcInvocationPayload(ui);
        Optional<Runnable> runnable = handler.handle(ui, invocationJson);

        Assert.assertEquals(Optional.empty(), runnable);
    }

    @Test
    public void inertUIWithPollingInterval_passingLegitimatePollingPayload_doesNotIgnorePolling() {

        UI ui = createInertUI();

        JsonNode invocationJson = createLegitimatePollingRpcInvocationPayload(
                ui);
        Optional<Runnable> runnable = handler.handle(ui, invocationJson);
        Assert.assertEquals(Optional.empty(), runnable);

        ui.setPollInterval(0);
        runnable = handler.handle(ui, invocationJson);
        Assert.assertEquals(Optional.empty(), runnable);

        ui.setPollInterval(5000);
        runnable = handler.handle(ui, invocationJson);
        Assert.assertNotEquals(Optional.empty(), runnable);
    }

    @Test
    public void inertUIWithPollingInterval_passingIllegitimateKeysForPollingPayload_ignoresInvocation() {

        UI ui = createInertUIWithPollInterval();
        JsonNode invocationJson = createIllegitimatePayloadKeysPollingRpcInvocationPayload(
                ui);
        Optional<Runnable> runnable = handler.handle(ui, invocationJson);

        Assert.assertEquals(Optional.empty(), runnable);
    }

    @Test
    public void inertUIWithPollingInterval_passingIllegitimateGreaterNumberOfKeysForPollingPayload_ignoresInvocation() {

        UI ui = createInertUIWithPollInterval();
        JsonNode invocationJson = createIllegitimatePayloadWithGreaterSizePollingRpcInvocationPayload(
                ui);
        Optional<Runnable> runnable = handler.handle(ui, invocationJson);

        Assert.assertEquals(Optional.empty(), runnable);
    }

    @Test
    public void inertUIWithPollingInterval_passingIllegitimateSmallerNumberOfKeysForPollingPayload_ignoresInvocation() {

        UI ui = createInertUIWithPollInterval();
        JsonNode invocationJson = createIllegitimatePayloadWithSmallerSizePollingRpcInvocationPayload(
                ui);
        Optional<Runnable> runnable = handler.handle(ui, invocationJson);

        Assert.assertEquals(Optional.empty(), runnable);
    }

    @Test
    public void inertUIWithPollingInterval_passingIllegitimateNoNodeKeyForPollingPayload_throwsAssertionError() {

        UI ui = createInertUIWithPollInterval();
        JsonNode invocationJson = createIllegitimatePayloadNoNodeKeyForPollingRpcInvocationPayload();
        Assert.assertThrows(AssertionError.class,
                () -> handler.handle(ui, invocationJson));
    }

    @Test
    public void inertUIWithPollingInterval_passingIllegitimateNonRootNodeIdForPollingPayload_ignoresInvocation() {

        UI ui = createInertUIWithPollInterval();
        JsonNode invocationJson = createIllegitimatePayloadWithNonRootNodePollingRpcInvocationPayload(
                ui);
        Optional<Runnable> runnable = handler.handle(ui, invocationJson);

        Assert.assertEquals(Optional.empty(), runnable);
    }

    @Test
    public void inertUIWithoutPollInterval_passingLegitimatePollingPayload_logsIgnoredPayloadInDebugLevel() {

        Logger logger = spy(Logger.class);
        try (MockedStatic<LoggerFactory> mockedLoggerFactory = mockStatic(
                LoggerFactory.class)) {
            mockedLoggerFactory
                    .when(() -> LoggerFactory.getLogger(
                            AbstractRpcInvocationHandler.class.getName()))
                    .thenReturn(logger);

            UI ui = createInertUI();
            JsonNode invocationJson = createLegitimatePollingRpcInvocationPayload(
                    ui);
            handler.handle(ui, invocationJson);

            verify(logger, times(1)).warn(anyString());
            verify(logger, times(1)).debug(anyString(), (Object) any());
        }
    }

    @Test
    public void inertUIWithPollingInterval_passingIllegitimatePollingPayload_logsIgnoredPayloadInDebugLevel() {

        Logger logger = spy(Logger.class);
        try (MockedStatic<LoggerFactory> mockedLoggerFactory = mockStatic(
                LoggerFactory.class)) {
            mockedLoggerFactory
                    .when(() -> LoggerFactory.getLogger(
                            AbstractRpcInvocationHandler.class.getName()))
                    .thenReturn(logger);

            UI ui = createInertUIWithPollInterval();
            JsonNode invocationJson = createIllegitimatePayloadKeysPollingRpcInvocationPayload(
                    ui);
            handler.handle(ui, invocationJson);

            verify(logger, times(1)).warn(anyString());
            verify(logger, times(1)).debug(anyString(), (Object) any());
        }
    }

    private Element createRpcInvocationData(UI ui,
            Consumer<Element> additionalConfig) {
        Element element = ElementFactory.createAnchor();
        ui.getElement().appendChild(element);

        if (additionalConfig != null) {
            additionalConfig.accept(element);
        }

        ObjectNode object = JacksonUtils.createObjectNode();
        object.put(JsonConstants.RPC_NODE, element.getNode().getId());
        handler.handle(ui, object);
        return element;
    }

    private UI createInertUIWithPollInterval() {
        UI ui = createInertUI();
        ui.setPollInterval(5000);
        return ui;
    }

    private UI createInertUI() {
        UI ui = new UI();
        ElementUtil.setInert(ui.getElement(), true);
        ui.getInternals().getStateTree().collectDirtyNodes()
                .forEach(stateNode -> stateNode.collectChanges(change -> {
                }));
        return ui;
    }

    private JsonNode createLegitimatePollingRpcInvocationPayload(UI ui) {
        ObjectNode payload = JacksonUtils.createObjectNode();
        payload.put(JsonConstants.RPC_TYPE, JsonConstants.RPC_TYPE_EVENT);
        payload.put(JsonConstants.RPC_NODE, ui.getElement().getNode().getId());
        payload.put(JsonConstants.RPC_EVENT_TYPE, PollEvent.DOM_EVENT_NAME);
        return payload;
    }

    private JsonNode createIllegitimatePayloadKeysPollingRpcInvocationPayload(
            UI ui) {
        ObjectNode payload = JacksonUtils.createObjectNode();
        payload.put(JsonConstants.RPC_EVENT_DATA, "DATA");
        payload.put(JsonConstants.RPC_NODE, ui.getElement().getNode().getId());
        payload.put(JsonConstants.RPC_EVENT_TYPE, PollEvent.DOM_EVENT_NAME);
        return payload;
    }

    private JsonNode createIllegitimatePayloadWithGreaterSizePollingRpcInvocationPayload(
            UI ui) {
        ObjectNode payload = JacksonUtils.createObjectNode();
        payload.put(JsonConstants.RPC_EVENT_DATA, "DATA");
        payload.put(JsonConstants.RPC_TYPE, JsonConstants.RPC_TYPE_EVENT);
        payload.put(JsonConstants.RPC_NODE, ui.getElement().getNode().getId());
        payload.put(JsonConstants.RPC_EVENT_TYPE, PollEvent.DOM_EVENT_NAME);
        return payload;
    }

    private JsonNode createIllegitimatePayloadWithSmallerSizePollingRpcInvocationPayload(
            UI ui) {
        ObjectNode payload = JacksonUtils.createObjectNode();
        payload.put(JsonConstants.RPC_NODE, ui.getElement().getNode().getId());
        payload.put(JsonConstants.RPC_EVENT_TYPE, PollEvent.DOM_EVENT_NAME);
        return payload;
    }

    private JsonNode createIllegitimatePayloadNoNodeKeyForPollingRpcInvocationPayload() {
        ObjectNode payload = JacksonUtils.createObjectNode();
        payload.put(JsonConstants.RPC_TYPE, JsonConstants.RPC_TYPE_EVENT);
        payload.put(JsonConstants.CHANGE_TYPE, "change");
        payload.put(JsonConstants.RPC_EVENT_TYPE, PollEvent.DOM_EVENT_NAME);
        return payload;
    }

    private JsonNode createNonPollingRpcInvocationPayload(UI ui) {
        ObjectNode payload = JacksonUtils.createObjectNode();
        payload.put(JsonConstants.RPC_TYPE, JsonConstants.RPC_TYPE_EVENT);
        payload.put(JsonConstants.RPC_NODE,
                ui.getInternals().getStateTree().getRootNode().getId());
        payload.put(JsonConstants.RPC_EVENT_TYPE,
                JsonConstants.RPC_TYPE_MAP_SYNC);
        return payload;
    }

    private JsonNode createIllegitimatePayloadWithNonRootNodePollingRpcInvocationPayload(
            UI ui) {
        Element element = ElementFactory.createAnchor();
        ui.getElement().appendChild(element);

        ObjectNode payload = JacksonUtils.createObjectNode();
        payload.put(JsonConstants.RPC_TYPE, JsonConstants.RPC_TYPE_EVENT);
        payload.put(JsonConstants.RPC_NODE, element.getNode().getId());
        payload.put(JsonConstants.RPC_EVENT_TYPE, PollEvent.DOM_EVENT_NAME);
        return payload;
    }
}
