/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

/**
 * The exception is thrown when a property change from client is disallowed on
 * the server side.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 *
 */
public class PropertyChangeDeniedException extends Exception {

    public PropertyChangeDeniedException(String message) {
        super(message);
    }

}
