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

import java.util.Objects;

import com.vaadin.client.hummingbird.RouterLinkHandler;

import elemental.client.Browser;
import elemental.events.Event;

/**
 * Handles <code>popstate</code> events and sents them to the server.
 *
 * @author Vaadin Ltd
 */
public class PopStateHandler {

    private String pathAfterPreviousResponse;
    private String hashAfterPreviousResponse;
    private Registry registry;

    /**
     * Sets up a <code>popstate</code> listener for delivering events to the
     * server. Ignores events caused by inside page fragment changes.
     * <p>
     * If the UI is stopped when a popstate event occurs, performs a refresh to
     * restart the UI.
     *
     * @param registry
     *            the registry for the UI
     */
    public void bind(Registry registry) {
        this.registry = registry;
        // track the location after the latest response from server
        registry.getRequestResponseTracker()
                .addResponseHandlingEndedHandler(event -> {
                    pathAfterPreviousResponse = Browser.getWindow()
                            .getLocation().getPathname();
                    hashAfterPreviousResponse = Browser.getWindow()
                            .getLocation().getHash();
                });

        Browser.getWindow().setOnpopstate(this::onPopStateEvent);
    }

    private void onPopStateEvent(Event e) {
        // refresh page if application has stopped
        if (!registry.getUILifecycle().isRunning()) {
            WidgetUtil.refresh();
            return;
        }

        final String hash = Browser.getWindow().getLocation().getHash();
        final String path = Browser.getWindow().getLocation().getPathname();

        assert hash != null : "window.location.hash should never be null";
        assert pathAfterPreviousResponse != null : "Initial response has not ended before pop state event was triggered";
        assert hashAfterPreviousResponse != null : "hash should never be null";

        // ignore pop state events caused by fragment change
        if (Objects.equals(path, pathAfterPreviousResponse)
                && !Objects.equals(hash, hashAfterPreviousResponse)) {
            return;
        }

        String location = URIResolver.getCurrentLocationRelativeToBaseUri();
        // don't send hash to server
        if (!hash.isEmpty()) {
            location = location.split("#", 2)[0];
        }
        Object stateObject = WidgetUtil.getJsProperty(e, "state");
        RouterLinkHandler.sendServerNavigationEvent(registry, location,
                stateObject);
    }

}
