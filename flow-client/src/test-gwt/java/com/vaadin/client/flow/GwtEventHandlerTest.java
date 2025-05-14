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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.client.ApplicationConfiguration;
import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.Registry;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.binding.Binder;
import com.vaadin.client.flow.binding.ServerEventObject;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.client.flow.util.NativeFunction;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;

import elemental.client.Browser;
import elemental.dom.Element;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class GwtEventHandlerTest extends ClientEngineTestBase {

    private StateNode node;

    private Element element;

    private StateTree tree;

    private Registry registry;

    private Map<String, JsArray<?>> serverMethods = new HashMap<>();
    private Map<String, StateNode> serverRpcNodes = new HashMap<>();
    private Map<String, Integer> serverPromiseIds = new HashMap<>();

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        restorePromiseImmediateFn();
        Reactive.reset();

        registry = new Registry() {
            {
                set(ConstantPool.class, new ConstantPool());
                set(ApplicationConfiguration.class,
                        new ApplicationConfiguration());
            }
        };

        tree = new StateTree(registry) {

            @Override
            public void sendTemplateEventToServer(StateNode node,
                    String methodName, JsArray<?> argValues, int promiseId) {
                serverMethods.put(methodName, argValues);
                serverRpcNodes.put(methodName, node);
                serverPromiseIds.put(methodName, Integer.valueOf(promiseId));
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

    public void testClientCallablePromises() {
        String methodName = "publishedMethod";

        node.getList(NodeFeatures.CLIENT_DELEGATE_HANDLERS).add(0, methodName);
        Binder.bind(node, element);
        Reactive.flush();
        ServerEventObject serverObject = ServerEventObject.get(element);

        NativeFunction publishedMethod = new NativeFunction(
                "return this." + methodName + "()");
        Object promise0 = publishedMethod.apply(serverObject,
                JsCollections.array());

        assertNotNull(promise0);
        assertEquals(Integer.valueOf(0), serverPromiseIds.get(methodName));
        assertTrue(hasPromise(element, 0));

        Object promise1 = publishedMethod.apply(serverObject,
                JsCollections.array());
        assertEquals(Integer.valueOf(1), serverPromiseIds.get(methodName));
        assertTrue(hasPromise(element, 1));

        addThen(promise0, value -> {
            assertEquals("promise0", value);
            assertFalse("Promise handlers should be cleared",
                    hasPromise(element, 0));

            completePromise(element, 1, false, null);
        });

        addCatch(promise1, message -> {
            assertEquals(
                    "Error: Something went wrong. Check server-side logs for more information.",
                    message);
            assertFalse("Promise handlers should be cleared",
                    hasPromise(element, 1));

            finishTest();
        });

        completePromise(element, 0, true, "promise0");

        delayTestFinish(100);
    }

    private static native void restorePromiseImmediateFn()
    /*-{
        window.Promise._immediateFn = window.Promise._originalImmediateFn;
    }-*/;

    private static native void addThen(Object promise,
            Consumer<String> callback)
    /*-{
        promise.then($entry(function(value) {
            callback.@Consumer::accept(*)(value);
        }));
    }-*/;

    private static native void addCatch(Object promise,
            Consumer<String> callback)
    /*-{
        promise['catch']($entry(function(value) {
            callback.@Consumer::accept(*)(""+value);
        }));
    }-*/;

    private static native void completePromise(Element element, int promiseId,
            boolean success, String value)
    /*-{
        element.$server[@ServerEventObject::PROMISE_CALLBACK_NAME](promiseId, success, value);
    }-*/;

    private static native boolean hasPromise(Element element, int promiseId)
    /*-{
        return promiseId in element.$server[@ServerEventObject::PROMISE_CALLBACK_NAME].promises;
    }-*/;

    public void testClientCallableMethodInDom() {
        assertServerEventHandlerMethodInDom(
                NodeFeatures.CLIENT_DELEGATE_HANDLERS,
                el -> assertPublishedMethods(el,
                        new String[] { "publishedMethod" }),
                "publishedMethod");
    }

    public void testAddClientCallableHandlerMethod() {
        assertAddServerEventHandlerMethod(NodeFeatures.CLIENT_DELEGATE_HANDLERS,
                this::assertPublishedMethods);
    }

    public void testRemoveServerEventHandlerMethod() {
        assertRemoveServerEventHandlerMethod(
                NodeFeatures.CLIENT_DELEGATE_HANDLERS,
                this::assertPublishedMethods);
    }

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

    private void assertPublishedMethods(Element element, String[] expected) {
        assertEventHandlerMethods(() -> getPublishedServerMethods(element),
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
