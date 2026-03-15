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
package com.vaadin.flow.component.html;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RangeInputTest extends ComponentTest {

    // Actual test methods in super class

    @BeforeEach
    @Override
    void setup() throws IntrospectionException, InstantiationException,
            IllegalAccessException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException {
        whitelistProperty("valueChangeMode");
        whitelistProperty("valueChangeTimeout");
        whitelistProperty("enabled");
        whitelistProperty("readOnly");
        super.setup();
    }

    @Override
    protected void addProperties() {
        // Object.class because of generics
        addProperty("value", Object.class, 0.0, 5.0, false, false);
        addProperty("min", double.class, 0.0, 4.0, false, false);
        addProperty("max", double.class, 100.0, 1000.0, false, false);
        addProperty("step", Double.class, 1.0, 0.5, false, false);
        final ComponentProperty orientationProperty = addProperty("orientation",
                RangeInput.Orientation.class, RangeInput.Orientation.HORIZONTAL,
                RangeInput.Orientation.VERTICAL, false, true);
        orientationProperty.propertyOrAttributeTag = "orient";
    }

    @Test
    @Override
    protected void testHasAriaLabelIsImplemented() {
        super.testHasAriaLabelIsImplemented();
    }

    @Test
    void settingOrientationUpdatesStylesProperly() {
        final RangeInput rangeInput = new RangeInput();
        assertNull(rangeInput.getStyle().get("-webkit-appearance"));
        assertNull(rangeInput.getStyle().get("appearance"));
        assertNull(rangeInput.getStyle().get("writing-mode"));
        assertEquals(RangeInput.Orientation.HORIZONTAL,
                rangeInput.getOrientation());
        rangeInput.setOrientation(RangeInput.Orientation.VERTICAL);
        assertEquals("slider-vertical",
                rangeInput.getStyle().get("-webkit-appearance"));
        assertEquals("slider-vertical",
                rangeInput.getStyle().get("appearance"));
        assertEquals("bt-lr", rangeInput.getStyle().get("writing-mode"));
        assertEquals(RangeInput.Orientation.VERTICAL,
                rangeInput.getOrientation());
        rangeInput.setOrientation(RangeInput.Orientation.HORIZONTAL);
        assertNull(rangeInput.getStyle().get("-webkit-appearance"));
        assertNull(rangeInput.getStyle().get("appearance"));
        assertNull(rangeInput.getStyle().get("writing-mode"));
        assertEquals(RangeInput.Orientation.HORIZONTAL,
                rangeInput.getOrientation());
    }
}
