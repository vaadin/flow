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
 * Restarting a WAR means re-running the project's build plugin, which is the
 * part of this runtime with the most to go wrong: a second Maven invocation has
 * to take the port the first one just released, and the daemon has to know the
 * application is up again from a log that begins with a build log.
 * <p>
 * That a restart brings the application back at all is
 * {@link DevLoopLifecycleIT}'s; what is here is what a restart costs and means
 * when the launcher is a build.
 */
class DevLoopJettyRestartIT extends AbstractDevLoopIT {

    @Test
    void restart_keepsTheRuntimeItChose() {
        cli.run("restart").assertExitCode(0);

        // A restart must not re-decide the project's shape into something else.
        cli.run("status").assertExitCode(0).assertOutputContains("jetty-ee10");
    }

    @Test
    void aStartupResourceEditEscalatesToARestart() {
        // A resource outside the public roots is read while the application
        // starts and never again, so copying it onto the classpath cannot make
        // it live. The honest answer is a restart, and under this runtime that
        // is a Maven invocation rather than a bare JVM launch.
        patch.create(APP.resolve("src/main/resources/devloop-jetty.properties"),
                "greeting=restarted\n");

        cli.run("apply").assertExitCode(0).assertOutputContains("restart");

        cli.run("status").assertExitCode(0).assertOutputContains("running");
    }

    @Test
    void stopThenStart_isTheSameApplicationAgain() {
        cli.run("stop").assertExitCode(0);
        cli.run("status").assertExitCode(0).assertOutputContains("stopped");

        cli.run("start").assertExitCode(0);

        cli.run("status").assertExitCode(0).assertOutputContains("running");
    }
}
