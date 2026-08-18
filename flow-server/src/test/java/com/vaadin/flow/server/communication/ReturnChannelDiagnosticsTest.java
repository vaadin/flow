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
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the information that the "Ignoring update for disabled return
 * channel" warning gives to an application developer about which part of the
 * application caused it.
 * <p>
 * The warning is not always about a return channel that the application
 * registered itself: JavaScript queued for an invisible element is retained in
 * the invocation queue, and retaining it makes the framework track the
 * completion of the invocation so that it can be released if the node is
 * detached. That tracking needs return channels even for a plain
 * fire-and-forget {@code executeJs}, which is how applications end up seeing
 * the warning without using {@code @ClientCallable}, a {@code LitRenderer} or a
 * return value callback of their own. How those cleanups are handled is to be
 * refactored separately, so these tests only cover what the warning tells about
 * its target.
 *
 * @see <a href="https://github.com/vaadin/flow/issues/20464">#20464</a>
 * @see <a href="https://github.com/vaadin/flow/issues/20961">#20961</a>
 */
class ReturnChannelDiagnosticsTest {

    /**
     * Stands for whatever the application happened to read from the browser,
     * which can be data that doesn't belong in a log file.
     */
    private static final String RETURN_VALUE = "account-FI7654321000000";

    @Tag("div")
    @Route("orders")
    private static class OrdersView extends Component {
    }

    @Tag("my-widget")
    private static class Widget extends Component {
    }

    @Test
    void frameworkTrackedJs_targetDisabledWhenCompleted_warnsAboutTarget() {
        MockUI ui = new MockUI();
        Widget widget = new Widget();
        ui.getElement().appendChild(widget.getElement());

        sendJsQueuedWhileInvisible(ui, widget, "this.doSomething()");
        widget.getElement().setEnabled(false);

        List<String> warnings = handleChannelMessage(ui, widget).warnings();

        assertEquals(1, warnings.size(),
                () -> "A plain executeJs with no return value callback should "
                        + "be enough to get the warning, got: " + warnings);
        assertTrue(warnings.get(0).contains(Widget.class.getName()),
                () -> "The warning should name the component even though the "
                        + "application registered no channel itself: "
                        + warnings.get(0));
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

        List<String> warnings = handleChannelMessage(ui, widget).warnings();

        assertEquals(1, warnings.size(),
                () -> "Dropping a value the application is waiting for should "
                        + "still be warned about, got: " + warnings);
        assertTrue(warnings.get(0).contains("dropped"),
                () -> "The warning should tell that the value is dropped: "
                        + warnings.get(0));
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

        String warning = handleChannelMessage(ui, widget).warnings().get(0);

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
        MockUI ui = new MockUI();
        OrdersView view = new OrdersView();
        Widget widget = new Widget();
        ui.getElement().appendChild(view.getElement());
        view.getElement().appendChild(widget.getElement());

        sendSubscribedJs(ui, widget, "return this.getSomething()", value -> {
        });
        // The widget itself is enabled, it is the view that is disabled
        view.getElement().setEnabled(false);

        String warning = handleChannelMessage(ui, widget).warnings().get(0);

        assertTrue(warning.contains(OrdersView.class.getName()),
                () -> "The warning should name the ancestor that is actually "
                        + "disabled: " + warning);
    }

    @Test
    void nodeWithoutReturnChannels_warningIdentifiesTarget() {
        MockUI ui = new MockUI();
        StateNode nodeWithoutChannels = new StateNode();
        ui.getElement().getNode().getFeature(ElementChildrenList.class).add(0,
                nodeWithoutChannels);

        String warning = handleChannelMessage(ui, nodeWithoutChannels)
                .warnings().get(0);

        assertTrue(warning.contains("no return channels"),
                () -> "Unexpected warning: " + warning);
        assertTrue(warning.contains("node id=" + nodeWithoutChannels.getId()),
                () -> "The warning should identify the node: " + warning);
    }

    @Test
    void alreadyHandledChannel_warningIdentifiesTarget() {
        MockUI ui = new MockUI();
        Widget widget = new Widget();
        ui.getElement().appendChild(widget.getElement());

        sendSubscribedJs(ui, widget, "return this.getSomething()", value -> {
        });

        assertEquals(List.of(), handleChannelMessage(ui, widget).warnings(),
                "Handling the return value while enabled should not warn");

        // The channel is removed once the return value has been handled
        String warning = handleChannelMessage(ui, widget).warnings().get(0);

        assertTrue(warning.contains("not found"),
                () -> "Unexpected warning: " + warning);
        assertTrue(warning.contains("my-widget"),
                () -> "The warning should name the element tag: " + warning);
    }

    @Test
    void disabledTarget_clientValuesLoggedOnlyOnDebugLevel() {
        MockUI ui = new MockUI();
        Widget widget = new Widget();
        ui.getElement().appendChild(widget.getElement());

        sendSubscribedJs(ui, widget, "return this.getSomething()", value -> {
        });
        widget.getElement().setEnabled(false);

        LoggedMessages logged = handleChannelMessage(ui, widget);

        assertFalse(logged.warnings().get(0).contains(RETURN_VALUE),
                () -> "The warning should not contain what the client sent: "
                        + logged.warnings().get(0));
        assertTrue(
                logged.debugMessages().stream()
                        .anyMatch(message -> message.contains(RETURN_VALUE)),
                () -> "The payload should still be available on debug level: "
                        + logged.debugMessages());
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

    private record LoggedMessages(List<String> warnings,
            List<String> debugMessages) {
    }

    /**
     * Simulates the browser calling the first return channel registered for the
     * given component's element and returns what was logged.
     */
    private LoggedMessages handleChannelMessage(MockUI ui,
            Component component) {
        return handleChannelMessage(ui, component.getElement().getNode());
    }

    private LoggedMessages handleChannelMessage(MockUI ui, StateNode node) {
        ArrayNode arguments = JacksonUtils.createArrayNode();
        arguments.add(RETURN_VALUE);

        ObjectNode invocationJson = JacksonUtils.createObjectNode();
        invocationJson.put(JsonConstants.RPC_TYPE,
                JsonConstants.RPC_TYPE_CHANNEL);
        invocationJson.put(JsonConstants.RPC_NODE, node.getId());
        invocationJson.put(JsonConstants.RPC_CHANNEL, 0);
        invocationJson.set(JsonConstants.RPC_CHANNEL_ARGUMENTS, arguments);

        List<String> warnings = new ArrayList<>();
        List<String> debugMessages = new ArrayList<>();
        Logger logger = Mockito.mock(Logger.class,
                invocation -> record(invocation, warnings, debugMessages));

        try (MockedStatic<LoggerFactory> loggerFactory = Mockito
                .mockStatic(LoggerFactory.class)) {
            loggerFactory
                    .when(() -> LoggerFactory
                            .getLogger(ReturnChannelHandler.class.getName()))
                    .thenReturn(logger);

            new ReturnChannelHandler().handle(ui, invocationJson);
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
}
