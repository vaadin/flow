package com.vaadin.hummingbird.kernel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BoundElementTemplate extends AbstractElementTemplate {

    private final String tag;

    private final Map<String, AttributeBinding> attributeBindings;
    private final Map<String, String> defaultAttributeValues;

    private final List<BoundElementTemplate> childTemplates;

    private BoundElementTemplate parentTemplate;

    public BoundElementTemplate(String tag,
            Collection<AttributeBinding> attributeBindings,
            Map<String, String> defaultAttributeValues) {
        this(tag, attributeBindings, defaultAttributeValues, null);
    }

    public BoundElementTemplate(String tag,
            Collection<AttributeBinding> attributeBindings,
            Map<String, String> defaultAttributeValues,
            List<BoundElementTemplate> childTemplates) {

        this.attributeBindings = new HashMap<String, AttributeBinding>();
        for (AttributeBinding b : attributeBindings) {
            this.attributeBindings.put(b.getAttributeName(), b);
        }

        // this.attributeBindings = attributeBindings.parallelStream()
        // .collect(Collectors.toMap(AttributeBinding::getAttributeName, v ->
        // v));
        this.defaultAttributeValues = new HashMap<>(defaultAttributeValues);

        this.tag = tag;

        this.childTemplates = childTemplates;
        if (childTemplates != null) {
            for (BoundElementTemplate childTemplate : childTemplates) {
                childTemplate.parentTemplate = this;
            }
        }
    }

    @Override
    public int getChildCount(StateNode node) {
        if (childTemplates == null) {
            return super.getChildCount(node);
        } else {
            int count = 0;
            for (BoundElementTemplate childTemplate : childTemplates) {
                count += childTemplate.getElementCount(node);
            }
            return count;
        }
    }

    @Override
    public Element getChild(int index, StateNode node) {
        if (childTemplates == null) {
            return super.getChild(index, node);
        } else {
            int count = 0;
            for (BoundElementTemplate childTemplate : childTemplates) {
                int templateElements = childTemplate.getElementCount(node);
                if (count + templateElements > index) {
                    int indexInTemplate = index - count;
                    return childTemplate.getElement(node, indexInTemplate);
                }
                count += templateElements;
            }
            throw new IndexOutOfBoundsException();
        }
    }

    protected Element getElement(StateNode node, int indexInTemplate) {
        if (indexInTemplate == 0) {
            return Element.getElement(this, node);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    protected int getElementCount(StateNode node) {
        return 1;
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
            throw new IllegalStateException(
                    "Attribute " + name + " is bound through a template");
        }

        super.setAttribute(name, value, node);
    }

    @Override
    protected StateNode getElementDataNode(StateNode node,
            boolean createIfNeeded) {
        StateNode elementData = node.get(this, StateNode.class);
        if (createIfNeeded && elementData == null) {
            elementData = StateNode.create();
            node.put(this, elementData);
        }

        return elementData;
    }

    @Override
    public boolean supports(StateNode node) {
        return node.get(Keys.TAG) == null;
    }

    @Override
    public Element getParent(StateNode node) {
        if (parentTemplate != null) {
            return Element.getElement(parentTemplate, node);
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
        return Collections.unmodifiableCollection(Stream
                .concat(Stream.concat(superAttributes.stream(),
                        attributeBindings.keySet().stream()),
                defaultAttributeValues.keySet().stream())
                .collect(Collectors.toSet()));
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

    public List<BoundElementTemplate> getChildTemplates() {
        return Collections.unmodifiableList(childTemplates);
    }

    @Override
    public void insertChild(int index, Element child, StateNode node) {
        if (childTemplates == null) {
            super.insertChild(index, child, node);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void removeChild(StateNode node, Element element) {
        if (childTemplates == null) {
            super.removeChild(node, element);
        } else {
            throw new IllegalStateException();
        }
    }

}
