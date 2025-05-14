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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class HasAriaLabelTest {

    @Tag(Tag.MAIN) // main is used, because div is not a valid target by default
    private static class TestComponent extends Component
            implements HasAriaLabel {

    }

    @Test
    public void withoutAriaLabelComponent_getAriaLabelReturnsEmptyOptional() {
        TestComponent component = new TestComponent();

        assertFalse(component.getAriaLabel().isPresent());
    }

    @Test
    public void withNullAriaLabel_getAriaLabelReturnsEmptyOptional() {
        TestComponent component = new TestComponent();
        component.setAriaLabel(null);
        assertFalse(component.getAriaLabel().isPresent());
    }

    @Test
    public void withEmptyAriaLabel_getAriaLabelReturnsEmptyString() {
        TestComponent component = new TestComponent();
        component.setAriaLabel("");
        assertEquals("", component.getAriaLabel().get());
    }

    @Test
    public void withAriaLabel_setAriaLabelToNullClearsAriaLabel() {
        TestComponent component = new TestComponent();
        component.setAriaLabel("test AriaLabel");

        component.setAriaLabel(null);
        assertFalse(component.getAriaLabel().isPresent());
    }

    @Test
    public void setAriaLabel() {
        TestComponent component = new TestComponent();
        component.setAriaLabel("test AriaLabel");

        assertEquals("test AriaLabel", component.getAriaLabel().get());
    }

    @Test
    public void withoutAriaLabelledByComponent_getAriaLabelledByReturnsEmptyOptional() {
        TestComponent component = new TestComponent();

        assertFalse(component.getAriaLabelledBy().isPresent());
    }

    @Test
    public void withNullAriaLabelledBy_getAriaLabelledByReturnsEmptyOptional() {
        TestComponent component = new TestComponent();
        component.setAriaLabelledBy(null);
        assertFalse(component.getAriaLabelledBy().isPresent());
    }

    @Test
    public void withEmptyAriaLabelledBy_getAriaLabelledByReturnsEmptyString() {
        TestComponent component = new TestComponent();
        component.setAriaLabelledBy("");
        assertEquals("", component.getAriaLabelledBy().get());
    }

    @Test
    public void withAriaLabelledBy_setAriaLabelledByToNullClearsAriaLabelledBy() {
        TestComponent component = new TestComponent();
        component.setAriaLabelledBy("test AriaLabelledBy");

        component.setAriaLabelledBy(null);
        assertFalse(component.getAriaLabelledBy().isPresent());
    }

    @Test
    public void setAriaLabelledBy() {
        TestComponent component = new TestComponent();
        component.setAriaLabelledBy("test AriaLabelledBy");

        assertEquals("test AriaLabelledBy",
                component.getAriaLabelledBy().get());
    }
}
