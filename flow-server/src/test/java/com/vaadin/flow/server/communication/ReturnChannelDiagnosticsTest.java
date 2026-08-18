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
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the information that the "Ignoring update for disabled return
 * channel" warning gives to an application developer about which part of the
 * application caused it.
 * <p>
 * The scenario used here is the one from
 * <a href="https://github.com/vaadin/flow/issues/20961">#20961</a>: JavaScript
 * queued for an invisible element is retained in the invocation queue, and
 * retaining it subscribes to its return value so that the invocation can be
 * released if the node is detached. The subscription makes {@link UidlWriter}
 * register return channels for the invocation, so the warning is logged when
 * the invocation finally completes while the element is disabled - without the
 * application using {@code @ClientCallable}, a {@code LitRenderer} or a return
 * value callback of its own.
 *
 * @see <a href="https://github.com/vaadin/flow/issues/20464">#20464</a>
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
    void plainExecuteJs_ownerDisabledWhenCompleted_logsWarning() {
        MockUI ui = new MockUI();
        Widget widget = new Widget();
        ui.getElement().appendChild(widget.getElement());

        sendJsThatWasQueuedWhileInvisible(ui, widget, "this.doSomething()");
        widget.getElement().setEnabled(false);

        List<String> warnings = handleChannelMessage(ui, widget);

        assertEquals(1, warnings.size(),
                () -> "Exactly one warning expected, got: " + warnings);
        assertTrue(warnings.get(0).contains("disabled return channel"),
                () -> "Unexpected warning: " + warnings.get(0));
    }

    @Test
    void disabledElement_warningIdentifiesElementComponentAndRoute() {
        MockUI ui = new MockUI();
        OrdersView view = new OrdersView();
        Widget widget = new Widget();
        ui.getElement().appendChild(view.getElement());
        view.getElement().appendChild(widget.getElement());

        sendJsThatWasQueuedWhileInvisible(ui, widget, "this.doSomething()");
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

        sendJsThatWasQueuedWhileInvisible(ui, widget, "this.doSomething()");
        // The widget itself is enabled, it is the view that is disabled
        view.getElement().setEnabled(false);

        String warning = handleChannelMessage(ui, widget).get(0);

        assertTrue(warning.contains(OrdersView.class.getName()),
                () -> "The warning should name the ancestor that is actually "
                        + "disabled: " + warning);
    }

    @Test
    void executeJsReturnChannel_warningIdentifiesTheJavaScriptExpression() {
        MockUI ui = new MockUI();
        Widget widget = new Widget();
        ui.getElement().appendChild(widget.getElement());

        sendJsThatWasQueuedWhileInvisible(ui, widget, "this.doSomething()");
        widget.getElement().setEnabled(false);

        String warning = handleChannelMessage(ui, widget).get(0);

        assertTrue(warning.contains("this.doSomething()"),
                () -> "The warning should tell what registered the channel, "
                        + "here the executeJs expression: " + warning);
    }

    /**
     * Queues JavaScript for the given component while it is invisible, then
     * makes it visible and encodes the invocation the same way as when it is
     * sent to the browser, which is when its return channels are registered.
     */
    private void sendJsThatWasQueuedWhileInvisible(MockUI ui,
            Component component, String expression) {
        component.getElement().setVisible(false);
        component.getElement().executeJs(expression);

        assertEquals(List.of(), ui.dumpPendingJsInvocations(),
                "The invocation should be retained while invisible");

        component.getElement().setVisible(true);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(1, invocations.size(),
                "The retained invocation should now be sent");
        UidlWriter.encodeExecuteJavaScriptList(invocations);
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
