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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HasAriaRoleTest {

    @Tag(Tag.DIV)
    private static class TestComponent extends Component
            implements HasAriaRole {

    }

    @Test
    void withoutAriaRole_getAriaRoleReturnsEmptyOptional() {
        TestComponent component = new TestComponent();

        assertFalse(component.getAriaRole().isPresent());
    }

    @Test
    void withNullAriaRole_getAriaRoleReturnsEmptyOptional() {
        TestComponent component = new TestComponent();
        component.setAriaRole(null);
        assertFalse(component.getAriaRole().isPresent());
    }

    @Test
    void setAriaRole() {
        TestComponent component = new TestComponent();
        component.setAriaRole("alertdialog");

        assertEquals("alertdialog", component.getAriaRole().get());
    }

    @Test
    void withAriaRole_setAriaRoleToNullClearsAriaRole() {
        TestComponent component = new TestComponent();
        component.setAriaRole("alertdialog");

        component.setAriaRole(null);
        assertFalse(component.getAriaRole().isPresent());
    }
}
