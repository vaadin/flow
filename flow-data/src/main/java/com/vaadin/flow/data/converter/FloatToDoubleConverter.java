/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

import java.math.BigDecimal;

/**
 * A converter that converts from {@link Float} to {@link Double} and back.
 *
 * @since 24.5
 */
public class FloatToDoubleConverter implements Converter<Float, Double> {

    @Override
    public Result<Double> convertToModel(Float value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }
        return Result.ok(value.doubleValue());
    }

    @Override
    public Float convertToPresentation(Double value, ValueContext context) {
        if (value == null) {
            return null;
        }
        return value.floatValue();
    }
}
