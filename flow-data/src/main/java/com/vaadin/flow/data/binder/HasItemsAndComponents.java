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
package com.vaadin.flow.data.binder;

import java.util.Optional;
import java.util.stream.IntStream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasElement;

/**
 * Represents a component that display a collection of items and can have
 * additional components between the items.
 * <p>
 * The items should be represented by components that implement
 * {@link ItemComponent}. Additionally any type of components can be added at
 * any position with {@link #addComponents(Object, Component...)} or
 * {@link #prependComponents(Object, Component...)}.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            the type of the displayed items
 */
public interface HasItemsAndComponents<T> extends HasComponents, HasItems<T> {

    /**
     * Represents a single item component that is used inside an
     * {@link HasItemsAndComponents}.
     *
     * @author Vaadin Ltd
     * @since 1.0.
     *
     * @param <T>
     *            the type of the displayed item
     */
    public interface ItemComponent<T> extends HasElement {
        T getItem();
    }

    /**
     * Adds the components after the given item.
     *
     * @param afterItem
     *            item to add components after
     * @param components
     *            components to add after item
     * @throws IllegalArgumentException
     *             if this component doesn't contain the item
     */
    default void addComponents(T afterItem, Component... components) {
        int insertPosition = getItemPosition(afterItem);
        if (insertPosition == -1) {
            throw new IllegalArgumentException(
                    "Could not locate the item after which to insert components.");
        }
        for (Component component : components) {
            insertPosition++;
            getElement().insertChild(insertPosition, component.getElement());
        }
    }

    /**
     * Adds the components before the given item.
     *
     * @param beforeItem
     *            item to add components in front of
     * @param components
     *            components to add before item
     * @throws IllegalArgumentException
     *             if this component doesn't contain the item
     */
    default void prependComponents(T beforeItem, Component... components) {
        int insertPosition = getItemPosition(beforeItem);
        if (insertPosition == -1) {
            throw new IllegalArgumentException(
                    "Could not locate the item before which to insert components.");
        }
        for (Component component : components) {
            getElement().insertChild(insertPosition, component.getElement());
            insertPosition++;
        }
    }

    /**
     * Gets the index of the child element that represents the given item.
     *
     * @param item
     *            the item to look for
     * @return the index of the child element that represents the item, or -1 if
     *         the item is not found
     */
    default int getItemPosition(T item) {
        if (item == null) {
            return -1;
        }
        return IntStream.range(0, getElement().getChildCount()).filter(i -> {
            Optional<Component> c = getElement().getChild(i).getComponent();
            return c.isPresent() && c.get() instanceof ItemComponent
                    && item.equals(((ItemComponent<?>) c.get()).getItem());
        }).findFirst().orElse(-1);
    }
}
