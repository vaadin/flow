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
package com.vaadin.client.hummingbird;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.Registry;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.binding.Binder;
import com.vaadin.client.hummingbird.binding.ServerEventObject;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.client.hummingbird.util.NativeFunction;
import com.vaadin.hummingbird.shared.NodeFeatures;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * @author Vaadin Ltd
 *
 */
public class GwtEventHandlerTest extends ClientEngineTestBase {

    private StateNode node;

    private Element element;

    private StateTree tree;

    private Registry registry;

    private Map<String, JsonObject> serverMethods = new HashMap<>();
    private Map<String, StateNode> serverRpcNodes = new HashMap<>();

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        Reactive.reset();

        registry = new Registry() {
            {
                set(ConstantPool.class, new ConstantPool());
            }
        };

        tree = new StateTree(registry) {

            @Override
            public void sendTemplateEventToServer(StateNode node,
                    String methodName, JsArray<?> argValues) {
                serverMethods.put(methodName,
                        WidgetUtil.crazyJsCast(argValues));
                serverRpcNodes.put(methodName, node);
            }

            @Override
            public void sendEventToServer(StateNode node, String methodName,
                    JsonObject eventData) {
                serverMethods.put(methodName, eventData);
                serverRpcNodes.put(methodName, node);
            }
        };

        node = new StateNode(0, tree);
        // populate "element data" feature to be able to bind node as a plain
        // element
        node.getMap(NodeFeatures.ELEMENT_DATA);
        element = Browser.getDocument().createElement("div");
    }

    public void testNoServerEventHandler_nothingInDom() {
        Binder.bind(node, element);
        Reactive.flush();
        assertNull(WidgetUtil.getJsProperty(element, "$server"));
    }

    public void testServerEventHandlerMethodInDom() {
        assertServerEventHandlerMethodInDom(
                NodeFeatures.PUBLISHED_SERVER_EVENT_HANDLERS,
                element -> assertPublishedMethods(element,
                        new String[] { "publishedMethod" }),
                "publishedMethod");
    }

    public void testPolymerServerEventHandlerMethodInDom() {
        assertServerEventHandlerMethodInDom(
                NodeFeatures.ELEMENT_SERVER_EVENT_HANDLERS,
                element -> assertPolymerMethods(element,
                        new String[] { "eventHandler" }),
                "eventHandler");
    }

    public void testAddServerEventHandlerMethod() {
        assertAddServerEventHandlerMethod(
                NodeFeatures.PUBLISHED_SERVER_EVENT_HANDLERS,
                this::assertPublishedMethods);
    }

    public void testAddPolymerServerEventHandlerMethod() {
        assertAddServerEventHandlerMethod(
                NodeFeatures.ELEMENT_SERVER_EVENT_HANDLERS,
                this::assertPolymerMethods);
    }

    public void testRemoveServerEventHandlerMethod() {
        assertRemoveServerEventHandlerMethod(
                NodeFeatures.PUBLISHED_SERVER_EVENT_HANDLERS,
                this::assertPublishedMethods);
    }

    public void testRemovePolymerServerEventHandlerMethod() {
        assertRemoveServerEventHandlerMethod(
                NodeFeatures.ELEMENT_SERVER_EVENT_HANDLERS,
                this::assertPolymerMethods);
    }

    public void testPolymerMockedEventHandler() {
        String methodName = "eventHandler";
        node.getList(NodeFeatures.ELEMENT_SERVER_EVENT_HANDLERS).add(0,
                methodName);
        Binder.bind(node, element);
        Reactive.flush();

        NativeFunction mockedFunction = new NativeFunction(
                "this." + methodName + "()");
        mockedFunction.apply(element, JsCollections.array());

        assertEquals(1, serverMethods.size());
        assertEquals(methodName, serverMethods.keySet().iterator().next());
        assertEquals(0, serverMethods.get(methodName).keys().length);
        assertEquals(node, serverRpcNodes.get(methodName));
    }

    public void testPolymerMockedEventHandlerWithEventData() {
        String methodName = "eventHandler";
        String methodId = "handlerId";
        String eventData = "event.button";

        node.getList(NodeFeatures.ELEMENT_SERVER_EVENT_HANDLERS).add(0,
                methodName);
        node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty(methodName)
                .setValue(methodId);

        JsonObject json = Json.createObject();
        JsonArray array = Json.createArray();
        array.set(0, eventData);
        json.put(methodId, array);

        node.getTree().getRegistry().getConstantPool().importFromJson(json);
        Binder.bind(node, element);
        Reactive.flush();

        NativeFunction mockedFunction = new NativeFunction("this." + methodName
                + "({button: 2, altKey: false, clientX: 50, clientY: 100})");
        mockedFunction.apply(element, JsCollections.array());

        assertEquals("The amount of server methods was not as expected", 1,
                serverMethods.size());
        assertEquals("Expected method did not match", methodName,
                serverMethods.keySet().iterator().next());
        assertEquals("Wrong amount of method arguments", 1,
                serverMethods.get(methodName).keys().length);
        assertEquals("Gotten argument wasn't as expected", "2",
                serverMethods.get(methodName).get(eventData).toString());
        assertEquals("Method node did not match the expected node.", node,
                serverRpcNodes.get(methodName));
    }

    public void testPolymerMockedEventHandlerWithDefaultImplementation() {
        String methodName = "eventHandler";
        String methodId = "handlerId";
        String eventData = "event.button";
        String eventData2 = "event.result";
        String extraArgument = "Extra String argument";

        node.getList(NodeFeatures.ELEMENT_SERVER_EVENT_HANDLERS).add(0,
                methodName);
        node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty(methodName)
                .setValue(methodId);

        JsonObject json = Json.createObject();
        JsonArray array = Json.createArray();
        array.set(0, eventData);
        array.set(1, eventData2);
        json.put(methodId, array);

        node.getTree().getRegistry().getConstantPool().importFromJson(json);

        Binder.bind(node, element);
        Reactive.flush();

        setPrototypeEventHandler(element, methodName);

        NativeFunction mockedFunction = new NativeFunction("this." + methodName
                + "({button: 2}, '" + extraArgument + "')");
        mockedFunction.apply(element, JsCollections.array());

        JsonObject result = Json.createObject();
        result.put("event.button", 2);
        result.put("event.result", extraArgument);

        assertEquals("The amount of server methods was not as expected", 1,
                serverMethods.size());
        assertEquals("Expected method did not match", methodName,
                serverMethods.keySet().iterator().next());
        assertEquals("Wrong amount of method arguments", result.keys().length,
                serverMethods.get(methodName).keys().length);
        assertEquals("Gotten argument wasn't as expected",
                WidgetUtil.toPrettyJson(result),
                WidgetUtil.toPrettyJson(serverMethods.get(methodName)));
        assertEquals("Method node did not match the expected node.", node,
                serverRpcNodes.get(methodName));
    }

    /**
     * Add a function to the element prototype ("default" function) for
     * {@code methodName} that adds the second argument to the event (first
     * argument) as a result
     * 
     * @param element
     *            Element to add "default" method to
     * @param methodName
     *            Name of event to add method to
     */
    private native void setPrototypeEventHandler(Element element,
            String methodName)
    /*-{
        Object.getPrototypeOf(element)[methodName] = function(event) {
            if(this !== element) {
                throw "This and target element didn't match";
            };
            event.result = arguments[1];
        }
    }-*/;

    private void assertServerEventHandlerMethodInDom(int id,
            Consumer<Element> assertMethod, String method) {
        node.getList(id).add(0, method);
        Binder.bind(node, element);
        Reactive.flush();
        assertMethod.accept(element);
    }

    private void assertAddServerEventHandlerMethod(int id,
            BiConsumer<Element, String[]> assertMethod) {
        node.getList(id).add(0, "initialMethod");
        Binder.bind(node, element);
        Reactive.flush();
        assertMethod.accept(element, new String[] { "initialMethod" });
        node.getList(id).add(0, "newFirstMethod");
        assertMethod.accept(element,
                new String[] { "initialMethod", "newFirstMethod" });
    }

    private void assertRemoveServerEventHandlerMethod(int id,
            BiConsumer<Element, String[]> assertMethod) {
        node.getList(id).add(0, "method1");
        node.getList(id).add(1, "method2");
        node.getList(id).add(2, "method3");
        Binder.bind(node, element);
        assertMethod.accept(element,
                new String[] { "method1", "method2", "method3" });
        node.getList(id).splice(1, 2);
        assertMethod.accept(element, new String[] { "method1" });
        node.getList(id).add(0, "new1");
        node.getList(id).add(2, "new2");
        assertMethod.accept(element,
                new String[] { "new1", "method1", "new2" });
        JsArray<String> insert = JsCollections.array();
        insert.push("foo");
        insert.push("bar");
        node.getList(id).splice(0, 1, insert);
        assertMethod.accept(element,
                new String[] { "foo", "bar", "method1", "new2" });
    }

    private JsArray<String> getPublishedServerMethods(Element element) {
        ServerEventObject serverEventObject = WidgetUtil
                .crazyJsoCast(WidgetUtil.getJsProperty(element, "$server"));
        if (serverEventObject == null) {
            return JsCollections.array();
        } else {
            return serverEventObject.getMethods();
        }
    }

    private JsArray<String> getPublishedServerMethods(
            ServerEventObject object) {
        if (object == null) {
            return JsCollections.array();
        } else {
            return object.getMethods();
        }
    }

    private void assertPublishedMethods(Element element, String[] expected) {
        assertEventHandlerMethods(() -> getPublishedServerMethods(element),
                expected);
    }

    private void assertPolymerMethods(Element element, String[] expected) {
        ServerEventObject object = WidgetUtil.crazyJsoCast(element);
        assertEventHandlerMethods(() -> getPublishedServerMethods(object),
                expected);
    }

    private void assertEventHandlerMethods(
            Supplier<JsArray<String>> methodsProvider, String... expected) {
        JsArray<String> publishedServerMethods = methodsProvider.get();
        assertEquals(expected.length, publishedServerMethods.length());
        for (int i = 0; i < expected.length; i++) {
            assertTrue("$server does not contain " + expected[i],
                    publishedServerMethods.remove(expected[i]));
        }
        assertTrue(publishedServerMethods.isEmpty());
    }
}
