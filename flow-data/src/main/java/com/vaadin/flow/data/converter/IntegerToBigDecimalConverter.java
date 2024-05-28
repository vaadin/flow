package com.vaadin.flow.data.converter;

import java.math.BigDecimal;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link Integer} to {@link BigDecimal} and back.
 *
 * @since 1.0
 */
public class IntegerToBigDecimalConverter implements Converter<Integer, BigDecimal> {

    @Override
    public Result<BigDecimal> convertToModel(Integer value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }

        return Result.ok(BigDecimal.valueOf(value));
    }

    @Override
    public Integer convertToPresentation(BigDecimal value, ValueContext context) {
        if (value == null) {
            return null;
        }

        return value.intValue();
    }
}
