package com.vaadin.flow.data.converter;

import java.math.BigDecimal;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link BigDecimal} to {@link Double} and back.
 *
 * @since 24.5
 */
public class BigDecimalToDoubleConverter implements Converter<BigDecimal, Double> {

    @Override
    public Result<Double> convertToModel(BigDecimal value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }

        return Result.ok(value.doubleValue());
    }

    @Override
    public BigDecimal convertToPresentation(Double value, ValueContext context) {
        if (value == null) {
            return null;
        }

        return BigDecimal.valueOf(value);
    }
}

