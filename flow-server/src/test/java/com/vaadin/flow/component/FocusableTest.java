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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.FocusOption.FocusVisible;
import com.vaadin.flow.component.FocusOption.PreventScroll;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.tests.util.MockUI;

class FocusableTest {
    @Tag("div")
    private static class FocusableTestComponent extends Component
            implements Focusable {

    }

    private final MockUI ui = new MockUI();
    private final FocusableTestComponent component = new FocusableTestComponent();

    @Test
    public void focusUnattached_nothingScheduled() {
        component.focus();

        assertPendingInvocationCount(
                "Nothing should be scheduled when component is not attached",
                0);
    }

    @Test
    public void focusBeforeAttach_executionScheduled() {
        component.focus();
        ui.add(component);

        assertPendingInvocationCount(
                "An focus() inovocation should be pending for the attached component",
                1);
    }

    @Test
    public void focusAfterAttach_executionScheduled() {
        ui.add(component);
        component.focus();

        assertPendingInvocationCount(
                "An focus() inovocation should be pending for the attached component",
                1);
    }

    @Test
    public void detachAfterFocus_nothingScheduled() {
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
        Assertions.assertEquals(expected, invocations.size(), message);
    }

    @Test
    public void focus_withFocusVisible_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.VISIBLE);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assertions.assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        Assertions.assertTrue(expression.contains("setTimeout"),
                "Should contain setTimeout wrapper");
        Assertions.assertTrue(expression.contains(".focus($1)"),
                "Should contain focus call with parameter");

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is element, second param is the options object
        Assertions.assertTrue(params.size() >= 2,
                "Should have at least 2 parameters");
        String paramJson = params.get(1).toString();
        Assertions.assertTrue(paramJson.contains("\"focusVisible\":true"),
                "Should set focusVisible to true");
        Assertions.assertFalse(paramJson.contains("preventScroll"),
                "Should not contain preventScroll");
    }

    @Test
    public void focus_withFocusNotVisible_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.NOT_VISIBLE);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assertions.assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        Assertions.assertTrue(expression.contains("setTimeout"),
                "Should contain setTimeout wrapper");
        Assertions.assertTrue(expression.contains(".focus($1)"),
                "Should contain focus call with parameter");

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is element, second param is the options object
        Assertions.assertTrue(params.size() >= 2,
                "Should have at least 2 parameters");
        String paramJson = params.get(1).toString();
        Assertions.assertTrue(paramJson.contains("\"focusVisible\":false"),
                "Should set focusVisible to false");
    }

    @Test
    public void focus_withPreventScrollEnabled_generatesCorrectJS() {
        ui.add(component);
        component.focus(PreventScroll.ENABLED);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assertions.assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        Assertions.assertTrue(expression.contains("setTimeout"),
                "Should contain setTimeout wrapper");
        Assertions.assertTrue(expression.contains(".focus($1)"),
                "Should contain focus call with parameter");

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is element, second param is the options object
        Assertions.assertTrue(params.size() >= 2,
                "Should have at least 2 parameters");
        String paramJson = params.get(1).toString();
        Assertions.assertTrue(paramJson.contains("\"preventScroll\":true"),
                "Should set preventScroll to true");
        Assertions.assertFalse(paramJson.contains("focusVisible"),
                "Should not contain focusVisible");
    }

    @Test
    public void focus_withPreventScrollDisabled_generatesCorrectJS() {
        ui.add(component);
        component.focus(PreventScroll.DISABLED);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assertions.assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        Assertions.assertTrue(expression.contains("setTimeout"),
                "Should contain setTimeout wrapper");
        Assertions.assertTrue(expression.contains(".focus($1)"),
                "Should contain focus call with parameter");

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is element, second param is the options object
        Assertions.assertTrue(params.size() >= 2,
                "Should have at least 2 parameters");
        String paramJson = params.get(1).toString();
        Assertions.assertTrue(paramJson.contains("\"preventScroll\":false"),
                "Should set preventScroll to false");
    }

    @Test
    public void focus_withBothOptions_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.VISIBLE, PreventScroll.ENABLED);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assertions.assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        Assertions.assertTrue(expression.contains("setTimeout"),
                "Should contain setTimeout wrapper");
        Assertions.assertTrue(expression.contains(".focus($1)"),
                "Should contain focus call with parameter");

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is element, second param is the options object
        Assertions.assertTrue(params.size() >= 2,
                "Should have at least 2 parameters");
        String paramJson = params.get(1).toString();
        Assertions.assertTrue(paramJson.contains("\"preventScroll\":true"),
                "Should set preventScroll to true");
        Assertions.assertTrue(paramJson.contains("\"focusVisible\":true"),
                "Should set focusVisible to true");
    }

    @Test
    public void focus_withBothOptionsFalse_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.NOT_VISIBLE, PreventScroll.DISABLED);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assertions.assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        Assertions.assertTrue(expression.contains("setTimeout"),
                "Should contain setTimeout wrapper");
        Assertions.assertTrue(expression.contains(".focus($1)"),
                "Should contain focus call with parameter");

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is element, second param is the options object
        Assertions.assertTrue(params.size() >= 2,
                "Should have at least 2 parameters");
        String paramJson = params.get(1).toString();
        Assertions.assertTrue(paramJson.contains("\"preventScroll\":false"),
                "Should set preventScroll to false");
        Assertions.assertTrue(paramJson.contains("\"focusVisible\":false"),
                "Should set focusVisible to false");
    }

    @Test
    public void focus_withoutOptions_generatesCorrectJS() {
        ui.add(component);
        component.focus();

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assertions.assertEquals(1, invocations.size());

        String expression = invocations.getFirst().getInvocation()
                .getExpression();
        Assertions.assertTrue(expression.contains("setTimeout"),
                "Should contain setTimeout wrapper");
        Assertions.assertTrue(expression.contains(".focus()"),
                "Should contain focus call without parameters");
        Assertions.assertFalse(expression.contains(".focus($1)"),
                "Should not contain focus call with parameter");

        // Check the parameters
        List<Object> params = invocations.getFirst().getInvocation()
                .getParameters();
        Assertions.assertEquals(2, params.size(),
                "Should have exactly 1 parameter (the element node and wrapped parameter)");
    }
}
