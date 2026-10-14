/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.webpush;

import java.io.Serializable;

/**
 * Callback for receiving web push client-side state boolean.
 *
 * @since 24.2
 */
@FunctionalInterface
public interface WebPushState extends Serializable {

    /**
     * Invoked when the client-side details are available.
     *
     * @param state
     *            boolean for requested state
     */
    void state(boolean state);
}
