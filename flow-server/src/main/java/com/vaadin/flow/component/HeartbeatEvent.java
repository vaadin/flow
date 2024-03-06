/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.util.EventObject;

/**
 * Event created for an application heartbeat from the client.
 *
 * @since 2.0
 */
public class HeartbeatEvent extends EventObject {

    private final long heartbeatTime;

    /**
     * Constructs a heartbeat Event.
     *
     * @param ui
     *            UI for which the Event occurred
     * @param heartbeatTime
     *            value for the heartbeat
     */
    public HeartbeatEvent(UI ui, long heartbeatTime) {
        super(ui);
        this.heartbeatTime = heartbeatTime;
    }

    public long getHeartbeatTime() {
        return heartbeatTime;
    }

    @Override
    public UI getSource() {
        return (UI) super.getSource();
    }
}
