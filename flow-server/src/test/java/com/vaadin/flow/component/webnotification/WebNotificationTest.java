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
import org.mockito.ArgumentCaptor;
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
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.signals.Signal;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void permissionSignal_withoutSessionLock_wiresThroughAccess() {
        MockUI ui = new MockUI();
        VaadinSession session = ui.getSession();
        when(session.hasLock()).thenReturn(false);

        Signal<WebNotificationPermission> permission = WebNotification
                .permissionSignal(ui);

        ArgumentCaptor<Command> access = ArgumentCaptor.forClass(Command.class);
        verify(session).access(access.capture());
        firePermissionChange(ui, "GRANTED");
        assertEquals(WebNotificationPermission.UNKNOWN, permission.peek(),
                "the UI must not be changed without the session lock");

        when(session.hasLock()).thenReturn(true);
        access.getValue().execute();
        firePermissionChange(ui, "GRANTED");
        assertEquals(WebNotificationPermission.GRANTED, permission.peek());
    }

    @Test
    void onClick_requestPermission_requestsInsideTheClickHandler() {
        UI ui = new MockUI();
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());

        WebNotification.onClick(button).requestPermission();

        assertEquals("window.Vaadin.Flow.webNotification.requestPermission()",
                clickHandler(ui).getBody());
    }

    @Test
    void show_sendsTitleAndOnlyTheOptionsThatAreSet() {
        MockUI ui = new MockUI();
        ui.getInternals().setFullAppId("myapp-123");

        WebNotification.show("Report ready", WebNotificationOptions.builder()
                .body("March").tag("report").build(), ui);

        List<Object> parameters = showInvocation(ui).getParameters();
        assertEquals("Report ready", parameters.get(0));
        assertEquals("{\"body\":\"March\",\"tag\":\"report\"}", JacksonUtils
                .getMapper().valueToTree(parameters.get(1)).toString());
        assertEquals("myapp", parameters.get(4));
    }

    @Test
    void show_clickAndClose_runListenersAndStopTracking() {
        MockUI ui = new MockUI();
        List<String> events = new ArrayList<>();
        WebNotificationHandle handle = WebNotification.show("Hi", ui);
        handle.addClickListener(() -> events.add("click"));
        handle.addCloseListener(() -> events.add("close"));
        handle.addErrorListener(error -> events.add("error"));
        JavaScriptInvocation show = showInvocation(ui);

        report(show, "click");
        report(show, "close");
        report(show, "click");

        assertEquals(List.of("click", "close"), events,
                "a closed notification no longer reports");
        handle.close();
        assertTrue(ui.dumpPendingJsInvocations().isEmpty(),
                "closing a closed notification does nothing");
    }

    @ParameterizedTest
    @CsvSource({ "NOT_ALLOWED,NOT_ALLOWED", "SOMETHING_NEW,UNKNOWN" })
    void show_errorFromClient_runsErrorListenersWithReason(String sent,
            WebNotificationError.Reason expected) {
        MockUI ui = new MockUI();
        List<Object> events = new ArrayList<>();
        WebNotificationHandle handle = WebNotification.show("Hi", ui);
        handle.addErrorListener(events::add);
        handle.addCloseListener(() -> events.add("close"));
        JavaScriptInvocation show = showInvocation(ui);

        report(show, "error", sent, "no permission");
        report(show, "close");

        assertEquals(
                List.of(new WebNotificationError(expected, "no permission")),
                events);
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
    }

    @Test
    void closeByTag_sendsTheTagAndTheApplication() {
        MockUI ui = new MockUI();
        ui.getInternals().setFullAppId("myapp-123");

        WebNotification.closeByTag("inbox", ui);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(1, invocations.size());
        List<Object> parameters = invocations.get(0).getInvocation()
                .getParameters();
        assertEquals("inbox", parameters.get(0));
        assertEquals("myapp", parameters.get(1));
    }

    @Test
    void show_uiDetached_stopsTrackingAndRunsCloseListeners() {
        MockUI ui = new MockUI();
        List<String> events = new ArrayList<>();
        WebNotificationHandle handle = WebNotification.show("Hi", ui);
        handle.addCloseListener(() -> events.add("close"));
        handle.addClickListener(() -> events.add("click"));
        JavaScriptInvocation show = showInvocation(ui);

        // A page reload closes the UI, also when @PreserveOnRefresh moves its
        // content to a new UI, and the new page never reports on the
        // notification
        ui.getInternals().setSession(null);
        report(show, "click");

        assertEquals(List.of("close"), events);
    }

    @Test
    void show_moreThanTheLimit_closesTheOldest() {
        MockUI ui = new MockUI();
        List<String> events = new ArrayList<>();
        WebNotificationHandle oldest = WebNotification.show("First", ui);
        oldest.addCloseListener(() -> events.add("close"));
        oldest.addClickListener(() -> events.add("click"));
        JavaScriptInvocation oldestShow = showInvocation(ui);
        for (int i = 1; i < WebNotificationHandle.MAX_TRACKED_PER_UI; i++) {
            WebNotification.show("Next", ui);
        }
        ui.dumpPendingJsInvocations();

        WebNotification.show("One too many", ui);

        assertEquals(List.of("close"), events);
        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(2, invocations.size());
        assertEquals(oldestShow.getParameters().get(2),
                invocations.get(0).getInvocation().getParameters().get(0),
                "the oldest notification is closed before the next is shown");
        // An event the browser sent before it heard of this is ignored
        report(oldestShow, "click");
        report(oldestShow, "close");
        assertEquals(List.of("close"), events);
        oldest.close();
        assertTrue(ui.dumpPendingJsInvocations().isEmpty(),
                "the browser no longer keeps the notification for closing");
    }

    @Test
    void show_closedHandle_doesNotCountTowardsTheLimit() {
        MockUI ui = new MockUI();
        WebNotification.show("First", ui);
        JavaScriptInvocation firstShow = showInvocation(ui);
        List<String> events = new ArrayList<>();
        WebNotification.show("Second", ui)
                .addCloseListener(() -> events.add("second closed"));
        for (int i = 2; i < WebNotificationHandle.MAX_TRACKED_PER_UI; i++) {
            WebNotification.show("Next", ui);
        }
        report(firstShow, "close");

        WebNotification.show("Fits again", ui);
        assertTrue(events.isEmpty(), "the closed handle freed a slot");

        WebNotification.show("One too many", ui);
        assertEquals(List.of("second closed"), events);
    }

    @Test
    void show_severalNotifications_shareOneChannelAndGetTheirOwnEvents() {
        MockUI ui = new MockUI();
        List<String> events = new ArrayList<>();
        WebNotificationHandle first = WebNotification.show("First", ui);
        first.addClickListener(() -> events.add("first click"));
        first.addCloseListener(() -> events.add("first close"));
        JavaScriptInvocation firstShow = showInvocation(ui);
        WebNotificationHandle second = WebNotification.show("Second", ui);
        second.addClickListener(() -> events.add("second click"));
        second.addCloseListener(() -> events.add("second close"));
        JavaScriptInvocation secondShow = showInvocation(ui);

        report(firstShow, "click");
        report(firstShow, "close");

        assertEquals(List.of("first click", "first close"), events);
        assertEquals(firstShow.getParameters().get(3),
                secondShow.getParameters().get(3));
        // The browser passes the key first, as the report helper does
        assertTrue(
                firstShow.getExpression()
                        .contains("$3($2, type, reason, message)"),
                firstShow.getExpression());
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
    void options_renotifyWithoutTagOrWithSilent_throws() {
        WebNotificationOptions.Builder withoutTag = WebNotificationOptions
                .builder().renotify(true);
        assertThrows(IllegalArgumentException.class, withoutTag::build);
        WebNotificationOptions.Builder silent = WebNotificationOptions.builder()
                .tag("inbox").renotify(true).silent(true);
        assertThrows(IllegalArgumentException.class, silent::build);
    }

    private static JsFunction clickHandler(UI ui) {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        JsFunction install = (JsFunction) ui.getInternals()
                .dumpPendingJavaScriptInvocations().get(0).getInvocation()
                .getParameters().get(2);
        assertTrue(install.getCaptures().contains("click"),
                "click trigger install captures: " + install.getCaptures());
        return (JsFunction) install.getCaptures().get(0);
    }

    private static JavaScriptInvocation showInvocation(MockUI ui) {
        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(1, invocations.size());
        return invocations.get(0).getInvocation();
    }

    /**
     * Reports an event the way the browser does: through the channel of the UI,
     * preceded by the key of the notification.
     */
    private static void report(JavaScriptInvocation show, String... event) {
        ArrayNode args = args(event);
        args.insert(0, (String) show.getParameters().get(2));
        ((ReturnChannelRegistration) show.getParameters().get(3)).invoke(args);
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
