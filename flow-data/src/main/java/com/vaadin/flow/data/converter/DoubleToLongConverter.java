package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link Double} to {@link Integer} and back.
 *
 * @since 24.5
 */
public class DoubleToLongConverter implements Converter<Double, Long> {

    @Override
    public Result<Long> convertToModel(Double value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }

        return Result.ok(value.longValue());
    }

    @Override
    public Double convertToPresentation(Long value, ValueContext context) {
        if (value == null) {
            return null;
        }

        return value.doubleValue();
    }
}
