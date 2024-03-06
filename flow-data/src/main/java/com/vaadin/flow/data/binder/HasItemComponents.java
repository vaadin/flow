/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import java.util.Optional;
import java.util.stream.IntStream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasElement;

/**
 * Represents a component that can have additional components between the items.
 * <p>
 * The items should be represented by components that implement
 * {@link ItemComponent}. Additionally any type of components can be added at
 * any position with {@link #addComponents(Object, Component...)} or
 * {@link #prependComponents(Object, Component...)}.
 *
 * @author Vaadin Ltd
 * @since
 *
 * @param <T>
 *            the type of the displayed items
 */
public interface HasItemComponents<T> extends HasComponents {

    /**
     * Represents a single item component that is used inside a
     * {@link HasItemComponents}.
     *
     * @author Vaadin Ltd
     * @since 1.0.
     *
     * @param <T>
     *            the type of the displayed item
     */
    interface ItemComponent<T> extends HasElement {
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
