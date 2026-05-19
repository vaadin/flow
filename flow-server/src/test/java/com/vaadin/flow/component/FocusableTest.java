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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.FocusOption.FocusVisible;
import com.vaadin.flow.component.FocusOption.PreventScroll;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

    /**
     * Returns the JsFunction passed as the first parameter of the single
     * pending invocation, after verifying that the outer expression wraps it in
     * a setTimeout call.
     */
    private JsFunction singleFocusJsFunction() {
        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertEquals(1, invocations.size());
        String expression = invocations.get(0).getInvocation().getExpression();
        assertTrue(expression.contains("setTimeout($0, 0)"),
                "Should contain setTimeout wrapper");
        Object first = invocations.get(0).getInvocation().getParameters()
                .get(0);
        return assertInstanceOf(JsFunction.class, first,
                "First parameter should be a JsFunction");
    }

    @Test
    void focus_withFocusVisible_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.VISIBLE);

        JsFunction fn = singleFocusJsFunction();
        assertTrue(fn.getBody().contains(".focus($1)"),
                "Body should contain focus call with parameter");
        String optionsJson = fn.getCaptures().get(1).toString();
        assertTrue(optionsJson.contains("\"focusVisible\":true"),
                "Should set focusVisible to true");
        assertFalse(optionsJson.contains("preventScroll"),
                "Should not contain preventScroll");
    }

    @Test
    void focus_withFocusNotVisible_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.NOT_VISIBLE);

        JsFunction fn = singleFocusJsFunction();
        assertTrue(fn.getBody().contains(".focus($1)"),
                "Body should contain focus call with parameter");
        String optionsJson = fn.getCaptures().get(1).toString();
        assertTrue(optionsJson.contains("\"focusVisible\":false"),
                "Should set focusVisible to false");
    }

    @Test
    void focus_withPreventScrollEnabled_generatesCorrectJS() {
        ui.add(component);
        component.focus(PreventScroll.ENABLED);

        JsFunction fn = singleFocusJsFunction();
        assertTrue(fn.getBody().contains(".focus($1)"),
                "Body should contain focus call with parameter");
        String optionsJson = fn.getCaptures().get(1).toString();
        assertTrue(optionsJson.contains("\"preventScroll\":true"),
                "Should set preventScroll to true");
        assertFalse(optionsJson.contains("focusVisible"),
                "Should not contain focusVisible");
    }

    @Test
    void focus_withPreventScrollDisabled_generatesCorrectJS() {
        ui.add(component);
        component.focus(PreventScroll.DISABLED);

        JsFunction fn = singleFocusJsFunction();
        assertTrue(fn.getBody().contains(".focus($1)"),
                "Body should contain focus call with parameter");
        String optionsJson = fn.getCaptures().get(1).toString();
        assertTrue(optionsJson.contains("\"preventScroll\":false"),
                "Should set preventScroll to false");
    }

    @Test
    void focus_withBothOptions_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.VISIBLE, PreventScroll.ENABLED);

        JsFunction fn = singleFocusJsFunction();
        assertTrue(fn.getBody().contains(".focus($1)"),
                "Body should contain focus call with parameter");
        String optionsJson = fn.getCaptures().get(1).toString();
        assertTrue(optionsJson.contains("\"preventScroll\":true"),
                "Should set preventScroll to true");
        assertTrue(optionsJson.contains("\"focusVisible\":true"),
                "Should set focusVisible to true");
    }

    @Test
    void focus_withBothOptionsFalse_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.NOT_VISIBLE, PreventScroll.DISABLED);

        JsFunction fn = singleFocusJsFunction();
        assertTrue(fn.getBody().contains(".focus($1)"),
                "Body should contain focus call with parameter");
        String optionsJson = fn.getCaptures().get(1).toString();
        assertTrue(optionsJson.contains("\"preventScroll\":false"),
                "Should set preventScroll to false");
        assertTrue(optionsJson.contains("\"focusVisible\":false"),
                "Should set focusVisible to false");
    }

    @Test
    void focus_withoutOptions_generatesCorrectJS() {
        ui.add(component);
        component.focus();

        JsFunction fn = singleFocusJsFunction();
        assertTrue(fn.getBody().contains(".focus()"),
                "Body should contain focus call without parameters");
        assertFalse(fn.getBody().contains(".focus($1)"),
                "Body should not contain focus call with parameter");
        assertEquals(1, fn.getCaptures().size(),
                "Should have only the element capture");
    }
}
