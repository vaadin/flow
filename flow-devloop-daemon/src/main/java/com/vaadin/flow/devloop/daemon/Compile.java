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

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The compile leg: change detection plus an in-process compile, over every
 * module in the edit loop.
 * <p>
 * Uses {@link JavaCompiler} rather than shelling out to Maven for two reasons.
 * It skips a JVM start per apply, and its {@link Diagnostic} objects already
 * carry file, line, column, code and message — so the agent-facing
 * {@code diagnostics[]} contract needs no parsing of compiler text output.
 * <p>
 * One instance covers all modules rather than one instance per module: the two
 * fingerprint maps are keyed by absolute source path, which stays unique across
 * modules, and a single instance is what keeps "the change-set" one list with
 * one answer. What every module multiplies is the <em>paths</em>, so each of
 * those is derived from the module that owns the file instead of from one fixed
 * root.
 */
final class Compile {

    /** One compiler error, in the shape the agent API promises. */
    record Message(String file, long line, long column, String code,
            String text, Optional<String> hint) {

        String terse() {
            return file + ":" + line + ":" + column + "  error: " + text;
        }

        String json() {
            return "{\"file\":\"" + Json.escape(file) + "\",\"line\":" + line
                    + ",\"column\":" + column + ",\"code\":\""
                    + Json.escape(code) + "\",\"message\":\""
                    + Json.escape(text) + "\",\"hint\":"
                    + hint.map(h -> "\"" + Json.escape(h) + "\"").orElse("null")
                    + "}";
        }
    }

    record Result(boolean success, List<Message> errors,
            List<String> writtenClasses, long millis) {
    }

    /**
     * A file's identity for change detection. Size is included because a same-
     * millisecond rewrite of a different length is otherwise invisible.
     */
    record Stamp(long modified, long size) {
    }

    record Changes(List<Path> modified, List<Path> deleted) {
        boolean isEmpty() {
            return modified.isEmpty() && deleted.isEmpty();
        }

        int size() {
            return modified.size() + deleted.size();
        }
    }

    /**
     * Frontend changes. Separate from {@link Changes} because these never
     * compile and are never copied anywhere - what happens to them is decided
     * by {@link Frontend}, not by this class.
     */
    record FrontendChanges(List<Path> modified, List<Path> deleted) {

        static final FrontendChanges EMPTY = new FrontendChanges(List.of(),
                List.of());

        boolean isEmpty() {
            return modified.isEmpty() && deleted.isEmpty();
        }

        int size() {
            return modified.size() + deleted.size();
        }
    }

    /**
     * What a change to a resource means, decided by where the file sits under
     * {@code src/main/resources}.
     * <p>
     * The split is by who reads the file at runtime, the same rule the frontend
     * leg uses. Only the public roots are re-read per request; everything else
     * - {@code application.properties} first among them - was read once while
     * the application was starting, so a fresh copy on the classpath changes
     * nothing about what the running JVM is using.
     */
    enum ResourceKind {
        /**
         * An editor's own file rather than the developer's. Never enters a
         * change-set: a swap file must not copy itself onto the classpath, and
         * it certainly must not restart the application.
         */
        IGNORED,
        /**
         * Served from the classpath per request, so the copy into
         * {@code target/classes} is the whole of the work and the browser only
         * has to be told.
         */
        LIVE,
        /**
         * Read while the application starts and not again - configuration,
         * bundled frontend assets, anything Spring or Flow folded into state at
         * boot. The copy keeps the classpath honest, but only a restart makes
         * the new bytes take effect.
         */
        STARTUP
    }

    /**
     * One apply's worth of resource changes, split by {@link ResourceKind}.
     * <p>
     * Each half is a {@link Changes} - the same "edited and deleted" pair the
     * compile leg uses - because a deletion asks the same question an edit
     * does, and it is only the consequence that differs by kind.
     *
     * @param live
     *            resources the browser can be shown once the classpath copy has
     *            been written or removed
     * @param startup
     *            resources the running application will not read again
     */
    record ResourceChanges(Changes live, Changes startup) {

        boolean isEmpty() {
            return live.isEmpty() && startup.isEmpty();
        }

        int size() {
            return live.size() + startup.size();
        }

        /** Sources whose bytes have to be written onto the classpath. */
        List<Path> copies() {
            return joined(live.modified(), startup.modified());
        }

        /** Sources whose classpath copy has to go. */
        List<Path> removals() {
            return joined(live.deleted(), startup.deleted());
        }

        private static List<Path> joined(List<Path> first, List<Path> second) {
            return Stream.concat(first.stream(), second.stream())
                    .sorted(Comparator.naturalOrder()).toList();
        }
    }

    /**
     * The classpath-root-relative directories a servlet container or Spring
     * Boot serves static content from. A resource outside them is never fetched
     * over HTTP, so copying it can never be what makes an edit visible.
     */
    private static final List<String> PUBLIC_RESOURCE_ROOTS = List
            .of("META-INF/resources/", "static/", "public/", "resources/");

    /** A file and the module it belongs to, which is all a walk ever needs. */
    private interface Visitor {
        void accept(Reactor.Module module, Path file, Stamp stamp);
    }

    /** Fingerprints of Java sources as of the last time they went live. */
    private final Map<Path, Stamp> applied = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Fingerprints as of the last browser notification, keyed by source path.
     */
    private final Map<Path, Stamp> notified = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Fingerprints of frontend files as of the last time the daemon acted on
     * them.
     * <p>
     * There is deliberately no {@code copyIsCurrent} counterpart as there is
     * for resources: a frontend file has no {@code target/classes} mirror and
     * must not get one. In dev-bundle mode Flow reads the frontend folder from
     * source per request, and in Vite mode Vite's root <em>is</em> that folder.
     * So staleness here is the single question "has the daemon acted on these
     * bytes yet?".
     */
    private final Map<Path, Stamp> frontendNotified = new java.util.concurrent.ConcurrentHashMap<>();

    /** The edit loop, application module first. */
    private final List<Reactor.Module> modules;

    /** The application's frontend folder, and the rules about it. */
    private final Frontend frontend;

    /**
     * The compile classpath each module was last compiled against.
     * <p>
     * Seeded from the build as it stands when this instance is created, so the
     * first pom edit after that is measurable. Without it a dependency removal
     * is invisible to the compile leg: no {@code .java} file changes, so
     * nothing is stale, so javac never runs and the error surfaces - if at all
     * - as a {@code ClassNotFoundException} at runtime, or not until the next
     * full Maven build. Maven's own {@code compile} does not fill the gap
     * either; it was measured not to recompile on a dependency change here.
     */
    private final Map<String, String> compiledAgainst = new java.util.concurrent.ConcurrentHashMap<>();

    Compile(Launch.Project project) {
        this.modules = List.copyOf(project.modules());
        this.frontend = Frontend.of(project.app());
        for (Reactor.Module module : modules) {
            compiledAgainst.put(module.artifactId(),
                    Launch.membership(project.compileClasspath(module)));
        }
    }

    List<Reactor.Module> modules() {
        return modules;
    }

    /**
     * The application's frontend folder and what a change under it means.
     * <p>
     * Held here rather than on {@link Reactor.Module} because exactly one
     * module in the loop has a frontend folder, and this instance is rebuilt
     * precisely when the module set changes - the only event in a daemon's life
     * that can move it.
     */
    Frontend frontend() {
        return frontend;
    }

    /**
     * The application module, which is the base every reported path is relative
     * to.
     */
    private Reactor.Module app() {
        return modules.get(0);
    }

    /**
     * The module a file belongs to. A linear scan: the loop holds a handful of
     * modules, and this replaces every place that used to relativize against
     * one fixed root and would now throw for a file in a sibling.
     */
    private Optional<Reactor.Module> moduleOf(Path file) {
        return modules.stream().filter(module -> module.owns(file)).findFirst();
    }

    /**
     * Resources the daemon has not acted on yet, split by what acting on them
     * means.
     * <p>
     * Two questions have to be asked, and the artifact comparison alone answers
     * only the first:
     * <ul>
     * <li><b>Is the classpath copy current?</b> Flow watches the source tree
     * but never refreshes {@code target/classes}, so anything that re-fetches
     * the file - a page reload, a new tab - would get stale bytes.</li>
     * <li><b>Has the daemon acted on this content?</b> An IDE that copies
     * resources on save (IntelliJ does, with auto-build on) makes the classpath
     * copy current on its own, and then a pure artifact check reports "no
     * changes" for an edit the browser has never received. So the daemon also
     * remembers the fingerprint of every resource as of the last time it acted
     * on it.</li>
     * </ul>
     * Both questions are asked of every resource; only the consequence of a
     * "yes" differs, which is why the two kinds come back separately.
     * <p>
     * Deletions are the third question, and the walk cannot answer it: a file
     * that is gone is not visited. The fingerprint map is the inventory that
     * can - every resource on disk at the last seed is a key in it - and a
     * deletion has to be noticed, because the copy under {@code target/classes}
     * is what the application actually reads. Leave that behind and the file
     * goes on being served, and the config goes on being loaded, until the next
     * full Maven build.
     * <p>
     * The fingerprint map is seeded at daemon start, so a first apply with
     * nothing edited is still quiet.
     */
    ResourceChanges staleResources() {
        List<Path> live = new ArrayList<>();
        List<Path> startup = new ArrayList<>();
        List<Path> deletedLive = new ArrayList<>();
        List<Path> deletedStartup = new ArrayList<>();
        java.util.Set<Path> seen = new java.util.HashSet<>();
        forEachResource((module, source, stamp) -> {
            seen.add(source);
            if (!copyIsCurrent(module, source)
                    || !stamp.equals(notified.get(source))) {
                (resourceKindOf(module, source) == ResourceKind.LIVE ? live
                        : startup).add(source);
            }
        });
        for (Path source : notified.keySet()) {
            if (seen.contains(source)) {
                continue;
            }
            resourceOwner(source).ifPresent(module -> (resourceKindOf(module,
                    source) == ResourceKind.LIVE ? deletedLive : deletedStartup)
                    .add(source));
        }
        return new ResourceChanges(
                new Changes(sorted(live), sorted(deletedLive)),
                new Changes(sorted(startup), sorted(deletedStartup)));
    }

    private static List<Path> sorted(List<Path> paths) {
        return paths.stream().sorted(Comparator.naturalOrder()).toList();
    }

    /**
     * Records that the browser has been told about these resources.
     * <p>
     * Only ever called for {@link ResourceKind#LIVE} files. A startup-only
     * resource goes live when the application restarts, and
     * {@link #seedFromDisk()} is what records that.
     */
    void markResourcesNotified(List<Path> resources) {
        for (Path source : resources) {
            stampOf(source).ifPresent(stamp -> notified.put(source, stamp));
        }
    }

    /**
     * Drops resources from the inventory, so a deletion is reported once.
     * <p>
     * Called for {@link ResourceKind#LIVE} deletions only, and for the same
     * reason {@link #markResourcesNotified} is: the classpath is what a live
     * resource is read from, so removing the copy is the whole of the change. A
     * startup-only deletion stays in the inventory until the application
     * restarts and {@link #seedResources()} clears it, because until then the
     * running JVM is still holding what the file used to say.
     */
    void forgetResources(List<Path> resources) {
        resources.forEach(notified::remove);
    }

    /** Seeds the fingerprints, so an untouched project reports no changes. */
    void seedResources() {
        notified.clear();
        forEachResource((module, source, stamp) -> notified.put(source, stamp));
    }

    /**
     * Frontend files the daemon has not acted on yet, and tracked ones that are
     * gone.
     * <p>
     * Deletions are reported here and not for Java sources because the
     * inventory is complete - every frontend file the daemon has ever stamped
     * is a key in the map - and because a deletion matters: a removed module
     * the bundle still imports breaks the next build, and a removed stylesheet
     * leaves what was already pushed on the page.
     */
    FrontendChanges staleFrontend() {
        Path root = frontend.root().orElse(null);
        if (root == null) {
            return FrontendChanges.EMPTY;
        }
        List<Path> modified = new ArrayList<>();
        java.util.Set<Path> seen = new java.util.HashSet<>();
        forEachFrontendFile(root, (file, stamp) -> {
            seen.add(file);
            if (!stamp.equals(frontendNotified.get(file))) {
                modified.add(file);
            }
        });
        List<Path> deleted = frontendNotified.keySet().stream()
                .filter(path -> !seen.contains(path))
                .sorted(Comparator.naturalOrder()).toList();
        modified.sort(Comparator.naturalOrder());
        return new FrontendChanges(List.copyOf(modified), deleted);
    }

    /** Records that the app has been told about these frontend changes. */
    void markFrontendNotified(FrontendChanges changes) {
        for (Path file : changes.modified()) {
            stampOf(file).ifPresent(stamp -> frontendNotified.put(file, stamp));
        }
        changes.deleted().forEach(frontendNotified::remove);
    }

    /**
     * Seeds the fingerprints, so an untouched project reports no changes.
     * <p>
     * Files newer than the cutoff are deliberately left unstamped. A frontend
     * file has no artifact to be compared against - no {@code .class}, no
     * classpath copy - so without this a baseline taken while the app is
     * already running would swallow an edit made since it started, and the very
     * first apply after a cold daemon would answer "no changes" to the change
     * it was asked about. The cutoff is the app's own start time, which is what
     * read the frontend folder.
     *
     * @param cutoffMillis
     *            the newest modification time still considered live;
     *            {@code Long.MAX_VALUE} to accept everything on disk
     */
    void seedFrontend(long cutoffMillis) {
        frontendNotified.clear();
        frontend.root()
                .ifPresent(root -> forEachFrontendFile(root, (file, stamp) -> {
                    if (stamp.modified() <= cutoffMillis) {
                        frontendNotified.put(file, stamp);
                    }
                }));
    }

    /** A frontend file and its fingerprint; there is no owning module. */
    private interface FrontendVisitor {
        void accept(Path file, Stamp stamp);
    }

    /**
     * Walks the frontend tree, pruning rather than filtering.
     * <p>
     * {@code Files.walk} cannot skip a subtree, and both {@code generated/} and
     * a project's {@code node_modules/} can hold tens of thousands of files
     * that would be stat-ed on every apply only to be discarded. Kept separate
     * from {@link #walk} rather than generalising it: that one is the compile
     * leg's, it is tested, and it has no reason to grow a pruning concept.
     */
    private void forEachFrontendFile(Path root, FrontendVisitor action) {
        try {
            Files.walkFileTree(root, new java.nio.file.SimpleFileVisitor<>() {
                @Override
                public java.nio.file.FileVisitResult preVisitDirectory(Path dir,
                        BasicFileAttributes attrs) {
                    Path name = dir.getFileName();
                    return name != null && !dir.equals(root)
                            && Frontend.isPruned(name.toString())
                                    ? java.nio.file.FileVisitResult.SKIP_SUBTREE
                                    : java.nio.file.FileVisitResult.CONTINUE;
                }

                @Override
                public java.nio.file.FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) {
                    if (attrs.isRegularFile()
                            && frontend.kindOf(file) != Frontend.Kind.IGNORED) {
                        action.accept(file,
                                new Stamp(attrs.lastModifiedTime().toMillis(),
                                        attrs.size()));
                    }
                    return java.nio.file.FileVisitResult.CONTINUE;
                }

                @Override
                public java.nio.file.FileVisitResult visitFileFailed(Path file,
                        IOException exception) {
                    // An unreadable file yields an empty change-set, not a
                    // failed apply.
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            // Same contract as the compile leg's walk.
        }
    }

    private boolean copyIsCurrent(Reactor.Module module, Path source) {
        Path target = module.targetFor(source);
        try {
            return Files.isRegularFile(target)
                    && Files.getLastModifiedTime(source)
                            .compareTo(Files.getLastModifiedTime(target)) <= 0;
        } catch (IOException e) {
            return false;
        }
    }

    private Optional<Stamp> stampOf(Path file) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(file,
                    BasicFileAttributes.class);
            return Optional.of(new Stamp(attrs.lastModifiedTime().toMillis(),
                    attrs.size()));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private void forEachResource(Visitor action) {
        for (Reactor.Module module : modules) {
            java.util.function.Predicate<Path> tracked = file -> resourceKindOf(
                    module, file) != ResourceKind.IGNORED;
            walk(module, module.resourceDir(), tracked, action);
        }
    }

    private ResourceKind resourceKindOf(Reactor.Module module, Path file) {
        return resourceKindOf(module.resourceDir().relativize(file).toString());
    }

    /**
     * What a change to this resource means, decided on its path relative to the
     * module's resource root.
     * <p>
     * Package-visible for the tests, which name paths rather than build them.
     *
     * @param relative
     *            the resource path relative to {@code src/main/resources}
     * @return how the file has to be handled
     */
    static ResourceKind resourceKindOf(String relative) {
        String path = relative.replace(File.separatorChar, '/');
        String name = path.substring(path.lastIndexOf('/') + 1);
        if (name.startsWith(".") || name.startsWith("~")
                || name.endsWith("~")) {
            return ResourceKind.IGNORED;
        }
        return PUBLIC_RESOURCE_ROOTS.stream().anyMatch(path::startsWith)
                ? ResourceKind.LIVE
                : ResourceKind.STARTUP;
    }

    /**
     * Removes the classpath copies of resources whose sources are gone; returns
     * what was removed.
     * <p>
     * {@code deleteIfExists} rather than {@code delete}: a startup-only
     * deletion is offered again on every apply until the restart that clears
     * it, and the second offer has nothing left to remove. An empty directory
     * left behind is deliberate - it is what {@code mvn} leaves too, and a
     * daemon that prunes upwards would eventually prune something the build put
     * there.
     */
    List<Path> removeResourceCopies(List<Path> sources) throws IOException {
        List<Path> removed = new ArrayList<>();
        for (Path source : sources) {
            Optional<Reactor.Module> owner = resourceOwner(source);
            if (owner.isEmpty()) {
                continue;
            }
            Path target = owner.get().targetFor(source);
            if (Files.deleteIfExists(target)) {
                removed.add(target);
            }
        }
        return removed;
    }

    /**
     * The module whose resource tree a file belongs to.
     * <p>
     * Prefix-matched rather than {@link #moduleOf} so it answers for a path
     * that no longer exists, which is what a deletion is, and so a Java source
     * can never be mistaken for a resource.
     */
    private Optional<Reactor.Module> resourceOwner(Path resource) {
        return modules.stream()
                .filter(module -> resource.startsWith(module.resourceDir()))
                .findFirst();
    }

    /** Copies changed resources onto the classpath; returns what was copied. */
    List<Path> copyResources(List<Path> sources) throws IOException {
        List<Path> copied = new ArrayList<>();
        for (Path source : sources) {
            Optional<Reactor.Module> owner = moduleOf(source);
            if (owner.isEmpty()) {
                // Only reachable when a module has just left the loop; the next
                // apply will not offer the file at all.
                continue;
            }
            Path target = owner.get().targetFor(source);
            Files.createDirectories(target.getParent());
            Files.copy(source, target,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            copied.add(target);
        }
        return copied;
    }

    /**
     * The change-set: sources that either need compiling, or are compiled but
     * not yet live in the running JVM, across every module in the loop.
     * <p>
     * The artifact comparison alone is not enough. It is right that a source
     * newer than its {@code .class} needs work, and it survives a daemon
     * restart without state — but an IDE building on save writes the
     * {@code .class} first, and then the artifact looks current while the JVM
     * is still running the old bytecode. That reported "no changes" for a real
     * edit. So the daemon also tracks which source fingerprints it has actually
     * made live, reset whenever the app starts or restarts (at which point the
     * JVM has loaded whatever is on disk).
     * <p>
     * A failed compile still stays in the change-set until it succeeds, which
     * is what "only the latest bytes on disk matter" requires.
     * <p>
     * Deleted sources are not detected yet; removing a class needs the stale
     * artifact cleaned up too, which is P3 work alongside class removal.
     */
    Changes stale() {
        List<Path> modified = new ArrayList<>();
        forEachSource((module, source, stamp) -> {
            // Two questions again, and for the same reason as resources. The
            // artifact check answers "does this need compiling?"; it cannot
            // answer "is this live in the running JVM?" - an IDE building on
            // save
            // makes the .class newer than the .java, and a pure artifact check
            // then reports "no changes" for an edit the JVM has never loaded.
            if (isStale(module, source) || !stamp.equals(applied.get(source))) {
                modified.add(source);
            }
        });
        modified.sort(Comparator.naturalOrder());
        return new Changes(modified, List.of());
    }

    /**
     * Every source of every module whose compile classpath has moved since it
     * was last compiled - a dependency added or removed, in that module or in
     * one it inherits from.
     * <p>
     * A whole module rather than a guess at which files care: a removed
     * dependency breaks whichever file happened to import it, and there is no
     * cheaper way to find out than to ask javac. This is the only path that
     * recompiles files nothing has edited, and it runs only when a pom actually
     * changed the classpath, which is rare enough to pay for.
     */
    List<Path> classpathForced(Launch.Project project) {
        List<Path> forced = new ArrayList<>();
        for (Reactor.Module module : modules) {
            String previous = compiledAgainst.get(module.artifactId());
            if (previous == null || previous.equals(
                    Launch.membership(project.compileClasspath(module)))) {
                continue;
            }
            walk(module, module.sourceDir(),
                    path -> path.toString().endsWith(".java"),
                    (owner, file, stamp) -> forced.add(file));
        }
        forced.sort(Comparator.naturalOrder());
        return forced;
    }

    /**
     * The modules {@link #classpathForced} is answering for, for one log line.
     */
    List<String> classpathChangedModules(Launch.Project project) {
        return modules.stream().filter(module -> {
            String previous = compiledAgainst.get(module.artifactId());
            return previous != null && !previous.equals(
                    Launch.membership(project.compileClasspath(module)));
        }).map(Reactor.Module::name).toList();
    }

    /** Records that these sources are now live in the running JVM. */
    void markSourcesApplied(List<Path> sources) {
        for (Path source : sources) {
            stampOf(source).ifPresent(stamp -> applied.put(source, stamp));
        }
    }

    /**
     * Declares everything on disk to be live, which is true immediately after
     * the app starts or restarts: it loaded its classes and resources from
     * there.
     */
    void seedFromDisk() {
        seedFromDisk(Long.MAX_VALUE);
    }

    /**
     * @param frontendCutoffMillis
     *            how new a frontend file may be and still count as live; see
     *            {@link #seedFrontend(long)}
     */
    void seedFromDisk(long frontendCutoffMillis) {
        applied.clear();
        forEachSource((module, source, stamp) -> applied.put(source, stamp));
        seedResources();
        // Load-bearing for the frontend leg, not just tidiness: a bundled
        // frontend edit escalates to a restart, the restart re-registers, and
        // this runs again. Without it the same file would be offered after the
        // restart that already folded it into the bundle, and would restart the
        // app for ever.
        seedFrontend(frontendCutoffMillis);
    }

    private void forEachSource(Visitor action) {
        for (Reactor.Module module : modules) {
            walk(module, module.sourceDir(),
                    path -> path.toString().endsWith(".java"), action);
        }
    }

    private void walk(Reactor.Module module, Path tree,
            java.util.function.Predicate<Path> accept, Visitor action) {
        if (!Files.isDirectory(tree)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(tree)) {
            walk.filter(Files::isRegularFile).filter(accept)
                    .forEach(file -> stampOf(file).ifPresent(
                            stamp -> action.accept(module, file, stamp)));
        } catch (IOException ignored) {
            // An unreadable tree yields an empty change-set, not a failure.
        }
    }

    private boolean isStale(Reactor.Module module, Path source) {
        try {
            Path artifact = module.artifactFor(source);
            if (!Files.isRegularFile(artifact)) {
                return true;
            }
            return Files.getLastModifiedTime(source)
                    .compareTo(Files.getLastModifiedTime(artifact)) > 0;
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * Compiles a change-set, one javac invocation per module.
     * <p>
     * Grouped rather than compiled in one pass because javac takes a single
     * {@code -d}: one shared output would write a library module's classes into
     * the application's {@code target/classes}, where Maven would never look
     * for them again and where the next {@code mvn} run would not delete them
     * either. Each group also compiles against its own module's classpath, so a
     * library cannot silently start depending on a type only the application
     * declares.
     * <p>
     * Groups run with the application last, since every in-loop module is one
     * of its dependencies, and the first failing group ends the compile: a
     * downstream module's errors after an upstream failure are consequences,
     * and reporting them would bury the one line that has to be fixed.
     */
    Result compile(List<Path> sources, Launch.Project project) {
        long started = System.nanoTime();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new Result(false, List.of(new Message("-", 0, 0,
                    "no-compiler",
                    "no system Java compiler; the daemon must run on a JDK",
                    Optional.empty())), List.of(), 0);
        }

        List<String> written = new ArrayList<>();
        for (Map.Entry<Reactor.Module, List<Path>> group : groupByModule(
                sources).entrySet()) {
            Result result = compileModule(compiler, group.getKey(),
                    group.getValue(), project);
            if (!result.success()) {
                return new Result(false, result.errors(), List.of(),
                        (System.nanoTime() - started) / 1_000_000);
            }
            written.addAll(result.writtenClasses());
        }
        return new Result(true, List.of(), written.stream().sorted().toList(),
                (System.nanoTime() - started) / 1_000_000);
    }

    /** The change-set by module, dependencies before the application. */
    private Map<Reactor.Module, List<Path>> groupByModule(List<Path> sources) {
        Map<Reactor.Module, List<Path>> grouped = new LinkedHashMap<>();
        for (Reactor.Module module : modules.subList(1, modules.size())) {
            collect(grouped, module, sources);
        }
        collect(grouped, app(), sources);
        return grouped;
    }

    private void collect(Map<Reactor.Module, List<Path>> into,
            Reactor.Module module, List<Path> sources) {
        List<Path> owned = sources.stream().filter(module::owns).toList();
        if (!owned.isEmpty()) {
            into.put(module, owned);
        }
    }

    private Result compileModule(JavaCompiler compiler, Reactor.Module module,
            List<Path> sources, Launch.Project project) {
        long started = System.nanoTime();
        DiagnosticCollector<JavaFileObject> collected = new DiagnosticCollector<>();
        long before = System.currentTimeMillis();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(
                collected, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units = fm
                    .getJavaFileObjectsFromFiles(
                            sources.stream().map(Path::toFile).toList());
            List<String> options = List.of("-classpath",
                    project.compileClasspath(module), "-d",
                    module.classesDir().toString(), "-proc:none", "-encoding",
                    "UTF-8", "-nowarn");
            boolean ok = compiler
                    .getTask(null, fm, collected, options, null, units).call();
            List<Message> errors = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> d : collected
                    .getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    errors.add(toMessage(d));
                }
            }
            if (!ok && errors.isEmpty()) {
                errors.add(new Message("-", 0, 0, "unknown",
                        "compilation failed without an error diagnostic",
                        Optional.empty()));
            }
            long millis = (System.nanoTime() - started) / 1_000_000;
            List<String> classes = ok ? classesWrittenSince(module, before)
                    : List.of();
            if (ok) {
                // Only on success: a failed compile has to stay forced, or the
                // next apply would report the broken module as having no
                // changes.
                compiledAgainst.put(module.artifactId(),
                        Launch.membership(project.compileClasspath(module)));
            }
            return new Result(ok, errors, classes, millis);
        } catch (IOException e) {
            return new Result(false,
                    List.of(new Message("-", 0, 0, "io-error",
                            String.valueOf(e.getMessage()), Optional.empty())),
                    List.of(), (System.nanoTime() - started) / 1_000_000);
        }
    }

    /**
     * The file a diagnostic points at. Bare file name for the application,
     * whose output shape is documented, and prefixed with the module directory
     * for anything else — "Task.java:12" in a project with five modules is not
     * enough to open the file with.
     */
    private Message toMessage(Diagnostic<? extends JavaFileObject> d) {
        String file = "-";
        if (d.getSource() != null) {
            Path path = Path.of(d.getSource().getName());
            file = path.getFileName().toString();
            Optional<Reactor.Module> owner = moduleOf(path);
            if (owner.isPresent() && !owner.get().equals(app())) {
                file = owner.get().name() + "/" + file;
            }
        }
        String text = tidy(d.getMessage(null));
        return new Message(file, d.getLineNumber(), d.getColumnNumber(),
                String.valueOf(d.getCode()), text, hintFor(d.getCode(), text));
    }

    /**
     * javac's messages are multi-line and, once flattened, repetitive ("cannot
     * find symbol symbol: method x()"). Output length is a real cost for an
     * agent reading this every apply, so collapse the duplication and shorten
     * fully-qualified names to simple ones.
     */
    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");

    private static final Pattern SYMBOL_LABEL = Pattern
            .compile("\\s+symbol:\\s+");

    private static final Pattern LOCATION_LABEL = Pattern
            .compile("\\s+location:\\s+");

    /** A qualified name, whose package part is what gets dropped. */
    private static final Pattern QUALIFIED_NAME = Pattern
            .compile("\\b(?:\\p{Lower}\\w*\\.)+(\\p{Upper}\\w*)");

    static String tidy(String raw) {
        String text = raw == null ? ""
                : WHITESPACE_RUN.matcher(raw).replaceAll(" ").trim();
        text = text.replace("cannot find symbol symbol:",
                "cannot find symbol:");
        text = SYMBOL_LABEL.matcher(text).replaceAll(" ");
        text = LOCATION_LABEL.matcher(text).replaceAll(" in ");
        // com.example.foo.Bar -> Bar, but leave lowercase-only words alone.
        return QUALIFIED_NAME.matcher(text).replaceAll("$1");
    }

    /**
     * A next-action hint for the handful of errors an agent actually hits.
     * Deliberately narrow: a wrong hint is worse than none, so anything
     * unrecognised gets no hint rather than a guess.
     */
    private Optional<String> hintFor(String code, String text) {
        if (code == null) {
            return Optional.empty();
        }
        return switch (code) {
        case "compiler.err.cant.resolve.location.args",
                "compiler.err.cant.resolve.location" ->
            Optional.of("check the name, or add the missing member/import");
        case "compiler.err.cant.resolve.location.args.params" ->
            Optional.of("check the argument types at the call site");
        case "compiler.err.prob.found.req" -> Optional
                .of("types do not match; adjust the value or the declaration");
        case "compiler.err.does.not.override.abstract" ->
            Optional.of("implement the missing abstract method(s)");
        case "compiler.err.missing.ret.stmt" ->
            Optional.of("add a return statement on every path");
        case "compiler.err.expected", "compiler.err.illegal.start.of.expr" ->
            Optional.of("syntax error - check brackets and semicolons nearby");
        default -> Optional.empty();
        };
    }

    private List<String> classesWrittenSince(Reactor.Module module,
            long epochMillis) {
        Path classesDir = module.classesDir();
        if (!Files.isDirectory(classesDir)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(classesDir)) {
            return walk.filter(p -> p.toString().endsWith(".class"))
                    .filter(p -> {
                        try {
                            return Files.getLastModifiedTime(p)
                                    .toMillis() >= epochMillis;
                        } catch (IOException e) {
                            return false;
                        }
                    }).map(module::binaryNameOf).sorted().toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * How a changed file is named back to the caller: relative to the
     * application module, so a sibling reads
     * {@code ../shared/src/main/java/...}. One base, and it is the directory
     * the CLI is run from, so a path an agent is handed is a path it can open.
     * Absolute is the fallback for a module on another Windows drive, where
     * relativizing is not possible at all.
     */
    String relative(Path path) {
        try {
            return app().dir().relativize(path).toString()
                    .replace(File.separatorChar, '/');
        } catch (IllegalArgumentException e) {
            return path.toString().replace(File.separatorChar, '/');
        }
    }
}
