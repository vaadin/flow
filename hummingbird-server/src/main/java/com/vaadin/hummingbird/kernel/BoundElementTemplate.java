package com.vaadin.hummingbird.kernel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BoundElementTemplate extends AbstractElementTemplate {

    private final String tag;

    private final Map<String, AttributeBinding> attributeBindings;
    private final Map<String, String> defaultAttributeValues;

    private Function<StateNode, Element> parentResolver;

    public BoundElementTemplate(String tag, Collection<AttributeBinding> attributeBindings, Map<String, String> defaultAttributeValues) {

        this.attributeBindings = new HashMap<String, AttributeBinding>();
        for (AttributeBinding b : attributeBindings) {
            this.attributeBindings.put(b.getAttributeName(), b);
        }

        // this.attributeBindings = attributeBindings.parallelStream()
        // .collect(Collectors.toMap(AttributeBinding::getAttributeName, v ->
        // v));
        this.defaultAttributeValues = new HashMap<>(defaultAttributeValues);

        this.tag = tag;
    }

    @Override
    public String getTag(StateNode node) {
        return tag;
    }

    @Override
    public String getAttribute(String name, StateNode node) {
        AttributeBinding binding = attributeBindings.get(name);
        if (binding == null) {
            String value = super.getAttribute(name, node);
            if (value == null) {
                value = defaultAttributeValues.get(name);
            }
            return value;
        }

        return binding.getValue(node);
    }

    @Override
    public void setAttribute(String name, String value, StateNode node) {
        AttributeBinding binding = attributeBindings.get(name);
        if (binding != null) {
            throw new IllegalStateException("Attribute " + name + " is bound through a template");
        }

        super.setAttribute(name, value, node);
    }

    @Override
    protected StateNode getElementDataNode(StateNode node, boolean createIfNeeded) {
        StateNode elementData = node.get(this, StateNode.class);
        if (createIfNeeded && elementData == null) {
            elementData = StateNode.create();
            node.put(this, elementData);
        }

        return elementData;
    }

    public void setParentResolver(Function<StateNode, Element> parentResolver) {
        if (this.parentResolver != null) {
            throw new IllegalStateException();
        }
        this.parentResolver = parentResolver;
    }

    @Override
    public boolean supports(StateNode node) {
        return node.get(Keys.TAG) == null;
    }

    @Override
    public Element getParent(StateNode node) {
        if (parentResolver != null) {
            return parentResolver.apply(node);
        }
        return super.getParent(node);
    }

    @Override
    public Collection<String> getAttributeNames(StateNode node) {
        Collection<String> superAttributes = super.getAttributeNames(node);
        if (attributeBindings.isEmpty() && defaultAttributeValues.isEmpty()) {
            return superAttributes;
        }

        // TODO Ignore defaultAttributes that have been explicitly cleared and
        // bindings that resolve to null
        return Collections.unmodifiableCollection(Stream.concat(Stream.concat(superAttributes.stream(), attributeBindings.keySet().stream()), defaultAttributeValues.keySet().stream()).collect(Collectors.toSet()));
    }

    public Map<String, AttributeBinding> getAttributeBindings() {
        return Collections.unmodifiableMap(attributeBindings);
    }

    public Map<String, String> getDefaultAttributeValues() {
        return Collections.unmodifiableMap(defaultAttributeValues);
    }

    public String getTag() {
        return tag;
    }
}
