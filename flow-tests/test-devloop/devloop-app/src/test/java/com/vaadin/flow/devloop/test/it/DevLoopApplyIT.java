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
 * {@code apply} makes a view edit live without a reload, and says nothing at
 * all when there is nothing to say.
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
        patch.replace(MUTABLE.resolve("TaskListView.java"), "\"Task List\"",
                "\"Tasks, applied\"");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        // The change-set names the file, so a reader sees what was picked up.
        outcome.assertOutputContains("TaskListView.java");
        // A method body is what a stock JVM accepts, so this must not restart.
        outcome.assertOutputContains("hot-reload:");
        outcome.assertOutputDoesNotContain("restarting");
    }

    @Test
    void compileError_failsWithADiagnosticAndKeepsTheAppRunning() {
        patch.replace(MUTABLE.resolve("TaskListView.java"),
                "return \"Task List\";", "return nope();");

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
        // reason for the app to be down.
        cli.run("status").assertExitCode(0).assertOutputContains("running");
    }

    @Test
    void noRestart_stopsAfterTheCompileGate() {
        patch.addMember(MUTABLE.resolve("TaskService.java"),
                "\n    public int added() {\n        return 1;\n    }\n");

        // Structural, so the only way to make it live is a restart - which
        // --no-restart forbids. The honest answer is "compiled", not "Stable".
        cli.run("apply", "--no-restart").assertExitCode(0)
                .assertOutputContains("compiled")
                .assertOutputDoesNotContain("restarting");
    }

    @Test
    void statusJson_isASingleParseableObject() {
        VaadinDevCli.Outcome outcome = cli.run("status", "--json")
                .assertExitCode(0);

        String json = outcome.output().strip();
        assertTrue(json.startsWith("{") && json.endsWith("}"),
                () -> "expected one JSON object and no progress lines, got:\n"
                        + json);
        outcome.assertOutputContains("\"state\":\"running\"");
    }
}
