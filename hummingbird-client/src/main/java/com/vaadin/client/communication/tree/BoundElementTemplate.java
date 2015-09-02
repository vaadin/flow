package com.vaadin.client.communication.tree;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.vaadin.client.communication.Polymer;
import com.vaadin.client.communication.tree.ElementNotifier.ElementUpdater;
import com.vaadin.client.communication.tree.NodeListener.ListInsertChange;
import com.vaadin.client.communication.tree.NodeListener.ListInsertNodeChange;
import com.vaadin.client.communication.tree.NodeListener.ListRemoveChange;
import com.vaadin.client.communication.tree.NodeListener.PutChange;
import com.vaadin.client.communication.tree.NodeListener.PutNodeChange;
import com.vaadin.client.communication.tree.NodeListener.PutOverrideChange;
import com.vaadin.client.communication.tree.NodeListener.RemoveChange;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class BoundElementTemplate extends Template {
    private class BoundElementListener implements ElementUpdater {

        private final Element element;

        public BoundElementListener(Element element) {
            this.element = element;
        }

        @Override
        public void putNode(String property, PutNodeChange change) {
            // Don't care
        }

        @Override
        public void put(String property, PutChange change) {
            String targetAttribute = getTargetAttribute(property);
            if (targetAttribute != null) {
                TreeUpdater.setAttributeOrProperty(element, targetAttribute,
                        change.getValue());
            }

            String classPartMapping = getClassPartMapping(property);
            if (classPartMapping != null) {
                if (isTrueIsh(change.getValue())) {
                    Polymer.dom(element).getClassList().add(classPartMapping);
                } else {
                    Polymer.dom(element).getClassList()
                            .remove(classPartMapping);
                }
            }
        }

        private boolean isTrueIsh(JsonValue value) {
            switch (value.getType()) {
            case BOOLEAN:
                return value.asBoolean();
            case STRING:
                return !"false".equalsIgnoreCase(value.asString());
            default:
                throw new RuntimeException(value.getType().name());
            }
        }

        @Override
        public void listInsertNode(String property,
                ListInsertNodeChange change) {
            // Don't care
        }

        @Override
        public void listInsert(String property, ListInsertChange change) {
            // Don't care
        }

        @Override
        public void listRemove(String property, ListRemoveChange change) {
            // Don't care
        }

        @Override
        public void remove(String property, RemoveChange change) {
            String targetAttribute = getTargetAttribute(property);
            if (targetAttribute != null) {
                TreeUpdater.setAttributeOrProperty(element, targetAttribute,
                        null);
            }

            String classPartMapping = getClassPartMapping(property);
            if (classPartMapping != null) {
                Polymer.dom(element).getClassList().remove(classPartMapping);
            }
        }

        @Override
        public void putOverride(String property, PutOverrideChange change) {
            if (change.getKey() != getId()) {
                return;
            }

            int nodeId = change.getValue();
            JsonObject overrideNode = treeUpdater.getNode(nodeId);

            BasicElementListener basicElementListener = new BasicElementListener(
                    treeUpdater, overrideNode, element);
            treeUpdater.addNodeListener(overrideNode, basicElementListener);
        }
    }

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
    public Node createElement(final JsonObject node, NodeContext context) {
        assert tag != null;
        TreeUpdater.debug("Create element with tag " + tag);

        Element element = Document.get().createElement(tag);

        initElement(node, element, context);

        context.getNotifier().addUpdater(new BoundElementListener(element));

        return element;
    }

    protected void initElement(JsonObject node, Element element,
            NodeContext context) {
        assert element != null;

        for (Entry<String, String> entry : defaultAttributeValues.entrySet()) {
            Polymer.dom(element).setAttribute(entry.getKey(), entry.getValue());
        }

        if (events != null) {
            for (Entry<String, String[]> entry : events.entrySet()) {
                String type = entry.getKey();
                String[] handlers = entry.getValue();
                TreeUpdater.addDomListener(element, type, new DomListener() {
                    @Override
                    public void handleEvent(JavaScriptObject event) {
                        for (String handler : handlers) {
                            JsArrayString newFunctionParams = JavaScriptObject
                                    .createArray().cast();
                            JsArray<JavaScriptObject> params = JavaScriptObject
                                    .createArray().cast();

                            newFunctionParams.push("event");
                            params.push(event);

                            newFunctionParams.push("element");
                            params.push(element);

                            newFunctionParams.push("server");
                            params.push(context.getServerProxy());

                            newFunctionParams.push("model");
                            params.push(context.getModelProxy());

                            newFunctionParams.push(handler);

                            TreeUpdater.createAndRunFunction(newFunctionParams,
                                    params);
                        }
                    }
                });
            }
        }

        if (childElementTemplates != null) {
            for (int templateId : childElementTemplates) {
                Node newChildElement = treeUpdater.createElement(
                        treeUpdater.getTemplate(templateId), node, context);
                Polymer.dom(element).appendChild(newChildElement);
            }
        }
    }

    private String getTargetAttribute(String property) {
        return propertyToAttribute.get(property);
    }

    private String getClassPartMapping(String property) {
        if (classPartBindings == null) {
            return null;
        } else {
            return classPartBindings.get(property);
        }
    }

    @Override
    public JavaScriptObject createServerProxy(Integer nodeId) {
        JavaScriptObject proxy = JavaScriptObject.createObject();

        if (eventHandlerMethods != null) {
            for (String serverMethodName : eventHandlerMethods) {
                attachServerProxyMethod(treeUpdater, proxy, nodeId.intValue(),
                        getId(), serverMethodName);
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

    @Override
    public JavaScriptObject createModelProxy(JsonObject node) {
        if (modelStructure == null
                || modelStructure.getType() == JsonType.NULL) {
            throw new RuntimeException();
        }

        JavaScriptObject object = JavaScriptObject.createObject();
        for (int i = 0; i < modelStructure.length(); i++) {
            JsonValue value = modelStructure.get(i);
            if (value.getType() == JsonType.STRING) {
                String name = value.asString();
                defineModelProperty(object, node, name);
            } else if (value.getType() == JsonType.OBJECT) {
                throw new RuntimeException("Not yet supported");
            } else {
                throw new RuntimeException(
                        "Unexpected model structure value: " + value.toJson());
            }
        }

        return object;
    }

    private static native void defineModelProperty(JavaScriptObject object,
            JsonObject node, String name)
            /*-{
                Object.defineProperty(object, name, {
                    enumerable: true,
                    get: function() {
                        console.log("Getting value for " + name);
                        return node[name];
                    },
                    set: function(value) {
                        console.log("Setting " + name + " to", value);
                    }
                });
            }-*/;
}