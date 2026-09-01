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
package com.vaadin.flow.devloop.daemon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Both signals a launch is reported from - the process exit and the
 * registration connection - arrive asynchronously, so what is unit-tested here
 * is which launch each one is allowed to speak for once a restart has moved on.
 * The launch itself needs a project to build, and is covered by
 * {@code flow-tests/test-devloop}.
 */
class AppProcessTest {

    @TempDir
    Path root;

    @Test
    void exitOfCurrentProcess_reportsCrash() throws Exception {
        AppProcess app = new AppProcess(root, null);
        AppProcess.Run current = app.beginRun(exited(), log());

        app.handleExit(current);

        assertEquals(AppProcess.State.CRASHED, app.state());
        assertEquals(0, app.exitCode().orElseThrow());
        assertTrue(current.registrationLatch.await(0, TimeUnit.SECONDS),
                "the startup waiter must be released");
    }

    @Test
    void exitOfExpectedStop_reportsStopped() throws Exception {
        AppProcess app = new AppProcess(root, null);
        AppProcess.Run current = app.beginRun(exited(), log());
        current.stopExpected.set(true);

        app.handleExit(current);

        assertEquals(AppProcess.State.STOPPED, app.state());
        assertTrue(app.failureReason().isEmpty());
    }

    @Test
    void exitOfSupersededProcess_leavesRestartedAppAlone() throws Exception {
        AppProcess app = new AppProcess(root, null);
        AppProcess.Run previous = app.beginRun(exited(), log());
        // The restart: a new process takes over before the old callback runs.
        AppProcess.Run restarted = app.beginRun(exited(), log());
        AppProcess.State before = app.state();

        app.handleExit(previous);

        assertEquals(before, app.state(),
                "a superseded exit may not crash the new app");
        assertTrue(app.exitCode().isEmpty(),
                "no exit code from a superseded process");
        assertTrue(app.failureReason().isEmpty());
        assertFalse(restarted.registrationLatch.await(0, TimeUnit.SECONDS),
                "the new app's startup waiter must stay blocked");
    }

    @Test
    void registrationFromSupersededProcess_isRefused() throws Exception {
        AppProcess app = new AppProcess(root, null);
        AppProcess.Run previous = app.beginRun(exited(), log());
        app.beginRun(exited(), log());

        assertTrue(
                app.onRegistered("dev", OptionalLong.of(previous.process.pid()))
                        .isEmpty(),
                "a predecessor's connector may not register");
        assertFalse(app.isRegistered());
        assertEquals(AppProcess.State.STOPPED, app.state());
    }

    @Test
    void closeOfSupersededRegistration_keepsNewAppRegistered()
            throws Exception {
        AppProcess app = new AppProcess(root, null);
        AppProcess.Run previous = app.beginRun(exited(), log());
        app.onRegistered("dev", OptionalLong.of(previous.process.pid()));
        AppProcess.Run restarted = app.beginRun(exited(), log());
        app.onRegistered("dev", OptionalLong.of(restarted.process.pid()));

        // The predecessor's socket closes only now, after the restart is up.
        app.onUnregistered(previous);

        assertTrue(app.isRegistered(), "the new app must stay registered");
    }

    @Test
    void abandonedLaunch_killsTheAppAndLetsTheNextStartTryAgain()
            throws Exception {
        AppProcess app = new AppProcess(root, null);
        Files.writeString(log(), "");
        Process alive = alive();
        // Where start() waits for the connector, reached the way it reaches it.
        app.markStarting();
        AppProcess.Run current = app.beginRun(alive, log());
        assertTrue(app.alreadyLaunching(), "precondition: the launch is open");

        // The registration timeout: the JVM is still up, it just never called
        // home. Nothing else will kill it - the process did not exit, so no
        // exit callback is coming.
        AppProcess.Startup startup = app.abandon(current,
                "app did not register within 5 minutes", log());

        assertFalse(startup.ok());
        assertTrue(alive.waitFor(10, TimeUnit.SECONDS),
                "the app that never registered must not be left running");
        // The whole point: a start that failed has to leave the app startable.
        // Left in STARTING, the next start answers "already starting" with exit
        // 0 over a JVM that is never going to serve anything.
        assertFalse(app.alreadyLaunching(),
                "the next start must be allowed to proceed");
        assertTrue(
                app.failureReason().orElseThrow().contains("did not register"),
                app.failureReason().orElse("<none>"));
    }

    private Path log() {
        return root.resolve("app.log");
    }

    /** A real, still-running child, so there is something to kill. */
    private Process alive() throws IOException {
        Path source = root.resolve("Sleeper.java");
        Files.writeString(source, """
                public class Sleeper {
                    public static void main(String[] args) throws Exception {
                        Thread.sleep(600_000);
                    }
                }
                """);
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        return new ProcessBuilder(java.toString(), source.toString())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
    }

    /** A real, already terminated child, so {@code exitValue()} is genuine. */
    private Process exited() throws IOException, InterruptedException {
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        Process process = new ProcessBuilder(java.toString(), "-version")
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        process.waitFor();
        return process;
    }
}
