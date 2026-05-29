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
package com.vaadin.flow.micrometer.client;

import java.util.Set;

import com.vaadin.flow.micrometer.MeterNames;

/**
 * Allowlist of client-emitted meter names. Samples whose names are not in this
 * set are dropped at ingest time, capping cardinality from malicious or buggy
 * clients.
 */
final class ClientMetricNames {

    static final Set<String> ALLOWED = Set.of(
            MeterNames.CLIENT_BOOTSTRAP_DURATION,
            MeterNames.CLIENT_NAVIGATION_DURATION,
            MeterNames.CLIENT_RPC_DURATION, MeterNames.CLIENT_WEB_VITALS_LCP,
            MeterNames.CLIENT_WEB_VITALS_FCP, MeterNames.CLIENT_ERRORS);

    static final Set<String> COUNTER_NAMES = Set.of(MeterNames.CLIENT_ERRORS);

    static boolean isAllowed(String name) {
        return name != null && ALLOWED.contains(name);
    }

    static boolean isCounter(String name) {
        return COUNTER_NAMES.contains(name);
    }

    private ClientMetricNames() {
    }
}
