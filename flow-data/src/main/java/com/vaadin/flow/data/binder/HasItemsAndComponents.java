/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

/**
 * Represents a component that display a collection of items and can have
 * additional components between the items.
 * <p>
 * <em>Note:</em> this interface is gradually replaced by
 * {@link HasItemComponents} in components, so as to replace {@link HasItems}
 * with {@link com.vaadin.flow.data.provider.HasListDataView},
 * {@link com.vaadin.flow.data.provider.HasLazyDataView} or
 * {@link com.vaadin.flow.data.provider.HasDataView}.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            the type of the displayed items
 */
public interface HasItemsAndComponents<T>
        extends HasItemComponents<T>, HasItems<T> {
}
