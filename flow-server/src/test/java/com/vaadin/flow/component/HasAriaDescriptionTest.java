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
    void setAriaDescribedBy() {
        TestComponent component = new TestComponent();
        Assertions.assertFalse(component.getAriaDescribedBy().isPresent());

        component.setAriaDescribedBy("description-id");
        Assertions.assertEquals("description-id",
                component.getAriaDescribedBy().get());

        component.setAriaDescribedBy((String) null);
        Assertions.assertFalse(component.getAriaDescribedBy().isPresent());
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
        Assertions.assertTrue(generatedId.startsWith("ariadescribedby-"));
        Assertions.assertEquals(generatedId,
                component.getAriaDescribedBy().get());
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

        Assertions.assertEquals("description-id",
                component.getAriaDescribedBy().get());
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

        Assertions.assertEquals("description-id",
                component.getAriaDescribedBy().get());
    }
}
