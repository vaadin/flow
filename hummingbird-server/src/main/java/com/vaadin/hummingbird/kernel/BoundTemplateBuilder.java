package com.vaadin.hummingbird.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.hummingbird.parser.EventBinding;

public class BoundTemplateBuilder implements TemplateBuilder {

    @FunctionalInterface
    private interface TemplateCreator {
        public BoundElementTemplate create(String tag,
                Collection<AttributeBinding> attributeBindings,
                Map<String, String> defaultAttributeValues,
                Collection<EventBinding> events,
                List<BoundElementTemplate> childTemplates);
    }

    private final String tag;
    private final Collection<AttributeBinding> attributeBindings = new ArrayList<>();
    private final Map<String, String> defaultAttributeValues = new HashMap<>();
    private final Collection<EventBinding> events = new ArrayList<>();
    private List<TemplateBuilder> childTemplates;
    private TemplateCreator templateCreator = null;

    public BoundTemplateBuilder(String tag) {
        this.tag = tag;
    }

    @Override
    public BoundElementTemplate build() {
        TemplateCreator actualCreator = templateCreator != null
                ? templateCreator : BoundElementTemplate::new;
        return actualCreator.create(tag, new ArrayList<>(attributeBindings),
                new HashMap<>(defaultAttributeValues), new ArrayList<>(events),
                childTemplates == null ? null
                        : childTemplates.stream().map(TemplateBuilder::build)
                                .collect(Collectors.toList()));
    }

    public BoundTemplateBuilder addChild(TemplateBuilder childTemplate) {
        if (childTemplates == null) {
            childTemplates = new ArrayList<>();
        }
        childTemplates.add(childTemplate);
        return this;
    }

    public BoundTemplateBuilder setForDefinition(ModelPath listPath,
            String innerScope) {
        if (templateCreator != null) {
            throw new IllegalStateException(
                    "Only one for definition allowed per builder");
        }

        templateCreator = new TemplateCreator() {
            @Override
            public BoundElementTemplate create(String tag,
                    Collection<AttributeBinding> attributeBindings,
                    Map<String, String> defaultAttributeValues,
                    Collection<EventBinding> events,
                    List<BoundElementTemplate> childTemplates) {
                return new ForElementTemplate(tag, attributeBindings,
                        defaultAttributeValues, events, listPath, innerScope,
                        childTemplates);
            }
        };
        return this;
    }

    public BoundTemplateBuilder bindAttribute(
            AttributeBinding attributeBinding) {
        attributeBindings.add(attributeBinding);
        return this;
    }

    public BoundTemplateBuilder bindAttribute(String attributeName,
            String propertyName) {
        return bindAttribute(
                new ModelAttributeBinding(attributeName, propertyName));
    }

    public BoundTemplateBuilder setAttribute(String attributeName,
            String value) {
        defaultAttributeValues.put(attributeName, value);
        return this;
    }

    public BoundTemplateBuilder addEventBinding(EventBinding eventBinding) {
        events.add(eventBinding);
        return this;
    }
}
