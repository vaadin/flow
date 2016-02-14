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
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.client.hummingbird.namespace.MapNamespace;
import com.vaadin.client.hummingbird.namespace.MapPropertyChangeEvent;
import com.vaadin.hummingbird.shared.Namespaces;

/**
 * Provides the poll configuration stored in the root node with an easier to use
 * API.
 *
 * @author Vaadin
 * @since
 */
public class PollConfiguration {

    private final Registry registry;

    /**
     * Creates a new instance using the given registry.
     *
     * @param registry
     *            the registry
     */
    public PollConfiguration(Registry registry) {
        this.registry = registry;
        StateTree stateTree = registry.getStateTree();
        setupListener(stateTree.getRootNode()
                .getMapNamespace(Namespaces.POLL_CONFIGURATION));
    }

    private void setupListener(MapNamespace uiConfiguration) {
        uiConfiguration
                .getProperty(
                        com.vaadin.hummingbird.namespace.PollConfigurationNamespace.POLL_INTERVAL_KEY)
                .addChangeListener(this::onPollIntervalChange);
    }

    /**
     * Called whenever the poll interval value changes.
     *
     * @param event
     *            the change event for the poll interval property
     */
    private void onPollIntervalChange(MapPropertyChangeEvent event) {
        registry.getPoller().setInterval((int) (double) event.getNewValue());
    }

}
