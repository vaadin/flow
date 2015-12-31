package com.vaadin.server.communication;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.vaadin.hummingbird.kernel.Binding;
import com.vaadin.hummingbird.kernel.BoundElementTemplate;
import com.vaadin.hummingbird.kernel.DynamicTextTemplate;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.ForElementTemplate;
import com.vaadin.hummingbird.kernel.ModelBinding;
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

    private String serializeBinding(Binding binding) {
        if (binding instanceof ModelBinding) {
            ModelBinding mb = (ModelBinding) binding;

            assert mb.getBinding() != null;
            return mb.getBinding();
        } else {
            throw new RuntimeException(
                    "Only " + ModelBinding.class.getName() + " is supported");
        }
    }

    private void serializeDynamicTextTemplate(JsonObject serialized,
            DynamicTextTemplate template, UI ui) {
        Binding binding = template.getBinding();
        serialized.put("binding", serializeBinding(binding));
    }

    private void serializeForTemplate(JsonObject serialized,
            ForElementTemplate template, UI ui) {
        Binding binding = template.getListBinding();
        serialized.put("modelKey", serializeBinding(binding));
        serialized.put("innerScope", template.getInnerScope());
        String indexVariable = template.getIndexVariable();
        if (indexVariable != null) {
            serialized.put("indexVar", indexVariable);
        }
        String evenVariable = template.getEvenVariable();
        if (evenVariable != null) {
            serialized.put("evenVar", evenVariable);
        }
        String oddVariable = template.getOddVariable();
        if (oddVariable != null) {
            serialized.put("oddVar", oddVariable);
        }
        String lastVariable = template.getLastVariable();
        if (lastVariable != null) {
            serialized.put("lastVar", lastVariable);
        }
        serializeBoundElementTemplate(serialized, template);
    }

    private void serializeBoundElementTemplate(JsonObject serialized,
            BoundElementTemplate bet) {
        JsonObject attributeBindings = Json.createObject();
        for (Entry<String, Binding> entry : bet.getAttributeBindings()
                .entrySet()) {
            String attributeName = entry.getKey();
            Binding attributeBinding = entry.getValue();
            attributeBindings.put(attributeName,
                    serializeBinding(attributeBinding));
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
            classPartBindings.put(key, serializeBinding(binding));
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
