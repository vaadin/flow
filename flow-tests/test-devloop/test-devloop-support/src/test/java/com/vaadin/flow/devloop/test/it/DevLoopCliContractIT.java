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

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The exit code is the contract agents depend on, so it has to mean the same
 * thing whichever launcher ran - which is why every fixture runs this, against
 * the CLI its own build installed: {@code 0} live, {@code 1} failed, {@code 4}
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
    void statusAndPing_withNoDaemon_answerWithoutSpawningOne(@TempDir Path app)
            throws IOException {
        // A daemon fixes the app's environment to the shell that spawned it,
        // so one spawned by a status would hand the app that shell's
        // environment rather than the one the following start was run with.
        Files.writeString(app.resolve("pom.xml"), "<project/>");
        VaadinDevCli cli = VaadinDevCli.of(AbstractDevLoopIT.APP);

        cli.run("--app", app.toString(), "status").assertExitCode(0)
                .assertOutputContains("no daemon running");
        cli.run("--app", app.toString(), "status", "--json").assertExitCode(0)
                .assertOutputContains("{\"app\":{\"state\":\"stopped\",")
                .assertOutputContains(
                        "\"modules\":null,\"daemon\":null,\"transaction\":null}");
        cli.run("--app", app.toString(), "ping").assertExitCode(1)
                .assertOutputContains("no daemon running");

        assertFalse(Files.exists(app.resolve(".vaadin/daemon.properties")),
                "a command that only reports spawned a daemon");
    }

    @Test
    void statusAndPing_withAKilledDaemon_reapItWithoutSpawningAnother(
            @TempDir Path app) throws IOException {
        // A daemon that was killed rather than shut down leaves its handshake
        // behind, and nothing listens on the port it names. Its pid is either
        // gone or, later, reused by an unrelated process, which the recorded
        // start time tells apart from the daemon.
        Files.writeString(app.resolve("pom.xml"), "<project/>");
        Path handshake = app.resolve(".vaadin/daemon.properties");
        Files.createDirectories(handshake.getParent());
        VaadinDevCli cli = VaadinDevCli.of(AbstractDevLoopIT.APP);
        String gonePid = "pid=" + Integer.MAX_VALUE;
        String reusedPid = "pid=" + ProcessHandle.current().pid()
                + "\nstartEpochMillis=1";

        for (String process : new String[] { gonePid, reusedPid }) {
            Files.writeString(handshake, "port=" + unusedPort()
                    + "\ntoken=stale\n" + process + "\n");
            cli.run("--app", app.toString(), "status").assertExitCode(0)
                    .assertOutputContains("left its handshake behind")
                    .assertOutputContains("no daemon running");
            assertFalse(Files.exists(handshake), "status neither reaped the"
                    + " stale handshake nor stopped there: " + process);

            Files.writeString(handshake, "port=" + unusedPort()
                    + "\ntoken=stale\n" + process + "\n");
            cli.run("--app", app.toString(), "ping").assertExitCode(1)
                    .assertOutputContains("no daemon running");
            assertFalse(Files.exists(handshake), "ping neither reaped the"
                    + " stale handshake nor stopped there: " + process);
        }
    }

    @Test
    void anyCommand_withALiveDaemonNotAnswering_keepsItsRecord(
            @TempDir Path app) throws IOException {
        // The recorded process is alive, so there is a daemon even though it
        // does not answer. It still holds the daemon lock, so a fresh daemon
        // cannot come up, and reaping its record would leave nothing naming
        // the pid to kill: no command may report none or reap the record.
        Files.writeString(app.resolve("pom.xml"), "<project/>");
        Path handshake = app.resolve(".vaadin/daemon.properties");
        Files.createDirectories(handshake.getParent());
        Files.writeString(handshake,
                "port=" + unusedPort() + "\ntoken=stale\npid="
                        + ProcessHandle.current().pid() + "\nstartEpochMillis="
                        + ProcessHandle.current().info().startInstant()
                                .orElseThrow().toEpochMilli()
                        + "\n");
        VaadinDevCli cli = VaadinDevCli.of(AbstractDevLoopIT.APP);

        String pid = Long.toString(ProcessHandle.current().pid());
        for (String command : new String[] { "apply", "start", "shutdown",
                "status", "ping" }) {
            cli.run("--app", app.toString(), command).assertExitCode(70)
                    .assertOutputContains("running but not answering")
                    .assertOutputContains("Kill pid " + pid)
                    .assertOutputDoesNotContain("daemon did not come up")
                    .assertOutputDoesNotContain("no daemon running");
            assertTrue(Files.exists(handshake),
                    command + " reaped the record of a live daemon");
        }
    }

    private static int unusedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Test
    void unknownApp_isAUsageError() {
        VaadinDevCli.of(AbstractDevLoopIT.APP)
                .run("--app", "no-such-directory", "status").assertExitCode(64);
    }
}
