/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * An exception thrown when the given JSON value cannot be decoded into a value
 * of the given Java type.
 *
 * @author Vaadin Ltd
 * @since 24.4
 */
public class JsonDecodingException extends RuntimeException {
    public JsonDecodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
