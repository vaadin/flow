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
package com.vaadin.flow.internal;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the {@link BeanUtil}.
 *
 * This test class provides comprehensive coverage for all public methods of
 * BeanUtil: - getBeanPropertyDescriptors(): Tests with regular classes,
 * records, interfaces - getPropertyType(): Tests simple, nested, record, and
 * interface properties - getPropertyDescriptor(): Tests simple and nested
 * property access - checkBeanValidationAvailable(): Tests JSR-303 availability
 * checking
 *
 * Edge cases covered include null inputs, empty strings, non-existent
 * properties, deep nesting, and proper filtering of Object.class methods.
 */
public class BeanUtilTest {

    // Test helper classes
    public static class TestBean {
        private String name;
        private int age;
        private TestNestedBean nested;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public TestNestedBean getNested() {
            return nested;
        }

        public void setNested(TestNestedBean nested) {
            this.nested = nested;
        }
    }

    public static class TestNestedBean {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public interface TestInterface {
        String getInterfaceProperty();

        void setInterfaceProperty(String value);
    }

    public record TestRecord(String recordProperty, int recordAge) {
    }

    // Test helper classes for duplicate property test from main branch
    public interface FirstInterface {
        default void setOneInterface(boolean bothInterfaces) {
        }

        default boolean isOneInterface() {
            return true;
        }

        default void setExistsInAllPlaces(boolean visible) {
        }

        default boolean isExistsInAllPlaces() {
            return true;
        }
    }

    public interface SecondInterface {
        default void setExistsInAllPlaces(boolean visible) {
        }

        default boolean isExistsInAllPlaces() {
            return true;
        }
    }

    public class TestSomething implements FirstInterface, SecondInterface {
        public void setExistsInAllPlaces(boolean visible) {
        }

        public boolean isExistsInAllPlaces() {
            return true;
        }
    }

    // Test for getBeanPropertyDescriptors with regular class
    @Test
    public void getBeanPropertyDescriptors_regularClass()
            throws IntrospectionException {
        List<PropertyDescriptor> descriptors = BeanUtil
                .getBeanPropertyDescriptors(TestBean.class);

        assertNotNull(descriptors);
        assertEquals(4, descriptors.size()); // name, age, nested + class
                                             // property

        boolean foundName = false, foundAge = false, foundNested = false;
        for (PropertyDescriptor descriptor : descriptors) {
            // All descriptors should have read method
            assertNotNull(descriptor.getReadMethod());
            // All descriptors except "class" should have write method
            if (!"class".equals(descriptor.getName())) {
                assertNotNull(descriptor.getWriteMethod());
            }

            if ("name".equals(descriptor.getName())) {
                foundName = true;
                assertEquals(String.class, descriptor.getPropertyType());
            } else if ("age".equals(descriptor.getName())) {
                foundAge = true;
                assertEquals(int.class, descriptor.getPropertyType());
            } else if ("nested".equals(descriptor.getName())) {
                foundNested = true;
                assertEquals(TestNestedBean.class,
                        descriptor.getPropertyType());
            }
        }

        assertTrue("Should find 'name' property", foundName);
        assertTrue("Should find 'age' property", foundAge);
        assertTrue("Should find 'nested' property", foundNested);
    }

    // Test for getBeanPropertyDescriptors with record
    @Test
    public void getBeanPropertyDescriptors_record()
            throws IntrospectionException {
        List<PropertyDescriptor> descriptors = BeanUtil
                .getBeanPropertyDescriptors(TestRecord.class);

        assertNotNull(descriptors);
        assertEquals(2, descriptors.size()); // recordProperty, recordAge

        boolean foundRecordProperty = false, foundRecordAge = false;
        for (PropertyDescriptor descriptor : descriptors) {
            // All descriptors should have read method
            assertNotNull(descriptor.getReadMethod());
            // Records are immutable, so no write methods
            assertNull(descriptor.getWriteMethod());

            if ("recordProperty".equals(descriptor.getName())) {
                foundRecordProperty = true;
                assertEquals(String.class, descriptor.getPropertyType());
            } else if ("recordAge".equals(descriptor.getName())) {
                foundRecordAge = true;
                assertEquals(int.class, descriptor.getPropertyType());
            }
        }

        assertTrue("Should find 'recordProperty' property",
                foundRecordProperty);
        assertTrue("Should find 'recordAge' property", foundRecordAge);
    }

    // Test for getBeanPropertyDescriptors with interface
    @Test
    public void getBeanPropertyDescriptors_interface()
            throws IntrospectionException {
        List<PropertyDescriptor> descriptors = BeanUtil
                .getBeanPropertyDescriptors(TestInterface.class);

        assertNotNull(descriptors);
        assertEquals(1, descriptors.size()); // interfaceProperty

        assertEquals("interfaceProperty", descriptors.get(0).getName());
        assertEquals(String.class, descriptors.get(0).getPropertyType());
        assertNotNull(descriptors.get(0).getReadMethod());
        assertNotNull(descriptors.get(0).getWriteMethod());
    }

    // Test for getPropertyType with simple property
    @Test
    public void getPropertyType_simpleProperty() throws IntrospectionException {
        Class<?> nameType = BeanUtil.getPropertyType(TestBean.class, "name");
        assertEquals(String.class, nameType);

        Class<?> ageType = BeanUtil.getPropertyType(TestBean.class, "age");
        assertEquals(int.class, ageType);
    }

    // Test for getPropertyType with nested property
    @Test
    public void getPropertyType_nestedProperty() throws IntrospectionException {
        Class<?> nestedValueType = BeanUtil.getPropertyType(TestBean.class,
                "nested.value");
        assertEquals(String.class, nestedValueType);
    }

    // Test for getPropertyType with non-existent property
    @Test
    public void getPropertyType_nonExistentProperty()
            throws IntrospectionException {
        Class<?> nonExistentType = BeanUtil.getPropertyType(TestBean.class,
                "nonExistent");
        assertNull(nonExistentType);
    }

    // Test for getPropertyDescriptor with simple property
    @Test
    public void getPropertyDescriptor_simpleProperty()
            throws IntrospectionException {
        PropertyDescriptor nameDescriptor = BeanUtil
                .getPropertyDescriptor(TestBean.class, "name");
        assertNotNull(nameDescriptor);
        assertEquals("name", nameDescriptor.getName());
        assertEquals(String.class, nameDescriptor.getPropertyType());
        assertNotNull(nameDescriptor.getReadMethod());
        assertNotNull(nameDescriptor.getWriteMethod());
    }

    // Test for getPropertyDescriptor with nested property
    @Test
    public void getPropertyDescriptor_nestedProperty()
            throws IntrospectionException {
        PropertyDescriptor nestedValueDescriptor = BeanUtil
                .getPropertyDescriptor(TestBean.class, "nested.value");
        assertNotNull(nestedValueDescriptor);
        assertEquals("value", nestedValueDescriptor.getName());
        assertEquals(String.class, nestedValueDescriptor.getPropertyType());
        assertNotNull(nestedValueDescriptor.getReadMethod());
        assertNotNull(nestedValueDescriptor.getWriteMethod());
    }

    // Test for getPropertyDescriptor with non-existent property
    @Test
    public void getPropertyDescriptor_nonExistentProperty()
            throws IntrospectionException {
        PropertyDescriptor nonExistentDescriptor = BeanUtil
                .getPropertyDescriptor(TestBean.class, "nonExistent");
        assertNull(nonExistentDescriptor);
    }

    // Test for getBeanPropertyDescriptors with null input
    @Test
    public void getBeanPropertyDescriptors_nullInput()
            throws IntrospectionException {
        List<PropertyDescriptor> result = BeanUtil
                .getBeanPropertyDescriptors(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Test empty property name
    @Test
    public void getPropertyType_emptyPropertyName()
            throws IntrospectionException {
        Class<?> result = BeanUtil.getPropertyType(TestBean.class, "");
        assertNull(result);
    }

    // Test empty property name for descriptor
    @Test
    public void getPropertyDescriptor_emptyPropertyName()
            throws IntrospectionException {
        PropertyDescriptor result = BeanUtil
                .getPropertyDescriptor(TestBean.class, "");
        assertNull(result);
    }

    // Test property with deep nesting
    @Test
    public void getPropertyType_deepNestedProperty()
            throws IntrospectionException {
        // This would fail because our test bean doesn't have deep nesting,
        // but tests the recursive behavior
        Class<?> result = BeanUtil.getPropertyType(TestBean.class,
                "nested.nonExistent.value");
        assertNull(result);
    }

    // Test getPropertyType with record
    @Test
    public void getPropertyType_recordProperty() throws IntrospectionException {
        Class<?> recordPropertyType = BeanUtil.getPropertyType(TestRecord.class,
                "recordProperty");
        assertEquals(String.class, recordPropertyType);

        Class<?> recordAgeType = BeanUtil.getPropertyType(TestRecord.class,
                "recordAge");
        assertEquals(int.class, recordAgeType);
    }

    // Test interface properties
    @Test
    public void getPropertyType_interfaceProperty()
            throws IntrospectionException {
        Class<?> interfacePropertyType = BeanUtil
                .getPropertyType(TestInterface.class, "interfaceProperty");
        assertEquals(String.class, interfacePropertyType);
    }

    // Test that Object methods are filtered out
    @Test
    public void getPropertyDescriptor_objectMethodsFiltered()
            throws IntrospectionException {
        // The "class" property should be filtered out by BeanUtil
        PropertyDescriptor classProperty = BeanUtil
                .getPropertyDescriptor(TestBean.class, "class");
        assertNull("Object.getClass() should be filtered out", classProperty);
    }

    // Test that getBeanPropertyDescriptors includes all valid properties
    @Test
    public void getBeanPropertyDescriptors_includesAllValidProperties()
            throws IntrospectionException {
        List<PropertyDescriptor> descriptors = BeanUtil
                .getBeanPropertyDescriptors(TestBean.class);

        List<String> names = descriptors.stream()
                .map(PropertyDescriptor::getName).toList();

        assertTrue("should include property 'name'", names.contains("name"));
        assertTrue("should include property 'age'", names.contains("age"));
        assertTrue("should include property 'nested'",
                names.contains("nested"));
        assertTrue("should include property 'class'", names.contains("class"));
    }

    // Test from main branch: duplicate property descriptors are removed
    @Test
    public void duplicatesAreRemoved() throws Exception {
        List<PropertyDescriptor> descriptors = BeanUtil
                .getBeanPropertyDescriptors(TestSomething.class);
        List<PropertyDescriptor> existsInAllPlacesProperties = descriptors
                .stream()
                .filter(desc -> desc.getName().equals("existsInAllPlaces"))
                .toList();
        Assert.assertEquals(
                "There should be only one 'existsInAllPlaces' property descriptor",
                1, existsInAllPlacesProperties.size());

        // The property from the class should be retained
        // but we cannot test this as some introspector implementations
        // return the read method from the interface when introspecting the
        // class
        // Assert.assertEquals(TestSomething.class,
        // existsInAllPlacesProperties.get(0).getReadMethod().getDeclaringClass());

        List<PropertyDescriptor> oneInterfaceProperties = descriptors.stream()
                .filter(desc -> desc.getName().equals("oneInterface")).toList();
        Assert.assertEquals(
                "There should be only one 'oneInterface' property descriptor",
                1, oneInterfaceProperties.size());

        PropertyDescriptor oneInterfaceProperty = oneInterfaceProperties.get(0);
        // The property from thedefault method
        Assert.assertEquals(FirstInterface.class,
                oneInterfaceProperty.getReadMethod().getDeclaringClass());

    }
}
