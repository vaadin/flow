/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.converter;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link Date} to {@link String} and back. Uses
 * the given locale and {@link DateFormat} for formatting and parsing.
 * <p>
 * Leading and trailing white spaces are ignored when converting from a String.
 * </p>
 * <p>
 * Override and overwrite {@link #getFormat(Locale)} to use a different format.
 * </p>
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class StringToDateConverter implements Converter<String, Date> {

    /**
     * Returns the format used by
     * {@link #convertToPresentation(Date, ValueContext)} and
     * {@link #convertToModel(String, ValueContext)}.
     *
     * @param locale
     *            The locale to use
     * @return A DateFormat instance
     */
    protected DateFormat getFormat(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }

        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                DateFormat.MEDIUM, locale);
        format.setLenient(false);
        return format;
    }

    @Override
    public Result<Date> convertToModel(String value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }

        // Remove leading and trailing white space
        value = value.trim();

        ParsePosition parsePosition = new ParsePosition(0);
        Date parsedValue = getFormat(context.getLocale().orElse(null))
                .parse(value, parsePosition);
        if (parsePosition.getIndex() != value.length()) {
            return Result.error("Could not convert '" + value);
        }

        return Result.ok(parsedValue);
    }

    @Override
    public String convertToPresentation(Date value, ValueContext context) {
        if (value == null) {
            return null;
        }

        return getFormat(context.getLocale().orElse(null)).format(value);
    }

}
