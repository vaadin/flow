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
