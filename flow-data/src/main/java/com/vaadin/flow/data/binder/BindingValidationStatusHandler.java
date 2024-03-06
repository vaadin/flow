/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.function.SerializableEventListener;

/**
 * Handler for {@link BindingValidationStatus} changes.
 * <p>
 * {@link BindingBuilder#withValidationStatusHandler(BindingValidationStatusHandler)}
 * Register} an instance of this class to be able to override the default
 * handling.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @see BindingBuilder#withValidationStatusHandler(BindingValidationStatusHandler)
 * @see BindingValidationStatus
 *
 *
 */
@FunctionalInterface
public interface BindingValidationStatusHandler
        extends SerializableEventListener {

    /**
     * Invoked when the validation status has changed in a binding.
     *
     * @param statusChange
     *            the changed status
     */
    void statusChange(BindingValidationStatus<?> statusChange);
}
