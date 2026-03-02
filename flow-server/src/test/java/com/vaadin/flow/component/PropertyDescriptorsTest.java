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

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.ComponentTest.TestComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertyDescriptorsTest {

    private static final Integer ZERO = Integer.valueOf(0);
    private static final Integer ONE = Integer.valueOf(1);
    private static final Double ZERO_DOUBLE = 0.0;
    private static final Double ONE_DOUBLE = 1.0;
    private static final String EMPTY_STRING = "";
    private static final String FOO = "foo";
    private static final String SOME_STRING_VALUE = "foobar123456";
    private static final Integer SOME_INTEGER_VALUE = Integer
            .valueOf(129837419);
    private static final Double SOME_DOUBLE_VALUE = 3.14;
    private static final String TEST_PROPERTY = "someproperty";
    private TestComponent component;

    private static PropertyDescriptor<String, String> stringAttributeDefaultEmpty = PropertyDescriptors
            .attributeWithDefault(TEST_PROPERTY, EMPTY_STRING);
    private static PropertyDescriptor<String, String> stringAttributeDefaultFoo = PropertyDescriptors
            .attributeWithDefault(TEST_PROPERTY, FOO);
    private static PropertyDescriptor<String, Optional<String>> stringAttributeOptionalDefaultEmpty = PropertyDescriptors
            .optionalAttributeWithDefault(TEST_PROPERTY, EMPTY_STRING);
    private static PropertyDescriptor<String, Optional<String>> stringAttributeOptionalDefaultFoo = PropertyDescriptors
            .optionalAttributeWithDefault(TEST_PROPERTY, FOO);

    private static PropertyDescriptor<String, String> stringPropertyDefaultEmpty = PropertyDescriptors
            .propertyWithDefault(TEST_PROPERTY, EMPTY_STRING);
    private static PropertyDescriptor<String, String> stringPropertyDefaultFoo = PropertyDescriptors
            .propertyWithDefault(TEST_PROPERTY, FOO);
    private static PropertyDescriptor<Integer, Integer> integerPropertyDefaultZero = PropertyDescriptors
            .propertyWithDefault(TEST_PROPERTY, 0);
    private static PropertyDescriptor<Integer, Integer> integerPropertyDefaultOne = PropertyDescriptors
            .propertyWithDefault(TEST_PROPERTY, 1);
    private static PropertyDescriptor<Double, Double> doublePropertyDefaultZero = PropertyDescriptors
            .propertyWithDefault(TEST_PROPERTY, 0.0);
    private static PropertyDescriptor<Double, Double> doublePropertyDefaultOne = PropertyDescriptors
            .propertyWithDefault(TEST_PROPERTY, 1.0);
    private static PropertyDescriptor<Boolean, Boolean> booleanPropertyDefaultFalse = PropertyDescriptors
            .propertyWithDefault(TEST_PROPERTY, false);
    private static PropertyDescriptor<Boolean, Boolean> booleanPropertyDefaultTrue = PropertyDescriptors
            .propertyWithDefault(TEST_PROPERTY, true);

    @BeforeEach
    public void setup() {
        component = new TestComponent();
    }

    @Test
    public void stringPropertyDefaultEmptyString_initial() {
        assertEquals(EMPTY_STRING, stringPropertyDefaultEmpty.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void stringPropertyDefaultEmptyString_setNonDefault() {
        stringPropertyDefaultEmpty.set(component, SOME_STRING_VALUE);
        assertEquals(SOME_STRING_VALUE,
                stringPropertyDefaultEmpty.get(component));
        assertEquals(SOME_STRING_VALUE,
                component.getElement().getPropertyRaw(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void stringPropertyDefaultEmptyString_resetToDefault() {
        stringPropertyDefaultEmpty.set(component, SOME_STRING_VALUE);
        stringPropertyDefaultEmpty.set(component, EMPTY_STRING);
        assertEquals(EMPTY_STRING, stringPropertyDefaultEmpty.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void stringPropertyDefaultEmptyString_setToNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            stringPropertyDefaultEmpty.set(component, null);
        });
    }

    @Test
    public void stringPropertyDefaultFoo_initial() {
        assertEquals(FOO, stringPropertyDefaultFoo.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void stringPropertyDefaultFoo_setNonDefault() {
        stringPropertyDefaultFoo.set(component, SOME_STRING_VALUE);
        assertEquals(SOME_STRING_VALUE,
                stringPropertyDefaultFoo.get(component));
        assertEquals(SOME_STRING_VALUE,
                component.getElement().getPropertyRaw(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void stringPropertyDefaultFoo_resetToDefault() {
        stringPropertyDefaultFoo.set(component, SOME_STRING_VALUE);
        stringPropertyDefaultFoo.set(component, FOO);
        assertEquals(FOO, stringPropertyDefaultFoo.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void stringPropertyDefaultFoo_setToNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            stringPropertyDefaultFoo.set(component, null);
        });
    }

    @Test
    public void integerPropertyDefaultZero_initial() {
        assertEquals(ZERO, integerPropertyDefaultZero.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void integerPropertyDefaultZero_setNonDefault() {
        integerPropertyDefaultZero.set(component, SOME_INTEGER_VALUE);
        assertEquals(SOME_INTEGER_VALUE,
                integerPropertyDefaultZero.get(component));
        assertEquals(SOME_INTEGER_VALUE, Integer.valueOf(
                component.getElement().getProperty(TEST_PROPERTY, -1)));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void integerPropertyDefaultZero_resetToDefault() {
        integerPropertyDefaultZero.set(component, SOME_INTEGER_VALUE);
        integerPropertyDefaultZero.set(component, ZERO);
        assertEquals(ZERO, integerPropertyDefaultZero.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void integerPropertyDefaultZero_setToNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            integerPropertyDefaultZero.set(component, null);
        });
    }

    @Test
    public void integerPropertyDefaultOne_initial() {
        assertEquals(ONE, integerPropertyDefaultOne.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void integerPropertyDefaultOne_setNonDefault() {
        integerPropertyDefaultOne.set(component, SOME_INTEGER_VALUE);
        assertEquals(SOME_INTEGER_VALUE,
                integerPropertyDefaultOne.get(component));
        assertEquals(SOME_INTEGER_VALUE, Integer.valueOf(
                component.getElement().getProperty(TEST_PROPERTY, -1)));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void integerPropertyDefaultOne_resetToDefault() {
        integerPropertyDefaultOne.set(component, SOME_INTEGER_VALUE);
        integerPropertyDefaultOne.set(component, ONE);
        assertEquals(ONE, integerPropertyDefaultOne.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void integerPropertyDefaultOne_setToNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            integerPropertyDefaultOne.set(component, null);
        });
    }

    @Test
    public void doublePropertyDefaultZero_initial() {
        assertEquals(ZERO_DOUBLE, doublePropertyDefaultZero.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void doublePropertyDefaultZero_setNonDefault() {
        doublePropertyDefaultZero.set(component, SOME_DOUBLE_VALUE);
        assertEquals(SOME_DOUBLE_VALUE,
                doublePropertyDefaultZero.get(component));
        assertEquals(SOME_DOUBLE_VALUE, Double.valueOf(
                component.getElement().getProperty(TEST_PROPERTY, -1.0)));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void doublePropertyDefaultZero_resetToDefault() {
        doublePropertyDefaultZero.set(component, SOME_DOUBLE_VALUE);
        doublePropertyDefaultZero.set(component, ZERO_DOUBLE);
        assertEquals(ZERO_DOUBLE, doublePropertyDefaultZero.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void doublePropertyDefaultZero_setToNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            doublePropertyDefaultZero.set(component, null);
        });
    }

    @Test
    public void doublePropertyDefaultOne_initial() {
        assertEquals(ONE_DOUBLE, doublePropertyDefaultOne.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void doublePropertyDefaultOne_setNonDefault() {
        doublePropertyDefaultOne.set(component, SOME_DOUBLE_VALUE);
        assertEquals(SOME_DOUBLE_VALUE,
                doublePropertyDefaultOne.get(component));
        assertEquals(SOME_DOUBLE_VALUE, Double.valueOf(
                component.getElement().getProperty(TEST_PROPERTY, -1.0)));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void doublePropertyDefaultOne_resetToDefault() {
        doublePropertyDefaultOne.set(component, SOME_DOUBLE_VALUE);
        doublePropertyDefaultOne.set(component, ONE_DOUBLE);
        assertEquals(ONE_DOUBLE, doublePropertyDefaultOne.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void doublePropertyDefaultOne_setToNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            doublePropertyDefaultOne.set(component, null);
        });
    }

    @Test
    public void booleanPropertyDefaultFalse_initial() {
        assertEquals(false, booleanPropertyDefaultFalse.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void booleanPropertyDefaultFalse_setNonDefault() {
        booleanPropertyDefaultFalse.set(component, true);
        assertEquals(true, booleanPropertyDefaultFalse.get(component));
        assertEquals(true, Boolean.valueOf(component.getElement()
                .getProperty(TEST_PROPERTY, "neverused")));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void booleanPropertyDefaultFalse_resetToDefault() {
        booleanPropertyDefaultFalse.set(component, true);
        booleanPropertyDefaultFalse.set(component, false);
        assertEquals(false, booleanPropertyDefaultFalse.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void booleanPropertyDefaultFalse_setToNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            booleanPropertyDefaultFalse.set(component, null);
        });
    }

    @Test
    public void booleanPropertyDefaultTrue_initial() {
        assertEquals(true, booleanPropertyDefaultTrue.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void booleanPropertyDefaultTrue_setNonDefault() {
        booleanPropertyDefaultTrue.set(component, false);
        assertEquals(false, booleanPropertyDefaultTrue.get(component));
        assertEquals(false, Boolean.valueOf(component.getElement()
                .getProperty(TEST_PROPERTY, "neverused")));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void booleanPropertyDefaultTrue_resetToDefault() {
        booleanPropertyDefaultTrue.set(component, false);
        booleanPropertyDefaultTrue.set(component, true);
        assertEquals(true, booleanPropertyDefaultTrue.get(component));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void booleanPropertyDefaultTrue_setToNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            booleanPropertyDefaultTrue.set(component, null);
        });
    }

    @Test
    public void stringAttributeDefaultEmpty_initial() {
        assertEquals(EMPTY_STRING, stringAttributeDefaultEmpty.get(component));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeDefaultEmpty_setNonDefault() {
        stringAttributeDefaultEmpty.set(component, SOME_STRING_VALUE);
        assertEquals(SOME_STRING_VALUE,
                stringAttributeDefaultEmpty.get(component));
        assertEquals(SOME_STRING_VALUE,
                component.getElement().getAttribute(TEST_PROPERTY));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeDefaultEmpty_resetToDefault() {
        stringAttributeDefaultEmpty.set(component, SOME_STRING_VALUE);
        stringAttributeDefaultEmpty.set(component, EMPTY_STRING);
        assertEquals(EMPTY_STRING, stringAttributeDefaultEmpty.get(component));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));

    }

    @Test
    public void stringAttributeDefaultEmpty_setToNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            stringAttributeDefaultEmpty.set(component, null);
        });
    }

    @Test
    public void stringAttributeDefaultFoo_initial() {
        assertEquals(FOO, stringAttributeDefaultFoo.get(component));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeDefaultFoo_setNonDefault() {
        stringAttributeDefaultFoo.set(component, SOME_STRING_VALUE);
        assertEquals(SOME_STRING_VALUE,
                stringAttributeDefaultFoo.get(component));
        assertEquals(SOME_STRING_VALUE,
                component.getElement().getAttribute(TEST_PROPERTY));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeDefaultFoo_resetToDefault() {
        stringAttributeDefaultFoo.set(component, SOME_STRING_VALUE);
        stringAttributeDefaultFoo.set(component, FOO);
        assertEquals(FOO, stringAttributeDefaultFoo.get(component));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));

    }

    @Test
    public void stringAttributeDefaultFoo_setToNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            stringAttributeDefaultFoo.set(component, null);
        });
    }

    @Test
    public void stringAttributeOptionalDefaultEmpty_initial() {
        assertEquals(Optional.empty(),
                stringAttributeOptionalDefaultEmpty.get(component));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeOptionalDefaultEmpty_setNonOptionalDefault() {
        stringAttributeOptionalDefaultEmpty.set(component, SOME_STRING_VALUE);
        assertEquals(Optional.of(SOME_STRING_VALUE),
                stringAttributeOptionalDefaultEmpty.get(component));
        assertEquals(SOME_STRING_VALUE,
                component.getElement().getAttribute(TEST_PROPERTY));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeOptionalDefaultEmpty_resetToOptionalDefault() {
        stringAttributeOptionalDefaultEmpty.set(component, SOME_STRING_VALUE);
        stringAttributeOptionalDefaultEmpty.set(component, EMPTY_STRING);
        assertEquals(Optional.empty(),
                stringAttributeOptionalDefaultEmpty.get(component));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));

    }

    @Test
    public void stringAttributeOptionalDefaultEmpty_setToNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            stringAttributeOptionalDefaultEmpty.set(component, null);
        });
    }

    @Test
    public void stringAttributeOptionalDefaultFoo_initial() {
        assertEquals(Optional.empty(),
                stringAttributeOptionalDefaultFoo.get(component));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeOptionalDefaultFoo_setNonOptionalDefault() {
        stringAttributeOptionalDefaultFoo.set(component, SOME_STRING_VALUE);
        assertEquals(Optional.of(SOME_STRING_VALUE),
                stringAttributeOptionalDefaultFoo.get(component));
        assertEquals(SOME_STRING_VALUE,
                component.getElement().getAttribute(TEST_PROPERTY));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeOptionalDefaultFoo_resetToOptionalDefault() {
        stringAttributeOptionalDefaultFoo.set(component, SOME_STRING_VALUE);
        stringAttributeOptionalDefaultFoo.set(component, FOO);
        assertEquals(Optional.empty(),
                stringAttributeOptionalDefaultFoo.get(component));
        assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        assertFalse(component.getElement().hasProperty(TEST_PROPERTY));

    }

    @Test
    public void stringAttributeOptionalDefaultFoo_setToNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            stringAttributeOptionalDefaultFoo.set(component, null);
        });
    }

    @Test
    public void propertyName() {
        assertEquals(TEST_PROPERTY,
                stringAttributeDefaultEmpty.getPropertyName());
    }
}
