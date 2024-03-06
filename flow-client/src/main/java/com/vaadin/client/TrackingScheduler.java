/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client;

import com.google.gwt.core.client.impl.SchedulerImpl;

/**
 * Scheduler implementation which tracks and reports whether there is any work
 * queued or currently being executed.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class TrackingScheduler extends SchedulerImpl {

    /**
     * Keeps track of if there are deferred commands that are being executed. 0
     * == no deferred commands currently in progress, > 0 otherwise.
     */
    private int deferredCommandTrackers = 0;

    @Override
    public void scheduleDeferred(ScheduledCommand cmd) {
        deferredCommandTrackers++;
        super.scheduleDeferred(cmd);
        super.scheduleDeferred(() -> deferredCommandTrackers--);
    }

    /**
     * Checks if there is work queued or currently being executed.
     *
     * @return true if there is work queued or if work is currently being
     *         executed, false otherwise
     */
    public boolean hasWorkQueued() {
        return deferredCommandTrackers != 0;
    }
}
