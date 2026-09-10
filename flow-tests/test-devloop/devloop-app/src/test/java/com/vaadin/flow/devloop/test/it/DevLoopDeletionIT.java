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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deleting a source is the one edit that writes no bytes for a walk to find, so
 * it is the one an artifact comparison cannot see at all. Left undetected it is
 * the worst answer this daemon can give: {@code no changes}, exit 0, over a
 * route that is still answering out of a {@code .class} nobody deleted.
 */
class DevLoopDeletionIT extends AbstractDevLoopIT {

    private static final Path DOOMED = MUTABLE.resolve("DoomedView.java");

    private static final Path DOOMED_CLASS = APP.resolve("target")
            .resolve("classes").resolve("com").resolve("vaadin").resolve("flow")
            .resolve("devloop").resolve("test").resolve("app")
            .resolve("mutable").resolve("DoomedView.class");

    @Test
    void deletedRoute_losesItsClassAndRestartsInsteadOfReportingNoChanges() {
        patch.create(DOOMED, """
                package com.vaadin.flow.devloop.test.app.mutable;

                import com.vaadin.flow.component.html.Div;
                import com.vaadin.flow.router.Route;

                /** Created by DevLoopDeletionIT and deleted again by it. */
                @Route("doomed")
                public class DoomedView extends Div {
                    public DoomedView() {
                        setText("doomed");
                    }
                }
                """);
        // A route is read at startup, so this comes up by restarting - which is
        // also what puts the new source into the daemon's inventory.
        cli.run("apply").assertExitCode(0);
        assertTrue(Files.isRegularFile(DOOMED_CLASS),
                "the fixture has to be compiled before it can be deleted");

        patch.delete(DOOMED);
        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        outcome.assertOutputContains("DoomedView.java (deleted)");
        outcome.assertOutputDoesNotContain("no changes");
        // A JVM cannot un-define a loaded class, so nothing short of a restart
        // makes the route actually gone.
        outcome.assertOutputContains("restarting");
        outcome.assertOutputContains("Stable");
        // And the artifact has to go with it, or the restart would load the
        // route straight back off the classpath.
        assertFalse(Files.exists(DOOMED_CLASS),
                "the class of a deleted source must not survive the apply");

        cli.run("status").assertExitCode(0).assertOutputContains("running");
    }
}
