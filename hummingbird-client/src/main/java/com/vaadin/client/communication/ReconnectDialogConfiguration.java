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
import com.vaadin.client.hummingbird.namespace.MapPropertyChangeEvent;
import com.vaadin.client.hummingbird.reactive.FlushListener;
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
    private FlushListener flushListener = null;

    /**
     * Creates a new instance using the given registry.
     *
     * @param registry
     *            the registry
     */
    public ReconnectDialogConfiguration(Registry registry) {
        this.registry = registry;
        setupListener();
    }

    private void setupListener() {
        // Listen to any changes..
        String[] keys = new String[] {
                ReconnectDialogConfigurationNamespace.DIALOG_GRACE_PERIOD_KEY,
                ReconnectDialogConfigurationNamespace.DIALOG_TEXT_GAVE_UP_KEY,
                ReconnectDialogConfigurationNamespace.DIALOG_TEXT_KEY,
                ReconnectDialogConfigurationNamespace.RECONNECT_ATTEMPTS_KEY,
                ReconnectDialogConfigurationNamespace.RECONNECT_INTERVAL_KEY,
                ReconnectDialogConfigurationNamespace.DIALOG_MODAL_KEY };
        for (String key : keys) {
            getConfigurationNamespace().getProperty(key)
                    .addChangeListener(this::onConfigurationChange);
        }
    }

    /**
     * Called whenever any part of the configuration is changed.
     *
     * @param event
     *            the value change event for the property
     */
    private void onConfigurationChange(MapPropertyChangeEvent event) {
        // Wait until all parts of the configuration has been
        // updated
        if (flushListener == null) {
            flushListener = () -> {
                flushListener = null;
                registry.getConnectionStateHandler().configurationUpdated();
            };
            Reactive.addFlushListener(flushListener);
        }
    }

    private MapNamespace getConfigurationNamespace() {
        return registry.getStateTree().getRootNode()
                .getMapNamespace(Namespaces.RECONNECT_DIALOG_CONFIGURATION);
    }

    /**
     * Checks whether the reconnect dialog should be modal, i.e. prevent
     * application usage while being shown.
     *
     * @return true if the dialog is modal, false otherwise
     */
    public boolean isDialogModal() {
        MapProperty p = getConfigurationNamespace().getProperty(
                ReconnectDialogConfigurationNamespace.DIALOG_MODAL_KEY);
        if (!p.hasValue()) {
            return ReconnectDialogConfigurationNamespace.DIALOG_MODAL_DEFAULT;
        }
        return (boolean) p.getValue();
    }

    /**
     * Gets the text to show in the reconnect dialog.
     *
     * @return the text to show in the reconnect dialog.
     */
    public String getDialogText() {
        MapProperty p = getConfigurationNamespace().getProperty(
                ReconnectDialogConfigurationNamespace.DIALOG_TEXT_KEY);
        if (!p.hasValue()) {
            return ReconnectDialogConfigurationNamespace.DIALOG_TEXT_DEFAULT;
        }
        return (String) p.getValue();
    }

    /**
     * Gets the text to show in the reconnect dialog when no longer trying to
     * reconnect.
     *
     * @return the text to show in the reconnect dialog when no longer trying to
     *         reconnect
     */
    public String getDialogTextGaveUp() {
        MapProperty p = getConfigurationNamespace().getProperty(
                ReconnectDialogConfigurationNamespace.DIALOG_TEXT_GAVE_UP_KEY);
        if (!p.hasValue()) {
            return ReconnectDialogConfigurationNamespace.DIALOG_TEXT_GAVE_UP_DEFAULT;
        } else {
            return (String) p.getValue();
        }
    }

    /**
     * Gets the number of reconnect attempts that should be performed before
     * giving up.
     *
     * @return the number of reconnect attempts to perform
     */
    public int getReconnectAttempts() {
        MapProperty p = getConfigurationNamespace().getProperty(
                ReconnectDialogConfigurationNamespace.RECONNECT_ATTEMPTS_KEY);
        if (!p.hasValue()) {
            return ReconnectDialogConfigurationNamespace.RECONNECT_ATTEMPTS_DEFAULT;
        } else {
            return (int) (double) p.getValue();
        }
    }

    /**
     * Gets the interval in milliseconds to wait between reconnect attempts.
     *
     * @return the interval in milliseconds to wait between reconnect attempts
     */
    public int getReconnectInterval() {
        MapProperty p = getConfigurationNamespace().getProperty(
                ReconnectDialogConfigurationNamespace.RECONNECT_INTERVAL_KEY);
        if (!p.hasValue()) {
            return ReconnectDialogConfigurationNamespace.RECONNECT_INTERVAL_DEFAULT;
        } else {
            return (int) (double) p.getValue();
        }
    }

    /**
     * Gets the time in milliseconds to wait after noticing a loss of connection
     * but before showing the reconnect dialog.
     *
     * @return the time in milliseconds to wait before showing the dialog
     */
    public int getDialogGracePeriod() {
        MapProperty p = getConfigurationNamespace().getProperty(
                ReconnectDialogConfigurationNamespace.DIALOG_GRACE_PERIOD_KEY);
        if (!p.hasValue()) {
            return ReconnectDialogConfigurationNamespace.DIALOG_GRACE_PERIOD_DEFAULT;
        } else {
            return (int) (double) p.getValue();
        }
    }

}
