/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

/**
 * Internal status to detect whether the compatibility value is explicitely set.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
enum CompatibilityModeStatus {

    EXPLICITLY_SET_FALSE, // explicitly set to false
    UNDEFINED; // no explicit value
}
