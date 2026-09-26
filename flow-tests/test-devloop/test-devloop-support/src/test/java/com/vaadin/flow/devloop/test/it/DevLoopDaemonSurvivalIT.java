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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * The daemon has to outlive the command that spawned it, and surviving the
 * shell is not the same thing as surviving its process group.
 * <p>
 * Agent runners, CI steps, {@code timeout --kill-after} and a closing terminal
 * all end a command by killing the whole process group it ran in. A daemon that
 * is only {@code nohup}'d stays in that group, so it - and the application it
 * owns - dies with the very command that started it. The next command then
 * finds a handshake pointing at nothing, spawns a second daemon and reports the
 * app as stopped, which is how the loop stops holding across two commands.
 * <p>
 * What is asserted here is the property that prevents it: the daemon runs in a
 * session of its own, which no kill aimed at the spawning group can reach. The
 * kill itself is not staged - the only group this JVM could aim at is its own,
 * and the daemon under test is the one the other ITs share, so shutting it down
 * to spawn a killable one would cost the run a cold start to prove what the
 * session id already says.
 */
@Isolated
class DevLoopDaemonSurvivalIT {

    @Test
    void daemon_runsInASessionOfItsOwn() {
        Assumptions.assumeFalse(VaadinDevCli.WINDOWS,
                "process groups and sessions are a POSIX notion");
        Assumptions.assumeTrue(onPath("setsid") || onPath("perl"),
                "no setsid and no perl, so this platform has nothing to detach with");

        VaadinDevCli cli = VaadinDevCli.of(AbstractDevLoopIT.APP);
        // ping rather than start: the session is decided when the daemon is
        // spawned, and an application is not needed to read it.
        cli.run("ping").assertExitCode(0);

        long daemon = daemonPid();
        Assertions.assertTrue(ProcessHandle.of(daemon).isPresent(),
                "the daemon the handshake names (pid " + daemon
                        + ") is not running");

        String own = session(ProcessHandle.current().pid());
        String daemonSession = session(daemon);
        Assumptions.assumeFalse(own.isEmpty() || daemonSession.isEmpty(),
                "ps here cannot report a session id");

        // The CLI is a child of this JVM and does not change session, so the
        // daemon sharing this session is exactly the state a group kill takes
        // down with the command.
        Assertions.assertNotEquals(own, daemonSession,
                "the daemon runs in this test JVM's session (" + own
                        + "), so a kill aimed at the spawning command's process "
                        + "group would take the daemon and its application down "
                        + "with it");
    }

    /** The pid the daemon recorded for itself, as the CLI reads it. */
    private static long daemonPid() {
        Path handshake = AbstractDevLoopIT.APP
                .resolve(".vaadin/daemon.properties");
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(handshake)) {
            properties.load(in);
        } catch (IOException e) {
            throw new AssertionError("could not read " + handshake, e);
        }
        String pid = properties.getProperty("pid", "");
        Assertions.assertFalse(pid.isEmpty(),
                handshake + " carries no pid, so no daemon can be found");
        return Long.parseLong(pid.trim());
    }

    /**
     * The session id of a process, or empty when {@code ps} cannot say.
     */
    private static String session(long pid) {
        return run(List.of("ps", "-o", "sess=", "-p", String.valueOf(pid)));
    }

    private static boolean onPath(String command) {
        return !run(List.of("sh", "-c", "command -v " + command)).isEmpty();
    }

    private static String run(List<String> command) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true).start();
            String output;
            try (InputStream in = process.getInputStream()) {
                output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "";
            }
            return process.exitValue() == 0 ? output.trim() : "";
        } catch (IOException e) {
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }
}
