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
package com.vaadin.flow.component;

import java.util.Objects;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.dom.Element;

/**
 * A component to which the user can add and remove child components.
 * {@link Component} in itself provides basic support for child components that
 * are manually added as children of an element belonging to the component. This
 * interface provides an explicit API for components that explicitly supports
 * adding and removing arbitrary child components.
 * <p>
 * The default implementations assume that children are attached to
 * {@link #getElement()}. Override all methods in this interface if the
 * components should be added to some other element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface HasComponents extends HasElement, HasEnabled {
    /**
     * Adds the given components as children of this component.
     * <p>
     * In case the any of the specified components has already been added to
     * another parent, it will be removed from there and added to this one.
     *
     * @param components
     *            the components to add
     */
    default void add(Component... components) {
        Objects.requireNonNull(components, "Components should not be null");
        for (Component component : components) {
            Objects.requireNonNull(component,
                    "Component to add cannot be null");
            getElement().appendChild(component.getElement());
        }
    }

    /**
     * Add the given text as a child of this component.
     *
     * @param text
     *            the text to add, not <code>null</code>
     */
    default void add(String text) {
        add(new Text(text));
    }

    /**
     * Removes the given child components from this component.
     *
     * @param components
     *            the components to remove
     * @throws IllegalArgumentException
     *             if any of the components is not a child of this component
     */
    default void remove(Component... components) {
        Objects.requireNonNull(components, "Components should not be null");
        for (Component component : components) {
            Objects.requireNonNull(component,
                    "Component to remove cannot be null");
            Element parent = component.getElement().getParent();
            if (parent == null) {
                LoggerFactory.getLogger(HasComponents.class).debug(
                        "Remove of a component with no parent does nothing.");
                return;
            }
            if (getElement().equals(parent)) {
                getElement().removeChild(component.getElement());
            } else {
                throw new IllegalArgumentException("The given component ("
                        + component + ") is not a child of this component");
            }
        }
    }

    /**
     * Removes all contents from this component, this includes child components,
     * text content as well as child elements that have been added directly to
     * this component using the {@link Element} API. it also removes the
     * children that were added only at the client-side.
     */
    default void removeAll() {
        getElement().removeAllChildren();
    }

    /**
     * Adds the given component as child of this component at the specific
     * index.
     * <p>
     * In case the specified component has already been added to another parent,
     * it will be removed from there and added to this one.
     *
     * @param index
     *            the index, where the component will be added. The index must
     *            be non-negative and may not exceed the children count
     * @param component
     *            the component to add, value should not be null
     */
    default void addComponentAtIndex(int index, Component component) {
        Objects.requireNonNull(component, "Component should not be null");
        if (index < 0) {
            throw new IllegalArgumentException(
                    "Cannot add a component with a negative index");
        }
        // The case when the index is bigger than the children count is handled
        // inside the method below
        getElement().insertChild(index, component.getElement());
    }

    /**
     * Adds the given component as the first child of this component.
     * <p>
     * In case the specified component has already been added to another parent,
     * it will be removed from there and added to this one.
     *
     * @param component
     *            the component to add, value should not be null
     */
    default void addComponentAsFirst(Component component) {
        addComponentAtIndex(0, component);
    }
}
