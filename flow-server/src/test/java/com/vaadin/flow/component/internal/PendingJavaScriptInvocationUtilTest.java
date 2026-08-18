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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.internal.PendingJavaScriptInvocationUtil.DEFAULT_WARNING_THRESHOLD;
import static com.vaadin.flow.component.internal.PendingJavaScriptInvocationUtil.buildWarningMessage;
import static com.vaadin.flow.component.internal.PendingJavaScriptInvocationUtil.shouldWarn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingJavaScriptInvocationUtilTest {

    @Tag("div")
    private static class TestComponent extends Component {
    }

    @Test
    void shouldWarn_firstWarningAtThresholdAndThenTenfold() {
        assertFalse(shouldWarn(999, 1000), "below the threshold");
        assertTrue(shouldWarn(1000, 1000), "at the threshold");
        assertFalse(shouldWarn(2000, 1000),
                "twice the threshold should not warn again");
        assertFalse(shouldWarn(9000, 1000));
        assertTrue(shouldWarn(10000, 1000), "tenfold the threshold");
        assertFalse(shouldWarn(20000, 1000));
        assertTrue(shouldWarn(100000, 1000), "hundredfold the threshold");
    }

    @Test
    void shouldWarn_customThreshold_warningsFollowTheThreshold() {
        assertFalse(shouldWarn(200, 300));
        assertTrue(shouldWarn(300, 300));
        assertFalse(shouldWarn(600, 300));
        assertTrue(shouldWarn(3000, 300));
    }

    @Test
    void shouldWarn_zeroOrNegativeThreshold_neverWarns() {
        assertFalse(shouldWarn(1000000, 0), "0 should disable the warning");
        assertFalse(shouldWarn(1000000, -1));
    }

    @Test
    void scheduleInvocation_countedUntilSentToBrowser() {
        StateNode node = new StateNode(ElementData.class);

        PendingJavaScriptInvocation invocation = createInvocation(node);
        assertEquals(1, node.getUndeliveredJavaScriptInvocations(),
                "a scheduled invocation should be counted");

        createInvocation(node);
        assertEquals(2, node.getUndeliveredJavaScriptInvocations());

        invocation.setSentToBrowser();
        assertEquals(1, node.getUndeliveredJavaScriptInvocations(),
                "a sent invocation should no longer be counted");
    }

    @Test
    void cancelInvocation_notCountedAndNotCountedTwice() {
        StateNode node = new StateNode(ElementData.class);
        PendingJavaScriptInvocation invocation = createInvocation(node);

        assertTrue(invocation.cancelExecution());
        assertEquals(0, node.getUndeliveredJavaScriptInvocations(),
                "a canceled invocation should no longer be counted");

        assertFalse(invocation.cancelExecution(),
                "canceling twice should have no effect");
        assertEquals(0, node.getUndeliveredJavaScriptInvocations());
    }

    @Test
    void warningMessage_containsCauseAndAdvice() {
        StateNode node = new StateNode(ElementData.class);
        String message = buildWarningMessage(createInvocation(node), 1000,
                DEFAULT_WARNING_THRESHOLD);

        assertTrue(message.startsWith("1000 JavaScript invocations"), message);
        assertTrue(message.contains("'return $0;'"),
                "the scheduled expression should be included: " + message);
        assertTrue(message.contains("OutOfMemoryError"), message);
        assertTrue(message.contains("cancelExecution()"),
                "the message should explain how to keep only the last value: "
                        + message);
        assertTrue(message.contains("element.setProperty(...)"), message);
        assertTrue(
                message.contains(
                        InitParameters.PENDING_JAVASCRIPT_INVOCATIONS_WARNING_THRESHOLD),
                "the message should tell how to configure the threshold: "
                        + message);
    }

    @Test
    void warningMessage_detachedOwner_explainsThatOwnerIsDetached() {
        StateNode node = new StateNode(ElementData.class);
        String message = buildWarningMessage(createInvocation(node), 1000,
                DEFAULT_WARNING_THRESHOLD);

        assertTrue(message.contains("not attached to a UI"), message);
    }

    @Test
    void warningMessage_invisibleOwner_explainsThatOwnerIsInvisible() {
        MockUI ui = new MockUI();
        TestComponent component = new TestComponent();
        ui.add(component);
        component.setVisible(false);

        String message = buildWarningMessage(
                createInvocation(component.getElement().getNode()), 1000,
                DEFAULT_WARNING_THRESHOLD);

        assertTrue(message.contains("invisible"), message);
    }

    @Test
    void warningMessage_noPushConnection_explainsMissingConnection() {
        MockUI ui = new MockUI();
        TestComponent component = new TestComponent();
        ui.add(component);

        StateNode node = component.getElement().getNode();
        String message = buildWarningMessage(createInvocation(node), 1000,
                DEFAULT_WARNING_THRESHOLD);

        assertTrue(message.contains("no open push connection"), message);
        assertTrue(
                message.contains(TestComponent.class.getName())
                        && message.contains("state node " + node.getId()),
                "the owner component should be identified: " + message);
    }

    @Test
    void warningMessage_longExpression_truncatedToSingleLine() {
        StateNode node = new StateNode(ElementData.class);
        String expression = "return (async function() {\n    this.invalid = $0;\n"
                + "x".repeat(500) + "\n}).apply($1)";

        String message = buildWarningMessage(
                new PendingJavaScriptInvocation(node,
                        new JavaScriptInvocation(expression)),
                1000, DEFAULT_WARNING_THRESHOLD);

        assertFalse(message.contains("\n"),
                "the expression should be logged on a single line: " + message);
        assertTrue(message.contains("..."),
                "a long expression should be truncated: " + message);
        assertTrue(message.length() < 1500,
                "the message should stay readable: " + message);
    }

    @Test
    void executeJsOnDetachedElement_countedOnOwnerNode() {
        Element element = new Element("div");

        element.executeJs("this.foo = $0", "bar");
        element.executeJs("this.foo = $0", "baz");

        assertEquals(2, element.getNode().getUndeliveredJavaScriptInvocations(),
                "invocations scheduled for a detached element should be counted");
    }

    @Test
    void executeJsSentToBrowser_countIsReset() {
        MockUI ui = new MockUI();
        TestComponent component = new TestComponent();
        ui.add(component);
        StateNode node = component.getElement().getNode();

        component.getElement().executeJs("this.foo = $0", "bar");
        assertEquals(1, node.getUndeliveredJavaScriptInvocations());

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().dumpPendingJavaScriptInvocations();

        assertEquals(0, node.getUndeliveredJavaScriptInvocations(),
                "invocations sent to the browser should not be counted");
    }

    private static PendingJavaScriptInvocation createInvocation(
            StateNode node) {
        return new PendingJavaScriptInvocation(node,
                new JavaScriptInvocation("return $0;", "foo"));
    }
}
