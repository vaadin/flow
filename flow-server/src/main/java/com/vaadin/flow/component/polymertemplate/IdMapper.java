package com.vaadin.flow.component.polymertemplate;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;

public class IdMapper implements Serializable {
    private final Map<String, Element> registeredElementIdToInjected = new HashMap<>();

    private AbstractTemplate<?> template;

    public IdMapper(AbstractTemplate<?> template) {
        this.template = template;
    }

    public void mapComponentOrElement(Field field, String id, String tag,
            Consumer<Element> beforeComponentInject) {
        Element element = getElementById(id).orElse(null);

        if (element == null) {
            injectClientSideElement(tag, id, field, beforeComponentInject);
        } else {
            injectServerSideElement(element, field, beforeComponentInject);
        }
    }

    private void injectServerSideElement(Element element, Field field,
            Consumer<Element> beforeComponentInject) {
        if (getElement().equals(element)) {
            throw new IllegalArgumentException(
                    "Cannot map the root element of the template. "
                            + "This is always mapped to the template instance itself ("
                            + getContainerClass().getName() + ')');
        } else if (element != null) {
            injectTemplateElement(element, field, beforeComponentInject);
        }
    }

    private Class<? extends Component> getContainerClass() {
        return template.getClass();
    }

    private void injectClientSideElement(String tagName, String id, Field field,
            Consumer<Element> beforeComponentInject) {
        Class<?> fieldType = field.getType();

        Tag tag = fieldType.getAnnotation(Tag.class);
        if (tag != null && !tagName.equalsIgnoreCase(tag.value())) {
            String msg = String.format(
                    "Class '%s' has field '%s' whose type '%s' is annotated with "
                            + "tag '%s' but the element defined in the HTML "
                            + "template with id '%s' has tag name '%s'",
                    getContainerClass().getName(), field.getName(),
                    fieldType.getName(), tag.value(), id, tagName);
            throw new IllegalStateException(msg);
        }
        attachExistingElementById(tagName, id, field, beforeComponentInject);
    }

    public ShadowRoot getOrCreateShadowRoot() {
        return getElement().getShadowRoot()
                .orElseGet(() -> getElement().attachShadow());
    }

    private Element getElement() {
        return template.getElement();
    }

    private Optional<Element> getElementById(String id) {
        return getOrCreateShadowRoot().getChildren()
                .flatMap(this::flattenChildren)
                .filter(element -> id.equals(element.getAttribute("id")))
                .findFirst();
    }

    private Stream<Element> flattenChildren(Element node) {
        if (node.getChildCount() > 0) {
            return node.getChildren().flatMap(this::flattenChildren);
        }
        return Stream.of(node);
    }

    /**
     * Attaches a child element with the given {@code tagName} and {@code id} to
     * an existing dom element on the client side with matching data.
     *
     * @param tagName
     *            tag name of element, not {@code null}
     * @param id
     *            id of element to attach to
     * @param field
     *            field to attach {@code Element} or {@code Component} to
     * @param beforeComponentInject
     */
    private void attachExistingElementById(String tagName, String id,
            Field field, Consumer<Element> beforeComponentInject) {
        if (tagName == null) {
            throw new IllegalArgumentException(
                    "Tag name parameter cannot be null");
        }

        Element element = registeredElementIdToInjected.get(id);
        if (element == null) {
            element = new Element(tagName);
            VirtualChildrenList list = getElement().getNode()
                    .getFeature(VirtualChildrenList.class);
            list.append(element.getNode(), NodeProperties.INJECT_BY_ID, id);
            registeredElementIdToInjected.put(id, element);
        }
        injectTemplateElement(element, field, beforeComponentInject);
    }

    @SuppressWarnings("unchecked")
    private void injectTemplateElement(Element element, Field field,
            Consumer<Element> beforeComponentInject) {
        Class<?> fieldType = field.getType();
        if (Component.class.isAssignableFrom(fieldType)) {
            beforeComponentInject.accept(element);
            Component component;

            Optional<Component> wrappedComponent = element.getComponent();
            if (wrappedComponent.isPresent()) {
                component = wrappedComponent.get();
            } else {
                Class<? extends Component> componentType = (Class<? extends Component>) fieldType;
                component = Component.from(element, componentType);
            }

            ReflectTools.setJavaFieldValue(template, field, component);
        } else if (Element.class.isAssignableFrom(fieldType)) {
            ReflectTools.setJavaFieldValue(template, field, element);
        } else {
            String msg = String.format(
                    "The field '%s' in '%s' has an @'%s' "
                            + "annotation but the field type '%s' "
                            + "does not extend neither '%s' nor '%s'",
                    field.getName(), getContainerClass().getName(),
                    Id.class.getSimpleName(), fieldType.getName(),
                    Component.class.getSimpleName(),
                    Element.class.getSimpleName());

            throw new IllegalArgumentException(msg);
        }
    }

    public void clear() {
        registeredElementIdToInjected.clear();
    }

    public boolean isMapped(String id) {
        return registeredElementIdToInjected.containsKey(id);
    }

}
