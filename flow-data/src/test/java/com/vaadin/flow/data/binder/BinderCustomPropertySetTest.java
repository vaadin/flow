/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.function.ValueProvider;

public class BinderCustomPropertySetTest {
    public static class MapPropertyDefinition
            implements PropertyDefinition<Map<String, String>, String> {

        private MapPropertySet propertySet;
        private String name;

        public MapPropertyDefinition(MapPropertySet propertySet, String name) {
            this.propertySet = propertySet;
            this.name = name;
        }

        @Override
        public ValueProvider<Map<String, String>, String> getGetter() {
            return map -> map.get(name);
        }

        @Override
        public Optional<Setter<Map<String, String>, String>> getSetter() {
            return Optional.of((map, value) -> {
                if (value == null) {
                    map.remove(name);
                } else {
                    map.put(name, value);
                }
            });
        }

        @Override
        public Class<String> getType() {
            return String.class;
        }

        @Override
        public Class<?> getPropertyHolderType() {
            return Map.class;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public PropertySet<Map<String, String>> getPropertySet() {
            return propertySet;
        }

        @Override
        public String getCaption() {
            return name.toUpperCase(Locale.ENGLISH);
        }

        @Override
        public PropertyDefinition<Map<String, String>, ?> getParent() {
            return null;
        }

    }

    public static class MapPropertySet
            implements PropertySet<Map<String, String>> {
        @Override
        public Stream<PropertyDefinition<Map<String, String>, ?>> getProperties() {
            return Stream.of("one", "two", "three").map(this::createProperty);
        }

        @Override
        public Optional<PropertyDefinition<Map<String, String>, ?>> getProperty(
                String name) {
            return Optional.of(createProperty(name));
        }

        private PropertyDefinition<Map<String, String>, ?> createProperty(
                String name) {
            return new MapPropertyDefinition(this, name);
        }
    }

    public static class InstanceFields {
        private TestTextField one;
        private TestTextField another;
    }

    @Test
    public void testBindByString() {
        TestTextField field = new TestTextField();
        Map<String, String> map = new HashMap<>();
        Binder<Map<String, String>> binder = Binder
                .withPropertySet(new MapPropertySet());

        binder.bind(field, "key");
        binder.setBean(map);

        field.setValue("value");
        Assert.assertEquals(
                "Field value should propagate to the corresponding key in the map",
                "value", map.get("key"));
    }

    @Test
    public void testBindInstanceFields() {
        Map<String, String> map = new HashMap<>();
        Binder<Map<String, String>> binder = Binder
                .withPropertySet(new MapPropertySet());
        InstanceFields instanceFields = new InstanceFields();

        binder.bindInstanceFields(instanceFields);

        Assert.assertNotNull(
                "Field corresponding to supported property name should be bound",
                instanceFields.one);
        Assert.assertNull(
                "Field corresponding to unsupported property name should be ignored",
                instanceFields.another);

        binder.setBean(map);

        instanceFields.one.setValue("value");
        Assert.assertEquals(
                "Field value should propagate to the corresponding key in the map",
                "value", map.get("one"));
    }
}
