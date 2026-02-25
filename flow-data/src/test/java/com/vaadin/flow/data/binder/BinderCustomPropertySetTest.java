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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.function.ValueProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class BinderCustomPropertySetTest {
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
        public boolean isGenericType() {
            return false;
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
    void testBindByString() {
        TestTextField field = new TestTextField();
        Map<String, String> map = new HashMap<>();
        Binder<Map<String, String>> binder = Binder
                .withPropertySet(new MapPropertySet());

        binder.bind(field, "key");
        binder.setBean(map);

        field.setValue("value");
        assertEquals("value", map.get("key"),
                "Field value should propagate to the corresponding key in the map");
    }

    @Test
    void testBindInstanceFields() {
        Map<String, String> map = new HashMap<>();
        Binder<Map<String, String>> binder = Binder
                .withPropertySet(new MapPropertySet());
        InstanceFields instanceFields = new InstanceFields();

        binder.bindInstanceFields(instanceFields);

        assertNotNull(instanceFields.one,
                "Field corresponding to supported property name should be bound");
        assertNull(instanceFields.another,
                "Field corresponding to unsupported property name should be ignored");

        binder.setBean(map);

        instanceFields.one.setValue("value");
        assertEquals("value", map.get("one"),
                "Field value should propagate to the corresponding key in the map");
    }
}
