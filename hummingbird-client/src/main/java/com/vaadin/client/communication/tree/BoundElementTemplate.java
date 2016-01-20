package com.vaadin.client.communication.tree;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.js.JsFunction;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.vaadin.client.Profiler;
import com.vaadin.client.communication.CustomElement;
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
    private final String is;
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
        is = templateDescription.getString("is");

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
        Profiler.enter("BoundElementTemplate.createElement");

        Element element;
        if (is != null) {
            TreeUpdater.debug(
                    "Create custom element with tag " + tag + " that is " + is);
            element = CustomElement.createElement(tag, is);
        } else {
            TreeUpdater.debug("Create element with tag " + tag);
            element = Document.get().createElement(tag);
        }

        initElement(node, element, context);

        Profiler.enter("BoundElementTemplate.createElement bind attributes");
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
        Profiler.leave("BoundElementTemplate.createElement bind attributes");

        Profiler.enter("BoundElementTemplate.createElement bind classes");
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
        Profiler.leave("BoundElementTemplate.createElement bind classes");

        node.getProperty(Integer.toString(getId())).addPropertyChangeListener(
                new TreeNodePropertyValueChangeListener() {
                    @Override
                    public void changeValue(Object oldValue, Object newValue) {
                        if (oldValue != null) {
                            throw new RuntimeException("Not yet supported");
                        }

                        Profiler.enter(
                                "BoundElementTemplate override node changeValue");

                        TreeNode overrideNode = (TreeNode) newValue;

                        BasicElementListener.bind(overrideNode, element,
                                treeUpdater);
                        Profiler.leave(
                                "BoundElementTemplate override node changeValue");
                    }
                });

        node.addTreeNodeChangeListener((name, property) -> {
            if (!name.equals("CLASS_LIST")) {
                return;
            }

            Profiler.enter("BoundElementListener CLASS_LIST addProperty");
            ListTreeNode classListListNode = (ListTreeNode) node
                    .getProperty("CLASS_LIST").getValue();
            classListListNode.addArrayEventListener(
                    (listTreeNode, startIndex, removed, added) -> {
                ClassListUpdater.splice(element, listTreeNode, startIndex,
                        removed, added);
            });
            Profiler.leave("BoundElementListener CLASS_LIST addProperty");
        });

        Profiler.leave("BoundElementTemplate.createElement");
        return element;
    }

    public static Object evaluateExpression(String expression,
            NodeContext nodeContext) {
        Profiler.enter("BoundElementTemplate.evaluateExpression");

        JavaScriptObject context = nodeContext.getExpressionContext();

        JavaScriptObject value = TreeUpdater.evalWithContextFactory(context,
                "return " + expression);
        Profiler.leave("BoundElementTemplate.evaluateExpression");
        return value;
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
        Profiler.enter("BoundElementTemplate.initElement");

        Profiler.enter("BoundElementTemplate.initElement default attributes");
        for (Entry<String, String> entry : defaultAttributeValues.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            if (name.equals("class")) {
                addClassFromTemplate(element, value);
            } else if (name.equals("LOCAL_ID")) {
                context.registerLocalId(value, element);
            } else {
                DomApi.wrap(element).setAttribute(name, value);
            }
        }
        Profiler.leave("BoundElementTemplate.initElement default attributes");

        Profiler.enter("BoundElementTemplate.initElement event listeners");
        if (events != null) {
            for (Entry<String, String[]> entry : events.entrySet()) {
                String type = entry.getKey();
                String[] handlers = entry.getValue();
                TreeUpdater.addDomListener(element, type, new DomListener() {
                    @Override
                    public void handleEvent(JavaScriptObject event) {
                        long start = System.currentTimeMillis();
                        for (String handler : handlers) {
                            JavaScriptObjectWithUsefulMethods evalContext = JavaScriptObject
                                    .createObject().cast();
                            context.populateEventHandlerContext(evalContext);

                            evalContext.put("server", context.getServerProxy());
                            evalContext.put("model", context.getModelProxy());
                            evalContext.put("event", event);
                            evalContext.put("element", element);

                            TreeUpdater.evalWithContextFactory(evalContext,
                                    handler);
                        }

                        JsonArray modelChanges = Json.createArray();

                        node.getCallbackQueue().flush(modelChanges);

                        for (int i = 0; i < modelChanges.length(); i++) {
                            treeUpdater.addPendingNodeChange(
                                    modelChanges.getObject(i));
                        }

                        treeUpdater.afterNodeChanges();
                        long end = System.currentTimeMillis();
                        getLogger().log(Level.INFO, "Handled " + type
                                + " event in " + (end - start) + " ms");
                    }
                });
            }
        }
        Profiler.leave("BoundElementTemplate.initElement event listeners");

        Profiler.enter("BoundElementTemplate.initElement children");
        if (childElementTemplates != null) {
            for (int templateId : childElementTemplates) {
                Profiler.enter(
                        "BoundElementTemplate.initElement children create");
                Node newChildElement = treeUpdater.createElement(
                        treeUpdater.getTemplate(templateId), node, context);
                Profiler.leave(
                        "BoundElementTemplate.initElement children create");

                if (TreeUpdater.debug) {
                    Profiler.enter(
                            "BoundElementTemplate.initElement children log");
                    TreeUpdater.debug("Appended node "
                            + TreeUpdater.debugHtml(newChildElement) + " into "
                            + TreeUpdater.debugHtml(element));
                    Profiler.leave(
                            "BoundElementTemplate.initElement children log");
                }

                Profiler.enter(
                        "BoundElementTemplate.initElement children append");
                DomApi.wrap(element).appendChild(newChildElement);
                Profiler.leave(
                        "BoundElementTemplate.initElement children append");
            }
        }
        Profiler.leave("BoundElementTemplate.initElement children");

        Profiler.leave("BoundElementTemplate.initElement");
    }

    private void addClassFromTemplate(Element element, String className) {
        className = className.trim();
        if (className.isEmpty()) {
            return;
        } else if (className.contains(" ")) {
            String[] split = className.split("\\s+");
            for (String str : split) {
                DomApi.wrap(element).getClassList().add(str);
            }
        } else {
            DomApi.wrap(element).getClassList().add(className);
        }
    }

    @Override
    public JavaScriptObject createServerProxy(int nodeId) {
        Profiler.enter("BoundElementTemplate.createServerProxy");

        JavaScriptObject proxy = JavaScriptObject.createObject();

        if (eventHandlerMethods != null) {
            for (String serverMethodName : eventHandlerMethods) {
                attachServerProxyMethod(treeUpdater, proxy, nodeId, getId(),
                        serverMethodName);
            }
        }

        Profiler.leave("BoundElementTemplate.createServerProxy");
        return proxy;
    }

    private static native void attachServerProxyMethod(TreeUpdater treeUpdater,
            JavaScriptObject proxy, int nodeId, int templateId, String name)
            /*-{
                proxy[name] = function() {
                    // Convert to proper Array
                    var args = Array.prototype.slice.call(arguments);
                    return new $wnd.Promise($entry(function(resolve, reject) {
                        @BoundElementTemplate::sendTemplateEventToServer(*)(treeUpdater, nodeId, templateId, name, args, resolve, reject);
                    }));
                };
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
                int proxyId = TreeNode.getProxyId(value);
                if (proxyId != -1) {
                    // Send id for tree nodes
                    jsonValue = Json.create(proxyId);
                } else {
                    jsonValue = value.cast();
                }
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

    private static Logger getLogger() {
        return Logger.getLogger(BoundElementTemplate.class.getName());
    }
}