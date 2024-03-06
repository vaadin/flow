/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import com.vaadin.flow.function.SerializableFunction;

/**
 * Provider interface for generating localizable error messages using
 * {@link ValueContext}.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
@FunctionalInterface
public interface ErrorMessageProvider
        extends SerializableFunction<ValueContext, String> {

    /**
     * Returns a generated error message for given {@code ValueContext}.
     *
     * @param context
     *            the value context
     *
     * @return generated error message
     */
    @Override
    String apply(ValueContext context);
}
