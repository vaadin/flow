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
 * What a resource edit can and cannot do when the container reads a deployed
 * copy of the WAR.
 * <p>
 * {@link DevLoopCssIT} is excluded for this module, and this is what replaces
 * it. That test fetches the sibling module's stylesheet over HTTP after an
 * apply, which works wherever the webapp class path <em>is</em> the modules'
 * own output - Jetty in {@code EMBED} mode, and Spring Boot from the classpath.
 * Here the sibling arrives inside a jar in a WAR that Cargo copied into
 * Tomcat's {@code webapps}, so refreshing {@code devloop-shared/target/classes}
 * - the whole of what an apply can do for a resource - never reaches what the
 * container serves. Only a restart, which rebuilds and redeploys the WAR, can.
 * <p>
 * That is a property of deploying a packaged WAR rather than a defect in the
 * loop, and it holds for WildFly and TomEE too. The class half of the same leg
 * is unaffected, because a redefine acts on a loaded class whatever loaded it;
 * {@code DevLoopMultiModuleIT} is what says so.
 */
class DevLoopCargoResourceIT extends AbstractDevLoopIT {

    @Test
    void aStylesheetEditIsPushedAndRefreshesTheClasspathCopy()
            throws Exception {
        patch.replace(STYLESHEET, "row-gap: 12px", "row-gap: 44px");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        // The same answer the other fixtures get: a stylesheet is a live
        // resource, so it is copied and pushed rather than restarted for.
        outcome.assertOutputContains("hmr:");
        outcome.assertOutputDoesNotContain("restarting");

        Path copy = SHARED.resolve("target/classes/META-INF/resources")
                .resolve("task-list.css");
        assertTrue(Files.readString(copy).contains("row-gap: 44px"),
                () -> "expected the copied stylesheet at " + copy
                        + " to hold the edited value");
    }

    /**
     * And the application's own stylesheet, which no servlet container serves:
     * {@code src/main/resources} is packaged into {@code WEB-INF/classes}, and
     * the servlet spec has no container serve from there. The classpath copy is
     * what is observable, and it is the part the daemon is responsible for.
     */
    @Test
    void theApplicationsOwnStylesheetIsCopiedToo() throws Exception {
        Path source = APP
                .resolve("src/main/resources/META-INF/resources/styles.css");
        patch.replace(source, "margin: 0", "margin: 4px");

        cli.run("apply").assertExitCode(0).assertOutputContains("styles.css");

        Path copy = APP.resolve("target/classes/META-INF/resources")
                .resolve("styles.css");
        assertTrue(Files.readString(copy).contains("margin: 4px"),
                () -> "expected the copied stylesheet at " + copy
                        + " to hold the edited value");
    }

    /**
     * The fix that made this fixture usable: a build regenerates resources
     * whether or not anything about them changed, and the loop used to read a
     * fresh modification time as a change. Nine rewritten-but-identical
     * {@code .d.ts} files under {@code vaadin-dev-server} restarted the
     * application on every apply, over an edit that was one method body.
     */
    @Test
    void aJavaOnlyEditIsNotDraggedIntoARestartByRegeneratedResources() {
        patch.replace(VIEW, "\"Task List\"", "\"Task List, java only\"");

        cli.run("apply").assertExitCode(0).assertOutputContains("hot-reload:")
                .assertOutputDoesNotContain("restarting");
    }
}
