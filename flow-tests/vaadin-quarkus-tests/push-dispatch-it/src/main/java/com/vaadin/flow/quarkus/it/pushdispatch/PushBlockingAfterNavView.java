/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it.pushdispatch;

import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Route;

/**
 * Reproduces the Push-dispatch deadlock shape the extension defaults against.
 * With {@code @Push} (on the app shell) and a single Vert.x event loop, the
 * Quarkus default ({@code quarkus.websocket.dispatch-to-worker=false}) lets
 * {@code PushHandler} run inline on the event loop. The synchronous Quarkus
 * REST client call below is what mirrors the original deadlock: the call needs
 * the Vert.x event loop to send the request and deliver the response, but
 * {@code PushHandler.onConnect} is sitting on that loop waiting for the Vaadin
 * session lock that this {@code afterNavigation} hook is holding. Without the
 * worker dispatch the call never returns, the lock is never released, and the
 * page never reaches its {@link #READY_TEXT} state.
 * <p>
 * The extension defaults {@code dispatch-to-worker} to {@code true}, so Push
 * runs on a worker and the event loop stays free to drive the REST call to
 * completion.
 */
@Route("push-blocking")
public class PushBlockingAfterNavView extends Div
        implements AfterNavigationObserver {

    public static final String VIEW_ID = "push-blocking-view";
    public static final String READY_TEXT = "ready";
    public static final String LOADING_TEXT = "loading";

    @Inject
    @RestClient
    SlowGreetingClient client;

    public PushBlockingAfterNavView() {
        setId(VIEW_ID);
        setText(LOADING_TEXT);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        setText(client.slowGreeting());
    }
}
