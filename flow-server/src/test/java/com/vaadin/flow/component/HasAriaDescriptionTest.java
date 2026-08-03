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

class HasAriaDescriptionTest {

    @Tag(Tag.MAIN) // main is used, because div is not a valid target by default
    private static class TestComponent extends Component
            implements HasAriaDescription {

    }

    @Test
    void withoutAriaDescription_getAriaDescriptionReturnsEmptyOptional() {
        TestComponent component = new TestComponent();

        Assertions.assertFalse(component.getAriaDescription().isPresent());
    }

    @Test
    void setAriaDescription() {
        TestComponent component = new TestComponent();
        component.setAriaDescription("test AriaDescription");

        Assertions.assertEquals("test AriaDescription",
                component.getAriaDescription().get());
    }

    @Test
    void withAriaDescription_setAriaDescriptionToNullClearsAriaDescription() {
        TestComponent component = new TestComponent();
        component.setAriaDescription("test AriaDescription");

        component.setAriaDescription(null);
        Assertions.assertFalse(component.getAriaDescription().isPresent());
    }

    @Test
    void withoutAriaDescribedBy_getAriaDescribedByReturnsEmptyOptional() {
        TestComponent component = new TestComponent();

        Assertions.assertFalse(component.getAriaDescribedBy().isPresent());
    }

    @Test
    void setAriaDescribedBy() {
        TestComponent component = new TestComponent();
        component.setAriaDescribedBy("test AriaDescribedBy");

        Assertions.assertEquals("test AriaDescribedBy",
                component.getAriaDescribedBy().get());
    }

    @Test
    void withAriaDescribedBy_setAriaDescribedByToNullClearsAriaDescribedBy() {
        TestComponent component = new TestComponent();
        component.setAriaDescribedBy("test AriaDescribedBy");

        component.setAriaDescribedBy((String) null);
        Assertions.assertFalse(component.getAriaDescribedBy().isPresent());
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

        Assertions.assertEquals("the-description",
                component.getAriaDescribedBy().get());
    }

    @Test
    void setAriaDescribedByComponent_withoutId_generatesId() {
        UI ui = new UI();
        TestComponent component = new TestComponent();
        TestComponent descriptionComponent = new TestComponent();
        Assertions.assertFalse(descriptionComponent.getId().isPresent());
        ui.add(component, descriptionComponent);

        component.setAriaDescribedBy(descriptionComponent);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        Assertions.assertTrue(descriptionComponent.getId().isPresent());
        Assertions.assertTrue(descriptionComponent.getId().get()
                .startsWith("ariadescribedby-"));
        Assertions.assertEquals(descriptionComponent.getId().get(),
                component.getAriaDescribedBy().get());
    }

    @Test
    void setAriaDescribedByComponent_idSetLater() {
        UI ui = new UI();
        TestComponent component = new TestComponent();
        TestComponent descriptionComponent = new TestComponent();
        ui.add(component, descriptionComponent);

        component.setAriaDescribedBy(descriptionComponent);
        descriptionComponent.setId("manually-set-id");
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        Assertions.assertEquals("manually-set-id",
                component.getAriaDescribedBy().get());
    }
}
