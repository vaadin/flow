package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

import java.math.BigDecimal;

/**
 * A converter that converts from {@link BigDecimal} to {@link Float} and back.
 */
public class BigDecimalToFloatConverter
        implements Converter<BigDecimal, Float> {

    @Override
    public Result<Float> convertToModel(BigDecimal value,
            ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }
        return Result.ok(value.floatValue());
    }

    @Override
    public BigDecimal convertToPresentation(Float value, ValueContext context) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value);
    }
}
