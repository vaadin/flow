/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

/**
 * Represents the error levels displayed on components.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public enum ErrorLevel {

    /**
     * Error level for informational messages.
     */
    INFO,

    /**
     * Error level for warning messages.
     */
    WARNING,

    /**
     * Error level for regular messages.
     */
    ERROR,

    /**
     * Error level for critical messages.
     */
    CRITICAL,

    /**
     * Error level for system errors and bugs.
     */
    SYSTEM;

    /**
     * Integer representation of error severity for comparison.
     *
     * @return integer for error severity
     */
    public int intValue() {
        return ordinal();
    }
}
