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
package com.vaadin.client;

import com.vaadin.client.communication.ServerConnector;

import elemental.client.Browser;

/**
 * Helper for binding <code>popstate</code> events to the server.
 *
 * @since
 * @author Vaadin Ltd
 */
public class PopStateBinder {

    private PopStateBinder() {
        // Only static functionality
    }

    /**
     * Sets up a <code>popstate</code> listener for delivering events to the
     * server.
     *
     * @param connector
     *            the connector sending the update to server
     */
    public static void bind(ServerConnector connector) {
        Browser.getWindow().setOnpopstate(e -> {
            Object stateObject = WidgetUtil.getJsProperty(e, "state");
            String location = URIResolver.getCurrentLocationRelativeToBaseUri();

            connector.sendNavigationMessage(location, stateObject);
        });
    }

}
