package com.vaadin.hummingbird.namespace;

import java.util.Collection;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.PushConfiguration;

/**
 * Map for storing the push configuration for a UI.
 *
 * @author Vaadin
 * @since
 */
public class PushConfigurationMap extends MapNamespace
        implements PushConfiguration {
    // Implements PushConfiguration to get javadocs...

    /**
     * Map for storing push parameters.
     *
     * @author Vaadin
     * @since
     */
    public static class PushConfigurationParametersMap extends MapNamespace {

        /**
         * Creates a new namespace for the given node.
         *
         * @param node
         *            the node that the namespace belongs to
         */
        public PushConfigurationParametersMap(StateNode node) {
            super(node);
        }

    }

    public static final String TRANSPORT_KEY = "transport";
    public static final String FALLBACK_TRANSPORT_KEY = "fallbackTransport";
    public static final String PUSHMODE_KEY = "pushMode";
    public static final String ALWAYS_USE_XHR_TO_SERVER = "alwaysXhrToServer";
    public static final String PUSH_URL_KEY = "pushUrl";
    public static final String PARAMETERS_KEY = "parameters";

    /**
     * Creates a new namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
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

    private MapNamespace getParameters() {
        if (!contains(PARAMETERS_KEY)) {
            put(PARAMETERS_KEY,
                    new StateNode(PushConfigurationParametersMap.class));
        }

        return ((StateNode) get(PARAMETERS_KEY))
                .getNamespace(PushConfigurationParametersMap.class);
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

        Transport tr = Transport
                .getByIdentifier(getParameters().get(TRANSPORT_KEY).toString());
        if (tr == Transport.WEBSOCKET && contains(ALWAYS_USE_XHR_TO_SERVER)) {
            return Transport.WEBSOCKET_XHR;
        } else {
            return tr;
        }
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
    public void setPushUrl(String pushUrl) {
        setParameter(PUSH_URL_KEY, pushUrl);
    }

    @Override
    public String getPushUrl() {
        return getParameter(PUSH_URL_KEY);
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
}
