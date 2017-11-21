/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.server.communication.rpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import org.junit.Test;

import com.vaadin.flow.JsonCodec;
import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.nodefeature.ElementPropertyMap;
import com.vaadin.flow.nodefeature.ModelList;
import com.vaadin.flow.nodefeature.NodeFeatureRegistry;
import com.vaadin.shared.JsonConstants;
import com.vaadin.ui.ComponentTest.TestComponent;
import com.vaadin.ui.UI;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class MapSyncRpcHandlerTest {

    private static final String NEW_VALUE = "newValue";
    private static final String DUMMY_EVENT = "dummy-event";
    private static final String TEST_PROPERTY = "test-property";

    @Test
    public void testSynchronizeProperty() throws Exception {
        TestComponent c = new TestComponent();
        Element element = c.getElement();
        UI ui = new UI();
        ui.add(c);
        assertFalse(element.hasProperty(TEST_PROPERTY));
        sendSynchronizePropertyEvent(element, ui, TEST_PROPERTY, "value1");
        assertEquals("value1", element.getPropertyRaw(TEST_PROPERTY));
        sendSynchronizePropertyEvent(element, ui, TEST_PROPERTY, "value2");
        assertEquals("value2", element.getPropertyRaw(TEST_PROPERTY));
    }

    @Test
    public void syncJSON_jsonIsForStateNodeInList_propertySetToStateNodeCopy()
            throws Exception {
        // Let's use element's ElementPropertyMap for testing.
        TestComponent component = new TestComponent();
        Element element = component.getElement();
        UI ui = new UI();
        ui.add(component);

        StateNode node = element.getNode();

        // Set model value directly via ElementPropertyMap
        ElementPropertyMap propertyMap = node
                .getFeature(ElementPropertyMap.class);
        ModelList modelList = propertyMap.resolveModelList("foo");
        // fake StateNode has been created for the model
        StateNode item = new StateNode(ElementPropertyMap.class);
        modelList.add(item);
        item.getFeature(ElementPropertyMap.class).setProperty("bar", "baz");

        // Use the model node id for JSON object which represents a value to
        // update
        JsonObject json = Json.createObject();
        json.put("nodeId", item.getId());

        // send sync request
        sendSynchronizePropertyEvent(element, ui, TEST_PROPERTY, json);

        // Now the model node should be copied and available as the
        // TEST_PROPERTY value
        Serializable testPropertyValue = propertyMap.getProperty(TEST_PROPERTY);

        assertTrue(testPropertyValue instanceof StateNode);

        StateNode newNode = (StateNode) testPropertyValue;
        assertNotEquals(item.getId(), newNode.getId());

        assertEquals("baz", newNode.getFeature(ElementPropertyMap.class)
                .getProperty("bar"));
    }

    @Test
    public void syncJSON_jsonIsPropertyValueOfStateNode_propertySetToNode()
            throws Exception {
        // Let's use element's ElementPropertyMap for testing.
        TestComponent component = new TestComponent();
        Element element = component.getElement();
        UI ui = new UI();
        ui.add(component);

        StateNode node = element.getNode();

        // Set model value directly via ElementPropertyMap
        ElementPropertyMap propertyMap = node
                .getFeature(ElementPropertyMap.class);
        ElementPropertyMap modelMap = propertyMap.resolveModelMap("foo");
        // fake StateNode has been created for the model
        StateNode model = modelMap.getNode();
        modelMap.setProperty("bar", "baz");

        // Use the model node id for JSON object which represents a value to
        // update
        JsonObject json = Json.createObject();
        json.put("nodeId", model.getId());

        // send sync request
        sendSynchronizePropertyEvent(element, ui, "foo", json);

        Serializable testPropertyValue = propertyMap.getProperty("foo");

        assertTrue(testPropertyValue instanceof StateNode);

        StateNode newNode = (StateNode) testPropertyValue;
        assertSame(model, newNode);
    }

    @Test
    public void syncJSON_jsonIsNotListItemAndNotPropertyValue_propertySetToJSON()
            throws Exception {
        // Let's use element's ElementPropertyMap for testing.
        TestComponent component = new TestComponent();
        Element element = component.getElement();
        UI ui = new UI();
        ui.add(component);

        StateNode node = element.getNode();

        TestComponent anotherComonent = new TestComponent();
        StateNode anotherNode = anotherComonent.getElement().getNode();

        // Use the model node id for JSON object which represents a value to
        // update
        JsonObject json = Json.createObject();
        json.put("nodeId", anotherNode.getId());

        // send sync request
        sendSynchronizePropertyEvent(element, ui, "foo", json);

        Serializable testPropertyValue = node
                .getFeature(ElementPropertyMap.class).getProperty("foo");

        assertNotSame(anotherNode, testPropertyValue);
        assertTrue(testPropertyValue instanceof JsonValue);
    }

    private static void sendSynchronizePropertyEvent(Element element, UI ui,
            String eventType, Serializable value) throws Exception {
        new MapSyncRpcHandler().handle(ui,
                createSyncPropertyInvocation(element, eventType, value));
    }

    private static JsonObject createSyncPropertyInvocation(Element element,
            String property, Serializable value) {
        StateNode node = EventRpcHandlerTest.getInvocationNode(element);
        // Copied from ServerConnector
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_NODE, node.getId());
        message.put(JsonConstants.RPC_FEATURE,
                NodeFeatureRegistry.getId(ElementPropertyMap.class));
        message.put(JsonConstants.RPC_PROPERTY, property);
        message.put(JsonConstants.RPC_PROPERTY_VALUE,
                JsonCodec.encodeWithoutTypeInfo(value));

        return message;
    }
}
