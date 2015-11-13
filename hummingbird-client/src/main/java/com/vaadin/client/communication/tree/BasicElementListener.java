package com.vaadin.client.communication.tree;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.communication.DomApi;
import com.vaadin.client.communication.tree.ListTreeNode.ArrayEventListener;
import com.vaadin.client.communication.tree.TreeNode.TreeNodeChangeListener;

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

            property.addPropertyChangeListener((old, value) -> {
                if (value instanceof TreeNode) {
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
            });
        }

    }

    private static void insertNodeAtIndex(Element parent, Node child,
            int index) {
        if (DomApi.wrap(parent).getChildNodes().size() == index) {
            DomApi.wrap(parent).appendChild(child);
            if (TreeUpdater.debug) {
                TreeUpdater
                        .debug("Appended node " + TreeUpdater.debugHtml(child)
                                + " into " + TreeUpdater.debugHtml(parent));
            }
        } else {
            Node reference = DomApi.wrap(parent).getChildNodes().get(index);
            DomApi.wrap(parent).insertBefore(child, reference);
            if (TreeUpdater.debug) {
                TreeUpdater.debug("Inserted node "
                        + TreeUpdater.debugHtml(child) + " into "
                        + TreeUpdater.debugHtml(parent) + " at index " + index);
            }
        }
    }

    private static boolean isMetadata(String name) {
        return name.toUpperCase().equals(name); // Metadata is in ALLUPPERCASE
    }

    private static void addListener(String type, TreeUpdater treeUpdater,
            TreeNode elementNode, Element element) {
        TreeUpdater.debug("Add listener for " + type + " node "
                + TreeUpdater.debugHtml(element));
        DomListener listener = new DomListener() {
            @Override
            public void handleEvent(JavaScriptObject event) {
                JsonObject eventData = null;

                TreeUpdater.debug("Handling " + type + " for "
                        + TreeUpdater.debugHtml(element) + ". Event: "
                        + ((JsonObject) event.cast()).toJson());
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
            }
        };

        JavaScriptObject wrappedListener = TreeUpdater.addDomListener(element,
                type, listener);

        treeUpdater.saveDomListener(Integer.valueOf(elementNode.getId()), type,
                wrappedListener);
    }

    private static void sendEventToServer(int nodeId, String eventType,
            JsonObject eventData, TreeUpdater treeUpdater) {
        TreeUpdater.debug("Sending event " + eventType + " for node " + nodeId
                + " to server (data: " + eventData.toJson() + ")");
        JsonArray arguments = Json.createArray();
        arguments.set(0, nodeId);
        arguments.set(1, eventType);
        arguments.set(2, eventData);

        treeUpdater.sendRpc("vEvent", arguments);
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

        node.addTreeNodeChangeListener(new TreeNodeChangeListener() {

            @Override
            public void addProperty(String name, TreeNodeProperty property) {
                if (!name.equals("CHILDREN")) {
                    return;
                }

                ListTreeNode childrenListNode = (ListTreeNode) node
                        .getProperty("CHILDREN").getValue();
                addChildrenListener(node, childrenListNode, element,
                        treeUpdater);
            }
        });

        node.addTreeNodeChangeListener(new TreeNodeChangeListener() {
            @Override
            public void addProperty(String name, TreeNodeProperty property) {
                if (!name.equals("LISTENERS")) {
                    return;
                }

                ListTreeNode listenersListNode = (ListTreeNode) node
                        .getProperty("LISTENERS").getValue();
                addListenersListener(node, listenersListNode, element,
                        treeUpdater);
            }
        });
    }

    private static void addChildrenListener(TreeNode elementNode,
            ListTreeNode childrenListNode, Element element,
            TreeUpdater treeUpdater) {
        childrenListNode.addArrayEventListener(new ArrayEventListener() {
            @Override
            public void splice(ListTreeNode listTreeNode, int startIndex,
                    JsArrayObject<Object> removed,
                    JsArrayObject<Object> added) {
                for (int i = 0; i < removed.size(); i++) {
                    TreeNode removedNode = (TreeNode) removed.get(i);
                    Node removedElement = treeUpdater
                            .getOrCreateElement(removedNode);
                    if (DomApi.wrap(removedElement)
                            .getParentNode() == element) {
                        DomApi.wrap(element).removeChild(removedElement);
                    }
                }
                for (int i = 0; i < added.size(); i++) {
                    TreeNode addedNode = (TreeNode) added.get(i);
                    Node node = treeUpdater.getOrCreateElement(addedNode);
                    insertNodeAtIndex(element, node, startIndex + i);
                }
            }
        });
    }

    private static void addListenersListener(TreeNode elementNode,
            ListTreeNode listenersListNode, Element element,
            TreeUpdater treeUpdater) {
        listenersListNode.addArrayEventListener(new ArrayEventListener() {
            @Override
            public void splice(ListTreeNode listTreeNode, int startIndex,
                    JsArrayObject<Object> removed,
                    JsArrayObject<Object> added) {
                for (int i = 0; i < removed.size(); i++) {
                    String type = (String) removed.get(i);
                    Integer id = Integer.valueOf(elementNode.getId());

                    JavaScriptObject listener = treeUpdater
                            .removeSavedDomListener(type, id);
                    assert listener != null;

                    TreeUpdater.removeDomListener(element, type, listener);
                }

                for (int i = 0; i < added.size(); i++) {
                    String type = (String) added.get(i);
                    addListener(type, treeUpdater, elementNode, element);
                }
            }
        });
    }
}