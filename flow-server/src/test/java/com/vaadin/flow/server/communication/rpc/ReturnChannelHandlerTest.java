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
package com.vaadin.flow.server.communication.rpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.communication.ReturnChannelHandler;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReturnChannelHandlerTest {
    private MockUI ui = new MockUI();

    private AtomicReference<JsonNode> observedArguments = new AtomicReference<>();
    private SerializableConsumer<ArrayNode> observingConsumer = arguments -> {
        assertNotNull(arguments, "Arguments should not be null");
        assertNull(observedArguments.getAndSet(arguments),
                "There should be no previous arguments");
    };

    private ArrayNode args = JacksonUtils.createArrayNode();

    @Test
    void happyPath_everythingWorks() {
        ReturnChannelRegistration registration = registerUiChannel();

        handleMessage(registration);

        assertSame(args, observedArguments.get(),
                "Handler should have been invoked with the given arguments.");
    }

    @Test
    void noReturnChannelMap_invocationIgnored() {
        StateNode nodeWithoutMap = new StateNode();

        ui.getElement().getNode().getFeature(ElementChildrenList.class).add(0,
                nodeWithoutMap);

        handleMessage(nodeWithoutMap.getId(), 0);

        // Nothing to assert, just checking that no exception is thrown
    }

    @Test
    void returnChannelMapNotInitialized_noInitializedAfterInvocation() {
        handleMessage(ui.getElement().getNode().getId(), 0);

        assertFalse(ui.getElement().getNode()
                .getFeatureIfInitialized(ReturnChannelMap.class).isPresent(),
                "Feature should not be initialized");
    }

    @Test
    void unregisteredChannel_invocationIgnored() {
        ReturnChannelRegistration registration = registerUiChannel();
        registration.remove();

        handleMessage(registration);

        assertNull(observedArguments.get(),
                "Channel handler should not be called");
    }

    @Test
    void disabledElement_defaultRegistration_invocationIgnored() {
        ReturnChannelRegistration registration = registerUiChannel();

        ui.setEnabled(false);

        handleMessage(registration);

        assertNull(observedArguments.get(),
                "Channel handler should not be called");
    }

    @Test
    void disabledElement_registrationAlwaysAllowed_invocationProcessed() {
        ReturnChannelRegistration registration = registerUiChannel();
        registration.setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        ui.setEnabled(false);

        handleMessage(registration);

        assertNotNull(observedArguments.get(),
                "Channel handler should be called");
    }

    @Test
    void modalComponent_registrationExists_invocationProcessed() {
        ReturnChannelRegistration registration = registerUiChannel();

        Div modal = new Div();
        ui.addModal(modal);

        handleMessage(registration);

        assertNotNull(observedArguments.get(),
                "Channel handler should be called");
    }

    @Test
    void modalComponent_unregisteredChannel_invocationIgnored() {
        ReturnChannelRegistration registration = registerUiChannel();
        registration.remove();

        Div modal = new Div();
        ui.addModal(modal);

        handleMessage(registration);

        assertNull(observedArguments.get(),
                "Channel handler should not be called");
    }

    @Test
    void disabledElement_warningIdentifiesElementComponentAndRoute() {
        Widget widget = addWidgetToOrdersView();
        ReturnChannelRegistration registration = registerChannel(widget);

        widget.getElement().setEnabled(false);

        String warning = getOnlyWarning(registration);

        assertTrue(warning.contains("my-widget"),
                () -> "The warning should name the element tag: " + warning);
        assertTrue(warning.contains(Widget.class.getName()),
                () -> "The warning should name the component class: "
                        + warning);
        assertTrue(warning.contains("orders"),
                () -> "The warning should name the route the component is in: "
                        + warning);
        assertTrue(warning.contains("The target itself is disabled"),
                () -> "The warning should tell that the target itself is the "
                        + "disabled one: " + warning);
        assertFalse(warning.contains(OrdersView.class.getName()),
                () -> "The enclosing view is not the disabled one, so it "
                        + "should not be blamed: " + warning);
    }

    @Test
    void disabledAncestor_warningIdentifiesTheDisabledAncestor() {
        Widget widget = addWidgetToOrdersView();
        ReturnChannelRegistration registration = registerChannel(widget);

        // The widget itself is enabled, it is the view that is disabled
        widget.getParent().orElseThrow().getElement().setEnabled(false);

        String warning = getOnlyWarning(registration);

        assertTrue(warning.contains(OrdersView.class.getName()),
                () -> "The warning should name the ancestor that is actually "
                        + "disabled: " + warning);
    }

    @Test
    void disabledElement_clientArgumentsLoggedOnlyOnDebugLevel() {
        Widget widget = addWidgetToOrdersView();
        ReturnChannelRegistration registration = registerChannel(widget);

        // Stands for whatever the application reads from the browser, which
        // can be data that doesn't belong in a log file
        args.add("account-FI7654321000000");
        widget.getElement().setEnabled(false);

        LoggedMessages logged = handleMessageCapturingLogs(registration);

        assertFalse(
                logged.warnings().get(0).contains("account-FI7654321000000"),
                () -> "The warning should not contain what the client sent: "
                        + logged.warnings().get(0));
        assertTrue(
                logged.debugMessages().stream().anyMatch(
                        message -> message.contains("account-FI7654321000000")),
                () -> "The payload should still be available on debug level: "
                        + logged.debugMessages());
    }

    @Test
    void noReturnChannelMap_warningIdentifiesTarget() {
        StateNode nodeWithoutMap = new StateNode();

        ui.getElement().getNode().getFeature(ElementChildrenList.class).add(0,
                nodeWithoutMap);

        String warning = getOnlyWarning(nodeWithoutMap.getId(), 0);

        assertTrue(warning.contains("no return channels"),
                () -> "Unexpected warning: " + warning);
        assertTrue(warning.contains("node id=" + nodeWithoutMap.getId()),
                () -> "The warning should identify the node: " + warning);
    }

    @Test
    void unregisteredChannel_warningIdentifiesTarget() {
        Widget widget = addWidgetToOrdersView();
        ReturnChannelRegistration registration = registerChannel(widget);
        registration.remove();

        String warning = getOnlyWarning(registration);

        assertTrue(warning.contains("not found"),
                () -> "Unexpected warning: " + warning);
        assertTrue(warning.contains("my-widget"),
                () -> "The warning should name the element tag: " + warning);
    }

    private Widget addWidgetToOrdersView() {
        OrdersView view = new OrdersView();
        Widget widget = new Widget();

        ui.getElement().appendChild(view.getElement());
        view.getElement().appendChild(widget.getElement());

        return widget;
    }

    private ReturnChannelRegistration registerChannel(Component component) {
        return component.getElement().getNode()
                .getFeature(ReturnChannelMap.class)
                .registerChannel(observingConsumer);
    }

    private String getOnlyWarning(ReturnChannelRegistration registration) {
        return getOnlyWarning(registration.getStateNodeId(),
                registration.getChannelId());
    }

    private String getOnlyWarning(int nodeId, int channelId) {
        List<String> warnings = handleMessageCapturingLogs(nodeId, channelId)
                .warnings();

        assertEquals(1, warnings.size(),
                () -> "Exactly one warning expected, got: " + warnings);

        return warnings.get(0);
    }

    private LoggedMessages handleMessageCapturingLogs(
            ReturnChannelRegistration registration) {
        return handleMessageCapturingLogs(registration.getStateNodeId(),
                registration.getChannelId());
    }

    private LoggedMessages handleMessageCapturingLogs(int nodeId,
            int channelId) {
        List<String> warnings = new ArrayList<>();
        List<String> debugMessages = new ArrayList<>();
        Logger logger = Mockito.mock(Logger.class,
                invocation -> record(invocation, warnings, debugMessages));

        // Only the handler's own logger is mocked. Invoking a channel runs
        // application and framework code that may log or cache a logger of its
        // own, so everything else keeps using the real logger factory.
        try (MockedStatic<LoggerFactory> loggerFactory = Mockito
                .mockStatic(LoggerFactory.class, Mockito.CALLS_REAL_METHODS)) {
            loggerFactory
                    .when(() -> LoggerFactory
                            .getLogger(ReturnChannelHandler.class.getName()))
                    .thenReturn(logger);

            handleMessage(nodeId, channelId);
        }

        return new LoggedMessages(warnings, debugMessages);
    }

    private static Object record(InvocationOnMock invocation,
            List<String> warnings, List<String> debugMessages)
            throws Throwable {
        String method = invocation.getMethod().getName();
        if ("warn".equals(method) || "debug".equals(method)) {
            Object[] arguments = invocation.getArguments();
            Object[] parameters = Arrays.stream(arguments).skip(1)
                    .flatMap(argument -> argument instanceof Object[] array
                            ? Arrays.stream(array)
                            : Stream.of(argument))
                    .toArray();
            String message = MessageFormatter
                    .arrayFormat((String) arguments[0], parameters)
                    .getMessage();
            ("warn".equals(method) ? warnings : debugMessages).add(message);
        }
        return Mockito.RETURNS_DEFAULTS.answer(invocation);
    }

    private record LoggedMessages(List<String> warnings,
            List<String> debugMessages) {
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
        JsonNode invocationJson = createInvocationJson(nodeId, channelId);

        new ReturnChannelHandler().handle(ui, invocationJson);
    }

    private JsonNode createInvocationJson(int stateNodeId, int channelId) {
        ObjectNode invocationJson = JacksonUtils.createObjectNode();

        invocationJson.put(JsonConstants.RPC_NODE, stateNodeId);
        invocationJson.put(JsonConstants.RPC_CHANNEL, channelId);
        invocationJson.set(JsonConstants.RPC_CHANNEL_ARGUMENTS, args);

        return invocationJson;
    }

    @Tag("div")
    private class Div extends Component {
    }

    @Tag("div")
    @Route("orders")
    private static class OrdersView extends Component {
    }

    @Tag("my-widget")
    private static class Widget extends Component {
    }
}
