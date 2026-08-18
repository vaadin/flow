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
package com.vaadin.flow.server.communication;

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
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for what happens when the return value of a JavaScript execution
 * arrives while its target is disabled, and for the information that the
 * "Ignoring update for disabled return channel" warning gives to an application
 * developer about which part of the application caused it.
 * <p>
 * JavaScript queued for an invisible element is retained in the invocation
 * queue, and retaining it makes the framework track the completion of the
 * invocation so that it can be released if the node is detached. That tracking
 * needs return channels even when the application isn't interested in the
 * return value, which is how applications end up seeing warnings about return
 * channels they never registered themselves.
 *
 * @see <a href="https://github.com/vaadin/flow/issues/20464">#20464</a>
 * @see <a href="https://github.com/vaadin/flow/issues/20961">#20961</a>
 */
class ReturnChannelDiagnosticsTest {

    @Tag("div")
    @Route("orders")
    private static class OrdersView extends Component {
    }

    @Tag("my-widget")
    private static class Widget extends Component {
    }

    @Test
    void frameworkTrackedJs_targetDisabledWhenCompleted_cleanedUpSilently() {
        MockUI ui = new MockUI();
        Widget widget = new Widget();
        ui.getElement().appendChild(widget.getElement());

        sendJsQueuedWhileInvisible(ui, widget, "this.doSomething()");
        widget.getElement().setEnabled(false);

        List<String> warnings = handleChannelMessage(ui, widget);

        assertEquals(List.of(), warnings,
                "Releasing the framework's own tracking should not warn the "
                        + "application about anything");
        assertFalse(hasChannels(widget),
                "The return channels of the completed invocation should have "
                        + "been removed");
    }

    @Test
    void applicationSubscribedJs_targetDisabled_valueDroppedWithWarning() {
        MockUI ui = new MockUI();
        Widget widget = new Widget();
        ui.getElement().appendChild(widget.getElement());

        AtomicReference<JsonNode> returnValue = new AtomicReference<>();
        sendSubscribedJs(ui, widget, "return this.getSomething()",
                returnValue::set);
        widget.getElement().setEnabled(false);

        List<String> warnings = handleChannelMessage(ui, widget);

        assertEquals(1, warnings.size(),
                () -> "Dropping a value the application is waiting for should "
                        + "still be warned about, got: " + warnings);
        assertNull(returnValue.get(),
                "The return value should not be delivered while disabled");
    }

    @Test
    void disabledTarget_warningIdentifiesElementComponentAndRoute() {
        MockUI ui = new MockUI();
        OrdersView view = new OrdersView();
        Widget widget = new Widget();
        ui.getElement().appendChild(view.getElement());
        view.getElement().appendChild(widget.getElement());

        sendSubscribedJs(ui, widget, "return this.getSomething()", value -> {
        });
        widget.getElement().setEnabled(false);

        String warning = handleChannelMessage(ui, widget).get(0);

        assertTrue(warning.contains("my-widget"),
                () -> "The warning should name the element tag: " + warning);
        assertTrue(warning.contains(Widget.class.getName()),
                () -> "The warning should name the component class: "
                        + warning);
        assertTrue(warning.contains("orders"),
                () -> "The warning should name the route the component is in: "
                        + warning);
    }

    @Test
    void disabledAncestor_warningIdentifiesTheDisabledAncestor() {
        MockUI ui = new MockUI();
        OrdersView view = new OrdersView();
        Widget widget = new Widget();
        ui.getElement().appendChild(view.getElement());
        view.getElement().appendChild(widget.getElement());

        sendSubscribedJs(ui, widget, "return this.getSomething()", value -> {
        });
        // The widget itself is enabled, it is the view that is disabled
        view.getElement().setEnabled(false);

        String warning = handleChannelMessage(ui, widget).get(0);

        assertTrue(warning.contains(OrdersView.class.getName()),
                () -> "The warning should name the ancestor that is actually "
                        + "disabled: " + warning);
    }

    /**
     * Queues JavaScript for the given component while it is invisible, then
     * makes it visible and encodes the invocation the same way as when it is
     * sent to the browser, which is when its return channels are registered.
     */
    private void sendJsQueuedWhileInvisible(MockUI ui, Component component,
            String expression) {
        component.getElement().setVisible(false);
        component.getElement().executeJs(expression);

        assertEquals(List.of(), ui.dumpPendingJsInvocations(),
                "The invocation should be retained while invisible");

        component.getElement().setVisible(true);

        encodeForBrowser(ui.dumpPendingJsInvocations());
    }

    /**
     * Executes JavaScript for the given component with the application
     * subscribing to the return value, and encodes the invocation the same way
     * as when it is sent to the browser.
     */
    private void sendSubscribedJs(MockUI ui, Component component,
            String expression, SerializableConsumer<JsonNode> handler) {
        component.getElement().executeJs(expression).then(handler);

        encodeForBrowser(ui.dumpPendingJsInvocations());
    }

    private void encodeForBrowser(
            List<PendingJavaScriptInvocation> invocations) {
        assertEquals(1, invocations.size(),
                "Exactly one invocation should be sent to the browser");
        UidlWriter.encodeExecuteJavaScriptList(invocations);
    }

    private boolean hasChannels(Component component) {
        return component.getElement().getNode()
                .getFeatureIfInitialized(ReturnChannelMap.class)
                .map(ReturnChannelMap::hasChannels).orElse(false);
    }

    /**
     * Simulates the browser calling the first return channel registered for the
     * given component's element and returns the warnings that were logged.
     */
    private List<String> handleChannelMessage(MockUI ui, Component component) {
        ArrayNode arguments = JacksonUtils.createArrayNode();
        arguments.addNull();

        ObjectNode invocationJson = JacksonUtils.createObjectNode();
        invocationJson.put(JsonConstants.RPC_TYPE,
                JsonConstants.RPC_TYPE_CHANNEL);
        invocationJson.put(JsonConstants.RPC_NODE,
                component.getElement().getNode().getId());
        invocationJson.put(JsonConstants.RPC_CHANNEL, 0);
        invocationJson.set(JsonConstants.RPC_CHANNEL_ARGUMENTS, arguments);

        List<String> warnings = new ArrayList<>();
        Logger logger = Mockito.mock(Logger.class,
                invocation -> recordWarning(invocation, warnings));

        try (MockedStatic<LoggerFactory> loggerFactory = Mockito
                .mockStatic(LoggerFactory.class)) {
            loggerFactory
                    .when(() -> LoggerFactory
                            .getLogger(ReturnChannelHandler.class.getName()))
                    .thenReturn(logger);

            new ReturnChannelHandler().handle(ui, invocationJson);
        }

        return warnings;
    }

    private static Object recordWarning(InvocationOnMock invocation,
            List<String> warnings) throws Throwable {
        if ("warn".equals(invocation.getMethod().getName())) {
            Object[] arguments = invocation.getArguments();
            Object[] parameters = Arrays.stream(arguments).skip(1)
                    .flatMap(argument -> argument instanceof Object[] array
                            ? Arrays.stream(array)
                            : Stream.of(argument))
                    .toArray();
            warnings.add(MessageFormatter
                    .arrayFormat((String) arguments[0], parameters)
                    .getMessage());
        }
        return Mockito.RETURNS_DEFAULTS.answer(invocation);
    }
}
