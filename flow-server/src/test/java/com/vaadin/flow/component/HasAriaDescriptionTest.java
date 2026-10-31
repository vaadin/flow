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
import static org.junit.jupiter.api.Assertions.assertTrue;

class HasAriaDescriptionTest {

    @Tag(Tag.MAIN) // main is used, because div is not a valid target by default
    private static class TestComponent extends Component
            implements HasAriaDescription {

    }

    @Test
    void setAriaDescribedBy() {
        TestComponent component = new TestComponent();
        assertFalse(component.getAriaDescribedBy().isPresent());

        component.setAriaDescribedBy("description-id");
        assertEquals("description-id", component.getAriaDescribedBy().get());

        component.setAriaDescribedBy((String) null);
        assertFalse(component.getAriaDescribedBy().isPresent());
    }

    @Test
    void setAriaDescribedByComponent_withoutId() {
        UI ui = new UI();
        TestComponent component = new TestComponent();
        TestComponent descriptionComponent = new TestComponent();
        ui.add(component, descriptionComponent);

        component.setAriaDescribedBy(descriptionComponent);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        String generatedId = descriptionComponent.getId().get();
        assertTrue(generatedId.startsWith("ariadescribedby-"));
        assertEquals(generatedId, component.getAriaDescribedBy().get());
    }

    @Test
    void setAriaDescribedByComponent_withIdSetBefore() {
        UI ui = new UI();
        TestComponent component = new TestComponent();
        TestComponent descriptionComponent = new TestComponent();
        ui.add(component, descriptionComponent);

        descriptionComponent.setId("description-id");
        component.setAriaDescribedBy(descriptionComponent);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertEquals("description-id", component.getAriaDescribedBy().get());
    }

    @Test
    void setAriaDescribedByComponent_explicitValueSetAfter_explicitValueWins() {
        UI ui = new UI();
        TestComponent component = new TestComponent();
        TestComponent descriptionComponent = new TestComponent();
        ui.add(component, descriptionComponent);

        component.setAriaDescribedBy(descriptionComponent);
        component.setAriaDescribedBy("explicit-id");
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertEquals("explicit-id", component.getAriaDescribedBy().get());
        assertFalse(descriptionComponent.getId().isPresent());
    }

    @Test
    void setAriaDescribedByComponent_removedAfter_attributeStaysRemoved() {
        UI ui = new UI();
        TestComponent component = new TestComponent();
        TestComponent descriptionComponent = new TestComponent();
        ui.add(component, descriptionComponent);

        component.setAriaDescribedBy(descriptionComponent);
        component.setAriaDescribedBy((String) null);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertFalse(component.getAriaDescribedBy().isPresent());
        assertFalse(descriptionComponent.getId().isPresent());
    }

    @Test
    void setAriaDescribedByComponent_calledTwice_lastComponentWins() {
        UI ui = new UI();
        TestComponent component = new TestComponent();
        TestComponent firstDescription = new TestComponent();
        TestComponent secondDescription = new TestComponent();
        ui.add(component, firstDescription, secondDescription);

        component.setAriaDescribedBy(firstDescription);
        component.setAriaDescribedBy(secondDescription);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertFalse(firstDescription.getId().isPresent(),
                "The superseded description should keep its original id");
        assertEquals(secondDescription.getId().orElse(null),
                component.getAriaDescribedBy().orElse(null));
    }

    @Test
    void setAriaDescribedByComponent_withIdSetAfter() {
        UI ui = new UI();
        TestComponent component = new TestComponent();
        TestComponent descriptionComponent = new TestComponent();
        ui.add(component, descriptionComponent);

        component.setAriaDescribedBy(descriptionComponent);
        descriptionComponent.setId("description-id");
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertEquals("description-id", component.getAriaDescribedBy().get());
    }
}
