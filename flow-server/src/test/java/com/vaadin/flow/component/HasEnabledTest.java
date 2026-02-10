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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HasEnabledTest {

    @Tag(Tag.DIV)
    private static class TestComponent extends Component
            implements HasComponents {

    }

    @Test
    public void enabledComponent_isEnabledReturnsTrue() {
        TestComponent component = new TestComponent();

        Assertions.assertTrue(component.isEnabled());
    }

    @Test
    public void explicitlyDisabledComponent_isEnabledReturnsFalse() {
        TestComponent component = new TestComponent();
        component.setEnabled(false);

        Assertions.assertFalse(component.isEnabled());
    }

    @Test
    public void implicitlyDisabledComponent_isEnabledReturnsFalse() {
        TestComponent component = new TestComponent();

        TestComponent parent = new TestComponent();
        parent.setEnabled(false);

        parent.add(component);

        Assertions.assertFalse(component.isEnabled());
    }

    @Test
    public void implicitlyDisabledComponent_detach_componentBecomesEnabled() {
        TestComponent component = new TestComponent();

        TestComponent parent = new TestComponent();
        parent.add(component);

        parent.setEnabled(false);

        parent.remove(component);

        Assertions.assertTrue(component.isEnabled());
    }

    @Test
    public void explicitlyDisabledComponent_enableParent_componentRemainsDisabled() {
        TestComponent component = new TestComponent();
        component.setEnabled(false);

        TestComponent parent = new TestComponent();
        parent.add(component);

        parent.setEnabled(false);

        Assertions.assertFalse(component.isEnabled());

        parent.setEnabled(true);

        Assertions.assertFalse(component.isEnabled());
    }

}
