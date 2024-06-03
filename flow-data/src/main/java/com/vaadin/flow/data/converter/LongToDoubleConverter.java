package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link Long} to {@link Double} and back.
 */
public class LongToDoubleConverter implements Converter<Long, Double> {

    @Override
    public Result<Double> convertToModel(Long value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }
        return Result.ok(value.doubleValue());
    }

    @Override
    public Long convertToPresentation(Double value, ValueContext context) {
        if (value == null) {
            return null;
        }
        return value.longValue();
    }
}
