/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

import java.math.BigDecimal;

/**
 * A converter that converts from {@link Long} to {@link BigDecimal} and back.
 *
 * @since 24.5
 */
public class LongToBigDecimalConverter implements Converter<Long, BigDecimal> {

    @Override
    public Result<BigDecimal> convertToModel(Long value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }
        return Result.ok(BigDecimal.valueOf(value));
    }

    @Override
    public Long convertToPresentation(BigDecimal value, ValueContext context) {
        if (value == null) {
            return null;
        }
        return value.longValue();
    }
}
