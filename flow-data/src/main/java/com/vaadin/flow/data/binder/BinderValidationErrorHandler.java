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

import com.vaadin.flow.component.HasValue;

/**
 * Handler for applying effects to {@link HasValue} components in {@link Binder}
 * based on {@link ValidationResult} for the user input. Use
 * {@link Binder#setValidationErrorHandler(BinderValidationErrorHandler)} to
 * register an instance of this class to be able to customize validation error
 * handling.
 *
 *
 * @see DefaultBinderValidationErrorHandler
 * @see Binder#setValidationErrorHandler(BinderValidationErrorHandler)
 * @author Vaadin Ltd
 * @since
 *
 */
public interface BinderValidationErrorHandler extends Serializable {

    /**
     * Handles a validation error emitted when trying to write the value of the
     * given field.
     * <p>
     * See #clearError for clearing the error.
     *
     * @see #clearError(HasValue)
     * @param field
     *            the field with the invalid value
     * @param result
     *            the validation error result
     */
    void handleError(HasValue<?, ?> field, ValidationResult result);

    /**
     * Clears the error condition of the given field, if one has been previously
     * set with {@link #handleError}.
     *
     * @see #handleError(HasValue, ValidationResult)
     * @param field
     *            the field to clear the previous error from
     */
    void clearError(HasValue<?, ?> field);
}
