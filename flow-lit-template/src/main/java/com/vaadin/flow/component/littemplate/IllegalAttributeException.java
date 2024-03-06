/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.littemplate;

/**
 * Thrown to indicate that an element had an illegal or inappropriate attribute.
 *
 * @since
 */
public class IllegalAttributeException extends RuntimeException {

    /**
     * Constructs an <code>IllegalAttributeException</code> with the specified
     * detail message.
     *
     * @param s
     *            the detail message.
     */
    public IllegalAttributeException(String s) {
        super(s);
    }
}
