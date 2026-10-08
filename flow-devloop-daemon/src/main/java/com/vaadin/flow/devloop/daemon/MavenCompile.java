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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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
 * Maven writes to a directory of its own in each module,
 * {@code DevLoopBuildExtension#COMPILE_OUTPUT}, and only the class files whose
 * bytes changed are copied on into the classes directory the application runs
 * from. maven-compiler-plugin deletes a module's previous class files before it
 * rebuilds the module and writes none back when the compile fails; in the
 * classes directory that would take every class the application has not loaded
 * yet from under it, and a class that once failed to resolve a reference keeps
 * failing on it after the file is back. It also keeps a server that watches the
 * classes directory from seeing a whole module rewritten on every apply.
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
     * The bytes of a class as of a moment the daemon knows them.
     *
     * @param stamp
     *            the stamp Maven's output file had then, or {@code null} for
     *            the bytes the application was launched with
     * @param digest
     *            the bytes' fingerprint, or {@code null} when not taken yet or
     *            unreadable
     */
    private record Known(Compile.Stamp stamp, String digest) {
    }

    /**
     * Which classes a run wrote.
     *
     * @param written
     *            the written classes by binary name, sorted
     * @param classFiles
     *            the class files behind {@code written}, with the stamps of
     *            Maven's output files they come from
     */
    record Diff(List<String> written, Map<Path, Compile.Stamp> classFiles) {
    }

    private final Compile owner;

    private final Launch launch;

    /**
     * The class files the application was launched with, and their stamps then,
     * until the first compile takes their digests into {@link #live}.
     */
    private final Map<Path, Compile.Stamp> launched = new ConcurrentHashMap<>();

    /**
     * The class files the running application holds, keyed by their path in the
     * classes directory.
     * <p>
     * Digests are taken at the first compile, for the files whose stamp has not
     * moved since the launch - those bytes are still the ones it loaded. It
     * moves forward only when a redefine has been accepted
     * ({@link #markApplied}), never on a compile alone: a compile whose
     * redefine never happens - superseded, or run with {@code --no-restart} -
     * would otherwise hide its classes from the next apply, whose Maven run
     * writes the same bytes again.
     */
    private final Map<Path, Known> live = new ConcurrentHashMap<>();

    /** What the latest compiles reported, until it goes live. */
    private final Map<Path, Known> pending = new ConcurrentHashMap<>();

    /**
     * Each file of Maven's output, with the stamp it had when the classes
     * directory was last made to hold the same bytes, so a file a run did not
     * rewrite is not compared again. Kept apart from {@link #live}: what is on
     * disk and what the application holds differ whenever a compiled change
     * never reached it.
     */
    private final Map<Path, Compile.Stamp> installed = new ConcurrentHashMap<>();

    /** Whether the fallback to one execution id has been logged. */
    private volatile boolean warnedAboutExecution;

    /**
     * Whether Maven writes to {@code DevLoopBuildExtension#COMPILE_OUTPUT}
     * rather than to the classes directory, which takes the build extension.
     */
    private volatile boolean staged = true;

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
        Set<Path> builtBefore;
        try {
            List<String> command = command();
            builtBefore = builtClassFiles(project.modules());
            run = Launch.runMavenCommand(command, launch.reactor().root());
        } catch (IOException e) {
            return ioError(e, started);
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
        try {
            install(project.modules(), builtBefore);
        } catch (IOException e) {
            return ioError(e, started);
        }
        // Maven compiled the whole -am closure, so every module in the loop
        // now compiles against what its pom says.
        project.modules().forEach(
                module -> owner.recordCompiledAgainst(module, project));
        return new Compile.Result(true, List.of(), diff.written(),
                millisSince(started), diff.classFiles(),
                removedClasses(project.modules()));
    }

    private static Compile.Result ioError(IOException e, long started) {
        return new Compile.Result(false,
                List.of(new Compile.Message("-", 0, 0, "io-error",
                        String.valueOf(e.getMessage()), Optional.empty())),
                List.of(), millisSince(started));
    }

    @Override
    public void seed(Map<Path, Compile.Stamp> classes) {
        live.clear();
        pending.clear();
        // A restart may follow a build that rewrote the classes directory.
        installed.clear();
        launched.clear();
        launched.putAll(classes);
    }

    /**
     * Moves what the application holds forward to the class files the given
     * compile reported - only those, and only as that compile found them. A
     * newer compile that ran while this one's redefine was in flight may have
     * reported other bytes for the same file since; those stay pending, so the
     * next apply still offers them.
     */
    @Override
    public void markApplied(Compile.Result applied) {
        applied.classFiles().forEach((file, stamp) -> {
            Known reported = pending.get(file);
            if (reported != null && reported.stamp().equals(stamp)) {
                live.put(file, reported);
                pending.remove(file, reported);
            } else {
                // The newer compile's entry has replaced this one's digest, so
                // the bytes taken are known by their stamp alone.
                live.put(file, new Known(stamp, null));
            }
        });
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
            staged = false;
            if (!warnedAboutExecution) {
                warnedAboutExecution = true;
                launch.log().line("maven compile: no build extension to load, "
                        + "so every module compiles with the configuration of "
                        + "execution '" + execution + "', straight into its "
                        + "classes directory - a failed compile leaves the "
                        + "classes Maven removed missing until the next one");
            }
        } else {
            staged = true;
            command.addAll(extension);
            command.add(
                    "-D" + DevLoopBuildExtension.COMPILE_PROPERTY + "=true");
            execution = DevLoopBuildExtension.COMPILE_EXECUTION;
        }
        // Nothing here compiles tests; the property is passed because the
        // resolve passes it, so a profile activated by it is active in both
        // runs and the compile sees the configuration the resolve recorded.
        command.addAll(List.of("compiler:compile@" + execution,
                "-Dmaven.test.skip=true"));
        return command;
    }

    /**
     * Fingerprints the class files the application was launched with, which is
     * the comparison a run's output is made against. Each is read once in an
     * application's life, and only while its stamp is the one it was launched
     * with - those bytes are still the ones it loaded.
     */
    void takeMissingDigests() {
        launched.forEach((file, stamp) -> live.put(file,
                new Known(null,
                        Compile.stampOf(file).filter(stamp::equals)
                                .flatMap(same -> Compile.digestOf(file))
                                .orElse(null))));
        launched.clear();
    }

    /**
     * Where Maven writes a module's classes in a compile run.
     *
     * @param module
     *            the module
     * @return its own directory under {@code target/devloop}
     */
    static Path stagingDir(Reactor.Module module) {
        return module.dir().resolve(DevLoopBuildExtension.COMPILE_OUTPUT);
    }

    private Path outputOf(Reactor.Module module) {
        return staged ? stagingDir(module) : module.classesDir();
    }

    /**
     * Maven's own output for a source, once a run has written it. A class whose
     * bytes did not change is not copied into the classes directory, so the
     * copy there can stay older than an edit that has been compiled. Until a
     * run has written it - before the first one, or after one that failed - the
     * classes directory's copy is the answer.
     */
    @Override
    public Path artifactFor(Reactor.Module module, Path source) {
        Path artifact = module.artifactFor(source);
        if (!staged) {
            return artifact;
        }
        Path built = stagingDir(module)
                .resolve(module.classesDir().relativize(artifact).toString());
        return Files.isRegularFile(built) ? built : artifact;
    }

    /**
     * The classes a run changed, by comparing Maven's output with what the
     * running application holds.
     * <p>
     * Stamps first, so only a class file the run touched is read. A touched
     * file whose bytes are the ones already live is not reported, and its new
     * stamp is recorded so the next run does not read it again; anything else -
     * a new class, different bytes, a digest that could not be taken - is.
     *
     * @param modules
     *            the modules in the loop
     * @return what the run changed
     */
    Diff changedClasses(List<Reactor.Module> modules) {
        Set<String> written = new TreeSet<>();
        Map<Path, Compile.Stamp> classFiles = new HashMap<>();
        Set<Path> seen = new HashSet<>();
        for (Reactor.Module module : modules) {
            Path output = outputOf(module);
            for (Path built : classFiles(output)) {
                Optional<Compile.Stamp> stamp = Compile.stampOf(built);
                if (stamp.isEmpty()) {
                    continue;
                }
                Path file = module.classesDir()
                        .resolve(output.relativize(built).toString());
                seen.add(file);
                if (changed(file, built, stamp.get())) {
                    written.add(module.binaryNameOf(file));
                    classFiles.put(file, stamp.get());
                }
            }
        }
        pending.keySet().retainAll(seen);
        return new Diff(List.copyOf(written), Map.copyOf(classFiles));
    }

    /**
     * Whether Maven's output for a class holds bytes the application does not.
     *
     * @param file
     *            the class file in the classes directory
     * @param built
     *            Maven's output for it
     * @param stamp
     *            the stamp of Maven's output
     */
    private boolean changed(Path file, Path built, Compile.Stamp stamp) {
        Known loaded = live.get(file);
        if (loaded != null && stamp.equals(loaded.stamp())) {
            return false;
        }
        Known reported = pending.get(file);
        String digest = reported != null && stamp.equals(reported.stamp())
                ? reported.digest()
                : Compile.digestOf(built).orElse(null);
        if (loaded != null && loaded.digest() != null
                && loaded.digest().equals(digest)) {
            live.put(file, new Known(stamp, digest));
            pending.remove(file);
            return false;
        }
        pending.put(file, new Known(stamp, digest));
        return true;
    }

    /**
     * Makes the classes directories hold what Maven built: every file of its
     * output whose bytes differ there, classes and what an annotation processor
     * generates beside them, such as a service file.
     * <p>
     * Decided against the disk rather than against what the application holds:
     * an edit that compiled but never reached the application, then was
     * reverted, compiles back to the bytes the application has - nothing to
     * redefine - while the classes directory still holds the edit, which a
     * later class load or a restart would run. Files that already match are
     * left alone, so a server watching the classes directory sees only what
     * changed.
     * <p>
     * A class Maven no longer builds is removed from the classes directory too
     * - a nested class taken out of a source that is still there is not the
     * deletion leg's to remove, and left on disk it stays discoverable, a
     * removed route or bean included, even after a restart. Only a class file
     * Maven's output had before the run, or one compiled from a Java source
     * that is still there (see {@link #hasJavaSource}), is removed: a class
     * another compiler writes into the classes directory, Kotlin's for one, is
     * not Maven's compile's to take away.
     *
     * @param modules
     *            the modules in the loop
     * @param builtBefore
     *            Maven's class files before the run; see
     *            {@link #builtClassFiles}
     * @throws IOException
     *             if a file could not be copied or removed
     */
    void install(List<Reactor.Module> modules, Set<Path> builtBefore)
            throws IOException {
        if (!staged) {
            return;
        }
        installed.keySet().removeIf(built -> !Files.exists(built));
        for (Reactor.Module module : modules) {
            Path output = stagingDir(module);
            for (Path file : classFiles(module.classesDir())) {
                Path built = output.resolve(
                        module.classesDir().relativize(file).toString());
                if (!Files.exists(built) && (builtBefore.contains(built)
                        || hasJavaSource(module, file))) {
                    Files.deleteIfExists(file);
                }
            }
            for (Path built : files(output)) {
                Optional<Compile.Stamp> stamp = Compile.stampOf(built);
                if (stamp.isEmpty()
                        || stamp.get().equals(installed.get(built))) {
                    continue;
                }
                Path file = module.classesDir()
                        .resolve(output.relativize(built).toString());
                if (!Files.isRegularFile(file)
                        || Files.mismatch(built, file) != -1) {
                    copyOver(built, file);
                }
                installed.put(built, stamp.get());
            }
        }
    }

    /**
     * The class files in Maven's output for the modules, taken before a run so
     * that the run's removals can be told apart afterwards.
     *
     * @param modules
     *            the modules in the loop
     * @return the class files, empty when Maven writes into the classes
     *         directories itself
     */
    Set<Path> builtClassFiles(List<Reactor.Module> modules) {
        Set<Path> built = new HashSet<>();
        if (staged) {
            modules.forEach(
                    module -> built.addAll(classFiles(stagingDir(module))));
        }
        return built;
    }

    /**
     * Whether a class file comes from a Java source that is still there. The
     * source is the one its {@code SourceFile} attribute names, which is what
     * ties a secondary top-level class - {@code class Removed} declared in
     * {@code View.java} - to its file; only a class compiled without the
     * attribute ({@code -g:none}) falls back to its top-level class's name. A
     * source that is not Java - Kotlin's {@code .kt} - is not Maven's
     * compile's.
     */
    private static boolean hasJavaSource(Reactor.Module module, Path file) {
        Path relative = module.classesDir().relativize(file);
        String name = relative.getFileName().toString();
        int nested = name.indexOf('$');
        String topLevel = nested < 0
                ? name.substring(0, name.length() - CLASS_SUFFIX.length())
                : name.substring(0, nested);
        String sourceFile = ClassFile.read(file).map(ClassFile::sourceFile)
                .orElse(topLevel + ".java");
        if (!sourceFile.endsWith(".java")) {
            return false;
        }
        Path packageDir = relative.getParent();
        Path sources = packageDir == null ? module.sourceDir()
                : module.sourceDir().resolve(packageDir.toString());
        return Files.isRegularFile(sources.resolve(sourceFile));
    }

    /**
     * The classes the running application holds whose class files are gone, by
     * binary name. Reported on every compile until a restart re-seeds what it
     * holds, the way a deleted source is: a JVM cannot un-define a class, so a
     * removed route or bean goes on answering until then.
     *
     * @param modules
     *            the modules in the loop
     * @return the classes, sorted
     */
    List<String> removedClasses(List<Reactor.Module> modules) {
        Set<String> removed = new TreeSet<>();
        for (Path file : live.keySet()) {
            if (Files.exists(file)) {
                continue;
            }
            modules.stream()
                    .filter(module -> file.startsWith(module.classesDir()))
                    .findFirst().ifPresent(
                            module -> removed.add(module.binaryNameOf(file)));
        }
        return List.copyOf(removed);
    }

    private static void copyOver(Path from, Path to) throws IOException {
        Files.createDirectories(to.getParent());
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    private static List<Path> files(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            // Unreadable, the same as for the class files: nothing to copy.
            return List.of();
        }
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
