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

import com.vaadin.hummingbird.dom.Element;

/**
 * A Component is a higher level abstraction of an {@link Element} or a
 * hierarchy of {@link Element}s.
 * <p>
 * A component must have exactly one root element and it is set using the
 * constructor {@link #Component(Element)}. The root element cannot be changed
 * once it has been set.
 *
 * @author Vaadin
 * @since
 */
public abstract class Component implements HasElement, Serializable {

    private Element element;

    /**
     * Creates a component instance based on the given element.
     * <p>
     * For nearly all cases you want to pass an element reference but it is
     * possible to pass {@code null} to this method. If you pass {@code null}
     * you must ensure that {@link #setElement(Element)} is called before
     * {@link #getElement()}.
     *
     * @param element
     *            the root element for the component
     */
    protected Component(Element element) {
        if (element != null) {
            setElement(element);
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
     * Initializes the root element of this component.
     * <p>
     * Each component must have a root element and it must be set before the
     * component is attached to a parent.
     * <p>
     * This method must be called only once. The root element of a component
     * cannot be changed once it has been set.
     * <p>
     * This method should never be overridden but is not marked as final because
     * of dependency injection frameworks.
     *
     * @param element
     *            the root element of the component
     */
    protected void setElement(Element element) {
        if (this.element != null) {
            throw new IllegalStateException("Element has already been set");
        }
        if (element == null) {
            throw new IllegalArgumentException("Element must not be null");
        }
        this.element = element;
        element.attachComponent(this);
    }

    /**
     * Gets the parent component of this component.
     * <p>
     * A component can only have one parent.
     *
     * @return the parent component
     */
    public Optional<Component> getParent() {
        assert ComponentUtil.isAttachedTo(this, getElement());

        Element parentElement = getElement().getParent();
        while (parentElement != null
                && !parentElement.getComponent().isPresent()) {
            parentElement = parentElement.getParent();
        }

        if (parentElement == null) {
            return Optional.empty();
        }

        return parentElement.getComponent();
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
        assert ComponentUtil.isAttachedTo(this, getElement());

        Builder<Component> childComponents = Stream.builder();
        getElement().getChildren().forEach(childElement -> {
            ComponentUtil.findComponents(childElement, component -> {
                childComponents.add(component);
            });
        });
        return childComponents.build();
    }

}
