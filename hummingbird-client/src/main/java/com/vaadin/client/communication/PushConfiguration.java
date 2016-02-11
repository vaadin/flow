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

import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;
import com.vaadin.client.hummingbird.namespace.MapNamespace;
import com.vaadin.client.hummingbird.namespace.MapProperty;
import com.vaadin.client.hummingbird.namespace.MapPropertyChangeEvent;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.hummingbird.namespace.PushConfigurationMap;
import com.vaadin.hummingbird.shared.Namespaces;

/**
 * Provides the push configuration stored in the root node with an easier to use
 * API.
 *
 * Additionally tracks when push is enabled/disabled and informs
 * {@link MessageSender}.
 *
 * @author Vaadin
 * @since
 */
public class PushConfiguration {

    private ApplicationConnection connection;

    /**
     * Sets the application connection this instance is connected to.
     *
     * @param connection
     *            the application connection
     */
    public void setConnection(ApplicationConnection connection) {
        this.connection = connection;
        setupListener();
    }

    private void setupListener() {
        getConfigurationNamespace()
                .getProperty(PushConfigurationMap.PUSHMODE_KEY)
                .addChangeListener(this::onPushModeChange);
    }

    /**
     * Called whenever the push mode is changed.
     *
     * @param event
     *            the value change event for push mode
     */
    private void onPushModeChange(MapPropertyChangeEvent event) {
        boolean oldModeEnabled = isPushEnabled(event.getOldValue());
        boolean newModeEnabled = isPushEnabled(event.getNewValue());

        if (!oldModeEnabled && newModeEnabled) {
            // Switch push on

            // We must wait until all parts of push configuration has been
            // updated
            Reactive.addFlushListener(
                    () -> connection.getMessageSender().setPushEnabled(true));
        } else if (oldModeEnabled && !newModeEnabled) {
            // Switch push off
            // We must wait until all parts of push configuration has been
            // updated
            Reactive.addFlushListener(
                    () -> connection.getMessageSender().setPushEnabled(false));
        }
    }

    private MapNamespace getConfigurationNamespace() {
        return connection.getTree().getRootNode()
                .getMapNamespace(Namespaces.UI_PUSHCONFIGURATION);
    }

    /**
     * Gets the push URL configured on the server.
     *
     * @return the push URL configured on the server or null if none has been
     *         configured
     */
    public String getPushUrl() {
        if (getConfigurationNamespace()
                .hasPropertyValue(PushConfigurationMap.PUSH_URL_KEY)) {
            return (String) getConfigurationNamespace()
                    .getProperty(PushConfigurationMap.PUSH_URL_KEY).getValue();
        }

        return null;
    }

    /**
     * Checks if XHR should be used for client -> server messages even though we
     * are using a bidirectional push transport such as websockets.
     *
     * @return true if XHR should always be used, false otherwise
     */
    public boolean isAlwaysXhrToServer() {
        // The only possible value is "true"
        return (getConfigurationNamespace().hasPropertyValue(
                PushConfigurationMap.ALWAYS_USE_XHR_TO_SERVER));
    }

    /**
     * Gets all configured push parameters.
     *
     * The parameters configured on the server, including transports.
     *
     * @return a map of all parameters configured on the server
     */
    public JsMap<String, String> getParameters() {
        MapProperty p = getConfigurationNamespace()
                .getProperty(PushConfigurationMap.PARAMETERS_KEY);
        StateNode parametersNode = (StateNode) p.getValue();
        MapNamespace parametersMap = parametersNode
                .getMapNamespace(Namespaces.UI_PUSHCONFIGURATION_PARAMETERS);

        JsMap<String, String> parameters = JsCollections.map();
        parametersMap.forEachProperty((property, key) -> {
            parameters.set(key, (String) property.getValue());
        });

        return parameters;
    }

    /**
     * Checks if push is enabled.
     *
     * @return true if push is enabled, false otherwise
     */
    public boolean isPushEnabled() {
        return isPushEnabled(getConfigurationNamespace()
                .getProperty(PushConfigurationMap.PUSHMODE_KEY).getValue());
    }

    /**
     * Checks the given propertyValue from the PUSHMODE key to determine if push
     * is enabled or not.
     *
     * @param propertyValue
     *            the PushMode value
     * @return true if push is enabled, false otherwise
     */
    private static boolean isPushEnabled(Object propertyValue) {
        if (propertyValue == null) {
            return false;
        }

        String pushMode = (String) propertyValue;
        // Intentionally avoiding bringing the enum to client side
        return !"DISABLED".equals(pushMode);
    }

}
