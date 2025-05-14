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
package com.vaadin.flow.data.binder;

import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import junit.framework.AssertionFailedError;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.internal.BeanUtil;
import com.vaadin.flow.tests.data.bean.Address;
import com.vaadin.flow.tests.data.bean.Country;
import com.vaadin.flow.tests.data.bean.FatherAndSon;
import com.vaadin.flow.tests.data.bean.Sex;
import com.vaadin.flow.tests.server.ClassesSerializableUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class BeanPropertySetTest {

    public static class Person implements Serializable {
        private String name;
        private final int born;

        public Person(String name, int born) {
            this.name = name;
            this.born = born;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getBorn() {
            return born;
        }

        @Override
        public String toString() {
            return name + "(" + born + ")";
        }
    }

    public record TestRecord(String name, int age) {
    }

    interface Iface3 extends Iface2, Iface {
    }

    interface Iface2 extends Iface {
    }

    interface Iface {
        String getName();
    }

    interface DefaultMethodIface {
        default String getName() {
            return "FIXED";
        }
    }

    interface GenericIface<TYPE> {
        TYPE getProperty();

        GenericIface<TYPE> getWrappedProperty();
    }

    interface DefaultMethodSubclassIface extends GenericIface<String> {
        String getName();

        @Override
        default String getProperty() {
            return this.getName();
        }

        @Override
        default GenericIface<String> getWrappedProperty() {
            return null;
        }
    }

    @Test
    public void testSerializeDeserialize_propertySet() throws Exception {
        PropertySet<Person> originalPropertySet = BeanPropertySet
                .get(Person.class);

        PropertySet<Person> deserializedPropertySet = ClassesSerializableUtils
                .serializeAndDeserialize(originalPropertySet);

        comparePropertySet(originalPropertySet, deserializedPropertySet,
                "Deserialized instance should be the same as the original");
    }

    private void comparePropertySet(PropertySet<?> propertySetA,
            PropertySet<?> propertySetB, String message) {

        PropertyDefinition<?, ?>[] propertiesA = propertySetA.getProperties()
                .sorted(Comparator.comparing(PropertyDefinition::getName))
                .toArray(PropertyDefinition<?, ?>[]::new);
        PropertyDefinition<?, ?>[] propertiesB = propertySetA.getProperties()
                .sorted(Comparator.comparing(PropertyDefinition::getName))
                .toArray(PropertyDefinition<?, ?>[]::new);

        assertEquals(message, propertiesA.length, propertiesB.length);
        for (int i = 0; i < propertiesB.length; i++) {
            assertSame(message, propertiesA[i], propertiesB[i]);
        }
    }

    @Test
    public void testSerializeDeserialize_propertySet_cacheCleared()
            throws Exception {
        PropertySet<Person> originalPropertySet = BeanPropertySet
                .get(Person.class);

        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bs);
        out.writeObject(originalPropertySet);
        byte[] data = bs.toByteArray();

        // Simulate deserializing into a different JVM by clearing the instance
        // map
        Field instancesField = BeanPropertySet.class
                .getDeclaredField("INSTANCES");
        instancesField.setAccessible(true);
        Map<?, ?> instances = (Map<?, ?>) instancesField.get(null);
        instances.clear();

        ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(data));
        PropertySet<Person> deserializedPropertySet = (PropertySet<Person>) in
                .readObject();

        comparePropertySet(BeanPropertySet.get(Person.class),
                deserializedPropertySet,
                "Deserialized instance should be the same as in the cache");

        Assert.assertNotSame(
                "Deserialized instance should not be the same as the original",
                originalPropertySet, deserializedPropertySet);
    }

    @Test
    public void testSerializeDeserialize_propertyDefinition() throws Exception {
        PropertyDefinition<Person, ?> definition = BeanPropertySet
                .get(Person.class).getProperty("born")
                .orElseThrow(RuntimeException::new);

        PropertyDefinition<Person, ?> deserializedDefinition = ClassesSerializableUtils
                .serializeAndDeserialize(definition);

        ValueProvider<Person, ?> getter = deserializedDefinition.getGetter();
        Person person = new Person("Milennial", 2000);
        Integer age = (Integer) getter.apply(person);

        Assert.assertEquals("Deserialized definition should be functional",
                Integer.valueOf(2000), age);

        Assert.assertSame(
                "Deserialized instance should be the same as in the cache",
                BeanPropertySet.get(Person.class).getProperty("born")
                        .orElseThrow(RuntimeException::new),
                deserializedDefinition);
    }

    @Test
    public void testSerializeDeserialize_nestedPropertyDefinition()
            throws Exception {
        PropertyDefinition<com.vaadin.flow.tests.data.bean.Person, ?> definition = BeanPropertySet
                .get(com.vaadin.flow.tests.data.bean.Person.class, true,
                        PropertyFilterDefinition.getDefaultFilter())
                .getProperty("address.postalCode")
                .orElseThrow(AssertionFailedError::new);

        PropertyDefinition<com.vaadin.flow.tests.data.bean.Person, ?> deserializedDefinition = ClassesSerializableUtils
                .serializeAndDeserialize(definition);

        ValueProvider<com.vaadin.flow.tests.data.bean.Person, ?> getter = deserializedDefinition
                .getGetter();
        Address address = new Address("Ruukinkatu 2-4", 20540, "Turku",
                Country.FINLAND);
        com.vaadin.flow.tests.data.bean.Person person = new com.vaadin.flow.tests.data.bean.Person(
                "Jon", "Doe", "jon.doe@vaadin.com", 32, Sex.MALE, address);

        Integer postalCode = (Integer) getter.apply(person);

        Assert.assertEquals("Deserialized definition should be functional",
                address.getPostalCode(), postalCode);
    }

    @Test
    public void testSerializeDeserializeRecord() throws Exception {
        PropertyDefinition<TestRecord, ?> definition = BeanPropertySet
                .get(TestRecord.class).getProperty("name")
                .orElseThrow(AssertionFailedError::new);

        PropertyDefinition<TestRecord, ?> deserializedDefinition = ClassesSerializableUtils
                .serializeAndDeserialize(definition);

        ValueProvider<TestRecord, ?> getter = deserializedDefinition
                .getGetter();

        TestRecord testRecord = new TestRecord("someone", 42);

        String name = (String) getter.apply(testRecord);

        Assert.assertEquals("Deserialized definition should be functional",
                "someone", name);

        PropertyDescriptor namePropertyDescriptor = BeanUtil
                .getPropertyDescriptor(TestRecord.class, "name");
        Assert.assertNotNull(namePropertyDescriptor);
        Assert.assertEquals("Property has unexpected name",
                namePropertyDescriptor.getName(), "name");
        Assert.assertEquals("Property read method has unexpected name",
                namePropertyDescriptor.getReadMethod().getName(), "name");

        Class<?> namePropertyType = BeanUtil.getPropertyType(TestRecord.class,
                "name");
        Assert.assertEquals("Property type is unexpected", namePropertyType,
                String.class);

        // Ensure props for Record are not sorted, but are in code order
        List<PropertyDefinition<TestRecord, ?>> propertyList = definition
                .getPropertySet().getProperties().toList();
        Assert.assertEquals("name", propertyList.get(0).getName());
        Assert.assertEquals("age", propertyList.get(1).getName());
    }

    @Test
    public void nestedPropertyDefinition_samePropertyNameOnMultipleLevels() {
        PropertyDefinition<FatherAndSon, ?> definition = BeanPropertySet
                .get(FatherAndSon.class).getProperty("father.father.firstName")
                .orElseThrow(RuntimeException::new);

        ValueProvider<FatherAndSon, ?> getter = definition.getGetter();

        FatherAndSon grandFather = new FatherAndSon("Grand Old Jon", "Doe",
                null, null);
        FatherAndSon father = new FatherAndSon("Old Jon", "Doe", grandFather,
                null);
        FatherAndSon son = new FatherAndSon("Jon", "Doe", father, null);

        String firstName = (String) getter.apply(son);

        Assert.assertEquals(grandFather.getFirstName(), firstName);
    }

    @Test(expected = NullPointerException.class)
    public void nestedPropertyDefinition_propertyChainBroken() {
        PropertyDefinition<FatherAndSon, ?> definition = BeanPropertySet
                .get(FatherAndSon.class).getProperty("father.firstName")
                .orElseThrow(RuntimeException::new);

        ValueProvider<FatherAndSon, ?> getter = definition.getGetter();

        getter.apply(new FatherAndSon("Jon", "Doe", null, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nestedPropertyDefinition_invalidPropertyNameInChain() {
        BeanPropertySet.get(FatherAndSon.class)
                .getProperty("grandfather.firstName");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nestedPropertyDefinition_invalidPropertyNameAtChainEnd() {
        BeanPropertySet.get(FatherAndSon.class).getProperty("father.age");
    }

    @Test
    public void properties() {
        PropertySet<Person> propertySet = BeanPropertySet.get(Person.class);

        Set<String> propertyNames = propertySet.getProperties()
                .map(PropertyDefinition::getName).collect(Collectors.toSet());

        Assert.assertEquals(new HashSet<>(Arrays.asList("name", "born")),
                propertyNames);
    }

    @Test
    public void isSubProperty() {
        PropertySet<FatherAndSon> propertySet = BeanPropertySet
                .get(FatherAndSon.class);

        Assert.assertTrue(
                "Dot-separated property chain \"father.firstName\" should refer to a sub-property",
                propertySet.getProperty("father.firstName").get()
                        .isSubProperty());
        Assert.assertFalse(
                "Property name without dot-separated parent properties should not refer to a sub-property",
                propertySet.getProperty("father").get().isSubProperty());
        Assert.assertTrue(
                "Dot-separated property chain \"father.son.father.son\" should refer to a sub-property",
                propertySet.getProperty("father.son.father.son").get()
                        .isSubProperty());
    }

    @Test
    public void getFullName_returnsFullPropertyChain() {
        PropertySet<FatherAndSon> propertySet = BeanPropertySet
                .get(FatherAndSon.class);
        String subPropertyFullName = "father.son.father.son.firstName";
        PropertyDefinition<FatherAndSon, ?> subProperty = propertySet
                .getProperty(subPropertyFullName).get();
        Assert.assertEquals(
                "Name of a sub-property should be the simple name of the property",
                "firstName", subProperty.getTopLevelName());
        Assert.assertEquals(
                "Full name of a sub-property should be the full property chain with parent properties",
                subPropertyFullName, subProperty.getName());
    }

    @Test
    public void getParentForDirectProperty_returnsNull() {
        PropertySet<FatherAndSon> propertySet = BeanPropertySet
                .get(FatherAndSon.class);
        Assert.assertNull(
                "Direct property of a property set should not have a parent",
                propertySet.getProperty("father").get().getParent());
    }

    @Test
    public void getParentForSubProperty_returnsParent() {
        PropertySet<FatherAndSon> propertySet = BeanPropertySet
                .get(FatherAndSon.class);
        Assert.assertEquals(
                "Parent property of \"father.son.father\" should be \"father.son\"",
                "father.son", propertySet.getProperty("father.son.father").get()
                        .getParent().getName());
    }

    @Test
    public void get_beanImplementsSameInterfaceSeveralTimes_interfacePropertyIsNotDuplicated() {
        PropertySet<Iface3> set = BeanPropertySet.get(Iface3.class, false,
                PropertyFilterDefinition.getDefaultFilter());

        List<PropertyDefinition<Iface3, ?>> defs = set.getProperties()
                .collect(Collectors.toList());

        Assert.assertEquals(1, defs.size());
        Assert.assertEquals("name", defs.get(0).getName());
    }

    @Test
    public void get_beanImplementsInterfaceWithDefaultMethod_propertyFound() {
        PropertySet<DefaultMethodIface> set = BeanPropertySet
                .get(DefaultMethodIface.class);

        List<PropertyDefinition<DefaultMethodIface, ?>> defs = set
                .getProperties().collect(Collectors.toList());

        Assert.assertEquals(1, defs.size());
        Assert.assertEquals("name", defs.get(0).getName());
    }

    @Test
    public void get_beanImplementsGenericInterfaceSubclassWithDefaultMethod_interfacePropertyIsNotDuplicated() {
        PropertySet<DefaultMethodSubclassIface> set = BeanPropertySet
                .get(DefaultMethodSubclassIface.class);

        List<PropertyDefinition<DefaultMethodSubclassIface, ?>> defs = set
                .getProperties().collect(Collectors.toList());

        Assert.assertEquals(3, defs.size());
        Assert.assertEquals("name", defs.get(0).getName());
        Assert.assertEquals(String.class, defs.get(0).getType());
        Assert.assertEquals("property", defs.get(1).getName());
        Assert.assertEquals(String.class, defs.get(1).getType());
        Assert.assertEquals("wrappedProperty", defs.get(2).getName());
        Assert.assertEquals(GenericIface.class, defs.get(2).getType());
    }

}
