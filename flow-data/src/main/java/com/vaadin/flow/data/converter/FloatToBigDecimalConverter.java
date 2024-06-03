package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

import java.math.BigDecimal;

/**
 * A converter that converts from {@link Float} to {@link BigDecimal} and back.
 */
public class FloatToBigDecimalConverter
        implements Converter<Float, BigDecimal> {

    @Override
    public Result<BigDecimal> convertToModel(Float value,
            ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }
        return Result.ok(BigDecimal.valueOf(value));
    }

    @Override
    public Float convertToPresentation(BigDecimal value, ValueContext context) {
        if (value == null) {
            return null;
        }
        return value.floatValue();
    }
}
