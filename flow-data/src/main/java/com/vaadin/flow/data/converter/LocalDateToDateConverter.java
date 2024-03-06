/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.converter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts between <code>LocalDate</code> and
 * <code>Date</code>.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class LocalDateToDateConverter implements Converter<LocalDate, Date> {

    private ZoneId zoneId;

    /**
     * Creates a new converter using the given time zone.
     *
     * @param zoneId
     *            the time zone id to use, not <code>null</code>
     */
    public LocalDateToDateConverter(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "Zone id cannot be null");
    }

    /**
     * Creates a new converter using the system's default time zone.
     *
     * @see ZoneId#systemDefault()
     */
    public LocalDateToDateConverter() {
        this(ZoneId.systemDefault());
    }

    @Override
    public Result<Date> convertToModel(LocalDate localDate,
            ValueContext context) {
        if (localDate == null) {
            return Result.ok(null);
        }

        return Result.ok(Date.from(localDate.atStartOfDay(zoneId).toInstant()));
    }

    @Override
    public LocalDate convertToPresentation(Date date, ValueContext context) {
        if (date == null) {
            return null;
        }

        return Instant.ofEpochMilli(date.getTime()).atZone(zoneId)
                .toLocalDate();
    }

}
