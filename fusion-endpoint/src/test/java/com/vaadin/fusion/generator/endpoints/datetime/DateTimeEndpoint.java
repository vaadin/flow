/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.datetime;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.vaadin.fusion.Endpoint;

@Endpoint
public class DateTimeEndpoint {
    public Instant echoInstant(Instant instant) {
        return instant;
    }

    public Date echoDate(Date date) {
        return date;
    }

    public LocalDate echoLocalDate(LocalDate localDate) {
        return localDate;
    }

    public LocalDateTime echoLocalDateTime(LocalDateTime localDateTime) {
        return localDateTime;
    }

    public LocalTime echoLocalTime(LocalTime localTime) {
        return localTime;
    }

    public List<LocalDateTime> echoListLocalDateTime(
            List<LocalDateTime> localDateTimeList) {
        return localDateTimeList;
    }

    public Map<String, Instant> echoMapInstant(
            Map<String, Instant> mapInstant) {
        return mapInstant;
    }
}
