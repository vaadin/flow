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

import com.google.gwt.user.client.Timer;
import com.vaadin.client.Registry;
import com.vaadin.client.flow.StateTree;
import com.vaadin.flow.component.PollEvent;

/**
 * Handles polling the server with a given interval.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Poller {

    private Timer pollTimer = null;

    private final Registry registry;

    /**
     * Creates a new instance using the given registry.
     *
     * @param registry
     *            the registry
     */
    public Poller(Registry registry) {
        this.registry = registry;
        registry.getUILifecycle().addHandler(e -> {
            if (e.getUiLifecycle().isTerminated()) {
                stop();
            }
        });
    }

    /**
     * Stops any ongoing polling.
     */
    private void stop() {
        if (pollTimer != null) {
            pollTimer.cancel();
            pollTimer = null;
        }
    }

    /**
     * Sets the polling interval.
     * <p>
     * Changing the polling interval will stop any current polling and schedule
     * a new poll to happen after the given interval.
     *
     * @param interval
     *            The interval to use
     */
    public void setInterval(int interval) {
        stop();
        if (interval >= 0) {
            pollTimer = new Timer() {
                @Override
                public void run() {
                    poll();
                }

            };
            pollTimer.scheduleRepeating(interval);
        }
    }

    /**
     * Polls the server for changes.
     */
    public void poll() {
        StateTree stateTree = registry.getStateTree();
        stateTree.sendEventToServer(stateTree.getRootNode(),
                PollEvent.DOM_EVENT_NAME, null);
    }

}
