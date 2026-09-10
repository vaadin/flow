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

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * Base for the dev-loop ITs: one CLI, one patch set per test, and an
 * application that is running before the first assertion.
 * <p>
 * {@link Isolated} and the module's single-threaded failsafe configuration are
 * both deliberate: there is one daemon, one application process and one HTTP
 * port, so two of these running at once would be measuring each other.
 * <p>
 * The daemon is deliberately <em>not</em> shut down between tests. Its whole
 * value is that it survives, and a start costs about thirty seconds; the idle
 * watchdog reaps it when the run is over.
 */
@Isolated
abstract class AbstractDevLoopIT {

    /** Where the application module is, as the CLI needs it. */
    static final Path APP = Path.of(System.getProperty("user.dir"))
            .toAbsolutePath().normalize();

    /** The sibling library, which several tests edit. */
    static final Path SHARED = APP.getParent().resolve("devloop-shared");

    static final Path MUTABLE = APP
            .resolve("src/main/java/com/vaadin/flow/devloop/test/app/mutable");

    /**
     * The port the application serves on. The Maven build passes it in from
     * {@code <server.port>}; the default matches
     * {@code src/main/resources/application.properties} for a hand-run test.
     */
    static final int SERVER_PORT = Integer
            .parseInt(System.getProperty("serverPort", "8899"));

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
}
