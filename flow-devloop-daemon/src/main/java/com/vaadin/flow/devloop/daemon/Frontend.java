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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * The frontend folder: where it is, and what a change under it means.
 * <p>
 * All of the frontend leg's judgement lives here, and none of it in
 * {@link TransactionEngine}, for a practical reason: this class is
 * constructible from a directory and a string, so every rule below is
 * unit-testable, while {@code TransactionEngine} needs a {@link Launch} and a
 * running app and is therefore only reachable from an IT. What is left over
 * there is plumbing.
 * <p>
 * Only the application module has a frontend folder. A sibling library
 * contributes frontend assets through
 * {@code src/main/resources/META-INF/frontend}, which is already the resource
 * leg's business.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class Frontend {

    /**
     * What a changed frontend file needs before the browser shows it.
     * <p>
     * The split is not by file type but by who reads the file at runtime, which
     * is what decides whether an edit can be live without rebuilding the
     * bundle.
     */
    enum Kind {
        /**
         * Build output or an editor's leftovers. Never enters a change-set: a
         * generated file changes on every build, and offering it would make
         * every apply noisy.
         */
        IGNORED,
        /**
         * Read from the frontend folder per request, so the bytes on disk are
         * already what the server serves - {@code index.html} through
         * {@code FrontendUtils.getFileFromFrontendDir}, theme assets through
         * {@code StaticFileServer}. Only the open page is stale, so a reload is
         * the whole of the work.
         */
        SERVED_LIVE,
        /**
         * Theme CSS, which Flow can push into the open page as text. The one
         * frontend edit that needs no reload at all.
         */
        THEME_CSS,
        /**
         * Part of the bundle. In dev-bundle mode only a Vite build can fold it
         * in, and that happens at startup - so the honest way to make it live
         * is a restart.
         */
        BUNDLED
    }

    /** Where the frontend folder came from, for the one line that says so. */
    enum Source {
        OVERRIDE("-Dvaadin.dev.frontend"),
        PROPERTY("-Dvaadin.frontend.folder"),
        BUILD_INFO(
                "target/classes/META-INF/VAADIN/config/flow-build-info.json"),
        CONVENTION("the Maven convention"),
        NONE("");

        final String label;

        Source(String label) {
            this.label = label;
        }
    }

    /**
     * What the frontend leg should do about one apply's worth of changes.
     *
     * @param themeCss
     *            theme stylesheets to push into the open page
     * @param servedLive
     *            files the server already serves from disk; the page is stale
     * @param bundled
     *            files that only a bundle rebuild can fold in
     * @param deleted
     *            tracked files that are gone
     * @param vite
     *            whether Vite is the one that applied them
     * @param escalation
     *            why a restart is needed, or empty when none is
     */
    record Plan(List<Path> themeCss, List<Path> servedLive, List<Path> bundled,
            List<Path> deleted, boolean vite, String escalation) {

        static final Plan EMPTY = new Plan(List.of(), List.of(), List.of(),
                List.of(), false, "");

        /** How many files this plan is about, however they are handled. */
        int size() {
            return themeCss.size() + servedLive.size() + bundled.size()
                    + deleted.size();
        }

        /** Whether there is anything for the frontend leg to act on or say. */
        boolean hasWork() {
            return size() > 0;
        }
    }

    /** Relative to the frontend root, and never walked into. */
    private static final List<String> PRUNED = List.of("generated",
            "node_modules");

    private final Path app;
    private final Path classesDir;

    /**
     * Resolved lazily and cached only when found. A negative answer is re-asked
     * on every scan, which costs two {@code isDirectory} calls and means a
     * project that gains {@code src/main/frontend} while the daemon is up
     * starts working without a restart.
     */
    private Path root;
    private Source source = Source.NONE;

    private Frontend(Path app, Path classesDir) {
        this.app = app;
        this.classesDir = classesDir;
    }

    static Frontend of(Reactor.Module app) {
        return new Frontend(app.dir(), app.classesDir());
    }

    /**
     * The frontend folder, if this application has one.
     * <p>
     * The precedence exists because each source knows something the next one
     * does not. The build-info token is preferred over the convention because
     * Flow wrote it after resolving both the legacy {@code frontend/} fallback
     * and the plugin's {@code <frontendDirectory>} parameter - and a project
     * that configures {@code <frontendDirectory>} is exactly the one where
     * "apply does not see my edits" is hardest to diagnose. It is readable
     * before the app has ever started, because {@code prepare-frontend} runs at
     * {@code process-resources} and the daemon's own resolve runs
     * {@code compile}.
     *
     * @return the frontend folder, or empty if there is none
     */
    Optional<Path> root() {
        if (root != null) {
            return Optional.of(root);
        }
        for (Candidate candidate : candidates()) {
            Path path = candidate.path();
            if (path != null && Files.isDirectory(path)) {
                root = path.toAbsolutePath().normalize();
                source = candidate.source();
                return Optional.of(root);
            }
        }
        source = Source.NONE;
        return Optional.empty();
    }

    /** Where {@link #root()} came from; only meaningful once it answered. */
    Source source() {
        return source;
    }

    private record Candidate(Path path, Source source) {
    }

    private List<Candidate> candidates() {
        return List.of(
                new Candidate(configured("vaadin.dev.frontend"),
                        Source.OVERRIDE),
                new Candidate(configured("vaadin.frontend.folder"),
                        Source.PROPERTY),
                new Candidate(fromBuildInfo(), Source.BUILD_INFO),
                new Candidate(
                        app.resolve("src").resolve("main").resolve("frontend"),
                        Source.CONVENTION),
                new Candidate(app.resolve("frontend"), Source.CONVENTION));
    }

    /** A configured directory, absolute or relative to the application. */
    private Path configured(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            return null;
        }
        Path path = Path.of(value.trim());
        return path.isAbsolute() ? path : app.resolve(path);
    }

    /**
     * The folder Flow itself resolved, as recorded in the build-info token.
     * <p>
     * Only {@code frontendFolder} is read from it. The same file also carries
     * {@code frontend.hotdeploy}, and that one must not be trusted: it records
     * what the <em>build</em> was configured with, while the mode the app is
     * actually running in can be set in {@code application.properties} the
     * build never read. The mode comes from the app itself, over the connector.
     */
    private Path fromBuildInfo() {
        Path token = classesDir.resolve("META-INF").resolve("VAADIN")
                .resolve("config").resolve("flow-build-info.json");
        if (!Files.isRegularFile(token)) {
            return null;
        }
        try {
            String json = Files.readString(token, StandardCharsets.UTF_8);
            return Json.stringField(json, "frontendFolder").map(Path::of)
                    .orElse(null);
        } catch (IOException | RuntimeException e) {
            // Unreadable or malformed: fall through to the convention rather
            // than fail an apply over a file that is only an optimisation.
            return null;
        }
    }

    /** Whether a directory should be walked into at all. */
    static boolean isPruned(String directoryName) {
        return PRUNED.contains(directoryName);
    }

    /**
     * What a change to this file means, decided on its path relative to the
     * frontend root. First match wins.
     *
     * @param file
     *            an absolute path under the frontend root
     * @return how the file has to be handled
     */
    Kind kindOf(Path file) {
        Path base = root().orElse(null);
        if (base == null || !file.startsWith(base)) {
            return Kind.IGNORED;
        }
        String relative = base.relativize(file).toString().replace('\\', '/');
        return kindOfRelative(relative);
    }

    /**
     * Package-visible for the tests, which name paths rather than build them.
     */
    static Kind kindOfRelative(String relative) {
        String[] segments = relative.split("/");
        String name = segments[segments.length - 1];

        // split always yields at least one element, so segments[0] is safe.
        if (isPruned(segments[0])) {
            return Kind.IGNORED;
        }
        if (isTemporary(name)) {
            return Kind.IGNORED;
        }
        if (relative.equals("index.html")) {
            // Served from the frontend folder per request in dev mode, which is
            // why the bundle's own hash check skips it.
            return Kind.SERVED_LIVE;
        }
        if (!segments[0].equals("themes") || segments.length < 3) {
            return Kind.BUNDLED;
        }
        if (name.equals("theme.json")) {
            // Not a stylesheet: the bundle validation compares theme.json's
            // contents, so a change to it needs the bundle rebuilt.
            return Kind.BUNDLED;
        }
        return name.endsWith(".css") ? Kind.THEME_CSS : Kind.SERVED_LIVE;
    }

    /**
     * An editor's own file rather than the developer's. The same rule
     * {@code PublicResourcesLiveUpdater} applies on the app side.
     */
    private static boolean isTemporary(String name) {
        return name.startsWith(".") || name.startsWith("~")
                || name.endsWith("~");
    }

    /**
     * Sorts one apply's frontend changes into what has to happen to them.
     *
     * @param modified
     *            frontend files whose bytes the daemon has not acted on yet
     * @param deleted
     *            tracked frontend files that are gone
     * @param vite
     *            whether the app is running a Vite dev server
     * @return the plan, empty when nothing is left after the ignored files
     */
    Plan plan(List<Path> modified, List<Path> deleted, boolean vite) {
        List<Path> themeCss = new java.util.ArrayList<>();
        List<Path> servedLive = new java.util.ArrayList<>();
        List<Path> bundled = new java.util.ArrayList<>();
        for (Path file : modified) {
            switch (kindOf(file)) {
            case THEME_CSS -> themeCss.add(file);
            case SERVED_LIVE -> servedLive.add(file);
            case BUNDLED -> bundled.add(file);
            case IGNORED -> {
                // Not offered in the first place; here for completeness.
            }
            }
        }
        if (themeCss.isEmpty() && servedLive.isEmpty() && bundled.isEmpty()
                && deleted.isEmpty()) {
            return Plan.EMPTY;
        }
        // Vite watches the frontend folder itself and has already applied
        // everything - including deletions - by the time apply runs. Nothing
        // escalates, and the daemon pushes nothing.
        String escalation = vite ? "" : escalationFor(bundled, deleted);
        return new Plan(List.copyOf(themeCss), List.copyOf(servedLive),
                List.copyOf(bundled), List.copyOf(deleted), vite, escalation);
    }

    /**
     * The single most specific reason a restart is needed, or empty.
     * <p>
     * One reason rather than all of them: it is printed as the verdict's second
     * line, and a reader who has just been told the app restarted needs to know
     * why once.
     */
    private static String escalationFor(List<Path> bundled,
            List<Path> deleted) {
        if (!deleted.isEmpty()) {
            return deleted.size()
                    + " frontend file(s) removed (dev bundle rebuild)";
        }
        boolean themeJson = bundled.stream()
                .anyMatch(path -> path.getFileName() != null
                        && path.getFileName().toString().equals("theme.json"));
        if (themeJson && bundled.size() == 1) {
            return "theme.json changed (dev bundle rebuild)";
        }
        if (!bundled.isEmpty()) {
            return "frontend changed (dev bundle rebuild)";
        }
        return "";
    }
}
