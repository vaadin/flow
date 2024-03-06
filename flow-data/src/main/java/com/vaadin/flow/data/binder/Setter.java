/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import java.io.Serializable;
import java.util.function.BiConsumer;

import com.vaadin.flow.component.HasValue;

/**
 * The function to write the field value to the bean property
 *
 * @see BiConsumer
 * @see Binder#bind(HasValue, ValueProvider, Setter)
 * @param <BEAN>
 *            the type of the target bean
 * @param <FIELDVALUE>
 *            the field value type to be written to the bean
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@FunctionalInterface
public interface Setter<BEAN, FIELDVALUE>
        extends BiConsumer<BEAN, FIELDVALUE>, Serializable {

    /**
     * Save value to the bean property.
     *
     * @param bean
     *            the target bean
     * @param fieldvalue
     *            the field value to be written to the bean
     */
    @Override
    void accept(BEAN bean, FIELDVALUE fieldvalue);
}
