/*
 * Copyright 2000-2018 Vaadin Ltd.
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
     *         UI for which the Event occurred
     * @param heartbeatTime
     *         value for the heartbeat
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
