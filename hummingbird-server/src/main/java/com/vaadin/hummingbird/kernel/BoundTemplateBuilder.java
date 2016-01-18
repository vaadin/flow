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

    private final String is;

    private final Map<String, Binding> attributeBindings = new HashMap<>();
    private final Map<String, String> defaultAttributeValues = new HashMap<>();
    private final Collection<EventBinding> events = new ArrayList<>();
    private final Set<String> eventHandlerMethods = new HashSet<>();

    private List<TemplateBuilder> childTemplates;
    private Supplier<BoundElementTemplate> templateCreator = null;

    public BoundTemplateBuilder(String tag) {
        this.tag = tag;
        is = null;
    }

    public BoundTemplateBuilder(String tag, String is) {
        this.tag = tag;
        this.is = is;
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

    public BoundTemplateBuilder setForDefinition(Binding binding,
            String innerScope, String indexVariable, String evenVariable,
            String oddVariable, String lastVariable) {
        if (templateCreator != null) {
            throw new IllegalStateException(
                    "Only one for definition allowed per builder");
        }

        templateCreator = () -> new ForElementTemplate(this, binding,
                innerScope, indexVariable, evenVariable, oddVariable,
                lastVariable);
        return this;
    }

    public BoundTemplateBuilder bindAttribute(String attributeName,
            Binding attributeBinding) {
        attributeBindings.put(attributeName, attributeBinding);
        return this;
    }

    public BoundTemplateBuilder bindAttribute(String attributeName,
            String propertyName) {
        return bindAttribute(attributeName, new StateNodeBinding(propertyName));
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

    public String getIs() {
        return is;
    }

    public Map<String, Binding> getAttributeBindings() {
        return Collections.unmodifiableMap(attributeBindings);
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
