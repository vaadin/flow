/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.converter;

import java.math.BigDecimal;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link BigDecimal} to {@link Integer} and
 * back.
 *
 * @since 24.5
 */
public class BigDecimalToIntegerConverter
        implements Converter<BigDecimal, Integer> {

    @Override
    public Result<Integer> convertToModel(BigDecimal value,
            ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }

        return Result.ok(value.intValue());
    }

    @Override
    public BigDecimal convertToPresentation(Integer value,
            ValueContext context) {
        if (value == null) {
            return null;
        }

        return BigDecimal.valueOf(value);
    }
}
