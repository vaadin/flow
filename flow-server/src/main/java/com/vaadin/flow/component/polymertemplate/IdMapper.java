/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.component.polymertemplate;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;

/**
 * Creates or maps Element instances to fields mapped using {@link Id @Id}.
 *
 * @since 2.0
 */
public class IdMapper implements Serializable {

    private final HashMap<String, Element> registeredElementIdToInjected = new HashMap<>();

    private AbstractTemplate<?> template;

    /**
     * Creates a mapper for the given template.
     *
     * @param template
     *            a template instance
     */
    public IdMapper(AbstractTemplate<?> template) {
        this.template = template;
    }

    /**
     * Maps an element or component to the given field.
     * <p>
     * If an element with the given id exists in the template element tree, that
     * element is used.
     * <p>
     * If no element exists (the typical case), a virtual element is created and
     * later on, when the template has been rendered in the client, is connected
     * to the rendered element with the given id.
     *
     * @param field
     *            the field to assign the element/component to
     * @param id
     *            the id of the element to map
     * @param tag
     *            the tag of the injected element or <code>null</code> if not
     *            known
     * @param beforeComponentInject
     *            a callback invoked before assigning the element/component to
     *            the field
     */
    public void mapComponentOrElement(Field field, String id, String tag,
            Consumer<Element> beforeComponentInject) {
        injectClientSideElement(tag, id, field, beforeComponentInject);
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

    /**
     * Gets the shadow root for the template.
     * <p>
     * Creates a shadow root if the template does not have one.
     *
     * @return the shadow root for the template
     */
    public ShadowRoot getOrCreateShadowRoot() {
        return getElement().getShadowRoot()
                .orElseGet(() -> getElement().attachShadow());
    }

    private Element getElement() {
        return template.getElement();
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

    /**
     * Resets the mapper to its original state, clearing any registered
     * mappings.
     */
    public void reset() {
        registeredElementIdToInjected.clear();
    }

    /**
     * Checks if the given id has been mapped.
     *
     * @param id
     *            the id to check
     * @return <code>true</code> if the element has been mapped,
     *         <code>false</code> otherwise
     */
    public boolean isMapped(String id) {
        return registeredElementIdToInjected.containsKey(id);
    }

}
