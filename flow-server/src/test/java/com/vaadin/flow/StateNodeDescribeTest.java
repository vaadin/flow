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
package com.vaadin.flow;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.ComponentTracker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the parts of {@link com.vaadin.flow.internal.StateNode#describe()} that
 * need component tracking.
 * <p>
 * Note that this is intentionally in the "wrong" package, as
 * {@code com.vaadin.flow.internal} is one of the packages the tracker skips
 * when looking for the relevant stack frame.
 */
class StateNodeDescribeTest {

    @Tag("div")
    public static class TrackedComponent extends Component {
    }

    private Object previousDisabled;
    private Field disabledField;

    @BeforeEach
    void enableTracking() throws Exception {
        disabledField = ComponentTracker.class.getDeclaredField("disabled");
        disabledField.setAccessible(true);
        previousDisabled = disabledField.get(null);
        disabledField.set(null, false);
    }

    @AfterEach
    void restoreTracking() throws Exception {
        disabledField.set(null, previousDisabled);
    }

    @Test
    void describe_trackingEnabled_createAndAttachLocationIncluded() {
        TrackedComponent component = new TrackedComponent();
        UI ui = new UI();
        ui.getElement().appendChild(component.getElement());

        String description = component.getElement().getNode().describe();

        String filename = getClass().getSimpleName() + ".java:";
        assertTrue(description.contains("created at " + filename), description);
        assertTrue(description.contains("attached at " + filename),
                description);
        // The locations tell the instances apart, so the component's own
        // toString() is not needed
        assertFalse(description.contains(component.toString()), description);
    }
}
