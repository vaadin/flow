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

import org.junit.jupiter.api.Test;

/**
 * The changes a redefine cannot make live, and the reason each one gives.
 * <p>
 * Both cases here are ones that used to be reported as live: the JVM accepts
 * the redefine, and what the application built from the old class at startup -
 * a bean's proxy, an ORM's metamodel - silently no longer matches.
 */
class DevLoopRestartIT extends AbstractDevLoopIT {

    private static final Path VIEW = MUTABLE.resolve("TaskListView.java");

    @Test
    void newMemberOnAProxiedBean_restartsAndStaysUp() {
        patch.addMember(MUTABLE.resolve("TaskService.java"),
                "\n    public String added() {\n        return \"added\";\n    }\n");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        outcome.assertOutputContains("restarting");
        outcome.assertOutputContains("Stable");
        // Not just that it restarted, but why: "restarting" with no reason is
        // indistinguishable from the daemon deciding to restart on a whim.
        outcome.assertOutputContains("restart:");

        cli.run("status").assertExitCode(0).assertOutputContains("running");
    }

    @Test
    void addingAnEntityAnnotation_escalatesEvenThoughTheClassRedefines() {
        // A class-level annotation is an attribute rather than a member, so the
        // redefine is accepted and the class really does come back carrying
        // @Entity. Hibernate mapped neither version, though - the metamodel and
        // schema were fixed at startup - so this has to escalate. Asked only of
        // the class the application is running, the annotation is not there
        // yet, and the apply reports as live the very change that is not.
        patch.replace(VIEW, "public class TaskListView extends Div {",
                "@jakarta.persistence.Entity\npublic class TaskListView extends Div {");

        // --no-restart stops at the verdict, which is what is under test; the
        // restart itself is the case above.
        VaadinDevCli.Outcome outcome = cli
                .run("apply", "--no-restart", "--json").assertExitCode(0);

        outcome.assertOutputContains(
                "entity mapping cannot hot reload (TaskListView)");
    }

    @Test
    void restart_bringsTheAppBackOnTheSamePort() {
        cli.run("restart").assertExitCode(0);

        cli.run("status").assertExitCode(0).assertOutputContains("running")
                .assertOutputContains("registered=true");
    }
}
