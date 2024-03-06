/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.validator;

import java.util.Comparator;

/**
 * Validator for validating that a {@link Double} is inside a given range.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
@SuppressWarnings("serial")
public class DoubleRangeValidator extends RangeValidator<Double> {

    /**
     * Creates a validator for checking that an Double is within a given range.
     *
     * By default the range is inclusive i.e. both minValue and maxValue are
     * valid values. Use {@link #setMinValueIncluded(boolean)} or
     * {@link #setMaxValueIncluded(boolean)} to change it.
     *
     *
     * @param errorMessage
     *            the message to display in case the value does not validate.
     * @param minValue
     *            The minimum value to accept or null for no limit
     * @param maxValue
     *            The maximum value to accept or null for no limit
     */
    public DoubleRangeValidator(String errorMessage, Double minValue,
            Double maxValue) {
        super(errorMessage, Comparator.naturalOrder(), minValue, maxValue);
    }

}
