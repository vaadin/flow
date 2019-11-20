/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.client.flow.binding;

import jsinterop.annotations.JsFunction;

import com.google.gwt.core.client.JavaScriptObject;

import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.ConstantPool;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.util.NativeFunction;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.shared.JsonConstants;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental.events.Event;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * A representation of a server object able to send notifications to the server.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public final class ServerEventObject extends JavaScriptObject {
    private static final String NODE_ID = "nodeId";
    private static final String EVENT_PREFIX = "event";

    private static final String PROMISE_CALLBACK_NAME = JsonConstants.RPC_PROMISE_CALLBACK_NAME;

    /**
     * Callback interface for an event data expression parsed using new
     * Function() in JavaScript.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    private interface ServerEventDataExpression {
        /**
         * Callback interface for an event data expression parsed using new
         * Function() in JavaScript.
         *
         * @param event
         *            Event to expand
         * @param serverEventObject
         *            target server event handler object
         * @return Result of evaluated function
         */
        JsonObject evaluate(Event event, ServerEventObject serverEventObject);
    }

    private static final JsMap<String, ServerEventDataExpression> expressionCache = JsCollections
            .map();

    /**
     * JSO constructor.
     */
    protected ServerEventObject() {
    }

    private native void initPromiseHandler()
    /*-{
        var name = @ServerEventObject::PROMISE_CALLBACK_NAME
        // Use defineProperty to make it non-enumerable
        Object.defineProperty(this, name, {
            value: function(promiseId, success, value) {
                var promise = this[name].promises[promiseId];

                // undefined if client-side node was recreated after execution was scheduled
                if (promise !== undefined) {
                    delete this[name].promises[promiseId];

                    if (success) {
                        // Resolve
                        promise[0](value);
                    } else {
                        // Reject
                        promise[1](Error("Something went wrong. Check server-side logs for more information."));
                    }
                }
            }
        });
        this[name].promises = [];
    }-*/;

    /**
     * Defines a method with the given name to be a callback to the server for
     * the given state node.
     * <p>
     * Note! If the Polymer.Element contains an implementation for
     * {@code methodName} it will be run before the server-side method.
     *
     * @param methodName
     *            the name of the method to add
     * @param node
     *            the node to use as an identifier when sending an event to the
     *            server
     * @param returnPromise
     *            <code>true</code> if the handler should return a promise that
     *            will reflect the server-side result; <code>false</code> to not
     *            return any value
     */
    public native void defineMethod(String methodName, StateNode node,
            boolean returnPromise)
    /*-{
        this[methodName] = $entry(function(eventParameter) {
            var prototype = Object.getPrototypeOf(this);
            if(prototype[methodName] !== undefined) {
                prototype[methodName].apply(this, arguments);
            }
            var event = eventParameter || $wnd.event;
            var tree = node.@com.vaadin.client.flow.StateNode::getTree()();
            var args = this.@com.vaadin.client.flow.binding.ServerEventObject::getEventData(*)(event, methodName, node);
            if(args === null) {
                args = Array.prototype.slice.call(arguments);
            }
    
            var returnValue;
            var promiseId = -1;
    
            if (returnPromise) {
                var promises = this[@ServerEventObject::PROMISE_CALLBACK_NAME].promises;
    
                promiseId = promises.length;
    
                returnValue = new Promise(function(resolve, reject) {
                    // Store each callback for later use
                    promises[promiseId] = [resolve, reject];
                });
            }
    
            tree.@com.vaadin.client.flow.StateTree::sendTemplateEventToServer(*)(node, methodName, args, promiseId);
    
            return returnValue;
        });
    }-*/;

    /**
     * Collect extra data for element event if any has been sent from the
     * server. Note! Data is sent in the array in the same order as defined on
     * the server side.
     *
     * @param event
     *            The fired Event
     * @param methodName
     *            Method name that is called
     * @param node
     *            Target node
     * @return Array of extra event data
     */
    private JsonArray getEventData(Event event, String methodName,
            StateNode node) {

        if (node.getMap(NodeFeatures.POLYMER_EVENT_LISTENERS)
                .hasPropertyValue(methodName)) {
            JsonArray dataArray = Json.createArray();
            ConstantPool constantPool = node.getTree().getRegistry()
                    .getConstantPool();
            String expressionConstantKey = (String) node
                    .getMap(NodeFeatures.POLYMER_EVENT_LISTENERS)
                    .getProperty(methodName).getValue();

            JsArray<String> dataExpressions = constantPool
                    .get(expressionConstantKey);

            for (int i = 0; i < dataExpressions.length(); i++) {
                String expression = dataExpressions.get(i);

                dataArray.set(i, getExpressionValue(event, node, expression));
            }
            return dataArray;
        }

        return null;
    }

    private JsonObject getExpressionValue(Event event, StateNode node,
            String expression) {
        JsonObject expressionValue;

        if (serverExpectsNodeId(expression)) {
            return getPolymerPropertyObject(event, node, expression);
        }

        ServerEventDataExpression dataExpression = getOrCreateExpression(
                expression);
        expressionValue = dataExpression.evaluate(event, this);

        return expressionValue;
    }

    private boolean serverExpectsNodeId(String expression) {
        return !expression.startsWith(EVENT_PREFIX)
                || "event.model.item".equals(expression);
    }

    private JsonObject getPolymerPropertyObject(Event event, StateNode node,
            String expression) {
        if (expression.startsWith(EVENT_PREFIX)) {
            return createPolymerPropertyObject(event, expression);
        } else {
            return getPolymerPropertyObject(node.getDomNode(), expression);
        }
    }

    private JsonObject createPolymerPropertyObject(Event event,
            String expression) {
        ServerEventDataExpression dataExpression = getOrCreateExpression(
                expression);
        JsonObject expressionValue = dataExpression.evaluate(event, this);
        JsonObject object = Json.createObject();
        object.put(NODE_ID, expressionValue.getNumber(NODE_ID));
        return object;
    }

    private native JsonObject getPolymerPropertyObject(Node node,
            String propertyName)
    /*-{
        if (typeof(node.get) === 'function') {
            var polymerProperty = node.get(propertyName);
            if (typeof(polymerProperty) === 'object'
                && typeof(polymerProperty["nodeId"]) !== 'undefined') {
                return { nodeId: polymerProperty["nodeId"] };
            }
        }
        return null;
    }-*/;

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
            serverObject = (ServerEventObject) JavaScriptObject.createObject();
            serverObject.initPromiseHandler();
            WidgetUtil.setJsProperty(element, "$server", serverObject);
        }
        return serverObject;
    }

    protected static ServerEventDataExpression getOrCreateExpression(
            String expressionString) {
        ServerEventDataExpression expression = expressionCache
                .get(expressionString);

        if (expression == null) {
            expression = NativeFunction.create(EVENT_PREFIX, "element",
                    "return (" + expressionString + ")");
            expressionCache.set(expressionString, expression);
        }

        return expression;
    }
}
