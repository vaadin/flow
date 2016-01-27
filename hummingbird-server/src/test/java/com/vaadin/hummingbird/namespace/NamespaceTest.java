/*
 * Copyright 2000-2016 Vaadin Ltd.
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

package com.vaadin.hummingbird.namespace;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.StateNodeTest;

public class NamespaceTest {
    private static abstract class UnregisteredNamespace extends Namespace {
        public UnregisteredNamespace(StateNode node) {
            super(node);
        }
    }

    @Test(expected = AssertionError.class)
    public void testCreateNullTypeThrows() {
        Namespace.create(null, StateNodeTest.createEmptyNode());
    }

    @Test(expected = AssertionError.class)
    public void testCreateNullNodeThrows() {
        Namespace.create(ElementDataNamespace.class, null);
    }

    @Test(expected = AssertionError.class)
    public void testCreateUnknownNamespaceThrows() {
        Namespace.create(UnregisteredNamespace.class,
                StateNodeTest.createEmptyNode());
    }

    @Test
    public void testGetIdValues() {
        // Verifies that the ids are the same as on the client side
        Map<Class<? extends Namespace>, Integer> expectedIds = new HashMap<>();

        expectedIds.put(ElementDataNamespace.class, 0);
        expectedIds.put(ElementPropertiesNamespace.class, 1);
        expectedIds.put(ElementChildrenNamespace.class, 2);

        Assert.assertEquals(
                "The number of expected namespaces is not up to date",
                expectedIds.size(), Namespace.namespaces.size());

        expectedIds.forEach((type, expectedId) -> {
            Assert.assertEquals("Unexpected id for " + type.getName(),
                    expectedId.intValue(), Namespace.getId(type));
        });
    }
}
