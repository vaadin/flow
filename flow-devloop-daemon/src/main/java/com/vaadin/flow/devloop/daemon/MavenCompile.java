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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.vaadin.flow.devloop.mavenext.DevLoopBuildExtension;

/**
 * The compile leg's Maven backend: every apply runs the project's own
 * {@code maven-compiler-plugin} instead of javac in-process.
 * <p>
 * What that buys is the build's own compile - annotation processors, the
 * plugin's configuration, and the callers of a changed signature or constant
 * that the plugin recompiles with it. What it costs is a Maven start per apply,
 * which is why it is an opt-in; see {@link Compile.Compiler}.
 * <p>
 * Maven decides what to recompile, and usually recompiles a whole module. So
 * which classes changed is read off the output directories afterwards rather
 * than off the change-set, and only a class whose <em>bytes</em> differ from
 * what the running application holds is reported: redefining every class of a
 * module would slow the swap, and Flow's hot-swap handling would treat every
 * route in it as changed.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class MavenCompile implements Compile.Backend {

    private static final String COMPILER_GROUP = "org.apache.maven.plugins";

    private static final String COMPILER_ARTIFACT = "maven-compiler-plugin";

    /** The execution Maven's default lifecycle binds the compile goal to. */
    private static final String DEFAULT_EXECUTION = "default-compile";

    private static final String ERROR_PREFIX = "[ERROR]";

    private static final String CLASS_SUFFIX = ".class";

    /**
     * A located compiler error after the {@code [ERROR]} prefix:
     * {@code /path/File.java:[12,5] message}. The path is matched lazily up to
     * {@code :[}, so a Windows drive letter's colon stays part of it.
     */
    private static final Pattern LOCATED = Pattern
            .compile("^(.+?):\\[(\\d+),(\\d+)] ?(.*)$");

    /**
     * How maven-compiler-plugin writes a Windows path: as a URI path, with a
     * slash ahead of the drive letter.
     */
    private static final Pattern WINDOWS_URI_PATH = Pattern
            .compile("^/[A-Za-z]:[/\\\\].*");

    /**
     * A class file as of a moment the daemon knows its bytes for.
     *
     * @param stamp
     *            the file's stamp then
     * @param digest
     *            the bytes' fingerprint, or {@code null} when not taken yet or
     *            unreadable
     */
    private record Known(Compile.Stamp stamp, String digest) {
    }

    /** Which classes a run wrote, and which ones it left gone. */
    record Diff(List<String> written, List<Path> vanished) {
    }

    private final Compile owner;

    private final Launch launch;

    /**
     * The class files the running application holds, keyed by path.
     * <p>
     * Seeded when it is launched, with stamps only: digests are taken at the
     * first compile, for the files whose stamp has not moved since - those
     * bytes are still the ones it loaded. It moves forward only when a redefine
     * has been accepted ({@link #markApplied}), never on a compile alone: a
     * compile whose redefine never happens - superseded, or run with
     * {@code --no-restart} - would otherwise hide its classes from the next
     * apply, whose Maven run writes the same bytes again.
     */
    private final Map<Path, Known> live = new ConcurrentHashMap<>();

    /** What the last successful compile reported, until it goes live. */
    private final Map<Path, Known> pending = new ConcurrentHashMap<>();

    /** Class files already reported as gone, so each is logged once. */
    private final Set<Path> reportedGone = ConcurrentHashMap.newKeySet();

    /** Whether the fallback to one execution id has been logged. */
    private volatile boolean warnedAboutExecution;

    /**
     * @param owner
     *            the baseline this compiles for, which names diagnostic files
     *            and keeps the classpath bookkeeping
     * @param launch
     *            what knows how to run Maven for this project
     */
    MavenCompile(Compile owner, Launch launch) {
        this.owner = owner;
        this.launch = launch;
    }

    /**
     * Compiles through Maven.
     * <p>
     * The change-set is not handed on: Maven works out what is stale on its
     * own, which is the point of compiling with it. It is still what decided
     * that an apply has to compile at all.
     */
    @Override
    public Compile.Result compile(List<Path> sources, Launch.Project project) {
        long started = System.nanoTime();
        takeMissingDigests();
        Launch.Attempt run;
        try {
            run = Launch.runMavenCommand(command(), launch.reactor().root());
        } catch (IOException e) {
            return new Compile.Result(false,
                    List.of(new Compile.Message("-", 0, 0, "io-error",
                            String.valueOf(e.getMessage()), Optional.empty())),
                    List.of(), millisSince(started));
        }
        if (!run.ok()) {
            List<Compile.Message> errors = parseErrors(run.output(),
                    owner::diagnosticFileOf);
            if (errors.isEmpty()) {
                errors = List.of(new Compile.Message("-", 0, 0, "maven",
                        Launch.failureReason(run.output()), Optional.empty()));
            }
            return new Compile.Result(false, errors, List.of(),
                    millisSince(started));
        }
        Diff diff = changedClasses(project.modules());
        if (!diff.vanished().isEmpty()) {
            // Deleted sources are acted on by the deletion leg; this only says
            // that the build took class files away too.
            launch.log().line("maven compile: " + diff.vanished().size()
                    + " class file(s) the application has loaded are gone");
        }
        // Maven compiled the whole -am closure, so every module in the loop
        // now compiles against what its pom says.
        project.modules().forEach(
                module -> owner.recordCompiledAgainst(module, project));
        return new Compile.Result(true, List.of(), diff.written(),
                millisSince(started));
    }

    @Override
    public void seed(Map<Path, Compile.Stamp> classes) {
        live.clear();
        pending.clear();
        reportedGone.clear();
        classes.forEach(
                (file, stamp) -> live.put(file, new Known(stamp, null)));
    }

    @Override
    public void markApplied() {
        live.putAll(pending);
        pending.clear();
    }

    /**
     * The command line.
     * <p>
     * The same reactor shape the resolve uses, {@code -pl :app -am}, and for
     * the same reason: with only the application in the reactor, a sibling
     * module would be compiled against its installed jar. Offline, because the
     * resolve has already fetched everything this needs.
     * <p>
     * The compile goal on its own rather than the {@code compile} phase, which
     * would also run the resource copy - the daemon's own leg - and whatever
     * the project binds before it, Vaadin's {@code prepare-frontend} among
     * them.
     * <p>
     * A goal on the command line takes its configuration from the execution its
     * id names, in every module of the reactor, and modules need not agree on
     * that id. So the build extension is loaded to give each module's own
     * compile execution one shared id; see
     * {@code DevLoopBuildExtension#COMPILE_EXECUTION}. A daemon with no jar to
     * load as the extension falls back to the application module's id, which is
     * right whenever the modules agree.
     */
    private List<String> command() throws IOException {
        Reactor reactor = launch.reactor();
        List<String> command = new ArrayList<>(List.of(
                launch.mavenCommand().toString(), "-o", "-B", "-ntp", "-q"));
        if (reactor.isMultiModule()) {
            command.addAll(
                    List.of("-f", reactor.root().resolve("pom.xml").toString(),
                            "-pl", ":" + reactor.app().artifactId(), "-am"));
        }
        command.addAll(Launch.extraMavenArguments());
        List<String> extension = launch.buildExtension();
        String execution;
        if (extension.isEmpty()) {
            execution = reactor
                    .findExecution(COMPILER_GROUP, COMPILER_ARTIFACT, "compile")
                    .orElse(DEFAULT_EXECUTION);
            if (!warnedAboutExecution) {
                warnedAboutExecution = true;
                launch.log().line("maven compile: no build extension to load, "
                        + "so every module compiles with the configuration of "
                        + "execution '" + execution + "'");
            }
        } else {
            command.addAll(extension);
            command.add(
                    "-D" + DevLoopBuildExtension.COMPILE_PROPERTY + "=true");
            execution = DevLoopBuildExtension.COMPILE_EXECUTION;
        }
        command.addAll(List.of("compiler:compile@" + execution,
                "-Dmaven.test.skip=true"));
        return command;
    }

    /**
     * Fingerprints the class files the application loaded whose bytes are still
     * the ones it loaded, which is the comparison a run's output is made
     * against. Each is read once in an application's life.
     */
    void takeMissingDigests() {
        live.forEach((file, known) -> {
            if (known.digest() == null && Compile.stampOf(file)
                    .map(known.stamp()::equals).orElse(false)) {
                Compile.digestOf(file).ifPresent(digest -> live.put(file,
                        new Known(known.stamp(), digest)));
            }
        });
    }

    /**
     * The classes a run changed, by comparing the output directories with what
     * the running application holds.
     * <p>
     * Stamps first, so only a class file the run touched is read. A touched
     * file whose bytes are the ones already live is not reported, and its new
     * stamp is recorded so the next run does not read it again; anything else -
     * a new class, different bytes, a digest that could not be taken - is.
     *
     * @param modules
     *            the modules in the loop
     * @return the written classes by binary name, sorted, and the loaded class
     *         files that are gone
     */
    Diff changedClasses(List<Reactor.Module> modules) {
        Set<String> written = new java.util.TreeSet<>();
        Set<Path> seen = new HashSet<>();
        for (Reactor.Module module : modules) {
            for (Path file : classFiles(module.classesDir())) {
                Optional<Compile.Stamp> stamp = Compile.stampOf(file);
                if (stamp.isEmpty()) {
                    continue;
                }
                seen.add(file);
                if (changed(file, stamp.get())) {
                    written.add(module.binaryNameOf(file));
                }
            }
        }
        pending.keySet().retainAll(seen);
        List<Path> vanished = live.keySet().stream()
                .filter(file -> !seen.contains(file)).filter(reportedGone::add)
                .sorted().toList();
        return new Diff(List.copyOf(written), vanished);
    }

    private boolean changed(Path file, Compile.Stamp stamp) {
        Known loaded = live.get(file);
        if (loaded != null && stamp.equals(loaded.stamp())) {
            return false;
        }
        Known reported = pending.get(file);
        String digest = reported != null && stamp.equals(reported.stamp())
                ? reported.digest()
                : Compile.digestOf(file).orElse(null);
        if (loaded != null && loaded.digest() != null
                && loaded.digest().equals(digest)) {
            live.put(file, new Known(stamp, digest));
            pending.remove(file);
            return false;
        }
        pending.put(file, new Known(stamp, digest));
        return true;
    }

    private static List<Path> classFiles(Path classesDir) {
        if (!Files.isDirectory(classesDir)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(classesDir)) {
            return walk.filter(path -> path.toString().endsWith(CLASS_SUFFIX))
                    .filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            // An unreadable tree reports nothing written, the same contract as
            // the change-set walk.
            return List.of();
        }
    }

    /**
     * The compiler errors in a Maven run's output.
     * <p>
     * maven-compiler-plugin prints each error twice: once as it is reported,
     * with the continuation lines ({@code symbol:}, {@code location:}) bare,
     * and again in the build failure summary, every line prefixed. Both shapes
     * are read and the repetition is dropped, so output that has only one of
     * them - a different log level, a different plugin version - still yields
     * its errors.
     *
     * @param output
     *            what Maven printed
     * @param naming
     *            how a diagnostic names its file; see
     *            {@link Compile#diagnosticFileOf}
     * @return the errors, in the order first reported
     */
    static List<Compile.Message> parseErrors(String output,
            Function<Path, String> naming) {
        Set<Compile.Message> errors = new LinkedHashSet<>();
        Matcher current = null;
        StringBuilder text = new StringBuilder();
        for (String line : output.lines().toList()) {
            boolean error = line.startsWith(ERROR_PREFIX);
            String rest = error ? line.substring(ERROR_PREFIX.length()) : line;
            Matcher located = LOCATED.matcher(rest.strip());
            if (error && located.matches()) {
                add(errors, current, text, naming);
                current = located;
                text.setLength(0);
                text.append(located.group(4));
            } else if (current != null && isContinuation(line, error, rest)) {
                text.append(' ').append(rest.strip());
            } else {
                add(errors, current, text, naming);
                current = null;
            }
        }
        add(errors, current, text, naming);
        return List.copyOf(errors);
    }

    /**
     * A line that carries on the message above it: unprefixed, or prefixed and
     * indented, as the failure summary repeats it.
     */
    private static boolean isContinuation(String line, boolean error,
            String rest) {
        if (line.isBlank()) {
            return false;
        }
        return error ? rest.startsWith("  ") && !rest.isBlank()
                : !line.startsWith("[");
    }

    private static void add(Set<Compile.Message> errors, Matcher located,
            StringBuilder text, Function<Path, String> naming) {
        if (located == null) {
            return;
        }
        errors.add(new Compile.Message(fileOf(located.group(1), naming),
                Long.parseLong(located.group(2)),
                Long.parseLong(located.group(3)), "maven",
                Compile.tidy(text.toString()), Optional.empty()));
    }

    private static String fileOf(String raw, Function<Path, String> naming) {
        String path = WINDOWS_URI_PATH.matcher(raw).matches() ? raw.substring(1)
                : raw;
        try {
            return naming.apply(Path.of(path));
        } catch (InvalidPathException e) {
            return path;
        }
    }

    private static long millisSince(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
