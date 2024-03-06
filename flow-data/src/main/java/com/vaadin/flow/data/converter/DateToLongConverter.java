/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.converter;

import java.util.Date;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link Long} to {@link Date} and back.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DateToLongConverter implements Converter<Date, Long> {

    @Override
    public Result<Long> convertToModel(Date value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }

        return Result.ok(value.getTime());
    }

    @Override
    public Date convertToPresentation(Long value, ValueContext context) {
        if (value == null) {
            return null;
        }

        return new Date(value);
    }

}
