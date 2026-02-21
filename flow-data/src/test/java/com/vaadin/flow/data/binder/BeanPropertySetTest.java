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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.internal.BeanUtil;
import com.vaadin.flow.tests.data.bean.Address;
import com.vaadin.flow.tests.data.bean.Country;
import com.vaadin.flow.tests.data.bean.FatherAndSon;
import com.vaadin.flow.tests.data.bean.Sex;
import com.vaadin.flow.tests.server.ClassesSerializableUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanPropertySetTest {

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
    void testSerializeDeserialize_propertySet() throws Exception {
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

        assertEquals(propertiesA.length, propertiesB.length, message);
        for (int i = 0; i < propertiesB.length; i++) {
            assertSame(propertiesA[i], propertiesB[i], message);
        }
    }

    @Test
    void testSerializeDeserialize_propertySet_cacheCleared() throws Exception {
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

        assertNotSame(originalPropertySet, deserializedPropertySet,
                "Deserialized instance should not be the same as the original");
    }

    @Test
    void testSerializeDeserialize_propertyDefinition() throws Exception {
        PropertyDefinition<Person, ?> definition = BeanPropertySet
                .get(Person.class).getProperty("born")
                .orElseThrow(RuntimeException::new);

        PropertyDefinition<Person, ?> deserializedDefinition = ClassesSerializableUtils
                .serializeAndDeserialize(definition);

        ValueProvider<Person, ?> getter = deserializedDefinition.getGetter();
        Person person = new Person("Milennial", 2000);
        Integer age = (Integer) getter.apply(person);

        assertEquals(Integer.valueOf(2000), age,
                "Deserialized definition should be functional");

        assertSame(
                BeanPropertySet.get(Person.class).getProperty("born")
                        .orElseThrow(RuntimeException::new),
                deserializedDefinition,
                "Deserialized instance should be the same as in the cache");
    }

    @Test
    void testSerializeDeserialize_nestedPropertyDefinition() throws Exception {
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

        assertEquals(address.getPostalCode(), postalCode,
                "Deserialized definition should be functional");
    }

    @Test
    void testSerializeDeserializeRecord() throws Exception {
        PropertyDefinition<TestRecord, ?> definition = BeanPropertySet
                .get(TestRecord.class).getProperty("name")
                .orElseThrow(AssertionFailedError::new);

        PropertyDefinition<TestRecord, ?> deserializedDefinition = ClassesSerializableUtils
                .serializeAndDeserialize(definition);

        ValueProvider<TestRecord, ?> getter = deserializedDefinition
                .getGetter();

        TestRecord testRecord = new TestRecord("someone", 42);

        String name = (String) getter.apply(testRecord);

        assertEquals("someone", name,
                "Deserialized definition should be functional");

        PropertyDescriptor namePropertyDescriptor = BeanUtil
                .getPropertyDescriptor(TestRecord.class, "name");
        assertNotNull(namePropertyDescriptor);
        assertEquals(namePropertyDescriptor.getName(), "name",
                "Property has unexpected name");
        assertEquals(namePropertyDescriptor.getReadMethod().getName(), "name",
                "Property read method has unexpected name");

        Class<?> namePropertyType = BeanUtil.getPropertyType(TestRecord.class,
                "name");
        assertEquals(namePropertyType, String.class,
                "Property type is unexpected");

        // Ensure props for Record are not sorted, but are in code order
        List<PropertyDefinition<TestRecord, ?>> propertyList = definition
                .getPropertySet().getProperties().toList();
        assertEquals("name", propertyList.get(0).getName());
        assertEquals("age", propertyList.get(1).getName());
    }

    @Test
    void nestedPropertyDefinition_samePropertyNameOnMultipleLevels() {
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

        assertEquals(grandFather.getFirstName(), firstName);
    }

    @Test
    void nestedPropertyDefinition_propertyChainBroken() {
        assertThrows(NullPointerException.class, () -> {
            PropertyDefinition<FatherAndSon, ?> definition = BeanPropertySet
                    .get(FatherAndSon.class).getProperty("father.firstName")
                    .orElseThrow(RuntimeException::new);

            ValueProvider<FatherAndSon, ?> getter = definition.getGetter();

            getter.apply(new FatherAndSon("Jon", "Doe", null, null));
        });
    }

    @Test
    void nestedPropertyDefinition_invalidPropertyNameInChain() {
        assertThrows(IllegalArgumentException.class, () -> {
            BeanPropertySet.get(FatherAndSon.class)
                    .getProperty("grandfather.firstName");
        });
    }

    @Test
    void nestedPropertyDefinition_invalidPropertyNameAtChainEnd() {
        assertThrows(IllegalArgumentException.class, () -> BeanPropertySet
                .get(FatherAndSon.class).getProperty("father.age"));
    }

    @Test
    void properties() {
        PropertySet<Person> propertySet = BeanPropertySet.get(Person.class);

        Set<String> propertyNames = propertySet.getProperties()
                .map(PropertyDefinition::getName).collect(Collectors.toSet());

        assertEquals(new HashSet<>(Arrays.asList("name", "born")),
                propertyNames);
    }

    @Test
    void isSubProperty() {
        PropertySet<FatherAndSon> propertySet = BeanPropertySet
                .get(FatherAndSon.class);

        assertTrue(
                propertySet.getProperty("father.firstName").get()
                        .isSubProperty(),
                "Dot-separated property chain \"father.firstName\" should refer to a sub-property");
        assertFalse(propertySet.getProperty("father").get().isSubProperty(),
                "Property name without dot-separated parent properties should not refer to a sub-property");
        assertTrue(
                propertySet.getProperty("father.son.father.son").get()
                        .isSubProperty(),
                "Dot-separated property chain \"father.son.father.son\" should refer to a sub-property");
    }

    @Test
    void getFullName_returnsFullPropertyChain() {
        PropertySet<FatherAndSon> propertySet = BeanPropertySet
                .get(FatherAndSon.class);
        String subPropertyFullName = "father.son.father.son.firstName";
        PropertyDefinition<FatherAndSon, ?> subProperty = propertySet
                .getProperty(subPropertyFullName).get();
        assertEquals("firstName", subProperty.getTopLevelName(),
                "Name of a sub-property should be the simple name of the property");
        assertEquals(subPropertyFullName, subProperty.getName(),
                "Full name of a sub-property should be the full property chain with parent properties");
    }

    @Test
    void getParentForDirectProperty_returnsNull() {
        PropertySet<FatherAndSon> propertySet = BeanPropertySet
                .get(FatherAndSon.class);
        assertNull(propertySet.getProperty("father").get().getParent(),
                "Direct property of a property set should not have a parent");
    }

    @Test
    void getParentForSubProperty_returnsParent() {
        PropertySet<FatherAndSon> propertySet = BeanPropertySet
                .get(FatherAndSon.class);
        assertEquals("father.son",
                propertySet.getProperty("father.son.father").get().getParent()
                        .getName(),
                "Parent property of \"father.son.father\" should be \"father.son\"");
    }

    @Test
    void get_beanImplementsSameInterfaceSeveralTimes_interfacePropertyIsNotDuplicated() {
        PropertySet<Iface3> set = BeanPropertySet.get(Iface3.class, false,
                PropertyFilterDefinition.getDefaultFilter());

        List<PropertyDefinition<Iface3, ?>> defs = set.getProperties()
                .collect(Collectors.toList());

        assertEquals(1, defs.size());
        assertEquals("name", defs.get(0).getName());
    }

    @Test
    void get_beanImplementsInterfaceWithDefaultMethod_propertyFound() {
        PropertySet<DefaultMethodIface> set = BeanPropertySet
                .get(DefaultMethodIface.class);

        List<PropertyDefinition<DefaultMethodIface, ?>> defs = set
                .getProperties().collect(Collectors.toList());

        assertEquals(1, defs.size());
        assertEquals("name", defs.get(0).getName());
    }

    @Test
    void get_beanImplementsGenericInterfaceSubclassWithDefaultMethod_interfacePropertyIsNotDuplicated() {
        PropertySet<DefaultMethodSubclassIface> set = BeanPropertySet
                .get(DefaultMethodSubclassIface.class);

        List<PropertyDefinition<DefaultMethodSubclassIface, ?>> defs = set
                .getProperties().collect(Collectors.toList());

        assertEquals(3, defs.size());
        assertEquals("name", defs.get(0).getName());
        assertEquals(String.class, defs.get(0).getType());
        assertEquals("property", defs.get(1).getName());
        assertEquals(String.class, defs.get(1).getType());
        assertEquals("wrappedProperty", defs.get(2).getName());
        assertEquals(GenericIface.class, defs.get(2).getType());
    }

    public interface HasSomething {
        default String getSomething() {
            return "something";
        }

        default void setSomething(String something) {
            // do nothing
        }

    }

    public interface HasName extends HasSomething {

        default String getName() {
            return this.getLastName() + ", " + this.getFirstName();
        }

        default void setName(String name) {
            final Matcher matcher = Pattern.compile("^(.+), (.+)$")
                    .matcher(name);
            this.setLastName(matcher.group(1));
            this.setFirstName(matcher.group(2));
        }

        String getLastName();

        void setLastName(String lastName);

        String getFirstName();

        void setFirstName(String firstName);
    }

    public class MyClass implements HasName {

        private String lastName;
        private String firstName;

        @Override
        public String getLastName() {
            return this.lastName;
        }

        @Override
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        @Override
        public String getFirstName() {
            return this.firstName;
        }

        @Override
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
    }

    @Test
    void includesDefaultMethodsFromInterfaces() {
        PropertySet<MyClass> set = BeanPropertySet.get(MyClass.class);

        List<PropertyDefinition<MyClass, ?>> defs = set.getProperties()
                .collect(Collectors.toList());

        assertEquals(4, defs.size());
        assertEquals("firstName", defs.get(0).getName());
        assertEquals(String.class, defs.get(0).getType());
        assertEquals("lastName", defs.get(1).getName());
        assertEquals(String.class, defs.get(1).getType());
        assertEquals("name", defs.get(2).getName());
        assertEquals(String.class, defs.get(2).getType());
        assertEquals("something", defs.get(3).getName());
        assertEquals(String.class, defs.get(3).getType());

    }
}
