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
 * The frontend folder is a source tree like any other: an edit under it belongs
 * in the change-set, and the outcome says how - or whether - it went live.
 * <p>
 * This application runs in dev-bundle mode ({@code hotdeploy=false}), which is
 * the mode with something to decide. A theme stylesheet is pushed in place;
 * anything the bundle would have to be rebuilt for escalates to a restart. Vite
 * mode has nothing to decide - Vite applied the edit when the file was saved -
 * and is covered by {@code FrontendTest} in the daemon plus the manual sequence
 * in {@code flow-devloop-daemon/README.md}, because switching this module to it
 * would cost a second daemon, a second port and a pnpm install.
 * <p>
 * What is asserted here is the daemon's <em>decision</em>. The bundle rebuild
 * that a real restart triggers is Flow's own {@code needsBuild} /
 * {@code TaskRunDevBundleBuild}, tested upstream and far too slow to run here -
 * so the escalation is read from {@code apply --no-restart --json}, which
 * reaches the same verdict in milliseconds.
 */
class DevLoopFrontendIT extends AbstractDevLoopIT {

    private static final Path FRONTEND = APP.resolve("src/main/frontend");

    private static final Path THEME_CSS = FRONTEND
            .resolve("themes/devloop/styles.css");

    private static final Path IMPORTED_CSS = FRONTEND
            .resolve("themes/devloop/imported.css");

    private static final Path BUNDLED_TS = FRONTEND
            .resolve("mutable-greeting.ts");

    private static final Path GENERATED = FRONTEND
            .resolve("generated/vaadin.ts");

    private static final Path VIEW = MUTABLE.resolve("TaskListView.java");

    @Test
    void themeCssEdit_isPushedWithoutARestart() {
        patch.replace(THEME_CSS, "padding: 8px;", "padding: 21px;");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        outcome.assertOutputContains("theme file(s) pushed in place");
        // A stylesheet cannot break a class: nothing here may restart the app
        // or recompile anything.
        outcome.assertOutputDoesNotContain("restarting");
        outcome.assertOutputDoesNotContain("compiling");
        outcome.assertOutputContains("src/main/frontend/themes/devloop/styles.css");
    }

    @Test
    void themeImportEdit_isPushedAsPartOfTheWholeTheme() {
        // The update is styles.css with its imports inlined, so editing an
        // imported file has to reach the browser as an update to the theme
        // rather than as nothing at all.
        patch.replace(IMPORTED_CSS, "font-weight: 600;", "font-weight: 800;");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        outcome.assertOutputContains("theme file(s) pushed in place");
        outcome.assertOutputDoesNotContain("restarting");
    }

    @Test
    void bundledFrontendEdit_escalatesToARestart() {
        patch.replace(BUNDLED_TS, "hello from the frontend",
                "hei from the frontend");

        // --no-restart stops at the verdict, so this asserts the decision
        // without paying for the Vite build the real restart would trigger.
        VaadinDevCli.Outcome outcome = cli.run("apply", "--no-restart",
                "--json").assertExitCode(0);

        outcome.assertOutputContains(
                "\"escalation\":\"frontend changed (dev bundle rebuild)\"");
        outcome.assertOutputContains("mutable-greeting.ts");
    }

    @Test
    void addingAJsModuleAnnotation_escalatesEvenThoughTheClassRedefines() {
        // The class redefines cleanly and really does carry the new
        // annotation - but @JsModule is read by the build, not at runtime: it
        // goes into generated-flow-imports.js at startup and reaches the
        // browser through a bundle chunk. Reporting hot-reload here would be
        // reporting a change live that is not, and no frontend file changed on
        // disk for the frontend leg to notice.
        patch.replace(VIEW, "@Route(\"\")",
                "@Route(\"\")\n@com.vaadin.flow.component.dependency.JsModule(\"./mutable-greeting.ts\")");

        VaadinDevCli.Outcome outcome = cli
                .run("apply", "--no-restart", "--json").assertExitCode(0);

        outcome.assertOutputContains("frontend imports changed (TaskListView)");
        outcome.assertOutputDoesNotContain("\"classification\":\"hot-reload\"");
    }

    @Test
    void anOrdinaryJavaEdit_stillHotSwaps() {
        // The guard against over-restarting: only a change to the frontend
        // annotations escalates, not any edit to a class that has some.
        patch.replace(VIEW, "addClassName(\"task-list-view\")",
                "addClassName(\"task-list-view-renamed\")");

        cli.run("apply").assertExitCode(0)
                .assertOutputContains("hot-reload:");
    }

    @Test
    void generatedFrontendFiles_areNeverInTheChangeSet() {
        // The build rewrites everything under generated/ on every run, so
        // offering it would make every apply in every project noisy. This is
        // the one frontend rule whose failure is felt immediately.
        patch.append(GENERATED, "\n// touched by DevLoopFrontendIT\n");

        cli.run("apply").assertExitCode(0).assertOutputContains("no changes");
    }
}
