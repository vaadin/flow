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
 * A stylesheet under a public classpath root is refreshed by copying it onto
 * the classpath, so an apply is the whole of the work.
 * <p>
 * The stylesheet is the sibling module's, so this is the cross-module resource
 * leg as well - and the one a container really serves, whatever runs it: a
 * {@code META-INF/resources} directory on the class path is served by Spring
 * Boot and by a servlet container alike. Whether the <em>open page</em>
 * re-renders is a browser question, asserted where there is a browser.
 */
class DevLoopCssIT extends AbstractDevLoopIT {

    @Test
    void cssEdit_isPushedWithoutARestart() {
        patch.replace(STYLESHEET, "row-gap: 12px;", "row-gap: 33px;");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        // The push itself, reported: neither fixture runs a dev server, so this
        // is the daemon's own push over the dev-tools connection rather than
        // Vite's.
        outcome.assertOutputContains("hmr:");
        // A stylesheet cannot break a class, so nothing here may restart the
        // app or recompile anything.
        outcome.assertOutputDoesNotContain("restarting");
        outcome.assertOutputDoesNotContain("compiling");
        // The change-set is relative to the application, so a sibling reads as
        // ../devloop-shared/...
        outcome.assertOutputContains("../devloop-shared/");
    }

    @Test
    void cssEdit_refreshesWhatTheServerServes() throws Exception {
        patch.replace(STYLESHEET, "row-gap: 12px;", "row-gap: 44px;");

        cli.run("apply").assertExitCode(0);

        // Flow watches the source tree but never refreshes target/classes, so
        // anything that re-fetches the file - a page reload, a new tab - would
        // get stale bytes. The daemon copies it first; this is that, through
        // whatever caching the server in front of it does.
        assertTrue(fetch("/task-list.css").contains("row-gap: 44px"),
                "the served stylesheet should hold the edited value");
    }

    @Test
    void cssAndJavaInOneApply_pushTheStylesheetAndHotSwapTheClass()
            throws Exception {
        // The mixed change-set: the resource leg runs, and then the Java leg
        // decides the outcome. The resource half has to be pushed all the same
        // - a redefine that succeeds says nothing about a stylesheet the page
        // never received, and the reply from that push is what says which
        // happened.
        patch.replace(STYLESHEET, "row-gap: 12px;", "row-gap: 55px;");
        patch.replace(VIEW, "\"Task List\"", "\"Tasks, mixed\"");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        outcome.assertOutputContains("hot-reload:");
        // Both legs ran, so both are reported: the redefine alone would leave
        // the reader unable to tell a stylesheet that went to the page from
        // one that never left the classpath. No page is open here, which is
        // what the push says.
        outcome.assertOutputContains("hmr: 1 resource(s) copied");
        outcome.assertOutputDoesNotContain("restarting");
        assertTrue(fetch("/task-list.css").contains("55px"),
                "the served stylesheet should hold the edited value");
    }
}
