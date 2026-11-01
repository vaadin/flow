/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
    LONG_POLLING("long-polling"),
    /**
     * Server-sent events for server-to-client messages, XHR for
     * client-to-server messages.
     * <p>
     * The connection is unidirectional at the transport level: the browser's
     * {@code EventSource} API streams messages from the server, while
     * client-to-server messages are sent as separate XHR requests.
     * <p>
     * This is an experimental transport and using it requires the
     * {@code com.vaadin.experimental.ssePushTransport} feature flag to be
     * enabled. Selecting it while the feature flag is disabled throws a
     * {@code DisabledFeatureException}.
     * 
     * @since 25.3
     */
    SERVER_SENT_EVENTS("sse"),
    /**
     * Server-sent events for server-to-client messages, XHR for
     * client-to-server messages, implemented directly in Flow without the
     * Atmosphere framework.
     * <p>
     * Unlike {@link #SERVER_SENT_EVENTS}, this transport does not need the
     * {@code flow-push} module or Atmosphere on the client or the server.
     * <p>
     * Over HTTP/1.1, each open event stream takes one of the few connections
     * (six in current browsers) a browser opens to the same server, so the
     * application stops responding in a browser that has more tabs open to it.
     * Over HTTP/2 and HTTP/3, which browsers use with servers and reverse
     * proxies that support them, the streams share one connection and there is
     * no such limit.
     */
    SSE_PURE("sse-pure");

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
