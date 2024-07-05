/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.component.internal;

import java.util.EventObject;

import com.vaadin.flow.component.UI;

/**
 * Event created for a application heartbeat from the client.
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
