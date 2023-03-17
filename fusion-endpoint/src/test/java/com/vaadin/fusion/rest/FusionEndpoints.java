/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.rest;

import java.time.LocalTime;
import java.time.ZonedDateTime;

import com.vaadin.fusion.Endpoint;

@Endpoint
public class FusionEndpoints {

    public BeanWithZonedDateTimeField getBeanWithZonedDateTimeField() {
        return new BeanWithZonedDateTimeField();
    }

    public BeanWithPrivateFields getBeanWithPrivateFields() {
        return new BeanWithPrivateFields();
    }

    public BeanWithJacksonAnnotation getBeanWithJacksonAnnotation() {
        return new BeanWithJacksonAnnotation();
    }

    public LocalTime getLocalTime() {
        return LocalTime.of(8, 0, 0);
    }

    public static class BeanWithZonedDateTimeField {
        private ZonedDateTime zonedDateTime = ZonedDateTime.now();

        public ZonedDateTime getZonedDateTime() {
            return zonedDateTime;
        }

        public void setZonedDateTime(ZonedDateTime zonedDateTime) {
            this.zonedDateTime = zonedDateTime;
        }
    }
}
