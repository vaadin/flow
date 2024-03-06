/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.communication;

import com.vaadin.client.Registry;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap;

/**
 * Tracks the reconnect configuration stored in the root node and provides it
 * with an easier to use API.
 * <p>
 * Also triggers {@link ConnectionStateHandler#configurationUpdated()} whenever
 * part of the configuration changes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ReconnectConfiguration {

    private final Registry registry;

    /**
     * Creates a new instance using the given registry.
     *
     * @param registry
     *            the registry
     */
    public ReconnectConfiguration(Registry registry) {
        this.registry = registry;
    }

    /**
     * Binds this ReconnectDialogConfiguration to the given
     * {@link ConnectionStateHandler} so that
     * {@link ConnectionStateHandler#configurationUpdated()} is run whenever a
     * relevant part of {@link ReconnectConfiguration} changes.
     *
     * @param connectionStateHandler
     *            the connection state handler to bind to
     */
    public static void bind(ConnectionStateHandler connectionStateHandler) {
        Reactive.runWhenDependenciesChange(
                () -> connectionStateHandler.configurationUpdated());
    }

    private MapProperty getProperty(String key) {
        NodeMap configurationMap = registry.getStateTree().getRootNode()
                .getMap(NodeFeatures.RECONNECT_DIALOG_CONFIGURATION);
        return configurationMap.getProperty(key);
    }

    /**
     * Gets the text to show in the reconnect dialog.
     *
     * @return the text to show in the reconnect dialog.
     *
     * @deprecated The API for configuring the connection indicator has changed.
     */
    @Deprecated
    public String getDialogText() {
        return getProperty(ReconnectDialogConfigurationMap.DIALOG_TEXT_KEY)
                .getValueOrDefault(null);
    }

    /**
     * Gets the text to show in the reconnect dialog when no longer trying to
     * reconnect.
     *
     * @return the text to show in the reconnect dialog when no longer trying to
     *         reconnect
     *
     * @deprecated The API for configuring the connection indicator has changed.
     */
    @Deprecated
    public String getDialogTextGaveUp() {
        return getProperty(
                ReconnectDialogConfigurationMap.DIALOG_TEXT_GAVE_UP_KEY)
                .getValueOrDefault(null);
    }

    /**
     * Gets the number of reconnect attempts that should be performed before
     * giving up.
     *
     * @return the number of reconnect attempts to perform
     */
    public int getReconnectAttempts() {
        return getProperty(
                ReconnectDialogConfigurationMap.RECONNECT_ATTEMPTS_KEY)
                .getValueOrDefault(
                        ReconnectDialogConfigurationMap.RECONNECT_ATTEMPTS_DEFAULT);
    }

    /**
     * Gets the interval in milliseconds to wait between reconnect attempts.
     *
     * @return the interval in milliseconds to wait between reconnect attempts
     */
    public int getReconnectInterval() {
        return getProperty(
                ReconnectDialogConfigurationMap.RECONNECT_INTERVAL_KEY)
                .getValueOrDefault(
                        ReconnectDialogConfigurationMap.RECONNECT_INTERVAL_DEFAULT);
    }
}
