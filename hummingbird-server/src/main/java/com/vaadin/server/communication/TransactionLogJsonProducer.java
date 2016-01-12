package com.vaadin.server.communication;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.hummingbird.kernel.ComputedProperty;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.JsonConverter;
import com.vaadin.hummingbird.kernel.LazyList;
import com.vaadin.hummingbird.kernel.ListNode;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.ValueType;
import com.vaadin.hummingbird.kernel.ValueType.ArrayType;
import com.vaadin.hummingbird.kernel.ValueType.ObjectType;
import com.vaadin.hummingbird.kernel.change.IdChange;
import com.vaadin.hummingbird.kernel.change.ListChange;
import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListInsertManyChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;
import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.hummingbird.kernel.change.NodeContentsChange;
import com.vaadin.hummingbird.kernel.change.NodeDataChange;
import com.vaadin.hummingbird.kernel.change.PutChange;
import com.vaadin.hummingbird.kernel.change.RangeEndChange;
import com.vaadin.hummingbird.kernel.change.RangeStartChange;
import com.vaadin.hummingbird.kernel.change.RemoveChange;
import com.vaadin.ui.UI;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class TransactionLogJsonProducer {

    private static final String ID = "id";
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String VALUE_MAP = "mapValue";
    private static final String VALUE_LIST = "listValue";
    private static final String TYPE_PUT = "put";
    private static final String LIST_INDEX = "index";
    private static final String NODE_TYPE = "nodeType";

    private static final String TYPE_REMOVE = "remove";
    private static final String TYPE_PUT_OVERRIDE = "putOverride";
    private static final String TYPE_SPLICE = "splice";
    private static final String TYPE_RANGE_START = "rangeStart";
    private static final String TYPE_RANGE_END = "rangeEnd";
    private static final String TYPE_CREATE = "create";

    private JsonArray changesJson = Json.createArray();
    private JsonObject templatesJson = Json.createObject();
    private JsonObject valueTypesJson = null;

    private UI ui;

    public TransactionLogJsonProducer(UI ui,
            LinkedHashMap<StateNode, List<NodeChange>> changes,
            Set<ElementTemplate> set, Set<ObjectType> valueTypes) {
        this.ui = ui;
        changesToJson(changes);
        templatesToJson(set);
        valueTypesToJson(valueTypes);
    }

    private void valueTypesToJson(Set<ObjectType> valueTypes) {
        if (valueTypes.isEmpty()) {
            return;
        }

        valueTypesJson = Json.createObject();
        for (ObjectType type : valueTypes) {
            JsonObject typeJson = Json.createObject();

            JsonObject properties = Json.createObject();
            boolean hasProperties = false;

            for (Entry<Object, ValueType> entry : type.getPropertyTypes()
                    .entrySet()) {
                Object nameObj = entry.getKey();
                if (nameObj instanceof String) {
                    hasProperties = true;
                    String name = (String) nameObj;
                    properties.put(name, entry.getValue().getId());
                }
            }
            if (hasProperties) {
                typeJson.put("properties", properties);
            }

            JsonObject computed = Json.createObject();
            boolean hasComputed = false;

            for (ComputedProperty computedProperty : type
                    .getComputedProperties().values()) {
                if (computedProperty.hasClientCode()) {
                    hasComputed = true;

                    computed.put(computedProperty.getName(),
                            computedProperty.getClientCode());
                }
            }

            if (hasComputed) {
                typeJson.put("computed", computed);
            }

            if (type instanceof ArrayType) {
                ArrayType arrayType = (ArrayType) type;

                typeJson.put("member", arrayType.getMemberType().getId());
            }

            valueTypesJson.put(Integer.toString(type.getId()), typeJson);
        }
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
                Object value = ((NodeDataChange) change).getValue();
                if (value instanceof StateNode) {
                    StateNode child = (StateNode) value;
                    JsonNumber id = Json.create(Math.abs(child.getId()));
                    if (value instanceof ListNode
                            || value instanceof LazyList<?>) {
                        changeJson.put(VALUE_LIST, id);
                    } else {
                        changeJson.put(VALUE_MAP, id);
                    }
                } else {
                    changeJson.put(VALUE, encode(value));
                }
            }
        } else if (change instanceof ListChange) {
            List<?> values = null;
            ListChange listChange = (ListChange) change;
            changeJson.put(LIST_INDEX, listChange.getIndex());

            if (listChange instanceof ListInsertChange) {
                values = Collections.singletonList(listChange.getValue());
            } else if (listChange instanceof ListReplaceChange) {
                changeJson.put("remove", 1);
                values = Collections.singletonList(listChange.getValue());
            } else if (listChange instanceof ListRemoveChange) {
                changeJson.put("remove", 1);
            } else if (listChange instanceof ListInsertManyChange) {
                values = (List<?>) listChange.getValue();
            }

            if (values != null && !values.isEmpty()) {
                String key;
                Stream<JsonValue> valueStream;

                // Assumes all children are of the same kind
                Object first = values.get(0);
                if (first instanceof StateNode) {
                    if (first instanceof ListNode
                            || first instanceof LazyList<?>) {
                        key = VALUE_LIST;
                    } else {
                        key = VALUE_MAP;
                    }
                    valueStream = values.stream()
                            .map(v -> Json.create(((StateNode) v).getId()));
                } else {
                    key = VALUE;
                    valueStream = values.stream().map(this::encode);
                }

                JsonArray value = Json.createArray();
                valueStream.forEach(v -> value.set(value.length(), v));
                changeJson.put(key, value);
            }
        } else if (change instanceof RangeEndChange) {
            changeJson.put(VALUE, ((RangeEndChange) change).getRangeEnd());
        } else if (change instanceof RangeStartChange) {
            changeJson.put(VALUE, ((RangeStartChange) change).getRangeStart());
        } else if (change instanceof IdChange) {
            changeJson.put(NODE_TYPE, node.getType().getId());
        }

        return changeJson;
    }

    private JsonValue encode(Object value) {
        if (value instanceof ElementTemplate) {
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
            if (pc.getValue() instanceof StateNode
                    && pc.getKey() instanceof ElementTemplate) {
                return TYPE_PUT_OVERRIDE;
            } else {
                return TYPE_PUT;
            }
        } else if (change instanceof IdChange) {
            return TYPE_CREATE;
        } else if (change instanceof ListChange) {
            return TYPE_SPLICE;
        } else if (change instanceof RangeStartChange) {
            return TYPE_RANGE_START;
        } else if (change instanceof RangeEndChange) {
            return TYPE_RANGE_END;
        } else {
            throw new IllegalArgumentException(
                    "Unknown change type: " + change.getClass().getName());
        }
    }

    public JsonObject getValueTypesJson() {
        return valueTypesJson;
    }

}
