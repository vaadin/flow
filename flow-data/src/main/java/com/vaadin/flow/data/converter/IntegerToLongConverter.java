package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link Integer} to {@link Long} and back.
 */
public class IntegerToLongConverter implements Converter<Integer, Long> {

    @Override
    public Result<Long> convertToModel(Integer value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }
        return Result.ok(value.longValue());
    }

    @Override
    public Integer convertToPresentation(Long value, ValueContext context) {
        if (value == null) {
            return null;
        }
        return value.intValue();
    }
}
