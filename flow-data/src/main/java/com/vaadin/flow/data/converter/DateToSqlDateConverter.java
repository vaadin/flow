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
 * Converter for handling conversion between {@link java.util.Date} and
 * {@link java.sql.Date}. This is used when a PopupDateField or InlineDateField
 * is connected to a java.sql.Date property. Note that information (time
 * information) is lost when converting from {@link java.util.Date} to
 * {@link java.sql.Date}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DateToSqlDateConverter implements Converter<Date, java.sql.Date> {

    @Override
    public Result<java.sql.Date> convertToModel(Date value,
            ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }

        return Result.ok(new java.sql.Date(value.getTime()));
    }

    @Override
    public Date convertToPresentation(java.sql.Date value,
            ValueContext context) {
        if (value == null) {
            return null;
        }

        return new Date(value.getTime());
    }

}
