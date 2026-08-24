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
package com.vaadin.flow.signals.shared;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

import com.vaadin.flow.signals.SignalTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SharedSignalTypeReferenceTest extends SignalTestBase {
    enum Role {
        ADMIN, USER
    }

    private static final TypeReference<Set<Role>> ROLES = new TypeReference<>() {
    };

    @Test
    void valueSignal_classType_elementTypeIsLost() {
        // Documents the limitation that the type reference API addresses: a
        // class token cannot express the element type
        @SuppressWarnings({ "unchecked", "rawtypes" })
        SharedValueSignal<Set<Role>> signal = new SharedValueSignal<Set<Role>>(
                (Class) Set.class);
        signal.set(Set.of(Role.ADMIN));

        Set<?> erased = signal.peek();
        assertEquals(String.class, erased.iterator().next().getClass());
        assertThrows(ClassCastException.class, () -> {
            for (Role role : signal.peek()) {
                assertEquals(Role.ADMIN, role);
            }
        });
    }

    @Test
    void valueSignal_typeReference_elementTypeIsRetained() {
        var signal = new SharedValueSignal<>(ROLES);
        assertNull(signal.peek());

        signal.set(Set.of(Role.ADMIN));
        assertEquals(Set.of(Role.ADMIN), signal.peek());
    }

    @Test
    void valueSignal_initialValueAndTypeReference_elementTypeIsRetained() {
        var signal = new SharedValueSignal<>(Set.of(Role.ADMIN), ROLES);

        assertEquals(Set.of(Role.ADMIN), signal.peek());

        signal.set(Set.of(Role.USER));
        assertEquals(Set.of(Role.USER), signal.peek());
    }

    @Test
    void valueSignal_nestedTypeArguments_areRetained() {
        var signal = new SharedValueSignal<>(
                new TypeReference<Map<String, List<Role>>>() {
                });
        signal.set(Map.of("alice", List.of(Role.ADMIN)));

        assertEquals(Map.of("alice", List.of(Role.ADMIN)), signal.peek());
    }

    @Test
    void listAndMapSignal_typeReference_elementTypeIsRetained() {
        var list = new SharedListSignal<>(ROLES);
        list.insertLast(Set.of(Role.USER));
        assertEquals(Set.of(Role.USER), list.peek().get(0).peek());

        var map = new SharedMapSignal<>(ROLES);
        map.put("alice", Set.of(Role.ADMIN));
        assertEquals(Set.of(Role.ADMIN),
                Objects.requireNonNull(map.peek().get("alice")).peek());
    }

    @Test
    void nodeSignal_typeReferenceViews_elementTypeIsRetained() {
        var node = new SharedValueSignal<>(ROLES).asNode();
        node.asValue(ROLES).set(Set.of(Role.ADMIN));

        assertEquals(Set.of(Role.ADMIN), node.asValue(ROLES).peek());
        assertEquals(Set.of(Role.ADMIN), node.peek().value(ROLES));

        var listNode = new SharedListSignal<>(ROLES).asNode();
        listNode.asList(ROLES).insertLast(Set.of(Role.USER));
        assertEquals(Set.of(Role.USER),
                listNode.asList(ROLES).peek().get(0).peek());

        var mapNode = new SharedMapSignal<>(ROLES).asNode();
        mapNode.asMap(ROLES).put("alice", Set.of(Role.USER));
        assertEquals(Set.of(Role.USER),
                Objects.requireNonNull(mapNode.asMap(ROLES).peek().get("alice"))
                        .peek());
    }

    @Test
    void signalsWithDifferentTypeArguments_areNotEqual() {
        var roles = new SharedValueSignal<>(ROLES);
        var strings = new SharedValueSignal<>(new TypeReference<Set<String>>() {
        });

        assertNotEquals(roles, strings);
    }
}
