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
        /** Frontend files this change touched, however they were handled. */
        volatile int frontendFiles;
        /** Theme stylesheets pushed into the open page. */
        volatile int themePushed;
        /**
         * Which frontend the app is running: {@code vite}, {@code dev-bundle}.
         */
        volatile String frontendMode = "";
        /** What the app logged while this change was going live. */
        volatile List<String> logErrors = List.of();
        /**
         * Dev-server errors the log already held when this apply started.
         * <p>
         * Every other leg makes its change during the apply, so anything older
         * belongs to what came before and {@code mark()} drops it. Vite does
         * not: it compiles on save, which is before the apply, so its errors
         * are always older than the window and would be dropped along with them
         * - leaving apply to report a clean Stable over a file the browser is
         * refusing to load. For a frontend change the window has to start at
         * the previous apply instead, and this is that carry-over.
         */
        volatile List<String> carriedLogErrors = List.of();
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
                    + Json.escape(frontend) + "\",\"frontendFiles\":"
                    + frontendFiles + ",\"themePushed\":" + themePushed
                    + ",\"frontendMode\":\"" + Json.escape(frontendMode)
                    // The internal separator never leaves the daemon: a control
                    // character in a JSON string is legal but nothing a reader
                    // or a jq expression expects.
                    + "\",\"logErrors\":"
                    + Json.strings(logErrors.stream()
                            .map(line -> line.replace(AppLog.SEGMENT, " | "))
                            .toList())
                    + ",\"timings\":{\"detectMs\":" + detectMs
                    + ",\"compileMs\":" + compileMs + ",\"runtimeMs\":"
                    + runtimeMs + ",\"totalMs\":" + totalMs
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
        // A baseline built while the app is already running must not swallow a
        // frontend edit made since it started - that is exactly the "start,
        // edit, first apply" sequence, and answering "no changes" to it is the
        // bug the frontend leg exists to fix. With no app running there is
        // nothing to be newer than: starting it re-seeds through onConnector.
        fresh.seedFromDisk(app.startedAtMillis().orElse(Long.MAX_VALUE));
        // Said once per baseline rather than per apply: "why did apply not see
        // my edit?" is answerable from daemon.log only if the folder the daemon
        // decided on is written down somewhere.
        fresh.frontend().root().ifPresentOrElse(
                root -> log.line("frontend folder: " + root + " (from "
                        + fresh.frontend().source().label + ")"),
                () -> log.line(
                        "frontend folder: none found; frontend edits are not tracked"));
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
            Compile.ResourceChanges staleResources = compile.staleResources();
            Compile.FrontendChanges frontendChanges = compile.staleFrontend();
            tx.detectMs = (System.nanoTime() - detectStart) / 1_000_000;
            tx.changeSet = new ArrayList<>(changes.modified().stream()
                    .map(compile::relative).toList());
            changes.deleted().forEach(path -> tx.changeSet
                    .add(compile.relative(path) + " (deleted)"));
            staleResources.all()
                    .forEach(path -> tx.changeSet.add(compile.relative(path)));
            frontendChanges.modified()
                    .forEach(path -> tx.changeSet.add(compile.relative(path)));
            frontendChanges.deleted().forEach(path -> tx.changeSet
                    .add(compile.relative(path) + " (deleted)"));

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
            //
            // With one exception, taken before the window closes: a dev server
            // compiles on save, so its complaint about a file in this very
            // change-set is already in the log and would be dropped as
            // somebody else's. Only dev-server errors, and only when a frontend
            // file actually changed, so nothing else is carried across.
            if (!frontendChanges.isEmpty()) {
                tx.carriedLogErrors = app.watch().map(AppLog.Watch::errors)
                        .orElse(List.of()).stream()
                        .filter(AppLog::devServerError).toList();
            }
            app.watch().ifPresent(AppLog.Watch::mark);

            // The resource leg. Runs first because a Java change may also need
            // fresh resources on the classpath, and it is the whole transaction
            // when nothing else changed.
            if (!staleResources.isEmpty()) {
                tx.state = "frontend";
                try {
                    compile.copyResources(staleResources.all());
                    tx.resources = staleResources.size();
                    log.line("resources: copied " + tx.resources
                            + " to the classpath");
                } catch (java.io.IOException e) {
                    return finish(tx, Outcome.FAILED,
                            "resource copy: " + e.getMessage(), "none",
                            "check file permissions under target/classes",
                            started);
                }
                // The copy is not the same as the change being live. Everything
                // outside the public resource roots was read while the app was
                // starting - application.properties above all - and the running
                // JVM will not read it again, so reporting Stable here would be
                // a green answer over values the app is not using.
                if (!staleResources.startup().isEmpty()) {
                    tx.escalation = resourceEscalation(
                            staleResources.startup());
                    log.line("resources: " + tx.escalation
                            + "; only a restart can apply that");
                }
            }

            // The frontend leg decides; it does not act. Which mode the app is
            // in is what the whole decision turns on, and only the app can say,
            // so the probe happens once here and is reused by every leg below.
            String probe = probeFrontend(tx);
            Frontend.Plan plan = compile.frontend().plan(
                    frontendChanges.modified(), frontendChanges.deleted(),
                    isVite(tx, probe, log));
            tx.frontendFiles = plan.size();
            tx.frontendMode = plan.hasWork()
                    ? (plan.vite() ? "vite" : "dev-bundle")
                    : "";
            if (plan.hasWork()) {
                describeFrontend(plan, tx, log);
            }
            if (!plan.escalation().isEmpty()) {
                // Only a Vite build folds a frontend file into the dev bundle,
                // and that runs at startup - so the restart is not a fallback
                // here, it is the mechanism.
                tx.escalation = plan.escalation();
            }

            if (changes.isEmpty() && drift.isEmpty() && tx.escalation.isEmpty()
                    && (!staleResources.isEmpty() || plan.hasWork())) {
                // Reached only with the escalation empty, so there are no
                // startup-only resources left to account for here.
                return finishFrontendOnly(tx, log, staleResources.live(), plan,
                        started);
            }
            if (!staleResources.live().isEmpty()) {
                // Java and resources in one change-set: push the resources too,
                // otherwise the CSS half of the edit would sit on the classpath
                // unseen until something reloaded the page.
                notifyResources(staleResources.live(), log);
            }
            if (tx.escalation.isEmpty()) {
                // Skipped when a restart is coming: it re-reads every frontend
                // file anyway, and pushing first would put a stylesheet on a
                // page that is about to be reloaded.
                notifyFrontend(plan, tx, log);
            }

            if (changes.isEmpty() && drift.isEmpty()
                    && tx.escalation.isEmpty()) {
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
            // An escalation already decided this cannot be redefined away -
            // a drifted classpath, or a frontend change only a rebuild can
            // fold in. Redefining first would report "hot-reload" and return
            // Stable over a change that is not live, which is the one answer
            // this daemon must never give.
            Connector connector = this.connector;
            if (tx.escalation.isEmpty()
                    && app.state() == AppProcess.State.RUNNING
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
     * Why a resource change cannot be made live in the running application.
     * <p>
     * The file is named when it is the only one, because
     * "application.properties changed" is the whole explanation and a count is
     * not.
     */
    private static String resourceEscalation(List<Path> startup) {
        String what = startup.size() == 1
                ? startup.get(0).getFileName() + " changed"
                : startup.size() + " resource(s) changed";
        return what + " (read only while the app starts)";
    }

    /**
     * Asks the app about its frontend, once per apply.
     * <p>
     * Hoisted out of the fast path because the answer decides what the frontend
     * leg does long before that path is reached, and asking twice would cost a
     * second round-trip to learn the same thing.
     *
     * @return the {@code frontend=} field, or {@code unknown} when there is no
     *         app to ask
     */
    private String probeFrontend(Transaction tx) {
        Connector active = this.connector;
        if (app.state() != AppProcess.State.RUNNING || active == null
                || !active.isOpen()) {
            return "unknown";
        }
        Map<String, String> fields = active
                .command("FRONTEND " + compile.frontend().root()
                        .map(Path::toString).orElse(""), 10)
                .map(Connector::fields).orElse(Map.of());
        tx.frontend = fields.getOrDefault("frontend", "unknown");
        return fields.getOrDefault("mode", "") + " "
                + fields.getOrDefault("frontend", "unknown");
    }

    /**
     * Whether Vite is what applied a frontend edit.
     * <p>
     * The app's own mode and a live probe of the dev server can disagree - the
     * mode was read at registration, the probe is measured now - and the probe
     * wins, because a dev server that is answering is applying edits whatever
     * the configuration said.
     */
    private boolean isVite(Transaction tx, String probe, Launch.Log log) {
        String status = tx.frontend;
        boolean byProbe = status.startsWith("up:")
                || status.startsWith("starting(");
        boolean byMode = probe.startsWith("DEVELOPMENT_FRONTEND_LIVERELOAD");
        if (status.startsWith("no-dev-server(") && byMode) {
            log.line("frontend: the app reports mode="
                    + "DEVELOPMENT_FRONTEND_LIVERELOAD but no dev server is"
                    + " running; treating it as a dev bundle");
            return false;
        }
        if (byProbe && !byMode && !probe.isBlank()) {
            log.line("frontend: the app reports a dev bundle but the dev server"
                    + " answers " + status + "; trusting the dev server");
        }
        return byProbe;
    }

    /** The one line that says what was found and what will happen to it. */
    private void describeFrontend(Frontend.Plan plan, Transaction tx,
            Launch.Log log) {
        String where = plan.vite() ? "Vite " + tx.frontend : "dev bundle";
        String consequence;
        if (plan.vite()) {
            consequence = "Vite applied them on save";
        } else if (!plan.escalation().isEmpty()) {
            consequence = "a restart rebuilds the bundle";
        } else if (plan.servedLive().isEmpty()) {
            consequence = "pushed into the open page";
        } else if (plan.themeCss().isEmpty()) {
            consequence = "served from the frontend folder; reloading the page";
        } else {
            consequence = "pushed, and the page reloaded for the rest";
        }
        log.line("frontend: " + plan.size() + " file(s) changed (" + where
                + "); " + consequence);
    }

    /**
     * A change with no Java in it: never a restart. The frontend leg is blocked
     * rather than hung when Vite is down, so the agent gets a terminal answer
     * either way.
     */
    private Transaction finishFrontendOnly(Transaction tx, Launch.Log log,
            List<Path> resources, Frontend.Plan plan, long started) {
        Connector active = this.connector;
        if (app.state() != AppProcess.State.RUNNING || active == null
                || !active.isOpen()) {
            return finish(tx, Outcome.COMPILED, "", "hmr",
                    app.state() == AppProcess.State.RUNNING ? ""
                            : "vaadin-dev start",
                    started);
        }

        String frontend = tx.frontend;
        if (frontend.startsWith("down")) {
            return finish(tx, Outcome.FAILED,
                    "frontend-down: the Vite dev server is not answering on "
                            + frontend.substring(frontend.indexOf(':') + 1),
                    "hmr", "restart the app to bring the dev server back",
                    started);
        }

        long runtimeStart = System.nanoTime();
        if (!resources.isEmpty()) {
            Optional<String> reply = notifyResources(resources, log);
            if (reply.isEmpty() || !reply.get().startsWith("OK")) {
                tx.runtimeMs = (System.nanoTime() - runtimeStart) / 1_000_000;
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
        }
        if (!notifyFrontend(plan, tx, log)) {
            tx.runtimeMs = (System.nanoTime() - runtimeStart) / 1_000_000;
            return finish(tx, Outcome.FAILED, "frontend notify: no reply",
                    "hmr", "see target/devloop/app.log", started);
        }
        tx.runtimeMs = (System.nanoTime() - runtimeStart) / 1_000_000;
        // A push the daemon performed itself is done when the connector
        // answers, so there is nothing to wait for: a stylesheet cannot break a
        // class, and this leg spends no time settling.
        //
        // Vite is the exception. It compiles in its own process, and a
        // TypeScript error surfaces when the browser fetches the module - which
        // is after the save that triggered the HMR push, not before. Returning
        // the instant the probe answers reads an empty log and calls a broken
        // file Stable, which is the same mistake the redefine leg settles to
        // avoid.
        app.watch()
                .ifPresent(watching -> tx.logErrors = plan.vite()
                        ? watching.settle(ERROR_SETTLE_MILLIS)
                        : watching.errors());
        return finish(tx, Outcome.STABLE, "", "hmr", "", started);
    }

    /**
     * Gets the frontend half of a change in front of the browser: theme
     * stylesheets pushed as text, everything the server already serves from
     * disk covered by one reload.
     * <p>
     * In Vite mode this does nothing at all, deliberately - Vite pushed the
     * change when the file was saved, and a reload on top of that would throw
     * away the state its hot update preserved.
     *
     * @return {@code false} only when the app was asked and did not answer
     */
    private boolean notifyFrontend(Frontend.Plan plan, Transaction tx,
            Launch.Log log) {
        Connector active = this.connector;
        if (!plan.hasWork() || plan.vite() || active == null
                || !active.isOpen()) {
            // Nothing to do, but the bytes are accounted for either way: Vite
            // already applied them, so offering them again would be a lie.
            markFrontendApplied(plan);
            return true;
        }
        if (!plan.themeCss().isEmpty()) {
            Optional<String> reply = active
                    .command("THEME " + join(plan.themeCss()), 30);
            if (reply.isEmpty()) {
                return false;
            }
            if (!reply.get().startsWith("OK")) {
                log.line("theme push: " + reply.get());
                return false;
            }
            tx.themePushed = parseInt(
                    Connector.fields(reply.get()).get("pushed"));
        }
        if (!plan.servedLive().isEmpty()) {
            // index.html and theme assets are read from the frontend folder per
            // request, so the bytes are already right and only the open page is
            // stale.
            Optional<String> reply = active.command("RELOAD", 30);
            if (reply.isEmpty() || !reply.get().startsWith("OK")) {
                return false;
            }
        }
        markFrontendApplied(plan);
        return true;
    }

    private void markFrontendApplied(Frontend.Plan plan) {
        List<Path> applied = new ArrayList<>();
        applied.addAll(plan.themeCss());
        applied.addAll(plan.servedLive());
        applied.addAll(plan.bundled());
        compile.markFrontendNotified(
                new Compile.FrontendChanges(applied, plan.deleted()));
    }

    private static String join(List<Path> paths) {
        return paths.stream().map(Path::toString)
                .collect(java.util.stream.Collectors.joining(","));
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
        // The layout boilerplate goes first, not last: a Spring Boot prefix is
        // about a hundred characters of timestamp, level, pid, thread and
        // abbreviated logger, and truncating with it still attached spends the
        // whole budget saying nothing and cuts off the half that would let the
        // reader fix the problem without opening the log at all.
        String trimmed = AppLog.message(line).replace(AppLog.SEGMENT, " | ");
        return trimmed.length() <= 160 ? trimmed
                : trimmed.substring(0, 157) + "...";
    }

    /**
     * How wide a quoted log line gets before it is wrapped onto another row.
     */
    private static final int QUOTE_WIDTH = 100;

    /** How many rows one quoted error may occupy before it is cut off. */
    private static final int QUOTE_ROWS = 6;

    /**
     * One logged error, as the rows it needs.
     * <p>
     * Wrapped rather than truncated. A compiler's message is the one piece of
     * output whose tail matters as much as its head - "Expected `}` but found
     * `EOF`" is the whole diagnosis, and a summary that stops just before it
     * only tells the reader to go and open the log, which is the work this line
     * exists to save them. Segments joined by {@code |} - the opening line of a
     * report and the detail underneath it - get a row each, because they are
     * two separate facts.
     */
    private static List<String> quote(String line) {
        List<String> rows = new ArrayList<>();
        for (String segment : AppLog.message(line).split(AppLog.SEGMENT)) {
            String rest = segment.strip();
            while (!rest.isEmpty()) {
                if (rows.size() == QUOTE_ROWS) {
                    // Whatever is left is a stack or a source excerpt; the log
                    // has it and the header already says where.
                    rows.add("    ...");
                    return rows;
                }
                int cut = breakAt(rest);
                rows.add((rows.isEmpty() ? "  " : "    ")
                        + rest.substring(0, cut).strip());
                rest = rest.substring(cut).strip();
            }
        }
        return rows;
    }

    /**
     * Where to wrap: the last space inside the width.
     * <p>
     * A single token longer than the width - an absolute path, a URL, a stack
     * frame - overflows the row rather than being cut in half. The width is
     * there to keep output readable, and a path broken across two rows is
     * neither readable nor something the reader can copy into an editor, which
     * is the one thing they want to do with it.
     */
    private static int breakAt(String text) {
        if (text.length() <= QUOTE_WIDTH) {
            return text.length();
        }
        int space = text.lastIndexOf(' ', QUOTE_WIDTH);
        if (space > QUOTE_WIDTH / 2) {
            return space;
        }
        int overflow = text.indexOf(' ', QUOTE_WIDTH);
        return overflow < 0 ? text.length() : overflow;
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
        // @JsModule and friends are read by the build, not at runtime: they end
        // up in generated-flow-imports.js, which is written during startup, and
        // the browser reaches them through a bundle chunk keyed by class name.
        // The redefine itself succeeds - the class really does carry the new
        // annotation - but the import is in no chunk the client can load, so
        // reporting hot-reload here would be reporting a change live that is
        // not. Only a restart regenerates the imports.
        String frontend = fields.getOrDefault("frontendImports", "-");
        if (!"-".equals(frontend)) {
            return Optional.of("frontend imports changed (" + frontend
                    + "): @JsModule and friends are read at startup"
                    + " (dev bundle rebuild)");
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
        // Merged here rather than at every assignment site, so no leg can
        // report Stable while a dev-server error it inherited goes unmentioned.
        if (!tx.carriedLogErrors.isEmpty()) {
            List<String> merged = new ArrayList<>(tx.carriedLogErrors);
            tx.logErrors.stream().filter(line -> !merged.contains(line))
                    .forEach(merged::add);
            tx.logErrors = List.copyOf(merged);
        }
        // A superseded transaction is not the answer to "what is the state?".
        if (outcome != Outcome.SUPERSEDED) {
            last = tx;
        }
        return tx;
    }

    /**
     * What an {@code hmr} outcome actually did, as the clauses that apply.
     * <p>
     * Composed rather than templated because one apply can carry a classpath
     * resource, a theme stylesheet and a file the server reads from disk, and
     * naming only one of them would leave a reader wondering about the rest.
     * The resource clause stays first and stays word-for-word what it was.
     */
    private static String hmrDetail(Transaction tx) {
        List<String> clauses = new ArrayList<>();
        if (tx.resources > 0) {
            clauses.add(tx.resources + " resource(s) copied");
            if (!tx.hotswapDetail.isEmpty()) {
                clauses.add(tx.hotswapDetail);
            }
        }
        if (tx.themePushed > 0) {
            clauses.add(tx.themePushed + " theme file(s) pushed in place");
        }
        if ("vite".equals(tx.frontendMode)) {
            clauses.add(tx.frontendFiles
                    + " frontend file(s), applied by Vite (dev server "
                    + tx.frontend + ")");
        } else if (tx.frontendFiles > tx.themePushed) {
            clauses.add((tx.frontendFiles - tx.themePushed)
                    + " frontend file(s) served live, browser reloaded");
        }
        if (clauses.isEmpty()) {
            return tx.resources + " resource(s) copied, " + tx.hotswapDetail;
        }
        return String.join(", ", clauses);
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
            // Nothing was compiled on a frontend-only change, and naming a
            // phase that did not run sends the reader to the wrong place - the
            // same reason the restart header is conditional.
            boolean frontendOnly = tx.classes.isEmpty() && tx.frontendFiles > 0;
            lines.add((frontendOnly ? "frontend" : "compiling")
                    + " → compiled   (" + seconds + ")");
            // "0 class(es)" is a misleading way to describe a frontend-only
            // change that has not been made live yet; name what is actually
            // waiting.
            lines.add((frontendOnly ? tx.frontendFiles + " frontend file(s)"
                    : tx.classes.size() + " class(es)") + "; " + tx.nextAction);
        }
        case STABLE -> {
            if ("hmr".equals(tx.classification)) {
                lines.add("frontend → Stable   (" + seconds + ")");
                lines.add("hmr: " + hmrDetail(tx));
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
                // Naming phases that did not happen sends the reader looking
                // in the wrong place: a frontend-only or pom-only restart
                // compiled nothing and redefined nothing.
                lines.add((tx.classes.isEmpty() ? "" : "compiling → runtime → ")
                        + "restarting → Stable   (" + seconds + ")");
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
            lines.addAll(quote(tx.logErrors.get(0)));
        }
        return lines;
    }
}
