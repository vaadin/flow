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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Runs the installed {@code vaadin-dev} CLI and returns what it said.
 * <p>
 * The ITs drive the CLI rather than the daemon's socket on purpose: the exit
 * code is the contract agents depend on, so it is the thing worth testing, and
 * the script is exactly the one {@code install-dev-cli} put in
 * {@code .vaadin/}.
 */
final class VaadinDevCli {

    /** The launchers, in the order they are preferred on this platform. */
    enum Launcher {

        /** The reference implementation. */
        BASH(".vaadin/vaadin-dev"),
        /** The Windows port, driven through its batch shim. */
        CMD(".vaadin/vaadin-dev.cmd");

        private final String script;

        Launcher(String script) {
            this.script = script;
        }

        /**
         * The command line for a verb, or empty when this launcher cannot run
         * here - no bash on a bare Windows machine, no cmd.exe anywhere else.
         */
        Optional<List<String>> command(Path app, List<String> arguments) {
            Path file = app.resolve(script);
            if (!Files.isRegularFile(file)) {
                return Optional.empty();
            }
            List<String> command = new ArrayList<>();
            if (this == BASH) {
                Optional<Path> bash = bash();
                if (bash.isEmpty()) {
                    return Optional.empty();
                }
                command.add(bash.get().toString());
                // The path as bash sees it: a Windows drive letter is fine for
                // git-bash, which is the only bash there is on Windows.
                command.add(file.toString().replace('\\', '/'));
            } else {
                if (!WINDOWS) {
                    return Optional.empty();
                }
                command.add("cmd.exe");
                command.add("/c");
                command.add(file.toString());
            }
            command.addAll(arguments);
            return Optional.of(command);
        }
    }

    static final boolean WINDOWS = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT).startsWith("windows");

    /**
     * The test JVM's property naming the compiler the daemon compiles applies
     * with, {@code javac} or {@code maven}: how the build runs the same suite
     * once per compiler.
     */
    static final String COMPILER = "devloop.it.compiler";

    /** The compiler this run hands the daemon; see {@link #COMPILER}. */
    static String compiler() {
        return System.getProperty(COMPILER, "javac");
    }

    /** What one CLI invocation produced. */
    record Outcome(int exitCode, String output) {

        boolean isLive() {
            return exitCode == 0;
        }

        /**
         * Fails with the CLI's own words rather than a bare exit code: the
         * reason a start or an apply failed only exists in that output.
         */
        Outcome assertExitCode(int expected) {
            if (exitCode != expected) {
                throw new AssertionError(
                        "expected exit " + expected + " but got " + exitCode
                                + "; vaadin-dev said:\n" + output);
            }
            return this;
        }

        Outcome assertOutputContains(String expected) {
            if (!output.contains(expected)) {
                throw new AssertionError("expected the output to contain \""
                        + expected + "\"; vaadin-dev said:\n" + output);
            }
            return this;
        }

        Outcome assertOutputDoesNotContain(String unexpected) {
            if (output.contains(unexpected)) {
                throw new AssertionError("expected the output not to contain \""
                        + unexpected + "\"; vaadin-dev said:\n" + output);
            }
            return this;
        }
    }

    private final Path app;
    private final Launcher launcher;

    private VaadinDevCli(Path app, Launcher launcher) {
        this.app = app;
        this.launcher = launcher;
    }

    /**
     * The CLI for an application, using whichever launcher runs on this
     * platform.
     *
     * @param app
     *            the application module directory
     * @return the CLI runner
     */
    static VaadinDevCli of(Path app) {
        for (Launcher candidate : Launcher.values()) {
            if (candidate.command(app, List.of("ping")).isPresent()) {
                VaadinDevCli cli = new VaadinDevCli(app, candidate);
                cli.bindDaemonLifecycleOnce();
                return cli;
            }
        }
        throw new IllegalStateException(
                "no vaadin-dev launcher can run here; is " + app
                        + "/.vaadin/ installed, and is bash or cmd.exe available?");
    }

    /**
     * Runs one command and waits for its verdict.
     *
     * @param arguments
     *            the verb and its options
     * @return the exit code and the whole output
     */
    Outcome run(String... arguments) {
        List<String> command = launcher.command(app, List.of(arguments))
                .orElseThrow(() -> new IllegalStateException(
                        launcher + " cannot run here"));
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(app.toFile()).redirectErrorStream(true);
        // The reactor root has to be pinned: this application sits inside the
        // Flow repository, whose own root aggregates it too, and the daemon
        // would
        // otherwise resolve the classpath by building all of Flow.
        builder.environment().put("VAADIN_DEV_DAEMON_OPTS",
                "-Dvaadin.dev.reactorRoot=" + app.getParent()
                        + " -Dvaadin.dev.idleSeconds=600"
                        // The Flow repository's install-git-hooks profile runs
                        // a
                        // script it finds next to
                        // maven.multiModuleProjectDirectory, which is this test
                        // reactor once the daemon drives Maven from here - so
                        // the
                        // profile has to stay out of the resolve.
                        + " -Dvaadin.dev.mavenArgs=-P!install-git-hooks"
                        + " -Dvaadin.dev.compiler=" + compiler());
        // No spinner: the output is read by assertions, not by a person.
        builder.environment().put("VAADIN_DEV_PROGRESS", "never");
        String verb = "vaadin-dev " + String.join(" ", arguments);
        try {
            Process process = builder.start();
            // Read on a thread of its own so the timeout below actually holds:
            // reading to the end first blocks for as long as anything keeps
            // the pipe open, which a hung command - or a process it started
            // that inherited the pipe - does forever.
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            InputStream in = process.getInputStream();
            Thread reader = Thread.ofPlatform().daemon()
                    .name("vaadin-dev-output").start(() -> {
                        byte[] buffer = new byte[8192];
                        try {
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                synchronized (output) {
                                    output.write(buffer, 0, read);
                                }
                            }
                        } catch (IOException e) {
                            // Closed below after a timeout; what was read so
                            // far is all there is.
                        }
                    });
            try {
                if (!process.waitFor(COMMAND_TIMEOUT.toMillis(),
                        TimeUnit.MILLISECONDS)) {
                    process.descendants()
                            .forEach(ProcessHandle::destroyForcibly);
                    process.destroyForcibly();
                    reader.join(OUTPUT_DRAIN_TIMEOUT.toMillis());
                    throw new AssertionError(verb + " did not finish in "
                            + COMMAND_TIMEOUT.toMinutes()
                            + " minutes; it said:\n" + textOf(output));
                }
                reader.join(OUTPUT_DRAIN_TIMEOUT.toMillis());
                if (reader.isAlive()) {
                    throw new AssertionError(verb + " exited with "
                            + process.exitValue()
                            + " but something it started still holds its"
                            + " output open; it said:\n" + textOf(output));
                }
                return new Outcome(process.exitValue(), textOf(output));
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new AssertionError("could not run " + command, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while running " + command, e);
        }
    }

    private static String textOf(ByteArrayOutputStream output) {
        synchronized (output) {
            return output.toString(StandardCharsets.UTF_8);
        }
    }

    /**
     * Takes ownership of the daemon for this run: no daemon from an earlier one
     * survives into it, and none of this one survives past it. Run once,
     * whichever test needed the CLI first.
     */
    private void bindDaemonLifecycleOnce() {
        if (!LIFECYCLE_BOUND.compareAndSet(false, true)) {
            return;
        }
        shutdownADaemonLeftByAnEarlierRun();
        shutdownWhenThisJvmExits();
    }

    /**
     * Stops a daemon this run did not start, before anything else asks it to do
     * something.
     * <p>
     * The exit hook below is best-effort by nature: a test JVM that exits while
     * a transaction is still in flight leaves the daemon running. Reusing that
     * one makes this run's first apply race the previous run's, which the
     * daemon correctly reports as superseded - failing a test for a reason that
     * has nothing to do with what it is testing. A run can guarantee its own
     * preconditions and not much else, so it does not trust the previous one to
     * have cleaned up.
     * <p>
     * Nothing to do without a handshake file: that file is how the CLI finds a
     * daemon at all, so its absence is the same answer as a shutdown.
     */
    private void shutdownADaemonLeftByAnEarlierRun() {
        if (Files.isRegularFile(app.resolve(HANDSHAKE))) {
            run("shutdown");
        }
    }

    /**
     * Stops the daemon and the application it owns when the test JVM exits.
     * <p>
     * The daemon's whole value is that it survives between commands, so nothing
     * shuts it down between tests - but a build must not leave a JVM and a
     * bound port behind for the idle watchdog to reap minutes later.
     */
    private void shutdownWhenThisJvmExits() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                run("shutdown");
            } catch (RuntimeException | AssertionError e) {
                // Nothing to report to at this point, and a daemon that is
                // already gone is the outcome we wanted anyway.
            }
        }, "devloop-it-shutdown"));
    }

    /**
     * How long one command may take. A start or an apply takes well under two
     * minutes on CI; the limit has to stay far enough below the CI step's
     * timeout that a hang fails the test, with the CLI's output, instead of the
     * whole job - including the reruns failsafe gives a failed test.
     */
    private static final Duration COMMAND_TIMEOUT = Duration.ofMinutes(5);

    /**
     * How long the output may keep coming after the command has exited or has
     * been killed.
     */
    private static final Duration OUTPUT_DRAIN_TIMEOUT = Duration.ofSeconds(30);

    /** The daemon's handshake, and the only way the CLI can find it. */
    private static final String HANDSHAKE = ".vaadin/daemon.properties";

    private static final java.util.concurrent.atomic.AtomicBoolean LIFECYCLE_BOUND = new java.util.concurrent.atomic.AtomicBoolean();

    private static Optional<Path> bash() {
        List<String> candidates = WINDOWS
                ? List.of("C:\\Program Files\\Git\\bin\\bash.exe",
                        "C:\\Program Files\\Git\\usr\\bin\\bash.exe",
                        "C:\\Windows\\System32\\bash.exe")
                : List.of("/bin/bash", "/usr/bin/bash");
        for (String candidate : candidates) {
            Path path = Path.of(candidate);
            if (Files.isRegularFile(path)) {
                return Optional.of(path);
            }
        }
        return Optional.empty();
    }
}
