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
package com.vaadin.client.communication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.client.js.JsFunction;
import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class TreeUpdater {

    public class StaticChildrenElementTemplate extends BoundElementTemplate {

        private final int[] childElementTemplates;

        public StaticChildrenElementTemplate(JsonObject templateDescription) {
            super(templateDescription);

            JsonArray children = templateDescription.getArray("children");
            childElementTemplates = new int[children.length()];
            for (int i = 0; i < childElementTemplates.length; i++) {
                childElementTemplates[i] = (int) children.getNumber(i);
            }
        }

        @Override
        protected void initElement(JsonObject node, Element element) {
            super.initElement(node, element);

            for (int templateId : childElementTemplates) {
                element.appendChild(createTemplateElement(node, templateId));
            }
        }

    }

    public class ForElementTemplate extends BoundElementTemplate {
        private final int childTemplateId;

        public ForElementTemplate(JsonObject templateDescription) {
            super(templateDescription);

            childTemplateId = (int) templateDescription
                    .getNumber("childTemplate");
        }

        @Override
        protected void listInsertNode(JsonObject node, Element element,
                ListInsertNodeChange change) {
            JsonObject childNode = idToNode.get(change.getValue());
            Node child = createTemplateElement(childNode, childTemplateId);
            insertNodeAtIndex(element, child, change.getIndex());
        }
    }

    public class BoundElementTemplate implements Template {

        private final String tag;
        private final Map<String, String> defaultAttributeValues;
        private final Map<String, String> propertyToAttribute;

        public BoundElementTemplate(JsonObject templateDescription) {
            tag = templateDescription.getString("tag");

            defaultAttributeValues = readStringMap(
                    templateDescription.getObject("defaultAttributes"));

            propertyToAttribute = readStringMap(
                    templateDescription.getObject("attributeBindings"));
        }

        private Map<String, String> readStringMap(JsonObject json) {
            Map<String, String> values = new HashMap<>();
            for (String name : json.keys()) {
                assert json.get(name).getType() == JsonType.STRING;
                values.put(name, json.getString(name));
            }
            return values;
        }

        @Override
        public Node createElement(final JsonObject node) {
            final Element element = Document.get().createElement(tag);
            initElement(node, element);

            addNodeListener(node, new NodeListener() {
                @Override
                public void remove(RemoveChange change) {
                    throw new RuntimeException("Not yet implemented");
                }

                @Override
                public void putNode(PutNodeChange change) {
                    throw new RuntimeException("Not yet implemented");
                }

                @Override
                public void put(PutChange change) {
                    BoundElementTemplate.this.put(node, element, change);
                }

                @Override
                public void listRemove(ListRemoveChange change) {
                    throw new RuntimeException("Not yet implemented");
                }

                @Override
                public void listInsertNode(ListInsertNodeChange change) {
                    BoundElementTemplate.this.listInsertNode(node, element,
                            change);
                }

                @Override
                public void listInsert(ListInsertChange change) {
                    throw new RuntimeException("Not yet implemented");
                }
            });

            return element;
        }

        protected void put(JsonObject node, Element element, PutChange change) {
            String key = change.getKey();
            String targetAttribute = propertyToAttribute.get(key);
            if (targetAttribute != null) {
                element.setAttribute(targetAttribute,
                        change.getValue().asString());
            } else if (!"TEMPLATE".equals(change.getKey())) {
                throw new RuntimeException(
                        "Unsupported property change: " + change.getKey());
            }
        }

        protected void initElement(JsonObject node, Element element) {
            for (Entry<String, String> entry : defaultAttributeValues
                    .entrySet()) {
                element.setAttribute(entry.getKey(), entry.getValue());
            }
        }

        protected void listInsertNode(JsonObject node, Element element,
                ListInsertNodeChange change) {
            throw new RuntimeException(
                    "Not implemented for " + getClass().getSimpleName());
        }
    }

    @JsType
    public interface RemoveChange extends Change {
        @JsProperty
        String getKey();

        @JsProperty
        JsonValue getValue();

        @JsProperty
        void setValue(JsonValue value);
    }

    @JsType
    public interface ListRemoveChange extends Change {
        @JsProperty
        String getKey();

        @JsProperty
        int getIndex();

        @JsProperty
        JsonValue getValue();

        @JsProperty
        void setValue(JsonValue value);
    }

    @JsType
    public interface ListInsertChange extends PutChange {
        @JsProperty
        int getIndex();
    }

    @JsType
    public interface ListInsertNodeChange extends PutNodeChange {
        @JsProperty
        int getIndex();
    }

    @JsType
    public interface PutChange extends Change {
        @JsProperty
        String getKey();

        @JsProperty
        JsonValue getValue();
    }

    @JsType
    public interface PutNodeChange extends Change {
        @JsProperty
        int getValue();

        @JsProperty
        String getKey();
    }

    @JsFunction
    public interface DomListener {
        public void handleEvent(JavaScriptObject event);
    }

    public class BasicElementListener implements NodeListener {

        private JsonObject node;
        private String tag;
        private Element element;

        public BasicElementListener(JsonObject node, String tag,
                Element element) {
            this.node = node;
            this.tag = tag;
            this.element = element;
        }

        @Override
        public void putNode(PutNodeChange change) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void put(PutChange change) {
            String value = change.getValue().asString();
            String key = change.getKey();
            if ("TAG".equals(key)) {
                return;
            }

            if (value == null) {
                element.removeAttribute(key);
            } else {
                element.setAttribute(key, value);
            }
        }

        @Override
        public void listInsertNode(ListInsertNodeChange change) {
            if ("CHILDREN".equals(change.getKey())) {
                insertChild(change.getIndex(), idToNode.get(change.getValue()));
            } else {
                throw new RuntimeException("Not supported: " + change.getKey());
            }
        }

        private void insertChild(int index, JsonObject node) {
            Node child = createElement(node);
            insertNodeAtIndex(element, child, index);
        }

        @Override
        public void listInsert(ListInsertChange change) {
            if ("LISTENERS".equals(change.getKey())) {
                addListener(change.getValue().asString());
            } else {
                throw new RuntimeException("Not supported: " + change.getKey());
            }
        }

        @Override
        public void listRemove(ListRemoveChange change) {
            switch (change.getKey()) {
            case "LISTENERS":
                removeListener(change.getValue().asString());
                break;
            case "CHILDREN":
                element.getChild(change.getIndex()).removeFromParent();
                break;
            default:
                throw new RuntimeException("Not supported: " + change.getKey());
            }
        }

        private void addListener(final String type) {
            final Integer id = nodeToId.get(node);
            DomListener listener = new DomListener() {
                @Override
                public void handleEvent(JavaScriptObject event) {
                    sendEventToServer(id.intValue(), type);
                }
            };

            JavaScriptObject wrappedListener = addDomListener(element, type,
                    listener);

            Map<String, JavaScriptObject> nodeListeners = domListeners.get(id);
            if (nodeListeners == null) {
                nodeListeners = new HashMap<>();
                domListeners.put(id, nodeListeners);
            }

            assert!nodeListeners.containsKey(type);
            nodeListeners.put(type, wrappedListener);

        }

        private void removeListener(String type) {
            Integer id = nodeToId.get(node);
            Map<String, JavaScriptObject> nodeListeners = domListeners.get(id);
            JavaScriptObject listener = nodeListeners.remove(type);
            assert listener != null;

            removeDomListener(element, type, listener);
        }

        @Override
        public void remove(RemoveChange change) {
            if ("LISTENERS".equals(change.getKey())) {
                // This means we have no listeners left, remove the map as well
                Integer id = nodeToId.get(node);

                assert domListeners.containsKey(id);
                assert domListeners.get(id).isEmpty();

                domListeners.remove(id);
            } else {
                throw new RuntimeException("Not supported: " + change.getKey());
            }
        }

    }

    private void sendEventToServer(int nodeId, String eventType) {
        JsonArray arguments = Json.createArray();
        arguments.set(0, nodeId);
        arguments.set(1, eventType);
        Object[] parameters = new Object[] { "vEvent", arguments };

        /*
         * Must invoke manually as the RPC interface can't be used in GWT
         * because of the JSONArray parameter
         */
        rpcQueue.add(new JavaScriptMethodInvocation(
                "com.vaadin.ui.JavaScript$JavaScriptCallbackRpc", "call",
                parameters), false);
        rpcQueue.flush();
    }

    public class TextElementListener implements NodeListener {

        private JsonObject node;
        private Text textNode;

        public TextElementListener(JsonObject node, Text textNode) {
            this.node = node;
            this.textNode = textNode;
        }

        @Override
        public void putNode(PutNodeChange change) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void put(PutChange change) {
            String key = change.getKey();
            switch (key) {
            case "TAG":
                break;
            case "content":
                textNode.setData(change.getValue().asString());
                break;
            default:
                throw new RuntimeException("Unsupported key: " + key);
            }
        }

        @Override
        public void listInsertNode(ListInsertNodeChange change) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void listInsert(ListInsertChange change) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void listRemove(ListRemoveChange change) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void remove(RemoveChange change) {
            throw new RuntimeException("Not supported");
        }

    }

    public class DynamicTemplateListener implements NodeListener {

        private JsonObject node;
        private String template;
        private Element element;

        public DynamicTemplateListener(JsonObject node, String template,
                Element element) {
            this.node = node;
            this.template = template;
            this.element = element;
        }

        @Override
        public void putNode(PutNodeChange change) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void put(PutChange change) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void listInsertNode(ListInsertNodeChange change) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void listInsert(ListInsertChange change) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void listRemove(ListRemoveChange change) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void remove(RemoveChange change) {
            throw new RuntimeException("Not supported");
        }
    }

    public interface NodeListener {

        void putNode(PutNodeChange change);

        void put(PutChange change);

        /**
         * @since
         * @param change
         */
        void listInsertNode(ListInsertNodeChange change);

        /**
         * @since
         * @param change
         */
        void listInsert(ListInsertChange change);

        /**
         * @since
         * @param change
         */
        void listRemove(ListRemoveChange change);

        /**
         * @since
         * @param change
         */
        void remove(RemoveChange change);

    }

    private interface Template {
        public Node createElement(JsonObject node);
    }

    @JsType
    public interface Change {
        @JsProperty
        public int getId();

        @JsProperty
        public String getType();
    }

    private Element rootElement;

    private Map<Integer, Template> templates = new HashMap<>();

    private Map<Integer, JsonObject> idToNode = new HashMap<>();
    private Map<JsonObject, Integer> nodeToId = new HashMap<>();

    private Map<Integer, List<NodeListener>> listeners = new HashMap<>();

    private Map<Integer, Map<String, JavaScriptObject>> domListeners = new HashMap<>();

    private boolean rootInitialized = false;

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
            change.setValue(value);

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

                    listeners.remove(id);
                    domListeners.remove(id);
                }
            });
        }
    };

    private ServerRpcQueue rpcQueue;

    public void init(Element rootElement, ServerRpcQueue rpcQueue) {
        assert this.rootElement == null : "Can only init once";

        this.rootElement = rootElement;
        this.rpcQueue = rpcQueue;
    }

    private static native JavaScriptObject addDomListener(Element element,
            String type, DomListener listener)
            /*-{
                var f = $entry(listener);
                element.addEventListener(type, f);
                return f;
            }-*/;

    private static native void removeDomListener(Element element, String type,
            JavaScriptObject listener)
            /*-{
                element.removeEventListener(type, listener);
            }-*/;

    private Node createElement(JsonObject node) {
        if (node.hasKey("TEMPLATE")) {
            return createTemplateElement(node,
                    (int) node.getNumber("TEMPLATE"));
        } else {
            String tag = node.getString("TAG");
            if ("#text".equals(tag)) {
                Text textNode = Document.get().createTextNode("");
                addNodeListener(node, new TextElementListener(node, textNode));
                return textNode;
            } else {
                Element element = Document.get().createElement(tag);
                addNodeListener(node,
                        new BasicElementListener(node, tag, element));
                return element;
            }
        }
    }

    private Node createTemplateElement(JsonObject node, int templateId) {
        Template template = templates.get(Integer.valueOf(templateId));
        assert template != null;

        return template.createElement(node);
    }

    private static void insertNodeAtIndex(Element parent, Node child,
            int index) {
        if (parent.getChildCount() == index) {
            parent.appendChild(child);
        } else {
            Node reference = parent.getChildNodes().getItem(index);
            parent.insertBefore(child, reference);
        }
    }

    public void update(JsonObject elementTemplates, JsonArray elementChanges) {
        extractTemplates(elementTemplates);

        updateTree(elementChanges);

        logTree("After changes", idToNode.get(Integer.valueOf(1)));

        if (!rootInitialized) {
            initRoot();
            rootInitialized = true;
        }

        notifyListeners(elementChanges);
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
                    notifyListener(nodeListener, change);
                }
            }
        }
    }

    private void notifyListener(NodeListener nodeListener, Change change) {
        switch (change.getType()) {
        case "putNode":
            nodeListener.putNode((PutNodeChange) change);
            break;
        case "put":
            nodeListener.put((PutChange) change);
            break;
        case "listInsertNode":
            nodeListener.listInsertNode((ListInsertNodeChange) change);
            break;
        case "listInsert":
            nodeListener.listInsert((ListInsertChange) change);
            break;
        case "listRemove":
            nodeListener.listRemove((ListRemoveChange) change);
            break;
        case "remove":
            nodeListener.remove((RemoveChange) change);
            break;
        default:
            throw new RuntimeException(
                    "Unsupported change type: " + change.getType());
        }
    }

    private void initRoot() {
        JsonObject rootNode = idToNode.get(Integer.valueOf(1));
        JsonObject bodyNode = rootNode.get("body");
        addNodeListener(bodyNode, new BasicElementListener(bodyNode,
                bodyNode.getString("TAG"), rootElement));
    }

    private void addNodeListener(JsonObject node, NodeListener listener) {
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

            notifyListener(treeUpdater, change);
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
            Template template = createTemplate(templateDescription);
            templates.put(Integer.valueOf(keyString), template);
        }
    }

    private Template createTemplate(JsonObject templateDescription) {
        String type = templateDescription.getString("type");
        switch (type) {
        case "BoundElementTemplate":
            return new BoundElementTemplate(templateDescription);
        case "ForElementTemplate":
            return new ForElementTemplate(templateDescription);
        case "StaticChildrenElementTemplate":
            return new StaticChildrenElementTemplate(templateDescription);
        default:
            throw new RuntimeException("Unsupported template type: " + type);
        }
    }
}
