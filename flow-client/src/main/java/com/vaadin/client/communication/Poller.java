/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
