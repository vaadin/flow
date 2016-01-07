package com.vaadin.hummingbird.kernel;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vaadin.annotations.JS;
import com.vaadin.hummingbird.kernel.ValueType.ObjectType;
import com.vaadin.hummingbird.kernel.change.IdChange;
import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListInsertManyChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;
import com.vaadin.hummingbird.kernel.change.NodeChangeVisitor;
import com.vaadin.hummingbird.kernel.change.ParentChange;
import com.vaadin.hummingbird.kernel.change.PutChange;
import com.vaadin.hummingbird.kernel.change.RangeEndChange;
import com.vaadin.hummingbird.kernel.change.RangeStartChange;
import com.vaadin.hummingbird.kernel.change.RemoveChange;
import com.vaadin.server.ServerRpcManager.RpcInvocationException;
import com.vaadin.server.ServerRpcMethodInvocation;
import com.vaadin.tests.util.MockUI;
import com.vaadin.ui.JavaScript.JavaScriptCallbackRpc;
import com.vaadin.ui.Template;
import com.vaadin.ui.Template.Model;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class TemplateModelTest {
    public interface SubModelType {
        public static final ObjectType TYPE = ValueType
                .getBeanType(SubModelType.class);

        public String getValue();

        public void setValue(String value);
    }

    public interface MyTestModel extends Model {
        public static final ObjectType TYPE = ValueType
                .getBeanType(MyTestModel.class);

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

        public default int getServerComptued() {
            return getInt() + (isBoolean() ? 1 : 2);
        }

        @JS("int  - (boolean ? 1 : 2)")
        public int getJsComputed();

        public default int getServerListLength() {
            List<String> list = getSimpleList();
            if (list == null) {
                return 0;
            }
            return list.size();
        }

        @JS("simpleList.length")
        public int getJsListLength();

        @JS("simpleList.indexOf('a')")
        public int getJsSimpleListIndexOfA();

        public default int getServerSimpleListIndexOfA() {
            if (getSimpleList() == null) {
                return -1;
            } else {
                return getSimpleList().indexOf("a");
            }
        }
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
    public void testBooleanFalseKeepsProperty() {
        Assert.assertFalse(model.isBoolean());

        model.setBoolean(true);
        Assert.assertEquals(Boolean.TRUE, node.get("boolean"));

        model.setBoolean(false);
        Assert.assertTrue(node.containsKey("boolean"));
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

        model.setSubValue(Model.create(SubModelType.class));

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
        Assert.assertNotNull(model.getSimpleList());

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
        Assert.assertNotNull(model.getComplexList());
        List<Object> nodeList = node.getMultiValued("complexList");

        List<SubModelType> modelList = model.getComplexList();
        Assert.assertEquals(0, modelList.size());

        // Add through node
        StateNode child = StateNode.create(SubModelType.TYPE);
        child.put("value", "foo");
        nodeList.add(child);

        Assert.assertEquals(1, modelList.size());
        Assert.assertEquals("foo", modelList.get(0).getValue());

        // Add through model
        SubModelType subItem = Model.create(SubModelType.class);
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
        StateNode node = StateNode.create(SubModelType.TYPE);

        SubModelType modelValue = Model.wrap(node, SubModelType.class);
        modelValue.setValue("foo");
        Assert.assertEquals("foo", node.get("value"));
    }

    @Test
    public void testSameValueNoChange() {
        model.setSubValue(Model.create(SubModelType.class));
        model.setSimpleList(Arrays.asList("foo", "bar"));
        RootNode root = new RootNode();
        root.put("child", node);
        root.commit();

        model.setBoolean(model.isBoolean());
        model.setInt(model.getInt());
        model.setValue(model.getValue());
        model.setSimpleList(model.getSimpleList());
        model.setSubValue(model.getSubValue());

        root.commit(new NodeChangeVisitor() {
            @Override
            public void visitRemoveChange(StateNode node,
                    RemoveChange removeChange) {
                Assert.fail(String.valueOf(removeChange.getKey()));
            }

            @Override
            public void visitPutChange(StateNode node, PutChange putChange) {
                Assert.fail(String.valueOf(putChange.getKey()));
            }

            @Override
            public void visitParentChange(StateNode node,
                    ParentChange parentChange) {
                Assert.fail(parentChange.toString());
            }

            @Override
            public void visitListReplaceChange(StateNode node,
                    ListReplaceChange listReplaceChange) {
                Assert.fail(listReplaceChange.toString());
            }

            @Override
            public void visitListRemoveChange(StateNode node,
                    ListRemoveChange listRemoveChange) {
                Assert.fail(listRemoveChange.toString());
            }

            @Override
            public void visitListInsertChange(StateNode node,
                    ListInsertChange listInsertChange) {
                Assert.fail(listInsertChange.toString());
            }

            @Override
            public void visitListInsertManyChange(StateNode node,
                    ListInsertManyChange listInsertManyChange) {
                Assert.fail(listInsertManyChange.toString());
            }

            @Override
            public void visitIdChange(StateNode node, IdChange idChange) {
                Assert.fail(idChange.toString());
            }

            @Override
            public void rangeStartChange(StateNode node,
                    RangeStartChange rangeStartChange) {
                Assert.fail(rangeStartChange.toString());
            }

            @Override
            public void rangeEndChange(StateNode node,
                    RangeEndChange rangeEndChange) {
                Assert.fail(rangeEndChange.toString());
            }

        });
    }

    @Test
    public void testProxyEqualsHashCode() {
        SubModelType value1 = Model.create(SubModelType.class);
        model.setSubValue(value1);
        SubModelType value2 = Model.wrap(node.get("subValue", StateNode.class),
                SubModelType.class);
        SubModelType value3 = Model.create(SubModelType.class);

        Assert.assertEquals(value1, value2);
        Assert.assertNotEquals(value1, value3);

        Assert.assertEquals(value1.hashCode(), value2.hashCode());
        Assert.assertNotEquals(value1.hashCode(), value3.hashCode());

    }

    @Test
    @SuppressWarnings("deprecation")
    public void testModelChangeType() throws RpcInvocationException {
        MockUI ui = new MockUI();
        ui.addComponent(template);
        ui.getRootNode().commit();

        Assert.assertEquals(0, model.getInt());

        JsonObject changeJson = Json.createObject();
        changeJson.put("id", template.getNode().getId());
        changeJson.put("type", "put");
        changeJson.put("key", "int");
        changeJson.put("value", Json.create(42));

        JsonArray parameters = Json.createArray();
        parameters.set(0, changeJson);

        ServerRpcMethodInvocation invocation = new ServerRpcMethodInvocation();
        invocation.setJavaScriptCallbackRpcName("vModelChange");
        invocation.setParameters(parameters);

        ui.getRpcManager(JavaScriptCallbackRpc.class.getName())
                .applyInvocation(invocation);

        Assert.assertEquals(42, model.getInt());
    }

    @Test
    public void testNodeContainsProperties() {
        // Verify that default node doesn't have the properties
        StateNode node = StateNode.create();
        Assert.assertFalse(node.containsKey("int"));
        Assert.assertFalse(node.containsKey("int2"));

        node = StateNode.create(MyTestModel.TYPE);
        Assert.assertTrue(node.containsKey("int"));
        Assert.assertFalse(node.containsKey("int2"));

        Assert.assertEquals("Should be initialized to default primitive value",
                Integer.valueOf(0), node.get("int"));
    }

    @Test
    public void testComputedProperties() {
        Assert.assertEquals(2, model.getServerComptued());
        Assert.assertEquals(-2, model.getJsComputed());

        model.setInt(5);
        model.setBoolean(true);
        Assert.assertEquals(6, model.getServerComptued());
        Assert.assertEquals(4, model.getJsComputed());
    }

    @Test
    public void testComputedListLength() {
        model.setSimpleList(new ArrayList<>());
        Assert.assertEquals(0, model.getServerListLength());
        Assert.assertEquals(0, model.getJsListLength());

        model.setSimpleList(new ArrayList<>(Arrays.asList("asdf")));
        Assert.assertEquals(1, model.getServerListLength());
        Assert.assertEquals(1, model.getJsListLength());

        model.getSimpleList().add("foo");
        Assert.assertEquals(2, model.getServerListLength());
        Assert.assertEquals(2, model.getJsListLength());
    }

    @Test
    public void testJSComputedListIndexOf() {
        model.setSimpleList(new ArrayList<>());
        Assert.assertEquals(-1, model.getJsSimpleListIndexOfA());
        model.setSimpleList(new ArrayList<>(Arrays.asList("a", "b", "c")));
        Assert.assertEquals(0, model.getJsSimpleListIndexOfA());
        model.getSimpleList().add(0, "foo");
        Assert.assertEquals(1, model.getJsSimpleListIndexOfA());
    }

    @Test
    public void testJavaComputedListIndexOf() {
        model.setSimpleList(new ArrayList<>());
        Assert.assertEquals(-1, model.getServerSimpleListIndexOfA());
        model.setSimpleList(new ArrayList<>(Arrays.asList("a", "b", "c")));
        Assert.assertEquals(0, model.getServerSimpleListIndexOfA());
        model.getSimpleList().add(0, "foo");
        Assert.assertEquals(1, model.getServerSimpleListIndexOfA());
    }

    @Test
    public void testModelListInitialization() {
        model.setComplexList(new ArrayList<>());
        Assert.assertNotNull(model.getComplexList());
        Assert.assertEquals(0, model.getComplexList().size());

        model.getComplexList().add(Model.create(SubModelType.class));
        Assert.assertEquals(1, model.getComplexList().size());
    }

    @Test
    public void testModelListInitializationWithPrePopulatedListOfProxies() {
        ArrayList<SubModelType> l = new ArrayList<>();
        l.add(Model.create(SubModelType.class));
        model.setComplexList(l);
        Assert.assertNotNull(model.getComplexList());
        Assert.assertEquals(1, model.getComplexList().size());

        l.clear();
        // The list is no longer connected to the model
        Assert.assertEquals(1, model.getComplexList().size());
    }

}
