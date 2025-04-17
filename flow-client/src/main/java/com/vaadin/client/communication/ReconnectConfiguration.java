/*
 * Copyright 2000-2025 Vaadin Ltd.
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
