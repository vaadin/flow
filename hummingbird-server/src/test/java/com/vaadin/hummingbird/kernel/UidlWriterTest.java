package com.vaadin.hummingbird.kernel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.vaadin.hummingbird.kernel.LazyList.DataProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.communication.TransactionLogBuilder;
import com.vaadin.server.communication.TransactionLogJsonProducer;
import com.vaadin.server.communication.TransactionLogOptimizer;
import com.vaadin.ui.UI;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
        ui.getRoot().getRootNode().commit();

        element = ui.getElement();
    }

    private JsonArray encodeElementChanges() {
        TransactionLogBuilder logBuilder = new TransactionLogBuilder();
        ui.getRoot().getRootNode().commit(logBuilder.getVisitor());

        TransactionLogOptimizer optimizer = new TransactionLogOptimizer(ui,
                logBuilder.getChanges(), logBuilder.getTemplates());

        TransactionLogJsonProducer jsonProducer = new TransactionLogJsonProducer(
                ui, optimizer.getChanges(), optimizer.getTemplates());

        return jsonProducer.getChangesJson();
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

        Assert.assertEquals(3, changes.length());
        int idx = 0;

        JsonObject putOverride = changes.getObject(idx++);
        Assert.assertEquals("putOverride", putOverride.getString("type"));
        Assert.assertEquals(templateElementNode.getId(),
                (int) putOverride.getNumber("id"));

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
        Assert.assertEquals(2, changes.length());

        int idx = 0;

        JsonObject addListenerListNode = changes.getObject(idx++);
        Assert.assertEquals("putListNode",
                addListenerListNode.getString("type"));
        Assert.assertEquals("LISTENERS", addListenerListNode.getString("key"));
        int listNodeId = (int) addListenerListNode.getNumber("value");

        JsonObject change = changes.getObject(idx++);
        Assert.assertEquals("listInsert", change.getString("type"));
        Assert.assertEquals(0, (int) change.getNumber("index"));
        Assert.assertEquals(listNodeId, (int) change.getNumber("id"));
        Assert.assertEquals("bar", change.getString("value"));

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

        Assert.assertEquals(1, changes.length());
        JsonObject change = changes.get(0);
        Assert.assertEquals("putListNode", change.getString("type"));
        Assert.assertEquals("list", change.getString("key"));

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

        // create list + RangeEnd + listInsert + 10 data nodes
        Assert.assertEquals(1 + 1 + 1 + 10, changes.length());
        // Ramge end + data through list insert for 0
        int idx = 0;
        JsonObject putList = changes.getObject(idx++);
        Assert.assertEquals("putListNode", putList.getString("type"));
        Assert.assertEquals("list", putList.getString("key"));
        int listId = (int) putList.getNumber("value");

        JsonObject rangeEnd = changes.getObject(idx++);
        Assert.assertEquals("rangeEnd", rangeEnd.getString("type"));
        Assert.assertEquals(listId, (int) rangeEnd.getNumber("id"));

        JsonObject data = changes.getObject(idx++);
        Assert.assertEquals("listInsertNodes", data.getString("type"));
        Assert.assertEquals(0, (int) data.getNumber("index"));

        // JsonArray values = Json.createArray();
        ArrayList<Integer> nodeIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            nodeIds.add((int) data.getArray("value").getNumber(i));
        }

        for (int i = 0; i < 10; i++) {
            JsonObject nodePut = changes.getObject(idx++);
            Assert.assertEquals(nodeIds.get(i), (int) nodePut.getNumber("id"),
                    0);
            Assert.assertEquals("put", nodePut.getString("type"));
            Assert.assertEquals("value", nodePut.getString("key"));
            Assert.assertEquals("Value " + i, nodePut.getString("value"));
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

}
