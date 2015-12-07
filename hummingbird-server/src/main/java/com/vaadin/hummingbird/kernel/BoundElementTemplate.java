package com.vaadin.hummingbird.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.hummingbird.parser.EventBinding;

public class BoundElementTemplate extends AbstractElementTemplate {

    private final String tag;

    private final Map<String, AttributeBinding> attributeBindings;
    private final Map<String, AttributeBinding> classPartBindings;
    private final Map<String, String> defaultAttributeValues;
    private final Map<String, List<EventBinding>> events;
    private final Set<String> eventHandlerMethods;

    private final Collection<ComputedProperty> computedProperties;

    private final List<BoundElementTemplate> childTemplates;

    private BoundElementTemplate parentTemplate;

    public BoundElementTemplate(BoundTemplateBuilder builder) {
        attributeBindings = new HashMap<>();
        classPartBindings = new LinkedHashMap<>();
        for (AttributeBinding b : builder.getAttributeBindings()) {
            String attributeName = b.getAttributeName();
            if (attributeName.startsWith("class.")) {
                classPartBindings
                        .put(attributeName.substring("class.".length()), b);
            } else {
                attributeBindings.put(attributeName, b);
            }
        }

        // this.attributeBindings = attributeBindings.parallelStream()
        // .collect(Collectors.toMap(AttributeBinding::getAttributeName, v ->
        // v));
        defaultAttributeValues = new HashMap<>(
                builder.getDefaultAttributeValues());

        events = builder.getEvents().stream()
                .collect(Collectors.groupingBy(EventBinding::getEventType));

        tag = builder.getTag();

        List<BoundElementTemplate> builderChildTemplates = builder
                .getChildTemplates();
        if (builderChildTemplates != null) {
            builderChildTemplates = new ArrayList<>(builderChildTemplates);
        }

        childTemplates = builderChildTemplates;
        if (childTemplates != null) {
            for (BoundElementTemplate childTemplate : childTemplates) {
                childTemplate.parentTemplate = this;
            }
        }

        Set<String> builderHandlerMehtods = builder.getEventHandlerMethods();
        if (builderHandlerMehtods.isEmpty()) {
            eventHandlerMethods = null;
        } else {
            eventHandlerMethods = new HashSet<>(builderHandlerMehtods);
        }

        Collection<ComputedProperty> builderComputedProperties = builder
                .getComputedProperties();
        if (builderComputedProperties.isEmpty()) {
            computedProperties = null;
        } else {
            computedProperties = new HashSet<>(builderComputedProperties);
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
    public Object getAttribute(String name, StateNode node) {
        AttributeBinding binding = attributeBindings.get(name);
        Object value;
        if (binding == null) {
            value = super.getAttribute(name, node);
            if (value == null) {
                value = defaultAttributeValues.get(name);
            }
        } else {
            Object bindingValue = binding.getValue(node);
            if (bindingValue == null) {
                value = null;
            } else {
                value = String.valueOf(bindingValue);
            }
        }
        if (value == null) {
            return null;
        }

        if ("class".equals(name)) {
            assert value instanceof String;
            StringBuilder builder = new StringBuilder(
                    value == null ? null : (String) value);
            for (Entry<String, AttributeBinding> entry : classPartBindings
                    .entrySet()) {
                Object classBindingValue = entry.getValue().getValue(node);
                if (Boolean.TRUE.equals(classBindingValue)
                        || (classBindingValue instanceof String
                                && !((String) classBindingValue)
                                        .equalsIgnoreCase("false"))) {
                    if (builder.length() != 0) {
                        builder.append(' ');
                    }
                    builder.append(entry.getKey());
                }
            }
            value = builder.toString();
        }
        return value;
    }

    @Override
    public void setAttribute(String name, Object value, StateNode node) {
        AttributeBinding binding = attributeBindings.get(name);
        if (binding != null) {
            throw new IllegalStateException(
                    "Attribute " + name + " is bound through a template");
        }

        super.setAttribute(name, value, node);
    }

    @Override
    public StateNode getElementDataNode(StateNode node,
            boolean createIfNeeded) {
        StateNode elementData = node.get(this, StateNode.class);
        if (createIfNeeded && elementData == null) {
            elementData = StateNode.create();
            node.put(this, elementData);
            elementData.put(Keys.OVERRIDE_TEMPLATE, this);
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

    public Map<String, AttributeBinding> getClassPartBindings() {
        return Collections.unmodifiableMap(classPartBindings);
    }

    public String getTag() {
        return tag;
    }

    public List<BoundElementTemplate> getChildTemplates() {
        if (childTemplates != null) {
            return Collections.unmodifiableList(childTemplates);
        } else {
            return null;
        }
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

    public Map<String, List<EventBinding>> getEvents() {
        return events;
    }

    public Set<String> getEventHandlerMethods() {
        if (eventHandlerMethods != null) {
            return Collections.unmodifiableSet(eventHandlerMethods);
        } else {
            return null;
        }
    }

    public Collection<ComputedProperty> getComputedProperties() {
        return computedProperties;
    }
}
