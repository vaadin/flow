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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Composes the app JVM command line, resolves the classpath through Maven, and
 * provisions HotswapAgent so the user never has to.
 * <p>
 * Phase 0.5 established that getting the flag set right matters as much as
 * having the jar: without the JPMS opens, HotswapAgent's core helper fails on
 * every redefine and its plugins degrade silently. Nine flags have to be right
 * and one wrong one fails quietly, which is exactly why the daemon composes
 * them rather than a human.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class Launch {

    static final boolean WINDOWS = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT).startsWith("windows");

    /**
     * Pinned, never "latest": a changing agent would make applies
     * irreproducible.
     * <p>
     * The checksum is of the {@code hotswap-agent-<version>.jar} release asset
     * at {@link #HA_URL}, not of the same version on Maven Central - the two
     * are built separately and there is no promise they are the same bytes.
     * Bumping the version means downloading that asset and recomputing this.
     */
    static final String HA_VERSION = "2.0.3";
    static final String HA_SHA256 = "4ef49724b7d8523536d2e2a7310f827f4db9f4fed3489224e05d7bf87f0594f9";
    static final String HA_URL = "https://github.com/HotswapProjects/HotswapAgent/releases/download/RELEASE-"
            + HA_VERSION + "/hotswap-agent-" + HA_VERSION + ".jar";

    private static final List<String> ADD_OPENS = List.of("java.base/java.lang",
            "java.base/java.lang.reflect", "java.base/java.io",
            "java.base/java.util", "java.desktop/java.beans");

    /**
     * Where each module is told to write its classpath, <em>relative</em> on
     * purpose: Maven aligns a relative {@code File} plugin parameter to the
     * executing project's own basedir, so one reactor invocation leaves every
     * module its own file instead of all of them overwriting one. An absolute
     * path made that a race the application module only won by sorting last.
     */
    private static final String CLASSPATH_FILE = "target/devloop/cp.txt";

    /**
     * Re-read whenever a pom changes: a pom edit can add or remove a module,
     * and the aggregation graph is what the cache stamp is computed from - so a
     * module that did not exist when the daemon started would otherwise never
     * be watched.
     */
    private volatile Reactor reactor;

    private final Path root;
    private final Log log;

    /**
     * The last successful resolution, so a Maven failure degrades instead of
     * breaking.
     */
    private volatile Project project;

    /**
     * Why the last resolution failed, when it failed and there was no earlier
     * answer to fall back on. Kept rather than thrown so {@code status} can
     * report it, and so an {@code apply} says "classpath: ..." instead of
     * handing javac a classpath of one directory and printing a hundred
     * consequences.
     */
    private volatile String resolutionError;

    /**
     * Whether the classpath in hand is the synthetic app-only fallback, as
     * opposed to a real resolution that has merely gone stale. Launching
     * against the former cannot work; launching against the latter is a
     * judgement call the developer should be told about, not refused.
     */
    private volatile boolean classpathUnusable;

    /**
     * The classpath the running app was actually launched with.
     * <p>
     * A JVM's class path is fixed for its lifetime, so this is the only way to
     * answer "is the running app still the app the poms describe?". Editing a
     * pom changes what the build resolves and nothing about the process already
     * running - and since a pom edit touches no {@code .java} file, the source
     * scan finds nothing and the apply used to report "no changes" over an app
     * running a classpath that no longer exists on paper.
     */
    private volatile String launchedClasspath;

    /** How many times Maven has actually been run, so a caller can tell. */
    private final AtomicLong resolutions = new AtomicLong();

    /**
     * The discovered application class, so a restart does not rediscover it.
     */
    private volatile String mainClass;

    /** Which poms moved the last time the stamp was rewritten, app-relative. */
    private volatile List<String> changedPoms = List.of();

    /** The JVM chosen for the application; see {@link #appJvm()}. */
    private volatile Jvm.Jdk appJvm;

    Launch(Reactor reactor, Log log) {
        this.reactor = reactor;
        this.root = reactor.app().dir();
        this.log = log;
    }

    /**
     * The resolved shape of the build: which modules are in the edit loop, what
     * the app runs with, and what each module compiles against.
     * <p>
     * Per-module compile classpaths rather than one shared one, because sharing
     * the application's would let a library module compile against a type only
     * the application declares - javac would pass and Maven could never build
     * it - and would hide a module cycle behind a green apply.
     */
    record Project(List<Reactor.Module> modules, String appClasspath,
            Map<String, String> compileClasspath, OptionalInt release) {

        String compileClasspath(Reactor.Module module) {
            return compileClasspath.getOrDefault(module.artifactId(),
                    appClasspath);
        }

        Reactor.Module app() {
            return modules.get(0);
        }
    }

    /**
     * Where the daemon keeps its per-app runtime artifacts: the classpath cache
     * and the app log. Under the module's own {@code target/} so a clean wipes
     * them and nothing lands in a source tree.
     */
    static Path workDir(Path appModule) {
        return appModule.resolve("target").resolve("devloop");
    }

    /**
     * The agent jar to load into the app JVM, which is this daemon's own jar.
     * <p>
     * The jar carries {@code Premain-Class} alongside {@code Main-Class}, so
     * there is one artifact to resolve rather than two to keep in step - and
     * nothing for the CLI to compile on demand. Only
     * {@link com.vaadin.flow.devloop.agent.DevLoopAgent} loads in the
     * application JVM; the daemon's own classes are never touched there.
     * <p>
     * {@code vaadin.dev.agentJar} overrides it, which is how a daemon running
     * from an exploded build directory - where there is no jar to point at -
     * still gets an agent.
     */
    private Optional<Path> agentJar() {
        String configured = System.getProperty("vaadin.dev.agentJar");
        if (configured != null && !configured.isBlank()) {
            return Optional.of(Path.of(configured));
        }
        try {
            java.security.CodeSource source = Launch.class.getProtectionDomain()
                    .getCodeSource();
            java.net.URL location = source == null ? null
                    : source.getLocation();
            if (location != null) {
                Path own = Path.of(location.toURI());
                if (own.getFileName() != null
                        && own.getFileName().toString().endsWith(".jar")) {
                    return Optional.of(own);
                }
                log.line("the daemon is running from " + own
                        + " rather than from a jar, so it cannot also be the "
                        + "javaagent; pass -Dvaadin.dev.agentJar=<jar> to supply one");
            }
        } catch (java.net.URISyntaxException | RuntimeException e) {
            log.line("could not locate the daemon's own jar: " + e);
        }
        return Optional.empty();
    }

    /**
     * Where machine-level dev-loop assets are cached: the HotswapAgent jar, and
     * nothing else so far.
     * <p>
     * Under the {@code ~/.vaadin} directory Vaadin already owns, so one
     * download serves every application on the machine and nothing is written
     * into a project. It also survives a {@code mvn clean}, which a per-project
     * cache did not.
     */
    static Path cacheDir() {
        return Path.of(System.getProperty("user.home", "."), ".vaadin",
                "devloop");
    }

    /**
     * Ensures the HotswapAgent jar is present and matches the pinned checksum.
     * Honours an already-present jar and an explicit override so air-gapped
     * setups still work.
     */
    Path ensureHotswapAgent() throws IOException {
        String override = System.getProperty("vaadin.dev.hotswapAgentJar");
        if (override != null && !override.isBlank()) {
            Path path = Path.of(override);
            if (!Files.isRegularFile(path)) {
                throw new IOException(
                        "vaadin.dev.hotswapAgentJar does not exist: " + path);
            }
            return path;
        }

        Path jar = cacheDir().resolve("hotswap-agent-" + HA_VERSION + ".jar");
        if (Files.isRegularFile(jar)) {
            String actual = sha256(jar);
            if (!HA_SHA256.equalsIgnoreCase(actual)) {
                throw new IOException("cached HotswapAgent checksum mismatch: "
                        + jar + " (expected " + HA_SHA256 + ", got " + actual
                        + "). Delete it to re-download.");
            }
            return jar;
        }

        Files.createDirectories(jar.getParent());
        log.line("provisioning HotswapAgent " + HA_VERSION + " from " + HA_URL);
        Path temp = jar.resolveSibling(jar.getFileName() + ".part");
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(HA_URL))
                    .GET().build();
            HttpResponse<InputStream> response = client.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("download failed with HTTP "
                        + response.statusCode() + " from " + HA_URL);
            }
            try (InputStream in = response.body()) {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("download interrupted", e);
        }

        String actual = sha256(temp);
        if (!HA_SHA256.equalsIgnoreCase(actual)) {
            Files.deleteIfExists(temp);
            throw new IOException(
                    "downloaded HotswapAgent checksum mismatch (expected "
                            + HA_SHA256 + ", got " + actual + ")");
        }
        Files.move(temp, jar, StandardCopyOption.REPLACE_EXISTING);
        log.line("provisioned " + jar + " (verified sha256)");
        return jar;
    }

    /**
     * The JVM the application runs on, chosen once against what the project
     * needs; see {@link Jvm}.
     * <p>
     * Decided once and kept: the set of installed JDKs does not change while a
     * daemon is up, and re-deciding would repeat the decision line on every
     * restart.
     */
    Jvm.Jdk appJvm() {
        Jvm.Jdk current = appJvm;
        if (current == null) {
            current = Jvm.select(Jvm.required(reactor), log);
            appJvm = current;
        }
        return current;
    }

    /**
     * What javac compiles to, so a class the daemon writes is one the
     * application's JVM can load.
     * <p>
     * The project's own release when a pom declares one - compiling a 17-target
     * project at 21 would let code through the dev loop that Maven then rejects
     * - and otherwise the level of the JVM the app will run on, which is the
     * constraint that actually has to hold.
     */
    private OptionalInt compileRelease() {
        int release = Jvm.required(reactor).map(Jvm.Requirement::feature)
                .orElseGet(() -> appJvm().feature());
        if (release <= 0) {
            return OptionalInt.empty();
        }
        int daemon = Runtime.version().feature();
        if (release > daemon) {
            // javac cannot target a release newer than itself, and the JVM
            // the daemon happens to run on is not the one chosen for the app.
            log.line("WARNING: the project targets Java " + release
                    + " but the daemon runs on Java " + daemon
                    + "; compiling without --release");
            return OptionalInt.empty();
        }
        return OptionalInt.of(release);
    }

    /**
     * The resolved build, from the cache when every pom in the reactor is older
     * than the stamp, and from Maven when one is not.
     * <p>
     * Never fatal. A Maven failure keeps the last good answer, or falls back to
     * the application module alone, and says so - a project the daemon cannot
     * resolve is still a project it can report on, and {@code status} must
     * never pay for a build.
     */
    Project project() {
        return project(text -> {
        });
    }

    /**
     * As {@link #project()}, with resolution progress copied to a caller's log.
     * <p>
     * Running Maven costs seconds, and an {@code apply} that spends them in
     * silence looks wedged - and then reports "no changes", which reads as
     * though it never noticed the pom at all.
     */
    Project project(Log progress) {
        Log tee = text -> {
            log.line(text);
            progress.line(text);
        };
        Project current = project;
        if (current != null && stampIsCurrent()) {
            return current;
        }
        try {
            if (!stampIsCurrent()) {
                // Before Maven, not after: a pom edit may have added or removed
                // a
                // module, and re-reading here is what puts a newly added pom
                // into
                // the stamp - otherwise it would never be watched at all.
                reactor = Reactor.discover(root, tee);
                runMaven(tee);
                writeStamp();
                resolutions.incrementAndGet();
            }
            Project resolved = read();
            project = resolved;
            resolutionError = null;
            classpathUnusable = false;
            return resolved;
        } catch (IOException e) {
            tee.line("classpath resolution failed: " + e.getMessage());
            // Recorded either way. Keeping the last good classpath is what lets
            // status keep answering, but it must not let an apply claim
            // success: a
            // project that cannot resolve is not a project with no changes.
            resolutionError = e.getMessage();
            if (current != null) {
                tee.line("keeping the last classpath that resolved");
                return current;
            }
            classpathUnusable = true;
            Project fallback = new Project(List.of(reactor.app()),
                    reactor.app().classesDir().toString(), Map.of(),
                    compileRelease());
            project = fallback;
            return fallback;
        }
    }

    /**
     * Set when {@link #project()} last had to fall back; empty when it is
     * sound.
     */
    Optional<String> resolutionError() {
        return Optional.ofNullable(resolutionError);
    }

    /**
     * How the build's classpath now differs from the one the running app was
     * launched with, in words; empty when they still agree or when nothing has
     * been launched.
     * <p>
     * This is the observable consequence of a pom edit. Names what moved rather
     * than only that something did, because "restarting" without a reason is
     * indistinguishable from the daemon deciding to restart on a whim.
     */
    Optional<String> classpathDrift() {
        String launched = launchedClasspath;
        if (launched == null) {
            return Optional.empty();
        }
        String current = project().appClasspath();
        Set<String> before = new LinkedHashSet<>(split(launched));
        Set<String> after = new LinkedHashSet<>(split(current));
        if (before.equals(after)) {
            // Membership, not order. Maven reorders a classpath for edits that
            // change nothing about what is on it, and restarting an app to hand
            // it the same jars in a different sequence is noise - the developer
            // cannot tell it from the daemon restarting on a whim.
            return Optional.empty();
        }
        List<String> added = after.stream().filter(e -> !before.contains(e))
                .map(Launch::entryName).toList();
        List<String> removed = before.stream().filter(e -> !after.contains(e))
                .map(Launch::entryName).toList();
        List<String> parts = new ArrayList<>();
        if (!added.isEmpty()) {
            parts.add("added " + summarise(added));
        }
        if (!removed.isEmpty()) {
            parts.add("removed " + summarise(removed));
        }
        return Optional.of(String.join(", ", parts));
    }

    /**
     * A classpath reduced to what is on it, so two resolutions that differ only
     * in order compare equal. Used wherever the question is "is this still the
     * same classpath?" rather than "in what order will it be searched?".
     */
    static String membership(String classpath) {
        return String.join(File.pathSeparator,
                split(classpath).stream().distinct().sorted().toList());
    }

    private static List<String> split(String classpath) {
        return List.of(classpath
                .split(java.util.regex.Pattern.quote(File.pathSeparator)));
    }

    /** A jar or a module output directory, as a developer would name it. */
    private static String entryName(String entry) {
        Path path = Path.of(entry);
        Path name = path.getFileName();
        if (name == null) {
            return entry;
        }
        if (!"classes".equals(name.toString())) {
            return name.toString();
        }
        // <module>/target/classes reads as nothing on its own.
        Path module = path.getParent() == null ? null
                : path.getParent().getParent();
        return module == null || module.getFileName() == null ? entry
                : module.getFileName() + "/target/classes";
    }

    /** Enough names to recognise the change by, then a count for the rest. */
    private static String summarise(List<String> names) {
        if (names.size() <= 3) {
            return String.join(", ", names);
        }
        return String.join(", ", names.subList(0, 3)) + " and "
                + (names.size() - 3) + " more";
    }

    /** The aggregation graph as last read; refreshed whenever a pom changes. */
    Reactor reactor() {
        return reactor;
    }

    /**
     * What has been resolved so far, without resolving: {@code status} must not
     * build.
     */
    Optional<Project> resolved() {
        return Optional.ofNullable(project);
    }

    /**
     * Builds the project model from what Maven left on disk: the app module's
     * classpath file, plus one file per module in the {@code -am} closure.
     */
    private Project read() throws IOException {
        Path cache = workDir(root).resolve("cp.txt");
        if (!Files.isRegularFile(cache)) {
            throw new IOException("no classpath file at " + cache);
        }
        List<String> entries = entriesOf(cache);
        List<Reactor.Module> modules = selectModules(entries);
        Map<String, String> compile = new LinkedHashMap<>();
        for (Reactor.Module module : modules) {
            Path own = workDir(module.dir()).resolve("cp.txt");
            List<String> ownEntries = Files.isRegularFile(own) ? entriesOf(own)
                    : entries;
            compile.put(module.artifactId(),
                    assemble(List.of(module), modules, ownEntries));
        }
        return new Project(modules, assemble(modules, modules, entries),
                compile, compileRelease());
    }

    private static List<String> entriesOf(Path file) throws IOException {
        String raw = Files.readString(file).trim();
        return raw.isEmpty() ? List.of()
                : List.of(raw.split(
                        java.util.regex.Pattern.quote(File.pathSeparator)));
    }

    /**
     * Which modules are in the edit loop.
     * <p>
     * Read off the resolved classpath rather than reconstructed from poms: the
     * only <em>directory</em> entries Maven emits are reactor modules' output
     * directories, so this is exactly the set of modules the application
     * depends on - transitively, after profiles and dependency management, and
     * correct even for a module that overrides {@code <outputDirectory>}. It
     * also means the loop cannot contain a module the application does not
     * actually use.
     */
    private List<Reactor.Module> selectModules(List<String> entries) {
        List<Reactor.Module> modules = new ArrayList<>();
        modules.add(reactor.app());
        Optional<List<Path>> forced = forcedModuleDirs();
        if (forced.isPresent()) {
            for (Path dir : forced.get()) {
                addModule(modules, dir, null);
            }
            return List.copyOf(modules);
        }
        for (String entry : entries) {
            Path path = Path.of(entry);
            if (!Files.isDirectory(path)) {
                continue;
            }
            Path owner = moduleDirOf(path);
            if (owner != null) {
                addModule(modules, owner, path);
            }
        }
        return List.copyOf(modules);
    }

    /**
     * {@code vaadin.dev.modules}: unset means auto; {@code .} or empty means
     * the application alone, which is the daemon's pre-reactor behaviour and
     * the escape hatch when discovery gets it wrong; anything else is a list of
     * directories, absolute or relative to the application.
     */
    private Optional<List<Path>> forcedModuleDirs() {
        String configured = System.getProperty("vaadin.dev.modules");
        if (configured == null) {
            return Optional.empty();
        }
        List<Path> dirs = new ArrayList<>();
        for (String value : configured.split(",")) {
            String trimmed = value.trim();
            if (trimmed.isEmpty() || ".".equals(trimmed)) {
                continue;
            }
            Path dir = Reactor.real(root.resolve(trimmed));
            if (Files.isRegularFile(dir.resolve("pom.xml"))) {
                dirs.add(dir);
            } else {
                log.line("vaadin.dev.modules: skipping " + trimmed
                        + " - no pom.xml there");
            }
        }
        return Optional.of(dirs);
    }

    private void addModule(List<Reactor.Module> into, Path dir,
            Path classesDir) {
        Path owner = Reactor.real(dir);
        if (into.stream().anyMatch(module -> module.dir().equals(owner))) {
            return;
        }
        String artifactId = reactor.candidates().stream()
                .filter(candidate -> candidate.dir().equals(owner)).findFirst()
                .map(Reactor.Module::artifactId)
                .orElse(owner.getFileName() == null ? owner.toString()
                        : owner.getFileName().toString());
        Reactor.Module module = classesDir == null
                ? Reactor.Module.of(owner, artifactId)
                : Reactor.Module.of(owner, artifactId, classesDir);
        if (!module.hasSources()) {
            return;
        }
        if (!Files.isDirectory(module.classesDir())) {
            // Then the JVM will load those classes from the installed jar until
            // the first apply writes here, which mixes two builds. It still
            // works - a redefine acts on the loaded Class, whatever it came
            // from
            // - but it is not something to discover from behaviour.
            log.line("module " + module.name() + " has no "
                    + module.classesDir().getFileName()
                    + " yet; build it once for a clean baseline");
        }
        into.add(module);
    }

    /**
     * The nearest ancestor that is a Maven module, never above the reactor
     * root.
     */
    private Path moduleDirOf(Path classesDir) {
        Path root = reactor.root();
        for (Path dir = classesDir.getParent(); dir != null; dir = dir
                .getParent()) {
            if (Files.isRegularFile(dir.resolve("pom.xml"))) {
                return dir;
            }
            if (dir.equals(root)) {
                return null;
            }
        }
        return null;
    }

    /**
     * The classpath the JVM (or javac) is given: the named modules' output
     * first, then Maven's answer with every in-loop module's own installed jar
     * removed.
     * <p>
     * Both halves are needed. Maven substitutes a module's
     * {@code target/classes} only for modules that took part in the reactor
     * build, so a jar can still appear - and then it is not enough to merely
     * put the directory first: {@code getResources()} returns <em>both</em>
     * copies, and Spring's component scanning, {@code META-INF/services} and
     * Flow's class finder all iterate rather than take the first. So the stale
     * copy is dropped, not shadowed.
     */
    private String assemble(List<Reactor.Module> first,
            List<Reactor.Module> inLoop, List<String> entries) {
        Set<String> out = new LinkedHashSet<>();
        first.forEach(module -> out.add(module.classesDir().toString()));
        for (String entry : entries) {
            if (!isSupersededJar(entry, inLoop)) {
                out.add(entry);
            }
        }
        return String.join(File.pathSeparator, out);
    }

    /**
     * Whether a classpath entry is the installed jar of a module that is in the
     * loop. Matched on the local repository's own layout
     * ({@code .../<artifactId>/<version>/<file>}) rather than on the file name:
     * {@code <finalName>}, classifiers and {@code ${revision}} versions all
     * make {@code artifactId-version.jar} the wrong thing to compare.
     */
    private boolean isSupersededJar(String entry,
            List<Reactor.Module> modules) {
        Path path = Path.of(entry);
        if (Files.isDirectory(path)) {
            // A directory entry is a module's output, not a copy of one. It
            // also
            // sits under <module>/target/classes, whose grandparent is the
            // module
            // directory - which the repository-layout test below would read as
            // an
            // artifactId and drop, taking the sibling off its own classpath.
            return false;
        }
        Path versionDir = path.getParent();
        Path artifactDir = versionDir == null ? null : versionDir.getParent();
        if (artifactDir == null || artifactDir.getFileName() == null) {
            return false;
        }
        String artifactId = artifactDir.getFileName().toString();
        return modules.stream()
                .anyMatch(module -> module.artifactId().equals(artifactId));
    }

    /**
     * Resolves the classpath through Maven.
     * <p>
     * Two parts of this command line are load-bearing. {@code -pl :app -am} is
     * what gives the reactor a chance to answer at all: run from the
     * application module alone there is no reactor, and a sibling resolves to
     * whatever jar happens to be installed - measured, and the reason an edit
     * in a sibling module used to be invisible. And {@code compile} is what
     * makes the answer be {@code target/classes}: Maven's reactor reader
     * substitutes a module's output directory only when that module's
     * {@code compile} phase actually ran in the same session, so the goal on
     * its own hands back a stale jar. It also means a fresh clone resolves
     * without anything having been installed first.
     * <p>
     * {@code build-frontend} is bound to {@code prepare-package}, so this stops
     * well short of a frontend build.
     */
    long resolutions() {
        return resolutions.get();
    }

    /** The poms that changed the last time this daemon re-resolved. */
    List<String> changedPoms() {
        return changedPoms;
    }

    private void runMaven(Log progress) throws IOException {
        Path maven = mavenCommand();
        List<String> base = new ArrayList<>(
                List.of(maven.toString(), "-B", "-ntp"));
        if (reactor.isMultiModule()) {
            base.addAll(
                    List.of("-f", reactor.root().resolve("pom.xml").toString(),
                            "-pl", ":" + reactor.app().artifactId(), "-am"));
        }
        base.addAll(extraMavenArguments());
        base.addAll(List.of("compile", "dependency:build-classpath",
                "-Dmdep.outputFile=" + CLASSPATH_FILE,
                // Off by default, and then an unchanged classpath is not
                // rewritten at all - which made every apply after a pom edit
                // re-run Maven, because the file it compares against never got
                // any newer.
                "-Dmdep.regenerateFile=true", "-Dmaven.test.skip=true"));

        progress.line("resolving classpath and building " + reactor.app().name()
                + (reactor.isMultiModule() ? " and the modules it depends on"
                        : ""));
        warnAboutIgnoredMavenConfig();

        // Offline first: it is the fast path and the air-gapped one. But a cold
        // local repository fails while building the model - a parent pom it has
        // never seen - long before it gets to dependencies, so a single retry
        // online is the difference between a working first run and a dead end.
        Attempt offline = attempt(base, true);
        if (offline.ok()) {
            return;
        }
        log.line("offline resolution failed; retrying online");
        Attempt online = attempt(base, false);
        if (online.ok()) {
            return;
        }
        throw new IOException(maven + ": " + failureReason(online.output()));
    }

    /**
     * Extra arguments for the resolve, from {@code vaadin.dev.mavenArgs}.
     * <p>
     * Driving the build from the reactor root rather than from the application
     * changes what Maven sees: a profile activated by a file next to
     * {@code maven.multiModuleProjectDirectory}, or a plugin bound with a path
     * relative to it, can behave differently or fail outright. Rather than
     * guess at those cases, the project gets to say what the resolve needs -
     * most often a profile to switch off, as in {@code -P!install-git-hooks}.
     * <p>
     * Split on whitespace, so an argument containing a space cannot be passed.
     * That is the trade for a single-property knob, and every argument this is
     * for is a flag.
     */
    private static List<String> extraMavenArguments() {
        String configured = System.getProperty("vaadin.dev.mavenArgs");
        if (configured == null || configured.isBlank()) {
            return List.of();
        }
        return List.of(configured.trim().split("\\s+"));
    }

    private record Attempt(boolean ok, String output) {
    }

    private Attempt attempt(List<String> base, boolean offline)
            throws IOException {
        List<String> command = new ArrayList<>(base);
        // After the wrapper, before the goals: Maven accepts options anywhere,
        // and inserting here keeps the goals last where a reader expects them.
        command.addAll(1, offline ? List.of("-o") : List.of("-nsu"));
        Path directory = reactor.root();
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(directory.toFile()).redirectErrorStream(true);
        // The distribution's launcher derives maven.multiModuleProjectDirectory
        // by walking up from the working directory looking for .mvn - which
        // this
        // repository has in the application module, not at the root. Setting it
        // explicitly takes that guess out of the picture.
        builder.environment().put("MAVEN_BASEDIR", directory.toString());
        Process process = builder.start();
        String output;
        try (InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes());
        }
        try {
            return new Attempt(process.waitFor() == 0, output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("classpath resolution interrupted", e);
        }
    }

    /**
     * The Maven to run: an explicit override, then the wrapper nearest the
     * reactor root (the version the reactor was written for), then any wrapper
     * between the application and the root, then Maven on the PATH.
     * <p>
     * The wrapper script reads {@code .mvn/wrapper/maven-wrapper.properties}
     * relative to <em>itself</em>, not to the working directory, so an absolute
     * path to it works from anywhere. Chosen by platform, not by which file
     * happens to exist: a project generated on Windows ships both wrappers, and
     * {@code mvnw.cmd} is a batch file no Linux or macOS shell can run.
     */
    private Path mavenCommand() throws IOException {
        String override = System.getProperty("vaadin.dev.maven");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        String wrapper = WINDOWS ? "mvnw.cmd" : "mvnw";
        List<Path> searched = new ArrayList<>();
        List<Path> candidates = new ArrayList<>();
        candidates.add(reactor.root());
        for (Path dir = root; dir != null
                && !dir.equals(reactor.root()); dir = dir.getParent()) {
            candidates.add(dir);
        }
        for (Path dir : candidates) {
            Path candidate = dir.resolve(wrapper);
            searched.add(candidate);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        Optional<Path> onPath = mavenOnPath();
        if (onPath.isPresent()) {
            log.line("no Maven wrapper found; using " + onPath.get());
            return onPath.get();
        }
        throw new IOException("no Maven wrapper and no mvn on PATH; looked for "
                + searched.stream().map(Path::toString).toList());
    }

    /**
     * Maven on the PATH, resolved by hand because {@code ProcessBuilder} does
     * not apply {@code PATHEXT}: handed the literal "mvn" on Windows it would
     * never find {@code mvn.cmd}.
     */
    private Optional<Path> mavenOnPath() {
        List<String> names = WINDOWS
                ? List.of("mvn.cmd", "mvn.bat", "mvn.exe", "mvn")
                : List.of("mvn");
        List<String> homes = new ArrayList<>();
        for (String variable : List.of("MAVEN_HOME", "M2_HOME")) {
            String value = System.getenv(variable);
            if (value != null && !value.isBlank()) {
                homes.add(value + File.separator + "bin");
            }
        }
        String path = System.getenv("PATH");
        if (path != null) {
            homes.addAll(List.of(path.split(File.pathSeparator)));
        }
        for (String dir : homes) {
            for (String name : names) {
                Path candidate = Path.of(dir).resolve(name);
                if (Files.isRegularFile(candidate)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Driving the build from the reactor root means Maven looks for
     * {@code .mvn/maven.config} there, so a config kept in the application
     * module stops being applied. Silently changing which arguments a build
     * gets is not something to leave for someone to discover.
     */
    private void warnAboutIgnoredMavenConfig() {
        if (!reactor.isMultiModule()) {
            return;
        }
        Path config = root.resolve(".mvn").resolve("maven.config");
        if (Files.isRegularFile(config) && !Files.isRegularFile(
                reactor.root().resolve(".mvn").resolve("maven.config"))) {
            log.line("WARNING: " + config + " is not read when the build runs "
                    + "from " + reactor.root()
                    + "; move it to the reactor root");
        }
    }

    /**
     * The cache stamp: every pom in the reactor, by modification time and size.
     * <p>
     * A stamp rather than a comparison against {@code cp.txt}'s own timestamp,
     * for two reasons. The plugin does not rewrite the file when the classpath
     * is unchanged, so its timestamp stops moving while poms keep changing; and
     * in a reactor any pom can change what the application resolves, including
     * one in a module that is not in the loop.
     */
    private Path stampFile() {
        return workDir(root).resolve("cp.stamp");
    }

    private String currentStamp() {
        StringBuilder sb = new StringBuilder();
        for (Path pom : reactor.poms()) {
            sb.append(pom);
            try {
                sb.append('\t')
                        .append(Files.getLastModifiedTime(pom).toMillis())
                        .append('\t').append(Files.size(pom));
            } catch (IOException e) {
                sb.append("\tmissing");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private boolean stampIsCurrent() {
        Path stamp = stampFile();
        if (!Files.isRegularFile(stamp)
                || !Files.isRegularFile(workDir(root).resolve("cp.txt"))) {
            return false;
        }
        try {
            return Files.readString(stamp).equals(currentStamp());
        } catch (IOException e) {
            return false;
        }
    }

    private void writeStamp() throws IOException {
        Path stamp = stampFile();
        String next = currentStamp();
        changedPoms = changedSince(stamp, next);
        Files.createDirectories(stamp.getParent());
        Files.writeString(stamp, next);
    }

    /**
     * Which poms differ between the stored stamp and the one about to replace
     * it. Named because "a pom changed" in a reactor of ten is not an answer.
     */
    private List<String> changedSince(Path stamp, String next) {
        Map<String, String> before = stampLines(readOrEmpty(stamp));
        Map<String, String> after = stampLines(next);
        List<String> changed = new ArrayList<>();
        after.forEach((path, fingerprint) -> {
            if (!fingerprint.equals(before.get(path))) {
                changed.add(relative(Path.of(path)));
            }
        });
        before.keySet().stream().filter(path -> !after.containsKey(path))
                .forEach(path -> changed.add(relative(Path.of(path))));
        return List.copyOf(changed);
    }

    private static String readOrEmpty(Path file) {
        try {
            return Files.isRegularFile(file) ? Files.readString(file) : "";
        } catch (IOException e) {
            return "";
        }
    }

    private static Map<String, String> stampLines(String stamp) {
        Map<String, String> lines = new LinkedHashMap<>();
        stamp.lines().filter(line -> !line.isBlank()).forEach(line -> {
            int at = line.indexOf('	');
            if (at > 0) {
                lines.put(line.substring(0, at), line.substring(at + 1));
            }
        });
        return lines;
    }

    /** A path as the developer would type it, relative to the application. */
    private String relative(Path path) {
        try {
            return root.relativize(path).toString().replace(File.separatorChar,
                    '/');
        } catch (IllegalArgumentException e) {
            return path.toString();
        }
    }

    /**
     * Maven's own words for why it failed - the first real {@code [ERROR]}
     * line.
     * <p>
     * Not the tail of the output: Maven ends every failure with eight lines of
     * "re-run with -e", "[Help 1]" and "you can resume the build with", so a
     * tail reports the epilogue and never the cause.
     * <p>
     * The first few error lines rather than only the first, because Maven
     * splits the answer: line one says which goal failed on which project, and
     * the line after it is the one that names the artifact it could not
     * resolve.
     */
    private static String failureReason(String output) {
        List<String> errors = output.lines().map(String::strip)
                .filter(line -> line.startsWith("[ERROR]"))
                .map(line -> line.substring("[ERROR]".length()).strip())
                .filter(line -> !line.isEmpty())
                .filter(line -> !BOILERPLATE.matcher(line).find())
                .map(line -> line.replaceAll("\\s*->\\s*\\[Help \\d+\\]$", ""))
                .limit(3).toList();
        if (errors.isEmpty()) {
            return lastLines(output, 10);
        }
        String reason = String.join(" | ", errors);
        return reason.length() <= 400 ? reason
                : reason.substring(0, 397) + "...";
    }

    private static final java.util.regex.Pattern BOILERPLATE = java.util.regex.Pattern
            .compile("^(To see the full stack trace|Re-run Maven|"
                    + "For more information about the errors|"
                    + "After correcting the problems|\\[Help \\d+\\]|mvn <args>)");

    private static String lastLines(String output, int count) {
        List<String> lines = output.strip().lines().toList();
        return String.join(" | ",
                lines.subList(Math.max(0, lines.size() - count), lines.size()));
    }

    /**
     * The class the app JVM is launched with, discovered once and remembered:
     * scanning an output directory is cheap, but a restart should not pay for
     * it twice.
     */
    private String mainClass(Reactor.Module app) throws IOException {
        String known = mainClass;
        if (known != null) {
            return known;
        }
        String found = MainClass.discover(app, log)
                .orElseThrow(() -> new IOException(
                        "no application class found under " + app.classesDir()
                                + ": build the module once, or name the class "
                                + "with -Dvaadin.dev.mainClass"));
        mainClass = found;
        return found;
    }

    /** The full command line, in the order a human would want to read it. */
    List<String> command(int daemonPort, String token) throws IOException {
        Jvm.Jdk java = appJvm();
        Path haJar = ensureHotswapAgent();
        Optional<Path> connectorAgent = agentJar();
        Project resolved = project();
        if (classpathUnusable) {
            // Launching against the app-only fallback would fail inside the app
            // with a stack trace instead of here with the reason.
            throw new IOException("classpath: " + resolutionError);
        }
        if (resolutionError != null) {
            log.line(
                    "WARNING: launching with the last classpath that resolved; "
                            + "the build does not currently resolve: "
                            + resolutionError);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(java.binary().toString());
        cmd.add("-javaagent:" + haJar);
        Optional<Path> presentAgent = connectorAgent
                .filter(Files::isRegularFile);
        if (presentAgent.isPresent()) {
            cmd.add("-javaagent:" + presentAgent.get());
        } else {
            // Without it there is no Instrumentation, so the runtime leg can
            // only
            // ever escalate to a restart. Say so rather than degrading quietly.
            log.line("WARNING: no dev-loop agent jar"
                    + connectorAgent.map(path -> " at " + path).orElse("")
                    + " - every apply will restart instead of hot reloading");
        }
        if (java.jbr()) {
            cmd.add("-XX:+AllowEnhancedClassRedefinition");
        }
        // Vaadin: the plugin declares itself for Vaadin 23-24, though 2.0.3
        // does look for the 25 Hotswapper as well - so the reason it stays off
        // is not that it would fail. It is that it would work: a second driver
        // of onHotswap and BrowserLiveReload, on its own file-watch schedule
        // and outside this transaction, so an apply could no longer say what it
        // did.
        // Spring/SpringBoot: measured to corrupt the context under repeated
        // redefinitions on this stack - HA's scanner loses the Spring Data
        // repository bean ("basePackage not associated with any scannerAgent"),
        // after which the app throws NoSuchBeanDefinitionException while the
        // redefine still reported success. A structural change to a bean is
        // escalated to a restart instead, which is slower but deterministic.
        // The key is "disabledPlugins", plural and unprefixed: HotswapAgent
        // loads
        // hotswap-agent.properties and then merges System.getProperties() over
        // it
        // with the same key names. A wrong name is accepted silently and
        // disables
        // nothing, which is how the Vaadin plugin kept firing its own full page
        // reload on top of Flow's soft refresh.
        cmd.add("-DdisabledPlugins=Vaadin,Spring,SpringBoot");
        cmd.add("-Dspring.devtools.restart.enabled=false");
        ADD_OPENS.forEach(target -> {
            cmd.add("--add-opens");
            cmd.add(target + "=ALL-UNNAMED");
        });
        cmd.add("-Dvaadin.launch-browser=false");
        System.getProperties().stringPropertyNames().stream()
                .filter(Launch::forwardedToApp).sorted().forEach(name -> cmd
                        .add("-D" + name + "=" + System.getProperty(name)));
        cmd.add("-Dvaadin.devloop.daemonPort=" + daemonPort);
        cmd.add("-Dvaadin.devloop.token=" + token);
        // Where the connector reads the bytes of a class it is asked to
        // redefine.
        // A list, in classpath order, because a change can land in any in-loop
        // module's output and the REDEFINE request carries only binary names.
        cmd.add("-Dvaadin.devloop.classes=" + String.join(File.pathSeparator,
                resolved.modules().stream()
                        .map(module -> module.classesDir().toString())
                        .toList()));
        cmd.add("-cp");
        cmd.add(resolved.appClasspath());
        cmd.add(mainClass(resolved.app()));
        // Recorded here rather than in AppProcess: this is the last point at
        // which
        // what the JVM will run is still in one place.
        launchedClasspath = resolved.appClasspath();
        return cmd;
    }

    /**
     * Whether a property the daemon itself was started with is passed on to the
     * app JVM.
     * <p>
     * The point is that a developer can steer the app through
     * {@code VAADIN_DEV_DAEMON_OPTS} without the daemon needing to know each
     * option - {@code -Dvaadin.frontend.hotdeploy=true} to run Vite rather than
     * build a bundle, say. It is an allowlist rather than "forward everything"
     * because the daemon's own JVM carries a hundred properties of its own, and
     * handing the app {@code user.dir} or {@code java.class.path} from another
     * process would be actively wrong.
     * <p>
     * {@code spring.*} is forwarded whole rather than one key at a time. The
     * profile is the case that forces it - an application that only runs under
     * one is otherwise outside the loop entirely - but a datasource, a port or
     * a banner mode are the same kind of thing, and an allowlist that has to
     * grow a name per option is one that is always missing the option someone
     * needs. A property set this way lives as long as the daemon and appears in
     * no file, so it is for steering a local run; anything the project always
     * needs belongs in its own properties files.
     * <p>
     * {@link #LOOP_OWNED} is what cannot be forwarded, because those are put on
     * the app's command line above with the value the loop requires and the
     * forwarding runs after them - and for a repeated {@code -D} the last one
     * wins, so a forwarded copy overrides rather than duplicates. Spring's own
     * devtools restart is the one that matters: two things restarting the
     * application on their own schedules is what the transaction model exists
     * to prevent.
     *
     * @param name
     *            a system property name
     * @return {@code true} if the app JVM is given it too
     */
    static boolean forwardedToApp(String name) {
        if (LOOP_OWNED.contains(name)) {
            return false;
        }
        return name.startsWith("vaadin.") || name.startsWith("spring.");
    }

    /**
     * Properties the loop sets on the app itself, so a forwarded copy would
     * override the value it needs rather than merely repeat it. See
     * {@link #forwardedToApp}.
     */
    private static final Set<String> LOOP_OWNED = Set.of(
            "spring.devtools.restart.enabled", "vaadin.launch-browser",
            "vaadin.devloop.classes");

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(file));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        }
    }

    /** Minimal sink so provisioning progress reaches the client that asked. */
    interface Log {
        void line(String text);
    }
}
