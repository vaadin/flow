/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.converter;

import java.text.NumberFormat;
import java.util.Locale;

import com.vaadin.flow.data.binder.ErrorMessageProvider;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link String} to {@link Double} and back.
 * Uses the given locale and a {@link NumberFormat} instance for formatting and
 * parsing.
 * <p>
 * Leading and trailing white spaces are ignored when converting from a String.
 * </p>
 * <p>
 * Override and overwrite {@link #getFormat(Locale)} to use a different format.
 * </p>
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class StringToDoubleConverter
        extends AbstractStringToNumberConverter<Double> {

    /**
     * Creates a new converter instance with the given error message. Empty
     * strings are converted to <code>null</code>.
     *
     * @param errorMessage
     *            the error message to use if conversion fails
     */
    public StringToDoubleConverter(String errorMessage) {
        this(null, errorMessage);
    }

    /**
     * Creates a new converter instance with the given presentation value for
     * empty string and error message.
     *
     * @param emptyValue
     *            the presentation value to return when converting an empty
     *            string, may be <code>null</code>
     * @param errorMessage
     *            the error message to use if conversion fails
     */
    public StringToDoubleConverter(Double emptyValue, String errorMessage) {
        super(emptyValue, errorMessage);
    }

    /**
     * Creates a new converter instance with the given error message provider.
     * Empty strings are converted to <code>null</code>.
     *
     * @param errorMessageProvider
     *            the error message provider to use if conversion fails
     */
    public StringToDoubleConverter(ErrorMessageProvider errorMessageProvider) {
        this(null, errorMessageProvider);
    }

    /**
     * Creates a new converter instance with the given presentation value for
     * empty string and error message provider.
     *
     * @param emptyValue
     *            the presentation value to return when converting an empty
     *            string, may be <code>null</code>
     * @param errorMessageProvider
     *            the error message provider to use if conversion fails
     */
    public StringToDoubleConverter(Double emptyValue,
            ErrorMessageProvider errorMessageProvider) {
        super(emptyValue, errorMessageProvider);
    }

    @Override
    public Result<Double> convertToModel(String value, ValueContext context) {
        Result<Number> n = convertToNumber(value, context);

        return n.map(number -> {
            if (number == null) {
                return null;
            } else {
                return number.doubleValue();
            }
        });
    }

}
