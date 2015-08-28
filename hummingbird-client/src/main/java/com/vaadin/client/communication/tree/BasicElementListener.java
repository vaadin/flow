package com.vaadin.client.communication.tree;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.vaadin.client.communication.Polymer;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class BasicElementListener implements NodeListener {

    private final TreeUpdater treeUpdater;
    private final JsonObject node;
    private final Element element;

    public BasicElementListener(TreeUpdater treeUpdater, JsonObject node,
            Element element) {
        this.treeUpdater = treeUpdater;
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
        TreeUpdater.setAttributeOrProperty(element, key, value);
    }

    @Override
    public void listInsertNode(ListInsertNodeChange change) {
        if ("CHILDREN".equals(change.getKey())) {
            insertChild(change.getIndex(),
                    treeUpdater.getNode(change.getValue()));
        } else {
            throw new RuntimeException("Not supported: " + change.getKey());
        }
    }

    private void insertChild(int index, JsonObject node) {
        Node child = treeUpdater.getOrCreateElement(node);
        insertNodeAtIndex(element, child, index);
    }

    private static void insertNodeAtIndex(Element parent, Node child,
            int index) {
        if (Polymer.dom(parent).getChildNodes().size() == index) {
            Polymer.dom(parent).appendChild(child);
            if (TreeUpdater.debug) {
                TreeUpdater
                        .debug("Appended node " + TreeUpdater.debugHtml(child)
                                + " into " + TreeUpdater.debugHtml(parent));
            }
        } else {
            Node reference = Polymer.dom(parent).getChildNodes().get(index);
            Polymer.dom(parent).insertBefore(child, reference);
            if (TreeUpdater.debug) {
                TreeUpdater.debug("Inserted node "
                        + TreeUpdater.debugHtml(child) + " into "
                        + TreeUpdater.debugHtml(parent) + " at index " + index);
            }
        }
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
            removeListener(change.getRemovedValue().asString());
            break;
        case "CHILDREN":
            JsonObject removedNode = (JsonObject) change.getRemovedValue();
            Node removedElement = treeUpdater.getOrCreateElement(removedNode);
            if (Polymer.dom(element).getParentNode() == element) {
                Polymer.dom(element).removeChild(removedElement);
            }
            break;
        default:
            throw new RuntimeException("Not supported: " + change.getKey());
        }
    }

    private void addListener(String type) {
        Integer id = treeUpdater.getNodeId(node);
        TreeUpdater.debug("Add listener for " + type + " node "
                + TreeUpdater.debugHtml(element));
        DomListener listener = new DomListener() {
            @Override
            public void handleEvent(JavaScriptObject event) {
                JsonObject eventData = null;

                TreeUpdater.debug("Handling " + type + " for "
                        + TreeUpdater.debugHtml(element) + ". Event: "
                        + ((JsonObject) event.cast()).toJson());
                JsonObject eventTypesToData = node.getObject("EVENT_DATA");
                if (eventTypesToData != null) {
                    JsonArray eventDataKeys = eventTypesToData.getArray(type);
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

        JavaScriptObject wrappedListener = TreeUpdater.addDomListener(element,
                type, listener);

        treeUpdater.saveDomListener(id, type, wrappedListener);
    }

    void sendEventToServer(int nodeId, String eventType, JsonObject eventData) {
        TreeUpdater.debug("Sending event " + eventType + " for node " + nodeId
                + " to server (data: " + eventData.toJson() + ")");
        JsonArray arguments = Json.createArray();
        arguments.set(0, nodeId);
        arguments.set(1, eventType);
        arguments.set(2, eventData);

        treeUpdater.sendRpc("vEvent", arguments);
    }

    private static JsonObject extractEventDetails(JavaScriptObject event,
            Element element, JsonArray eventDataKeys) {
        JsonObject eventData = Json.createObject();
        for (int i = 0; i < eventDataKeys.length(); i++) {
            String eventDataKey = eventDataKeys.getString(i);
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

    private void removeListener(String type) {
        Integer id = treeUpdater.getNodeId(node);

        JavaScriptObject listener = treeUpdater.removeSavedDomListener(type,
                id);
        assert listener != null;

        TreeUpdater.removeDomListener(element, type, listener);
    }

    @Override
    public void remove(RemoveChange change) {
        String key = change.getKey();
        if ("LISTENERS".equals(key)) {
            // This means we have no listeners left, remove the map as well
            Integer id = treeUpdater.getNodeId(node);

            treeUpdater.removeSavedDomListeners(id);
        } else if (!"TAG".equals(key)) {
            TreeUpdater.setAttributeOrProperty(element, key, null);
        }
    }

    @Override
    public void putOverride(PutOverrideChange change) {
        throw new RuntimeException("Not yet implemented");
    }
}