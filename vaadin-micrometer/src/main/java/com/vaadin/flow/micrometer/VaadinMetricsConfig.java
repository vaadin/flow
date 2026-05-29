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

import java.io.Serializable;

/**
 * Configuration for {@link VaadinMicrometer} instrumentation. Plain immutable
 * value object so it can be populated from any configuration source (Spring
 * properties, system properties, programmatic).
 */
public final class VaadinMetricsConfig implements Serializable {

    /**
     * Default upper bound on the number of distinct route tag values before
     * subsequent values are bucketed under {@code _other}.
     */
    public static final int DEFAULT_ROUTE_CARDINALITY_LIMIT = 200;

    /**
     * Default per-session ceiling on accepted client samples. Excess samples
     * are dropped and counted in {@code vaadin.client.throttled}.
     */
    public static final int DEFAULT_CLIENT_RATE_PER_SESSION = 100;

    private final boolean sessions;
    private final boolean uis;
    private final boolean navigation;
    private final boolean requests;
    private final boolean errors;
    private final boolean client;
    private final boolean traces;
    private final boolean tracesSessionId;
    private final int routeCardinalityLimit;
    private final int clientRatePerSession;

    private VaadinMetricsConfig(Builder b) {
        this.sessions = b.sessions;
        this.uis = b.uis;
        this.navigation = b.navigation;
        this.requests = b.requests;
        this.errors = b.errors;
        this.client = b.client;
        this.traces = b.traces;
        this.tracesSessionId = b.tracesSessionId;
        this.routeCardinalityLimit = b.routeCardinalityLimit;
        this.clientRatePerSession = b.clientRatePerSession;
    }

    public static VaadinMetricsConfig defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isSessions() {
        return sessions;
    }

    public boolean isUis() {
        return uis;
    }

    public boolean isNavigation() {
        return navigation;
    }

    public boolean isRequests() {
        return requests;
    }

    public boolean isErrors() {
        return errors;
    }

    public boolean isClient() {
        return client;
    }

    public boolean isTraces() {
        return traces;
    }

    public boolean isTracesSessionId() {
        return tracesSessionId;
    }

    public int getRouteCardinalityLimit() {
        return routeCardinalityLimit;
    }

    public int getClientRatePerSession() {
        return clientRatePerSession;
    }

    public static final class Builder {
        private boolean sessions = true;
        private boolean uis = true;
        private boolean navigation = true;
        private boolean requests = true;
        private boolean errors = true;
        private boolean client = true;
        private boolean traces = true;
        private boolean tracesSessionId = false;
        private int routeCardinalityLimit = DEFAULT_ROUTE_CARDINALITY_LIMIT;
        private int clientRatePerSession = DEFAULT_CLIENT_RATE_PER_SESSION;

        public Builder sessions(boolean enabled) {
            this.sessions = enabled;
            return this;
        }

        public Builder uis(boolean enabled) {
            this.uis = enabled;
            return this;
        }

        public Builder navigation(boolean enabled) {
            this.navigation = enabled;
            return this;
        }

        public Builder requests(boolean enabled) {
            this.requests = enabled;
            return this;
        }

        public Builder errors(boolean enabled) {
            this.errors = enabled;
            return this;
        }

        public Builder client(boolean enabled) {
            this.client = enabled;
            return this;
        }

        public Builder traces(boolean enabled) {
            this.traces = enabled;
            return this;
        }

        public Builder tracesSessionId(boolean enabled) {
            this.tracesSessionId = enabled;
            return this;
        }

        public Builder routeCardinalityLimit(int limit) {
            if (limit < 1) {
                throw new IllegalArgumentException(
                        "routeCardinalityLimit must be >= 1, got " + limit);
            }
            this.routeCardinalityLimit = limit;
            return this;
        }

        public Builder clientRatePerSession(int rate) {
            if (rate < 0) {
                throw new IllegalArgumentException(
                        "clientRatePerSession must be >= 0, got " + rate);
            }
            this.clientRatePerSession = rate;
            return this;
        }

        public VaadinMetricsConfig build() {
            return new VaadinMetricsConfig(this);
        }
    }
}
