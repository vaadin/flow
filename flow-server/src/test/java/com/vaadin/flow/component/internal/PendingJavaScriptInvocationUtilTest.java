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
package com.vaadin.flow.component.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.internal.PendingJavaScriptInvocationUtil.WARNING_THRESHOLD;
import static com.vaadin.flow.component.internal.PendingJavaScriptInvocationUtil.buildWarningMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingJavaScriptInvocationUtilTest {

    @Tag("div")
    private static class TestComponent extends Component {
    }

    @Test
    void invocationsBelowTheThreshold_noWarningLogged() {
        MockUI ui = new MockUI();
        UIInternals internals = ui.getInternals();
        StateNode node = attachedNode(ui);

        internals.addUndeliveredJsInvocations(WARNING_THRESHOLD - 2);

        scheduleInvocation(node);

        assertEquals(WARNING_THRESHOLD - 1,
                internals.addUndeliveredJsInvocations(0));
        assertTrue(internals.markUndeliveredJsInvocationsWarningLogged(),
                "no warning should have been logged below the threshold");
    }

    @Test
    void invocationsReachingTheThreshold_warningLogged() {
        MockUI ui = new MockUI();
        UIInternals internals = ui.getInternals();
        StateNode node = attachedNode(ui);

        internals.addUndeliveredJsInvocations(WARNING_THRESHOLD - 1);

        scheduleInvocation(node);

        assertEquals(WARNING_THRESHOLD,
                internals.addUndeliveredJsInvocations(0));
        assertFalse(internals.markUndeliveredJsInvocationsWarningLogged(),
                "the warning should have been logged when reaching the threshold");
    }

    @Test
    void scheduleInvocation_countedInUIUntilSentToBrowser() {
        MockUI ui = new MockUI();
        UIInternals internals = ui.getInternals();
        StateNode node = attachedNode(ui);

        PendingJavaScriptInvocation invocation = scheduleInvocation(node);
        assertEquals(1, internals.addUndeliveredJsInvocations(0),
                "a scheduled invocation should be counted");

        invocation.setSentToBrowser();
        assertEquals(0, internals.addUndeliveredJsInvocations(0),
                "a sent invocation should no longer be counted");

        assertFalse(invocation.cancelExecution(),
                "a sent invocation cannot be canceled");
        assertEquals(0, internals.addUndeliveredJsInvocations(0),
                "canceling a sent invocation should not change the count");
    }

    @Test
    void cancelInvocation_notCountedAndNotCountedTwice() {
        MockUI ui = new MockUI();
        UIInternals internals = ui.getInternals();
        PendingJavaScriptInvocation invocation = scheduleInvocation(
                attachedNode(ui));

        assertTrue(invocation.cancelExecution());
        assertEquals(0, internals.addUndeliveredJsInvocations(0),
                "a canceled invocation should no longer be counted");

        assertFalse(invocation.cancelExecution(),
                "canceling twice should have no effect");
        assertEquals(0, internals.addUndeliveredJsInvocations(0));
    }

    @Test
    void scheduleInvocationForOwnerOutsideAnyUI_notCounted() {
        MockUI ui = new MockUI();
        StateNode neverAttachedNode = new StateNode(ElementData.class);

        PendingJavaScriptInvocation invocation = createInvocation(
                neverAttachedNode);

        assertEquals(0, ui.getInternals().addUndeliveredJsInvocations(0),
                "an invocation for an owner that does not belong to a UI should not be counted");
        assertTrue(invocation.cancelExecution(),
                "an uncounted invocation should still be cancelable");
    }

    @Test
    void executeJsForDetachedOwner_countedWhenTheOwnerIsAttachedAgain() {
        MockUI ui = new MockUI();
        TestComponent component = new TestComponent();
        ui.add(component);
        ui.remove(component);
        // A detached node keeps the state tree it was attached to
        CurrentInstance.clearAll();

        component.getElement().executeJs("this.foo = $0", "bar");

        assertEquals(0, count(ui),
                "an invocation for a detached owner should not be counted before it is on its way to a client");

        ui.add(component);

        assertEquals(1, count(ui),
                "attaching the owner should count the invocation waiting for it");
    }

    @Test
    void executeJsForOwnerOutsideAnyUI_countedWhenTheOwnerIsAttached() {
        MockUI ui = new MockUI();
        TestComponent component = new TestComponent();

        component.getElement().executeJs("this.foo = $0", "bar");
        assertEquals(0, ui.getInternals().addUndeliveredJsInvocations(0),
                "an invocation for an owner that does not belong to a UI should not be counted");

        ui.add(component);

        assertEquals(1, ui.getInternals().addUndeliveredJsInvocations(0),
                "attaching the owner should count the invocation waiting for it");
    }

    @Test
    void pageExecuteJs_countedUntilSentToBrowser() {
        MockUI ui = new MockUI();

        ui.getPage().executeJs("this.foo = $0", "bar");

        assertEquals(1, count(ui),
                "an invocation queued for the UI should be counted");

        ui.dumpPendingJsInvocations();

        assertEquals(0, count(ui),
                "an invocation sent to the browser should not be counted");
    }

    @Test
    void executeJsForOwnerOfClosedUI_countedWhenAttachedToAnotherUI() {
        MockUI closedUI = new MockUI();
        TestComponent component = new TestComponent();
        closedUI.add(component);
        closedUI.remove(component);
        // Closing a UI clears its session, while its detached nodes keep
        // referencing its state tree
        closedUI.getInternals().setSession(null);

        component.getElement().executeJs("this.foo = $0", "bar");

        MockUI ui = new MockUI();
        // Reusing a component in another UI requires releasing it from the
        // state tree of the previous one, the way preserve on refresh does
        component.getElement().removeFromTree(false);
        ui.add(component);

        assertEquals(1, count(ui),
                "attaching the owner to another UI should count the invocation waiting for it");
    }

    @Test
    void warningMessage_containsCauseAndAdvice() {
        StateNode node = new StateNode(ElementData.class);
        String message = buildWarningMessage(createInvocation(node), 1000);

        assertTrue(
                message.startsWith(
                        "1000 JavaScript invocations scheduled for this UI"),
                message);
        assertTrue(message.contains("'return $0;'"),
                "the scheduled expression should be included: " + message);
        assertTrue(message.contains("OutOfMemoryError"), message);
        assertTrue(message.contains("cancelExecution()"),
                "the message should explain how to keep only the last value: "
                        + message);
        assertTrue(message.contains("element.setProperty(...)"), message);
        assertTrue(message.contains("UI.getLastUpdateSentTimestamp()"),
                "the message should point to the API for detecting that updates are not delivered: "
                        + message);
        assertTrue(message.contains("logged once per UI"),
                "the message should tell that it is not repeated: " + message);
    }

    @Test
    void warningMessage_detachedOwner_explainsThatOwnerIsDetached() {
        StateNode node = new StateNode(ElementData.class);
        String message = buildWarningMessage(createInvocation(node), 1000);

        assertTrue(message.contains("not attached to a UI"), message);
    }

    @Test
    void warningMessage_invisibleOwner_explainsThatOwnerIsInvisible() {
        MockUI ui = new MockUI();
        TestComponent component = new TestComponent();
        ui.add(component);
        component.setVisible(false);

        String message = buildWarningMessage(
                createInvocation(component.getElement().getNode()), 1000);

        assertTrue(message.contains("invisible"), message);
    }

    @Test
    void warningMessage_noPushConnection_explainsMissingConnection() {
        MockUI ui = new MockUI();
        TestComponent component = new TestComponent();
        ui.add(component);

        String message = buildWarningMessage(
                createInvocation(component.getElement().getNode()), 1000);

        assertTrue(message.contains("no open push connection"), message);
    }

    @Test
    void warningMessage_openPushConnection_noMissingConnectionClaimed() {
        MockUI ui = new MockUI();
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        PushConnection pushConnection = Mockito.mock(PushConnection.class);
        Mockito.when(pushConnection.isConnected()).thenReturn(true);
        ui.getInternals().setPushConnection(pushConnection);
        TestComponent component = new TestComponent();
        ui.add(component);

        String message = buildWarningMessage(
                createInvocation(component.getElement().getNode()), 1000);

        assertFalse(message.contains("no open push connection"),
                "the message should not claim that the connection is missing: "
                        + message);
        assertTrue(message.contains("has an open push connection"), message);
    }

    @Test
    void warningMessage_identifiesTheOwnerComponent() {
        MockUI ui = new MockUI();
        TestComponent component = new TestComponent();
        ui.add(component);

        StateNode node = component.getElement().getNode();
        String message = buildWarningMessage(createInvocation(node), 1000);

        assertTrue(
                message.contains(TestComponent.class.getName())
                        && message.contains("node id=" + node.getId()),
                "the owner component should be identified: " + message);
    }

    @Test
    void warningMessage_longExpression_truncatedToSingleLine() {
        StateNode node = new StateNode(ElementData.class);
        String expression = "return (async function() {\n    this.invalid = $0;\n"
                + "x".repeat(500) + "\n}).apply($1)";

        String message = buildWarningMessage(new PendingJavaScriptInvocation(
                node, new JavaScriptInvocation(expression)), 1000);

        assertFalse(message.contains("\n"),
                "the expression should be logged on a single line: " + message);
        assertTrue(message.contains("..."),
                "a long expression should be truncated: " + message);
        assertTrue(message.length() < 1500,
                "the message should stay readable: " + message);
    }

    @Test
    void executeJsSentToBrowser_countIsReset() {
        MockUI ui = new MockUI();
        TestComponent component = new TestComponent();
        ui.add(component);

        component.getElement().executeJs("this.foo = $0", "bar");
        assertEquals(1, ui.getInternals().addUndeliveredJsInvocations(0));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().dumpPendingJavaScriptInvocations();

        assertEquals(0, ui.getInternals().addUndeliveredJsInvocations(0),
                "invocations sent to the browser should not be counted");
    }

    @Test
    void executeJsThenOwnerDetached_stopsBeingCounted() {
        MockUI ui = new MockUI();
        TestComponent component = new TestComponent();
        ui.add(component);

        component.getElement().executeJs("this.foo = $0", "bar");
        assertEquals(1, count(ui));

        ui.remove(component);

        assertEquals(0, count(ui),
                "an invocation waiting for a response for a detached owner should not be counted");

        ui.dumpPendingJsInvocations();

        assertEquals(0, count(ui),
                "writing a response should not bring the invocation back");
    }

    @Test
    void retainedInvocationOwnerDetached_stopsBeingCounted() {
        MockUI ui = new MockUI();
        TestComponent component = new TestComponent();
        ui.add(component);
        component.setVisible(false);

        component.getElement().executeJs("this.foo = $0", "bar");
        // An invisible owner keeps the invocation in the queue of the UI
        assertTrue(ui.dumpPendingJsInvocations().isEmpty());
        assertEquals(1, count(ui));

        ui.remove(component);

        assertEquals(0, count(ui),
                "an invocation discarded from the queue should not be counted");
    }

    @Test
    void resynchronize_queuedInvocationsStopBeingCounted() {
        MockUI ui = new MockUI();
        TestComponent component = new TestComponent();
        ui.add(component);
        component.setVisible(false);

        component.getElement().executeJs("this.foo = $0", "bar");
        assertTrue(ui.dumpPendingJsInvocations().isEmpty());
        assertEquals(1, count(ui));

        ui.getInternals().getStateTree().prepareForResync();

        assertEquals(0, count(ui),
                "invocations discarded by a resynchronization should not be counted");
    }

    @Test
    void detachedOwnerAttachedAgain_countedAgainAndStillDelivered() {
        MockUI ui = new MockUI();
        TestComponent component = new TestComponent();
        ui.add(component);
        component.getElement().executeJs("this.foo = $0", "bar");
        ui.remove(component);
        assertEquals(0, count(ui));

        component.setVisible(false);
        ui.add(component);

        assertEquals(1, count(ui),
                "an invocation waiting for a response again should be counted again");

        assertTrue(ui.dumpPendingJsInvocations().isEmpty(),
                "an invisible owner should still retain the invocation");
        assertEquals(1, count(ui));

        component.setVisible(true);

        assertEquals(1, ui.dumpPendingJsInvocations().size(),
                "the invocation should still be delivered once the owner is visible");
        assertEquals(0, count(ui));
    }

    @Test
    void serializeOwnerOfScheduledInvocation_uiNotPartOfTheGraph()
            throws Exception {
        // There is a current UI, but the owner does not belong to it: it
        // holds on to the invocation until it is attached, and does not start
        // referencing a UI because of it
        new MockUI();
        TestComponent component = new TestComponent();
        component.getElement().executeJs("this.foo = $0", "bar");

        List<Object> serialized = new ArrayList<>();
        ObjectOutputStream stream = new ObjectOutputStream(
                new ByteArrayOutputStream()) {
            {
                enableReplaceObject(true);
            }

            @Override
            protected Object replaceObject(Object object) {
                serialized.add(object);
                return object;
            }
        };

        stream.writeObject(component);

        assertTrue(
                serialized.stream().anyMatch(
                        PendingJavaScriptInvocation.class::isInstance),
                "the scheduled invocation should be part of the serialized graph");
        assertTrue(serialized.stream().noneMatch(UIInternals.class::isInstance),
                "serializing the owner of a scheduled invocation should not pull in the current UI");
    }

    @Test
    void serializeUIWithCountedInvocation_countedUntilSentByTheRestoredUI()
            throws Exception {
        MockVaadinServletService service = new MockVaadinServletService();
        ApplicationConfiguration configuration = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        service.getContext().setAttribute(ApplicationConfiguration.class,
                configuration);
        MockUI ui = new MockUI(new AlwaysLockedVaadinSession(service));

        TestComponent component = new TestComponent();
        ui.add(component);
        component.getElement().executeJs("this.foo = $0", "bar");
        assertEquals(1, ui.getInternals().addUndeliveredJsInvocations(0));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new ObjectOutputStream(bytes).writeObject(ui);
        UI restored = (UI) new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray())).readObject();
        // The lock of a restored session is a fresh one that nobody holds
        restored.getSession().lock();

        assertEquals(1, restored.getInternals().addUndeliveredJsInvocations(0),
                "the count should be restored together with the invocations that make it up");

        restored.getInternals().getStateTree()
                .runExecutionsBeforeClientResponse();
        restored.getInternals().dumpPendingJavaScriptInvocations();

        assertEquals(0, restored.getInternals().addUndeliveredJsInvocations(0),
                "an invocation restored with its UI should stop being counted once it is sent");
    }

    private static int count(MockUI ui) {
        return ui.getInternals().addUndeliveredJsInvocations(0);
    }

    private static StateNode attachedNode(MockUI ui) {
        TestComponent component = new TestComponent();
        ui.add(component);
        return component.getElement().getNode();
    }

    private static PendingJavaScriptInvocation createInvocation(
            StateNode node) {
        return new PendingJavaScriptInvocation(node,
                new JavaScriptInvocation("return $0;", "foo"));
    }

    /**
     * Creates an invocation for the given attached node and counts it the way
     * the framework does once the owner of an invocation is attached.
     */
    private static PendingJavaScriptInvocation scheduleInvocation(
            StateNode node) {
        PendingJavaScriptInvocation invocation = createInvocation(node);
        invocation.countWhenAttached();
        return invocation;
    }
}
