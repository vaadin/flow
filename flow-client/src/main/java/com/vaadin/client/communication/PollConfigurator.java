/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.PollConfigurationMap;

/**
 * Observes the poll configuration stored in the given node and configures
 * polling accordingly.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class PollConfigurator {

    private PollConfigurator() {
        // No instance should ever be created
    }

    /**
     * Observes the given node for poll configuration changes and configures the
     * given poller accordingly.
     *
     * @param node
     *            the node containing the poll configuration
     * @param poller
     *            the poller to configure
     */
    public static void observe(StateNode node, Poller poller) {
        NodeMap configurationMap = node.getMap(NodeFeatures.POLL_CONFIGURATION);
        MapProperty pollIntervalProperty = configurationMap
                .getProperty(PollConfigurationMap.POLL_INTERVAL_KEY);
        pollIntervalProperty.addChangeListener(e -> {
            int interval = (int) (double) e.getNewValue();
            poller.setInterval(interval);
        });
    }

}
