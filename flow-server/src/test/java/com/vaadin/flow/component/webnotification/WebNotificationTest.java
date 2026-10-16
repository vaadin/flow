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
package com.vaadin.flow.component.webnotification;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebNotificationTest {

    @Tag("test-button")
    static final class TestButton extends Component
            implements ClickNotifier<TestButton> {
    }

    @Test
    void permissionSignal_followsChangesAndBootstrapValue() {
        MockUI ui = new MockUI();
        Signal<WebNotificationPermission> permission = WebNotification
                .permissionSignal(ui);
        assertEquals(WebNotificationPermission.UNKNOWN, permission.peek());

        // Reading the signal is enough to receive changes, also for a UI
        // whose bootstrap did not report a permission
        firePermissionChange(ui, "PROMPT");
        assertEquals(WebNotificationPermission.PROMPT, permission.peek());

        ui.getInternals().setWebNotificationPermissionFromClient("GRANTED");
        assertEquals(WebNotificationPermission.GRANTED, permission.peek());

        firePermissionChange(ui, "SOMETHING_NEW");
        assertEquals(WebNotificationPermission.GRANTED, permission.peek(),
                "an unknown value from a newer client keeps the permission");
    }

    @Test
    void onClick_requestPermission_requestsInsideTheClickHandler() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        WebNotification.onClick(button).requestPermission();

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        JsFunction install = (JsFunction) ui.getInternals()
                .dumpPendingJavaScriptInvocations().get(0).getInvocation()
                .getParameters().get(2);
        assertTrue(install.getCaptures().contains("click"),
                "click trigger install captures: " + install.getCaptures());
        assertEquals("window.Vaadin.Flow.webNotification.requestPermission()",
                ((JsFunction) install.getCaptures().get(0)).getBody());
    }

    @Test
    void show_sendsTitleAndOnlyTheOptionsThatAreSet() {
        MockUI ui = new MockUI();

        WebNotification.show("Report ready", WebNotificationOptions.builder()
                .body("March").tag("report").build(), ui);

        List<Object> parameters = showInvocation(ui).getParameters();
        assertEquals("Report ready", parameters.get(0));
        assertEquals("{\"body\":\"March\",\"tag\":\"report\"}", JacksonUtils
                .getMapper().valueToTree(parameters.get(1)).toString());
    }

    @Test
    void show_clickAndClose_runListenersAndDisconnect() {
        MockUI ui = new MockUI();
        List<String> events = new ArrayList<>();
        WebNotificationHandle handle = WebNotification.show("Hi", ui);
        handle.addClickListener(() -> events.add("click"));
        handle.addCloseListener(() -> events.add("close"));
        handle.addErrorListener(error -> events.add("error"));
        ReturnChannelRegistration channel = channel(showInvocation(ui));

        channel.invoke(args("click"));
        channel.invoke(args("close"));

        assertEquals(List.of("click", "close"), events);
        assertNull(
                ui.getElement().getNode().getFeature(ReturnChannelMap.class)
                        .get(channel.getChannelId()),
                "a closed notification no longer listens to the browser");
        handle.close();
        assertTrue(ui.dumpPendingJsInvocations().isEmpty(),
                "closing a closed notification does nothing");
    }

    @ParameterizedTest
    @CsvSource({ "NOT_ALLOWED,NOT_ALLOWED", "SOMETHING_NEW,UNKNOWN" })
    void show_errorFromClient_runsErrorListenersWithReason(String sent,
            WebNotificationError.Reason expected) {
        MockUI ui = new MockUI();
        List<WebNotificationError> errors = new ArrayList<>();
        WebNotificationHandle handle = WebNotification.show("Hi", ui);
        handle.addErrorListener(errors::add);
        ReturnChannelRegistration channel = channel(showInvocation(ui));

        channel.invoke(args("error", sent, "no permission"));

        assertEquals(
                List.of(new WebNotificationError(expected, "no permission")),
                errors);
        assertFalse(ui.getElement().getNode().getFeature(ReturnChannelMap.class)
                .hasChannels());
    }

    @Test
    void show_clientBridgeFails_errorListenerGetsUnknown() {
        MockUI ui = new MockUI();
        List<WebNotificationError> errors = new ArrayList<>();
        WebNotification.show("Hi", ui).addErrorListener(errors::add);

        // The browser could not run the show call at all, for example because
        // the client-side bridge is missing
        ui.dumpPendingJsInvocations().get(0)
                .completeExceptionally(JacksonUtils.createNode("boom"));

        assertEquals(1, errors.size());
        assertEquals(WebNotificationError.Reason.UNKNOWN,
                errors.get(0).reason());
        assertTrue(errors.get(0).message().contains("boom"),
                "the failure should be passed on: " + errors.get(0));
        assertFalse(ui.getElement().getNode().getFeature(ReturnChannelMap.class)
                .hasChannels());
    }

    @Test
    void closeByTag_sendsTheTag() {
        MockUI ui = new MockUI();

        WebNotification.closeByTag("inbox", ui);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(1, invocations.size());
        assertEquals("inbox",
                invocations.get(0).getInvocation().getParameters().get(0));
    }

    @Test
    void close_sendsTheKeyTheNotificationWasShownWith() {
        MockUI ui = new MockUI();
        WebNotificationHandle handle = WebNotification.show("Hi", ui);
        Object key = showInvocation(ui).getParameters().get(2);

        handle.close();

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(1, invocations.size());
        assertEquals(key,
                invocations.get(0).getInvocation().getParameters().get(0));
    }

    @Test
    void options_renotifyWithoutTag_throws() {
        WebNotificationOptions.Builder builder = WebNotificationOptions
                .builder().renotify(true);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    private static JavaScriptInvocation showInvocation(MockUI ui) {
        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(1, invocations.size());
        return invocations.get(0).getInvocation();
    }

    private static ReturnChannelRegistration channel(
            JavaScriptInvocation invocation) {
        return (ReturnChannelRegistration) invocation.getParameters().get(3);
    }

    private static ArrayNode args(String... values) {
        ArrayNode args = JacksonUtils.createArrayNode();
        for (String value : values) {
            args.add(value);
        }
        return args;
    }

    private static void firePermissionChange(MockUI ui, String permission) {
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("event.detail", permission);
        ui.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(ui.getElement(),
                        "vaadin-web-notification-permission-change",
                        eventData));
    }
}
