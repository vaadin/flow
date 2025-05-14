/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.client.flow;

import com.google.gwt.core.client.JavaScriptObject;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.InitialPropertiesHandler;
import com.vaadin.client.Registry;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.communication.ServerConnector;
import com.vaadin.client.flow.binding.ServerEventObject;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class GwtStateTreeTest extends ClientEngineTestBase {

    private StateTree tree;
    private Registry registry;

    private static class TestServerConnector extends ServerConnector {

        private StateNode node;
        private String methodName;
        private JsonArray args;

        private TestServerConnector(Registry registry) {
            super(registry);
        }

        @Override
        public void sendTemplateEventMessage(StateNode node, String methodName,
                JsonArray array, int promiseId) {
            this.node = node;
            this.methodName = methodName;
            args = array;
        }
    }

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();

        registry = new Registry() {

            {
                set(ServerConnector.class, new TestServerConnector(this));
                set(InitialPropertiesHandler.class,
                        new InitialPropertiesHandler(this));
            }

        };

        tree = new StateTree(registry);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testSendTemplateEventToServer_delegateToServerConnector() {
        StateNode node = new StateNode(0, tree);
        tree.registerNode(node);
        JsArray array = getArgArray();
        tree.sendTemplateEventToServer(node, "foo", array, -1);

        JsonObject object = Json.createObject();
        object.put("key", "value");
        array.set(array.length(), WidgetUtil.crazyJsCast(object));
        array.set(array.length(), getNativeArray());

        TestServerConnector serverConnector = (TestServerConnector) registry
                .getServerConnector();
        assertEquals(node, serverConnector.node);
        assertEquals("foo", serverConnector.methodName);
        JsonArray args = serverConnector.args;
        assertEquals(true, args.getBoolean(0));
        assertEquals("bar", args.getString(1));
        assertEquals(46.2, args.getNumber(2));
        assertEquals(object, args.getObject(3));

        assertEquals("item", ((JsonArray) args.getObject(4)).getString(0));
    }

    public void testDeferredTemplateMessage_isIgnored() {
        StateNode node = new StateNode(0, tree);
        tree.registerNode(node);

        Reactive.addPostFlushListener(() -> {
            tree.sendTemplateEventToServer(node, "click", null, -1);
            TestServerConnector serverConnector = (TestServerConnector) registry
                    .getServerConnector();
            assertNull(
                    "Found node even though message should not have been sent.",
                    serverConnector.node);
            assertNull(
                    "Found methodName even though message should not have been sent.",
                    serverConnector.methodName);
            assertNull(
                    "Found arguments even though message should not have been sent.",
                    serverConnector.args);
        });

        tree.unregisterNode(node);
        Reactive.flush();
    }

    public void testPrepareForResync_unregistersDescendantsAndClearsRootChildren() {
        // given
        StateNode root = tree.getRootNode();
        StateNode child = new StateNode(2, tree);
        child.setParent(root);
        tree.registerNode(child);
        root.getList(NodeFeatures.VIRTUAL_CHILDREN).add(0,child);

        StateNode grandChild = new StateNode(3, tree);
        grandChild.setParent(child);
        tree.registerNode(grandChild);
        child.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, grandChild);

        // when
        tree.prepareForResync();

        // then
        assertTrue(!root.isUnregistered());
        assertEquals(0, root.getList(NodeFeatures.VIRTUAL_CHILDREN).length());
        assertTrue(child.isUnregistered());
        assertEquals(0, child.getList(NodeFeatures.ELEMENT_CHILDREN).length());
        assertTrue(grandChild.isUnregistered());
    }

    public void testPrepareForResync_rejectsPendingPromise() {
        // given
        StateNode root = tree.getRootNode();
        StateNode child = new StateNode(2, tree);
        child.setParent(root);
        tree.registerNode(child);
        root.getList(NodeFeatures.VIRTUAL_CHILDREN).add(0,child);

        final DivElement element = Browser.getDocument().createDivElement();
        child.setDomNode(element);
        ServerEventObject.get(element);
        createMockPromise(element);

        // when
        tree.prepareForResync();

        // then
        assertFalse(getMockPromiseResult(element));
    }

    private native JsArray<JavaScriptObject> getArgArray()
    /*-{
        return [ true, "bar", 46.2];
     }-*/;

    private native JsArray<JavaScriptObject> getNativeArray()
    /*-{
        return [ "item" ];
     }-*/;

    private static native boolean createMockPromise(Element element)
    /*-{
       var eventObject = element.$server["}p"];
       eventObject.promiseResult = null;
       eventObject.promises[0] = [function() {
           eventObject.promiseResult = true;
       },function() {
           eventObject.promiseResult = false;
       }];
    }-*/;

    private static native boolean getMockPromiseResult(Element element)
    /*-{
        return element.$server["}p"].promiseResult;
    }-*/;
}
