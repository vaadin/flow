/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.ComponentTest.TestComponent;

public class PropertyDescriptorsTest {

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

    @Before
    public void setup() {
        component = new TestComponent();
    }

    @Test
    public void stringPropertyDefaultEmptyString_initial() {
        Assert.assertEquals(EMPTY_STRING,
                stringPropertyDefaultEmpty.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void stringPropertyDefaultEmptyString_setNonDefault() {
        stringPropertyDefaultEmpty.set(component, SOME_STRING_VALUE);
        Assert.assertEquals(SOME_STRING_VALUE,
                stringPropertyDefaultEmpty.get(component));
        Assert.assertEquals(SOME_STRING_VALUE,
                component.getElement().getPropertyRaw(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void stringPropertyDefaultEmptyString_resetToDefault() {
        stringPropertyDefaultEmpty.set(component, SOME_STRING_VALUE);
        stringPropertyDefaultEmpty.set(component, EMPTY_STRING);
        Assert.assertEquals(EMPTY_STRING,
                stringPropertyDefaultEmpty.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void stringPropertyDefaultEmptyString_setToNull() {
        stringPropertyDefaultEmpty.set(component, null);
    }

    @Test
    public void stringPropertyDefaultFoo_initial() {
        Assert.assertEquals(FOO, stringPropertyDefaultFoo.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void stringPropertyDefaultFoo_setNonDefault() {
        stringPropertyDefaultFoo.set(component, SOME_STRING_VALUE);
        Assert.assertEquals(SOME_STRING_VALUE,
                stringPropertyDefaultFoo.get(component));
        Assert.assertEquals(SOME_STRING_VALUE,
                component.getElement().getPropertyRaw(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void stringPropertyDefaultFoo_resetToDefault() {
        stringPropertyDefaultFoo.set(component, SOME_STRING_VALUE);
        stringPropertyDefaultFoo.set(component, FOO);
        Assert.assertEquals(FOO, stringPropertyDefaultFoo.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void stringPropertyDefaultFoo_setToNull() {
        stringPropertyDefaultFoo.set(component, null);
    }

    @Test
    public void integerPropertyDefaultZero_initial() {
        Assert.assertEquals(ZERO, integerPropertyDefaultZero.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void integerPropertyDefaultZero_setNonDefault() {
        integerPropertyDefaultZero.set(component, SOME_INTEGER_VALUE);
        Assert.assertEquals(SOME_INTEGER_VALUE,
                integerPropertyDefaultZero.get(component));
        Assert.assertEquals(SOME_INTEGER_VALUE, Integer.valueOf(
                component.getElement().getProperty(TEST_PROPERTY, -1)));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void integerPropertyDefaultZero_resetToDefault() {
        integerPropertyDefaultZero.set(component, SOME_INTEGER_VALUE);
        integerPropertyDefaultZero.set(component, ZERO);
        Assert.assertEquals(ZERO, integerPropertyDefaultZero.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void integerPropertyDefaultZero_setToNull() {
        integerPropertyDefaultZero.set(component, null);
    }

    @Test
    public void integerPropertyDefaultOne_initial() {
        Assert.assertEquals(ONE, integerPropertyDefaultOne.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void integerPropertyDefaultOne_setNonDefault() {
        integerPropertyDefaultOne.set(component, SOME_INTEGER_VALUE);
        Assert.assertEquals(SOME_INTEGER_VALUE,
                integerPropertyDefaultOne.get(component));
        Assert.assertEquals(SOME_INTEGER_VALUE, Integer.valueOf(
                component.getElement().getProperty(TEST_PROPERTY, -1)));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void integerPropertyDefaultOne_resetToDefault() {
        integerPropertyDefaultOne.set(component, SOME_INTEGER_VALUE);
        integerPropertyDefaultOne.set(component, ONE);
        Assert.assertEquals(ONE, integerPropertyDefaultOne.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void integerPropertyDefaultOne_setToNull() {
        integerPropertyDefaultOne.set(component, null);
    }

    @Test
    public void doublePropertyDefaultZero_initial() {
        Assert.assertEquals(ZERO_DOUBLE,
                doublePropertyDefaultZero.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void doublePropertyDefaultZero_setNonDefault() {
        doublePropertyDefaultZero.set(component, SOME_DOUBLE_VALUE);
        Assert.assertEquals(SOME_DOUBLE_VALUE,
                doublePropertyDefaultZero.get(component));
        Assert.assertEquals(SOME_DOUBLE_VALUE, Double.valueOf(
                component.getElement().getProperty(TEST_PROPERTY, -1.0)));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void doublePropertyDefaultZero_resetToDefault() {
        doublePropertyDefaultZero.set(component, SOME_DOUBLE_VALUE);
        doublePropertyDefaultZero.set(component, ZERO_DOUBLE);
        Assert.assertEquals(ZERO_DOUBLE,
                doublePropertyDefaultZero.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void doublePropertyDefaultZero_setToNull() {
        doublePropertyDefaultZero.set(component, null);
    }

    @Test
    public void doublePropertyDefaultOne_initial() {
        Assert.assertEquals(ONE_DOUBLE,
                doublePropertyDefaultOne.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void doublePropertyDefaultOne_setNonDefault() {
        doublePropertyDefaultOne.set(component, SOME_DOUBLE_VALUE);
        Assert.assertEquals(SOME_DOUBLE_VALUE,
                doublePropertyDefaultOne.get(component));
        Assert.assertEquals(SOME_DOUBLE_VALUE, Double.valueOf(
                component.getElement().getProperty(TEST_PROPERTY, -1.0)));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void doublePropertyDefaultOne_resetToDefault() {
        doublePropertyDefaultOne.set(component, SOME_DOUBLE_VALUE);
        doublePropertyDefaultOne.set(component, ONE_DOUBLE);
        Assert.assertEquals(ONE_DOUBLE,
                doublePropertyDefaultOne.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void doublePropertyDefaultOne_setToNull() {
        doublePropertyDefaultOne.set(component, null);
    }

    @Test
    public void booleanPropertyDefaultFalse_initial() {
        Assert.assertEquals(false, booleanPropertyDefaultFalse.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void booleanPropertyDefaultFalse_setNonDefault() {
        booleanPropertyDefaultFalse.set(component, true);
        Assert.assertEquals(true, booleanPropertyDefaultFalse.get(component));
        Assert.assertEquals(true, Boolean.valueOf(component.getElement()
                .getProperty(TEST_PROPERTY, "neverused")));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void booleanPropertyDefaultFalse_resetToDefault() {
        booleanPropertyDefaultFalse.set(component, true);
        booleanPropertyDefaultFalse.set(component, false);
        Assert.assertEquals(false, booleanPropertyDefaultFalse.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void booleanPropertyDefaultFalse_setToNull() {
        booleanPropertyDefaultFalse.set(component, null);
    }

    @Test
    public void booleanPropertyDefaultTrue_initial() {
        Assert.assertEquals(true, booleanPropertyDefaultTrue.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void booleanPropertyDefaultTrue_setNonDefault() {
        booleanPropertyDefaultTrue.set(component, false);
        Assert.assertEquals(false, booleanPropertyDefaultTrue.get(component));
        Assert.assertEquals(false, Boolean.valueOf(component.getElement()
                .getProperty(TEST_PROPERTY, "neverused")));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test
    public void booleanPropertyDefaultTrue_resetToDefault() {
        booleanPropertyDefaultTrue.set(component, false);
        booleanPropertyDefaultTrue.set(component, true);
        Assert.assertEquals(true, booleanPropertyDefaultTrue.get(component));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void booleanPropertyDefaultTrue_setToNull() {
        booleanPropertyDefaultTrue.set(component, null);
    }

    @Test
    public void stringAttributeDefaultEmpty_initial() {
        Assert.assertEquals(EMPTY_STRING,
                stringAttributeDefaultEmpty.get(component));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeDefaultEmpty_setNonDefault() {
        stringAttributeDefaultEmpty.set(component, SOME_STRING_VALUE);
        Assert.assertEquals(SOME_STRING_VALUE,
                stringAttributeDefaultEmpty.get(component));
        Assert.assertEquals(SOME_STRING_VALUE,
                component.getElement().getAttribute(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeDefaultEmpty_resetToDefault() {
        stringAttributeDefaultEmpty.set(component, SOME_STRING_VALUE);
        stringAttributeDefaultEmpty.set(component, EMPTY_STRING);
        Assert.assertEquals(EMPTY_STRING,
                stringAttributeDefaultEmpty.get(component));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));

    }

    @Test(expected = IllegalArgumentException.class)
    public void stringAttributeDefaultEmpty_setToNull() {
        stringAttributeDefaultEmpty.set(component, null);
    }

    @Test
    public void stringAttributeDefaultFoo_initial() {
        Assert.assertEquals(FOO, stringAttributeDefaultFoo.get(component));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeDefaultFoo_setNonDefault() {
        stringAttributeDefaultFoo.set(component, SOME_STRING_VALUE);
        Assert.assertEquals(SOME_STRING_VALUE,
                stringAttributeDefaultFoo.get(component));
        Assert.assertEquals(SOME_STRING_VALUE,
                component.getElement().getAttribute(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeDefaultFoo_resetToDefault() {
        stringAttributeDefaultFoo.set(component, SOME_STRING_VALUE);
        stringAttributeDefaultFoo.set(component, FOO);
        Assert.assertEquals(FOO, stringAttributeDefaultFoo.get(component));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));

    }

    @Test(expected = IllegalArgumentException.class)
    public void stringAttributeDefaultFoo_setToNull() {
        stringAttributeDefaultFoo.set(component, null);
    }

    @Test
    public void stringAttributeOptionalDefaultEmpty_initial() {
        Assert.assertEquals(Optional.empty(),
                stringAttributeOptionalDefaultEmpty.get(component));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeOptionalDefaultEmpty_setNonOptionalDefault() {
        stringAttributeOptionalDefaultEmpty.set(component, SOME_STRING_VALUE);
        Assert.assertEquals(Optional.of(SOME_STRING_VALUE),
                stringAttributeOptionalDefaultEmpty.get(component));
        Assert.assertEquals(SOME_STRING_VALUE,
                component.getElement().getAttribute(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeOptionalDefaultEmpty_resetToOptionalDefault() {
        stringAttributeOptionalDefaultEmpty.set(component, SOME_STRING_VALUE);
        stringAttributeOptionalDefaultEmpty.set(component, EMPTY_STRING);
        Assert.assertEquals(Optional.empty(),
                stringAttributeOptionalDefaultEmpty.get(component));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));

    }

    @Test(expected = IllegalArgumentException.class)
    public void stringAttributeOptionalDefaultEmpty_setToNull() {
        stringAttributeOptionalDefaultEmpty.set(component, null);
    }

    @Test
    public void stringAttributeOptionalDefaultFoo_initial() {
        Assert.assertEquals(Optional.empty(),
                stringAttributeOptionalDefaultFoo.get(component));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeOptionalDefaultFoo_setNonOptionalDefault() {
        stringAttributeOptionalDefaultFoo.set(component, SOME_STRING_VALUE);
        Assert.assertEquals(Optional.of(SOME_STRING_VALUE),
                stringAttributeOptionalDefaultFoo.get(component));
        Assert.assertEquals(SOME_STRING_VALUE,
                component.getElement().getAttribute(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));
    }

    @Test
    public void stringAttributeOptionalDefaultFoo_resetToOptionalDefault() {
        stringAttributeOptionalDefaultFoo.set(component, SOME_STRING_VALUE);
        stringAttributeOptionalDefaultFoo.set(component, FOO);
        Assert.assertEquals(Optional.empty(),
                stringAttributeOptionalDefaultFoo.get(component));
        Assert.assertFalse(component.getElement().hasAttribute(TEST_PROPERTY));
        Assert.assertFalse(component.getElement().hasProperty(TEST_PROPERTY));

    }

    @Test(expected = IllegalArgumentException.class)
    public void stringAttributeOptionalDefaultFoo_setToNull() {
        stringAttributeOptionalDefaultFoo.set(component, null);
    }

    @Test
    public void propertyName() {
        Assert.assertEquals(TEST_PROPERTY,
                stringAttributeDefaultEmpty.getPropertyName());
    }
}
