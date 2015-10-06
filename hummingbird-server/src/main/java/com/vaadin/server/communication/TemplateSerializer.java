package com.vaadin.server.communication;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.hummingbird.kernel.AttributeBinding;
import com.vaadin.hummingbird.kernel.BoundElementTemplate;
import com.vaadin.hummingbird.kernel.DynamicTextTemplate;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.ForElementTemplate;
import com.vaadin.hummingbird.kernel.ModelAttributeBinding;
import com.vaadin.hummingbird.kernel.StaticTextTemplate;
import com.vaadin.hummingbird.parser.EventBinding;
import com.vaadin.ui.UI;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class TemplateSerializer {

    private UI ui;

    public TemplateSerializer(UI ui) {
        this.ui = ui;
    }

    public JsonObject serializeTemplate(ElementTemplate template) {
        JsonObject serialized = Json.createObject();
        serialized.put("type", template.getClass().getSimpleName());
        serialized.put("id", template.getId());

        if (template.getClass() == BoundElementTemplate.class) {
            serializeBoundElementTemplate(serialized,
                    (BoundElementTemplate) template);
        } else if (template.getClass() == ForElementTemplate.class) {
            serializeForTemplate(serialized, (ForElementTemplate) template, ui);
        } else if (template.getClass() == DynamicTextTemplate.class) {
            serializeDynamicTextTemplate(serialized,
                    (DynamicTextTemplate) template, ui);
        } else if (template.getClass() == StaticTextTemplate.class) {
            serializeStaticTextTemplate(serialized,
                    (StaticTextTemplate) template, ui);
        } else {
            throw new RuntimeException(template.toString());
        }
        return serialized;
    }

    private void serializeStaticTextTemplate(JsonObject serialized,
            StaticTextTemplate template, UI ui) {
        serialized.put("content", template.getContent());
    }

    private void serializeDynamicTextTemplate(JsonObject serialized,
            DynamicTextTemplate template, UI ui) {
        AttributeBinding binding = template.getBinding();
        if (binding instanceof ModelAttributeBinding) {
            ModelAttributeBinding mab = (ModelAttributeBinding) binding;
            serialized.put("binding", mab.getPath().getFullPath());
        } else {
            throw new RuntimeException(binding.toString());
        }
    }

    private void serializeForTemplate(JsonObject serialized,
            ForElementTemplate template, UI ui) {
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
                JsonArray handlers = Json.createArray();
                list.stream().map(EventBinding::getEventHandler)
                        .forEach(p -> handlers.set(handlers.length(), p));

                eventsJson.put(type, handlers);
            });
            serialized.put("events", eventsJson);
        }

        Set<String> eventHandlerMethods = bet.getEventHandlerMethods();
        if (eventHandlerMethods != null) {
            JsonArray eventHandlerMethodsJson = Json.createArray();
            eventHandlerMethods.forEach(m -> eventHandlerMethodsJson
                    .set(eventHandlerMethodsJson.length(), m));
            serialized.put("eventHandlerMethods", eventHandlerMethodsJson);
        }

        serialized.put("attributeBindings", attributeBindings);
        serialized.put("defaultAttributes", defaultAttributes);
        serialized.put("tag", bet.getTag());
    }

}
