/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.dom.Element;

/**
 * A component to which the user can add and remove child components.
 * {@link Component} in itself provides basic support for child components that
 * are manually added as children of an element belonging to the component. This
 * interface provides an explicit API for components that explicitly supports
 * adding and removing arbitrary child components.
 * <p>
 * {@link HasComponents} is generally implemented by layouts or components whose
 * primary function is to host child components. It isn't for example
 * implemented by non-layout components such as fields.
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
     * In case any of the specified components has already been added to another
     * parent, it will be removed from there and added to this one.
     *
     * @param components
     *            the components to add
     */
    default void add(Component... components) {
        Objects.requireNonNull(components, "Components should not be null");
        add(Arrays.asList(components));
    }

    /**
     * Adds the given components as children of this component.
     * <p>
     * In case any of the specified components has already been added to another
     * parent, it will be removed from there and added to this one.
     *
     * @param components
     *            the components to add
     */
    default void add(Collection<Component> components) {
        Objects.requireNonNull(components, "Components should not be null");
        components.stream()
                .map(component -> Objects.requireNonNull(component,
                        "Component to add cannot be null"))
                .map(Component::getElement).forEach(getElement()::appendChild);
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
     *             if there is a component whose non {@code null} parent is not
     *             this component
     */
    default void remove(Component... components) {
        Objects.requireNonNull(components, "Components should not be null");
        remove(Arrays.asList(components));
    }

    /**
     * Removes the given child components from this component.
     *
     * @param components
     *            the components to remove
     * @throws IllegalArgumentException
     *             if there is a component whose non {@code null} parent is not
     *             this component
     */
    default void remove(Collection<Component> components) {
        Objects.requireNonNull(components, "Components should not be null");
        List<Component> toRemove = new ArrayList<>(components.size());
        for (Component component : components) {
            Objects.requireNonNull(component,
                    "Component to remove cannot be null");
            Element parent = component.getElement().getParent();
            if (parent == null) {
                LoggerFactory.getLogger(HasComponents.class).debug(
                        "Remove of a component with no parent does nothing.");
                continue;
            }
            if (getElement().equals(parent)) {
                toRemove.add(component);
            } else {
                throw new IllegalArgumentException("The given component ("
                        + component + ") is not a child of this component");
            }
        }
        toRemove.stream().map(Component::getElement)
                .forEach(getElement()::removeChild);
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

    /**
     * Replaces the component in the container with another one without changing
     * position. This method replaces component with another one is such way
     * that the new component overtakes the position of the old component. If
     * the old component is not in the container, the new component is added to
     * the container. If the both component are already in the container, their
     * positions are swapped. Component attach and detach events should be taken
     * care as with add and remove.
     *
     * @param oldComponent
     *            the old component that will be replaced. Can be
     *            <code>null</code>, which will make the newComponent to be
     *            added to the layout without replacing any other
     *
     * @param newComponent
     *            the new component to be replaced. Can be <code>null</code>,
     *            which will make the oldComponent to be removed from the layout
     *            without adding any other
     */
    default void replace(Component oldComponent, Component newComponent) {
        if (oldComponent == null && newComponent == null) {
            // NO-OP
            return;
        }
        if (oldComponent == null) {
            add(newComponent);
        } else if (newComponent == null) {
            remove(oldComponent);
        } else {
            Element element = getElement();
            int oldIndex = element.indexOfChild(oldComponent.getElement());
            int newIndex = element.indexOfChild(newComponent.getElement());
            if (oldIndex >= 0 && newIndex >= 0) {
                element.insertChild(oldIndex, newComponent.getElement());
                element.insertChild(newIndex, oldComponent.getElement());
            } else if (oldIndex >= 0) {
                element.setChild(oldIndex, newComponent.getElement());
            } else {
                add(newComponent);
            }
        }
    }

    /**
     * Returns the index of the given component.
     *
     * @param component
     *            the component to look up, can not be <code>null</code>
     * @return the index of the component or -1 if the component is not a child
     */
    default int indexOf(Component component) {
        if (component == null) {
            throw new IllegalArgumentException(
                    "The 'component' parameter cannot be null");
        }
        Iterator<Component> it = getChildren().sequential().iterator();
        int index = 0;
        while (it.hasNext()) {
            Component next = it.next();
            if (component.equals(next)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Gets the number of children components.
     *
     * @return the number of components
     */
    default int getComponentCount() {
        return (int) getChildren().count();
    }

    /**
     * Returns the component at the given position.
     *
     * @param index
     *            the position of the component, must be greater than or equals
     *            to 0 and less than the number of children components
     * @return The component at the given index
     * @throws IllegalArgumentException
     *             if the index is less than 0 or greater than or equals to the
     *             number of children components
     * @see #getComponentCount()
     */
    default Component getComponentAt(int index) {
        if (index < 0) {
            throw new IllegalArgumentException(
                    "The 'index' argument should be greater than or equal to 0. It was: "
                            + index);
        }
        return getChildren().sequential().skip(index).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "The 'index' argument should not be greater than or equals to the number of children components. It was: "
                                + index));
    }

    /**
     * Gets the children components of this component.
     *
     * @see Component#getChildren()
     *
     * @return the children components of this component
     */
    default Stream<Component> getChildren() {
        if (this instanceof Component parent) {
            return ComponentUtil.getChildren(parent);
        } else {
            throw new UnsupportedOperationException(
                    "getChildren is not supported for non-Component HasComponents");
        }
    }
}
