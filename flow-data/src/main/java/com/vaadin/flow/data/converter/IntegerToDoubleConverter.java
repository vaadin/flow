package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link Integer} to {@link Double} and back.
 *
 * @since 24.5
 * @author Alaina Faisal
 */

public class IntegerToDoubleConverter implements Converter<Integer, Double> {
    @Override
    public Result<Double> convertToModel(Integer value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }

        return Result.ok(value.doubleValue());
    }

    @Override
    public Integer convertToPresentation(Double value, ValueContext context) {
        if (value == null) {
            return null;
        }

        return value.intValue();
    }
}
