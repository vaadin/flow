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
import java.util.logging.Logger;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.client.js.JsFunction;
import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.vaadin.shared.communication.MethodInvocation;

import elemental.js.json.JsJsonValue;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class TreeUpdater {

    public class StaticTextTemplate implements Template {

        private String content;

        public StaticTextTemplate(JsonObject templateDescription,
                int templateId) {
            content = templateDescription.getString("content");
        }

        @Override
        public Node createElement(JsonObject node, ElementNotifier notifier) {
            return Document.get().createTextNode(content);
        }

    }

    public class TextUpdater implements ElementUpdater {

        private final Text textNode;
        private final String binding;

        public TextUpdater(DynamicTextTemplate template, Text textNode) {
            binding = template.getBinding();
            this.textNode = textNode;
        }

        @Override
        public void put(String scope, PutChange change) {
            if (scope.equals(binding)) {
                textNode.setData(change.getValue().asString());
            }
        }

        @Override
        public void remove(String scope, RemoveChange change) {
            if (scope.equals(binding)) {
                textNode.setData("");
            }
        }

        @Override
        public void putNode(String scope, PutNodeChange change) {
            // Don't care
        }

        @Override
        public void putOverride(String scope, PutOverrideChange change) {
            // Don't care
        }

        @Override
        public void listRemove(String scope, ListRemoveChange change) {
            // Don't care
        }

        @Override
        public void listInsert(String scope, ListInsertChange change) {
            // Don't care
        }

        @Override
        public void listInsertNode(String scope, ListInsertNodeChange change) {
            // Don't care
        }

    }

    public class DynamicTextTemplate implements Template {

        private String binding;

        public DynamicTextTemplate(JsonObject templateDescription,
                int templateId) {
            binding = templateDescription.getString("binding");
        }

        public String getBinding() {
            return binding;
        }

        @Override
        public Node createElement(JsonObject node, ElementNotifier notifier) {
            Text text = Document.get().createTextNode("");

            notifier.addUpdater(new TextUpdater(this, text));

            return text;
        }
    }

    public class ElementNotifier {
        private final List<ElementUpdater> updaters = new ArrayList<>();

        public ElementNotifier(JsonObject node, String scope) {
            addNodeListener(node, new NodeListener() {
                @Override
                public void putNode(PutNodeChange change) {
                    String parameter = scope + change.getKey();
                    for (ElementUpdater updater : new ArrayList<>(updaters)) {
                        updater.putNode(parameter, change);
                    }
                }

                @Override
                public void put(PutChange change) {
                    String parameter = scope + change.getKey();
                    for (ElementUpdater updater : new ArrayList<>(updaters)) {
                        updater.put(parameter, change);
                    }
                }

                @Override
                public void listInsertNode(ListInsertNodeChange change) {
                    String parameter = scope + change.getKey();
                    for (ElementUpdater updater : new ArrayList<>(updaters)) {
                        updater.listInsertNode(parameter, change);
                    }
                }

                @Override
                public void listInsert(ListInsertChange change) {
                    String parameter = scope + change.getKey();
                    for (ElementUpdater updater : new ArrayList<>(updaters)) {
                        updater.listInsert(parameter, change);
                    }
                }

                @Override
                public void listRemove(ListRemoveChange change) {
                    String parameter = scope + change.getKey();
                    for (ElementUpdater updater : new ArrayList<>(updaters)) {
                        updater.listRemove(parameter, change);
                    }
                }

                @Override
                public void remove(RemoveChange change) {
                    String parameter = scope + change.getKey();
                    for (ElementUpdater updater : new ArrayList<>(updaters)) {
                        updater.remove(parameter, change);
                    }
                }

                @Override
                public void putOverride(PutOverrideChange change) {
                    for (ElementUpdater updater : new ArrayList<>(updaters)) {
                        updater.putOverride(scope, change);
                    }
                }
            });
        }

        public void addUpdater(ElementUpdater updater) {
            updaters.add(updater);
        }

        public void removeUpdater(ElementUpdater updater) {
            updaters.remove(updater);
        }
    }

    public interface ElementUpdater {

        void putNode(String scope, PutNodeChange change);

        void putOverride(String scope, PutOverrideChange change);

        void remove(String scope, RemoveChange change);

        void listRemove(String scope, ListRemoveChange change);

        void listInsert(String scope, ListInsertChange change);

        void listInsertNode(String scope, ListInsertNodeChange change);

        void put(String scope, PutChange change);
    }

    public class ForElementTemplate implements Template {
        private final BoundElementTemplate childTemplate;
        private final String modelKey;
        private final String innerScope;

        public ForElementTemplate(JsonObject templateDescription,
                int templateId) {
            modelKey = templateDescription.getString("modelKey");
            innerScope = templateDescription.getString("innerScope");

            childTemplate = new BoundElementTemplate(templateDescription,
                    templateId);
        }

        @Override
        public Node createElement(JsonObject node, ElementNotifier notifier) {
            // Creates anchor element
            Node commentNode = createCommentNode("for " + modelKey);
            notifier.addUpdater(new ForAnchorListener(commentNode, this));
            return commentNode;
        }

        public String getModelKey() {
            return modelKey;
        }

        public String getInnerScope() {
            return innerScope;
        }

        public Template getChildTemplate() {
            return childTemplate;
        }
    }

    private static native Node createCommentNode(String comment)
    /*-{
        return $doc.createComment(comment);
    }-*/;

    public class BoundElementTemplate implements Template {

        private final String tag;
        private final Map<String, String> defaultAttributeValues;
        private final Map<String, String> propertyToAttribute;
        private final Map<String, String> classPartBindings;
        private final Map<String, JsonArray> events;

        private final int[] childElementTemplates;

        private final int templateId;

        public BoundElementTemplate(JsonObject templateDescription,
                int templateId) {
            this.templateId = templateId;
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
                    JsonArray parametersJson = eventsJson.getArray(type);
                    events.put(type, parametersJson);
                }
            } else {
                events = null;
            }

        }

        private Map<String, String> readStringMap(JsonObject json) {
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
        public Node createElement(final JsonObject node,
                ElementNotifier notifier) {
            final Element element = Document.get().createElement(tag);
            initElement(node, element, notifier);

            notifier.addUpdater(new BoundElementListener(node, element, this));

            return element;
        }

        public int getTemplateId() {
            return templateId;
        }

        protected void initElement(JsonObject node, Element element,
                ElementNotifier notifier) {
            for (Entry<String, String> entry : defaultAttributeValues
                    .entrySet()) {
                element.setAttribute(entry.getKey(), entry.getValue());
            }

            if (events != null) {
                for (Entry<String, JsonArray> entry : events.entrySet()) {
                    String type = entry.getKey();
                    JsonArray eventDataKeys = entry.getValue();
                    addDomListener(element, type, new DomListener() {

                        @Override
                        public void handleEvent(JavaScriptObject event) {
                            JsonObject eventDetails = extractEventDetails(event,
                                    element, eventDataKeys);

                            sendTemplateEventToServer(
                                    nodeToId.get(node).intValue(), templateId,
                                    type, eventDetails);
                        }
                    });
                }
            }

            if (childElementTemplates != null) {
                for (int templateId : childElementTemplates) {
                    element.appendChild(templates.get(templateId)
                            .createElement(node, notifier));
                }
            }
        }

        public String getTargetAttribute(String property) {
            return propertyToAttribute.get(property);
        }

        public String getClassPartMapping(String property) {
            if (classPartBindings == null) {
                return null;
            } else {
                return classPartBindings.get(property);
            }
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
    public interface PutOverrideChange extends Change {
        @JsProperty
        int getKey();

        @JsProperty
        int getValue();
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

    public class ForAnchorListener implements ElementUpdater {
        private final Node anchorNode;
        private final ForElementTemplate template;

        public ForAnchorListener(Node anchorNode, ForElementTemplate template) {
            this.anchorNode = anchorNode;
            this.template = template;
        }

        @Override
        public void listInsertNode(String property,
                ListInsertNodeChange change) {
            if (!template.getModelKey().equals(property)) {
                return;
            }
            JsonObject childNode = idToNode.get(change.getValue());

            ElementNotifier notifier = new ElementNotifier(childNode,
                    template.getInnerScope() + ".");
            Node child = template.getChildTemplate().createElement(childNode,
                    notifier);

            Node insertionPoint = findNodeBefore(change.getIndex());

            insertionPoint.getParentElement().insertAfter(child,
                    insertionPoint);
        }

        private Node findNodeBefore(int index) {
            Node refChild = anchorNode;
            for (int i = 0; i < index; i++) {
                refChild = refChild.getNextSibling();
            }
            return refChild;
        }

        @Override
        public void putNode(String property, PutNodeChange change) {
            // Don't care
        }

        @Override
        public void put(String property, PutChange change) {
            // Don't care
        }

        @Override
        public void listInsert(String property, ListInsertChange change) {
            // Don't care
        }

        @Override
        public void listRemove(String property, ListRemoveChange change) {
            if (!template.getModelKey().equals(property)) {
                return;
            }

            Node node = findNodeBefore(change.getIndex()).getNextSibling();

            node.removeFromParent();
        }

        @Override
        public void remove(String property, RemoveChange change) {
            // Don't care
        }

        @Override
        public void putOverride(String property, PutOverrideChange change) {
            // Don't care
        }
    }

    public class BoundElementListener implements ElementUpdater {

        private final JsonObject node;
        private final Element element;
        private final BoundElementTemplate template;

        public BoundElementListener(JsonObject node, Element element,
                BoundElementTemplate template) {
            this.node = node;
            this.element = element;
            this.template = template;
        }

        @Override
        public void putNode(String property, PutNodeChange change) {
            // Don't care
        }

        @Override
        public void put(String property, PutChange change) {
            String targetAttribute = template.getTargetAttribute(property);
            if (targetAttribute != null) {
                setAttributeOrProperty(element, targetAttribute,
                        change.getValue());
                return;
            }

            String classPartMapping = template.getClassPartMapping(property);
            if (classPartMapping != null) {
                if (isTrueIsh(change.getValue())) {
                    element.addClassName(classPartMapping);
                } else {
                    element.removeClassName(classPartMapping);
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
            String targetAttribute = template.getTargetAttribute(property);
            if (targetAttribute != null) {
                setAttributeOrProperty(element, targetAttribute, null);
            }

            String classPartMapping = template.getClassPartMapping(property);
            if (classPartMapping != null) {
                element.removeClassName(classPartMapping);
            }
        }

        @Override
        public void putOverride(String property, PutOverrideChange change) {
            if (change.getKey() != template.getTemplateId()) {
                return;
            }

            int nodeId = change.getValue();
            JsonObject overrideNode = idToNode.get(nodeId);

            BasicElementListener basicElementListener = new BasicElementListener(
                    overrideNode, element);
            addNodeListener(overrideNode, basicElementListener);
        }

    }

    public class BasicElementListener implements NodeListener {

        private JsonObject node;
        private Element element;

        public BasicElementListener(JsonObject node, Element element) {
            this.node = node;
            this.element = element;
        }

        @Override
        public void putNode(PutNodeChange change) {
            if ("EVENT_DATA".equals(change.getKey())) {
                return;
            }
            throw new RuntimeException("Not supported");
        }

        @Override
        public void put(PutChange change) {
            JsonValue value = change.getValue();
            String key = change.getKey();
            if ("TAG".equals(key)) {
                return;
            }
            setAttributeOrProperty(element, key, value);
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
                    JsonObject eventData = null;

                    JsonObject eventTypesToData = node.getObject("EVENT_DATA");
                    if (eventTypesToData != null) {
                        JsonArray eventDataKeys = eventTypesToData
                                .getArray(type);
                        if (eventDataKeys != null
                                && eventDataKeys.getType() != JsonType.NULL) {
                            eventData = extractEventDetails(event, element,
                                    eventDataKeys);
                        }
                    }

                    if (eventData == null) {
                        eventData = Json.createObject();
                    }

                    sendEventToServer(id.intValue(), type, eventData);
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
            String key = change.getKey();
            if ("LISTENERS".equals(key)) {
                // This means we have no listeners left, remove the map as well
                Integer id = nodeToId.get(node);

                assert domListeners.containsKey(id);
                assert domListeners.get(id).isEmpty();

                domListeners.remove(id);
            } else {
                if ("TAG".equals(key)) {
                    return;
                }

                setAttributeOrProperty(element, key, null);
            }
        }

        @Override
        public void putOverride(PutOverrideChange change) {
            throw new RuntimeException("Not yet implemented");
        }
    }

    private static JsonObject extractEventDetails(JavaScriptObject event,
            Element element, JsonArray eventDataKeys) {
        JsonObject eventData = Json.createObject();
        for (int i = 0; i < eventDataKeys.length(); i++) {
            String eventDataKey = eventDataKeys.getString(i);
            if (eventDataKey.startsWith("event.")) {
                String jsKey = eventDataKey.substring("event.".length());
                JsonValue value = ((JsonObject) event).get(jsKey);
                eventData.put(eventDataKey, value);
            } else if (eventDataKey.startsWith("element.")) {
                String jsKey = eventDataKey.substring("element.".length());
                JsonValue value = ((JsonObject) element).get(jsKey);
                eventData.put(eventDataKey, value);
            } else {
                throw new RuntimeException(
                        "Unsupported event data key: " + eventDataKey);
            }
        }
        return eventData;
    }

    private void sendEventToServer(int nodeId, String eventType,
            JsonObject eventData) {
        JsonArray arguments = Json.createArray();
        arguments.set(0, nodeId);
        arguments.set(1, eventType);
        arguments.set(2, eventData);

        sendRpc("vEvent", arguments);
    }

    private void sendRpc(String callbackName, JsonArray arguments) {
        /*
         * Must invoke manually as the RPC interface can't be used in GWT
         * because of the JSONArray parameter
         */
        rpcQueue.add(new MethodInvocation(callbackName, arguments), false);
        rpcQueue.flush();
    }

    protected void sendTemplateEventToServer(int nodeId, int templateId,
            String type, JsonObject eventData) {
        JsonArray arguments = Json.createArray();
        arguments.set(0, nodeId);
        arguments.set(1, templateId);
        arguments.set(2, type);
        arguments.set(3, eventData);

        sendRpc("vTemplateEvent", arguments);
    }

    private static void setAttributeOrProperty(Element element, String key,
            JsonValue value) {
        if (isAlwaysAttribute(key)) {
            if (value != null) {
                element.setAttribute(key, value.asString());
                debug("Set attribute " + key + "=" + value + " for "
                        + outerHtml(element));
            } else {
                element.removeAttribute(key);
                debug("Removed attribute " + key + " from "
                        + outerHtml(element));
            }
        } else {
            if (value.getType() == JsonType.BOOLEAN) {
                element.setPropertyBoolean(key, value.asBoolean());
                debug("Set property " + key + "=" + value + " (boolean) for "
                        + outerHtml(element));
            } else if (value.getType() == JsonType.NUMBER) {
                element.setPropertyDouble(key, value.asNumber());
                debug("Set property " + key + "=" + value + " (number) for "
                        + outerHtml(element));
            } else {
                element.setPropertyString(key, value.asString());
                debug("Set property " + key + "=" + value + " (string) for "
                        + outerHtml(element));
            }

        }

    }

    private static void debug(String string) {
        if (false) {
            getLogger().info(string);
        }
    }

    private static String outerHtml(Element element) {
        return element.getPropertyString("outerHTML");
    }

    private static String outerHtml(Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            return outerHtml((Element) node);
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

        @Override
        public void putOverride(PutOverrideChange change) {
            throw new RuntimeException("Not yet implemented");
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

        @Override
        public void putOverride(PutOverrideChange change) {
            throw new RuntimeException("Not supported");
        }
    }

    public interface NodeListener {

        void putNode(PutNodeChange change);

        void put(PutChange change);

        void listInsertNode(ListInsertNodeChange change);

        void listInsert(ListInsertChange change);

        void listRemove(ListRemoveChange change);

        void remove(RemoveChange change);

        void putOverride(PutOverrideChange change);
    }

    private interface Template {
        public Node createElement(JsonObject node, ElementNotifier notifier);
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

    // Node id -> template id -> override node id
    // XXX seems like this is never actually read, could maybe be removed?
    private Map<Integer, Map<Integer, Integer>> overrides = new HashMap<>();

    private Map<Integer, List<NodeListener>> listeners = new HashMap<>();

    private Map<Integer, Map<String, JavaScriptObject>> domListeners = new HashMap<>();

    private boolean rootInitialized = false;

    private Map<Integer, Node> nodeIdToBasicElement = new HashMap<>();

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
            int templateId = (int) node.getNumber("TEMPLATE");
            Template template = templates.get(Integer.valueOf(templateId));
            assert template != null;

            return template.createElement(node, new ElementNotifier(node, ""));
        } else {
            String tag = node.getString("TAG");
            if ("#text".equals(tag)) {
                Text textNode = Document.get().createTextNode("");
                addNodeListener(node, new TextElementListener(node, textNode));
                nodeIdToBasicElement.put(nodeToId.get(node), textNode);
                debug("Created text node");
                return textNode;
            } else {
                Element element = Document.get().createElement(tag);
                addNodeListener(node, new BasicElementListener(node, element));
                nodeIdToBasicElement.put(nodeToId.get(node), element);
                debug("Created element: " + outerHtml(element));
                return element;
            }
        }
    }

    private static void insertNodeAtIndex(Element parent, Node child,
            int index) {
        if (parent.getChildCount() == index) {
            parent.appendChild(child);
            debug("Appended node " + outerHtml(child) + " into "
                    + outerHtml(parent));
        } else {
            Node reference = parent.getChildNodes().getItem(index);
            parent.insertBefore(child, reference);
            debug("Inserted node " + outerHtml(child) + " into "
                    + outerHtml(parent) + " at index " + index);
        }
    }

    public void update(JsonObject elementTemplates, JsonArray elementChanges,
            JsonArray rpc) {
        extractTemplates(elementTemplates);

        updateTree(elementChanges);

        logTree("After changes", idToNode.get(Integer.valueOf(1)));

        if (!rootInitialized) {
            initRoot();
            rootInitialized = true;
        }

        notifyListeners(elementChanges);

        if (rpc != null) {
            runRpc(rpc);
        }
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

            newFunctionParams.push(script);
            createAndRunFunction(newFunctionParams, params);
        }
    }

    private static native void createAndRunFunction(
            JsArrayString newFunctionParams, JsArray<JavaScriptObject> params)
            /*-{
                Function.apply(Function, newFunctionParams).apply(null, params);
            }-*/;

    private Node findDomNode(int nodeId, int templateId) {
        if (templateId == 0) {
            return nodeIdToBasicElement.get(Integer.valueOf(nodeId));
        } else {
            throw new RuntimeException(
                    "Not yet implemented for template elements");
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
        case "putOverride":
            nodeListener.putOverride((PutOverrideChange) change);
            break;
        default:
            throw new RuntimeException(
                    "Unsupported change type: " + change.getType());
        }
    }

    private void initRoot() {
        JsonObject rootNode = idToNode.get(Integer.valueOf(1));
        JsonObject bodyNode = rootNode.get("body");
        addNodeListener(bodyNode,
                new BasicElementListener(bodyNode, rootElement));
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
            return new BoundElementTemplate(templateDescription, templateId);
        case "ForElementTemplate":
            return new ForElementTemplate(templateDescription, templateId);
        case "DynamicTextTemplate":
            return new DynamicTextTemplate(templateDescription, templateId);
        case "StaticTextTemplate":
            return new StaticTextTemplate(templateDescription, templateId);
        default:
            throw new RuntimeException("Unsupported template type: " + type);
        }
    }
}
