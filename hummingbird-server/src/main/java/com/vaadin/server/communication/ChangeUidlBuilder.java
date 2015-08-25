package com.vaadin.server.communication;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.vaadin.hummingbird.kernel.AbstractElementTemplate;
import com.vaadin.hummingbird.kernel.AttributeBinding;
import com.vaadin.hummingbird.kernel.BoundElementTemplate;
import com.vaadin.hummingbird.kernel.DynamicTextTemplate;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.ForElementTemplate;
import com.vaadin.hummingbird.kernel.JsonConverter;
import com.vaadin.hummingbird.kernel.ModelAttributeBinding;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.StaticTextTemplate;
import com.vaadin.hummingbird.kernel.change.IdChange;
import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;
import com.vaadin.hummingbird.kernel.change.NodeChangeVisitor;
import com.vaadin.hummingbird.kernel.change.ParentChange;
import com.vaadin.hummingbird.kernel.change.PutChange;
import com.vaadin.hummingbird.kernel.change.RemoveChange;
import com.vaadin.hummingbird.parser.EventBinding;
import com.vaadin.ui.UI;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public final class ChangeUidlBuilder implements NodeChangeVisitor {
    private JsonArray changes = Json.createArray();
    private JsonObject newTemplates = Json.createObject();
    private UI ui;

    public ChangeUidlBuilder(UI ui) {
        this.ui = ui;
    }

    public JsonArray getChanges() {
        return changes;
    }

    public JsonObject getNewTemplates() {
        return newTemplates;
    }

    private boolean isServerOnly(StateNode node) {
        if (node == null) {
            return false;
        } else if (node.containsKey(AbstractElementTemplate.Keys.SERVER_ONLY)) {
            return true;
        } else {
            return isServerOnly(node.getParent());
        }
    }

    private boolean isServerOnlyKey(Object key) {
        if (key == null) {
            return false;
        } else if (key instanceof Class) {
            return true;
        } else {
            return false;
        }
    }

    private JsonObject createChange(StateNode node, String type) {
        assert!isServerOnly(node);

        JsonObject change = Json.createObject();
        change.put("type", type);
        // abs since currently detached nodes have -id
        change.put("id", Math.abs(node.getId()));
        changes.set(changes.length(), change);
        return change;
    }

    @Override
    public void visitRemoveChange(StateNode node, RemoveChange removeChange) {
        if (isServerOnly(node)) {
            return;
        }
        if (removeChange.getValue() instanceof StateNode
                && isServerOnly((StateNode) removeChange.getValue())) {
            return;
        }
        Object key = removeChange.getKey();
        if (isServerOnlyKey(key)) {
            return;
        }

        JsonObject change = createChange(node, "remove");
        assert key instanceof String || key instanceof Enum;
        change.put("key", String.valueOf(key));
    }

    @Override
    public void visitPutChange(StateNode node, PutChange putChange) {
        if (isServerOnly(node)) {
            return;
        }
        JsonObject change;
        Object key = putChange.getKey();
        if (isServerOnlyKey(key)) {
            return;
        }

        Object value = putChange.getValue();
        if (value instanceof StateNode) {
            StateNode childNode = (StateNode) value;
            if (isServerOnly(childNode)) {
                return;
            }
            if (key instanceof ElementTemplate) {
                change = createChange(node, "putOverride");

                ElementTemplate template = (ElementTemplate) key;
                key = Integer.valueOf(template.getId());
                ensureTemplateSent(template, ui, newTemplates);
            } else {
                change = createChange(node, "putNode");
            }
            change.put("value", childNode.getId());
        } else {
            change = createChange(node, "put");
            if (value instanceof ElementTemplate) {
                ElementTemplate template = (ElementTemplate) value;
                value = Integer.valueOf(template.getId());
                ensureTemplateSent(template, ui, newTemplates);
            }
            change.put("value", JsonConverter.toJson(value));
        }
        assert key instanceof String || key instanceof Enum;
        change.put("key", String.valueOf(key));

        int length = changes.length();
        if (length >= 2) {
            JsonObject previousChange = changes.getObject(changes.length() - 2);
            if ("remove".equals(previousChange.getString("type"))
                    && change.getNumber("id") == previousChange.getNumber("id")
                    && key.equals(previousChange.getString("key"))) {
                changes.remove(changes.length() - 2);
            }
        }
    }

    private void ensureTemplateSent(ElementTemplate template, UI ui,
            JsonObject newTemplates) {

        if (!ui.knowsTemplate(template)) {
            newTemplates.put(Integer.toString(template.getId()),
                    serializeTemplate(template, ui, newTemplates));
            ui.registerTemplate(template);
        }
    }

    private JsonObject serializeTemplate(ElementTemplate template, UI ui,
            JsonObject newTemplates) {
        JsonObject serialized = Json.createObject();
        serialized.put("type", template.getClass().getSimpleName());
        serialized.put("id", template.getId());

        if (template.getClass() == BoundElementTemplate.class) {
            serializeBoundElementTemplate(serialized,
                    (BoundElementTemplate) template);
        } else if (template.getClass() == ForElementTemplate.class) {
            serializeForTemplate(serialized, (ForElementTemplate) template, ui,
                    newTemplates);
        } else if (template.getClass() == DynamicTextTemplate.class) {
            serializeDynamicTextTemplate(serialized,
                    (DynamicTextTemplate) template, ui, newTemplates);
        } else if (template.getClass() == StaticTextTemplate.class) {
            serializeStaticTextTemplate(serialized,
                    (StaticTextTemplate) template, ui, newTemplates);
        } else {
            throw new RuntimeException(template.toString());
        }
        return serialized;
    }

    private void serializeStaticTextTemplate(JsonObject serialized,
            StaticTextTemplate template, UI ui, JsonObject newTemplates) {
        serialized.put("content", template.getContent());
    }

    private void serializeDynamicTextTemplate(JsonObject serialized,
            DynamicTextTemplate template, UI ui, JsonObject newTemplates) {
        AttributeBinding binding = template.getBinding();
        if (binding instanceof ModelAttributeBinding) {
            ModelAttributeBinding mab = (ModelAttributeBinding) binding;
            serialized.put("binding", mab.getPath().getFullPath());
        } else {
            throw new RuntimeException(binding.toString());
        }
    }

    private void serializeForTemplate(JsonObject serialized,
            ForElementTemplate template, UI ui, JsonObject newTemplates) {
        serialized.put("modelKey", template.getModelProperty().getFullPath());
        serialized.put("innerScope", template.getInnerScope());

        serializeBoundElementTemplate(serialized, template);
    }

    private void serializeBoundElementTemplate(JsonObject serialized,
            BoundElementTemplate bet) {
        JsonObject attributeBindings = Json.createObject();
        for (AttributeBinding attributeBinding : bet.getAttributeBindings()
                .values()) {
            if (attributeBinding instanceof ModelAttributeBinding) {
                ModelAttributeBinding mab = (ModelAttributeBinding) attributeBinding;
                attributeBindings.put(mab.getPath().getFullPath(),
                        mab.getAttributeName());
            } else {
                // Not yet supported
                throw new RuntimeException(attributeBinding.toString());
            }
        }

        List<BoundElementTemplate> childTemplates = bet.getChildTemplates();
        if (childTemplates != null) {
            JsonArray children = Json.createArray();
            serialized.put("children", children);
            for (BoundElementTemplate childTemplate : childTemplates) {
                ensureTemplateSent(childTemplate, ui, newTemplates);
                children.set(children.length(), childTemplate.getId());
            }
        }

        JsonObject defaultAttributes = Json.createObject();
        bet.getDefaultAttributeValues().forEach(defaultAttributes::put);

        JsonObject classPartBindings = Json.createObject();
        bet.getClassPartBindings().forEach((key, binding) -> {
            if (binding instanceof ModelAttributeBinding) {
                ModelAttributeBinding mab = (ModelAttributeBinding) binding;
                classPartBindings.put(mab.getPath().getFullPath(), key);
            } else {
                // Not yet supported
                throw new RuntimeException(binding.toString());
            }
        });

        if (classPartBindings.keys().length != 0) {
            serialized.put("classPartBindings", classPartBindings);
        }

        Map<String, List<EventBinding>> events = bet.getEvents();
        if (events != null && !events.isEmpty()) {
            JsonObject eventsJson = Json.createObject();
            events.forEach((type, list) -> {
                JsonArray params = Json.createArray();
                list.stream().map(EventBinding::getParams)
                        .flatMap(Collection::stream)
                        .filter(p -> !"element".equals(p)).distinct()
                        .forEach(p -> params.set(params.length(), p));

                eventsJson.put(type, params);
            });
            serialized.put("events", eventsJson);
        }

        serialized.put("attributeBindings", attributeBindings);
        serialized.put("defaultAttributes", defaultAttributes);
        serialized.put("tag", bet.getTag());
    }

    @Override
    public void visitParentChange(StateNode node, ParentChange parentChange) {
        // Ignore
    }

    @Override
    public void visitListReplaceChange(StateNode node,
            ListReplaceChange listReplaceChange) {
        if (isServerOnly(node)) {
            return;
        }
        Object key = listReplaceChange.getKey();
        if (isServerOnlyKey(key)) {
            return;
        }

        JsonObject change;
        Object value = listReplaceChange.getNewValue();
        if (value instanceof StateNode) {
            change = createChange(node, "listReplaceNode");
            change.put("value", ((StateNode) value).getId());
        } else {
            change = createChange(node, "listReplace");
            change.put("value", JsonConverter.toJson(value));
        }
        change.put("index", listReplaceChange.getIndex());
        assert key instanceof String || key instanceof Enum;
        change.put("key", String.valueOf(key));
    }

    @Override
    public void visitListRemoveChange(StateNode node,
            ListRemoveChange listRemoveChange) {
        if (isServerOnly(node)) {
            return;
        }
        Object key = listRemoveChange.getKey();
        if (isServerOnlyKey(key)) {
            return;
        }

        JsonObject change = createChange(node, "listRemove");
        change.put("index", listRemoveChange.getIndex());
        assert key instanceof String || key instanceof Enum;
        change.put("key", String.valueOf(key));
    }

    @Override
    public void visitListInsertChange(StateNode node,
            ListInsertChange listInsertChange) {
        if (isServerOnly(node)) {
            return;
        }
        Object key = listInsertChange.getKey();
        if (isServerOnlyKey(key)) {
            return;
        }

        JsonObject change;
        Object value = listInsertChange.getValue();
        if (value instanceof StateNode) {
            change = createChange(node, "listInsertNode");
            change.put("value", ((StateNode) value).getId());
        } else {
            change = createChange(node, "listInsert");
            change.put("value", JsonConverter.toJson(value));
        }
        change.put("index", listInsertChange.getIndex());
        assert key instanceof String || key instanceof Enum;
        change.put("key", String.valueOf(key));
    }

    @Override
    public void visitIdChange(StateNode node, IdChange idChange) {
        // Ignore
    }
}