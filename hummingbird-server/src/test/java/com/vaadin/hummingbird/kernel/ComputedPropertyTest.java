package com.vaadin.hummingbird.kernel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.kernel.AbstractElementTemplate.Keys;
import com.vaadin.hummingbird.kernel.TemplateModelTest.MyTestModel;
import com.vaadin.hummingbird.kernel.TemplateModelTest.SubModelType;
import com.vaadin.ui.Template.Model;

public class ComputedPropertyTest {
    private static Map<String, ComputedProperty> createComputedPropertyMap(
            String name, Function<StateNode, Object> supplier) {
        return Collections.singletonMap(name,
                createComputedProperty(name, supplier));
    }

    public static Map<String, ComputedProperty> createMap(
            ComputedProperty... properties) {
        return Collections.unmodifiableMap(
                Arrays.stream(properties).collect(Collectors.toMap(
                        ComputedProperty::getName, Function.identity())));
    }

    public static ComputedProperty createComputedProperty(String name,
            Function<StateNode, Object> supplier) {
        return new ComputedProperty(name, null) {
            @Override
            public Object compute(StateNode context) {
                return supplier.apply(context);
            }
        };
    }

    @Test
    public void simpleComputedProperty() {
        StateNode node = StateNode.create();
        node.setComputedProperties(
                createComputedPropertyMap("computed", n -> "foo"));

        Assert.assertTrue(node.containsKey("computed"));

        Object value = node.get("computed");
        Assert.assertEquals("foo", value);
    }

    @Test
    public void hasComputedProperty() {
        StateNode node = StateNode.create();
        node.setComputedProperties(
                createComputedPropertyMap("computed", n -> "foo"));

        Set<String> stringKeys = node.getStringKeys();

        Assert.assertTrue(stringKeys.contains("computed"));
        Assert.assertEquals(1, stringKeys.size());
    }

    @Test
    public void computedNodeNotServerOnly() {
        StateNode node = StateNode.create();
        node.setComputedProperties(
                createComputedPropertyMap("computed", n -> "foo"));

        Assert.assertFalse(node.getKeys().stream()
                .anyMatch(Predicate.isEqual(Keys.SERVER_ONLY)));
        Assert.assertFalse(node.containsKey(Keys.SERVER_ONLY));

        node.put(Keys.SERVER_ONLY, null);
        Assert.assertTrue(node.getKeys().stream()
                .anyMatch(Predicate.isEqual(Keys.SERVER_ONLY)));
        Assert.assertTrue(node.containsKey(Keys.SERVER_ONLY));
    }

    @Test
    public void replacePropertyWithCompouted_usesComputed() {
        StateNode node = StateNode.create();
        node.put("foo", "bar");

        node.setComputedProperties(
                createComputedPropertyMap("foo", n -> "foo"));
        Assert.assertEquals("foo", node.get("foo"));
    }

    @Test(expected = IllegalStateException.class)
    public void setComputedMultipleTimes_throws() {
        StateNode node = StateNode.create();
        node.setComputedProperties(
                createComputedPropertyMap("computed", n -> "foo"));
        node.setComputedProperties(
                createComputedPropertyMap("computed", n -> "foo"));
    }

    @Test
    public void testBasicReactivity() {
        AtomicInteger invokeCount = new AtomicInteger();
        StateNode node = StateNode.create();

        node.put("a", Integer.valueOf(5));
        node.setComputedProperties(createComputedPropertyMap("b", n -> {
            invokeCount.incrementAndGet();
            return Integer.valueOf(2 * node.get("a", Integer.class).intValue());
        }));

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

        node.setComputedProperties(createMap(
                createComputedProperty("computed", n -> n.get(n.get("which"))),
                createComputedProperty("another", n -> n.get("a"))));

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

        node.setComputedProperties(createComputedPropertyMap("computed",
                n -> Boolean.valueOf(n.containsKey("foo"))));

        Assert.assertFalse(node.get("computed", Boolean.class).booleanValue());

        node.put("foo", null);

        Assert.assertTrue(node.get("computed", Boolean.class).booleanValue());

        node.remove("foo");

        Assert.assertFalse(node.get("computed", Boolean.class).booleanValue());
    }

    @Test(expected = AssertionError.class)
    public void setModifiableThrows() {
        StateNode node = StateNode.create();

        node.setComputedProperties(new HashMap<>());
    }

    @Test
    public void testJsComptedProperty() {
        StateNode node = StateNode.create();
        MyTestModel model = Model.wrap(node, MyTestModel.class);
        model.setInt(5);

        SubModelType subValue = Model.wrap(StateNode.create(),
                SubModelType.class);
        subValue.setValue("Foo");
        model.setSubValue(subValue);

        Object value = new JsComputedProperty("foo", "model.int + 1",
                Integer.class).compute(node);

        Assert.assertEquals(Integer.valueOf(6), value);

        value = new JsComputedProperty("foo", "model.subValue.value",
                String.class).compute(node);
        Assert.assertEquals("Foo", value);
    }
}
