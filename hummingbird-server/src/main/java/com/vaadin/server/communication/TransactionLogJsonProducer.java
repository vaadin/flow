package com.vaadin.server.communication;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.JsonConverter;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListInsertManyChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;
import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.hummingbird.kernel.change.NodeContentsChange;
import com.vaadin.hummingbird.kernel.change.NodeDataChange;
import com.vaadin.hummingbird.kernel.change.NodeListChange;
import com.vaadin.hummingbird.kernel.change.PutChange;
import com.vaadin.hummingbird.kernel.change.RangeEndChange;
import com.vaadin.hummingbird.kernel.change.RangeStartChange;
import com.vaadin.hummingbird.kernel.change.RemoveChange;
import com.vaadin.ui.UI;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class TransactionLogJsonProducer {

    private static final String ID = "id";
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String TYPE_PUT = "put";
    private static final String TYPE_PUT_NODE = "putNode";
    private static final String LIST_INDEX = "index";
    private static final String TYPE_REMOVE = "remove";
    private static final String TYPE_PUT_OVERRIDE = "putOverride";
    private static final String TYPE_LIST_INSERT_NODE = "listInsertNode";
    private static final String TYPE_LIST_INSERT_NODES = "listInsertNodes";
    private static final String TYPE_LIST_INSERT = "listInsert";
    private static final String TYPE_LIST_INSERTS = "listInserts";
    private static final String TYPE_LIST_REPLACE = "listReplace";
    private static final String TYPE_LIST_REPLACE_NODE = "listReplaceNode";
    private static final String TYPE_LIST_REMOVE = "listRemove";
    private static final String TYPE_RANGE_START = "rangeStart";
    private static final String TYPE_RANGE_END = "rangeEnd";

    private JsonArray changesJson = Json.createArray();
    private JsonObject templatesJson = Json.createObject();
    private UI ui;

    public TransactionLogJsonProducer(UI ui,
            LinkedHashMap<StateNode, List<NodeChange>> changes,
            Set<ElementTemplate> set) {
        this.ui = ui;
        changesToJson(changes);
        templatesToJson(set);
    }

    public JsonArray getChangesJson() {
        return changesJson;
    }

    public JsonObject getTemplatesJson() {
        return templatesJson;
    }

    private void templatesToJson(Set<ElementTemplate> templates) {
        templatesJson = Json.createObject();

        TemplateSerializer templateSerializer = new TemplateSerializer(ui);

        for (ElementTemplate template : templates) {
            templatesJson.put(Integer.toString(template.getId()),
                    templateSerializer.serializeTemplate(template));
            ui.registerTemplate(template);
        }
    }

    private void changesToJson(
            LinkedHashMap<StateNode, List<NodeChange>> changes) {
        for (StateNode node : changes.keySet()) {
            for (NodeChange change : changes.get(node)) {
                JsonObject changeJson = createChangeJson(node, change);
                changesJson.set(changesJson.length(), changeJson);
            }
        }
    }

    private JsonObject createChangeJson(StateNode node, NodeChange change) {
        JsonObject changeJson = Json.createObject();
        // abs since currently detached nodes have -id
        changeJson.put(ID, Math.abs(node.getId()));
        changeJson.put("type", changeToJsonType(change));

        if (change instanceof NodeContentsChange) {
            NodeContentsChange contentsChange = (NodeContentsChange) change;
            putKey(changeJson, contentsChange);

            if (contentsChange instanceof NodeDataChange) {
                putValue(changeJson, ((NodeDataChange) change).getValue());
            } else if (contentsChange instanceof NodeListChange) {
                NodeListChange listChange = (NodeListChange) contentsChange;
                changeJson.put(LIST_INDEX, listChange.getIndex());
                putValue(changeJson, listChange.getValue());
            } else if (contentsChange instanceof RangeEndChange) {
                changeJson.put(VALUE,
                        ((RangeEndChange) contentsChange).getRangeEnd());
            } else if (contentsChange instanceof RangeStartChange) {
                changeJson.put(VALUE,
                        ((RangeStartChange) contentsChange).getRangeStart());
            }
        }

        return changeJson;
    }

    private void putValue(JsonObject changeJson, Object value) {
        changeJson.put(VALUE, encode(value));
    }

    private JsonValue encode(Object value) {
        if (value instanceof StateNode) {
            return Json.create(Math.abs(((StateNode) value).getId()));
        } else if (value instanceof ElementTemplate) {
            return Json.create(((ElementTemplate) value).getId());
        } else if (value instanceof Collection) {
            JsonArray arr = Json.createArray();
            Iterator<Object> i = ((Collection) value).iterator();
            int index = 0;
            while (i.hasNext()) {
                arr.set(index++, encode(i.next()));
            }
            return arr;

        } else {
            return JsonConverter.toJson(value);
        }
    }

    private void putKey(JsonObject changeJson, NodeContentsChange change) {
        Object key = change.getKey();
        if (key instanceof ElementTemplate) {
            int templateId = ((ElementTemplate) key).getId();
            changeJson.put(KEY, templateId);
        } else {
            changeJson.put(KEY, String.valueOf(change.getKey()));
        }
    }

    private String changeToJsonType(NodeChange change) {
        if (change instanceof RemoveChange) {
            return TYPE_REMOVE;
        } else if (change instanceof PutChange) {
            PutChange pc = (PutChange) change;
            if (pc.getValue() instanceof StateNode) {
                if (pc.getKey() instanceof ElementTemplate) {
                    return TYPE_PUT_OVERRIDE;
                } else {
                    return TYPE_PUT_NODE;
                }
            } else {
                return TYPE_PUT;
            }
        } else if (change instanceof ListReplaceChange) {
            ListReplaceChange lrc = (ListReplaceChange) change;
            if (lrc.getValue() instanceof StateNode) {
                return TYPE_LIST_REPLACE_NODE;
            } else {
                return TYPE_LIST_REPLACE;
            }
        } else if (change instanceof ListInsertChange) {
            ListInsertChange lic = (ListInsertChange) change;
            if (lic.getValue() instanceof StateNode) {
                return TYPE_LIST_INSERT_NODE;
            } else {
                return TYPE_LIST_INSERT;
            }
        } else if (change instanceof ListRemoveChange) {
            return TYPE_LIST_REMOVE;
        } else if (change instanceof ListInsertManyChange) {
            ListInsertManyChange lic = (ListInsertManyChange) change;
            if (lic.getValue().get(0) instanceof StateNode) {
                return TYPE_LIST_INSERT_NODES;
            } else {
                return TYPE_LIST_INSERTS;
            }
        } else if (change instanceof RangeStartChange) {
            return TYPE_RANGE_START;
        } else if (change instanceof RangeEndChange) {
            return TYPE_RANGE_END;
        } else {
            throw new IllegalArgumentException(
                    "Unknown change type: " + change.getClass().getName());
        }
    }

}
