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
package com.vaadin.flow.micrometer.spring;

import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.web.filter.ServerHttpObservationFilter;

import com.vaadin.flow.server.VaadinRequest;

/**
 * Lifts Vaadin request information into Spring's HTTP server observation (the
 * {@code http <method> <uri>} span emitted by
 * {@link ServerHttpObservationFilter}). This makes the parent HTTP span render
 * as e.g. {@code http post /vaadin/uidl} instead of the generic
 * {@code http post /**} so the request type is visible in the trace UI without
 * having to drill into the child {@code vaadin.request.<type>} span.
 * <p>
 * Lives in {@code vaadin-micrometer-spring} (rather than the framework-agnostic
 * base module) so Spring's HTTP observation classes can be imported directly
 * and we don't need reflection.
 */
public final class SpringHttpObservationEnricher {

    private SpringHttpObservationEnricher() {
    }

    /**
     * Enriches the Spring HTTP observation attached to {@code request}, if any.
     * Best-effort: silently no-ops if Spring's filter didn't run (e.g.
     * non-Spring-MVC deployment) or the request isn't a servlet request.
     */
    public static void enrich(VaadinRequest request, String type) {
        if (request == null || type == null) {
            return;
        }
        Object ctx = request.getAttribute(
                ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE);
        if (!(ctx instanceof ServerRequestObservationContext src)) {
            return;
        }
        try {
            src.setPathPattern("/vaadin/" + type);
            String method = request.getMethod();
            src.setContextualName(
                    "http " + (method == null ? "?" : method.toLowerCase())
                            + " vaadin " + type);
        } catch (RuntimeException ignored) {
            // best-effort
        }
    }
}
