package com.vaadin.hummingbird.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vaadin.hummingbird.parser.EventBinding;

public class BoundTemplateBuilder implements TemplateBuilder {

    private final String tag;

    private final Collection<AttributeBinding> attributeBindings = new ArrayList<>();
    private final Map<String, String> defaultAttributeValues = new HashMap<>();
    private final Collection<EventBinding> events = new ArrayList<>();
    private final Set<String> eventHandlerMethods = new HashSet<>();

    private List<TemplateBuilder> childTemplates;
    private Supplier<BoundElementTemplate> templateCreator = null;

    public BoundTemplateBuilder(String tag) {
        this.tag = tag;
    }

    @Override
    public BoundElementTemplate build() {
        if (templateCreator == null) {
            return new BoundElementTemplate(this);
        } else {
            return templateCreator.get();
        }
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

        templateCreator = () -> new ForElementTemplate(this, listPath,
                innerScope);
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

    public BoundTemplateBuilder addEventHandlerMethod(String methodName) {
        eventHandlerMethods.add(methodName);
        return this;
    }

    public String getTag() {
        return tag;
    }

    public Collection<AttributeBinding> getAttributeBindings() {
        return Collections.unmodifiableCollection(attributeBindings);
    }

    public Map<String, String> getDefaultAttributeValues() {
        return Collections.unmodifiableMap(defaultAttributeValues);
    }

    public Collection<EventBinding> getEvents() {
        return Collections.unmodifiableCollection(events);
    }

    public List<BoundElementTemplate> getChildTemplates() {
        if (childTemplates == null) {
            return null;
        } else {
            return childTemplates.stream().map(TemplateBuilder::build)
                    .collect(Collectors.toList());
        }
    }

    public Set<String> getEventHandlerMethods() {
        return Collections.unmodifiableSet(eventHandlerMethods);
    }
}
