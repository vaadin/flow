/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

/**
 * An exception thrown when the given Java value cannot be encoded into JSON.
 *
 * @author Vaadin Ltd
 * @since 24.4
 */
public class JsonEncodingException extends RuntimeException {
    public JsonEncodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
