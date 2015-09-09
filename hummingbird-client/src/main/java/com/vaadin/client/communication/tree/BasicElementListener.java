package com.vaadin.client.communication.tree;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.communication.DomApi;
import com.vaadin.client.communication.tree.EventArray.ArrayEventListener;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class BasicElementListener {

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

    private static void addListener(String type, TreeUpdater treeUpdater,
            TreeNode node, Element element) {
        TreeUpdater.debug("Add listener for " + type + " node "
                + TreeUpdater.debugHtml(element));
        DomListener listener = new DomListener() {
            @Override
            public void handleEvent(JavaScriptObject event) {
                JsonObject eventData = null;

                TreeUpdater.debug("Handling " + type + " for "
                        + TreeUpdater.debugHtml(element) + ". Event: "
                        + ((JsonObject) event.cast()).toJson());
                TreeNode eventTypesToData = (TreeNode) node
                        .getProperty("EVENT_DATA").getValue();
                if (eventTypesToData != null) {
                    EventArray eventDataKeys = eventTypesToData
                            .getArrayProperty(type);
                    if (eventDataKeys != null) {
                        eventData = extractEventDetails(event, element,
                                eventDataKeys);
                    }
                }

                if (eventData == null) {
                    eventData = Json.createObject();
                }

                sendEventToServer(node.getId(), type, eventData, treeUpdater);
            }
        };

        JavaScriptObject wrappedListener = TreeUpdater.addDomListener(element,
                type, listener);

        treeUpdater.saveDomListener(Integer.valueOf(node.getId()), type,
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
            Element element, EventArray eventDataKeys) {
        JsonObject eventData = Json.createObject();
        for (int i = 0; i < eventDataKeys.getLength(); i++) {
            String eventDataKey = (String) eventDataKeys.get(i);
            JsonValue value;
            if (eventDataKey.startsWith("event.")) {
                String jsKey = eventDataKey.substring("event.".length());
                value = getValue((JsonObject) event, jsKey);
            } else if (eventDataKey.startsWith("element.")) {
                String jsKey = eventDataKey.substring("element.".length());
                value = getValue((JsonObject) element, jsKey);
            } else {
                String jsKey = eventDataKey;

                // Try event first, then element
                value = getValue((JsonObject) event, jsKey);
                if (value == null) {
                    value = getValue((JsonObject) element, jsKey);
                }
            }
            // FIXME This logs errors for "0"
            if (value == null) {
                TreeUpdater
                        .debug("No value found for event key " + eventDataKey);
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
        node.addTreeNodeChangeListener((name, property) -> {
            // Ignore metadata, e.g. TAG
            if (name.toLowerCase().equals(name)) {
                property.addPropertyChangeListener((p, old) -> {
                    TreeUpdater.setAttributeOrProperty(element, name,
                            p.getValue());
                });
            }
        });

        EventArray children = node.getArrayProperty("CHILDREN");
        children.addArrayEventListener(new ArrayEventListener() {
            @Override
            public void splice(EventArray eventArray, int startIndex,
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

        EventArray listeners = node.getArrayProperty("LISTENERS");
        listeners.addArrayEventListener(new ArrayEventListener() {
            @Override
            public void splice(EventArray eventArray, int startIndex,
                    JsArrayObject<Object> removed,
                    JsArrayObject<Object> added) {
                for (int i = 0; i < removed.size(); i++) {
                    String type = (String) removed.get(i);
                    Integer id = Integer.valueOf(node.getId());

                    JavaScriptObject listener = treeUpdater
                            .removeSavedDomListener(type, id);
                    assert listener != null;

                    TreeUpdater.removeDomListener(element, type, listener);
                }

                for (int i = 0; i < added.size(); i++) {
                    String type = (String) added.get(i);
                    addListener(type, treeUpdater, node, element);
                }
            }
        });
    }
}