package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link Float} to {@link Integer} and back.
 */
public class FloatToIntegerConverter implements Converter<Float, Integer> {

    @Override
    public Result<Integer> convertToModel(Float value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }
        return Result.ok(value.intValue());
    }

    @Override
    public Float convertToPresentation(Integer value, ValueContext context) {
        if (value == null) {
            return null;
        }
        return value.floatValue();
    }
}
