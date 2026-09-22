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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The claim this module exists to make: a WAR whose container is installed,
 * started and fed a deployed copy of itself by a build plugin is started and
 * owned by the daemon like any other application.
 * <p>
 * {@code test-devloop-jetty} covers a container that runs inside the build's
 * own JVM. Everything below is about the difference: Cargo forks, so the
 * application is a grandchild of the daemon, and neither {@code MAVEN_OPTS} nor
 * anything on Maven's command line reaches its JVM.
 */
class DevLoopCargoStartIT extends AbstractDevLoopIT {

    @Test
    void theRuntimeIsChosenFromThePluginTheBuildRuns() {
        // Not from the packaging, which is `war` here as it is for Jetty, and
        // not from a setting: the pom runs cargo-maven3-plugin and that is the
        // whole of the answer.
        cli.run("status").assertExitCode(0).assertOutputContains("cargo");
    }

    @Test
    void theApplicationIsServingFromTheContainerCargoInstalled()
            throws Exception {
        String page = fetch("/");

        // Vaadin's bootstrap page, served by a Tomcat that was downloaded,
        // unpacked and started by the run goal rather than by anything on this
        // project's classpath.
        assertTrue(page.contains("window.Vaadin"),
                () -> "expected the Vaadin bootstrap page, got: " + page);
    }

    /**
     * The one thing this runtime cannot work without.
     * <p>
     * No parameter of {@code cargo:run} that could carry the loop's JVM flags
     * has a user property, so the daemon asks its build extension to put
     * {@code cargo.start.jvmargs} on the model. If that channel were ever
     * broken the application would still start and still serve - it would just
     * carry no agents, and every apply would restart. This is what says the
     * agents arrived.
     */
    @Test
    void theAgentsReachedTheForkedContainer() {
        patch.replace(VIEW, "\"Task List\"", "\"Task List, agent check\"");

        cli.run("apply").assertExitCode(0).assertOutputContains("hot-reload:")
                .assertOutputDoesNotContain("restarting");
    }

    /**
     * A goal named on a Maven command line runs on every project in the
     * reactor, and the loop names one with {@code -pl :app -am}. Jetty's mojo
     * supports {@code war} packaging alone and skips the rest; Cargo's fails
     * the build on the first module that is not a WAR. The daemon switches the
     * goal off reactor-wide and switches it back on for this module, and a
     * container that came up at all is what proves both halves worked.
     */
    @Test
    void theRunGoalWasKeptToThisModule() {
        cli.run("status").assertExitCode(0).assertOutputContains("running");

        Path installed = APP.resolve("target/cargo-server/conf/server.xml");
        assertTrue(Files.isRegularFile(installed),
                () -> "expected a container configured under " + installed);
    }
}
