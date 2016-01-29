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
package com.vaadin.client.communication;

import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Timer;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.ApplicationConnection.ApplicationStoppedEvent;
import com.vaadin.client.elemental.js.util.Xhr;
import com.vaadin.client.Console;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.util.SharedUtil;

/**
 * Handles sending of heartbeats to the server and reacting to the response
 *
 * @since 7.2
 * @author Vaadin Ltd
 */
public class Heartbeat {

    private Timer timer = new Timer() {
        @Override
        public void run() {
            send();
        }
    };

    private ApplicationConnection connection;
    private String uri;
    private int interval = -1;

    /**
     * Initializes the heartbeat for the given application connection
     *
     * @param connection
     *            the connection
     */
    public void init(ApplicationConnection applicationConnection) {
        connection = applicationConnection;

        setInterval(connection.getConfiguration().getHeartbeatInterval());

        uri = SharedUtil.addGetParameters(
                connection.translateVaadinUri(
                        ApplicationConstants.APP_PROTOCOL_PREFIX
                                + ApplicationConstants.HEARTBEAT_PATH + '/'),
                ApplicationConstants.UI_ID_PARAMETER + "="
                        + connection.getConfiguration().getUIId());

        connection.addHandler(
                ApplicationConnection.ApplicationStoppedEvent.TYPE,
                new ApplicationConnection.ApplicationStoppedHandler() {

                    @Override
                    public void onApplicationStopped(
                            ApplicationStoppedEvent event) {
                        setInterval(-1);
                    }
                });
    }

    /**
     * Sends a heartbeat to the server
     */
    public void send() {
        timer.cancel();

        Console.debug("Sending heartbeat request...");
        Xhr.post(uri, null, "text/plain; charset=utf-8", new Xhr.Callback() {

            @Override
            public void onSuccess(XMLHttpRequest xhr) {
                int status = xhr.getStatus();

                if (status == Response.SC_OK) {
                    connection.getConnectionStateHandler().heartbeatOk();
                } else {
                    // Handler should stop the application if heartbeat should
                    // no longer be sent
                    connection.getConnectionStateHandler()
                            .heartbeatInvalidStatusCode(xhr);
                }

                schedule();
            }

            @Override
            public void onFail(XMLHttpRequest xhr) {
                // Handler should stop the application if heartbeat should no
                // longer be sent
                connection.getConnectionStateHandler()
                        .heartbeatInvalidStatusCode(xhr);
                schedule();

            }
        });

    }

    /**
     * @return the interval at which heartbeat requests are sent
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
    public void setInterval(int heartbeatInterval) {
        Console.log(
                "Setting hearbeat interval to " + heartbeatInterval + "sec.");
        interval = heartbeatInterval;
        schedule();
    }
}
