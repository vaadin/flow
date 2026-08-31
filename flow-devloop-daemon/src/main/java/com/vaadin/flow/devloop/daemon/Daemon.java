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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The dev-loop daemon: one per application module, discovered through
 * {@code .vaadin/daemon.properties}, never started by hand.
 * <p>
 * One application, which is not the same as one module: when the application
 * sits in a Maven reactor, every reactor module it depends on is in the same
 * edit loop. The application module is still the only tree the daemon owns —
 * the handshake, the logs and the caches all live under it.
 * <p>
 * P1 scope is lifecycle only - {@code status}, {@code start}, {@code stop},
 * {@code restart}, {@code shutdown}. The transaction verbs come in P2/P3, which
 * is why the wire protocol already streams progress lines before a final exit
 * code: {@code apply} will need exactly that shape.
 */
public final class Daemon {

    private static final String VERSION = "1";

    private static volatile int currentPort;
    private static volatile String currentToken;

    static int currentPort() {
        return currentPort;
    }

    static String currentToken() {
        return currentToken;
    }

    private final Path root;
    private final Launch launch;
    private final AppProcess app;
    private final Instant startedAt = Instant.now();
    private final AtomicInteger liveClients = new AtomicInteger();
    private volatile Instant lastActivity = Instant.now();
    private volatile boolean shuttingDown;

    /** Kept so the shutdown hook can stop it rather than leaving it running. */
    private volatile ScheduledExecutorService idleWatchdog;
    private ServerSocket serverSocket;

    private final Duration idleTimeout = Duration
            .ofSeconds(Long.getLong("vaadin.dev.idleSeconds", 1800L));

    private final TransactionEngine transactions;

    private Daemon(Path root) {
        this.root = root;
        this.launch = new Launch(Reactor.discover(root, System.out::println),
                System.out::println);
        this.app = new AppProcess(root, launch);
        this.transactions = new TransactionEngine(launch, app);
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath()
                .normalize();
        new Daemon(root).run();
    }

    private void run() throws IOException {
        // Stale-instance takeover, decided by process liveness rather than by
        // probing a socket. A live predecessor wins and we exit quietly.
        Optional<Handshake> existing = Handshake.read(root);
        if (existing.isPresent()) {
            if (existing.get().isProcessAlive()) {
                System.out.println(
                        "daemon already running on port " + existing.get().port
                                + " (pid " + existing.get().pid + ")");
                return;
            }
            System.out.println("reaping stale daemon record (pid "
                    + existing.get().pid + " is gone)");
            Handshake.delete(root);
        }

        serverSocket = new ServerSocket(0, 32,
                InetAddress.getLoopbackAddress());
        currentPort = serverSocket.getLocalPort();
        byte[] secret = new byte[24];
        new SecureRandom().nextBytes(secret);
        currentToken = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(secret);

        long pid = ProcessHandle.current().pid();
        long start = ProcessHandle.current().info().startInstant()
                .map(Instant::toEpochMilli).orElse(System.currentTimeMillis());
        new Handshake(pid, start, currentPort, currentToken, root.toString())
                .write(root);

        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
        startIdleWatchdog();

        System.out.println("vaadin-dev daemon " + VERSION + " listening on "
                + currentPort + " for " + root);
        // The loop's shape, said out loud. Which modules an apply watches is
        // the
        // one thing a developer cannot infer from behaviour until an edit fails
        // to be noticed, and by then they are debugging the wrong thing.
        if (launch.reactor().isMultiModule()) {
            System.out.println("reactor " + launch.reactor().root() + " - "
                    + launch.reactor().describe());
        }

        while (!shuttingDown) {
            try {
                Socket client = serverSocket.accept();
                Thread.ofVirtual().start(() -> handle(client));
            } catch (IOException e) {
                if (!shuttingDown) {
                    System.err.println("accept failed: " + e);
                }
            }
        }
    }

    private void startIdleWatchdog() {
        ScheduledExecutorService scheduler = Executors
                .newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "idle-watchdog");
                    t.setDaemon(true);
                    return t;
                });
        scheduler.scheduleAtFixedRate(() -> {
            if (shuttingDown || liveClients.get() > 0 || !app.isIdle()) {
                return;
            }
            if (Duration.between(lastActivity, Instant.now())
                    .compareTo(idleTimeout) > 0) {
                System.out.println("idle for " + idleTimeout.toSeconds()
                        + "s with no app running - shutting down");
                requestShutdown();
            }
        }, 5, 5, TimeUnit.SECONDS);
        idleWatchdog = scheduler;
    }

    private void handle(Socket client) {
        liveClients.incrementAndGet();
        try (client;
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        client.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(client.getOutputStream(),
                        true, StandardCharsets.UTF_8)) {
            String request = in.readLine();
            if (request == null) {
                return;
            }
            String[] parts = request.trim().split("\\s+");
            if (parts.length < 2 || !tokenMatches(parts[0])) {
                out.println("> unauthorized");
                out.println("EXIT 77");
                return;
            }
            lastActivity = Instant.now();
            String verb = parts[1].toLowerCase();
            List<String> rest = List.of(parts)
                    .subList(Math.min(2, parts.length), parts.length);

            if (verb.equals("register")) {
                handleRegistration(rest, in, out);
                return;
            }
            dispatch(verb, rest, out);
        } catch (IOException e) {
            // Client vanished mid-command; nothing to report to.
        } finally {
            liveClients.decrementAndGet();
            lastActivity = Instant.now();
        }
    }

    /**
     * The in-app connector's connection. It is held open for the app's
     * lifetime, so its close is the signal that the app is gone - no polling,
     * no probing.
     */
    private void handleRegistration(List<String> args, BufferedReader in,
            PrintWriter out) throws IOException {
        String mode = args.isEmpty() ? "unknown" : args.get(0);
        Connector connector = new Connector(out);
        app.onRegistered(mode);
        transactions.onConnector(connector);
        out.println("> registered");
        // Issued off-thread: the reply is only readable once the pump below is
        // running, so asking inline would wait for a line nobody is reading.
        Thread.ofVirtual()
                .start(() -> connector.command("INFO", 10)
                        .ifPresent(info -> System.out.println(
                                "app registered (mode=" + mode + ") " + info)));
        try {
            // Block until the app closes the connection or dies, feeding
            // replies
            // to whoever issued a command.
            Connector.pump(in, connector, () -> lastActivity = Instant.now());
        } finally {
            connector.close();
            transactions.onConnector(null);
            app.onUnregistered();
            System.out.println("app registration closed");
        }
    }

    private void dispatch(String verb, List<String> args, PrintWriter out) {
        Launch.Log log = text -> {
            out.println("> " + text);
            out.flush();
        };
        try {
            switch (verb) {
            case "ping" -> {
                log.line("pong " + VERSION + " pid "
                        + ProcessHandle.current().pid());
                out.println("EXIT 0");
            }
            case "status" -> {
                boolean json = args.contains("--json");
                (json ? statusJson() : statusText()).forEach(log::line);
                out.println("EXIT 0");
            }
            case "redefine" -> {
                // Diagnostic: pushes named classes at the running app and
                // returns
                // the connector's reply verbatim, with none of apply's
                // escalation
                // policy applied. This is what the measurement harnesses need -
                // raw JVM behaviour - and it keeps them on the one transport
                // instead of a second socket of their own.
                if (args.isEmpty()) {
                    log.line("usage: redefine <binary.Class,...>");
                    out.println("EXIT 64");
                } else {
                    Connector connector = transactions.connector();
                    if (connector == null || !connector.isOpen()) {
                        log.line("no app registered");
                        out.println("EXIT 1");
                    } else {
                        String reply = connector
                                .command("REDEFINE " + args.get(0), 60)
                                .orElse("ERR kind=no-reply");
                        log.line(reply);
                        out.println("EXIT " + (reply.startsWith("OK") ? 0 : 1));
                    }
                }
            }
            case "apply" -> {
                boolean json = args.contains("--json");
                // With --json the response must be parseable, so progress lines
                // are dropped rather than interleaved with the object.
                Launch.Log progress = json ? text -> {
                } : log;
                TransactionEngine.Transaction tx = transactions.apply(progress,
                        !args.contains("--no-restart"));
                if (json) {
                    log.line(tx.json());
                } else {
                    transactions.render(tx).forEach(log::line);
                }
                out.println("EXIT " + tx.outcome.exitCode);
            }
            case "start" -> {
                AppProcess.Startup startup = app.start(log);
                startup.lines().forEach(log::line);
                out.println("EXIT " + (startup.ok() ? 0 : 1));
            }
            case "stop" -> {
                log.line(app.stop());
                out.println("EXIT 0");
            }
            case "restart" -> {
                app.stop();
                AppProcess.Startup startup = app.start(log);
                startup.lines().forEach(log::line);
                out.println("EXIT " + (startup.ok() ? 0 : 1));
            }
            case "shutdown" -> {
                log.line("daemon shutting down");
                if (!app.isIdle()) {
                    log.line("stopping app it owns");
                    app.stop();
                }
                out.println("EXIT 0");
                out.flush();
                requestShutdown();
            }
            default -> {
                log.line("unknown command: " + verb);
                out.println("EXIT 64");
            }
            }
        } catch (Exception e) {
            log.line("error: " + e);
            out.println("EXIT 70");
        }
    }

    private List<String> statusText() {
        StringBuilder sb = new StringBuilder();
        sb.append(app.state().name().toLowerCase());
        app.pid().ifPresent(pid -> sb.append("  pid=").append(pid));
        // Only a crash reports an exit code. After a deliberate stop the code
        // is
        // an artifact of how we terminated it, and "exit=1" reads as a failure.
        if (app.state() == AppProcess.State.CRASHED) {
            app.exitCode().ifPresent(code -> sb.append("  exit=").append(code));
        }
        if (app.state() == AppProcess.State.RUNNING) {
            sb.append("  owner=daemon  registered=").append(app.isRegistered());
        }
        List<String> lines = new java.util.ArrayList<>();
        lines.add(sb.toString());
        modulesLine().ifPresent(lines::add);
        launch.resolutionError()
                .ifPresent(reason -> lines.add("classpath: " + reason));
        app.failureReason().ifPresent(reason -> lines.add(reason));
        frontendStatus().ifPresent(f -> lines.add("frontend " + f));
        // Errors the app has logged since the last apply. This is where a
        // failure
        // that only shows up when someone uses the app surfaces: an apply
        // cannot
        // wait for the next page render, but the question "is my change
        // alright?"
        // is asked again here, and by then the app has answered it.
        List<String> logErrors = appLogErrors();
        if (!logErrors.isEmpty()) {
            lines.add("app log: " + logErrors.size() + " error(s)"
                    + transactions.lastTransaction()
                            .map(tx -> " since tx#" + tx.id)
                            .orElse(" since the app started"));
            lines.add("  " + logErrors.get(0).strip());
        }
        transactions.current().ifPresent(
                tx -> lines.add("tx#" + tx.id + " in flight: " + tx.state));
        transactions.lastTransaction()
                .ifPresent(tx -> lines.add("last " + tx.summary()));
        lines.add("daemon pid=" + ProcessHandle.current().pid() + " port="
                + currentPort + " up="
                + Duration.between(startedAt, Instant.now()).toSeconds() + "s");
        return lines;
    }

    /**
     * The edit loop as far as it is known, and what is deliberately outside it.
     * <p>
     * Read from what has already been resolved, never by resolving:
     * {@code status} answers in milliseconds and must not start a Maven build
     * to do it. A single-module project says nothing, because there is nothing
     * to say.
     */
    private Optional<String> modulesLine() {
        if (!launch.reactor().isMultiModule()) {
            return Optional.empty();
        }
        Optional<Launch.Project> resolved = launch.resolved();
        if (resolved.isEmpty()) {
            return Optional.of("reactor " + launch.reactor().describe()
                    + "; the edit loop is resolved on the first apply or start");
        }
        List<String> loop = moduleNames(resolved.get().modules());
        List<String> excluded = excludedModules(loop);
        return Optional.of("modules " + String.join(", ", loop)
                + (excluded.isEmpty() ? ""
                        : "  (outside the loop: " + String.join(", ", excluded)
                                + " - the app does not depend on "
                                + (excluded.size() == 1 ? "it" : "them")
                                + ")"));
    }

    private List<String> moduleNames(List<Reactor.Module> modules) {
        return modules.stream().map(Reactor.Module::name).toList();
    }

    private List<String> excludedModules(List<String> loop) {
        return launch.reactor().candidates().stream().map(Reactor.Module::name)
                .filter(name -> !loop.contains(name)).distinct().toList();
    }

    /**
     * What the app has logged as an error since the last apply marked the log.
     * Empty before the first launch, and after an app that has been quiet.
     */
    private List<String> appLogErrors() {
        return app.watch().map(AppLog.Watch::errors).orElse(List.of());
    }

    /**
     * Asks the app for dev-server liveness; empty when no app is registered.
     */
    private Optional<String> frontendStatus() {
        Connector active = transactions.connector();
        if (active == null || !active.isOpen()) {
            return Optional.empty();
        }
        return active.command("FRONTEND", 5).map(reply -> Connector
                .fields(reply).getOrDefault("frontend", "unknown"));
    }

    private List<String> statusJson() {
        String json = "{\"app\":{\"state\":\""
                + app.state().name().toLowerCase() + "\",\"pid\":"
                + app.pid().map(String::valueOf).orElse("null")
                + ",\"exitCode\":"
                + app.exitCode().map(String::valueOf).orElse("null")
                // The reason belongs in the machine-readable answer too: an
                // agent
                // reading only JSON should not have to open app.log to learn
                // that
                // the port was taken.
                + ",\"failureReason\":"
                + app.failureReason()
                        .map(reason -> "\"" + Json.escape(reason) + "\"")
                        .orElse("null")
                + ",\"registered\":" + app.isRegistered()
                + ",\"owner\":\"daemon\"" + ",\"mode\":\"" + app.mode()
                + "\",\"frontend\":\""
                + Json.escape(frontendStatus().orElse("unknown"))
                + "\",\"logErrors\":" + Json.strings(appLogErrors())
                + "},\"modules\":" + modulesJson() + ",\"daemon\":{\"pid\":"
                + ProcessHandle.current().pid() + ",\"port\":" + currentPort
                + ",\"version\":\"" + VERSION + "\",\"uptimeSeconds\":"
                + Duration.between(startedAt, Instant.now()).toSeconds()
                + "},\"transaction\":{\"inFlight\":"
                + transactions.current()
                        .map(tx -> "\"tx#" + tx.id + ":" + tx.state + "\"")
                        .orElse("null")
                + ",\"last\":"
                + transactions.lastTransaction()
                        .map(TransactionEngine.Transaction::json).orElse("null")
                + "}}";
        return List.of(json);
    }

    /**
     * The loop in machine-readable form. {@code loop} is empty until something
     * has resolved it, which is itself the answer to "why did my edit not
     * count?".
     */
    private String modulesJson() {
        List<String> loop = launch.resolved()
                .map(project -> moduleNames(project.modules()))
                .orElse(List.of());
        // Nothing resolved means nothing is known to be excluded either; saying
        // every module is out would be a claim the daemon has not made.
        List<String> excluded = loop.isEmpty() ? List.of()
                : excludedModules(loop);
        return "{\"reactorRoot\":\""
                + Json.escape(launch.reactor().root().toString())
                + "\",\"loop\":" + Json.strings(loop) + ",\"excluded\":"
                + Json.strings(excluded) + "}";
    }

    private boolean tokenMatches(String candidate) {
        return java.security.MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8),
                currentToken.getBytes(StandardCharsets.UTF_8));
    }

    private void requestShutdown() {
        shuttingDown = true;
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // Closing is what unblocks accept(); failure here is not
            // actionable.
        }
    }

    private void cleanup() {
        ScheduledExecutorService watchdog = idleWatchdog;
        if (watchdog != null) {
            watchdog.shutdownNow();
        }
        // Shutting down the daemon stops the app it owns, per the ownership
        // model.
        if (!app.isIdle()) {
            app.stop();
        }
        Handshake.read(root).filter(h -> h.pid == ProcessHandle.current().pid())
                .ifPresent(h -> Handshake.delete(root));
    }
}
