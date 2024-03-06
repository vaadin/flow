/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import com.vaadin.flow.component.HasValue.ValueChangeEvent;

/**
 * A component that has a value.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <E>
 *            the value change event type
 * @param <V>
 *            the value type
 */
public interface HasValueAndElement<E extends ValueChangeEvent<V>, V>
        extends HasValue<E, V>, HasElement, HasEnabled {

    @Override
    default void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
        getElement().setProperty("required", requiredIndicatorVisible);
    }

    @Override
    default boolean isRequiredIndicatorVisible() {
        return getElement().getProperty("required", false);
    }

    @Override
    default void setReadOnly(boolean readOnly) {
        getElement().setProperty("readonly", readOnly);
    }

    @Override
    default boolean isReadOnly() {
        return getElement().getProperty("readonly", false);
    }
}
