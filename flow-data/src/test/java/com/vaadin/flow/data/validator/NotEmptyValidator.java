/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.validator;

import java.util.Objects;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;

/**
 * Simple validator to check against {@code null} value and empty {@link String}
 * value.
 * <p>
 * This validator works similar to {@link NotNullValidator} but in addition it
 * also check whether the value is not an empty String.
 * <p>
 * This validator can be suitable for fields that have been marked as required
 * with {@link HasValue#setRequiredIndicatorVisible(boolean)}.
 * <p>
 * Note that
 * {@link BindingBuilder#asRequired(com.vaadin.flow.data.ErrorMessageProvider)}
 * does almost the same thing, but verifies against the value NOT being equal to
 * what {@link HasValue#getEmptyValue()} returns and sets the required indicator
 * visible with {@link HasValue#setRequiredIndicatorVisible(boolean)}.
 *
 * @see HasValue#setRequiredIndicatorVisible(boolean)
 * @see BindingBuilder#asRequired(com.vaadin.flow.data.ErrorMessageProvider)
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class NotEmptyValidator<T> implements Validator<T> {

    private final String message;

    /**
     * @param message
     *            error validation message
     */
    public NotEmptyValidator(String message) {
        this.message = message;
    }

    @Override
    public ValidationResult apply(T value, ValueContext context) {
        if (Objects.isNull(value) || Objects.equals(value, "")) {
            return ValidationResult.error(message);
        } else {
            return ValidationResult.ok();
        }
    }

}
