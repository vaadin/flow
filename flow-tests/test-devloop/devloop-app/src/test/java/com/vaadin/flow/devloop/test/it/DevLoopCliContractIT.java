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
import org.junit.jupiter.api.parallel.Isolated;

/**
 * The exit code is the contract agents depend on, so it has to mean the same
 * thing whichever launcher ran: {@code 0} live, {@code 1} failed, {@code 4}
 * superseded, {@code 64} usage, {@code 70} internal, {@code 77} unauthorized.
 * <p>
 * The launcher is whichever one runs here. Comparing the bash and cmd launchers
 * against each other would take a Windows machine with git-bash, which no CI
 * agent is, so a divergence between them is something a Windows developer finds
 * by running this module's README sequence through the cmd launcher.
 */
@Isolated
class DevLoopCliContractIT {

    @Test
    void unknownVerb_isAUsageError() {
        VaadinDevCli.of(AbstractDevLoopIT.APP).run("not-a-verb")
                .assertExitCode(64);
    }

    @Test
    void help_succeedsWithoutStartingADaemon() {
        // --help must answer before anything is spawned or resolved: it is
        // what a developer runs when the loop is not working.
        VaadinDevCli.of(AbstractDevLoopIT.APP).run("--help").assertExitCode(0)
                .assertOutputContains("usage: vaadin-dev");
    }

    @Test
    void unknownApp_isAUsageError() {
        VaadinDevCli.of(AbstractDevLoopIT.APP)
                .run("--app", "no-such-directory", "status").assertExitCode(64);
    }
}
