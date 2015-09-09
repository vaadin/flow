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
import java.util.Map;
import java.util.logging.Logger;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.Window.Location;
import com.vaadin.client.ApplicationConnection.Client;
import com.vaadin.client.communication.DomApi;
import com.vaadin.client.communication.ServerRpcQueue;
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

    public static native JsonValue asJsonValue(Object value)
    /*-{
        return value;
    }-*/;

    public static void setAttributeOrProperty(Element element, String key,
            Object objectValue) {
        JsonValue value = asJsonValue(objectValue);
        assert element != null;
        if (value == null || value.getType() == JsonType.NULL) {
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

    public Node createElement(Template template, TreeNode node,
            NodeContext context) {
        Node element = template.createElement(node, context);

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
        int nodeId = node.getId();
        if (node.hasProperty("TEMPLATE")) {
            int templateId = node.getProperty("TEMPLATE").getIntValue();
            Template template = templates.get(Integer.valueOf(templateId));
            assert template != null;

            Node existingNode = findDomNode(nodeId, templateId);
            if (existingNode != null) {
                return existingNode;
            }

            JavaScriptObject serverProxy = template.createServerProxy(nodeId);
            return createElement(template, node, new NodeContext() {
                @Override
                public JavaScriptObject getServerProxy() {
                    return serverProxy;
                }

                @Override
                public TreeNodeProperty resolveProperty(String name) {
                    return node.getProperty(name);
                }

                @Override
                public EventArray resolveArrayProperty(String name) {
                    return node.getArrayProperty(name);
                }

            });
        } else {
            int templateId = 0;
            Node existingElement = node.getElement(templateId);
            if (existingElement != null) {
                return existingElement;
            }

            String tag = (String) node.getProperty("TAG").getValue();
            if ("#text".equals(tag)) {
                Text textNode = Document.get().createTextNode("");
                TextElementListener.bind(node, textNode);
                node.setElement(templateId, textNode);
                if (debug) {
                    debug("Created text node for nodeId=" + nodeId);
                }
                return textNode;
            } else {
                Element element = Document.get().createElement(tag);
                BasicElementListener.bind(node, element, this);
                node.setElement(templateId, element);
                if (debug) {
                    debug("Created element: " + debugHtml(element)
                            + " for nodeId=" + nodeId);
                }
                return element;
            }
        }
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

        logTree("After changes",
                (JsonObject) idToNode.get(Integer.valueOf(1)).getProxy());

        if (!rootInitialized) {
            initRoot();
            rootInitialized = true;
        }

        callbackQueue.flush();
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
        console.log(string, jsonObject);
    }-*/;

    private void initRoot() {
        TreeNode rootNode = idToNode.get(Integer.valueOf(1));
        TreeNode bodyNode = (TreeNode) rootNode.getProperty("containerElement")
                .getValue();

        bodyNode.setElement(0, rootElement);
        debug("Registered root element: " + debugHtml(rootElement)
                + " for nodeId=" + bodyNode.getId());

        BasicElementListener.bind(bodyNode, rootElement, this);
    }

    private void updateTree(JsonArray elementChanges) {
        for (int i = 0; i < elementChanges.length(); i++) {
            JsonObject change = elementChanges.get(i);

            int nodeId = (int) change.getNumber("id");
            TreeNode node = ensureNodeExists(nodeId);
            String type = change.getString("type");
            JsonValue key = change.get("key");
            JsonValue value = change.get("value");

            switch (type) {
            case "putNode": {
                TreeNode child = ensureNodeExists(
                        (int) change.getNumber("value"));
                node.getProperty(key.asString()).setValue(child);
                break;
            }
            case "put":
                node.getProperty(key.asString()).setValue(value);
                break;
            case "listInsertNode": {
                EventArray array = node.getArrayProperty(key.asString());
                TreeNode child = ensureNodeExists((int) value.asNumber());
                array.splice((int) change.getNumber("index"), 0, child);
                break;
            }
            case "listInsert": {
                EventArray array = node.getArrayProperty(key.asString());
                array.splice((int) change.getNumber("index"), 0, value);
                break;
            }
            case "listRemove": {
                EventArray array = node.getArrayProperty(key.asString());
                array.splice((int) change.getNumber("index"), 1);
                break;
            }
            case "remove": {
                TreeNodeProperty property = node.getProperty(key.asString());
                Object oldValue = property.getValue();

                property.setValue(null);

                break;
            }
            case "putOverride": {
                int templateId = (int) key.asNumber();
                int overrideNodeId = (int) value.asNumber();

                TreeNode overrideNode = ensureNodeExists(overrideNodeId);
                node.getProperty(String.valueOf(templateId))
                        .setValue(overrideNode);
                break;
            }
            default:
                throw new RuntimeException(
                        "Unsupported change type: " + change.getType());
            }
        }
    }

    private TreeNode ensureNodeExists(int id) {
        Integer key = Integer.valueOf(id);
        TreeNode node = idToNode.get(key);
        if (node == null) {
            node = new TreeNode(id, callbackQueue);
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
