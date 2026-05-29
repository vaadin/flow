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
package com.vaadin.flow.micrometer.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.vaadin.flow.micrometer.VaadinMetricsConfig;

/**
 * Boot-bound configuration properties under the {@code vaadin.metrics} prefix.
 * Converted to a plain {@link VaadinMetricsConfig} via {@link #toConfig()}.
 */
@ConfigurationProperties(prefix = "vaadin.metrics")
public class VaadinMetricsProperties {

    private boolean enabled = true;
    private boolean sessions = true;
    private boolean uis = true;
    private boolean navigation = true;
    private boolean requests = true;
    private boolean errors = true;
    private boolean client = true;
    private boolean traces = true;
    private boolean tracesSessionId = false;
    private int routeCardinalityLimit = VaadinMetricsConfig.DEFAULT_ROUTE_CARDINALITY_LIMIT;
    private int clientRatePerSession = VaadinMetricsConfig.DEFAULT_CLIENT_RATE_PER_SESSION;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSessions() {
        return sessions;
    }

    public void setSessions(boolean sessions) {
        this.sessions = sessions;
    }

    public boolean isUis() {
        return uis;
    }

    public void setUis(boolean uis) {
        this.uis = uis;
    }

    public boolean isNavigation() {
        return navigation;
    }

    public void setNavigation(boolean navigation) {
        this.navigation = navigation;
    }

    public boolean isRequests() {
        return requests;
    }

    public void setRequests(boolean requests) {
        this.requests = requests;
    }

    public boolean isErrors() {
        return errors;
    }

    public void setErrors(boolean errors) {
        this.errors = errors;
    }

    public boolean isClient() {
        return client;
    }

    public void setClient(boolean client) {
        this.client = client;
    }

    public boolean isTraces() {
        return traces;
    }

    public void setTraces(boolean traces) {
        this.traces = traces;
    }

    public boolean isTracesSessionId() {
        return tracesSessionId;
    }

    public void setTracesSessionId(boolean tracesSessionId) {
        this.tracesSessionId = tracesSessionId;
    }

    public int getRouteCardinalityLimit() {
        return routeCardinalityLimit;
    }

    public void setRouteCardinalityLimit(int routeCardinalityLimit) {
        this.routeCardinalityLimit = routeCardinalityLimit;
    }

    public int getClientRatePerSession() {
        return clientRatePerSession;
    }

    public void setClientRatePerSession(int clientRatePerSession) {
        this.clientRatePerSession = clientRatePerSession;
    }

    public VaadinMetricsConfig toConfig() {
        return VaadinMetricsConfig.builder().sessions(sessions).uis(uis)
                .navigation(navigation).requests(requests).errors(errors)
                .client(client).traces(traces).tracesSessionId(tracesSessionId)
                .routeCardinalityLimit(routeCardinalityLimit)
                .clientRatePerSession(clientRatePerSession).build();
    }
}
