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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.shared.communication.PushMode;
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

        createInvocation(node);

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

        createInvocation(node);

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

        PendingJavaScriptInvocation invocation = createInvocation(node);
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
        PendingJavaScriptInvocation invocation = createInvocation(
                attachedNode(ui));

        assertTrue(invocation.cancelExecution());
        assertEquals(0, internals.addUndeliveredJsInvocations(0),
                "a canceled invocation should no longer be counted");

        assertFalse(invocation.cancelExecution(),
                "canceling twice should have no effect");
        assertEquals(0, internals.addUndeliveredJsInvocations(0));
    }

    @Test
    void scheduleInvocationForDetachedOwner_countedInCurrentUI() {
        MockUI ui = new MockUI();
        StateNode detachedNode = new StateNode(ElementData.class);

        createInvocation(detachedNode);

        assertEquals(1, ui.getInternals().addUndeliveredJsInvocations(0),
                "an invocation for a detached owner should be counted in the UI that scheduled it");
    }

    @Test
    void scheduleInvocationWithoutAnyUI_notCounted() {
        MockUI ui = new MockUI();
        StateNode detachedNode = new StateNode(ElementData.class);
        CurrentInstance.clearAll();

        PendingJavaScriptInvocation invocation = createInvocation(detachedNode);

        assertEquals(0, ui.getInternals().addUndeliveredJsInvocations(0),
                "an invocation scheduled without any UI should not be counted");
        assertTrue(invocation.cancelExecution(),
                "an uncounted invocation should still be cancelable");
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
}
