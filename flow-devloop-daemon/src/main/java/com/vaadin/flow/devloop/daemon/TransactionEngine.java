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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The transaction: one logical source change carried through to a stable
 * running app. It is the spine — state, diagnostics and the agent API all hang
 * off it.
 * <p>
 * Two rules make "what is the state of my last change?" answerable:
 * <ul>
 * <li><b>At most one in flight.</b> Concurrent transactions would make the
 * question unanswerable.</li>
 * <li><b>Supersede, don't queue.</b> A new apply cancels the in-flight one and
 * proceeds with the accumulated change-set, because only the latest bytes on
 * disk matter. The superseded caller still gets a terminal answer.</li>
 * </ul>
 * P2 implements the compile leg. The runtime leg lands in P3; until then a
 * successful compile escalates straight to a restart when the app is running,
 * which is the honest way to make the change live with only P1+P2 machinery.
 */
final class TransactionEngine {

    enum Outcome {
        /** Change is live and consistent. The success an agent waits on. */
        STABLE(0),
        /** Compiled, but nothing is running to apply it to. */
        COMPILED(0),
        /** Nothing changed on disk since the last apply. */
        NO_CHANGES(0),
        /** Could not complete; reason names the phase. */
        FAILED(1),
        /** Cancelled by a newer apply. */
        SUPERSEDED(4);

        final int exitCode;

        Outcome(int exitCode) {
            this.exitCode = exitCode;
        }

        String label() {
            return switch (this) {
            case STABLE -> "Stable";
            case COMPILED -> "compiled";
            case NO_CHANGES -> "no changes";
            case FAILED -> "Failed";
            case SUPERSEDED -> "Failed(superseded)";
            };
        }
    }

    /** Everything a transaction carries, per the design. */
    static final class Transaction {
        final int id;
        volatile List<String> changeSet = List.of();
        volatile String state = "pending";
        volatile Outcome outcome;
        volatile String reason = "";
        volatile String classification = "none";
        volatile String nextAction = "";
        volatile List<Compile.Message> diagnostics = List.of();
        volatile List<String> classes = List.of();
        volatile boolean superseded;
        volatile int duplicates;
        volatile String hotswapDetail = "";
        volatile String escalation = "";
        volatile int resources;
        volatile String frontend = "";
        /** What the app logged while this change was going live. */
        volatile List<String> logErrors = List.of();
        long detectMs;
        long compileMs;
        long runtimeMs;
        long totalMs;

        Transaction(int id) {
            this.id = id;
        }

        String json() {
            List<String> diag = diagnostics.stream().map(Compile.Message::json)
                    .toList();
            return "{\"transaction\":\"tx#" + id + "\",\"outcome\":\""
                    + outcome.name().toLowerCase() + "\",\"classification\":\""
                    + classification + "\",\"reason\":\"" + Json.escape(reason)
                    + "\",\"changeSet\":" + Json.strings(changeSet)
                    + ",\"classes\":" + Json.strings(classes)
                    + ",\"diagnostics\":" + Json.array(diag)
                    + ",\"actionsTaken\":\"" + Json.escape(hotswapDetail)
                    + "\",\"escalation\":"
                    + (escalation.isEmpty() ? "null"
                            : "\"" + Json.escape(escalation) + "\"")
                    + ",\"duplicateClassCopies\":" + duplicates
                    + ",\"resources\":" + resources + ",\"frontend\":\""
                    + Json.escape(frontend) + "\",\"logErrors\":"
                    + Json.strings(logErrors) + ",\"timings\":{\"detectMs\":"
                    + detectMs + ",\"compileMs\":" + compileMs
                    + ",\"runtimeMs\":" + runtimeMs + ",\"totalMs\":" + totalMs
                    + "},\"nextAction\":\"" + Json.escape(nextAction) + "\"}";
        }

        String summary() {
            return "tx#" + id + " " + outcome.label()
                    + (reason.isEmpty() ? "" : " (" + reason + ")");
        }
    }

    /**
     * How long an apply follows the app's log after a redefine the JVM
     * accepted. Logging is asynchronous to the call that caused it, so
     * returning the instant the connector replies can read an empty log and
     * call a broken app stable. Long enough for a stack trace on another thread
     * to land, short enough to stay well under the compile it is added to;
     * {@code 0} turns it off.
     */
    private static final long ERROR_SETTLE_MILLIS = Long
            .getLong("vaadin.dev.errorSettleMillis", 400L);

    /** How many paths the change-set line spells out before counting. */
    private static final int PATHS_SHOWN = 5;

    private final Launch launch;
    private final AppProcess app;
    private final AtomicInteger ids = new AtomicInteger();
    private final ReentrantLock compileLock = new ReentrantLock(true);

    /**
     * Built from the resolved module set rather than at construction, because
     * resolving it runs Maven and the daemon must be answering on its socket
     * long before that finishes. Rebuilt when the module set changes.
     */
    private volatile Compile compile;

    private volatile Connector connector;
    private volatile Transaction inFlight;
    private volatile Transaction last;

    TransactionEngine(Launch launch, AppProcess app) {
        this.launch = launch;
        this.app = app;
    }

    /**
     * The compile leg for the current module set, seeded from disk the moment
     * it is built, so an untouched project reports "no changes" on its first
     * apply. A changed module set - someone edited a pom - starts a new
     * baseline rather than carrying stamps that describe a different build.
     */
    private Compile compileFor(Launch.Project project, Launch.Log log) {
        Compile current = compile;
        if (current != null && current.modules().equals(project.modules())) {
            return current;
        }
        if (current != null) {
            log.line("module set changed; re-seeding the change baseline");
        }
        Compile fresh = new Compile(project);
        fresh.seedFromDisk();
        compile = fresh;
        return fresh;
    }

    /** The poms that moved, named while there are few enough to name. */
    /**
     * The change-set, named and then counted, in the shape
     * {@code 2 file(s): a, b}. Truncated past {@link #PATHS_SHOWN} so a
     * wide-reaching change stays one line; the count still gives the size.
     */
    private static String describeChangeSet(List<String> changeSet) {
        String shown = String.join(", ",
                changeSet.subList(0, Math.min(PATHS_SHOWN, changeSet.size())));
        if (changeSet.size() > PATHS_SHOWN) {
            shown += " and " + (changeSet.size() - PATHS_SHOWN) + " more";
        }
        return changeSet.size() + " file(s): " + shown;
    }

    private static String pomsChanged(List<String> poms) {
        if (poms.isEmpty()) {
            return "the build changed";
        }
        return poms.size() <= 2 ? String.join(" and ", poms) + " changed"
                : poms.size() + " poms changed";
    }

    /**
     * The scanned change-set plus sources forced by a classpath change, without
     * duplicates and in the same order the scan would have produced.
     */
    private static Compile.Changes merge(Compile.Changes changes,
            List<Path> forced) {
        List<Path> modified = new ArrayList<>(changes.modified());
        forced.stream().filter(path -> !modified.contains(path))
                .forEach(modified::add);
        modified.sort(java.util.Comparator.naturalOrder());
        return new Compile.Changes(modified, changes.deleted());
    }

    Optional<Transaction> lastTransaction() {
        return Optional.ofNullable(last);
    }

    Optional<Transaction> current() {
        Transaction tx = inFlight;
        return tx != null && tx.outcome == null ? Optional.of(tx)
                : Optional.empty();
    }

    /**
     * Commits pending edits and blocks until the transaction is terminal. The
     * wait is on real state — a compile finishing, a restart registering —
     * never on a timer.
     */
    Transaction apply(Launch.Log log, boolean allowRestart) {
        Transaction tx = new Transaction(ids.incrementAndGet());
        long started = System.nanoTime();

        synchronized (this) {
            Transaction previous = inFlight;
            if (previous != null && previous.outcome == null) {
                previous.superseded = true;
                log.line("superseding tx#" + previous.id);
            }
            inFlight = tx;
        }

        try {
            // Resolved before anything is scanned: which modules are in the
            // loop
            // is what decides where the change-set is even looked for.
            long resolutionsBefore = launch.resolutions();
            Launch.Project project = launch.project(log);
            // Whether a pom actually moved during this apply. Without it, an
            // apply
            // that spent ten seconds re-resolving reports the same bare "no
            // changes" as one that did nothing at all - and the reader cannot
            // tell
            // that the pom edit was seen and deliberately needed no action.
            boolean reresolved = launch.resolutions() != resolutionsBefore;
            Optional<String> resolutionError = launch.resolutionError();
            if (resolutionError.isPresent()) {
                return finish(tx, Outcome.FAILED,
                        "classpath: " + resolutionError.get(), "none",
                        "check the Maven build", started);
            }
            compileFor(project, log);

            long detectStart = System.nanoTime();
            Compile.Changes changes = compile.stale();
            // A pom edit changes no source file, so the scan above cannot see
            // that a module no longer compiles. Whichever module's classpath
            // moved is recompiled whole - that is the only way the error
            // becomes
            // a diagnostic instead of a ClassNotFoundException at runtime, or
            // nothing at all until the next full build.
            List<Path> forced = compile.classpathForced(project);
            if (!forced.isEmpty()) {
                log.line(
                        "classpath changed for "
                                + String.join(", ",
                                        compile.classpathChangedModules(
                                                project))
                                + "; recompiling " + forced.size()
                                + " source(s)");
                changes = merge(changes, forced);
            }
            List<Path> staleResources = compile.staleResources();
            tx.detectMs = (System.nanoTime() - detectStart) / 1_000_000;
            tx.changeSet = new ArrayList<>(changes.modified().stream()
                    .map(compile::relative).toList());
            changes.deleted().forEach(path -> tx.changeSet
                    .add(compile.relative(path) + " (deleted)"));
            staleResources
                    .forEach(path -> tx.changeSet.add(compile.relative(path)));

            // A pom edit touches no source file, so the scan above cannot see
            // it -
            // but it does change what the app would be launched with, and a JVM
            // cannot be told a new class path. Comparing against what the
            // running
            // app was actually launched with is the only honest test, and the
            // only
            // one that stays quiet when a pom edit leaves the classpath alone.
            Optional<String> drift = app.state() == AppProcess.State.RUNNING
                    ? launch.classpathDrift()
                    : Optional.empty();
            drift.ifPresent(detail -> {
                tx.changeSet.add("classpath: " + detail);
                log.line("classpath changed (" + detail
                        + "); only a restart can apply that");
            });

            // Named rather than only counted: when an apply does not do
            // what was expected, the first thing to check is whether the file
            // that was edited is in the set at all. Printed here, ahead of
            // both legs, because a resource-only change returns before the
            // compile below and is just as worth naming.
            if (!tx.changeSet.isEmpty()) {
                log.line("change-set: " + describeChangeSet(tx.changeSet));
            }

            // From here on, whatever the app logs belongs to this change. Read
            // past what is already there rather than blaming it on this apply.
            app.watch().ifPresent(AppLog.Watch::mark);

            // The resource leg. Runs first because a Java change may also need
            // fresh resources on the classpath, and it is the whole transaction
            // when nothing else changed.
            if (!staleResources.isEmpty()) {
                tx.state = "frontend";
                try {
                    compile.copyResources(staleResources);
                    tx.resources = staleResources.size();
                    log.line("resources: copied " + tx.resources
                            + " to the classpath");
                } catch (java.io.IOException e) {
                    return finish(tx, Outcome.FAILED,
                            "resource copy: " + e.getMessage(), "none",
                            "check file permissions under target/classes",
                            started);
                }
            }

            if (changes.isEmpty() && !staleResources.isEmpty()
                    && drift.isEmpty()) {
                return finishResourceOnly(tx, log, staleResources, started);
            }
            if (!staleResources.isEmpty()) {
                // Java and resources in one change-set: push the resources too,
                // otherwise the CSS half of the edit would sit on the classpath
                // unseen until something reloaded the page.
                notifyResources(staleResources, log);
            }

            if (changes.isEmpty() && drift.isEmpty()) {
                // "No changes" is about the disk, and on its own it reads as
                // "all
                // is well" - which it is not when the app is not running at
                // all.
                // A crashed app is exactly where this lands after a pom edit
                // the
                // restart could not survive, and a bare "no changes" there is a
                // green answer over a dead application.
                return finish(tx, Outcome.NO_CHANGES,
                        reresolved
                                ? pomsChanged(launch.changedPoms())
                                        + "; nothing to recompile or restart"
                                : "",
                        "none", switch (app.state()) {
                        case RUNNING -> "edit a source file, then apply";
                        case CRASHED -> "the app is not running; "
                                + "vaadin-dev status says why";
                        default -> "vaadin-dev start";
                        }, started);
            }
            if (bailIfSuperseded(tx, started)) {
                return tx;
            }

            if (!changes.isEmpty()) {
                tx.state = "compiling";

                Compile.Result result;
                compileLock.lock();
                try {
                    // Re-check after queuing: a newer apply may have taken over
                    // while we waited, and its change-set already includes
                    // ours.
                    if (bailIfSuperseded(tx, started)) {
                        return tx;
                    }
                    result = compile.compile(changes.modified(), project);
                } finally {
                    compileLock.unlock();
                }
                tx.compileMs = result.millis();

                if (!result.success()) {
                    tx.diagnostics = result.errors();
                    return finish(tx, Outcome.FAILED, "compile", "none",
                            result.errors().isEmpty() ? "fix the compile error"
                                    : result.errors().get(0).hint()
                                            .orElse("fix the compile error"),
                            started);
                }
                tx.classes = result.writtenClasses();
                if (bailIfSuperseded(tx, started)) {
                    return tx;
                }
            }

            // A drifted classpath cannot be redefined away: the running JVM
            // would
            // keep the dependencies it was started with whatever the bytes say.
            // So the runtime leg is skipped rather than attempted and undone.
            drift.ifPresent(detail -> tx.escalation = "classpath changed ("
                    + detail + ")");

            // --- runtime leg: attempt the atomic redefine, escalate if it
            // cannot
            // stick. What actually happened is the authoritative answer; static
            // prediction is only ever a hint.
            Connector connector = this.connector;
            if (drift.isEmpty() && app.state() == AppProcess.State.RUNNING
                    && connector != null && connector.isOpen()
                    && !tx.classes.isEmpty()) {
                long runtimeStart = System.nanoTime();
                tx.state = "runtime";
                Optional<String> reply = connector.command(
                        "REDEFINE " + String.join(",", tx.classes), 60);
                tx.runtimeMs = (System.nanoTime() - runtimeStart) / 1_000_000;

                if (reply.isEmpty()) {
                    log.line("app did not answer the redefine; escalating");
                } else {
                    Map<String, String> fields = Connector.fields(reply.get());
                    tx.duplicates = parseInt(fields.get("dupes"));
                    if ("OK".equals(fields.get("status"))) {
                        Optional<String> blocker = blockedReason(fields);
                        if (blocker.isEmpty()) {
                            // What the JVM accepted, the app still has to run.
                            blocker = loggedFailure(tx, log);
                        }
                        if (blocker.isEmpty()) {
                            tx.hotswapDetail = "redefineClasses("
                                    + fields.getOrDefault("redefined", "0")
                                    + "); onHotswap completed="
                                    + fields.getOrDefault("completed", "?");
                            // These sources are now live in the JVM, so the
                            // next
                            // apply should not offer them again.
                            compile.markSourcesApplied(changes.modified());
                            return finish(tx, Outcome.STABLE, "", "hot-reload",
                                    visibilityAdvice(fields), started);
                        }
                        log.line("redefine applied but " + blocker.get()
                                + "; escalating to restart");
                        tx.escalation = blocker.get();
                    } else {
                        String detail = fields.getOrDefault("message",
                                reply.get());
                        log.line("redefine rejected: " + detail
                                + "; escalating to restart");
                        tx.escalation = detail;
                    }
                }
            }

            if (!allowRestart || app.state() != AppProcess.State.RUNNING) {
                String next = app.state() == AppProcess.State.RUNNING
                        ? "run apply without --no-restart to make it live"
                        : "vaadin-dev start";
                if (drift.isPresent()) {
                    next = app.state() == AppProcess.State.RUNNING
                            ? "the app is still running the old classpath; "
                                    + "apply without --no-restart, or restart it"
                            : "vaadin-dev start";
                }
                return finish(tx, Outcome.COMPILED, "", "compile-only", next,
                        started);
            }

            // P2 has no redefine yet, so every change escalates. P3 attempts an
            // atomic redefine first and only escalates when the JVM refuses.
            tx.state = "restarting";
            log.line("restarting");
            long runtimeStart = System.nanoTime();
            app.stop();
            AppProcess.Startup startup;
            try {
                startup = app.start(log);
            } catch (java.io.IOException e) {
                tx.runtimeMs = (System.nanoTime() - runtimeStart) / 1_000_000;
                return finish(tx, Outcome.FAILED, "restart: " + e.getMessage(),
                        "restart", "check target/devloop/app.log", started);
            }
            tx.runtimeMs = (System.nanoTime() - runtimeStart) / 1_000_000;
            if (!startup.ok()) {
                // The tail goes to whoever is watching; the reason already
                // names
                // the cause, so --json stays a single parseable object.
                startup.detail().forEach(log::line);
                return finish(tx, Outcome.FAILED,
                        "restart: " + startup.message(), "restart",
                        "check target/devloop/app.log", started);
            }
            return finish(tx, Outcome.STABLE, "", "restart", "", started);
        } catch (RuntimeException e) {
            return finish(tx, Outcome.FAILED, "internal: " + e, "none",
                    "see daemon.log", started);
        }
    }

    /**
     * A resources-only change: never a restart. The frontend leg is blocked
     * rather than hung when Vite is down, so the agent gets a terminal answer
     * either way.
     */
    private Transaction finishResourceOnly(Transaction tx, Launch.Log log,
            List<Path> resources, long started) {
        Connector active = this.connector;
        if (app.state() != AppProcess.State.RUNNING || active == null
                || !active.isOpen()) {
            return finish(tx, Outcome.COMPILED, "", "hmr",
                    app.state() == AppProcess.State.RUNNING ? ""
                            : "vaadin-dev start",
                    started);
        }

        String frontend = active.command("FRONTEND", 10).map(reply -> Connector
                .fields(reply).getOrDefault("frontend", "unknown"))
                .orElse("unknown");
        tx.frontend = frontend;
        if (frontend.startsWith("down")) {
            return finish(tx, Outcome.FAILED,
                    "frontend-down: the Vite dev server is not answering on "
                            + frontend.substring(frontend.indexOf(':') + 1),
                    "hmr", "restart the app to bring the dev server back",
                    started);
        }

        long runtimeStart = System.nanoTime();
        Optional<String> reply = notifyResources(resources, log);
        tx.runtimeMs = (System.nanoTime() - runtimeStart) / 1_000_000;
        if (reply.isEmpty() || !reply.get().startsWith("OK")) {
            return finish(tx, Outcome.FAILED,
                    "resource notify: " + reply.orElse("no reply"), "hmr",
                    "see target/devloop/app.log", started);
        }
        Map<String, String> resourceFields = Connector.fields(reply.get());
        int pushed = parseInt(resourceFields.get("pushed"));
        tx.hotswapDetail = pushed > 0
                ? "pushed " + pushed + " stylesheet(s) in place"
                : "true".equals(resourceFields.get("browserReload"))
                        ? "browser reload requested"
                        : "no browser connected";
        // Reported, not waited for: CSS is the fastest leg there is and pushing
        // a
        // stylesheet cannot break a class, so this leg spends no time settling.
        app.watch().ifPresent(watching -> tx.logErrors = watching.errors());
        return finish(tx, Outcome.STABLE, "", "hmr", "", started);
    }

    /**
     * Tells the app about changed resources and records that the browser has
     * now seen them, which is what keeps the next apply quiet.
     */
    private Optional<String> notifyResources(List<Path> resources,
            Launch.Log log) {
        Connector active = this.connector;
        if (active == null || !active.isOpen()) {
            return Optional.empty();
        }
        String paths = resources.stream().map(Path::toString)
                .collect(java.util.stream.Collectors.joining(","));
        Optional<String> reply = active.command("RESOURCES " + paths, 30);
        if (reply.isPresent() && reply.get().startsWith("OK")) {
            compile.markResourcesNotified(resources);
        } else if (reply.isPresent()) {
            log.line("resource notify: " + reply.get());
        }
        return reply;
    }

    void onConnector(Connector connector) {
        this.connector = connector;
        Compile current = compile;
        if (connector != null && current != null) {
            // An app that has just registered is running exactly what is on
            // disk,
            // so that becomes the new "already live" baseline. Before the first
            // apply there is no compile leg yet, and none is needed: the first
            // one
            // built seeds itself.
            current.seedFromDisk();
        }
    }

    Connector connector() {
        return connector;
    }

    /**
     * What the app logged while the change was going live, and whether any of
     * it means the change is not. The redefine is reported by the JVM and the
     * compile by javac; this is the only leg with an opinion about the app
     * actually running - a context that failed to re-create a bean, a call that
     * landed on a stale proxy - and it is the leg that was missing.
     * <p>
     * Every error is recorded on the transaction, but only the ones that cannot
     * be anything else escalate: an app is free to log an error of its own, and
     * a restart on account of one would be a worse answer than the truth.
     */
    private Optional<String> loggedFailure(Transaction tx, Launch.Log log) {
        AppLog.Watch watching = app.watch().orElse(null);
        if (watching == null) {
            return Optional.empty();
        }
        List<String> errors = watching.settle(ERROR_SETTLE_MILLIS);
        tx.logErrors = errors;
        if (!errors.isEmpty()) {
            log.line("app log: " + errors.size()
                    + " error(s) since the redefine");
        }
        return watching.failure().map(line -> "the app logged " + brief(line));
    }

    /** Enough of a log line to recognise it by, where there is room for one. */
    private static String brief(String line) {
        String trimmed = line.strip();
        return trimmed.length() <= 160 ? trimmed
                : trimmed.substring(0, 157) + "...";
    }

    /**
     * What is left for the developer to do when the bytes are live but the page
     * may not show it yet.
     * <p>
     * {@code onHotswap} refreshes what Flow owns - components and the route
     * registry - and nothing else. A redefined formatter, mapper or plain
     * helper is live and correct, and yet a Grid whose cells were rendered on
     * the server and pushed once keeps showing the old strings until something
     * re-renders them. **Measured**: a method-body change to such a class read
     * as "the apply did not work", when what had actually happened is that
     * nothing asked the view to render again. Reporting {@code Stable} with no
     * further word is the same kind of over-claim as C7, one level out from the
     * bytes.
     * <p>
     * Not escalated to a restart and not turned into a page reload: the change
     * <em>is</em> live, a restart would be a worse answer than the truth, and a
     * forced reload would throw away the no-reload property for a class that
     * may render nothing at all.
     */
    private String visibilityAdvice(Map<String, String> fields) {
        if (!"-".equals(fields.getOrDefault("ui", "-"))) {
            return "";
        }
        if (parseInt(fields.get("redefined")) == 0) {
            // Nothing was loaded to redefine, so nothing rendered from it
            // either:
            // the new bytes are simply what loads the first time it is used.
            return "";
        }
        return "live, but no Vaadin component was redefined - anything already "
                + "rendered keeps its old output until the view renders again "
                + "(interact with it, or reload the page)";
    }

    /**
     * Cases where the JVM accepts the redefine but the change still is not
     * live. Both were measured in P0.5, and both would otherwise be reported as
     * {@code Stable} on an app that is stale or, worse, broken.
     */
    private Optional<String> blockedReason(Map<String, String> fields) {
        String entities = fields.getOrDefault("entities", "-");
        if (!"-".equals(entities)) {
            return Optional.of("entity mapping cannot hot reload (" + entities
                    + "): Hibernate's metamodel and schema are fixed at startup");
        }
        // A method body inside a bean is fine: the proxy delegates to the
        // target
        // and the target's new body runs. What breaks is a change to the
        // class's
        // shape, because the proxy was generated against the old one. HA's
        // Spring
        // plugin could fix that, but it is disabled for stability (see Launch),
        // so these escalate.
        String structural = fields.getOrDefault("structural", "-");
        if ("-".equals(structural)) {
            return Optional.empty();
        }
        String beans = fields.getOrDefault("beans", "-");
        if (!"-".equals(beans)) {
            return Optional.of("structural change to a Spring bean (" + beans
                    + "): the existing proxy would not match the new class");
        }
        // The same failure without an annotation to spot it by. A Spring Data
        // repository is a bare interface, so the annotation check above says
        // nothing about it - and a method added to it lands on a proxy
        // generated
        // from the old interface, whose queries were derived at startup. This
        // is
        // the case that reported Stable for a repository the app can no longer
        // even start with, so the live proxy, not the annotation, is the
        // signal.
        String proxied = fields.getOrDefault("proxied", "-");
        if (!"-".equals(proxied)) {
            return Optional.of("structural change to a proxied type (" + proxied
                    + "): the live proxy was generated from the old shape");
        }
        return Optional.empty();
    }

    private static int parseInt(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean bailIfSuperseded(Transaction tx, long started) {
        if (tx.superseded) {
            finish(tx, Outcome.SUPERSEDED, "a newer apply took over", "none",
                    "read the newer apply's result", started);
            return true;
        }
        return false;
    }

    private Transaction finish(Transaction tx, Outcome outcome, String reason,
            String classification, String nextAction, long startedNanos) {
        tx.outcome = outcome;
        tx.reason = reason;
        tx.classification = classification;
        tx.nextAction = nextAction;
        tx.state = outcome.name().toLowerCase();
        tx.totalMs = (System.nanoTime() - startedNanos) / 1_000_000;
        // A superseded transaction is not the answer to "what is the state?".
        if (outcome != Outcome.SUPERSEDED) {
            last = tx;
        }
        return tx;
    }

    /**
     * Terse, human-and-agent-readable rendering; full detail only on failure.
     */
    List<String> render(Transaction tx) {
        List<String> lines = new ArrayList<>();
        // Locale.ROOT: the default locale would render "0,71s" on a Finnish
        // machine, and this output is parsed by agents as well as read by
        // people.
        String seconds = String.format(java.util.Locale.ROOT, "%.2fs",
                tx.totalMs / 1000.0);
        switch (tx.outcome) {
        case NO_CHANGES -> {
            // The reason is only set when something was examined and found not
            // to
            // matter, which is the case a bare "no changes" reads wrongly.
            lines.add("no changes"
                    + (tx.reason.isEmpty() ? "" : "   (" + tx.reason + ")"));
            // Only when there is something the reader has to know: with the app
            // up, "no changes" is the whole answer.
            if (app.state() != AppProcess.State.RUNNING) {
                lines.add("  → " + tx.nextAction);
            }
        }
        case SUPERSEDED -> lines.add("Failed(superseded): " + tx.reason);
        case FAILED -> {
            // Name the phase that actually failed; "compiling" on a frontend
            // failure sends the reader looking in the wrong place.
            String phase = "hmr".equals(tx.classification) ? "frontend"
                    : tx.escalation.isEmpty() ? "compiling" : "runtime";
            // The reason gets its own line below, so keeping it out of the
            // header
            // is what stops a long one - a restart failure quotes the app's own
            // words - from being printed twice.
            lines.add(phase + " → Failed");
            tx.diagnostics.forEach(message -> {
                lines.add(message.terse());
                message.hint().ifPresent(hint -> lines.add("  → " + hint));
            });
            if (tx.diagnostics.isEmpty() && !tx.reason.isEmpty()) {
                lines.add(tx.reason);
            }
        }
        case COMPILED -> {
            lines.add("compiling → compiled   (" + seconds + ")");
            lines.add(tx.classes.size() + " class(es); " + tx.nextAction);
        }
        case STABLE -> {
            if ("hmr".equals(tx.classification)) {
                lines.add("frontend → Stable   (" + seconds + ")");
                lines.add("hmr: " + tx.resources + " resource(s) copied, "
                        + tx.hotswapDetail);
            } else if ("hot-reload".equals(tx.classification)) {
                lines.add("compiling → runtime → Stable   (" + seconds + ")");
                lines.add("hot-reload: " + tx.hotswapDetail
                        + (tx.duplicates > 0 ? "; " + tx.duplicates
                                + " duplicate class copy/copies also redefined"
                                : ""));
                // Only set when the page may still be showing the old output;
                // silence here is the claim that there is nothing left to do.
                if (!tx.nextAction.isEmpty()) {
                    lines.add("  → " + tx.nextAction);
                }
            } else {
                lines.add("compiling → runtime → restarting → Stable   ("
                        + seconds + ")");
                lines.add("restart: " + tx.escalation);
            }
        }
        }
        // Errors the app logged while this change went live. Not a verdict -
        // the
        // ones that are a verdict escalated already and are named in the reason
        // -
        // but never swallowed either: an apply that says Stable over a stack
        // trace
        // is how a green result stops being worth anything.
        if (!tx.logErrors.isEmpty() && tx.outcome != Outcome.FAILED) {
            lines.add("app log: " + tx.logErrors.size()
                    + " error(s) since the change; see target/devloop/app.log");
            lines.add("  " + brief(tx.logErrors.get(0)));
        }
        return lines;
    }
}
