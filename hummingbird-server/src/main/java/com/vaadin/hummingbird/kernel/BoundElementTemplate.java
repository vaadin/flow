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
    private final String is;

    private final Map<String, Binding> attributeBindings;
    private final Map<String, Binding> classPartBindings;
    private final Map<String, String> defaultAttributeValues;
    private final Map<String, List<EventBinding>> events;
    private final Set<String> eventHandlerMethods;

    private final List<BoundElementTemplate> childTemplates;

    private BoundElementTemplate parentTemplate;

    public BoundElementTemplate(BoundTemplateBuilder builder) {
        attributeBindings = new HashMap<>();
        classPartBindings = new LinkedHashMap<>();
        for (Entry<String, Binding> entry : builder.getAttributeBindings()
                .entrySet()) {
            String attributeName = entry.getKey();
            Binding b = entry.getValue();

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
        is = builder.getIs();

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
    public String getIs(StateNode node) {
        return is;
    }

    @Override
    public Object getAttribute(String name, StateNode node) {
        Binding binding = attributeBindings.get(name);
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
                value = bindingValue;
            }
        }

        if ("class".equals(name)) {
            assert value instanceof String || value == null;
            StringBuilder builder = new StringBuilder();
            if (value != null) {
                builder.append((String) value);
            }
            for (Entry<String, Binding> entry : classPartBindings.entrySet()) {
                Object classBindingValue = entry.getValue().getValue(node);
                if (isTrueish(classBindingValue)) {
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

    public static boolean isTrueish(Object value) {
        if (Boolean.TRUE.equals(value)) {
            return true;
        } else if (value instanceof String) {
            return !((String) value).equalsIgnoreCase("false");
        } else {
            return false;
        }
    }

    @Override
    public void setAttribute(String name, Object value, StateNode node) {
        Binding binding = attributeBindings.get(name);
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
    public List<String> getAllClasses(StateNode node) {
        String boundClassNames = (String) getAttribute("class", node);
        if (boundClassNames == null || boundClassNames.isEmpty()) {
            return super.getAllClasses(node);
        } else {
            ArrayList<String> classList = new ArrayList<>();
            classList.addAll(super.getClassList(node, false));
            Stream.of(boundClassNames.split(" "))
                    .forEach(str -> classList.add(str));
            return Collections.unmodifiableList(classList);
        }
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
        if (attributeBindings.isEmpty() && defaultAttributeValues.isEmpty()
                && classPartBindings.isEmpty()) {
            return superAttributes;
        }

        // Collect all candidate names
        HashSet<String> attributeNames = new HashSet<>(
                attributeBindings.keySet());
        attributeNames.addAll(defaultAttributeValues.keySet());
        attributeNames.addAll(superAttributes);
        // Remove "class" since we will handled it separately
        attributeNames.remove("class");

        // Remove anything that isn't actually there
        attributeNames.removeIf(name -> {
            Object value = getAttribute(name, node);
            return value == null || Boolean.FALSE.equals(value);
        });

        if (!getAllClasses(node).isEmpty()) {
            attributeNames.add("class");
        }

        return Collections.unmodifiableCollection(attributeNames);
    }

    public Map<String, Binding> getAttributeBindings() {
        return Collections.unmodifiableMap(attributeBindings);
    }

    public Map<String, String> getDefaultAttributeValues() {
        return Collections.unmodifiableMap(defaultAttributeValues);
    }

    public Map<String, Binding> getClassPartBindings() {
        return Collections.unmodifiableMap(classPartBindings);
    }

    public String getTag() {
        return tag;
    }

    public String getIs() {
        return is;
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
}
