package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

import java.math.BigDecimal;

/**
 * A converter that converts from {@link BigDecimal} to {@link Long} and back.
 */
public class BigDecimalToLongConverter implements Converter<BigDecimal, Long> {

    @Override
    public Result<Long> convertToModel(BigDecimal value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }
        return Result.ok(value.longValue());
    }

    @Override
    public BigDecimal convertToPresentation(Long value, ValueContext context) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value);
    }
}
