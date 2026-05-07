/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import com.google.gwt.core.client.Scheduler;

import com.vaadin.client.ConnectionIndicator;
import com.vaadin.client.Registry;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsSet;
import com.vaadin.flow.shared.JsonConstants;

/**
 * Manages the state of loading indicator based on active RPC requests, event
 * types, and lifecycle events.
 * <p>
 * This class ensures appropriate visual feedback (e.g., loading bar) is shown
 * or hidden according to the current network conditions and request status. It
 * is responsible for muting the loading indication when RPC requests are
 * triggered by high-frequency UI events (mousemove and such) to avoid excessive
 * visual noise in these cases.
 */
public class LoadingIndicatorStateHandler {
    private final Registry registry;

    private boolean loading = false;

    private boolean showLoading = false;

    // High-frequency events, whose related RPC requests are not expected
    // to trigger loading indication.
    private static final JsSet<String> SILENT_EVENT_TYPES = JsCollections.set();
    {
        JsCollections.array("keydown", "keypress", "keyup", "mousemove",
                "pointermove", "pointerrawupdate", "touchmove", "beforeinput",
                "input", "scroll", "wheel", "drag", "dragover")
                .forEach(SILENT_EVENT_TYPES::add);
    }

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public LoadingIndicatorStateHandler(Registry registry) {
        this.registry = registry;
    }

    /**
     * Updates the connection state to {@link ConnectionIndicator#LOADING} when
     * a non-silent request starts.
     */
    public void startLoading() {
        if (!showLoading) {
            // The next request is muted, do not show loading.
            return;
        }

        update();
    }

    /**
     * Updates the connection state to {@link ConnectionIndicator#CONNECTED}
     * when active requests finish.
     */
    public void stopLoading() {
        if (registry.getRequestResponseTracker().hasActiveRequest()) {
            // Some request is in progress, skip the current stop.
            return;
        }

        // Reset the loading state
        showLoading = false;

        // Debounce the update to avoid hiding loading when a follow-up
        // request is started or scheduled right away.
        Scheduler.get().scheduleDeferred(this::update);
    }

    /**
     * Processes an RPC message to determine if a loading indicator should be
     * displayed.
     *
     * @param rpcType
     *            the type of RPC request being processed
     * @param eventType
     *            for event RPC requests, the name of the event, otherwise
     *            {@code null}
     */
    public void processMessage(String rpcType, String eventType) {
        // Require at least one non-silent message to indicate loading for
        // the next request.
        boolean silent = JsonConstants.RPC_TYPE_EVENT.equals(rpcType)
                && eventType != null && SILENT_EVENT_TYPES.has(eventType);
        if (!silent) {
            showLoading = true;
        }
    }

    /**
     * Applies the loading state change after a dirty check.
     */
    private void update() {
        if (showLoading == loading) {
            return;
        }

        loading = showLoading;
        // Setting the loading state directly using
        // `ConnectionIndicator.setState()` interferes with other loading
        // parties
        // (Flow router, Hilla requests), therefore `.loadingStarted()` /
        // `.loadingFinished()` are preferred.
        if (loading) {
            ConnectionIndicator.loadingStarted();
        } else {
            ConnectionIndicator.loadingFinished();
        }
    }
}
