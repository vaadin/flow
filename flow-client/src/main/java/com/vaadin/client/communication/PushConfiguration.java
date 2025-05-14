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
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.MapPropertyChangeEvent;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.PushConfigurationMap;

/**
 * Provides the push configuration stored in the root node with an easier to use
 * API.
 *
 * Additionally tracks when push is enabled/disabled and informs
 * {@link MessageSender}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class PushConfiguration {

    private final Registry registry;

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public PushConfiguration(Registry registry) {
        this.registry = registry;
        setupListener();
    }

    private void setupListener() {
        getConfigurationMap().getProperty(PushConfigurationMap.PUSHMODE_KEY)
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
                    () -> registry.getMessageSender().setPushEnabled(true));
        } else if (oldModeEnabled && !newModeEnabled) {
            // Switch push off
            // We must wait until all parts of push configuration has been
            // updated
            Reactive.addFlushListener(
                    () -> registry.getMessageSender().setPushEnabled(false));
        }
    }

    private NodeMap getConfigurationMap() {
        return registry.getStateTree().getRootNode()
                .getMap(NodeFeatures.UI_PUSHCONFIGURATION);
    }

    /**
     * Gets the push servlet mapping configured or determined on the server.
     *
     * @return the push servlet mapping configured or determined on the server
     *         or null if none has been configured
     */
    public String getPushServletMapping() {
        if (getConfigurationMap().hasPropertyValue(
                PushConfigurationMap.PUSH_SERVLET_MAPPING_KEY)) {
            return (String) getConfigurationMap()
                    .getProperty(PushConfigurationMap.PUSH_SERVLET_MAPPING_KEY)
                    .getValue();
        }

        return null;
    }

    /**
     * Checks if XHR should be used for client -&gt; server messages even though
     * we are using a bidirectional push transport such as websockets.
     *
     * @return true if XHR should always be used, false otherwise
     */
    public boolean isAlwaysXhrToServer() {
        // The only possible value is "true"
        return (getConfigurationMap().hasPropertyValue(
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
        MapProperty p = getConfigurationMap()
                .getProperty(PushConfigurationMap.PARAMETERS_KEY);
        StateNode parametersNode = (StateNode) p.getValue();
        NodeMap parametersMap = parametersNode
                .getMap(NodeFeatures.UI_PUSHCONFIGURATION_PARAMETERS);

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
        return isPushEnabled(getConfigurationMap()
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
