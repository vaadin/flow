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
    private final Map<String, String> attributeToExpression;
    private final Map<String, String> classPartBindings;
    private final Map<String, String[]> events;

    private final String[] eventHandlerMethods;

    private final int[] childElementTemplates;

    public BoundElementTemplate(TreeUpdater treeUpdater,
            JsonObject templateDescription, int templateId) {
        super(templateId);
        this.treeUpdater = treeUpdater;
        tag = templateDescription.getString("tag");

        defaultAttributeValues = readStringMap(
                templateDescription.getObject("defaultAttributes"));

        attributeToExpression = readStringMap(
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

        for (Entry<String, String> entry : attributeToExpression.entrySet()) {
            String expression = entry.getValue();
            String attribute = entry.getKey();

            TreeUpdater.debug("Binding " + expression + " to " + attribute);

            Reactive.keepUpToDate(() -> {
                Object value = evaluateExpression(expression, context);
                TreeUpdater.setAttributeOrProperty(element, attribute, value);
                TreeUpdater.debug("Binding (" + expression + " to " + attribute
                        + ") changed to " + value);
            });
        }

        if (classPartBindings != null) {
            for (Entry<String, String> entry : classPartBindings.entrySet()) {
                String expression = entry.getValue();
                String classPart = entry.getKey();
                Reactive.keepUpToDate(() -> {
                    Object value = evaluateExpression(expression, context);

                    if (isTrueIsh(TreeUpdater.asJsonValue(value))) {
                        DomApi.wrap(element).getClassList().add(classPart);
                    } else {
                        DomApi.wrap(element).getClassList().remove(classPart);
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

        node.addTreeNodeChangeListener((name, property) -> {
            if (!name.equals("CLASS_LIST")) {
                return;
            }

            ListTreeNode classListListNode = (ListTreeNode) node
                    .getProperty("CLASS_LIST").getValue();
            classListListNode.addArrayEventListener(
                    (listTreeNode, startIndex, removed, added) -> {
                ClassListUpdater.splice(element, listTreeNode, startIndex,
                        removed, added);
            });
        });

        return element;
    }

    public static Object evaluateExpression(String expression,
            NodeContext nodeContext) {
        Map<String, JavaScriptObject> context = nodeContext
                .buildExpressionContext();

        return TreeUpdater.evalWithContext(context, "return " + expression);
    }

    private static boolean isTrueIsh(JsonValue value) {
        if (value == null) {
            // JsonValue.getType() doens't work for undefined
            return false;
        }
        switch (value.getType()) {
        case BOOLEAN:
            return value.asBoolean();
        case STRING:
            return !"false".equalsIgnoreCase(value.asString());
        default:
            throw new RuntimeException(value.getType().name());
        }
    }

    protected void initElement(TreeNode node, Element element,
            NodeContext context) {
        assert element != null;

        for (Entry<String, String> entry : defaultAttributeValues.entrySet()) {
            if (entry.getKey().equals("class")) {
                DomApi.wrap(element).getClassList().add(entry.getValue());
            } else {
                DomApi.wrap(element).setAttribute(entry.getKey(),
                        entry.getValue());
            }
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

                        treeUpdater.afterNodeChanges();
                    }
                });
            }
        }

        if (childElementTemplates != null) {
            for (int templateId : childElementTemplates) {
                Node newChildElement = treeUpdater.createElement(
                        treeUpdater.getTemplate(templateId), node, context);
                if (TreeUpdater.debug) {
                    TreeUpdater.debug("Appended node "
                            + TreeUpdater.debugHtml(newChildElement) + " into "
                            + TreeUpdater.debugHtml(element));
                }

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
                    return new $wnd.Promise(function(resolve, reject) {
                        @BoundElementTemplate::sendTemplateEventToServer(*)(treeUpdater, nodeId, templateId, name, args, resolve, reject);
                    });
                });
            }-*/;

    private static void sendTemplateEventToServer(TreeUpdater treeUpdater,
            int nodeId, int templateId, String type,
            JsArray<JavaScriptObject> rawArguments, JavaScriptObject resolve,
            JavaScriptObject reject) {
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

        int promiseId = treeUpdater.registerPromise(resolve, reject);

        JsonArray arguments = Json.createArray();
        arguments.set(0, nodeId);
        arguments.set(1, templateId);
        arguments.set(2, type);
        arguments.set(3, eventData);
        arguments.set(4, promiseId);

        treeUpdater.sendRpc("vTemplateEvent", arguments);
    }

    @JsFunction
    @FunctionalInterface
    private interface Setter {
        public void set(JsonValue value);
    }
}