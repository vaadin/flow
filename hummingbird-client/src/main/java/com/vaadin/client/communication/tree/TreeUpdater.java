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
import java.util.logging.Logger;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.Window.Location;
import com.vaadin.client.ApplicationConnection.Client;
import com.vaadin.client.communication.Polymer;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.communication.tree.NodeListener.Change;
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

    private Map<Integer, JsonObject> idToNode = new HashMap<>();
    private Map<JsonObject, Integer> nodeToId = new HashMap<>();

    // Node id -> template id -> override node id
    // XXX seems like this is never actually read, could maybe be removed?
    private Map<Integer, Map<Integer, Integer>> overrides = new HashMap<>();

    private Map<Integer, List<NodeListener>> listeners = new HashMap<>();

    private Map<Integer, Map<String, JavaScriptObject>> domListeners = new HashMap<>();

    private boolean rootInitialized = false;

    private Map<Integer, Node> nodeIdToBasicElement = new HashMap<>();

    private Map<Integer, Map<Template, Node>> nodeIdToTemplateToElement = new HashMap<>();

    private NodeListener treeUpdater = new NodeListener() {
        @Override
        public void putNode(PutNodeChange change) {
            JsonObject parent = idToNode.get(change.getId());
            JsonObject childNode = ensureNodeExists(change.getValue());
            parent.put(change.getKey(), childNode);
        }

        @Override
        public void put(PutChange change) {
            JsonObject node = idToNode.get(change.getId());
            node.put(change.getKey(), change.getValue());
        }

        @Override
        public void listInsertNode(ListInsertNodeChange change) {
            JsonObject node = idToNode.get(change.getId());
            JsonArray array = node.getArray(change.getKey());
            if (array == null) {
                array = Json.createArray();
                node.put(change.getKey(), array);
            }
            JsonObject child = ensureNodeExists(change.getValue());
            array.set(change.getIndex(), child);
        }

        @Override
        public void listInsert(ListInsertChange change) {
            JsonObject node = idToNode.get(change.getId());
            JsonArray array = node.getArray(change.getKey());
            if (array == null) {
                array = Json.createArray();
                node.put(change.getKey(), array);
            }
            array.set(change.getIndex(), change.getValue());
        }

        @Override
        public void listRemove(ListRemoveChange change) {
            JsonObject node = idToNode.get(change.getId());
            JsonArray array = node.getArray(change.getKey());
            assert array != null;

            JsonValue value = array.get(change.getIndex());
            change.setRemovedValue(value);

            array.remove(change.getIndex());
        }

        @Override
        public void remove(RemoveChange change) {
            JsonObject node = idToNode.get(change.getId());
            JsonValue value = node.get(change.getKey());
            change.setValue(value);

            unregisterValue(value);
            node.remove(change.getKey());
        }

        private void unregisterValue(JsonValue value) {
            switch (value.getType()) {
            case OBJECT:
                unregisterNode((JsonObject) value);
                break;
            case ARRAY:
                JsonArray array = (JsonArray) value;
                for (int i = 0; i < array.length(); i++) {
                    unregisterValue(array.get(i));
                }
                break;
            default:
                // All other are ok
            }
        }

        private void unregisterNode(final JsonObject node) {
            // Clean up after all listeners have been run
            Scheduler.get().scheduleFinally(new ScheduledCommand() {
                @Override
                public void execute() {
                    Integer id = nodeToId.remove(node);
                    idToNode.remove(id);

                    nodeIdToBasicElement.remove(id);

                    listeners.remove(id);
                    domListeners.remove(id);
                }
            });
        }

        @Override
        public void putOverride(PutOverrideChange change) {
            int nodeId = change.getId();
            int templateId = change.getKey();
            int overrideNodeId = change.getValue();

            ensureNodeExists(overrideNodeId);

            Map<Integer, Integer> nodeOverrides = overrides.get(nodeId);
            if (nodeOverrides == null) {
                nodeOverrides = new HashMap<>();
                overrides.put(nodeId, nodeOverrides);
            }

            nodeOverrides.put(templateId, overrideNodeId);
        }
    };

    private ServerRpcQueue rpcQueue;

    private Client client;

    public void init(Element rootElement, ServerRpcQueue rpcQueue,
            Client client) {
        assert this.rootElement == null : "Can only init once";
        assert rpcQueue != null;
        assert client != null;

        this.rootElement = rootElement;
        this.rpcQueue = rpcQueue;
        this.client = client;
    }

    public void sendRpc(String callbackName, JsonArray arguments) {
        /*
         * Must invoke manually as the RPC interface can't be used in GWT
         * because of the JSONArray parameter
         */
        rpcQueue.add(new MethodInvocation(callbackName, arguments), false);
        rpcQueue.flush();
    }

    public static void setAttributeOrProperty(Element element, String key,
            JsonValue value) {
        assert element != null;
        if (value == null || value.getType() == JsonType.NULL) {
            // Null property and/or remove attribute
            // Sets property to null before as e.g. <input> will set maxlength=0
            // when we null the property..
            if (!isAlwaysAttribute(key)) {
                element.setPropertyString(key, null);
            }
            if (element.hasAttribute(key)) {
                Polymer.dom(element).removeAttribute(key);
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
                Polymer.dom(element).setAttribute(key, value.asString());
            } else {
                if (value.getType() == JsonType.BOOLEAN) {
                    if (debug) {
                        debug("Set property " + key + "=\"" + value
                                + "\" (boolean) for " + debugHtml(element));
                    }
                    element.setPropertyBoolean(key, value.asBoolean());
                } else if (value.getType() == JsonType.NUMBER) {
                    if (debug) {
                        debug("Set property " + key + "=\"" + value
                                + "\" (number) for " + debugHtml(element));
                    }
                    element.setPropertyDouble(key, value.asNumber());
                } else {
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
        } else {
            return "#Unknown: " + node.getNodeValue();
        }
    }

    private static Logger getLogger() {
        return Logger.getLogger(TreeUpdater.class.getName());
    }

    private static boolean isAlwaysAttribute(String key) {
        return key.equals("class") || key.equals("style");
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

    public Node createElement(Template template, JsonObject node,
            NodeContext context) {
        Node element = template.createElement(node, context);

        Integer nodeId = nodeToId.get(node);
        Map<Template, Node> templateToElement = nodeIdToTemplateToElement
                .get(nodeId);
        if (templateToElement == null) {
            templateToElement = new HashMap<>();
            nodeIdToTemplateToElement.put(nodeId, templateToElement);
        }

        templateToElement.put(template, element);

        storeTemplateAndNodeId(element, nodeId.intValue(), template.getId());

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

    public Node getOrCreateElement(JsonObject node) {
        Integer nodeId = nodeToId.get(node);
        if (node.hasKey("TEMPLATE")) {
            int templateId = (int) node.getNumber("TEMPLATE");
            Template template = templates.get(Integer.valueOf(templateId));
            assert template != null;

            Node existingNode = findDomNode(nodeId, templateId);
            if (existingNode != null) {
                return existingNode;
            }

            return createElement(template, node,
                    new NodeContext(new ElementNotifier(this, node, ""),
                            template.createServerProxy(nodeId),
                            template.createModelProxy(node, this)));
        } else {
            if (nodeIdToBasicElement.containsKey(nodeId)) {
                return nodeIdToBasicElement.get(nodeId);
            }

            String tag = node.getString("TAG");
            if ("#text".equals(tag)) {
                Text textNode = Document.get().createTextNode("");
                addNodeListener(node, new TextElementListener(textNode));
                nodeIdToBasicElement.put(nodeId, textNode);
                if (debug) {
                    debug("Created text node for nodeId=" + nodeId);
                }
                return textNode;
            } else {
                Element element = Document.get().createElement(tag);
                addNodeListener(node,
                        new BasicElementListener(this, node, element));
                nodeIdToBasicElement.put(nodeId, element);
                if (debug) {
                    debug("Created element: " + debugHtml(element)
                            + " for nodeId=" + nodeId);
                }
                return element;
            }
        }
    }

    public void applyLocalChange(Change change) {
        JsonArray transactionChanges = Json.createArray();
        transactionChanges.set(transactionChanges.length(), (JsonValue) change);
        applyNodeChanges(transactionChanges);
    }

    public void update(JsonObject elementTemplates, JsonArray elementChanges,
            JsonArray rpc) {
        extractTemplates(elementTemplates);

        applyNodeChanges(elementChanges);

        if (rpc != null) {
            runRpc(rpc);
        }
    }

    private void applyNodeChanges(JsonArray nodeChanges) {
        updateTree(nodeChanges);

        logTree("After changes", idToNode.get(Integer.valueOf(1)));

        if (!rootInitialized) {
            initRoot();
            rootInitialized = true;
        }

        notifyListeners(nodeChanges);
    }

    private void runRpc(JsonArray rpcInvocations) {
        for (int invocationIndex = 0; invocationIndex < rpcInvocations
                .length(); invocationIndex++) {
            JsonArray invocation = rpcInvocations.getArray(invocationIndex);
            String script = invocation.getString(0);

            int paramCount = invocation.length() - 1;

            JsArray<JavaScriptObject> params = JavaScriptObject.createArray()
                    .cast();
            JsArrayString newFunctionParams = JavaScriptObject.createArray()
                    .cast();
            for (int i = 0; i < paramCount; i++) {
                JavaScriptObject value;
                JsonValue paramValue = invocation.get(i + 1);

                if (paramValue.getType() == JsonType.ARRAY) {
                    throw new RuntimeException("Not supported");
                } else if (paramValue.getType() == JsonType.OBJECT) {
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

                params.push(value);
                newFunctionParams.push("$" + i);
            }
            if (debug) {
                String paramString = params.join(", ");
                debug("Executing: " + script + " (" + paramString + ")");
            }

            newFunctionParams.push("modules");
            params.push(client.getModules());
            newFunctionParams.push(script);
            createAndRunFunction(newFunctionParams, params);
        }
    }

    public static native void createAndRunFunction(
            JsArrayString newFunctionParams, JsArray<JavaScriptObject> params)
            /*-{
                // Using Function.apply to call Function constructor with variable number of parameters
                // Then use apply on the created function to run the actual code
                $wnd.Function.apply($wnd.Function, newFunctionParams).apply(null, params);
            }-*/;

    private Node findDomNode(int nodeId, int templateId) {
        if (templateId == 0) {
            Node n = nodeIdToBasicElement.get(Integer.valueOf(nodeId));
            if (n == null) {
                getLogger().warning("No element found for nodeId=" + nodeId);
            }
            return n;
        } else {
            Map<Template, Node> templateToElement = nodeIdToTemplateToElement
                    .get(Integer.valueOf(nodeId));
            if (templateToElement == null) {
                return null;
            }

            return templateToElement
                    .get(templates.get(Integer.valueOf(templateId)));
        }
    }

    private static native void logTree(String string, JsonObject jsonObject)
    /*-{
        console.log(string, jsonObject);
    }-*/;

    private void notifyListeners(JsonArray elementChanges) {
        for (int i = 0; i < elementChanges.length(); i++) {
            Change change = elementChanges.get(i);
            int id = change.getId();

            List<NodeListener> list = listeners.get(Integer.valueOf(id));
            if (list != null) {
                for (NodeListener nodeListener : new ArrayList<>(list)) {
                    nodeListener.notify(change);
                }
            }
        }
    }

    private void initRoot() {
        JsonObject rootNode = idToNode.get(Integer.valueOf(1));
        JsonObject bodyNode = rootNode.get("containerElement");

        // TODO Remove UI element hack
        nodeIdToBasicElement.put(2, rootElement);
        debug("Registered root element: " + debugHtml(rootElement)
                + " for nodeId=" + 2);

        addNodeListener(bodyNode,
                new BasicElementListener(this, bodyNode, rootElement));
    }

    public void addNodeListener(JsonObject node, NodeListener listener) {
        Integer id = nodeToId.get(node);
        List<NodeListener> list = listeners.get(id);
        if (list == null) {
            list = new ArrayList<>();
            listeners.put(id, list);
        }
        list.add(listener);
    }

    private void updateTree(JsonArray elementChanges) {
        for (int i = 0; i < elementChanges.length(); i++) {
            Change change = elementChanges.get(i);

            ensureNodeExists(change.getId());

            treeUpdater.notify(change);
        }
    }

    private JsonObject ensureNodeExists(int id) {
        Integer key = Integer.valueOf(id);
        JsonObject node = idToNode.get(key);
        if (node == null) {
            node = Json.createObject();
            idToNode.put(key, node);
            nodeToId.put(node, key);
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

    public JsonObject getNode(Integer id) {
        return idToNode.get(id);
    }

    public Integer getNodeId(JsonObject node) {
        return nodeToId.get(node);
    }

    public void saveDomListener(Integer id, String type,
            JavaScriptObject listener) {
        Map<String, JavaScriptObject> nodeListeners = domListeners.get(id);
        if (nodeListeners == null) {
            nodeListeners = new HashMap<>();
            domListeners.put(id, nodeListeners);
        }

        assert!nodeListeners.containsKey(type);
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

}
