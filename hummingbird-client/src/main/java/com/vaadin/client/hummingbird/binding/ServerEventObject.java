package com.vaadin.client.hummingbird.binding;

import jsinterop.annotations.JsFunction;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.ConstantPool;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;
import com.vaadin.client.hummingbird.util.NativeFunction;
import com.vaadin.hummingbird.shared.NodeFeatures;

import elemental.dom.Element;
import elemental.events.Event;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * A representation of a server object able to send notifications to the server
 *
 * @author Vaadin Ltd
 */
public final class ServerEventObject extends JavaScriptObject {
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
        JsonValue evaluate(Event event, ServerEventObject serverEventObject);
    }

    private static final JsMap<String, ServerEventDataExpression> expressionCache = JsCollections
            .map();

    /**
     * JSO constructor.
     */
    protected ServerEventObject() {

    }

    /**
     * Defines a method with the given name to be a callback to the server for
     * the given state node.
     *
     * @param methodName
     *            the name of the method to add
     * @param node
     *            the node to use as an identifier when sending an event to the
     *            server
     */
    public void defineMethod(String methodName, StateNode node) {
        defineMethod(methodName, node, false);
    }

    /**
     * Defines a method with the given name to be a callback to the server for
     * the given state node.
     *
     * @param methodName
     *            the name of the method to add
     * @param node
     *            the node to use as an identifier when sending an event to the
     *            server
     * @param ignoreArguments
     *            if {@code true} then the method parameters won't be sent to
     *            the server (when the method is invoked)
     */
    public native void defineMethod(String methodName, StateNode node,
            boolean ignoreArguments)
    /*-{
        this[methodName] = $entry(function() {
            var tree = node.@com.vaadin.client.hummingbird.StateNode::getTree()();
            if ( ignoreArguments ){
                tree.@com.vaadin.client.hummingbird.StateTree::sendTemplateEventToServer(*)(node, methodName, []);
            } else {
                var args = this.@com.vaadin.client.hummingbird.binding.ServerEventObject::getEventData(*)($wnd.event, this, methodName, node);
                if(args === null) {
                    args = Array.prototype.slice.call(arguments);
                }
                tree.@com.vaadin.client.hummingbird.StateTree::sendTemplateEventToServer(*)(node, methodName, args);
            }
        });
    }-*/;

    /**
     * Collect extra data for element event if any has been sent from the
     * server.
     * 
     * @param event
     *            The fired Event
     * @param element
     *            Target element
     * @param methodName
     *            Method name that is called
     * @param node
     *            Target node
     * @return Array of extra event data
     */
    protected JsonArray getEventData(Event event, ServerEventObject element,
            String methodName, StateNode node) {
        if (!node.getMap(NodeFeatures.POLYMER_EVENT_LISTENERS)
                .hasPropertyValue(methodName)) {
            return null;
        }

        JsonArray data = Json.createArray();

        ConstantPool constantPool = node.getTree().getRegistry()
                .getConstantPool();
        String expressionConstantKey = (String) node
                .getMap(NodeFeatures.POLYMER_EVENT_LISTENERS)
                .getProperty(methodName).getValue();

        JsArray<String> dataExpressions = constantPool
                .get(expressionConstantKey);

        for (int i = 0; i < dataExpressions.length(); i++) {
            String expression = dataExpressions.get(i);

            ServerEventDataExpression dataExpression = getOrCreateExpression(
                    expression);
            JsonValue expressionValue = dataExpression.evaluate(event, element);
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
            serverObject = (ServerEventObject) JavaScriptObject.createObject();
            WidgetUtil.setJsProperty(element, "$server", serverObject);
        }
        return serverObject;
    }

    protected static ServerEventDataExpression getOrCreateExpression(
            String expressionString) {
        ServerEventDataExpression expression = expressionCache
                .get(expressionString);

        if (expression == null) {
            expression = NativeFunction.create("event", "element",
                    "return (" + expressionString + ")");
            expressionCache.set(expressionString, expression);
        }

        return expression;
    }
}