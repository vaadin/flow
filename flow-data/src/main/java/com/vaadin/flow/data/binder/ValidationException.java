/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Indicates validation errors in a {@link Binder} when a field value is
 * validated.
 *
 * @see Binder#writeBean(Object)
 * @see Binder#writeBeanIfValid(Object)
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class ValidationException extends Exception {

    private final List<BindingValidationStatus<?>> fieldValidationErrors;
    private final List<ValidationResult> beanValidationErrors;

    /**
     * Constructs a new exception with validation {@code errors} list.
     *
     * @param fieldValidationErrors
     *            binding validation errors list
     * @param beanValidationErrors
     *            binder validation errors list
     */
    public ValidationException(
            List<BindingValidationStatus<?>> fieldValidationErrors,
            List<ValidationResult> beanValidationErrors) {
        super("Validation has failed for some fields");
        this.fieldValidationErrors = Collections
                .unmodifiableList(fieldValidationErrors);
        this.beanValidationErrors = Collections
                .unmodifiableList(beanValidationErrors);
    }

    /**
     * Gets both field and bean level validation errors.
     *
     * @return a list of all validation errors
     */
    public List<ValidationResult> getValidationErrors() {
        List<ValidationResult> errors = new ArrayList<>(
                getFieldValidationErrors().stream()
                        .map(s -> s.getResult().get())
                        .collect(Collectors.toList()));
        errors.addAll(getBeanValidationErrors());
        return errors;
    }

    /**
     * Returns a list of the field level validation errors which caused the
     * exception, or an empty list if the exception was caused by
     * {@link #getBeanValidationErrors() bean level validation errors}.
     *
     * @return binding validation errors list
     */
    public List<BindingValidationStatus<?>> getFieldValidationErrors() {
        return fieldValidationErrors;
    }

    /**
     * Returns a list of the bean level validation errors which caused the
     * exception, or an empty list if the exception was caused by
     * {@link #getFieldValidationErrors() field level validation errors}.
     *
     * @return binder validation errors list
     */
    public List<ValidationResult> getBeanValidationErrors() {
        return beanValidationErrors;
    }
}
