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
package com.vaadin.flow.component.html;

import org.junit.Assert;
import org.junit.Test;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;

public class RangeInputTest extends ComponentTest {

    // Actual test methods in super class

    @Override
    public void setup() throws IntrospectionException, InstantiationException,
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
    public void testHasAriaLabelIsImplemented() {
        super.testHasAriaLabelIsImplemented();
    }

    @Test
    public void settingOrientationUpdatesStylesProperly() {
        final RangeInput rangeInput = new RangeInput();
        Assert.assertNull(rangeInput.getStyle().get("-webkit-appearance"));
        Assert.assertNull(rangeInput.getStyle().get("appearance"));
        Assert.assertNull(rangeInput.getStyle().get("writing-mode"));
        Assert.assertEquals(RangeInput.Orientation.HORIZONTAL,
                rangeInput.getOrientation());
        rangeInput.setOrientation(RangeInput.Orientation.VERTICAL);
        Assert.assertEquals("slider-vertical",
                rangeInput.getStyle().get("-webkit-appearance"));
        Assert.assertEquals("slider-vertical",
                rangeInput.getStyle().get("appearance"));
        Assert.assertEquals("bt-lr", rangeInput.getStyle().get("writing-mode"));
        Assert.assertEquals(RangeInput.Orientation.VERTICAL,
                rangeInput.getOrientation());
        rangeInput.setOrientation(RangeInput.Orientation.HORIZONTAL);
        Assert.assertNull(rangeInput.getStyle().get("-webkit-appearance"));
        Assert.assertNull(rangeInput.getStyle().get("appearance"));
        Assert.assertNull(rangeInput.getStyle().get("writing-mode"));
        Assert.assertEquals(RangeInput.Orientation.HORIZONTAL,
                rangeInput.getOrientation());
    }
}
