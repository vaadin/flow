package com.vaadin.client.communication.tree;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.Profiler;
import com.vaadin.client.Util;
import com.vaadin.client.communication.DomApi;
import com.vaadin.client.communication.tree.TreeNode.TreeNodeChangeListener;

import elemental.js.json.JsJsonArray;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class BasicElementListener {

    private static final class PropertyPropagator
            implements TreeNodeChangeListener {
        private JsonObject target;
        private boolean alwaysSetProperty;

        public PropertyPropagator(JsonObject target) {
            assert target != null;
            this.target = target;
            alwaysSetProperty = true;
        }

        public PropertyPropagator(Element target) {
            this.target = (JsonObject) target;
            alwaysSetProperty = false;
        }

        @Override
        public void addProperty(String name, TreeNodeProperty property) {
            // Ignore metadata, e.g. TAG
            if (isMetadata(name)) {
                return;
            }
            Profiler.enter("PropertyPropagator.addProperty");

            property.addPropertyChangeListener((old, value) -> {
                Profiler.enter("PropertyPropagator.changeValue");

                if (value instanceof ListTreeNode) {
                    ListTreeNode child = (ListTreeNode) value;

                    JsonValue oldValue = target.get(name);
                    if (oldValue == null
                            || oldValue.getType() != JsonType.ARRAY) {
                        target.put(name, Json.createArray());
                        TreeUpdater.debug("Set property " + name
                                + " to empty array" + " for " + target);
                    }
                    JsArrayObject<Object> targetArray = ((JsJsonArray) target
                            .getArray(name)).cast();

                    child.addArrayEventListener(
                            (listTreeNode, startIndex, removed, added) -> {
                        Profiler.enter("PropertyPropagator.splice");
                        JsArrayObject<Object> newValues = added;
                        if (added != null && added.size() != 0) {
                            Object firstAdded = added.get(0);
                            if (firstAdded instanceof TreeNode) {
                                newValues = JavaScriptObject.createArray()
                                        .cast();
                                for (int i1 = 0; i1 < added.size(); i1++) {
                                    newValues.add(
                                            JavaScriptObject.createObject());
                                }
                            }
                        }
                        ListTreeNode.doSplice(targetArray, startIndex,
                                removed.size(), newValues);
                        if (newValues != added) {
                            for (int i2 = 0; i2 < added.size(); i2++) {
                                JsonValue childObject = TreeUpdater.asJsonValue(
                                        targetArray.get(startIndex + i2));
                                TreeNode childNode = (TreeNode) added.get(i2);
                                childNode.addTreeNodeChangeListener(
                                        new PropertyPropagator(
                                                (JsonObject) childObject));
                                TreeUpdater.debug("Add listener for child "
                                        + (startIndex + i2) + " in property "
                                        + name + " for element " + target);

                            }
                        }
                        Profiler.leave("PropertyPropagator.splice");
                    });

                } else if (value instanceof TreeNode) {
                    TreeNode child = (TreeNode) value;
                    JsonValue childValue = target.get(name);
                    if (childValue == null
                            || childValue.getType() != JsonType.OBJECT) {
                        target.put(name, Json.createObject());
                        // Read back again in case the setter did some magic
                        childValue = target.get(name);
                    }
                    TreeUpdater.debug("Set property " + name + " to " + value
                            + " for element " + target);
                    child.addTreeNodeChangeListener(
                            new PropertyPropagator((JsonObject) childValue));
                } else if (alwaysSetProperty) {
                    TreeUpdater.debug("Set property " + name + " to " + value
                            + " for element " + target);
                    target.put(name, TreeUpdater.asJsonValue(value));
                } else {
                    TreeUpdater.setAttributeOrProperty(
                            Element.as((JavaScriptObject) target), name, value);
                }

                Profiler.leave("PropertyPropagator.changeValue");
            });

            Profiler.leave("PropertyPropagator.addProperty");
        }

    }

    private static void insertNodeAtIndex(Element parent, Node child,
            int index) {
        Profiler.enter("TreeUpdater.insertNodeAtIndex");

        if (DomApi.wrap(parent).getChildNodes().getLength() == index) {
            DomApi.wrap(parent).appendChild(child);
            if (TreeUpdater.debug) {
                TreeUpdater
                        .debug("Appended node " + TreeUpdater.debugHtml(child)
                                + " into " + TreeUpdater.debugHtml(parent));
            }
        } else {
            Node reference = DomApi.wrap(parent).getChildNodes().getItem(index);
            DomApi.wrap(parent).insertBefore(child, reference);
            if (TreeUpdater.debug) {
                TreeUpdater.debug("Inserted node "
                        + TreeUpdater.debugHtml(child) + " into "
                        + TreeUpdater.debugHtml(parent) + " at index " + index);
            }
        }

        Profiler.leave("TreeUpdater.insertNodeAtIndex");
    }

    private static boolean isMetadata(String name) {
        return name.toUpperCase().equals(name); // Metadata is in ALLUPPERCASE
    }

    private static void addListener(String type, TreeUpdater treeUpdater,
            TreeNode elementNode, Element element) {
        TreeUpdater.debug("Add listener for " + type + " node "
                + TreeUpdater.debugHtml(element));
        DomListener listener = event -> {
            JsonObject eventData = null;

            TreeUpdater.debug("Handling " + type + " for "
                    + TreeUpdater.debugHtml(element) + ". Event: "
                    + TreeUpdater.debugEvent(event));

            // send any attribute updates first to server
            TreeNode eventUpdatedAttributeTypes = (TreeNode) elementNode
                    .getProperty("EVENT_ATTRIBUTE").getValue();
            if (eventUpdatedAttributeTypes != null) {
                ListTreeNode eventUpdatedAttributes = (ListTreeNode) eventUpdatedAttributeTypes
                        .getProperty(type).getValue();
                if (eventUpdatedAttributes != null) {
                    sendEventAttributeUpdatesToServer(elementNode, element,
                            eventUpdatedAttributes, treeUpdater);
                }
            }

            TreeNode eventTypesToData = (TreeNode) elementNode
                    .getProperty("EVENT_DATA").getValue();
            if (eventTypesToData != null) {
                ListTreeNode eventDataKeys = (ListTreeNode) eventTypesToData
                        .getProperty(type).getValue();
                if (eventDataKeys != null) {
                    eventData = extractEventDetails(event, element,
                            eventDataKeys);
                }
            }

            if (eventData == null) {
                eventData = Json.createObject();
            }

            sendEventToServer(elementNode.getId(), type, eventData,
                    treeUpdater);
        };

        JavaScriptObject wrappedListener = TreeUpdater.addDomListener(element,
                type, listener);

        treeUpdater.saveDomListener(Integer.valueOf(elementNode.getId()), type,
                wrappedListener);
    }

    private static void sendEventToServer(int nodeId, String eventType,
            JsonObject eventData, TreeUpdater treeUpdater) {
        TreeUpdater.debug("Sending event " + eventType + " for node " + nodeId
                + " to server, eventData: ["
                + TreeUpdater.debugObject(eventData) + "]");
        JsonArray arguments = Json.createArray();
        arguments.set(0, nodeId);
        arguments.set(1, eventType);
        arguments.set(2, eventData);

        treeUpdater.sendRpc("vEvent", arguments);
    }

    private static void sendEventAttributeUpdatesToServer(TreeNode elementNode,
            Element element, ListTreeNode eventUpdatedAttributes,
            TreeUpdater treeUpdater) {

        JsonArray arguments = Json.createArray();
        JsonArray updates = Json.createArray();
        updates.set(0, elementNode.getId());

        int arrayIndex = 1;
        for (int i = 0; i < eventUpdatedAttributes.getLength(); i++) {
            String attribute = (String) eventUpdatedAttributes.get(i);

            TreeNodeProperty property = elementNode.getProperty(attribute);
            Object oldValue = property.getProxyValue();
            JsonValue oldJsonValue = TreeUpdater.asJsonValue(oldValue);

            JsonValue newJsonValue = null;
            Object value = null;
            if (attribute.startsWith("attr.")
                    || TreeUpdater.isAlwaysAttribute(attribute)) {
                value = element.getAttribute(attribute.substring(5));
                if (value != null) {
                    newJsonValue = Json.create((String) value);
                } else {
                    newJsonValue = Json.createNull();
                }
            } else if (attribute.equals("classList")) {
                value = element.getAttribute("class");
                if (value != null) {
                    newJsonValue = Json.create((String) value);
                } else {
                    newJsonValue = Json.createNull();
                }
            } else { // property
                JavaScriptObject propertyJSO = element
                        .getPropertyJSO(attribute);
                if (propertyJSO != null) {
                    newJsonValue = Util.jso2json(propertyJSO);
                    // need to copy the value so that the old one is still kept
                    // in
                    // state node and not updated by the element
                    switch (newJsonValue.getType()) {
                    case NUMBER:
                        value = new Double(
                                element.getPropertyDouble(attribute));
                        break;
                    case STRING:
                        value = new String(
                                element.getPropertyString(attribute));
                        break;
                    case BOOLEAN:
                        value = new Boolean(
                                element.getPropertyBoolean(attribute));
                        break;
                    case OBJECT:
                        value = Json.parse(newJsonValue.toJson()).toNative();
                        break;
                    case ARRAY:
                        value = ((JsArrayObject<?>) propertyJSO).cloneArray();
                        break;
                    default:
                        break;
                    }
                }
            }

            TreeUpdater.debug("Checking for Element "
                    + TreeUpdater.debugHtml(element) + " attribute/property "
                    + attribute + " updates, previously: " + oldJsonValue
                    + ", now: " + newJsonValue);
            // send value to server and update node if changed
            if (oldJsonValue != null && !oldJsonValue.jsEquals(newJsonValue)
                    || newJsonValue != null
                            && !newJsonValue.jsEquals(oldJsonValue)) {
                updates.set(arrayIndex++, attribute);
                updates.set(arrayIndex++, newJsonValue);
                // do not fire update events
                property.doSetValue(value, false);
            }
        }

        if (updates.length() > 1) {
            arguments.set(0, updates);
            TreeUpdater
                    .debug("Sending updated attributes to server for event in node "
                            + elementNode.getId() + " with attributes: "
                            + updates.toJson());
            treeUpdater.sendRpc("vAttributeUpdate", arguments);
        }
    }

    private static JsonObject extractEventDetails(JavaScriptObject event,
            Element element, ListTreeNode eventDataKeys) {
        JsonObject eventData = Json.createObject();
        for (int i = 0; i < eventDataKeys.getLength(); i++) {
            String eventDataKey = (String) eventDataKeys.get(i);
            JsonValue value;
            if (eventDataKey.matches("^[a-zA-Z0-9]*$")) {
                String jsKey = eventDataKey;

                // Try event first, then element
                if (((JsonObject) event).hasKey(jsKey)) {
                    value = ((JsonObject) event).get(jsKey);
                } else if (((JsonObject) element).hasKey(jsKey)) {
                    value = ((JsonObject) element).get(jsKey);
                } else {
                    value = null;
                    TreeUpdater.debug(
                            "No value found for event key " + eventDataKey);
                }
            } else {
                Map<String, JavaScriptObject> context = new HashMap<>();
                context.put("element", element);
                context.put("event", event);
                value = TreeUpdater
                        .evalWithContext(context, "return " + eventDataKey)
                        .cast();
            }

            eventData.put(eventDataKey, value);
        }
        return eventData;
    }

    private static JsonValue getValue(JsonObject dataObject, String jsKey) {
        String keys[] = jsKey.split("\\.");
        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];
            dataObject = dataObject.getObject(key);
            if (dataObject == null) {
                return null;
            }
        }
        // Ensure we return 0 instead of null, e.g. for scrollLeft
        return dataObject.getObject(keys[keys.length - 1]);
    }

    public static void bind(TreeNode node, Element element,
            TreeUpdater treeUpdater) {
        node.addTreeNodeChangeListener(new PropertyPropagator(element));

        node.addTreeNodeChangeListener((name, property) -> {
            if (!name.equals("CHILDREN")) {
                return;
            }

            Profiler.enter("BasicElementListener CHILDREN addProperty");
            ListTreeNode childrenListNode = (ListTreeNode) node
                    .getProperty("CHILDREN").getValue();
            addChildrenListener(node, childrenListNode, element, treeUpdater);
            Profiler.leave("BasicElementListener CHILDREN addProperty");
        });

        node.addTreeNodeChangeListener((name, property) -> {
            if (!name.equals("LISTENERS")) {
                return;
            }

            Profiler.enter("BasicElementListener LISTENERS addProperty");
            ListTreeNode listenersListNode = (ListTreeNode) node
                    .getProperty("LISTENERS").getValue();
            addListenersListener(node, listenersListNode, element, treeUpdater);
            Profiler.leave("BasicElementListener LISTENERS addProperty");
        });
        node.addTreeNodeChangeListener((name, property) -> {
            if (!name.equals("CLASS_LIST")) {
                return;
            }

            Profiler.enter("BasicElementListener CLASS_LIST addProperty");
            ListTreeNode classListListNode = (ListTreeNode) node
                    .getProperty("CLASS_LIST").getValue();
            classListListNode.addArrayEventListener(
                    (listTreeNode, startIndex, removed, added) -> {
                ClassListUpdater.splice(element, listTreeNode, startIndex,
                        removed, added);
            });
            Profiler.leave("BasicElementListener CLASS_LIST addProperty");
        });

    }

    private static void addChildrenListener(TreeNode elementNode,
            ListTreeNode childrenListNode, Element element,
            TreeUpdater treeUpdater) {
        childrenListNode.addArrayEventListener(
                (listTreeNode, startIndex, removed, added) -> {
                    Profiler.enter("BasicElementListener CHILDREN splice");

                    Profiler.enter(
                            "BasicElementListener CHILDREN splice remove");
                    for (int i1 = 0; i1 < removed.size(); i1++) {
                        TreeNode removedNode = (TreeNode) removed.get(i1);
                        Node removedElement = treeUpdater
                                .getOrCreateElement(removedNode);
                        if (DomApi.wrap(removedElement)
                                .getParentNode() == element) {
                            DomApi.wrap(element).removeChild(removedElement);
                        }
                    }
                    Profiler.leave(
                            "BasicElementListener CHILDREN splice remove");

                    Profiler.enter(
                            "BasicElementListener CHILDREN splice insert");
                    for (int i2 = 0; i2 < added.size(); i2++) {
                        TreeNode addedNode = (TreeNode) added.get(i2);
                        Node node = treeUpdater.getOrCreateElement(addedNode);
                        insertNodeAtIndex(element, node, startIndex + i2);
                    }
                    Profiler.leave(
                            "BasicElementListener CHILDREN splice insert");

                    Profiler.leave("BasicElementListener CHILDREN splice");
                });
    }

    private static void addListenersListener(TreeNode elementNode,
            ListTreeNode listenersListNode, Element element,
            TreeUpdater treeUpdater) {
        listenersListNode.addArrayEventListener(
                (listTreeNode, startIndex, removed, added) -> {
                    Profiler.enter("BasicElementListener LISTENERS splice");
                    for (int i1 = 0; i1 < removed.size(); i1++) {
                        String type1 = (String) removed.get(i1);
                        Integer id = Integer.valueOf(elementNode.getId());

                        JavaScriptObject listener = treeUpdater
                                .removeSavedDomListener(type1, id);
                        assert listener != null;

                        TreeUpdater.removeDomListener(element, type1, listener);
                    }

                    for (int i2 = 0; i2 < added.size(); i2++) {
                        String type2 = (String) added.get(i2);
                        addListener(type2, treeUpdater, elementNode, element);
                    }
                    Profiler.leave("BasicElementListener LISTENERS splice");
                });
    }
}