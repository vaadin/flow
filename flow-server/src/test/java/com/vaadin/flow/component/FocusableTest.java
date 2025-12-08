/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.FocusOption.FocusVisible;
import com.vaadin.flow.component.FocusOption.PreventScroll;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.tests.util.MockUI;

public class FocusableTest {
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
        Assert.assertEquals(message, expected, invocations.size());
    }

    @Test
    public void focus_withFocusVisible_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.VISIBLE);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assert.assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        Assert.assertTrue("Should contain setTimeout wrapper",
                expression.contains("setTimeout"));
        Assert.assertTrue("Should contain focus call with parameter",
                expression.contains(".focus($1)"));

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is element, second param is the options object
        Assert.assertTrue("Should have at least 2 parameters",
                params.size() >= 2);
        String paramJson = params.get(1).toString();
        Assert.assertTrue("Should set focusVisible to true",
                paramJson.contains("\"focusVisible\":true"));
        Assert.assertFalse("Should not contain preventScroll",
                paramJson.contains("preventScroll"));
    }

    @Test
    public void focus_withFocusNotVisible_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.NOT_VISIBLE);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assert.assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        Assert.assertTrue("Should contain setTimeout wrapper",
                expression.contains("setTimeout"));
        Assert.assertTrue("Should contain focus call with parameter",
                expression.contains(".focus($1)"));

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is element, second param is the options object
        Assert.assertTrue("Should have at least 2 parameters",
                params.size() >= 2);
        String paramJson = params.get(1).toString();
        Assert.assertTrue("Should set focusVisible to false",
                paramJson.contains("\"focusVisible\":false"));
    }

    @Test
    public void focus_withPreventScrollEnabled_generatesCorrectJS() {
        ui.add(component);
        component.focus(PreventScroll.ENABLED);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assert.assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        Assert.assertTrue("Should contain setTimeout wrapper",
                expression.contains("setTimeout"));
        Assert.assertTrue("Should contain focus call with parameter",
                expression.contains(".focus($1)"));

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is element, second param is the options object
        Assert.assertTrue("Should have at least 2 parameters",
                params.size() >= 2);
        String paramJson = params.get(1).toString();
        Assert.assertTrue("Should set preventScroll to true",
                paramJson.contains("\"preventScroll\":true"));
        Assert.assertFalse("Should not contain focusVisible",
                paramJson.contains("focusVisible"));
    }

    @Test
    public void focus_withPreventScrollDisabled_generatesCorrectJS() {
        ui.add(component);
        component.focus(PreventScroll.DISABLED);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assert.assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        Assert.assertTrue("Should contain setTimeout wrapper",
                expression.contains("setTimeout"));
        Assert.assertTrue("Should contain focus call with parameter",
                expression.contains(".focus($1)"));

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is element, second param is the options object
        Assert.assertTrue("Should have at least 2 parameters",
                params.size() >= 2);
        String paramJson = params.get(1).toString();
        Assert.assertTrue("Should set preventScroll to false",
                paramJson.contains("\"preventScroll\":false"));
    }

    @Test
    public void focus_withBothOptions_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.VISIBLE, PreventScroll.ENABLED);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assert.assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        Assert.assertTrue("Should contain setTimeout wrapper",
                expression.contains("setTimeout"));
        Assert.assertTrue("Should contain focus call with parameter",
                expression.contains(".focus($1)"));

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is element, second param is the options object
        Assert.assertTrue("Should have at least 2 parameters",
                params.size() >= 2);
        String paramJson = params.get(1).toString();
        Assert.assertTrue("Should set preventScroll to true",
                paramJson.contains("\"preventScroll\":true"));
        Assert.assertTrue("Should set focusVisible to true",
                paramJson.contains("\"focusVisible\":true"));
    }

    @Test
    public void focus_withBothOptionsFalse_generatesCorrectJS() {
        ui.add(component);
        component.focus(FocusVisible.NOT_VISIBLE, PreventScroll.DISABLED);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assert.assertEquals(1, invocations.size());

        String expression = invocations.get(0).getInvocation().getExpression();
        Assert.assertTrue("Should contain setTimeout wrapper",
                expression.contains("setTimeout"));
        Assert.assertTrue("Should contain focus call with parameter",
                expression.contains(".focus($1)"));

        // Check the parameters
        List<Object> params = invocations.get(0).getInvocation()
                .getParameters();
        // First param is element, second param is the options object
        Assert.assertTrue("Should have at least 2 parameters",
                params.size() >= 2);
        String paramJson = params.get(1).toString();
        Assert.assertTrue("Should set preventScroll to false",
                paramJson.contains("\"preventScroll\":false"));
        Assert.assertTrue("Should set focusVisible to false",
                paramJson.contains("\"focusVisible\":false"));
    }

    @Test
    public void focus_withoutOptions_generatesCorrectJS() {
        ui.add(component);
        component.focus();

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assert.assertEquals(1, invocations.size());

        String expression = invocations.getFirst().getInvocation()
                .getExpression();
        Assert.assertTrue("Should contain setTimeout wrapper",
                expression.contains("setTimeout"));
        Assert.assertTrue("Should contain focus call without parameters",
                expression.contains(".focus()"));
        Assert.assertFalse("Should not contain focus call with parameter",
                expression.contains(".focus($1)"));

        // Check the parameters
        List<Object> params = invocations.getFirst().getInvocation()
                .getParameters();
        Assert.assertEquals("Should have exactly 1 parameter (the element)", 1,
                params.size());
    }
}
