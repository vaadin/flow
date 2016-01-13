package com.vaadin.hummingbird.kernel;

import java.util.Arrays;
import java.util.Collections;
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
        StateNode node = createNodeWithComputedProperties(
                createComputedPropertyMap("computed", n -> "foo"));

        Assert.assertTrue(node.containsKey("computed"));

        Object value = node.get("computed");
        Assert.assertEquals("foo", value);
    }

    @Test
    public void hasComputedProperty() {
        StateNode node = createNodeWithComputedProperties(
                createComputedPropertyMap("computed", n -> "foo"));

        Set<String> stringKeys = node.getStringKeys();

        Assert.assertTrue(stringKeys.contains("computed"));
        Assert.assertEquals(1, stringKeys.size());
    }

    @Test
    public void computedNodeNotServerOnly() {
        StateNode node = createNodeWithComputedProperties(
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
    public void testBasicReactivity() {
        AtomicInteger invokeCount = new AtomicInteger();
        StateNode[] reference = new StateNode[1];

        StateNode node = createNodeWithComputedProperties(
                createComputedPropertyMap("b", n -> {
                    invokeCount.incrementAndGet();
                    return Integer.valueOf(2
                            * reference[0].get("a", Integer.class).intValue());
                }));
        reference[0] = node;

        node.put("a", Integer.valueOf(5));

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
        StateNode node = createNodeWithComputedProperties(createMap(
                createComputedProperty("computed", n -> n.get(n.get("which"))),
                createComputedProperty("another", n -> n.get("a"))));

        RootNode root = new RootNode();
        root.put("child", node);

        node.put("a", "A");
        node.put("b", "B");
        node.put("which", "a");

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
        StateNode node = createNodeWithComputedProperties(
                createComputedPropertyMap("computed",
                        n -> Boolean.valueOf(n.containsKey("foo"))));

        Assert.assertFalse(node.get("computed", Boolean.class).booleanValue());

        node.put("foo", null);

        Assert.assertTrue(node.get("computed", Boolean.class).booleanValue());

        node.remove("foo");

        Assert.assertFalse(node.get("computed", Boolean.class).booleanValue());
    }

    @Test
    public void testJsComptedProperty() {
        StateNode node = StateNode.create(MyTestModel.TYPE);
        MyTestModel model = Model.wrap(node, MyTestModel.class);
        model.setInt(5);

        SubModelType subValue = Model.wrap(StateNode.create(SubModelType.TYPE),
                SubModelType.class);
        subValue.setValue("Foo");
        model.setSubValue(subValue);

        Object value = new JsComputedProperty("foo", "int + 1", Integer.class)
                .compute(node);

        Assert.assertEquals(Integer.valueOf(6), value);

        value = new JsComputedProperty("foo", "subValue.value", String.class)
                .compute(node);
        Assert.assertEquals("Foo", value);
    }

    @Test
    public void testJsComptedPropertyArray() {
        StateNode node = StateNode.create(MyTestModel.TYPE);
        MyTestModel model = Model.wrap(node, MyTestModel.class);
        model.setSimpleList(Arrays.asList("foo", "bar"));
        model.setInt(0);

        Object value = new JsComputedProperty("foo", "simpleList[int]",
                String.class).compute(node);

        Assert.assertEquals("foo", value);
        model.setInt(1);

        value = new JsComputedProperty("foo", "simpleList[int]", String.class)
                .compute(node);
        Assert.assertEquals("bar", value);
    }

    private static StateNode createNodeWithComputedProperties(
            Map<String, ComputedProperty> computedProperties) {
        return StateNode.create(
                ValueType.get(Collections.emptyMap(), computedProperties));
    }
}
