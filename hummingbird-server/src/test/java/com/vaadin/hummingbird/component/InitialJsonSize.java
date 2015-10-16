package com.vaadin.hummingbird.component;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.TransactionLogBuilderTest;
import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.communication.TransactionLogJsonProducer;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;
import elemental.json.impl.JsonUtil;

public class InitialJsonSize {

    public static class TestUI extends UI {

        @Override
        protected void init(VaadinRequest request) {
        }

    }

    private TestUI ui;

    @Before
    public void setup() {
        ui = new TestUI();
    }

    @Test
    public void initialJsonTwoButtons() {
        for (int i = 0; i < 2; i++) {
            final int j = i;
            Button b = new Button("Button " + i);
            b.addClickListener(e -> {
                System.out.println("Click on button " + j);
            });
            ui.addComponent(b);
        }

        String expectedJsonString = "[\n" + "    {\n" + "        \"id\": 1,\n"
                + "        \"type\": \"putNode\",\n"
                + "        \"key\": \"containerElement\",\n"
                + "        \"value\": 2\n" + "    },\n" + "    {\n"
                + "        \"id\": 2,\n" + "        \"type\": \"put\",\n"
                + "        \"key\": \"TAG\",\n" + "        \"value\": \"div\"\n"
                + "    },\n" + "    {\n" + "        \"id\": 2,\n"
                + "        \"type\": \"listInsertNode\",\n"
                + "        \"key\": \"CHILDREN\",\n" + "        \"index\": 0,\n"
                + "        \"value\": 3\n" + "    },\n" + "    {\n"
                + "        \"id\": 3,\n" + "        \"type\": \"put\",\n"
                + "        \"key\": \"TAG\",\n" + "        \"value\": \"div\"\n"
                + "    },\n" + "    {\n" + "        \"id\": 3,\n"
                + "        \"type\": \"put\",\n"
                + "        \"key\": \"class\",\n"
                + "        \"value\": \"layer ui v-scrollable\"\n" + "    },\n"
                + "    {\n" + "        \"id\": 3,\n"
                + "        \"type\": \"put\",\n"
                + "        \"key\": \"style\",\n"
                + "        \"value\": \"z-index:1;width:100.0%;height:100.0%\"\n"
                + "    },\n" + "    {\n" + "        \"id\": 3,\n"
                + "        \"type\": \"listInsertNodes\",\n"
                + "        \"key\": \"CHILDREN\",\n" + "        \"index\": 0,\n"
                + "        \"value\": [\n" + "            4,\n"
                + "            8\n" + "        ]\n" + "    },\n" + "    {\n"
                + "        \"id\": 4,\n" + "        \"type\": \"put\",\n"
                + "        \"key\": \"TAG\",\n"
                + "        \"value\": \"button\"\n" + "    },\n" + "    {\n"
                + "        \"id\": 4,\n"
                + "        \"type\": \"listInsertNode\",\n"
                + "        \"key\": \"CHILDREN\",\n" + "        \"index\": 0,\n"
                + "        \"value\": 7\n" + "    },\n" + "    {\n"
                + "        \"id\": 4,\n" + "        \"type\": \"putNode\",\n"
                + "        \"key\": \"EVENT_DATA\",\n"
                + "        \"value\": 6\n" + "    },\n" + "    {\n"
                + "        \"id\": 4,\n" + "        \"type\": \"listInsert\",\n"
                + "        \"key\": \"LISTENERS\",\n"
                + "        \"index\": 0,\n" + "        \"value\": \"click\"\n"
                + "    },\n" + "    {\n" + "        \"id\": 6,\n"
                + "        \"type\": \"listInserts\",\n"
                + "        \"key\": \"click\",\n" + "        \"index\": 0,\n"
                + "        \"value\": [\n" + "            \"event.button\",\n"
                + "            \"event.clientX\",\n"
                + "            \"event.clientY\",\n"
                + "            \"event.type\",\n"
                + "            \"event.altKey\",\n"
                + "            \"event.metaKey\",\n"
                + "            \"event.ctrlKey\",\n"
                + "            \"event.shiftKey\",\n"
                + "            \"event.relativeX\",\n"
                + "            \"event.relativeY\"\n" + "        ]\n"
                + "    },\n" + "    {\n" + "        \"id\": 7,\n"
                + "        \"type\": \"put\",\n" + "        \"key\": \"TAG\",\n"
                + "        \"value\": \"#text\"\n" + "    },\n" + "    {\n"
                + "        \"id\": 7,\n" + "        \"type\": \"put\",\n"
                + "        \"key\": \"content\",\n"
                + "        \"value\": \"Button 0\"\n" + "    },\n" + "    {\n"
                + "        \"id\": 8,\n" + "        \"type\": \"put\",\n"
                + "        \"key\": \"TAG\",\n"
                + "        \"value\": \"button\"\n" + "    },\n" + "    {\n"
                + "        \"id\": 8,\n"
                + "        \"type\": \"listInsertNode\",\n"
                + "        \"key\": \"CHILDREN\",\n" + "        \"index\": 0,\n"
                + "        \"value\": 11\n" + "    },\n" + "    {\n"
                + "        \"id\": 8,\n" + "        \"type\": \"putNode\",\n"
                + "        \"key\": \"EVENT_DATA\",\n"
                + "        \"value\": 10\n" + "    },\n" + "    {\n"
                + "        \"id\": 8,\n" + "        \"type\": \"listInsert\",\n"
                + "        \"key\": \"LISTENERS\",\n"
                + "        \"index\": 0,\n" + "        \"value\": \"click\"\n"
                + "    },\n" + "    {\n" + "        \"id\": 10,\n"
                + "        \"type\": \"listInserts\",\n"
                + "        \"key\": \"click\",\n" + "        \"index\": 0,\n"
                + "        \"value\": [\n" + "            \"event.button\",\n"
                + "            \"event.clientX\",\n"
                + "            \"event.clientY\",\n"
                + "            \"event.type\",\n"
                + "            \"event.altKey\",\n"
                + "            \"event.metaKey\",\n"
                + "            \"event.ctrlKey\",\n"
                + "            \"event.shiftKey\",\n"
                + "            \"event.relativeX\",\n"
                + "            \"event.relativeY\"\n" + "        ]\n"
                + "    },\n" + "    {\n" + "        \"id\": 11,\n"
                + "        \"type\": \"put\",\n" + "        \"key\": \"TAG\",\n"
                + "        \"value\": \"#text\"\n" + "    },\n" + "    {\n"
                + "        \"id\": 11,\n" + "        \"type\": \"put\",\n"
                + "        \"key\": \"content\",\n"
                + "        \"value\": \"Button 1\"\n" + "    }\n" + "]";
        JsonArray json = getJson(ui);

        int jsonSize = json.toJson().length();
        JsonValue expectedJson = Json.instance().parse(expectedJsonString);
        int expectedSize = expectedJson.toJson().length();

        // Assert.assertEquals(JsonUtil.stringify(expectedJson, 4),
        // JsonUtil.stringify(json, 4));

        Assert.assertTrue("JSON is larger (" + jsonSize + ") than expected ("
                + expectedSize + ")", jsonSize <= expectedSize);
        if (jsonSize < expectedSize) {
            System.out.println("Generated JSON was smaller (" + jsonSize
                    + ") than expected (" + expectedSize + ")");
            System.out.println("Generated JSON");
            System.out.println(JsonUtil.stringify(json, 4));
        }
    }

    private static JsonArray getJson(TestUI ui) {
        LinkedHashMap<StateNode, List<NodeChange>> log = TransactionLogBuilderTest
                .getTransactionLog(ui.getRoot().getRootNode());
        LinkedHashMap<StateNode, List<NodeChange>> optimized = TransactionLogBuilderTest
                .getOptimizedTransactionLog(log);
        return new TransactionLogJsonProducer(ui, optimized, new HashSet<>())
                .getChangesJson();
    }
}
