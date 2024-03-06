/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import com.vaadin.flow.function.SerializableFunction;

/**
 * {@link ItemLabelGenerator} can be used to customize the string shown to the
 * user for an item.
 *
 * @param <T>
 *            item type
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface ItemLabelGenerator<T> extends SerializableFunction<T, String> {

    /**
     * Gets a caption for the {@code item}.
     *
     * @param item
     *            the item to get caption for
     * @return the caption of the item, not {@code null}
     */
    @Override
    String apply(T item);
}
