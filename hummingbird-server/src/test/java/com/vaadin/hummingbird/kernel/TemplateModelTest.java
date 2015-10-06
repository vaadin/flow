package com.vaadin.hummingbird.kernel;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.ui.Template;
import com.vaadin.ui.Template.Model;

public class TemplateModelTest {
    public interface SubModelType {
        public String getValue();

        public void setValue(String value);
    }

    public interface MyTestModel extends Model {
        public String getValue();

        public void setValue(String value);

        public boolean isBoolean();

        public void setBoolean(boolean value);

        public int getInt();

        public void setInt(int value);

        public SubModelType getSubValue();

        public void setSubValue(SubModelType subValue);

        public List<String> getSimpleList();

        public void setSimpleList(List<String> simpleList);

        public List<SubModelType> getComplexList();

        public void setComplexList(List<SubModelType> complexList);
    }

    public class MyTestTemplate extends Template {
        public MyTestTemplate() {
            super(TemplateBuilder.withTag("foo").build());
        }

        @Override
        public MyTestModel getModel() {
            return (MyTestModel) super.getModel();
        }

        @Override
        public StateNode getNode() {
            // Override to make public
            return super.getNode();
        }
    }

    private MyTestTemplate template = new MyTestTemplate();
    private MyTestModel model = template.getModel();
    private StateNode node = template.getNode();

    @Test
    public void testTemplateModelAccess() {
        model.setValue("foo");
        Assert.assertEquals("foo", node.get("value"));

        template.getNode().put("value", "bar");
        Assert.assertEquals("bar", model.getValue());
    }

    @Test
    public void testBooleanFalseRemovesProperty() {
        Assert.assertFalse(model.isBoolean());

        model.setBoolean(true);
        Assert.assertEquals(Boolean.TRUE, node.get("boolean"));

        model.setBoolean(false);
        Assert.assertFalse(node.containsKey("boolean"));
    }

    @Test
    public void testPrimitiveType() {
        Assert.assertEquals(0, model.getInt());

        model.setInt(1);
        Assert.assertEquals(Integer.valueOf(1), node.get("int"));

        node.put("int", Integer.valueOf(2));
        Assert.assertEquals(2, model.getInt());
    }

    @Test
    public void testSubModelType() {
        Assert.assertNull(model.getSubValue());

        model.setSubValue(model.create(SubModelType.class));

        // TODO What do I do here?

        SubModelType value = model.getSubValue();
        Assert.assertNotNull(value);
        Assert.assertTrue(node.get("subValue") instanceof StateNode);

        StateNode subNode = node.get("subValue", StateNode.class);

        Assert.assertNull(value.getValue());
        value.setValue("lorem");
        Assert.assertEquals("lorem", subNode.get("value"));
    }

    @Test
    public void testModelSimpleList() {
        Assert.assertNull(model.getSimpleList());

        List<String> initialList = Arrays.asList("Foo");
        model.setSimpleList(initialList);
        List<String> modelLList = model.getSimpleList();

        Assert.assertEquals(1, modelLList.size());
        Assert.assertEquals("Foo", modelLList.get(0));

        List<Object> nodeList = node.getMultiValued("simpleList");
        Assert.assertEquals(1, nodeList.size());
        Assert.assertEquals("Foo", nodeList.get(0));

        nodeList.add("Bar");
        Assert.assertEquals(2, modelLList.size());
        Assert.assertEquals("Bar", modelLList.get(1));

        node.remove("simpleList");
        Assert.assertNull(model.getSimpleList());
    }

    @Test
    public void testComplexList() {
        Assert.assertNull(model.getComplexList());

        List<Object> nodeList = node.getMultiValued("complexList");

        List<SubModelType> modelList = model.getComplexList();
        Assert.assertEquals(0, modelList.size());

        // Add through node
        StateNode child = StateNode.create();
        child.put("value", "foo");
        nodeList.add(child);

        Assert.assertEquals(1, modelList.size());
        Assert.assertEquals("foo", modelList.get(0).getValue());

        // Add through model
        SubModelType subItem = model.create(SubModelType.class);
        modelList.add(subItem);
        Assert.assertEquals(2, nodeList.size());
        Assert.assertNull(((StateNode) nodeList.get(1)).get("value"));
        subItem.setValue("bar");
        Assert.assertEquals("bar", ((StateNode) nodeList.get(1)).get("value"));

        // Remove through model
        modelList.remove(0);
        Assert.assertEquals(1, nodeList.size());
        Assert.assertNull(child.getParent());

        nodeList.remove(0);
        Assert.assertEquals(0, modelList.size());
    }

    @Test
    public void testWrapNode() {
        StateNode node = StateNode.create();

        SubModelType modelValue = model.wrap(node, SubModelType.class);
        modelValue.setValue("foo");
        Assert.assertEquals("foo", node.get("value"));
    }
}
