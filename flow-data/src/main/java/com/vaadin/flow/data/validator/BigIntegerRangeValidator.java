/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.validator;

import java.math.BigInteger;
import java.util.Comparator;

/**
 * Validator for validating that an {@link BigInteger} is inside a given range.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
@SuppressWarnings("serial")
public class BigIntegerRangeValidator extends RangeValidator<BigInteger> {

    /**
     * Creates a validator for checking that an BigInteger is within a given
     * range.
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
    public BigIntegerRangeValidator(String errorMessage, BigInteger minValue,
            BigInteger maxValue) {
        super(errorMessage, Comparator.naturalOrder(), minValue, maxValue);
    }

}
