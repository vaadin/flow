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
package com.vaadin.flow.devloop.test.it;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base for every dev-loop IT: one CLI, one patch set per test, and an
 * application that is running before the first assertion.
 * <p>
 * Every fixture under {@code test-devloop} has the same shape, and the
 * constants below are what that means in practice: an application module with a
 * {@code mutable} package the ITs rewrite, and a sibling library beside it in
 * {@code ../devloop-shared} carrying a class and a stylesheet. Keeping the two
 * fixtures identical down to the file names is what lets the tests that are
 * about the loop - rather than about a servlet container or a Spring context -
 * be written once, here, and merely extended in each module.
 * <p>
 * {@link Isolated} and each module's single-threaded failsafe configuration are
 * both deliberate: there is one daemon, one application process and one HTTP
 * port per fixture, so two of these running at once would be measuring each
 * other.
 * <p>
 * The daemon is deliberately <em>not</em> shut down between tests. Its whole
 * value is that it survives, and a start costs tens of seconds; the idle
 * watchdog reaps it when the run is over.
 */
@Isolated
abstract class AbstractDevLoopIT {

    /** Where the application module is, as the CLI needs it. */
    static final Path APP = Path.of(System.getProperty("user.dir"))
            .toAbsolutePath().normalize();

    /** The sibling library, which several tests edit. */
    static final Path SHARED = APP.getParent().resolve("devloop-shared");

    /** The package the ITs rewrite, and the only one they ever touch. */
    static final Path MUTABLE = APP
            .resolve("src/main/java/com/vaadin/flow/devloop/test/app/mutable");

    /** The view: a method body to hot swap, and a class to break. */
    static final Path VIEW = MUTABLE.resolve("TaskListView.java");

    /**
     * A plain collaborator of the view, edited where a second class is needed.
     */
    static final Path SERVICE = MUTABLE.resolve("TaskService.java");

    /** The sibling library's class, for the cross-module leg. */
    static final Path FORMATTER = SHARED.resolve(
            "src/main/java/com/vaadin/flow/devloop/test/shared/DueDateFormatter.java");

    /** The sibling library's stylesheet, for the cross-module resource leg. */
    static final Path STYLESHEET = SHARED
            .resolve("src/main/resources/META-INF/resources/task-list.css");

    /** The port the application serves on. */
    static final int SERVER_PORT = serverPort();

    protected VaadinDevCli cli;

    protected SourcePatch patch;

    @BeforeEach
    void startTheApplication() {
        cli = VaadinDevCli.of(APP);
        patch = new SourcePatch();
        // Idempotent: `start` on a running app answers "already running" with
        // exit 0, so this is also how a test after the first one begins.
        cli.run("start").assertExitCode(0);
        // Whatever a previous test left pending is not this test's change-set.
        cli.run("apply").assertExitCode(0);
    }

    @AfterEach
    void revertTheSources() {
        // Revert first, then apply, so the next test starts from a tree that
        // matches what the running application holds.
        patch.close();
        if (cli != null) {
            cli.run("apply");
        }
    }

    protected String rootUrl() {
        return "http://localhost:" + SERVER_PORT;
    }

    /**
     * GETs a path from the running application.
     *
     * @param path
     *            the path, leading slash included
     * @return the response, status included, for a test that cares which it was
     */
    protected HttpResponse<String> get(String path)
            throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest
                    .newBuilder(URI.create(rootUrl() + path)).build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }

    /**
     * GETs a path and returns its body, failing the test if the server did not
     * answer 200.
     *
     * @param path
     *            the path, leading slash included
     * @return the response body
     */
    protected String fetch(String path)
            throws IOException, InterruptedException {
        HttpResponse<String> response = get(path);
        assertEquals(200, response.statusCode(),
                () -> "GET " + path + " returned " + response.statusCode());
        return response.body();
    }

    /**
     * The port from failsafe, or the one this module's pom configures.
     * <p>
     * The root pom passes {@code -DserverPort=${server.port}} into every IT, so
     * a Maven run always has it. A test run straight from an IDE has neither
     * that nor a default worth hard-coding here - the fixtures deliberately do
     * not share a port, so a constant would be right for one of them and
     * silently wrong for the other - and the module's own pom is where the
     * answer already is.
     */
    private static int serverPort() {
        String configured = System.getProperty("serverPort");
        if (configured != null && !configured.isBlank()) {
            return Integer.parseInt(configured.trim());
        }
        Path pom = APP.resolve("pom.xml");
        try {
            Matcher declared = Pattern
                    .compile("<server\\.port>\\s*(\\d+)\\s*</server\\.port>")
                    .matcher(Files.readString(pom));
            if (declared.find()) {
                return Integer.parseInt(declared.group(1));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + pom, e);
        }
        throw new IllegalStateException("no -DserverPort and no <server.port> "
                + "in " + pom + ": one of the two has to say which port the "
                + "application serves on");
    }
}
