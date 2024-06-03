package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link Long} to {@link Integer} and back.
 */
public class LongToIntegerConverter implements Converter<Long, Integer> {

    @Override
    public Result<Integer> convertToModel(Long value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }
        return Result.ok(value.intValue());
    }

    @Override
    public Long convertToPresentation(Integer value, ValueContext context) {
        if (value == null) {
            return null;
        }
        return value.longValue();
    }
}
