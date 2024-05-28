package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link Double} to {@link Integer} and back.
 *
 * @since 1.0
 */
public class DoubleToIntegerConverter implements Converter<Double, Integer> {

    @Override
    public Result<Integer> convertToModel(Double value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }

        return Result.ok(value.intValue());
    }

    @Override
    public Double convertToPresentation(Integer value, ValueContext context) {
        if (value == null) {
            return null;
        }

        return value.doubleValue();
    }
}
