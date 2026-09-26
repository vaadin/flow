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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What {@code apply} does to an ordinary edit, in any shape of application.
 * <p>
 * Nothing here is about a launcher: a method body is what a stock JVM accepts
 * whoever started the JVM, and a compile error is a compile error. What each
 * fixture adds on top of this - a bean's proxy, a webapp class loader - is in
 * its own subclass.
 */
class DevLoopApplyIT extends AbstractDevLoopIT {

    @Test
    void nothingEdited_reportsNoChanges() {
        // A bare "no changes" is right only when nothing was examined and
        // found not to matter, which is exactly this case.
        cli.run("apply").assertExitCode(0).assertOutputContains("no changes");
    }

    @Test
    void methodBodyEdit_isHotSwappedRatherThanRestarted() {
        patch.replace(VIEW, "\"Task List\"", "\"Tasks, applied\"");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        // The change-set names the file, so a reader sees what was picked up.
        outcome.assertOutputContains("TaskListView.java");
        // A method body is what a stock JVM accepts, so this must not restart.
        outcome.assertOutputContains("hot-reload:");
        outcome.assertOutputDoesNotContain("restarting");
    }

    @Test
    void aClassEditedTwiceOverStaysAHotSwapBothTimes() {
        // Two edits in a row, which is the shape that catches a "has the
        // application ever had this class?" answer built out of timestamps:
        // the first apply rewrites the class file, and a second apply that
        // reads only "this class file is newer than the launch" then mistakes
        // the application's own class for one it has never scanned - and
        // restarts for it.
        patch.replace(SERVICE, "\"Write the plan\"",
                "\"Write the plan, once\"");
        cli.run("apply").assertExitCode(0).assertOutputContains("hot-reload:")
                .assertOutputDoesNotContain("restarting");

        patch.replace(SERVICE, "\"Write the plan, once\"",
                "\"Write the plan, twice\"");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        outcome.assertOutputContains("hot-reload:");
        outcome.assertOutputDoesNotContain("restarting");
        // The reason line a restart would carry. Asserted separately from
        // "restarting" because it is the half that names what the daemon
        // thought it had found - a new bean, a changed import - and a failure
        // here is far easier to read with that in it.
        outcome.assertOutputDoesNotContain("restart:");
    }

    @Test
    void addingAnInterface_escalatesAlthoughNothingDeclaredChanged() {
        // The class declares exactly what it declared before - same members,
        // same annotations - so every signal the connector compares comes out
        // identical, and an enhanced-redefinition JVM accepts the new
        // hierarchy. What an interface can bring with it is the frontend
        // annotations it carries: @JsModule and friends are @Inherited across
        // classes and live on interfaces too, and they are read into
        // generated-flow-imports.js at startup. Reported hot-reload, the page
        // would run against a bundle with no chunk for the new import.
        patch.replace(SERVICE, "public class TaskService {",
                "public class TaskService implements java.io.Serializable {");

        VaadinDevCli.Outcome outcome = cli
                .run("apply", "--no-restart", "--json").assertExitCode(0);

        outcome.assertOutputContains("class hierarchy changed (TaskService)");
        outcome.assertOutputDoesNotContain("\"classification\":\"hot-reload\"");
    }

    @Test
    void compileError_failsWithADiagnosticAndKeepsTheAppRunning() {
        patch.replace(VIEW, "return \"Task List\";", "return nope();");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(1);

        outcome.assertOutputContains("Failed");
        // File, line and column, because that is what makes a diagnostic
        // actionable rather than just red.
        assertTrue(
                outcome.output()
                        .matches("(?s).*TaskListView\\.java:\\d+:\\d+.*"),
                () -> "expected a file:line:column diagnostic, got:\n"
                        + outcome.output());

        // The application keeps its last good bytes: a failed compile is not a
        // reason for the app to be down - and under a build-plugin runtime,
        // taking it down would cost a full Maven start to get back.
        cli.run("status").assertExitCode(0).assertOutputContains("running");
    }
}
