/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.ui;

import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementUtil;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.event.ComponentEventBus;
import com.vaadin.hummingbird.event.ComponentEventListener;

/**
 * A Component is a higher level abstraction of an {@link Element} or a
 * hierarchy of {@link Element}s.
 * <p>
 * A component must have exactly one root element which is created based on the
 * {@link Tag} annotation of the sub class (or in special cases set using the
 * constructor {@link #Component(Element)} or using
 * {@link #setElement(Component, Element)} before the element is attached to a
 * parent). The root element cannot be changed once it has been set.
 *
 * @author Vaadin Ltd
 */
public abstract class Component implements HasElement, Serializable,
        ComponentEventNotifier, AttachNotifier, DetachNotifier {

    private Element element;

    private ComponentEventBus eventBus = null;

    /**
     * Creates a component instance with an element created based on the
     * {@link Tag} annotation of the sub class.
     */
    protected Component() {
        Tag tag = getClass().getAnnotation(Tag.class);
        if (tag == null) {
            throw new IllegalStateException(getClass().getSimpleName()
                    + " (or a super class) must be annotated with @"
                    + Tag.class.getName()
                    + " if the default constructor is used.");
        }

        String tagName = tag.value();
        if (tagName.isEmpty()) {
            throw new IllegalStateException("@" + Tag.class.getSimpleName()
                    + " value cannot be empty.");
        }

        setElement(this, new Element(tagName));
    }

    /**
     * Creates a component instance based on the given element.
     * <p>
     * For nearly all cases you want to pass an element reference but it is
     * possible to pass {@code null} to this method. If you pass {@code null}
     * you must ensure that the element is initialized using
     * {@link #setElement(Component, Element)} before {@link #getElement()} is
     * used.
     *
     * @param element
     *            the root element for the component
     */
    protected Component(Element element) {
        if (element != null) {
            setElement(this, element);
        }
    }

    /**
     * Gets the root element of this component.
     * <p>
     * Each component must have exactly one root element. When the component is
     * attached to a parent component, this element is attached to the parent
     * component's element hierarchy.
     *
     * @return the root element of this component
     */
    @Override
    public Element getElement() {
        assert element != null : "getElement() must not be called before the element has been set";
        return element;
    }

    /**
     * Initializes the root element of a component.
     * <p>
     * Each component must have a root element and it must be set before the
     * component is attached to a parent. The root element of a component cannot
     * be changed once it has been set.
     * <p>
     * Typically you do not want to call this method but define the element
     * through {@link #Component(Element)} instead.
     *
     * @param element
     *            the root element of the component
     */
    protected static void setElement(Component component, Element element) {
        if (component.element != null) {
            throw new IllegalStateException("Element has already been set");
        }
        if (element == null) {
            throw new IllegalArgumentException("Element must not be null");
        }
        component.element = element;
        ElementUtil.setComponent(element, component);
    }

    /**
     * Gets the parent component of this component.
     * <p>
     * A component can only have one parent.
     *
     * @return an optional parent component, or an empty optional if the
     *         component is not attached to a parent
     */
    public Optional<Component> getParent() {
        assert ElementUtil.isComponentElementMappedCorrectly(this);

        // If "this" is a component inside a Composite, iterate from the
        // Composite downwards
        Optional<Component> mappedComponent = ElementUtil
                .getComponent(getElement());
        if (isInsideComposite(mappedComponent)) {
            Component parent = ComponentUtil.getParentUsingComposite(
                    (Composite<?>) mappedComponent.get(), this);
            return Optional.of(parent);
        }

        // Find the parent component based on the first parent element which is
        // mapped to a component
        return ComponentUtil.findParentComponent(getElement().getParent());
    }

    private boolean isInsideComposite(Optional<Component> mappedComponent) {
        if (!mappedComponent.isPresent()) {
            return false;
        }

        Component component = mappedComponent.get();
        return component instanceof Composite && component != this;
    }

    /**
     * Gets the child components of this component.
     * <p>
     * The default implementation finds child components by traversing each
     * child {@link Element} tree.
     *
     * @return the child components of this component
     */
    public Stream<Component> getChildren() {
        // This should not ever be called for a Composite as it will return
        // wrong results
        assert !(this instanceof Composite);
        assert ElementUtil.isComponentElementMappedCorrectly(this);

        Builder<Component> childComponents = Stream.builder();
        getElement().getChildren().forEach(childElement -> {
            ComponentUtil.findComponents(childElement, component -> {
                childComponents.add(component);
            });
        });
        return childComponents.build();
    }

    /**
     * Gets the event bus for this component.
     * <p>
     * This method will create the event bus if it has not yet been created.
     *
     * @return the event bus for this component
     */
    protected ComponentEventBus getEventBus() {
        if (eventBus == null) {
            eventBus = new ComponentEventBus(this);
        }
        return eventBus;
    }

    @Override
    public <T extends ComponentEvent<?>> EventRegistrationHandle addListener(
            Class<T> eventType, ComponentEventListener<T> listener) {

        return getEventBus().addListener(eventType, listener);
    }

    /**
     * Checks if there is at least one listener registered for the given event
     * type for this component.
     *
     * @param eventType
     *            the component event type
     * @return <code>true</code> if at least one listener is registered,
     *         <code>false</code> otherwise
     */
    @SuppressWarnings("rawtypes")
    protected boolean hasListener(Class<? extends ComponentEvent> eventType) {
        return eventBus != null && eventBus.hasListener(eventType);
    }

    /**
     * Dispatches the event to all listeners registered for the event type.
     *
     * @param componentEvent
     *            the event to fire
     */
    protected void fireEvent(ComponentEvent<?> componentEvent) {
        if (hasListener(componentEvent.getClass())) {
            getEventBus().fireEvent(componentEvent);
        }
    }

    /**
     * Gets the UI this component is attached to.
     *
     * @return an optional UI component, or an empty optional if this component
     *         is not attached to a UI
     */
    public Optional<UI> getUI() {
        return getParent().flatMap(Component::getUI);
    }

    /**
     * Sets the id of the root element of this component. The id is used with
     * various APIs to identify the element, and it should be unique on the
     * page.
     *
     * @param id
     *            the id to set, or <code>null</code> to remove any previously
     *            set id
     */
    public void setId(String id) {
        getElement().setAttribute("id", id);
    }

    /**
     * Gets the id of the root element of this component.
     *
     * @see #setId(String)
     *
     * @return the id, or <code>null</code> if no id has been set
     */
    public String getId() {
        return getElement().getAttribute("id");
    }

    /**
     * Called when the component is attached to a UI.
     * <p>
     * The default implementation does nothing.
     * <p>
     * This method is invoked before the {@link AttachEvent} is fired for the
     * component.
     */
    protected void onAttach() {
        // NOOP by default
    }

    /**
     * Called when the component is detached from a UI.
     * <p>
     * The default implementation does nothing.
     * <p>
     * This method is invoked before the {@link DetachEvent} is fired for the
     * component.
     */
    protected void onDetach() {
        // NOOP by default
    }
}
