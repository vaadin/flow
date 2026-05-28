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

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteConfiguration;

/**
 * Maps a Flow navigation target to a low-cardinality tag value suitable for
 * Micrometer. Resolves the route template (e.g. {@code users/:id}) rather than
 * the resolved URL, then enforces an upper bound on the number of distinct
 * values it will admit; further values are bucketed under
 * {@link MeterNames#ROUTE_OTHER}.
 */
final class RouteTagResolver {

    private final int limit;
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    RouteTagResolver(int limit) {
        this.limit = limit;
    }

    /**
     * Resolves the tag value for a navigation target. {@code null} input is
     * treated as an unknown route.
     */
    String tagFor(Class<? extends Component> navigationTarget) {
        if (navigationTarget == null) {
            return MeterNames.ROUTE_UNKNOWN;
        }
        String template = resolveTemplate(navigationTarget)
                .orElseGet(navigationTarget::getSimpleName);
        if (seen.contains(template)) {
            return template;
        }
        if (seen.size() < limit) {
            seen.add(template);
            return template;
        }
        return MeterNames.ROUTE_OTHER;
    }

    private Optional<String> resolveTemplate(
            Class<? extends Component> navigationTarget) {
        try {
            return RouteConfiguration.forSessionScope()
                    .getTemplate(navigationTarget);
        } catch (RuntimeException ignored) {
            // No current session bound; fall back to simple name.
            return Optional.empty();
        }
    }

    int trackedCount() {
        return seen.size();
    }
}
