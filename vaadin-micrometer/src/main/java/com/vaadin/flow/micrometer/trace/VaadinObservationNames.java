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
package com.vaadin.flow.micrometer.trace;

/**
 * Span name + attribute-key constants for vaadin-micrometer Observations.
 * <p>
 * Span names follow Micrometer/OpenTelemetry conventions (lowercase
 * dot-separated). They are intentionally distinct from the parallel Meter names
 * (e.g. {@code vaadin.request} span vs. {@code vaadin.request.duration} timer)
 * so that auto-Timer producers like {@code
 * DefaultMeterObservationHandler} do not collide with the existing manual
 * timers emitted by the binders.
 */
public final class VaadinObservationNames {

    public static final String REQUEST = "vaadin.request";
    public static final String NAVIGATION = "vaadin.navigation";
    public static final String UI_ACCESS = "vaadin.ui.access";
    public static final String RPC = "vaadin.rpc";

    public static final String KEY_OUTCOME = "outcome";
    public static final String KEY_REQUEST_TYPE = "vaadin.request.type";
    public static final String KEY_INTERACTION = "vaadin.interaction";
    public static final String KEY_ROUTE = "route";
    public static final String KEY_HTTP_METHOD = "http.method";
    public static final String KEY_SESSION_ID = "vaadin.session.id";
    public static final String KEY_UI_ID = "ui.id";
    public static final String KEY_CLIENT_LOCATION = "vaadin.client.location";
    /**
     * Low-cardinality RPC invocation type ({@code event}, {@code mSync}, ...).
     */
    public static final String KEY_RPC_TYPE = "vaadin.rpc.type";
    /** High-cardinality RPC detail: DOM event name, method name, location. */
    public static final String KEY_RPC_NAME = "vaadin.rpc.name";
    /** High-cardinality id of the targeted {@code StateNode}. */
    public static final String KEY_NODE_ID = "vaadin.node.id";

    /** A poll request triggered by a configured poll interval. */
    public static final String INTERACTION_POLL = "poll";
    /** The request performed a server-side navigation. */
    public static final String INTERACTION_NAVIGATION = "navigation";
    /**
     * A real client→server RPC (DOM event, {@code @ClientCallable}, property
     * sync, return channel) that we cannot break down further without parsing
     * the UIDL body.
     */
    public static final String INTERACTION_RPC = "rpc";

    public static final String UI_ID_UNKNOWN = "_unknown";
    public static final String LOCATION_UNKNOWN = "_unknown";

    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_ERROR = "error";

    public static final String REQUEST_TYPE_UIDL = "uidl";
    public static final String REQUEST_TYPE_HEARTBEAT = "heartbeat";
    public static final String REQUEST_TYPE_PUSH = "push";
    public static final String REQUEST_TYPE_STATIC = "static";
    public static final String REQUEST_TYPE_OTHER = "other";

    private VaadinObservationNames() {
    }
}
