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
package com.vaadin.flow.internal;

import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnmodifiablePropertiesTest {

    @Test
    void modifyingOperations_throwExceptionAndKeepValues() {
        Properties properties = createProperties();

        Map<String, Executable> modifyingOperations = new LinkedHashMap<>();
        modifyingOperations.put("setProperty",
                () -> properties.setProperty("foo", "baz"));
        modifyingOperations.put("put", () -> properties.put("foo", "baz"));
        modifyingOperations.put("putAll", () -> properties
                .putAll(Collections.singletonMap("foo", "baz")));
        modifyingOperations.put("putIfAbsent",
                () -> properties.putIfAbsent("other", "baz"));
        modifyingOperations.put("remove", () -> properties.remove("foo"));
        modifyingOperations.put("clear", properties::clear);
        modifyingOperations.put("replace",
                () -> properties.replace("foo", "baz"));
        modifyingOperations.put("replaceAll",
                () -> properties.replaceAll((key, value) -> "baz"));
        modifyingOperations.put("compute",
                () -> properties.compute("foo", (key, value) -> "baz"));
        modifyingOperations.put("merge",
                () -> properties.merge("foo", "baz", (a, b) -> "baz"));
        modifyingOperations.put("load",
                () -> properties.load(new StringReader("foo=baz")));
        modifyingOperations.put("keySet",
                () -> properties.keySet().remove("foo"));
        modifyingOperations.put("values",
                () -> properties.values().remove("bar"));
        modifyingOperations.put("entrySet",
                () -> properties.entrySet().iterator().next().setValue("baz"));

        modifyingOperations.forEach((name, operation) -> assertThrows(
                UnsupportedOperationException.class, operation,
                "'" + name + "' should not be able to modify the properties"));

        assertEquals("bar", properties.getProperty("foo"));
        assertEquals(1, properties.size());
    }

    @Test
    void copyOfProperties_valuesAreReadableAndModifiable() {
        Properties properties = createProperties();

        Properties copy = new Properties();
        copy.putAll(properties);
        copy.setProperty("foo", "baz");

        assertEquals("baz", copy.getProperty("foo"));
        assertEquals("bar", properties.getProperty("foo"),
                "The original properties should not be affected by changes made to a copy");
    }

    @Test
    void changesToOriginalProperties_areNotVisible() {
        Properties original = new Properties();
        original.setProperty("foo", "bar");

        Properties properties = new UnmodifiableProperties(original);
        original.setProperty("foo", "baz");

        assertEquals("bar", properties.getProperty("foo"));
    }

    private Properties createProperties() {
        Properties properties = new Properties();
        properties.setProperty("foo", "bar");
        return new UnmodifiableProperties(properties);
    }
}
