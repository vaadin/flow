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
package com.vaadin.flow.test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Delays the application readiness signal until the frontend dev server can
 * actually serve the application.
 * <p>
 * The Spring context is up long before dev mode is: the dev mode updaters
 * install the npm packages and start Vite on background threads, which on a
 * cold machine takes minutes. Since {@code spring-boot:start} returns as soon
 * as the application reports itself ready, the integration tests would
 * otherwise open browsers against a server that answers with the "dev mode not
 * ready" placeholder page, and against a Vite that is still pre-bundling
 * dependencies and therefore leaves requests hanging until the dev server proxy
 * read timeout expires.
 * <p>
 * Runners execute before the readiness signal is published, so blocking here
 * keeps {@code spring-boot:start} waiting, which in turn keeps the integration
 * tests from starting too early. Running the application manually benefits the
 * same way: it reports being ready only once it can be used.
 */
@Component
class DevServerReadyGate implements ApplicationRunner {

    /**
     * How long to wait in total. A cold CI agent needs a couple of minutes for
     * the npm install and the Vite dependency pre-bundling, so the limit is
     * only there to fail with a clear message instead of hanging forever.
     */
    private static final Duration TIMEOUT = Duration.ofMinutes(10);

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    /**
     * Requests are given more time than the dev server proxy read timeout
     * (currently two minutes), so that a slow Vite surfaces as a server
     * response to retry instead of a client side abort.
     */
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(3);

    /**
     * Flow client entry point, served by proxying to the dev server. Fetching
     * it proves Vite answers module requests, which starting up does not: the
     * dependency optimizer keeps requests waiting for a while after Vite itself
     * reports being ready.
     */
    private static final String CLIENT_ENTRY_PATH = "/VAADIN/generated/jar-resources/FlowClient.js";

    private final Environment environment;

    DevServerReadyGate(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws InterruptedException {
        String rootUrl = "http://localhost:" + port();
        Instant deadline = Instant.now().plus(TIMEOUT);
        Instant started = Instant.now();

        try (HttpClient client = HttpClient.newHttpClient()) {
            awaitDevServerStarted(client, rootUrl, deadline);
            awaitClientEntryServed(client, rootUrl, deadline);
        }

        getLogger().info("Frontend dev server ready in {} ms",
                Duration.between(started, Instant.now()).toMillis());
    }

    /**
     * Polls until the dev server process has been started, using the same
     * lightweight protocol as the placeholder page: a request carrying the
     * {@code X-DevModePoll} header is answered without creating a UI, and
     * carries the {@code X-DevModePending} header while the dev server is still
     * starting.
     */
    private void awaitDevServerStarted(HttpClient client, String rootUrl,
            Instant deadline) throws InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(rootUrl + "/"))
                .header("X-DevModePoll", "true").timeout(REQUEST_TIMEOUT)
                .build();
        while (true) {
            HttpResponse<String> response = send(client, request);
            if (response != null && !response.headers()
                    .firstValue("X-DevModePending").isPresent()) {
                // A failure to start is reported as preformatted console output
                // instead of the "Ready" body, and no amount of waiting fixes
                // it.
                if (response.body().startsWith("<pre>")) {
                    throw new IllegalStateException(
                            "The frontend dev server failed to start: "
                                    + response.body());
                }
                return;
            }
            sleepUntilNextPoll(deadline,
                    "the frontend dev server did not start");
        }
    }

    /**
     * Polls until the dev server serves the Flow client, which it does not do
     * while it is pre-bundling dependencies. Absorbing that wait here means the
     * first browsers do not have to.
     */
    private void awaitClientEntryServed(HttpClient client, String rootUrl,
            Instant deadline) throws InterruptedException {
        HttpRequest request = HttpRequest
                .newBuilder(URI.create(rootUrl + CLIENT_ENTRY_PATH))
                .timeout(REQUEST_TIMEOUT).build();
        while (true) {
            HttpResponse<String> response = send(client, request);
            if (response != null && response.statusCode() == 200) {
                return;
            }
            sleepUntilNextPoll(deadline,
                    "the frontend dev server did not serve "
                            + CLIENT_ENTRY_PATH);
        }
    }

    /**
     * Sends the given request, treating a failure as a reason to poll again
     * rather than as an error: the server is being contacted while it is
     * starting up, so refused connections and timeouts are expected.
     *
     * @return the response, or {@code null} if the request did not complete
     */
    private HttpResponse<String> send(HttpClient client, HttpRequest request)
            throws InterruptedException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            getLogger().debug("Waiting for the frontend dev server: {}",
                    e.getMessage());
            return null;
        }
    }

    private void sleepUntilNextPoll(Instant deadline, String failureMessage)
            throws InterruptedException {
        if (Instant.now().isAfter(deadline)) {
            throw new IllegalStateException(
                    failureMessage + " within " + TIMEOUT.toMinutes()
                            + " minutes. See the log above for the reason.");
        }
        Thread.sleep(POLL_INTERVAL);
    }

    /**
     * Resolves the port the application listens on, preferring the port Spring
     * reports for a randomized {@code server.port}.
     */
    private String port() {
        return environment.getProperty("local.server.port",
                environment.getProperty("server.port", "8080"));
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DevServerReadyGate.class);
    }
}
