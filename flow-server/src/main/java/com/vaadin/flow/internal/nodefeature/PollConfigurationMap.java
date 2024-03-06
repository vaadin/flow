/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.StateNode;

/**
 * A node map for storing configuration for polling.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class PollConfigurationMap extends NodeMap {

    public static final String POLL_INTERVAL_KEY = "pollInterval";

    /**
     * Creates a new map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     */
    public PollConfigurationMap(StateNode node) {
        super(node);
    }

    /**
     * Sets the poll interval.
     *
     * @see UI#setPollInterval(int)
     * @param pollInterval
     *            the interval
     */
    public void setPollInterval(int pollInterval) {
        put(POLL_INTERVAL_KEY, pollInterval);
    }

    /**
     * Gets the poll interval.
     *
     * @see UI#getPollInterval()
     * @return the poll interval
     */
    public int getPollInterval() {
        if (!contains(POLL_INTERVAL_KEY)) {
            return -1;
        }
        return (int) get(POLL_INTERVAL_KEY);

    }

}
