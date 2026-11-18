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
package com.vaadin.flow.quarkus.test.servlet;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Finds out whether a Vaadin servlet is mapped to a path, without needing a
 * frontend to render a page.
 * <p>
 * Without a session, a Vaadin servlet answers a heartbeat with 403 "Session
 * expired". Where no Vaadin servlet is mapped, the Undertow default servlet,
 * which only serves static files, answers the POST with 405.
 */
final class VaadinHeartbeat {

    static final int HANDLED_BY_VAADIN = 403;
    static final int HANDLED_BY_DEFAULT_SERVLET = 405;

    private VaadinHeartbeat() {
    }

    static int sendHeartbeat(URI path) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest
                    .newBuilder(path.resolve("?v-r=heartbeat&v-uiId=0"))
                    .POST(HttpRequest.BodyPublishers.noBody()).build();
            return client.send(request, HttpResponse.BodyHandlers.discarding())
                    .statusCode();
        }
    }
}
