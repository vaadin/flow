/*
 * Copyright 2000-2016 Vaadin Ltd.
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
import com.vaadin.client.hummingbird.namespace.MapNamespace;
import com.vaadin.client.hummingbird.namespace.MapProperty;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.hummingbird.namespace.ReconnectDialogConfigurationNamespace;
import com.vaadin.hummingbird.shared.Namespaces;

/**
 * Tracks the reconnect dialog configuration stored in the root node and
 * provides it with an easier to use API.
 * <p>
 * Also triggers {@link ConnectionStateHandler#configurationUpdated()} whenever
 * part of the configuration changes.
 *
 * @author Vaadin
 * @since
 */
public class ReconnectDialogConfiguration {

    private final Registry registry;

    /**
     * Creates a new instance using the given registry.
     *
     * @param registry
     *            the registry
     */
    public ReconnectDialogConfiguration(Registry registry) {
        this.registry = registry;
    }

    /**
     * Binds this ReconnectDialogConfiguration to the given
     * {@link ConnectionStateHandler} so that
     * {@link ConnectionStateHandler#configurationUpdated()} is run whenever a
     * relevant part of {@link ReconnectDialogConfiguration} changes.
     *
     * @param connectionStateHandler
     *            the connection state handler to bind to
     */
    public static void bind(ConnectionStateHandler connectionStateHandler) {
        Reactive.runWhenDepedenciesChange(
                () -> connectionStateHandler.configurationUpdated());
    }

    private MapProperty getProperty(String key) {
        MapNamespace namespace = registry.getStateTree().getRootNode()
                .getMapNamespace(Namespaces.RECONNECT_DIALOG_CONFIGURATION);
        return namespace.getProperty(key);
    }

    /**
     * Checks whether the reconnect dialog should be modal, i.e. prevent
     * application usage while being shown.
     *
     * @return true if the dialog is modal, false otherwise
     */
    public boolean isDialogModal() {
        return getProperty(
                ReconnectDialogConfigurationNamespace.DIALOG_MODAL_KEY)
                        .getValueOrDefault(
                                ReconnectDialogConfigurationNamespace.DIALOG_MODAL_DEFAULT);
    }

    /**
     * Gets the text to show in the reconnect dialog.
     *
     * @return the text to show in the reconnect dialog.
     */
    public String getDialogText() {
        return getProperty(
                ReconnectDialogConfigurationNamespace.DIALOG_TEXT_KEY)
                        .getValueOrDefault(
                                ReconnectDialogConfigurationNamespace.DIALOG_TEXT_DEFAULT);
    }

    /**
     * Gets the text to show in the reconnect dialog when no longer trying to
     * reconnect.
     *
     * @return the text to show in the reconnect dialog when no longer trying to
     *         reconnect
     */
    public String getDialogTextGaveUp() {
        return getProperty(
                ReconnectDialogConfigurationNamespace.DIALOG_TEXT_GAVE_UP_KEY)
                        .getValueOrDefault(
                                ReconnectDialogConfigurationNamespace.DIALOG_TEXT_GAVE_UP_DEFAULT);
    }

    /**
     * Gets the number of reconnect attempts that should be performed before
     * giving up.
     *
     * @return the number of reconnect attempts to perform
     */
    public int getReconnectAttempts() {
        return getProperty(
                ReconnectDialogConfigurationNamespace.RECONNECT_ATTEMPTS_KEY)
                        .getValueOrDefault(
                                ReconnectDialogConfigurationNamespace.RECONNECT_ATTEMPTS_DEFAULT);
    }

    /**
     * Gets the interval in milliseconds to wait between reconnect attempts.
     *
     * @return the interval in milliseconds to wait between reconnect attempts
     */
    public int getReconnectInterval() {
        return getProperty(
                ReconnectDialogConfigurationNamespace.RECONNECT_INTERVAL_KEY)
                        .getValueOrDefault(
                                ReconnectDialogConfigurationNamespace.RECONNECT_INTERVAL_DEFAULT);
    }

    /**
     * Gets the time in milliseconds to wait after noticing a loss of connection
     * but before showing the reconnect dialog.
     *
     * @return the time in milliseconds to wait before showing the dialog
     */
    public int getDialogGracePeriod() {
        return getProperty(
                ReconnectDialogConfigurationNamespace.DIALOG_GRACE_PERIOD_KEY)
                        .getValueOrDefault(
                                ReconnectDialogConfigurationNamespace.DIALOG_GRACE_PERIOD_DEFAULT);
    }

}
