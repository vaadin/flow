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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HasAriaDescriptionTest {

    @Tag(Tag.MAIN) // main is used, because div is not a valid target by default
    private static class TestComponent extends Component
            implements HasAriaDescription {

    }

    @Test
    void withoutAriaDescription_getAriaDescriptionReturnsNull() {
        TestComponent component = new TestComponent();

        assertNull(component.getAriaDescription());
    }

    @Test
    void setAriaDescription() {
        TestComponent component = new TestComponent();
        component.setAriaDescription("test AriaDescription");

        assertEquals("test AriaDescription", component.getAriaDescription());
    }

    @Test
    void withAriaDescription_setAriaDescriptionToNullClearsAriaDescription() {
        TestComponent component = new TestComponent();
        component.setAriaDescription("test AriaDescription");

        component.setAriaDescription(null);
        assertNull(component.getAriaDescription());
    }

    @Test
    void withoutAriaDescribedBy_getAriaDescribedByReturnsNull() {
        TestComponent component = new TestComponent();

        assertNull(component.getAriaDescribedBy());
    }

    @Test
    void setAriaDescribedBy() {
        TestComponent component = new TestComponent();
        component.setAriaDescribedBy("test AriaDescribedBy");

        assertEquals("test AriaDescribedBy", component.getAriaDescribedBy());
    }

    @Test
    void withAriaDescribedBy_setAriaDescribedByToNullClearsAriaDescribedBy() {
        TestComponent component = new TestComponent();
        component.setAriaDescribedBy("test AriaDescribedBy");

        component.setAriaDescribedBy((String) null);
        assertNull(component.getAriaDescribedBy());
    }

    @Test
    void setAriaDescribedByComponent_withExistingId() {
        UI ui = new UI();
        TestComponent component = new TestComponent();
        TestComponent descriptionComponent = new TestComponent();
        descriptionComponent.setId("the-description");
        ui.add(component, descriptionComponent);

        component.setAriaDescribedBy(descriptionComponent);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertEquals("the-description", component.getAriaDescribedBy());
    }

    @Test
    void setAriaDescribedByComponent_withoutId_generatesId() {
        UI ui = new UI();
        TestComponent component = new TestComponent();
        TestComponent descriptionComponent = new TestComponent();
        assertFalse(descriptionComponent.getId().isPresent());
        ui.add(component, descriptionComponent);

        component.setAriaDescribedBy(descriptionComponent);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertTrue(descriptionComponent.getId().isPresent());
        assertTrue(descriptionComponent.getId().get()
                .startsWith("ariadescribedby-"));
        assertEquals(descriptionComponent.getId().get(),
                component.getAriaDescribedBy());
    }
}
