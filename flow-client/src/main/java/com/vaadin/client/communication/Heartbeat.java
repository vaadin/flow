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
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.vaadin.client.Console;
import com.vaadin.client.Registry;
import com.vaadin.client.gwt.elemental.js.util.Xhr;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.util.SharedUtil;

/**
 * Handles sending of heartbeats to the server and reacting to the response
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Heartbeat {

    private Timer timer = new Timer() {
        @Override
        public void run() {
            send();
        }
    };

    private String uri;
    private int interval = -1;

    private final Registry registry;

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public Heartbeat(Registry registry) {
        this.registry = registry;
        setInterval(
                registry.getApplicationConfiguration().getHeartbeatInterval());

        uri = registry.getApplicationConfiguration().getServiceUrl();
        uri = SharedUtil.addGetParameter(uri,
                ApplicationConstants.REQUEST_TYPE_PARAMETER,
                ApplicationConstants.REQUEST_TYPE_HEARTBEAT);
        uri = SharedUtil.addGetParameter(uri,
                ApplicationConstants.UI_ID_PARAMETER,
                registry.getApplicationConfiguration().getUIId());

        registry.getUILifecycle().addHandler(e -> {
            if (e.getUiLifecycle().isTerminated()) {
                setInterval(-1);
            }
        });
    }

    /**
     * Sends a heartbeat to the server.
     */
    public void send() {
        timer.cancel();

        Console.debug("Sending heartbeat request...");
        Xhr.post(uri, null, "text/plain; charset=utf-8", new Xhr.Callback() {

            @Override
            public void onSuccess(XMLHttpRequest xhr) {
                registry.getConnectionStateHandler().heartbeatOk();
                schedule();
            }

            @Override
            public void onFail(XMLHttpRequest xhr, Exception e) {

                // Handler should stop the application if heartbeat should no
                // longer be sent
                if (e == null) {
                    registry.getConnectionStateHandler()
                            .heartbeatInvalidStatusCode(xhr);
                } else {
                    registry.getConnectionStateHandler().heartbeatException(xhr,
                            e);
                }
                schedule();

            }
        });

    }

    /**
     * @return the interval at which heartbeat requests are sent.
     */
    public int getInterval() {
        return interval;
    }

    /**
     * Updates the schedule of the heartbeat to match the set interval. A
     * negative interval disables the heartbeat.
     */
    public void schedule() {
        if (interval > 0) {
            Console.debug("Scheduling heartbeat in " + interval + " seconds");
            timer.schedule(interval * 1000);
        } else {
            Console.debug("Disabling heartbeat");
            timer.cancel();
        }
    }

    /**
     * Changes the heartbeatInterval in runtime and applies it.
     *
     * @param heartbeatInterval
     *            new interval in seconds.
     */
    public final void setInterval(int heartbeatInterval) {
        Console.log(
                "Setting heartbeat interval to " + heartbeatInterval + "sec.");
        interval = heartbeatInterval;
        schedule();
    }
}
