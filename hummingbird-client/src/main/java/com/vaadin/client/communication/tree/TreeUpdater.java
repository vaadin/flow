/*
 * Copyright 2000-2014 Vaadin Ltd.
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
package com.vaadin.client.communication.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.Window.Location;
import com.vaadin.client.ApplicationConnection.Client;
import com.vaadin.client.FastStringMap;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.Profiler;
import com.vaadin.client.Util;
import com.vaadin.client.communication.DomApi;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.communication.tree.TreeNodeProperty.TreeNodePropertyValueChangeListener;
import com.vaadin.shared.communication.MethodInvocation;

import elemental.js.json.JsJsonValue;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class TreeUpdater {

    public static final boolean debug = Location.getQueryString()
            .contains("superdevmode");

    private Element rootElement;

    private Map<Integer, Template> templates = new HashMap<>();

    private Map<Integer, TreeNode> idToNode = new HashMap<>();

    private Map<Integer, Map<String, JavaScriptObject>> domListeners = new HashMap<>();

    private boolean rootInitialized = false;

    private CallbackQueue callbackQueue = new CallbackQueue();

    private ServerRpcQueue rpcQueue;

    private Client client;

    private int nextPromiseId = 0;

    private Map<Integer, JavaScriptObject[]> promises = new HashMap<>();

    public void init(Element rootElement, ServerRpcQueue rpcQueue,
            Client client) {
        assert this.rootElement == null : "Can only init once";
        assert rpcQueue != null;
        assert client != null;

        this.rootElement = rootElement;
        this.rpcQueue = rpcQueue;
        this.client = client;
    }

    private JsonArray pendingChanges = Json.createArray();
    private ArrayList<MethodInvocation> pendingInvocations;

    private List<Element> createdElements = new ArrayList<>();

    public void sendRpc(String callbackName, JsonArray arguments) {
        if (pendingInvocations == null) {
            pendingInvocations = new ArrayList<>();
            Scheduler.get().scheduleFinally(() -> {
                if (pendingChanges.length() != 0) {
                    rpcQueue.add(new MethodInvocation("vModelChange",
                            pendingChanges), false);
                    pendingChanges = Json.createArray();
                }

                for (MethodInvocation methodInvocation : pendingInvocations) {
                    rpcQueue.add(methodInvocation, false);
                }
                pendingInvocations = null;

                rpcQueue.flush();
            });
        }

        /*
         * Must invoke manually as the RPC interface can't be used in GWT
         * because of the JSONArray parameter
         */
        pendingInvocations.add(new MethodInvocation(callbackName, arguments));
    }

    public void addPendingNodeChange(JsonObject nodeChnage) {
        pendingChanges.set(pendingChanges.length(), nodeChnage);
    }

    public static native JsonValue asJsonValue(Object value)
    /*-{
        return value;
    }-*/;

    public static native JavaScriptObject asJso(Object value)
    /*-{
        return value;
    }-*/;

    public static void setAttributeOrProperty(Element element, String key,
            Object objectValue) {
        JsonValue value = asJsonValue(objectValue);
        assert element != null;
        String attrKey = isAttribute(key);
        if (attrKey != null) {
            if (value == null || value.getType() == JsonType.NULL) {
                // NULL value is interpreted as adding attribute with empty
                // value, or removing attribute if it has been set already
                if (element.hasAttribute(attrKey)) {
                    DomApi.wrap(element).removeAttribute(attrKey);
                    if (debug) {
                        debug("Removed attribute " + key + " from "
                                + debugHtml(element));
                    }
                } else {
                    DomApi.wrap(element).setAttribute(attrKey, "");
                    if (debug) {
                        debug("Set attribute " + key + "=\"\" for "
                                + debugHtml(element));
                    }
                }
            } else {
                DomApi.wrap(element).setAttribute(attrKey, value.asString());
                if (debug) {
                    debug("Set attribute " + key + "=\"" + value + "\" for "
                            + debugHtml(element));
                }
            }
        } else if (value == null || value.getType() == JsonType.NULL) {
            // Null property and/or remove attribute
            // Sets property to null before as e.g. <input> will set maxlength=0
            // when we null the property..
            if (!isAlwaysAttribute(key)) {
                element.setPropertyString(key, null);
            }
            if (element.hasAttribute(key)) {
                DomApi.wrap(element).removeAttribute(key);
                if (debug) {
                    debug("Removed attribute " + key + " from "
                            + debugHtml(element));
                }
            }
        } else {
            // Update value (which is not null)
            if (isAlwaysAttribute(key)) {
                if (debug) {
                    debug("Set attribute " + key + "=\"" + value + "\" for "
                            + debugHtml(element));
                }
                DomApi.wrap(element).setAttribute(key, value.asString());
            } else {
                switch (value.getType()) {
                case BOOLEAN:
                    if (debug) {
                        debug("Set property " + key + "=\"" + value
                                + "\" (boolean) for " + debugHtml(element));
                    }
                    element.setPropertyBoolean(key, value.asBoolean());
                    break;
                case NUMBER:
                    if (debug) {
                        debug("Set property " + key + "=\"" + value
                                + "\" (number) for " + debugHtml(element));
                    }
                    element.setPropertyDouble(key, value.asNumber());
                    break;
                case ARRAY:
                    if (debug) {
                        debug("Set property " + key + "=\"" + value
                                + "\" (array) for " + debugHtml(element));
                    }
                    element.setPropertyJSO(key, Util.json2jso(value));
                    break;
                default:
                    if (debug) {
                        debug("Set property " + key + "=\"" + value
                                + "\" (string) for " + debugHtml(element));
                    }
                    element.setPropertyString(key, value.asString());
                }

            }
        }

    }

    public static void debug(String string) {
        if (debug) {
            getLogger().info(string);
        }
    }

    public static native String debugHtml(Element element)
    /*-{
       var str = "<"+element.tagName.toLowerCase();
       for (var i=0; i < element.attributes.length; i++) {
           var a = element.attributes[i];
           str += " ";
           str += a.name;
           str += "=\"";
           str += a.value;
           str +="\"";
       }
       return str+">";
    }-*/;

    public static String debugHtml(Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            return debugHtml((Element) node);
        } else if (node.getNodeType() == Node.TEXT_NODE) {
            return "#text " + ((Text) node).getNodeValue();
        } else if (node.getNodeType() == 8) {
            return "#comment: " + node.getNodeValue();
        } else {
            return "#Unknown(" + node.getNodeType() + "): "
                    + node.getNodeValue();
        }

    }

    private static Logger getLogger() {
        return Logger.getLogger(TreeUpdater.class.getName());
    }

    private static String isAttribute(String key) {
        if (key.startsWith("attr.")) {
            return key.substring(5);
        } else {
            return null;
        }
    }

    private static boolean isAlwaysAttribute(String key) {
        // FIXME There should be separate API for attribute and property and
        // eitherOr (https://github.com/vaadin/hummingbird/issues/5)
        return key.equals("style") || key.equals("for");
    }

    public static native JavaScriptObject addDomListener(Element element,
            String type, DomListener listener)
            /*-{
                var f = $entry(listener);
                element.addEventListener(type, f);
                return f;
            }-*/;

    public static native void removeDomListener(Element element, String type,
            JavaScriptObject listener)
            /*-{
                element.removeEventListener(type, listener);
            }-*/;

    public Node createElement(Template template, TreeNode node,
            NodeContext context) {
        Node element = template.createElement(node, context);
        if (Element.is(element)) {
            if (debug) {
                Profiler.enter("TreeUpdater.createElement log");
                getLogger().info("Created element of type "
                        + Element.as(element).getTagName() + " for node "
                        + node.getId());
                Profiler.leave("TreeUpdater.createElement log");
            }

            createdElements.add((Element) element);
        }
        int nodeId = node.getId();
        node.setElement(template.getId(), element);

        storeTemplateAndNodeId(element, nodeId, template.getId());

        return element;
    }

    private static void storeTemplateAndNodeId(Node element, int nodeId,
            int templateId) {
        JsonObject jsonHack = element.cast();
        jsonHack.put("vNodeId", nodeId);
        jsonHack.put("vTemplateId", templateId);
    }

    public static JsonArray getElementIdentifier(Node node) {
        JsonArray identifier = Json.createArray();
        identifier.set(0, getNodeId(node));
        identifier.set(1, getTemplateId(node));
        return identifier;
    }

    private static int getNodeId(Node element) {
        JsonObject jsonHack = element.cast();
        return (int) jsonHack.getNumber("vNodeId");
    }

    private static int getTemplateId(Node element) {
        JsonObject jsonHack = element.cast();
        return (int) jsonHack.getNumber("vTemplateId");
    }

    public Node getOrCreateElement(TreeNode node) {
        Profiler.enter("TreeUpdater.getOrCreateElement");

        int nodeId = node.getId();
        if (node.hasProperty("TEMPLATE")) {
            int templateId = node.getProperty("TEMPLATE").getIntValue();
            Template template = templates.get(Integer.valueOf(templateId));
            assert template != null;

            Node existingNode = findDomNode(nodeId, templateId);
            if (existingNode != null) {
                Profiler.leave("TreeUpdater.getOrCreateElement");
                return existingNode;
            }

            Profiler.enter("TreeUpdater.getOrCreateElement template");

            JavaScriptObject serverProxy = template.createServerProxy(nodeId);
            Node element = createElement(template, node, new NodeContext() {

                @Override
                public TreeNodeProperty getProperty(String name) {
                    return TreeListenerHelper.getProperty(node, name);
                }

                @Override
                public void listenToProperty(String name,
                        TreeNodePropertyValueChangeListener listener) {
                    TreeListenerHelper.addListener(node, name, true, listener);
                }

                @Override
                public ListTreeNode resolveListTreeNode(String name) {
                    return (ListTreeNode) node.getProperty(name).getValue();
                }

                @Override
                public Map<String, JavaScriptObject> buildEventHandlerContext() {
                    Map<String, JavaScriptObject> contextMap = new HashMap<>();
                    contextMap.put("server", serverProxy);
                    contextMap.put("model", node.getProxy());
                    return contextMap;
                }

                @Override
                public JavaScriptObject getExpressionContext() {
                    return TreeUpdater.createNodeContext(node);
                }
            });
            Profiler.leave("TreeUpdater.getOrCreateElement template");

            Profiler.leave("TreeUpdater.getOrCreateElement");
            return element;
        } else {
            int templateId = 0;
            Node existingElement = node.getElement(templateId);
            if (existingElement != null) {

                Profiler.leave("TreeUpdater.getOrCreateElement");
                return existingElement;
            }

            String tag = (String) node.getProperty("TAG").getValue();
            assert tag != null;
            if ("#text".equals(tag)) {
                Profiler.enter("TreeUpdater.getOrCreateElement text");

                Text textNode = Document.get().createTextNode("");
                TextElementListener.bind(node, textNode);
                node.setElement(templateId, textNode);
                if (debug) {
                    debug("Created text node for nodeId=" + nodeId);
                }

                Profiler.leave("TreeUpdater.getOrCreateElement text");
                Profiler.leave("TreeUpdater.getOrCreateElement");
                return textNode;
            } else {
                Profiler.enter("TreeUpdater.getOrCreateElement basic");
                Element element = Document.get().createElement(tag);
                BasicElementListener.bind(node, element, this);
                node.setElement(templateId, element);
                if (debug) {
                    debug("Created element: " + debugHtml(element)
                            + " for nodeId=" + nodeId);
                }

                Profiler.leave("TreeUpdater.getOrCreateElement basic");
                Profiler.leave("TreeUpdater.getOrCreateElement");
                return element;
            }
        }
    }

    @FunctionalInterface
    public interface ContextFactorySupplier {
        public JavaScriptObject get();
    }

    public static JavaScriptObject createNodeContext(TreeNode node) {
        JavaScriptObject context = JavaScriptObject.createObject();

        JsArrayString propertyNames = node.getPropertyNames();
        for (int i = 0; i < propertyNames.length(); i++) {
            String name = propertyNames.get(i);
            if (name.matches("[^0-9].*")) {
                TreeNodeProperty property = node.getProperty(name);
                addContextProperty(context, name, () -> {
                    Object value = property.getValue();
                    if (value instanceof TreeNode) {
                        TreeNode child = (TreeNode) value;
                        value = child.getProxy();
                    }
                    return asJso(value);
                });
            }
        }

        return context;
    }

    public void update(JsonObject elementTemplates, JsonArray elementChanges,
            JsonArray rpc) {
        Profiler.enter("TreeUpdater.extractTemplates");
        getLogger().info("Handling template updates");
        extractTemplates(elementTemplates);
        Profiler.leave("TreeUpdater.extractTemplates");

        getLogger().info("Handling tree node changes");
        applyNodeChanges(elementChanges, rpc);

        Profiler.enter("TreeUpdater.sendCreatedEvents");
        getLogger().info("Sending created events");
        sendCreatedEvents();
        Profiler.leave("TreeUpdater.sendCreatedEvents");

        if (rpc != null) {
            Profiler.enter("TreeUpdater.runRpc");
            getLogger().info("Running rpcs");
            runRpc(rpc);
            Profiler.leave("TreeUpdater.runRpc");
        }
    }

    protected void afterNodeChanges() {
        getLogger().info("Triggering updateStyles");
        ClassListUpdater.updateStyles();
    }

    private void extractComputedProperties(JsonArray rpc) {
        for (int i = 0; i < rpc.length(); i++) {
            JsonArray invocation = rpc.getArray(i);
            String script = invocation.getString(0);
            if ("}computed".equals(script)) {
                int nodeId = (int) invocation.getNumber(1);
                String name = invocation.getString(2);
                String code = invocation.getString(3);

                TreeNode node = getNode(nodeId);
                node.addComputedProperty(name, code);
            }
        }
    }

    private void sendCreatedEvents() {
        for (Element e : createdElements) {
            NativeEvent event = Document.get().createHtmlEvent("created", false,
                    false);
            e.dispatchEvent(event);
        }
        createdElements.clear();

    }

    private void applyNodeChanges(JsonArray nodeChanges, JsonArray rpc) {
        Profiler.enter("TreeUpdater.applyNodeChanges");

        Profiler.enter("TreeUpdater.updateTree");
        updateTree(nodeChanges);
        Profiler.leave("TreeUpdater.updateTree");

        Profiler.enter("TreeUpdater.logTree");
        logTree("After changes",
                (JsonObject) idToNode.get(Integer.valueOf(1)).getProxy());
        Profiler.leave("TreeUpdater.logTree");

        if (!rootInitialized) {
            Profiler.enter("TreeUpdater.initRoot");
            initRoot();
            Profiler.leave("TreeUpdater.initRoot");
            rootInitialized = true;
        }

        // Apply any computed properties added sent as rcp before delivering
        // events
        if (rpc != null) {
            Profiler.enter("TreeUpdater.extractComputedProperties");
            extractComputedProperties(rpc);
            Profiler.leave("TreeUpdater.extractComputedProperties");
        }

        callbackQueue.flush(null);

        Profiler.enter("TreeUpdater.afterNodeChanges");
        afterNodeChanges();
        Profiler.leave("TreeUpdater.afterNodeChanges");

        Profiler.leave("TreeUpdater.applyNodeChanges");
    }

    private void runRpc(JsonArray rpcInvocations) {
        for (int invocationIndex = 0; invocationIndex < rpcInvocations
                .length(); invocationIndex++) {
            JsonArray invocation = rpcInvocations.getArray(invocationIndex);
            String script = invocation.getString(0);

            // Magic token that would cause syntax error for ordinary code
            if (script.length() > 1 && script.charAt(0) == '}') {
                switch (script.substring(1)) {
                case "promise": {
                    int id = (int) invocation.getNumber(1);
                    boolean success = invocation.getBoolean(2);
                    JavaScriptObject result = invocation.get(3);
                    resolvePromise(id, success, result);

                    break;
                }
                case "computed":
                    // Already handled, explicit case here just to avoid default
                    break;
                default:
                    throw new RuntimeException(
                            "Unsupported special RPC token: " + script);
                }
                continue;
            }

            Map<String, JavaScriptObject> context = new HashMap<>();

            int paramCount = invocation.length() - 1;

            for (int i = 0; i < paramCount; i++) {
                JavaScriptObject value;
                JsonValue paramValue = invocation.get(i + 1);

                if (paramValue.getType() == JsonType.OBJECT) {
                    JsonObject object = (JsonObject) paramValue;
                    if (object.hasKey("template") && object.hasKey("node")) {
                        int templateId = (int) object.getNumber("template");
                        int nodeId = (int) object.getNumber("node");
                        value = findDomNode(nodeId, templateId);
                    } else {
                        throw new RuntimeException(object.toJson());
                    }
                } else {
                    // "primitive" type
                    value = (JsJsonValue) paramValue;
                }

                context.put("$" + i, value);
            }

            context.put("modules", client.getModules());
            evalWithContext(context, script);
        }
    }

    private static FastStringMap<JavaScriptObject> functionCache = FastStringMap
            .create();

    public static JavaScriptObject evalWithContextFactory(
            JavaScriptObject context, String script) {
        Profiler.enter("TreeUpdater.evalWithContextFactory");

        JsArrayString newFunctionParams = JavaScriptObject.createArray().cast();
        newFunctionParams.push("context");
        newFunctionParams.push("with(context) {" + script + " }");

        Profiler.enter(
                "TreeUpdater.evalWithContextFactory getOrCreateFunction");
        JavaScriptObject function = getOrCreateFunction(newFunctionParams);
        Profiler.leave(
                "TreeUpdater.evalWithContextFactory getOrCreateFunction");

        JsArray<JavaScriptObject> params = JavaScriptObject.createArray()
                .cast();
        params.push(context);

        Profiler.enter("TreeUpdater.evalWithContextFactory runFunction");
        JavaScriptObject value = runFunction(function, params);
        Profiler.leave("TreeUpdater.evalWithContextFactory runFunction");

        Profiler.leave("TreeUpdater.evalWithContextFactory");
        return value;
    }

    public static native void addContextProperty(JavaScriptObject context,
            String name, ContextFactorySupplier supplier)
            /*-{
                Object.defineProperty(context, name, {
                  get: function() {
                    return supplier.@ContextFactorySupplier::get()();
                  }
                });
            }-*/;

    public static JavaScriptObject evalWithContext(
            Map<String, JavaScriptObject> context, String script) {

        if (debug) {
            debug("Executing: " + script + " (" + context + ")");
        }

        JsArrayString newFunctionParams = JavaScriptObject.createArray().cast();
        JsArray<JavaScriptObject> params = JavaScriptObject.createArray()
                .cast();

        for (Entry<String, JavaScriptObject> entry : context.entrySet()) {
            newFunctionParams.push(entry.getKey());

            // Can't directly use the value as JavaScriptObject because of some
            // silly runtime type checks
            Object value = entry.getValue();
            params.push((JsJsonValue) asJsonValue(value));
        }

        newFunctionParams.push(script);

        JavaScriptObject function = getOrCreateFunction(newFunctionParams);

        return runFunction(function, params);
    }

    private static JavaScriptObject getOrCreateFunction(
            JsArrayString newFunctionParams) {
        String functionSignature = asJsonValue(newFunctionParams).toJson();
        JavaScriptObject function = functionCache.get(functionSignature);
        if (function == null) {
            function = createFunction(newFunctionParams);
            functionCache.put(functionSignature, function);
        }
        return function;
    }

    private static native JavaScriptObject createFunction(
            JsArrayString newFunctionParams)
            /*-{
                // Using Function.apply to call Function constructor with variable number of parameters
                return $wnd.Function.apply($wnd.Function, newFunctionParams);
            }-*/;

    private static native JavaScriptObject runFunction(JavaScriptObject f,
            JsArray<JavaScriptObject> params)
            /*-{
                return f.apply(null, params);
            }-*/;

    private Node findDomNode(int nodeId, int templateId) {
        TreeNode node = idToNode.get(Integer.valueOf(nodeId));
        if (node == null) {
            return null;
        } else {
            Node element = node.getElement(templateId);
            if (element == null) {
                getLogger().warning("No element found for nodeId=" + nodeId);
            }
            return element;
        }
    }

    private static native void logTree(String string, JsonObject jsonObject)
    /*-{
        console.log(string, JSON.parse(JSON.stringify(jsonObject)));
    }-*/;

    private void initRoot() {
        TreeNode rootNode = getRootNode();
        TreeNode bodyNode = (TreeNode) rootNode.getProperty("containerElement")
                .getValue();

        bodyNode.setElement(0, rootElement);
        debug("Registered root element: " + debugHtml(rootElement)
                + " for nodeId=" + bodyNode.getId());

        // Remove pre-render element, they will be replaced by tree node
        // elements
        Element childElement = rootElement.getFirstChildElement();
        while (childElement != null) {
            Element next = childElement.getNextSiblingElement();
            if (childElement.hasAttribute("pre-render")) {
                childElement.removeFromParent();
            }

            childElement = next;
        }
        BasicElementListener.bind(bodyNode, rootElement, this);
    }

    public TreeNode getRootNode() {
        return ensureNodeExists(1);
    }

    private void updateTree(JsonArray elementChanges) {
        for (int i = 0; i < elementChanges.length(); i++) {
            JsonObject change = elementChanges.get(i);

            int nodeId = (int) change.getNumber("id");
            TreeNode node = ensureNodeExists(nodeId);
            String type = change.getString("type");
            JsonValue key = change.get("key");

            switch (type) {
            case "put": {
                Object convertedValue = getConvertedValue(change);
                node.getProperty(key.asString()).setValue(convertedValue);
                break;
            }
            case "splice": {
                ListTreeNode listNode = (ListTreeNode) node;

                int index = (int) change.getNumber("index");

                int removeCount;
                if (change.hasKey("remove")) {
                    removeCount = (int) change.getNumber("remove");
                } else {
                    removeCount = 0;
                }

                JsArrayObject<Object> newValues = getConvertedValues(change);

                listNode.splice(index, removeCount, newValues);

                break;
            }
            case "remove": {
                // node.getProperty adds the property if it does not exist -
                // this is unnecessary if we are going to remove it
                if (node.hasProperty(key.asString())) {
                    TreeNodeProperty property = node
                            .getProperty(key.asString());
                    Object oldValue = property.getValue();

                    property.setValue(null);

                }
                break;
            }
            case "putOverride": {
                int templateId = (int) key.asNumber();
                int overrideNodeId = (int) change.getNumber("mapValue");

                TreeNode overrideNode = ensureNodeExists(overrideNodeId);
                node.getProperty(String.valueOf(templateId))
                        .setValue(overrideNode);
                break;
            }
            case "rangeStart":
                node.getProperty("rangeStart").setValue(change.get("value"));
                break;
            case "rangeEnd": {
                node.getProperty("rangeEnd").setValue(change.get("value"));
                break;
            }
            default:
                throw new RuntimeException("Unsupported change type: " + type);
            }
        }
    }

    private JsArrayObject<Object> getConvertedValues(JsonObject change) {
        if (change.hasKey("value")) {
            return Util.json2jso(change.getArray("value")).cast();
        } else if (change.hasKey("mapValue")) {
            JsonArray mapValues = change.getArray("mapValue");
            JsArrayObject<Object> newValues = JavaScriptObject.createArray()
                    .cast();
            for (int j = 0; j < mapValues.length(); j++) {
                TreeNode node = ensureNodeExists((int) mapValues.getNumber(j));
                newValues.add(node);
            }
            return newValues;
        } else if (change.hasKey("listValue")) {
            JsonArray listValues = change.getArray("listValue");
            JsArrayObject<Object> newValues = JavaScriptObject.createArray()
                    .cast();
            for (int j = 0; j < listValues.length(); j++) {
                ListTreeNode node = ensureListNodeExists(
                        (int) listValues.getNumber(j));
                newValues.add(node);
            }
            return newValues;
        } else {
            return null;
        }
    }

    private Object getConvertedValue(JsonObject change) {
        if (change.hasKey("mapValue")) {
            return ensureNodeExists((int) change.getNumber("mapValue"));
        } else if (change.hasKey("listValue")) {
            return ensureListNodeExists((int) change.getNumber("listValue"));
        } else {
            return change.get("value");
        }
    }

    private void listNodeInsert(ListTreeNode listNode, int insertIndex,
            TreeNode childNode) {
        listNode.splice(insertIndex, 0, createSingleArray(childNode));

    }

    private static native JsArrayObject<Object> createSingleArray(Object value)
    /*-{
        return [value];
    }-*/;

    private TreeNode ensureNodeExists(int id) {
        Integer key = Integer.valueOf(id);
        TreeNode node = idToNode.get(key);
        if (node == null) {
            node = new TreeNode(id, this);
            idToNode.put(key, node);
        }
        return node;
    }

    private ListTreeNode ensureListNodeExists(int id) {
        Integer key = Integer.valueOf(id);
        ListTreeNode node = (ListTreeNode) idToNode.get(key);
        if (node == null) {
            node = new ListTreeNode(id, this);
            idToNode.put(key, node);
        }
        return node;
    }

    private void extractTemplates(JsonObject elementTemplates) {
        String[] keys = elementTemplates.keys();
        for (String keyString : keys) {
            JsonObject templateDescription = elementTemplates
                    .getObject(keyString);
            Integer templateId = Integer.valueOf(keyString);
            Template template = createTemplate(templateDescription,
                    templateId.intValue());
            templates.put(templateId, template);
        }
    }

    private Template createTemplate(JsonObject templateDescription,
            int templateId) {
        String type = templateDescription.getString("type");
        switch (type) {
        case "BoundElementTemplate":
            return new BoundElementTemplate(this, templateDescription,
                    templateId);
        case "ForElementTemplate":
            return new ForElementTemplate(this, templateDescription,
                    templateId);
        case "DynamicTextTemplate":
            return new DynamicTextTemplate(templateDescription, templateId);
        case "StaticTextTemplate":
            return new StaticTextTemplate(templateDescription, templateId);
        default:
            throw new RuntimeException("Unsupported template type: " + type);
        }
    }

    public TreeNode getNode(Integer id) {
        return idToNode.get(id);
    }

    public void saveDomListener(Integer id, String type,
            JavaScriptObject listener) {
        Map<String, JavaScriptObject> nodeListeners = domListeners.get(id);
        if (nodeListeners == null) {
            nodeListeners = new HashMap<>();
            domListeners.put(id, nodeListeners);
        }

        assert !nodeListeners.containsKey(type);
        nodeListeners.put(type, listener);
    }

    public JavaScriptObject removeSavedDomListener(String type, Integer id) {
        Map<String, JavaScriptObject> nodeListeners = domListeners.get(id);
        JavaScriptObject listener = nodeListeners.remove(type);
        return listener;
    }

    public void removeSavedDomListeners(Integer id) {
        assert domListeners.containsKey(id);
        assert domListeners.get(id).isEmpty();

        domListeners.remove(id);
    }

    public Template getTemplate(int templateId) {
        return templates.get(Integer.valueOf(templateId));
    }

    public CallbackQueue getCallbackQueue() {
        return callbackQueue;
    }

    public int registerPromise(JavaScriptObject resolve,
            JavaScriptObject reject) {
        int id = nextPromiseId++;

        promises.put(Integer.valueOf(id),
                new JavaScriptObject[] { resolve, reject });

        return id;
    }

    public void resolvePromise(int id, boolean success,
            JavaScriptObject result) {
        JavaScriptObject[] resolvers = promises.remove(Integer.valueOf(id));
        if (resolvers == null) {
            throw new RuntimeException("Promise " + id
                    + " is already resolved (or never registered)");
        }

        JavaScriptObject resolver = resolvers[success ? 0 : 1];

        callResolveFunction(resolver, result);
    }

    private static native void callResolveFunction(JavaScriptObject f,
            JavaScriptObject result)
            /*-{
                f(result);
            }-*/;
}
