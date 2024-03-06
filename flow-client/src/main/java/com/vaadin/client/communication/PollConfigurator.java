/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
