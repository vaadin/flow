package com.vaadin.hummingbird.kernel;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.kernel.AbstractElementTemplate.Keys;

public class ComputedPropertyTest {
    @Test
    public void simpleComputedProperty() {
        StateNode node = StateNode.create();
        node.putComputed("computed", () -> "foo");

        Assert.assertTrue(node.containsKey("computed"));

        Object value = node.get("computed");
        Assert.assertEquals("foo", value);
    }

    @Test
    public void hasComputedProperty() {
        StateNode node = StateNode.create();
        node.putComputed("computed", () -> "foo");

        Set<String> stringKeys = node.getStringKeys();

        Assert.assertTrue(stringKeys.contains("computed"));
        Assert.assertEquals(1, stringKeys.size());
    }

    @Test
    public void computedNodeNotServerOnly() {
        StateNode node = StateNode.create();
        node.putComputed("computed", () -> "foo");

        Assert.assertFalse(
                node.getKeys().anyMatch(Predicate.isEqual(Keys.SERVER_ONLY)));
        Assert.assertFalse(node.containsKey(Keys.SERVER_ONLY));

        node.put(Keys.SERVER_ONLY, null);
        Assert.assertTrue(
                node.getKeys().anyMatch(Predicate.isEqual(Keys.SERVER_ONLY)));
        Assert.assertTrue(node.containsKey(Keys.SERVER_ONLY));
    }

    @Test
    public void replacePropertyWithCompouted_throws() {
        StateNode node = StateNode.create();
        node.put("foo", "bar");

        try {
            node.putComputed("foo", () -> "baz");
            Assert.fail();
        } catch (IllegalStateException e) {
            // Expected
        }

        Assert.assertEquals("bar", node.get("foo"));
    }

    @Test
    public void testBasicReactivity() {
        AtomicInteger invokeCount = new AtomicInteger();
        StateNode node = StateNode.create();

        node.put("a", Integer.valueOf(5));
        node.putComputed("b", () -> {
            invokeCount.incrementAndGet();
            return Integer.valueOf(2 * node.get("a", Integer.class).intValue());
        });

        Assert.assertEquals(0, invokeCount.get());

        Assert.assertEquals(Integer.valueOf(10), node.get("b"));
        Assert.assertEquals(1, invokeCount.get());

        Assert.assertEquals(Integer.valueOf(10), node.get("b"));
        Assert.assertEquals(1, invokeCount.get());

        node.put("a", Integer.valueOf(6));
        Assert.assertEquals(1, invokeCount.get());

        Assert.assertEquals(Integer.valueOf(12), node.get("b"));
        Assert.assertEquals(2, invokeCount.get());
    }

    @Test
    public void testDependenciesAfterRollback() {
        StateNode node = StateNode.create();
        RootNode root = new RootNode();
        root.put("child", node);

        node.put("a", "A");
        node.put("b", "B");
        node.put("which", "a");

        node.putComputed("computed", () -> node.get(node.get("which")));
        node.putComputed("another", () -> node.get("a"));

        Assert.assertEquals("A", node.get("computed"));
        Assert.assertEquals("A", node.get("another"));

        node.put("a", "C");

        root.commit();
        node.put("which", "b");

        Assert.assertEquals("B", node.get("computed"));

        root.rollback();

        Assert.assertEquals("a", node.get("which"));

        node.put("a", "D");
        Assert.assertEquals("D", node.get("computed"));
    }

    @Test
    public void testContainsIsDependency() {
        StateNode node = StateNode.create();

        node.putComputed("computed",
                () -> Boolean.valueOf(node.containsKey("foo")));

        Assert.assertFalse(node.get("computed", Boolean.class).booleanValue());

        node.put("foo", null);

        Assert.assertTrue(node.get("computed", Boolean.class).booleanValue());

        node.remove("foo");

        Assert.assertFalse(node.get("computed", Boolean.class).booleanValue());
    }
}
