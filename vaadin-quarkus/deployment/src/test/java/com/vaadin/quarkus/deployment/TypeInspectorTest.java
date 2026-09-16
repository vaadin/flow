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
package com.vaadin.quarkus.deployment;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whatever a {@code @ClientCallable} signature mentions has to end up
 * registered for reflection in a native image, so the inspector has to reach
 * through every shape a signature can take. A type it misses is a
 * ClassNotFoundException at runtime, in the native build only.
 */
class TypeInspectorTest {

    private Index index;

    @BeforeEach
    void setUp() throws IOException {
        Indexer indexer = new Indexer();
        for (Class<?> type : List.of(Signatures.class, Payload.class,
                Nested.class, Bound.class, BoundSubclass.class, List.class,
                Map.class, String.class, Object.class)) {
            indexer.indexClass(type);
        }
        index = indexer.complete();
    }

    private Set<String> typesOf(String methodName) {
        ClassInfo signatures = index
                .getClassByName(DotName.createSimple(Signatures.class));
        MethodInfo method = signatures.methods().stream()
                .filter(m -> m.name().equals(methodName)).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no method called " + methodName));
        return TypeInspector.collectTypes(method, index).stream()
                .map(info -> info.name().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void collectTypes_arrayParameter_reachesTheComponentType() {
        assertTrue(typesOf("array").contains(Payload.class.getName()));
    }

    @Test
    void collectTypes_nestedArray_reachesTheComponentType() {
        assertTrue(typesOf("nestedArray").contains(Payload.class.getName()));
    }

    @Test
    void collectTypes_wildcardWithUpperBound_reachesTheBound() {
        assertTrue(typesOf("upperBounded").contains(Payload.class.getName()));
    }

    @Test
    void collectTypes_wildcardWithLowerBound_reachesTheBound() {
        assertTrue(typesOf("lowerBounded").contains(Payload.class.getName()));
    }

    @Test
    void collectTypes_unboundedWildcard_reachesOnlyItsObjectBound() {
        // Jandex models a bare ? as "? extends Object", so the bound that is
        // followed is Object - nothing that names an application type.
        Set<String> types = typesOf("unbounded");

        assertEquals(Set.of(Object.class.getName()), types);
        assertFalse(types.contains(Payload.class.getName()));
    }

    @Test
    void collectTypes_typeVariable_reachesItsBound() {
        assertTrue(typesOf("typeVariable").contains(Bound.class.getName()));
    }

    @Test
    void collectTypes_mapWithNestedGenerics_reachesEveryArgument() {
        Set<String> types = typesOf("nestedGenerics");

        assertTrue(types.contains(String.class.getName()));
        assertTrue(types.contains(Payload.class.getName()));
        assertTrue(types.contains(Nested.class.getName()));
    }

    @Test
    void collectTypes_primitivesAndVoid_areIgnored() {
        assertEquals(Set.of(), typesOf("primitives"),
                "there is nothing to register for a signature of primitives");
    }

    @Test
    void collectTypes_typeNotInTheIndex_isSkipped() {
        assertFalse(typesOf("notIndexed").contains(
                "com.vaadin.quarkus.deployment.TypeInspectorTest$Unindexed"),
                "a type the index does not know cannot be registered");
    }

    @Test
    void collectTypes_nullType_collectsNothing() {
        assertTrue(TypeInspector
                .collectTypes((org.jboss.jandex.Type) null, index).isEmpty());
    }

    @SuppressWarnings("unused")
    static class Signatures {
        public void array(Payload[] payloads) {
        }

        public void nestedArray(Payload[][] payloads) {
        }

        public void upperBounded(List<? extends Payload> payloads) {
        }

        public void lowerBounded(List<? super Payload> payloads) {
        }

        public void unbounded(List<?> anything) {
        }

        public <T extends Bound> void typeVariable(T value) {
        }

        public Map<String, List<Payload>> nestedGenerics(Nested nested) {
            return null;
        }

        public void primitives(int count, boolean flag) {
        }

        public void notIndexed(Unindexed value) {
        }
    }

    static class Payload {
    }

    static class Nested {
    }

    static class Bound {
    }

    static class BoundSubclass extends Bound {
    }

    static class Unindexed {
    }
}
