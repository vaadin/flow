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
package com.vaadin.flow.micrometer;

/**
 * Centralized meter name constants. Kept in one place so dashboards and tests
 * can share the same values.
 */
public final class MeterNames {

    public static final String SESSIONS_ACTIVE = "vaadin.sessions.active";
    public static final String SESSIONS_CREATED = "vaadin.sessions.created";
    public static final String SESSIONS_DURATION = "vaadin.sessions.duration";

    public static final String SESSION_LOCK_WAIT = "vaadin.session.lock.wait";
    public static final String SESSION_LOCK_HOLD = "vaadin.session.lock.hold";

    public static final String UI_ACTIVE = "vaadin.ui.active";
    public static final String UI_CREATED = "vaadin.ui.created";

    public static final String NAVIGATION = "vaadin.navigation";

    public static final String REQUEST_DURATION = "vaadin.request.duration";

    public static final String ERRORS = "vaadin.errors";

    public static final String CLIENT_BOOTSTRAP_DURATION = "vaadin.client.bootstrap.duration";
    public static final String CLIENT_NAVIGATION_DURATION = "vaadin.client.navigation.duration";
    public static final String CLIENT_RPC_DURATION = "vaadin.client.rpc.duration";
    public static final String CLIENT_WEB_VITALS_LCP = "vaadin.client.web_vitals.lcp";
    public static final String CLIENT_WEB_VITALS_FCP = "vaadin.client.web_vitals.fcp";
    public static final String CLIENT_ERRORS = "vaadin.client.errors";
    public static final String CLIENT_DROPPED = "vaadin.client.dropped";
    public static final String CLIENT_THROTTLED = "vaadin.client.throttled";

    public static final String TAG_ROUTE = "route";
    public static final String TAG_OUTCOME = "outcome";
    public static final String TAG_EXCEPTION = "exception";
    public static final String TAG_TRIGGER = "trigger";
    public static final String TAG_KIND = "kind";
    public static final String TAG_CONTEXT = "context";

    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_ERROR = "error";

    public static final String CONTEXT_REQUEST = "request";
    public static final String CONTEXT_ACCESS = "access";

    public static final String ROUTE_OTHER = "_other";
    public static final String ROUTE_UNKNOWN = "_unknown";

    private MeterNames() {
    }
}
