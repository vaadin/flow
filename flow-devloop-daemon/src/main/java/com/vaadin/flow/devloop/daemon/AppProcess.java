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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Owns the app process. The daemon launches the app JVM directly rather than
 * through {@code spring-boot:run}, which forks: with a fork the app is a
 * grandchild, so exit codes are lost and a kill can orphan the JVM. A direct
 * child gives real exit codes and a clean stop.
 * <p>
 * Two independent signals decide the reported state, as the design requires:
 * the process exit (authoritative, carries the code) and the registration
 * connection held open by the in-app connector (its close means the app is
 * gone). Whether a stop was expected is what separates {@code stopped} from
 * {@code crashed}.
 */
final class AppProcess {

    enum State {
        STOPPED, STARTING, RUNNING, CRASHED
    }

    /** How long an app may take to register before a start gives up on it. */
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(5);

    /**
     * How long a registered app has to report a listening web server before the
     * start settles for "registered and still alive". A start returns the
     * moment that line appears, so this bound is only ever reached by a stack
     * that logs no such line - measured on this app, the bind lands several
     * seconds after registration, and a window shorter than the gap would wave
     * a port clash through as a success. See {@link #start}.
     */
    private static final Duration SETTLE = Duration
            .ofMillis(Long.getLong("vaadin.dev.startSettleMillis", 15_000L));

    private static final long POLL_MILLIS = 100L;

    /**
     * A start's verdict. Callers need the answer itself, not a message to match
     * on: "exit code is the outcome" only holds if the code is derived from the
     * same fact the caller sees.
     */
    record Startup(boolean ok, String message, List<String> detail) {

        static Startup ok(String message) {
            return new Startup(true, message, List.of());
        }

        /**
         * The verdict first, then whatever evidence there is. The message is
         * the bare reason so that a caller which has its own framing - a
         * transaction's {@code restart: ...} - does not end up saying "failed"
         * twice.
         */
        List<String> lines() {
            List<String> lines = new ArrayList<>();
            lines.add(ok ? message : "failed: " + message);
            lines.addAll(detail);
            return lines;
        }
    }

    private final Path root;
    private final Launch launch;

    private volatile State state = State.STOPPED;
    private volatile Process process;
    private volatile Integer exitCode;
    private volatile String mode = "unknown";
    private volatile boolean registered;
    private volatile String failureReason;
    private volatile Path logFile;
    private volatile AppLog.Watch watch;
    private final AtomicBoolean stopExpected = new AtomicBoolean();
    private volatile CountDownLatch registrationLatch = new CountDownLatch(1);

    /**
     * Serialises start against stop. A lock rather than the instance monitor
     * because a start blocks for as long as the app takes to come up, and the
     * daemon runs every command on a virtual thread: blocking inside
     * {@code synchronized} pins the carrier thread for that whole time, while
     * blocking under a {@link ReentrantLock} lets the carrier run other
     * commands.
     */
    private final ReentrantLock lifecycle = new ReentrantLock();

    AppProcess(Path root, Launch launch) {
        this.root = root;
        this.launch = launch;
    }

    State state() {
        return state;
    }

    Optional<Integer> exitCode() {
        return Optional.ofNullable(exitCode);
    }

    Optional<Long> pid() {
        Process current = process;
        return current != null && current.isAlive() ? Optional.of(current.pid())
                : Optional.empty();
    }

    /**
     * When the running JVM started, in epoch milliseconds.
     * <p>
     * This is the frontend leg's substitute for a build artifact. A Java source
     * is stale when it is newer than its {@code .class} and a resource when it
     * is newer than its classpath copy, but a frontend file has neither - what
     * read it was the application, at startup. So a frontend file newer than
     * this has not been folded into anything yet, whatever the daemon's own
     * fingerprints say.
     *
     * @return the start time, or empty when there is no process or the platform
     *         does not report one
     */
    Optional<Long> startedAtMillis() {
        Process current = process;
        return current == null || !current.isAlive() ? Optional.empty()
                : current.info().startInstant()
                        .map(java.time.Instant::toEpochMilli);
    }

    String mode() {
        return mode;
    }

    boolean isRegistered() {
        return registered;
    }

    Optional<String> failureReason() {
        return Optional.ofNullable(failureReason);
    }

    /**
     * The log watch for the current app, empty before the first launch. Held
     * here because it belongs to the process: a restart gets a truncated log
     * and a new watch to read it with.
     */
    Optional<AppLog.Watch> watch() {
        return Optional.ofNullable(watch);
    }

    /**
     * Launches and waits until the app is serving or gone, because a start that
     * reports success for an app that is already dying is worse than no answer.
     * Two gates, neither of them a timer:
     * <ol>
     * <li><b>Registered</b> - the in-app connector called home, so the app's
     * own code is running.</li>
     * <li><b>Serving</b> - registration happens while the Spring context is
     * still refreshing and the embedded web server binds its port
     * <em>after</em> that, so an app whose port is taken registers happily and
     * only then dies. The second gate is the app reporting its server
     * listening, with surviving a short settle as the fallback for a stack that
     * logs no such line.</li>
     * </ol>
     * Either gate losing the race to the process exiting is a failure, reported
     * with the reason from the app's own log instead of a bare exit code.
     */
    /**
     * The same command, with everything after the java binary moved into a JVM
     * argument file.
     * <p>
     * Not a nicety: one module's classpath is already 22 kB of Windows'
     * 32,767-character process-creation limit, and a reactor adds a module's
     * worth of entries at a time, so the direct command line is one shared
     * library away from {@code CreateProcess error=206}. The argument file has
     * no such limit, and it takes quoting out of the picture as well - within
     * it, every argument is one quoted line, and a backslash only escapes what
     * it is doubled onto.
     */
    private List<String> viaArgFile(List<String> command) throws IOException {
        Path file = Launch.workDir(root).resolve("jvm-args.txt");
        Files.createDirectories(file.getParent());
        StringBuilder sb = new StringBuilder();
        for (String argument : command.subList(1, command.size())) {
            sb.append('"').append(
                    argument.replace("\\", "\\\\").replace("\"", "\\\""))
                    .append('"').append('\n');
        }
        Files.writeString(file, sb.toString());
        return List.of(command.get(0), "@" + file);
    }

    Startup start(Launch.Log log) throws IOException {
        lifecycle.lock();
        try {
            if (state == State.RUNNING || state == State.STARTING) {
                return Startup.ok(state == State.STARTING ? "already starting"
                        : "already running");
            }
            List<String> command = launch.command(Daemon.currentPort(),
                    Daemon.currentToken());
            Path appLog = Launch.workDir(root).resolve("app.log");
            Files.createDirectories(appLog.getParent());

            stopExpected.set(false);
            registered = false;
            failureReason = null;
            exitCode = null;
            logFile = appLog;
            registrationLatch = new CountDownLatch(1);
            state = State.STARTING;

            log.line("launching " + command.get(0));
            // The launch line is worth showing - nine flags that all have to be
            // right
            // - but the auth token must not be echoed to stdout or into a log.
            String flags = command
                    .subList(1, Math.max(1, command.indexOf("-cp"))).stream()
                    .map(flag -> flag.startsWith("-Dvaadin.devloop.token=")
                            ? "-Dvaadin.devloop.token=<redacted>"
                            : flag)
                    .collect(java.util.stream.Collectors.joining(" "));
            log.line("flags: " + flags);

            Process started = new ProcessBuilder(viaArgFile(command))
                    .directory(root.toFile()).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.to(appLog.toFile()))
                    .start();
            this.process = started;
            log.line("app pid " + started.pid() + ", log " + appLog);

            started.onExit().thenAccept(this::handleExit);

            // Redirect.to truncates, so this run's output starts at offset
            // zero.
            // The
            // watch outlives the start: the errors a change provokes are logged
            // long
            // after the app came up, and this is the only reader of that log.
            AppLog.Watch watching = new AppLog.Watch(appLog);
            this.watch = watching;
            CountDownLatch latch = registrationLatch;
            long registerBy = System.nanoTime() + STARTUP_TIMEOUT.toNanos();
            long settleBy = 0;
            boolean up = false;
            boolean serving = false;

            while (true) {
                serving = serving
                        || watching.drain().stream().anyMatch(AppLog::serving);
                if (up && (serving || System.nanoTime() >= settleBy)) {
                    state = State.RUNNING;
                    return Startup.ok(serving ? "running"
                            : "running (registered; the app logged no server port)");
                }
                if (!started.isAlive()) {
                    // The authoritative signal, and the only one carrying a
                    // code.
                    return failed("app exited with code " + started.exitValue()
                            + (up ? " right after registering, before it was serving"
                                    : " before registering"),
                            appLog);
                }
                if (!up && System.nanoTime() >= registerBy) {
                    return failed(
                            "app did not register within "
                                    + STARTUP_TIMEOUT.toMinutes() + " minutes",
                            appLog);
                }
                try {
                    // The latch also fires when the process exits, so
                    // registration
                    // is
                    // decided by the flag the connector sets, not by the
                    // wake-up.
                    if (up) {
                        Thread.sleep(POLL_MILLIS);
                    } else if (latch.await(POLL_MILLIS, TimeUnit.MILLISECONDS)
                            && registered) {
                        up = true;
                        settleBy = System.nanoTime() + SETTLE.toNanos();
                        log.line(
                                "registered; waiting for the web server to bind");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return failed(
                            "interrupted while waiting for the app to start",
                            appLog);
                }
            }
        } finally {
            lifecycle.unlock();
        }
    }

    String stop() {
        lifecycle.lock();
        try {
            Process current = process;
            if (current == null || !current.isAlive()) {
                state = State.STOPPED;
                return "not running";
            }
            stopExpected.set(true);
            current.destroy();
            try {
                if (!current.waitFor(10, TimeUnit.SECONDS)) {
                    current.destroyForcibly();
                    current.waitFor(10, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            state = State.STOPPED;
            return "stopped";
        } finally {
            lifecycle.unlock();
        }
    }

    /**
     * Turns a dead or unreachable app into an answer that stands on its own.
     * The app's log holds the only copy of the real reason - "Port 8080 was
     * already in use" is printed by the app, nothing here can observe it - so
     * the cause goes into the message and an excerpt comes along as the
     * evidence.
     */
    private Startup failed(String message, Path appLog) {
        String cause = AppLog.cause(appLog).orElse("");
        String full = message + (cause.isEmpty() ? "" : ": " + cause);
        // A crash has already recorded its own reason from the same log; this
        // one
        // only fills the gap where the app is alive but never registered.
        if (failureReason == null) {
            failureReason = full;
        }
        // The report's own lines, not the log's last ones: a Spring failure
        // ends
        // in frames, so a plain tail shows the trace of the reason and never
        // the
        // reason itself.
        List<String> excerpt = AppLog.excerpt(appLog);
        if (excerpt.isEmpty()) {
            return new Startup(false, full, List.of("no output in " + appLog));
        }
        List<String> detail = new ArrayList<>();
        detail.add("--- " + excerpt.size() + " lines from " + appLog + " ---");
        detail.addAll(excerpt);
        return new Startup(false, full, detail);
    }

    private void handleExit(Process exited) {
        exitCode = exited.exitValue();
        registered = false;
        if (stopExpected.get()) {
            state = State.STOPPED;
        } else {
            state = State.CRASHED;
            Path appLog = logFile;
            // The reason, not just the code: an "exit=1" whose cause stays
            // buried
            // in a log file is what made a port clash look like a tool bug.
            failureReason = "app exited unexpectedly with code " + exitCode
                    + (appLog == null ? ""
                            : AppLog.cause(appLog).map(c -> ": " + c).orElse("")
                                    + " (see " + appLog + ")");
        }
        // Release anyone waiting on startup so a failed launch returns at once.
        registrationLatch.countDown();
    }

    /**
     * Called when the in-app connector registers over its long-lived socket.
     */
    void onRegistered(String reportedMode) {
        this.mode = reportedMode;
        this.registered = true;
        this.state = State.RUNNING;
        registrationLatch.countDown();
    }

    /**
     * Called when the registration connection closes. The process exit is the
     * authoritative signal, so this only records the loss of the app-side
     * channel; it does not by itself declare a crash.
     */
    void onUnregistered() {
        registered = false;
    }

    boolean isIdle() {
        return state == State.STOPPED || state == State.CRASHED;
    }
}
