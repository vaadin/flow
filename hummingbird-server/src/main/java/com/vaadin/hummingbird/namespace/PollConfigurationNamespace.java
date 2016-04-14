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
package com.vaadin.hummingbird.namespace;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.ui.UI;

/**
 * Namespace for storing configuration for polling.
 *
 * @author Vaadin Ltd
 * @since
 */
public class PollConfigurationNamespace extends MapNamespace {

    public static final String POLL_INTERVAL_KEY = "pollInterval";

    /**
     * Creates a new namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public PollConfigurationNamespace(StateNode node) {
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
