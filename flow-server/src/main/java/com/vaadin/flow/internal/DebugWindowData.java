/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.io.Serializable;

/**
 * Defines data that can be converted to a JSON message and sent to the debug
 * window.
 */
@FunctionalInterface
public interface DebugWindowData extends Serializable {
    /**
     * Converts data object to a JSON string.
     *
     * @return JSON representation if the debug window data.
     */
    String toJson();
}
