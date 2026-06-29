/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts between <code>LocalDateTime</code> and
 * <code>Instant</code>.
 *
 * @since 24.1
 */
public class LocalDateTimeToInstantConverter
        implements Converter<LocalDateTime, Instant> {
    private ZoneId zoneId;

    /**
     * Creates a new converter using the given time zone.
     *
     * @param zoneId
     *            the time zone to use, not <code>null</code>
     */
    public LocalDateTimeToInstantConverter(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId,
                "Zone identifier cannot be null");
    }

    @Override
    public Result<Instant> convertToModel(LocalDateTime localDateTime,
            ValueContext context) {
        if (localDateTime == null) {
            return Result.ok(null);
        }

        return Result.ok(localDateTime.atZone(zoneId).toInstant());
    }

    @Override
    public LocalDateTime convertToPresentation(Instant instant,
            ValueContext context) {
        if (instant == null) {
            return null;
        }

        return instant.atZone(zoneId).toLocalDateTime();
    }

}
