package com.vaadin.hummingbird.kernel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.hummingbird.json.JsonStream;
import com.vaadin.hummingbird.kernel.LazyList.DataProvider;
import com.vaadin.server.ServerRpcManager.RpcInvocationException;
import com.vaadin.server.ServerRpcMethodInvocation;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.communication.UidlWriter;
import com.vaadin.ui.JavaScript.JavaScriptCallbackRpc;
import com.vaadin.ui.UI;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;
import elemental.json.impl.JsonUtil;

public class UidlWriterTest {
    private static class UidlWriterTestUI extends UI {
        @Override
        protected void init(VaadinRequest request) {
            // Do nothing
        }
    }

    private Element element;
    private UI ui;

    @Before
    public void setup() {
        ui = new UI() {
            @Override
            protected void init(VaadinRequest request) {
                // Do nothing
            }
        };
        ui.getRootNode().commit();

        element = ui.getElement();
    }

    private JsonArray encodeElementChanges() {
        JsonObject response = Json.createObject();

        UidlWriter.encodeChanges(ui, response);

        return response.getArray("elementChanges");
    }

    @Test
    public void testBooleanAttribute_booleanChangeValue() {
        element.setAttribute("foo", true);

        JsonArray changes = encodeElementChanges();

        Assert.assertEquals(1, changes.length());

        JsonObject change = changes.getObject(0);

        Assert.assertEquals("put", change.getString("type"));
        Assert.assertEquals("foo", change.getString("key"));

        JsonValue value = change.get("value");

        Assert.assertNotNull(value);
        Assert.assertEquals(JsonType.BOOLEAN, value.getType());
        Assert.assertTrue(value.asBoolean());
    }

    @Test
    public void testTemplateOverrideNode() {
        BoundElementTemplate template = TemplateBuilder.withTag("div").build();

        StateNode templateElementNode = StateNode.create();
        Element templateElement = Element.getElement(template,
                templateElementNode);
        element.appendChild(templateElement);
        // Flush setup changes
        encodeElementChanges();

        templateElement.setAttribute("foo", "bar");

        JsonArray changes = encodeElementChanges();

        Assert.assertEquals(4, changes.length());
        int idx = 0;

        JsonObject putOverride = changes.getObject(idx++);
        Assert.assertEquals("putOverride", putOverride.getString("type"));
        Assert.assertEquals(templateElementNode.getId(),
                (int) putOverride.getNumber("id"));

        JsonObject createOverride = changes.getObject(idx++);
        Assert.assertEquals("create", createOverride.getString("type"));

        JsonObject putOverrideTemplate = changes.getObject(idx++);
        Assert.assertEquals("put", putOverrideTemplate.getString("type"));
        Assert.assertEquals("OVERRIDE_TEMPLATE",
                putOverrideTemplate.getString("key"));
        Assert.assertEquals(template.getId(),
                (int) putOverrideTemplate.getNumber("value"));

        JsonObject putAttribute = changes.getObject(idx++);
        Assert.assertEquals("put", putAttribute.getString("type"));
        Assert.assertEquals("foo", putAttribute.getString("key"));
        Assert.assertEquals("bar", putAttribute.getString("value"));
    }

    @Test
    public void testAddRemoveListener() {
        DomEventListener listener = e -> {
        };
        element.addEventListener("bar", listener);
        element.removeEventListener("bar", listener);
        element.addEventListener("bar", listener);

        JsonArray changes = encodeElementChanges();
        Assert.assertEquals(3, changes.length());

        int idx = 0;

        JsonObject addListenerListNode = changes.getObject(idx++);
        Assert.assertEquals("put", addListenerListNode.getString("type"));
        Assert.assertEquals("LISTENERS", addListenerListNode.getString("key"));
        int listNodeId = (int) addListenerListNode.getNumber("listValue");

        JsonObject createListenerListNode = changes.getObject(idx++);
        Assert.assertEquals("create", createListenerListNode.getString("type"));
        Assert.assertEquals(listNodeId,
                (int) createListenerListNode.getNumber("id"));

        JsonObject change = changes.getObject(idx++);
        Assert.assertEquals("splice", change.getString("type"));
        Assert.assertEquals(0, (int) change.getNumber("index"));
        Assert.assertEquals(listNodeId, (int) change.getNumber("id"));
        Assert.assertEquals("bar", change.getArray("value").getString(0));

    }

    @Test
    public void testCreateLazyList() {
        element.getElementDataNode().put("list",
                LazyList.create(new DataProvider<SimpleBean>() {
                    @Override
                    public List<SimpleBean> getValues(int index, int count) {
                        List<SimpleBean> l = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            l.add(new SimpleBean("Value " + index + i));
                        }
                        return l;
                    }

                    @Override
                    public Class<SimpleBean> getType() {
                        return SimpleBean.class;
                    }
                }));

        JsonArray changes = encodeElementChanges();

        Assert.assertEquals(2, changes.length());
        JsonObject change = changes.get(0);
        Assert.assertEquals("put", change.getString("type"));
        Assert.assertEquals("list", change.getString("key"));
        Assert.assertTrue(change.hasKey("listValue"));

        change = changes.get(1);
        Assert.assertEquals("create", change.getString("type"));
        Assert.assertEquals("list", change.getString("nodeType"));
    }

    @Test
    public void changeLazyListRangeEnd() {
        LazyList list = LazyList.create(new DataProvider<SimpleBean>() {
            @Override
            public List<SimpleBean> getValues(int index, int count) {
                List<SimpleBean> l = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    l.add(new SimpleBean("Value " + (index + i)));
                }
                return l;
            }

            @Override
            public Class<SimpleBean> getType() {
                return SimpleBean.class;
            }
        });
        element.getElementDataNode().put("list", list);

        list.setActiveRangeEnd(10); // 0-0 -> 0-10
        JsonArray changes = encodeElementChanges();
        System.out.println(JsonUtil.stringify(changes, 2));

        // put list + create list + RangeEnd + listInsert + 10 data nodes
        // (create + list insert)
        Assert.assertEquals(1 + 1 + 1 + 1 + 10 * 2, changes.length());
        // Range end + data through list insert for 0
        int idx = 0;
        JsonObject putList = changes.getObject(idx++);
        Assert.assertEquals("put", putList.getString("type"));
        Assert.assertEquals("list", putList.getString("key"));
        int listId = (int) putList.getNumber("listValue");

        JsonObject create = changes.getObject(idx++);
        Assert.assertEquals("create", create.getString("type"));
        Assert.assertEquals(listId, (int) create.getNumber("id"));

        JsonObject rangeEnd = changes.getObject(idx++);
        Assert.assertEquals("rangeEnd", rangeEnd.getString("type"));
        Assert.assertEquals(listId, (int) rangeEnd.getNumber("id"));

        JsonObject data = changes.getObject(idx++);
        Assert.assertEquals("splice", data.getString("type"));
        Assert.assertEquals(0, (int) data.getNumber("index"));

        // JsonArray values = Json.createArray();
        ArrayList<Integer> nodeIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            nodeIds.add((int) data.getArray("mapValue").getNumber(i));
        }

        Set<Integer> seenPut = new HashSet<>();
        Set<Integer> seenCreate = new HashSet<>();

        for (int i = 0; i < 10 * 2; i++) {
            JsonObject change = changes.getObject(idx++);
            int id = (int) change.getNumber("id");
            String type = change.getString("type");

            Set<Integer> seenSet;
            if ("put".equals(type)) {
                seenSet = seenPut;

                Assert.assertEquals("value", change.getString("key"));
            } else if ("create".equals(type)) {
                seenSet = seenCreate;

                Assert.assertEquals("map", change.getString("nodeType"));
            } else {
                throw new RuntimeException(
                        "Unexpected change type: " + change.toJson());
            }

            boolean newChange = seenSet.add(id);
            Assert.assertTrue("Change already seen: " + change.toJson(),
                    newChange);
        }
    }

    // @Test
    // public void changeLazyListCases() {
    // Assert.fail("List 10-20, change to 15-30");
    // Assert.fail("List 10-20, change to 5-30");
    // Assert.fail("List 10-20, change to 0-5");
    //
    // Assert.fail("List 10-20, change to 10-15");
    // Assert.fail("List 10-20, change to 10-25");
    // Assert.fail("List 10-20, change to 25-30");
    // }

    @Test
    public void testDateInNode() {
        long timestamp = 1445855880l;
        element.getNode().put("date", new Date(timestamp));

        JsonArray json = encodeElementChanges();
        Assert.assertEquals(1, json.length());

        JsonObject putChange = json.getObject(0);
        Assert.assertEquals("put", putChange.getString("type"));
        Assert.assertEquals(timestamp, (long) putChange.getNumber("value"));
    }

    @Test
    public void clientSideModelChangeEmptyReponse()
            throws RpcInvocationException {
        Assert.assertNull("Should be no attribute by default",
                element.getAttribute("foo"));

        JsonObject changeJson = Json.createObject();
        changeJson.put("id", element.getNode().getId());
        changeJson.put("type", "put");
        changeJson.put("key", "foo");
        changeJson.put("value", Json.create("bar"));

        JsonArray parameters = Json.createArray();
        parameters.set(0, changeJson);

        ServerRpcMethodInvocation invocation = new ServerRpcMethodInvocation();
        invocation.setJavaScriptCallbackRpcName("vModelChange");
        invocation.setParameters(parameters);

        ui.getRpcManager(JavaScriptCallbackRpc.class.getName())
                .applyInvocation(invocation);

        Assert.assertEquals("Tree should be updated", "bar",
                element.getAttribute("foo"));

        JsonArray json = encodeElementChanges();
        Assert.assertEquals("There should be no changes", 0, json.length());
    }

    @Test
    public void clientComputedProperty_dontSentValue() {
        ComputedProperty property = new ComputedProperty("computedJs", "") {
            @Override
            public Object compute(StateNode context) {
                return context.get("value", String.class).toUpperCase();
            }
        };
        element.getNode()
                .setComputedProperties(ComputedPropertyTest.createMap(property,
                        ComputedPropertyTest.createComputedProperty(
                                "computedJava", property::compute)));

        element.setAttribute("value", "myValue");

        Assert.assertEquals("Computed property should return uppercase string",
                "MYVALUE", element.getAttribute("computedJs"));
        Assert.assertEquals("Computed property should return uppercase string",
                "MYVALUE", element.getAttribute("computedJava"));

        JsonArray changes = encodeElementChanges();
        Assert.assertEquals("There should only be one change", 2,
                changes.length());

        Set<String> affectedKeys = JsonStream.objectStream(changes)
                .map(o -> o.getString("key")).collect(Collectors.toSet());

        Assert.assertEquals(
                new HashSet<>(Arrays.asList("computedJava", "value")),
                affectedKeys);
    }

}
