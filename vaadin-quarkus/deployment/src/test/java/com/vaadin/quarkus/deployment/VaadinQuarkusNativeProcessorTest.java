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
package com.vaadin.quarkus.deployment;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaadinQuarkusNativeProcessorTest {

    private VaadinQuarkusNativeProcessor processor;
    private Index index;

    @BeforeEach
    void setUp() throws IOException {
        processor = new VaadinQuarkusNativeProcessor();

        // Create an index with test classes
        Indexer indexer = new Indexer();

        // Index the test classes and related types
        indexer.indexClass(Collection.class);
        indexer.indexClass(String.class);
        indexer.indexClass(List.class);
        indexer.indexClass(Integer.class);

        indexer.indexClass(TestComponent.class);
        indexer.indexClass(SimpleDto.class);
        indexer.indexClass(ComplexDto.class);
        indexer.indexClass(NestedDto.class);
        indexer.indexClass(RecordId.class);
        indexer.indexClass(RecordDto.class);

        index = indexer.complete();
    }

    @Test
    void testDetectClientCallablesTypes_withSimpleReturnType() {
        Set<ClassInfo> result = processor.detectClientCallablesTypes(index);

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(containsClass(SimpleDto.class)),
                "Should detect SimpleDto from return type");
    }

    @Test
    void testDetectClientCallablesTypes_withParameterTypes() {
        Set<ClassInfo> result = processor.detectClientCallablesTypes(index);

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(containsClass(ComplexDto.class)),
                "Should detect ComplexDto from parameter type");
    }

    @Test
    void testDetectClientCallablesTypes_withNestedParameterizedTypes() {
        Set<ClassInfo> result = processor.detectClientCallablesTypes(index);

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(containsClass(NestedDto.class)),
                "Should detect NestedDto from parameterized type argument");
    }

    @Test
    void testDetectClientCallablesTypes_withGenericDefinition() {
        Set<ClassInfo> result = processor.detectClientCallablesTypes(index);

        assertNotNull(result);
        Set<String> classNames = result.stream().map(ci -> ci.name().toString())
                .collect(Collectors.toSet());
        assertTrue(classNames.contains(RecordDto.class.getName()),
                "Should detect RecordDto from parameterized return type");
        assertTrue(classNames.contains(RecordId.class.getName()),
                "Should detect RecordId from parameterized parameter type");
    }

    @Test
    void testDetectClientCallablesTypes_excludesPrimitives() {
        Set<ClassInfo> result = processor.detectClientCallablesTypes(index);

        assertNotNull(result);
        // Verify no primitive types are in the result
        assertFalse(result.stream().anyMatch(containsClass(int.class)),
                "Should not include primitive types");
    }

    @Test
    void testDetectClientCallablesTypes_returnsEmptySetWhenNoClientCallables()
            throws IOException {
        // Create a new index without any ClientCallable annotations
        Indexer indexer = new Indexer();
        indexer.indexClass(String.class);
        Index emptyIndex = indexer.complete();

        Set<ClassInfo> result = processor
                .detectClientCallablesTypes(emptyIndex);

        assertNotNull(result);
        assertTrue(result.isEmpty(),
                "Should return empty set when no ClientCallable methods exist");
    }

    @Test
    void testDetectClientCallablesTypes_onlyIncludesComponentSubclasses()
            throws IOException {
        // Create a new index with both Component and non-Component classes
        Indexer indexer = new Indexer();
        indexer.indexClass(TestComponent.class);
        indexer.indexClass(NonComponentClass.class);
        indexer.indexClass(SimpleDto.class);
        indexer.indexClass(OtherDto.class);
        indexer.indexClass(com.vaadin.flow.component.Component.class);
        Index testIndex = indexer.complete();

        Set<ClassInfo> result = processor.detectClientCallablesTypes(testIndex);

        // Should only include types from TestComponent methods (which extends
        // Component)
        // Should NOT include types from NonComponentClass methods
        assertTrue(result.stream().anyMatch(containsClass(SimpleDto.class)),
                "Should detect SimpleDto from Component subclass");
        assertFalse(result.stream().anyMatch(containsClass(OtherDto.class)),
                "Should NOT detect OtherDto from non-Component class");
    }

    @Test
    void testDetectClientCallablesTypes_handlesMultipleLevelInheritance()
            throws IOException {
        // Create index with multi-level Component hierarchy
        Indexer indexer = new Indexer();
        indexer.indexClass(ExtendedComponent.class);
        indexer.indexClass(TestComponent.class);
        indexer.indexClass(NestedDto.class);
        indexer.indexClass(com.vaadin.flow.component.Component.class);
        indexer.indexClass(List.class);
        Index testIndex = indexer.complete();

        Set<ClassInfo> result = processor.detectClientCallablesTypes(testIndex);

        // Should detect types from ExtendedComponent which extends
        // TestComponent which extends Component
        assertTrue(result.stream().anyMatch(containsClass(NestedDto.class)),
                "Should detect NestedDto from multi-level Component subclass");
    }

    private static Predicate<ClassInfo> containsClass(Class<?> expectedClass) {
        return ci -> ci.name().toString().equals(expectedClass.getName());
    }

    // Test component with ClientCallable methods
    public static class TestComponent extends Component {

        @ClientCallable
        public SimpleDto getSimpleData() {
            return null;
        }

        @ClientCallable
        public void processData(ComplexDto data) {
        }

        @ClientCallable
        public void processDataWithPrimitive(int value) {
        }

        @ClientCallable
        public List<NestedDto> getNestedList() {
            return null;
        }

        @ClientCallable
        public void handleVoid() {
        }

        @ClientCallable
        public <X extends RecordId, Y extends RecordDto> Collection<X> handleGenericDefinition(
                List<Y> id) {
            return null;
        }
    }

    // Test DTOs
    public static class SimpleDto {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class ComplexDto {
        private String id;
        private NestedDto nested;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public NestedDto getNested() {
            return nested;
        }

        public void setNested(NestedDto nested) {
            this.nested = nested;
        }
    }

    public static class NestedDto {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public record RecordId(int id) {
    }

    public record RecordDto(String value) {
    }

    // Non-Component class with ClientCallable (should be filtered out)
    public static class NonComponentClass {
        @ClientCallable
        public OtherDto getNonComponentData() {
            return null;
        }
    }

    public static class OtherDto {
        private String data;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    // Extended component for multi-level inheritance testing
    public static class ExtendedComponent extends TestComponent {
        @ClientCallable
        public List<NestedDto> getExtendedData() {
            return null;
        }
    }
}
