package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link Integer} to {@link Float} and back.
 *
 * @since 24.5
 */
public class IntegerToFloatConverter implements Converter<Integer, Float> {

    @Override
    public Result<Float> convertToModel(Integer value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }
        return Result.ok(value.floatValue());
    }

    @Override
    public Integer convertToPresentation(Float value, ValueContext context) {
        if (value == null) {
            return null;
        }
        return value.intValue();
    }
}