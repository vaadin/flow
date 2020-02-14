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
