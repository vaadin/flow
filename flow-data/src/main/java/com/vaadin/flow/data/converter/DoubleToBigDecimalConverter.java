package com.vaadin.flow.data.converter;

import java.math.BigDecimal;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link Double} to {@link BigDecimal} and back.
 *
 * @since 1.0
 */
public class DoubleToBigDecimalConverter implements Converter<Double, BigDecimal> {

    @Override
    public Result<BigDecimal> convertToModel(Double value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }

        return Result.ok(BigDecimal.valueOf(value));
    }

    @Override
    public Double convertToPresentation(BigDecimal value, ValueContext context) {
        if (value == null) {
            return null;
        }

        return value.doubleValue();
    }
}
