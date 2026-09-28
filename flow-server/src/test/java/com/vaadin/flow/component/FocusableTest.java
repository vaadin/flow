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
package com.vaadin.flow.component;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.FocusOption.FocusVisible;
import com.vaadin.flow.component.FocusOption.PreventScroll;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsCall;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FocusableTest {
    @Tag("div")
    private static class FocusableTestComponent extends Component
            implements Focusable {

    }

    private final MockUI ui = new MockUI();
    private final FocusableTestComponent component = new FocusableTestComponent();

    @Test
    void focusUnattached_nothingScheduled() {
        component.focus();

        assertPendingInvocationCount(
                "Nothing should be scheduled when component is not attached",
                0);
    }

    @Test
    void focusBeforeAttach_executionScheduled() {
        component.focus();
        ui.add(component);

        assertPendingInvocationCount(
                "An focus() inovocation should be pending for the attached component",
                1);
    }

    @Test
    void focusAfterAttach_executionScheduled() {
        ui.add(component);
        component.focus();

        assertPendingInvocationCount(
                "An focus() inovocation should be pending for the attached component",
                1);
    }

    @Test
    void detachAfterFocus_nothingScheduled() {
        ui.add(component);
        component.focus();
        ui.remove(component);

        assertPendingInvocationCount(
                "Nothing should be scheduled when component is not attached",
                0);
    }

    private void assertPendingInvocationCount(String message, int expected) {
        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(expected, invocations.size(), message);
    }

    @Test
    void focus_withFocusVisible_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.VISIBLE);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        assertTrue(expression.contains("setTimeout"),
                "Should contain setTimeout wrapper");
        assertTrue(expression.contains(".focus($0)"),
                "Should contain focus call with parameter");

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is the options object
        assertTrue(params.size() >= 1, "Should have at least 1 parameter");
        String paramJson = params.get(0).toString();
        assertTrue(paramJson.contains("\"focusVisible\":true"),
                "Should set focusVisible to true");
        assertFalse(paramJson.contains("preventScroll"),
                "Should not contain preventScroll");
    }

    @Test
    void focus_withFocusNotVisible_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.NOT_VISIBLE);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        assertTrue(expression.contains("setTimeout"),
                "Should contain setTimeout wrapper");
        assertTrue(expression.contains(".focus($0)"),
                "Should contain focus call with parameter");

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is the options object
        assertTrue(params.size() >= 1, "Should have at least 1 parameter");
        String paramJson = params.get(0).toString();
        assertTrue(paramJson.contains("\"focusVisible\":false"),
                "Should set focusVisible to false");
    }

    @Test
    void focus_withPreventScrollEnabled_generatesCorrectJS() {
        ui.add(component);
        component.focus(PreventScroll.ENABLED);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        assertTrue(expression.contains("setTimeout"),
                "Should contain setTimeout wrapper");
        assertTrue(expression.contains(".focus($0)"),
                "Should contain focus call with parameter");

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is the options object
        assertTrue(params.size() >= 1, "Should have at least 1 parameter");
        String paramJson = params.get(0).toString();
        assertTrue(paramJson.contains("\"preventScroll\":true"),
                "Should set preventScroll to true");
        assertFalse(paramJson.contains("focusVisible"),
                "Should not contain focusVisible");
    }

    @Test
    void focus_withPreventScrollDisabled_generatesCorrectJS() {
        ui.add(component);
        component.focus(PreventScroll.DISABLED);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        assertTrue(expression.contains("setTimeout"),
                "Should contain setTimeout wrapper");
        assertTrue(expression.contains(".focus($0)"),
                "Should contain focus call with parameter");

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is the options object
        assertTrue(params.size() >= 1, "Should have at least 1 parameter");
        String paramJson = params.get(0).toString();
        assertTrue(paramJson.contains("\"preventScroll\":false"),
                "Should set preventScroll to false");
    }

    @Test
    void focus_withBothOptions_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.VISIBLE, PreventScroll.ENABLED);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        assertTrue(expression.contains("setTimeout"),
                "Should contain setTimeout wrapper");
        assertTrue(expression.contains(".focus($0)"),
                "Should contain focus call with parameter");

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is the options object
        assertTrue(params.size() >= 1, "Should have at least 1 parameter");
        String paramJson = params.get(0).toString();
        assertTrue(paramJson.contains("\"preventScroll\":true"),
                "Should set preventScroll to true");
        assertTrue(paramJson.contains("\"focusVisible\":true"),
                "Should set focusVisible to true");
    }

    @Test
    void focus_withBothOptionsFalse_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.NOT_VISIBLE, PreventScroll.DISABLED);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        assertTrue(expression.contains("setTimeout"),
                "Should contain setTimeout wrapper");
        assertTrue(expression.contains(".focus($0)"),
                "Should contain focus call with parameter");

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is the options object
        assertTrue(params.size() >= 1, "Should have at least 1 parameter");
        String paramJson = params.get(0).toString();
        assertTrue(paramJson.contains("\"preventScroll\":false"),
                "Should set preventScroll to false");
        assertTrue(paramJson.contains("\"focusVisible\":false"),
                "Should set focusVisible to false");
    }

    @Test
    void focus_withoutOptions_generatesCorrectJS() {
        ui.add(component);
        component.focus();

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(1, invocations.size());

        String expression = invocations.getFirst().getInvocation()
                .getExpression();
        assertTrue(expression.contains("setTimeout"),
                "Should contain setTimeout wrapper");
        assertTrue(expression.contains(".focus($0)"),
                "Should contain focus call with the options parameter");

        // Check the parameters: the options are null, which the browser makes
        // the same as calling focus() with none
        List<Object> params = invocations.getFirst().getInvocation()
                .getParameters();
        assertEquals(2, params.size(),
                "Should have the options and the element the function runs on");
        assertNull(params.getFirst(), "Should pass no options");
    }

    @Test
    void pendingInvocations_runOnAnImplementationOfTheDefinition_plainJavaScriptLeftIntact() {
        ui.add(component);
        component.focus(PreventScroll.ENABLED);
        component.getElement().executeJs("this.scrollTop = 0");
        component.blur();

        // What a driver of the client side that cannot run JavaScript does:
        // take the queue once, in order, and let Java dispatch the calls it
        // recognizes onto its own implementation of the JavaScript definition
        List<String> log = new ArrayList<>();
        List<String> unhandledJs = new ArrayList<>();
        for (PendingJavaScriptInvocation pending : ui
                .dumpPendingJsInvocations()) {
            JsCall call = pending.getInvocation().getJsCall();
            if (call != null
                    && call.definitionType() == Focusable.FocusJs.class) {
                call.invokeOn(new FocusSimulation(
                        Element.get(pending.getOwner()), log));
            } else {
                log.add("unhandled");
                unhandledJs.add(pending.getInvocation().getExpression());
            }
        }

        assertEquals(
                List.of("focus div {\"preventScroll\":true}", "unhandled",
                        "blur div"),
                log,
                "calls should be dispatched onto the implementation, in order");
        assertEquals(1, unhandledJs.size(),
                "the application JavaScript should be left for the driver to report");
        assertTrue(unhandledJs.get(0).contains("this.scrollTop = 0"),
                "the unhandled invocation should be the application JavaScript");
    }

    /**
     * What a browserless driver would register for {@link Focusable.FocusJs}:
     * the server-side effect of the operations, with no JavaScript involved.
     */
    private record FocusSimulation(Element target,
            List<String> log) implements Focusable.FocusJs {

        @Override
        public void focus(ObjectNode options) {
            log.add("focus " + target.getTag() + " " + options);
        }

        @Override
        public void blur() {
            log.add("blur " + target.getTag());
        }
    }
}
