/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client;

import java.util.Objects;

import com.google.gwt.core.client.Scheduler;
import com.vaadin.client.flow.RouterLinkHandler;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.PopStateEvent;

/**
 * Handles <code>popstate</code> events and sends them to the server.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class PopStateHandler {

    private String pathAfterPreviousResponse;
    private String queryAfterPreviousResponse;

    private Registry registry;

    /**
     * Creates a new <code>popstate</code> listener for delivering events to the
     * server. Ignores events caused by hash changes, e.g. when the page stays
     * the same.
     * <p>
     * If the UI is stopped when a <code>popstate</code> event occurs, performs
     * a refresh to restart the UI.
     * <p>
     * NOTE: the listening won't start before invoking {@link #bind()}.
     *
     * @param registry
     *            the registry to bind to
     */
    public PopStateHandler(Registry registry) {
        this.registry = registry;
    }

    /**
     * Start listening to <code>popstate</code> events and send them to the
     * server.
     * <p>
     * This method should be triggered only once per instance.
     */
    public void bind() {
        // track the location and query string (#6107) after the latest response
        // from server
        registry.getRequestResponseTracker()
                .addResponseHandlingEndedHandler(event ->
                // history.pushState(...) instruction from server side
                // is invoked within a setTimeout(), so we need to defer
                // the retrieval of window.location properties on next
                // event loop cycle, otherwise we get values before
                // they change (#14323)
                Scheduler.get().scheduleDeferred(() -> {
                    pathAfterPreviousResponse = Browser.getWindow()
                            .getLocation().getPathname();
                    queryAfterPreviousResponse = Browser.getWindow()
                            .getLocation().getSearch();
                }));
        Browser.getWindow().addEventListener("popstate", this::onPopStateEvent);
    }

    private void onPopStateEvent(Event e) {
        // refresh page if application has stopped
        if (!registry.getUILifecycle().isRunning()) {
            WidgetUtil.refresh();
            return;
        }

        final String path = Browser.getWindow().getLocation().getPathname();
        final String query = Browser.getWindow().getLocation().getSearch();

        assert pathAfterPreviousResponse != null
                : "Initial response has not ended before pop state event was triggered";

        // don't visit server on pop state events caused by fragment change
        boolean requiresServerSideRoundtrip = !(Objects.equals(path,
                pathAfterPreviousResponse)
                && Objects.equals(query, queryAfterPreviousResponse));
        registry.getScrollPositionHandler().onPopStateEvent((PopStateEvent) e,
                requiresServerSideRoundtrip);
        if (!requiresServerSideRoundtrip) {
            return;
        }

        String location = URIResolver.getCurrentLocationRelativeToBaseUri();
        // don't send hash to server
        if (location.contains("#")) {
            location = location.split("#", 2)[0];
        }
        Object stateObject = WidgetUtil.getJsProperty(e, "state");
        RouterLinkHandler.sendServerNavigationEvent(registry, location,
                stateObject, false);
    }
}
