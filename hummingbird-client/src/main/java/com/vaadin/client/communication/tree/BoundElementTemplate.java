package com.vaadin.client.communication.tree;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.js.JsFunction;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.vaadin.client.communication.DomApi;
import com.vaadin.client.communication.tree.TreeNodeProperty.TreeNodePropertyValueChangeListener;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class BoundElementTemplate extends Template {
    private final TreeUpdater treeUpdater;
    private final String tag;
    private final Map<String, String> defaultAttributeValues;
    private final Map<String, String> propertyToAttribute;
    private final Map<String, String> classPartBindings;
    private final Map<String, String[]> events;

    private final String[] eventHandlerMethods;

    private final int[] childElementTemplates;

    private final JsonArray modelStructure;

    public BoundElementTemplate(TreeUpdater treeUpdater,
            JsonObject templateDescription, int templateId) {
        super(templateId);
        this.treeUpdater = treeUpdater;
        tag = templateDescription.getString("tag");

        defaultAttributeValues = readStringMap(
                templateDescription.getObject("defaultAttributes"));

        propertyToAttribute = readStringMap(
                templateDescription.getObject("attributeBindings"));

        classPartBindings = readStringMap(
                templateDescription.getObject("classPartBindings"));

        if (templateDescription.hasKey("children")) {
            JsonArray children = templateDescription.getArray("children");
            childElementTemplates = new int[children.length()];
            for (int i = 0; i < childElementTemplates.length; i++) {
                childElementTemplates[i] = (int) children.getNumber(i);
            }
        } else {
            childElementTemplates = null;
        }

        if (templateDescription.hasKey("events")) {
            events = new HashMap<>();
            JsonObject eventsJson = templateDescription.getObject("events");
            for (String type : eventsJson.keys()) {
                JsonArray handlersJson = eventsJson.getArray(type);
                String[] handlers = new String[handlersJson.length()];
                for (int i = 0; i < handlers.length; i++) {
                    handlers[i] = handlersJson.getString(i);
                }
                events.put(type, handlers);
            }
        } else {
            events = null;
        }

        if (templateDescription.hasKey("eventHandlerMethods")) {
            JsonArray array = templateDescription
                    .getArray("eventHandlerMethods");
            eventHandlerMethods = new String[array.length()];
            for (int i = 0; i < eventHandlerMethods.length; i++) {
                eventHandlerMethods[i] = array.getString(i);
            }
        } else {
            eventHandlerMethods = null;
        }

        modelStructure = templateDescription.getArray("modelStructure");
    }

    private static Map<String, String> readStringMap(JsonObject json) {
        Map<String, String> values = new HashMap<>();
        if (json == null) {
            return values;
        }
        for (String name : json.keys()) {
            assert json.get(name).getType() == JsonType.STRING;
            values.put(name, json.getString(name));
        }
        return values;
    }

    @Override
    public Node createElement(final TreeNode node, NodeContext context) {
        assert tag != null;
        TreeUpdater.debug("Create element with tag " + tag);

        Element element = Document.get().createElement(tag);

        initElement(node, element, context);

        for (Entry<String, String> entry : propertyToAttribute.entrySet()) {
            String property = entry.getKey();
            String attribute = entry.getValue();

            TreeUpdater.debug("Binding " + property + " to " + attribute);

            context.resolveProperty(property).addPropertyChangeListener(
                    new TreeNodePropertyValueChangeListener() {
                        @Override
                        public void changeValue(Object oldValue, Object value) {
                            TreeUpdater.debug("Binding (" + property + " to "
                                    + attribute + ") changed to " + value);

                            TreeUpdater.setAttributeOrProperty(element,
                                    attribute, value);
                        }
                    });
        }

        if (classPartBindings != null) {
            for (Entry<String, String> entry : classPartBindings.entrySet()) {
                String property = entry.getKey();
                String classPart = entry.getValue();
                context.resolveProperty(property).addPropertyChangeListener(
                        new TreeNodePropertyValueChangeListener() {
                            @Override
                            public void changeValue(Object oldValue,
                                    Object value) {
                                if (isTrueIsh(TreeUpdater.asJsonValue(value))) {
                                    DomApi.wrap(element).getClassList()
                                            .add(classPart);
                                } else {
                                    DomApi.wrap(element).getClassList()
                                            .remove(classPart);
                                }
                            }
                        });
            }
        }

        node.getProperty(Integer.toString(getId())).addPropertyChangeListener(
                new TreeNodePropertyValueChangeListener() {
                    @Override
                    public void changeValue(Object oldValue, Object newValue) {
                        if (oldValue != null) {
                            throw new RuntimeException("Not yet supported");
                        }

                        TreeNode overrideNode = (TreeNode) newValue;

                        BasicElementListener.bind(overrideNode, element,
                                treeUpdater);
                    }
                });

        return element;
    }

    private static boolean isTrueIsh(JsonValue value) {
        switch (value.getType()) {
        case BOOLEAN:
            return value.asBoolean();
        case STRING:
            return !"false".equalsIgnoreCase(value.asString());
        case NULL:
            return false;
        default:
            throw new RuntimeException(value.getType().name());
        }
    }

    protected void initElement(TreeNode node, Element element,
            NodeContext context) {
        assert element != null;

        for (Entry<String, String> entry : defaultAttributeValues.entrySet()) {
            DomApi.wrap(element).setAttribute(entry.getKey(), entry.getValue());
        }

        if (events != null) {
            for (Entry<String, String[]> entry : events.entrySet()) {
                String type = entry.getKey();
                String[] handlers = entry.getValue();
                TreeUpdater.addDomListener(element, type, new DomListener() {
                    @Override
                    public void handleEvent(JavaScriptObject event) {
                        for (String handler : handlers) {
                            Map<String, JavaScriptObject> contextMap = context
                                    .buildEventHandlerContext();
                            contextMap.put("event", event);
                            contextMap.put("element", element);
                            TreeUpdater.evalWithContext(contextMap, handler);
                        }

                        JsonArray modelChanges = Json.createArray();

                        node.getCallbackQueue().flush(modelChanges);

                        for (int i = 0; i < modelChanges.length(); i++) {
                            treeUpdater.addPendingNodeChange(
                                    modelChanges.getObject(i));
                        }
                    }
                });
            }
        }

        if (childElementTemplates != null) {
            for (int templateId : childElementTemplates) {
                Node newChildElement = treeUpdater.createElement(
                        treeUpdater.getTemplate(templateId), node, context);
                DomApi.wrap(element).appendChild(newChildElement);
            }
        }
    }

    @Override
    public JavaScriptObject createServerProxy(int nodeId) {
        JavaScriptObject proxy = JavaScriptObject.createObject();

        if (eventHandlerMethods != null) {
            for (String serverMethodName : eventHandlerMethods) {
                attachServerProxyMethod(treeUpdater, proxy, nodeId, getId(),
                        serverMethodName);
            }
        }

        return proxy;
    }

    private static native void attachServerProxyMethod(TreeUpdater treeUpdater,
            JavaScriptObject proxy, int nodeId, int templateId, String name)
            /*-{
                proxy[name] = $entry(function() {
                    // Convert to proper Array
                    var args = Array.prototype.slice.call(arguments);
                    @BoundElementTemplate::sendTemplateEventToServer(*)(treeUpdater, nodeId, templateId, name, args);
                });
            }-*/;

    private static void sendTemplateEventToServer(TreeUpdater treeUpdater,
            int nodeId, int templateId, String type,
            JsArray<JavaScriptObject> rawArguments) {
        JsonArray eventData = Json.createArray();
        for (int i = 0; i < rawArguments.length(); i++) {
            JavaScriptObject value = rawArguments.get(i);
            JsonValue jsonValue;
            if (Node.is(value)) {
                // Convert element instance to [nodeId, templateId]
                Node node = Node.as(value);
                jsonValue = TreeUpdater.getElementIdentifier(node);
            } else {
                jsonValue = value.cast();
            }
            eventData.set(i, jsonValue);
        }

        JsonArray arguments = Json.createArray();
        arguments.set(0, nodeId);
        arguments.set(1, templateId);
        arguments.set(2, type);
        arguments.set(3, eventData);

        treeUpdater.sendRpc("vTemplateEvent", arguments);
    }

    @JsFunction
    @FunctionalInterface
    private interface Setter {
        public void set(JsonValue value);
    }
}