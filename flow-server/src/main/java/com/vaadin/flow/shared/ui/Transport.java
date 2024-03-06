/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.shared.ui;

/**
 * Transport modes for Push
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public enum Transport {
    /**
     * Websocket.
     */
    WEBSOCKET("websocket"),
    /**
     * Websocket for server to client, XHR for client to server.
     *
     */
    WEBSOCKET_XHR("websocket-xhr"),
    /**
     * HTTP long polling.
     */
    LONG_POLLING("long-polling");

    private String identifier;

    private Transport(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Gets the low level identifier for the transport.
     *
     * @return the low level transport identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns a Transport by its identifier. Returns null if no value is found
     * for the given identifier.
     *
     * @param identifier
     *            the transport identifier, as returned by
     *            {@link #getIdentifier()}
     * @return the transport identified by the identifier or {@code null} if no
     *         matching transport was found
     */
    public static Transport getByIdentifier(String identifier) {
        for (Transport t : values()) {
            if (t.getIdentifier().equals(identifier)) {
                return t;
            }
        }
        return null;
    }
}
