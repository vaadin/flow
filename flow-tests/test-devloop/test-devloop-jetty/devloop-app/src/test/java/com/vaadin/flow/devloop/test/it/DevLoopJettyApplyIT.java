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

/**
 * {@code apply} makes an edit live in a servlet container without restarting
 * it.
 * <p>
 * {@link DevLoopApplyIT} has what an apply does anywhere. What this adds is the
 * class loader. The connector redefines through {@code Instrumentation}, which
 * acts on a loaded {@code Class} whatever loaded it - and in a WAR the
 * application's classes are loaded by the container's webapp loader rather than
 * by the system one. The tests below are what says that holds in practice, and
 * they repeat where a single apply would not show it.
 */
class DevLoopJettyApplyIT extends AbstractDevLoopIT {

    @Test
    void repeatedEdits_keepHotSwapping() {
        // Three in a row, because the failures this guards against were not
        // first-apply failures: a copy of the class in the build tool's own
        // scanning class loader, and an agent that could not instrument it,
        // both surfaced on applies after the app had settled.
        String previous = "\"Task List\"";
        for (int edit = 1; edit <= 3; edit++) {
            String next = "\"Task List " + edit + "\"";
            patch.replace(VIEW, previous, next);
            previous = next;

            cli.run("apply").assertExitCode(0)
                    .assertOutputContains("hot-reload:")
                    .assertOutputDoesNotContain("restarting");
        }
    }

    @Test
    void methodBodyEdit_isNotReadAsAFrontendChange() {
        patch.replace(VIEW, "\"Task List\"", "\"Tasks, unchanged imports\"");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        // The regression this fixture's supertype exists for. TaskListView
        // extends ImportingLayout, which declares a @JsModule - the shape every
        // real Vaadin view has, and the one that used to make an ordinary
        // method-body edit come out as a frontend change and restart. Under
        // this runtime in particular: the build runs in the application's own
        // JVM, so a second copy of the class exists in the plugin's scanning
        // loader and the two copies answered differently.
        outcome.assertOutputDoesNotContain("frontend imports changed");
        outcome.assertOutputDoesNotContain("restarting");
    }
}
