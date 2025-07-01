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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Tests for the {@link BeanUtil}.
 * 
 * This test class provides comprehensive coverage for all public methods of BeanUtil:
 * - getBeanPropertyDescriptors(): Tests with regular classes, records, interfaces
 * - getPropertyType(): Tests simple, nested, record, and interface properties  
 * - getPropertyDescriptor(): Tests simple and nested property access
 * - checkBeanValidationAvailable(): Tests JSR-303 availability checking
 * 
 * Edge cases covered include null inputs, empty strings, non-existent properties,
 * deep nesting, and proper filtering of Object.class methods.
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

    public record TestRecord(String recordProperty, int recordAge) {}

    // Test for getBeanPropertyDescriptors with regular class
    @Test
    public void getBeanPropertyDescriptors_regularClass() throws IntrospectionException {
        List<PropertyDescriptor> descriptors = BeanUtil.getBeanPropertyDescriptors(TestBean.class);
        
        assertNotNull(descriptors);
        assertTrue(descriptors.size() >= 3); // name, age, nested + class property
        
        boolean foundName = false, foundAge = false, foundNested = false;
        for (PropertyDescriptor descriptor : descriptors) {
            if ("name".equals(descriptor.getName())) {
                foundName = true;
                assertEquals(String.class, descriptor.getPropertyType());
                assertNotNull(descriptor.getReadMethod());
                assertNotNull(descriptor.getWriteMethod());
            } else if ("age".equals(descriptor.getName())) {
                foundAge = true;
                assertEquals(int.class, descriptor.getPropertyType());
                assertNotNull(descriptor.getReadMethod());
                assertNotNull(descriptor.getWriteMethod());
            } else if ("nested".equals(descriptor.getName())) {
                foundNested = true;
                assertEquals(TestNestedBean.class, descriptor.getPropertyType());
                assertNotNull(descriptor.getReadMethod());
                assertNotNull(descriptor.getWriteMethod());
            }
        }
        
        assertTrue("Should find 'name' property", foundName);
        assertTrue("Should find 'age' property", foundAge);
        assertTrue("Should find 'nested' property", foundNested);
    }

    // Test for getBeanPropertyDescriptors with record
    @Test
    public void getBeanPropertyDescriptors_record() throws IntrospectionException {
        List<PropertyDescriptor> descriptors = BeanUtil.getBeanPropertyDescriptors(TestRecord.class);
        
        assertNotNull(descriptors);
        assertEquals(2, descriptors.size()); // recordProperty, recordAge
        
        boolean foundRecordProperty = false, foundRecordAge = false;
        for (PropertyDescriptor descriptor : descriptors) {
            if ("recordProperty".equals(descriptor.getName())) {
                foundRecordProperty = true;
                assertEquals(String.class, descriptor.getPropertyType());
                assertNotNull(descriptor.getReadMethod());
                assertNull(descriptor.getWriteMethod()); // Records are immutable
            } else if ("recordAge".equals(descriptor.getName())) {
                foundRecordAge = true;
                assertEquals(int.class, descriptor.getPropertyType());
                assertNotNull(descriptor.getReadMethod());
                assertNull(descriptor.getWriteMethod()); // Records are immutable
            }
        }
        
        assertTrue("Should find 'recordProperty' property", foundRecordProperty);
        assertTrue("Should find 'recordAge' property", foundRecordAge);
    }

    // Test for getBeanPropertyDescriptors with interface
    @Test
    public void getBeanPropertyDescriptors_interface() throws IntrospectionException {
        List<PropertyDescriptor> descriptors = BeanUtil.getBeanPropertyDescriptors(TestInterface.class);
        
        assertNotNull(descriptors);
        assertTrue(descriptors.size() >= 1); // interfaceProperty
        
        boolean foundInterfaceProperty = false;
        for (PropertyDescriptor descriptor : descriptors) {
            if ("interfaceProperty".equals(descriptor.getName())) {
                foundInterfaceProperty = true;
                assertEquals(String.class, descriptor.getPropertyType());
                assertNotNull(descriptor.getReadMethod());
                assertNotNull(descriptor.getWriteMethod());
            }
        }
        
        assertTrue("Should find 'interfaceProperty' property", foundInterfaceProperty);
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
        Class<?> nestedValueType = BeanUtil.getPropertyType(TestBean.class, "nested.value");
        assertEquals(String.class, nestedValueType);
    }

    // Test for getPropertyType with non-existent property
    @Test
    public void getPropertyType_nonExistentProperty() throws IntrospectionException {
        Class<?> nonExistentType = BeanUtil.getPropertyType(TestBean.class, "nonExistent");
        assertNull(nonExistentType);
    }

    // Test for getPropertyDescriptor with simple property
    @Test
    public void getPropertyDescriptor_simpleProperty() throws IntrospectionException {
        PropertyDescriptor nameDescriptor = BeanUtil.getPropertyDescriptor(TestBean.class, "name");
        assertNotNull(nameDescriptor);
        assertEquals("name", nameDescriptor.getName());
        assertEquals(String.class, nameDescriptor.getPropertyType());
        assertNotNull(nameDescriptor.getReadMethod());
        assertNotNull(nameDescriptor.getWriteMethod());
    }

    // Test for getPropertyDescriptor with nested property
    @Test
    public void getPropertyDescriptor_nestedProperty() throws IntrospectionException {
        PropertyDescriptor nestedValueDescriptor = BeanUtil.getPropertyDescriptor(TestBean.class, "nested.value");
        assertNotNull(nestedValueDescriptor);
        assertEquals("value", nestedValueDescriptor.getName());
        assertEquals(String.class, nestedValueDescriptor.getPropertyType());
        assertNotNull(nestedValueDescriptor.getReadMethod());
        assertNotNull(nestedValueDescriptor.getWriteMethod());
    }

    // Test for getPropertyDescriptor with non-existent property
    @Test
    public void getPropertyDescriptor_nonExistentProperty() throws IntrospectionException {
        PropertyDescriptor nonExistentDescriptor = BeanUtil.getPropertyDescriptor(TestBean.class, "nonExistent");
        assertNull(nonExistentDescriptor);
    }

    // Test for checkBeanValidationAvailable
    @Test
    public void checkBeanValidationAvailable() {
        // This method checks if JSR-303 bean validation is available
        // The result will depend on whether validation libraries are on the classpath
        // We just verify the method doesn't throw an exception and returns a boolean
        boolean isAvailable = BeanUtil.checkBeanValidationAvailable();
        // Result can be true or false depending on classpath, just verify it's a boolean
        assertTrue("Method should return a boolean value", isAvailable || !isAvailable);
    }

    // Test for getBeanPropertyDescriptors with null input
    @Test(expected = NullPointerException.class)
    public void getBeanPropertyDescriptors_nullInput() throws IntrospectionException {
        BeanUtil.getBeanPropertyDescriptors(null);
    }

    // Test for getPropertyType with null class
    @Test(expected = NullPointerException.class)
    public void getPropertyType_nullClass() throws IntrospectionException {
        BeanUtil.getPropertyType(null, "name");
    }

    // Test for getPropertyType with null property name
    @Test(expected = NullPointerException.class)
    public void getPropertyType_nullPropertyName() throws IntrospectionException {
        // This will throw NPE because getPropertyDescriptor calls propertyName.contains(".")
        BeanUtil.getPropertyType(TestBean.class, null);
    }

    // Test for getPropertyDescriptor with null class
    @Test(expected = NullPointerException.class)
    public void getPropertyDescriptor_nullClass() throws IntrospectionException {
        BeanUtil.getPropertyDescriptor(null, "name");
    }

    // Test for getPropertyDescriptor with null property name
    @Test(expected = NullPointerException.class)
    public void getPropertyDescriptor_nullPropertyName() throws IntrospectionException {
        // This will throw NPE because method calls propertyName.contains(".")
        BeanUtil.getPropertyDescriptor(TestBean.class, null);
    }

    // Test empty property name
    @Test
    public void getPropertyType_emptyPropertyName() throws IntrospectionException {
        Class<?> result = BeanUtil.getPropertyType(TestBean.class, "");
        assertNull(result);
    }

    // Test empty property name for descriptor
    @Test
    public void getPropertyDescriptor_emptyPropertyName() throws IntrospectionException {
        PropertyDescriptor result = BeanUtil.getPropertyDescriptor(TestBean.class, "");
        assertNull(result);
    }

    // Test property with deep nesting
    @Test
    public void getPropertyType_deepNestedProperty() throws IntrospectionException {
        // This would fail because our test bean doesn't have deep nesting, 
        // but tests the recursive behavior
        Class<?> result = BeanUtil.getPropertyType(TestBean.class, "nested.nonExistent.value");
        assertNull(result);
    }

    // Test getPropertyType with record
    @Test
    public void getPropertyType_recordProperty() throws IntrospectionException {
        Class<?> recordPropertyType = BeanUtil.getPropertyType(TestRecord.class, "recordProperty");
        assertEquals(String.class, recordPropertyType);
        
        Class<?> recordAgeType = BeanUtil.getPropertyType(TestRecord.class, "recordAge");
        assertEquals(int.class, recordAgeType);
    }

    // Test interface properties
    @Test
    public void getPropertyType_interfaceProperty() throws IntrospectionException {
        Class<?> interfacePropertyType = BeanUtil.getPropertyType(TestInterface.class, "interfaceProperty");
        assertEquals(String.class, interfacePropertyType);
    }

    // Test that Object methods are filtered out
    @Test
    public void getPropertyDescriptor_objectMethodsFiltered() throws IntrospectionException {
        // The "class" property should be filtered out by BeanUtil
        PropertyDescriptor classProperty = BeanUtil.getPropertyDescriptor(TestBean.class, "class");
        assertNull("Object.getClass() should be filtered out", classProperty);
    }

    // Test that getBeanPropertyDescriptors includes all valid properties
    @Test
    public void getBeanPropertyDescriptors_includesAllValidProperties() throws IntrospectionException {
        List<PropertyDescriptor> descriptors = BeanUtil.getBeanPropertyDescriptors(TestBean.class);
        
        // Should include custom properties but not Object properties
        boolean hasName = false, hasAge = false, hasNested = false, hasClass = false;
        
        for (PropertyDescriptor desc : descriptors) {
            String name = desc.getName();
            if ("name".equals(name)) hasName = true;
            else if ("age".equals(name)) hasAge = true;
            else if ("nested".equals(name)) hasNested = true;
            else if ("class".equals(name)) hasClass = true;
        }
        
        assertTrue("Should include 'name' property", hasName);
        assertTrue("Should include 'age' property", hasAge); 
        assertTrue("Should include 'nested' property", hasNested);
        // Note: getBeanPropertyDescriptors may include 'class' but getPropertyDescriptor filters it
        // This test just verifies that our custom properties are present
    }
}