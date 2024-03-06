/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import java.util.Collection;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.communication.PushConnectionFactory;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Map for storing the push configuration for a UI.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class PushConfigurationMap extends NodeMap implements PushConfiguration {
    // Implements PushConfiguration to get javadocs...

    /**
     * Map for storing push parameters.
     *
     * @author Vaadin Ltd
     * @since 1.0
     */
    public static class PushConfigurationParametersMap extends NodeMap {

        /**
         * Creates a new map for the given node.
         *
         * @param node
         *            the node that the map belongs to
         */
        public PushConfigurationParametersMap(StateNode node) {
            super(node);
        }

    }

    public static final String TRANSPORT_KEY = "transport";
    public static final String FALLBACK_TRANSPORT_KEY = "fallbackTransport";
    public static final String PUSH_SERVLET_MAPPING_KEY = "pushServletMapping";
    public static final String PUSHMODE_KEY = "pushMode";
    public static final String ALWAYS_USE_XHR_TO_SERVER = "alwaysXhrToServer";
    public static final String PARAMETERS_KEY = "parameters";

    /**
     * Creates a new map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     */
    public PushConfigurationMap(StateNode node) {
        super(node);
    }

    @Override
    public void setTransport(Transport transport) {
        if (transport == Transport.WEBSOCKET_XHR) {
            getParameters().put(TRANSPORT_KEY,
                    Transport.WEBSOCKET.getIdentifier());
            put(ALWAYS_USE_XHR_TO_SERVER, true);
        } else {
            getParameters().put(TRANSPORT_KEY, transport.getIdentifier());
            remove(ALWAYS_USE_XHR_TO_SERVER);
        }
    }

    private NodeMap getParameters() {
        if (!contains(PARAMETERS_KEY)) {
            put(PARAMETERS_KEY,
                    new StateNode(PushConfigurationParametersMap.class));
        }

        return ((StateNode) get(PARAMETERS_KEY))
                .getFeature(PushConfigurationParametersMap.class);
    }

    @Override
    public Transport getTransport() {
        if (!getParameters().contains(TRANSPORT_KEY)) {
            return null;
        }

        Transport tr = Transport
                .getByIdentifier(getParameters().get(TRANSPORT_KEY).toString());
        if (tr == Transport.WEBSOCKET && contains(ALWAYS_USE_XHR_TO_SERVER)) {
            return Transport.WEBSOCKET_XHR;
        } else {
            return tr;
        }
    }

    @Override
    public void setFallbackTransport(Transport fallbackTransport) {
        if (fallbackTransport == Transport.WEBSOCKET_XHR) {
            throw new IllegalArgumentException(
                    "WEBSOCKET_XHR can only be used as primary transport");
        }
        getParameters().put(FALLBACK_TRANSPORT_KEY,
                fallbackTransport.getIdentifier());
    }

    @Override
    public Transport getFallbackTransport() {
        if (!getParameters().contains(FALLBACK_TRANSPORT_KEY)) {
            return null;
        }

        return Transport.getByIdentifier(
                getParameters().get(FALLBACK_TRANSPORT_KEY).toString());
    }

    @Override
    public void setPushServletMapping(String pushServletMapping) {
        put(PUSH_SERVLET_MAPPING_KEY, pushServletMapping);
    }

    @Override
    public String getPushServletMapping() {
        return getOrDefault(PUSH_SERVLET_MAPPING_KEY, null);
    }

    @Override
    public void setPushMode(PushMode pushMode) {
        put(PUSHMODE_KEY, pushMode.name());
    }

    @Override
    public PushMode getPushMode() {
        return PushMode.valueOf(get(PUSHMODE_KEY).toString());
    }

    @Override
    public String getParameter(String key) {
        return (String) getParameters().get(key);
    }

    @Override
    public void setParameter(String key, String value) {
        getParameters().put(key, value);
    }

    @Override
    public Collection<String> getParameterNames() {
        return getParameters().keySet();
    }

    @Override
    public void setPushConnectionFactory(PushConnectionFactory factory) {
        throw new UnsupportedOperationException(
                "Setting push connection factory is not supported");
    }
}
