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

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
        // Each command waits out the grace an exiting daemon gets, so one that
        // only reports and one that would otherwise spawn a daemon stand in
        // for the rest.
        for (String command : new String[] { "status", "apply" }) {
            cli.run("--app", app.toString(), command).assertExitCode(70)
                    .assertOutputContains("running but not answering")
                    .assertOutputContains("Kill pid " + pid)
                    .assertOutputDoesNotContain("daemon did not come up")
                    .assertOutputDoesNotContain("no daemon running");
            assertTrue(Files.exists(handshake),
                    command + " reaped the record of a live daemon");
        }
    }

    @Test
    void status_withALivePidItCannotConfirm_reapsTheRecord(@TempDir Path app)
            throws IOException {
        // Without a start time to compare - none recorded, or a ps that cannot
        // report one, as on Git Bash - a live pid may just as well belong to
        // an unrelated process that reused it. Keeping the record would wedge
        // every command on it until it was deleted by hand, so it is reaped
        // once the grace an exiting daemon gets has run out.
        Files.writeString(app.resolve("pom.xml"), "<project/>");
        Path handshake = app.resolve(".vaadin/daemon.properties");
        Files.createDirectories(handshake.getParent());
        VaadinDevCli cli = VaadinDevCli.of(AbstractDevLoopIT.APP);
        String pid = "pid=" + ProcessHandle.current().pid();

        Files.writeString(handshake,
                "port=" + unusedPort() + "\ntoken=stale\n" + pid + "\n");
        assertReapedUnconfirmed(cli.run("--app", app.toString(), "status"),
                handshake);

        // The PowerShell launcher reads the start time without ps.
        if (!VaadinDevCli.WINDOWS) {
            Path bin = Files.createDirectories(app.resolve("bin"));
            Path ps = bin.resolve("ps");
            Files.writeString(ps, "#!/bin/sh\nexit 1\n");
            ps.toFile().setExecutable(true);
            Files.writeString(handshake, "port="
                    + unusedPort() + "\ntoken=stale\n" + pid
                    + "\nstartEpochMillis=" + ProcessHandle.current().info()
                            .startInstant().orElseThrow().toEpochMilli()
                    + "\n");
            assertReapedUnconfirmed(cli.run(
                    Map.of("PATH",
                            bin + File.pathSeparator + System.getenv("PATH")),
                    "--app", app.toString(), "status"), handshake);
        }
    }

    private static void assertReapedUnconfirmed(VaadinDevCli.Outcome outcome,
            Path handshake) {
        outcome.assertExitCode(0).assertOutputContains("its record was removed")
                .assertOutputContains("no daemon running")
                .assertOutputDoesNotContain("running but not answering");
        assertFalse(Files.exists(handshake),
                "status kept the record of a pid it cannot confirm");
    }

    @Test
    void status_whileTheDaemonExits_waitsForIt(@TempDir Path app)
            throws IOException, InterruptedException {
        // A daemon that has answered a shutdown has closed its port but takes
        // a moment to exit and remove its handshake. A command landing in that
        // moment must wait for it rather than call it wedged, and only call it
        // killed if it exits without removing its handshake. A process that
        // lives on for a few seconds after the CLI starts stands in for it.
        Files.writeString(app.resolve("pom.xml"), "<project/>");
        Path handshake = app.resolve(".vaadin/daemon.properties");
        Files.createDirectories(handshake.getParent());
        Path ready = app.resolve("ready");
        Path exiting = app.resolve("Exiting.java");
        Files.writeString(exiting,
                """
                                        import java.nio.file.Files;
                                        import java.nio.file.Path;
                        import java.util.Map;

                                        public class Exiting {
                                            public static void main(String[] args) throws Exception {
                                                Files.createFile(Path.of(args[1]));
                                                Thread.sleep(3500);
                                                if (args[2].equals("remove")) {
                                                    Files.delete(Path.of(args[0]));
                                                }
                                            }
                                        }
                                        """);
        VaadinDevCli cli = VaadinDevCli.of(AbstractDevLoopIT.APP);

        for (String exit : new String[] { "remove", "keep" }) {
            Files.deleteIfExists(ready);
            Process daemon = new ProcessBuilder(
                    ProcessHandle.current().info().command().orElseThrow(),
                    exiting.toString(), handshake.toString(), ready.toString(),
                    exit).start();
            try {
                Files.writeString(handshake,
                        "port=" + unusedPort() + "\ntoken=stale\npid="
                                + daemon.pid() + "\nstartEpochMillis="
                                + daemon.info().startInstant().orElseThrow()
                                        .toEpochMilli()
                                + "\n");
                for (int i = 0; i < 300 && !Files.exists(ready); i++) {
                    Thread.sleep(100);
                }
                assertTrue(Files.exists(ready) && daemon.isAlive(),
                        "the stand-in daemon did not come up");

                VaadinDevCli.Outcome result = cli
                        .run("--app", app.toString(), "status")
                        .assertExitCode(0)
                        .assertOutputContains("no daemon running")
                        .assertOutputDoesNotContain("not answering");
                if (exit.equals("remove")) {
                    result.assertOutputDoesNotContain(
                            "left its handshake behind");
                } else {
                    result.assertOutputContains("left its handshake behind");
                }
                assertFalse(Files.exists(handshake),
                        "status left the handshake of an exited daemon");
            } finally {
                daemon.destroyForcibly().waitFor();
            }
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
