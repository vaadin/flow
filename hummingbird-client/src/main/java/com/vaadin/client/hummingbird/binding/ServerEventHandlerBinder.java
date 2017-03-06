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
package com.vaadin.client.hummingbird.binding;

import java.util.function.Supplier;

import jsinterop.annotations.JsFunction;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.ConstantPool;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;
import com.vaadin.client.hummingbird.nodefeature.NodeList;
import com.vaadin.client.hummingbird.util.NativeFunction;
import com.vaadin.hummingbird.shared.NodeFeatures;

import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventRemover;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Binds and updates <code>element.$server</code>.
 *
 * @author Vaadin Ltd
 */
public class ServerEventHandlerBinder {

    /**
     * A representation of <code>element.$server</code>.
     *
     * @author Vaadin Ltd
     */
    public static final class ServerEventObject extends JavaScriptObject {

        /**
         * JSO constructor.
         */
        protected ServerEventObject() {

        }

        /**
         * Defines a method with the given name to be a callback to the server
         * for the given state node.
         *
         * @param methodName
         *            the name of the method to add
         * @param node
         *            the node to use as an identifier when sending an event to
         *            the server
         */
        public void defineMethod(String methodName, StateNode node) {
            defineMethod(methodName, node, null, false);
        }

        /**
         * Defines a method with the given name to be a callback to the server
         * for the given state node.
         *
         * @param methodName
         *            the name of the method to add
         * @param node
         *            the node to use as an identifier when sending an event to
         *            the server
         * @param ignoreArguments
         *            if {@code true} then the method parameters won't be sent
         *            to the server (when the method is invoked)
         */
        public native void defineMethod(String methodName, StateNode node,
                ServerEventObject element, boolean ignoreArguments)
        /*-{
            this[methodName] = $entry(function() {
                var tree = node.@com.vaadin.client.hummingbird.StateNode::getTree()();
                if ( ignoreArguments ){
                    tree.@com.vaadin.client.hummingbird.StateTree::sendTemplateEventToServer(*)(node, methodName, []);
                } else {
                    var args = this.@com.vaadin.client.hummingbird.binding.ServerEventHandlerBinder.ServerEventObject::getEventData(*)($wnd.event, element, methodName, node);
                    if(args === null) {
                        args = Array.prototype.slice.call(arguments);
                    }
                    tree.@com.vaadin.client.hummingbird.StateTree::sendTemplateEventToServer(*)(node, methodName, args);
                }
            });
        }-*/;

        public JsonArray getEventData(Event event, Element element,
                String methodName, StateNode node) {
            if(!node
                    .getMap(NodeFeatures.POLYMER_EVENT_LISTENERS).hasPropertyValue(methodName)){
                return null;
            }

            JsonArray data = Json.createArray();

            ConstantPool constantPool = node.getTree().getRegistry()
                    .getConstantPool();
            String expressionConstantKey = (String) node
                    .getMap(NodeFeatures.POLYMER_EVENT_LISTENERS)
                    .getProperty(methodName).getValue();

            if(!constantPool.has(expressionConstantKey)) {
                return null;
            }

            JsArray<String> dataExpressions = constantPool
                    .get(expressionConstantKey);

            for (int i = 0; i < dataExpressions.length(); i++) {
                String expression = dataExpressions.get(i);

                SimpleElementBindingStrategy.EventDataExpression dataExpression = SimpleElementBindingStrategy.getOrCreateExpression(
                        expression);
                JsonValue expressionValue = dataExpression.evaluate(event,
                        element);
                JsonObject eventData = Json.createObject();
                eventData.put(expression, expressionValue);
                data.set(i, eventData);
            }

            return data;
        }

        /**
         * Removes a method with the given name.
         *
         * @param methodName
         *            the name of the method to remove
         */
        public native void removeMethod(String methodName)
        /*-{
           delete this[methodName];
        }-*/;

        /**
         * Gets the defined methods.
         *
         * @return an array of defined method names
         */
        public native JsArray<String> getMethods()
        /*-{
           return Object.keys(this);
        }-*/;

        /**
         * Gets or creates <code>element.$server</code> for the given element.
         *
         * @param element
         *            the element to use
         * @return a reference to the <code>$server</code> object in the element
         */
        public static ServerEventObject get(Element element) {
            ServerEventObject serverObject = WidgetUtil
                    .crazyJsoCast(WidgetUtil.getJsProperty(element, "$server"));
            if (serverObject == null) {
                serverObject = (ServerEventObject) JavaScriptObject
                        .createObject();
                WidgetUtil.setJsProperty(element, "$server", serverObject);
            }
            return serverObject;
        }

    }

    private ServerEventHandlerBinder() {
        // Only static methods
    }

    /**
     * Registers all the server event handler names found in the
     * {@link NodeFeatures#PUBLISHED_SERVER_EVENT_HANDLERS} feature in the state
     * node as <code>element.$server.&lt;methodName&gt;</code>. Additionally
     * listens to changes in the feature and updates <code>$server</code>
     * accordingly.
     *
     * @param element
     *            the element to update
     * @param node
     *            the state node containing the feature
     * @return a handle which can be used to remove the listener for the feature
     */
    public static EventRemover bindServerEventHandlerNames(Element element,
            StateNode node) {
        return bindServerEventHandlerNames(() -> ServerEventObject.get(element),
                node, NodeFeatures.PUBLISHED_SERVER_EVENT_HANDLERS, false);
    }

    /**
     * Registers all the server event handler names found in the feature with
     * the {@code featureId} in the {@link ServerEventObject} {@code object}.
     * Additionally listens to changes in the feature and updates server event
     * object accordingly.
     *
     * @param objectProvider
     *            the provider of the event object to update
     * @param node
     *            the state node containing the feature
     * @param featureId
     *            the feature id which contains event handler methods
     * @param ignoreMethodArguments
     *            if {@code true} then the event handler parameters won't be
     *            sent to the server (when the method is invoked)
     * @return a handle which can be used to remove the listener for the feature
     */
    public static EventRemover bindServerEventHandlerNames(
            Supplier<ServerEventObject> objectProvider, StateNode node,
            int featureId, boolean ignoreMethodArguments) {
        NodeList serverEventHandlerNamesList = node.getList(featureId);

        if (serverEventHandlerNamesList.length() > 0) {
            ServerEventObject object = objectProvider.get();

            for (int i = 0; i < serverEventHandlerNamesList.length(); i++) {
                String serverEventHandlerName = (String) serverEventHandlerNamesList
                        .get(i);
                // ignore arguments for now
                object.defineMethod(serverEventHandlerName, node, object,
                        ignoreMethodArguments);
            }
        }

        return serverEventHandlerNamesList.addSpliceListener(e -> {
            ServerEventObject serverObject = objectProvider.get();

            JsArray<?> remove = e.getRemove();
            for (int i = 0; i < remove.length(); i++) {
                serverObject.removeMethod((String) remove.get(i));
            }

            JsArray<?> add = e.getAdd();
            for (int i = 0; i < add.length(); i++) {
                // ignore arguments for now
                serverObject.defineMethod((String) add.get(i), node,
                        serverObject, ignoreMethodArguments);
            }
        });
    }
}
