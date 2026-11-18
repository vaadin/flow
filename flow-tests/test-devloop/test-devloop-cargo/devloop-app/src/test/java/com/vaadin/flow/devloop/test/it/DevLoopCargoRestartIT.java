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
 * Restarting here means re-running a build that installs a container, packages
 * a WAR and deploys it, and then waiting for a process the daemon did not start
 * directly to bind a port.
 * <p>
 * That is the part of this runtime with the most to go wrong: the application
 * is a grandchild of the daemon, so its own exit code is lost and stopping it
 * has to reach a process Maven owns. That a restart brings the application back
 * at all is {@code DevLoopLifecycleIT}'s; what is here is what a restart means
 * when the launcher is a build and the container is a fork.
 */
class DevLoopCargoRestartIT extends AbstractDevLoopIT {

    @Test
    void restart_keepsTheRuntimeItChose() {
        cli.run("restart").assertExitCode(0);

        // A restart must not re-decide the project's shape into something else.
        cli.run("status").assertExitCode(0).assertOutputContains("cargo");
    }

    @Test
    void aStartupResourceEditEscalatesToARestart() {
        // A resource outside the public roots is read while the application
        // starts and never again, so copying it onto the classpath cannot make
        // it live - and here it would not even reach the container, which reads
        // the deployed WAR. The honest answer is a restart.
        patch.create(APP.resolve("src/main/resources/devloop-cargo.properties"),
                "greeting=restarted\n");

        cli.run("apply").assertExitCode(0).assertOutputContains("restart");

        cli.run("status").assertExitCode(0).assertOutputContains("running");
    }

    @Test
    void stopThenStart_isTheSameApplicationAgain() {
        // Stopping has to end a container Maven forked, not just the Maven the
        // daemon launched; AppProcess ends a launch descendants-first, and a
        // port that comes back is what says it worked.
        cli.run("stop").assertExitCode(0);
        cli.run("status").assertExitCode(0).assertOutputContains("stopped");

        cli.run("start").assertExitCode(0);

        cli.run("status").assertExitCode(0).assertOutputContains("running");
    }
}
