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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Maven backend reads two things back that javac in-process hands over
 * directly: the errors, out of Maven's text output, and the classes it wrote,
 * out of the output directories.
 */
class MavenCompileTest {

    @TempDir
    private Path repo;

    /** How many files {@link #build} has written. */
    private int runs;

    /**
     * maven-compiler-plugin reports each error as it happens and again in the
     * failure summary, with the continuation lines prefixed the second time.
     * Each error is one diagnostic, named the way javac's are.
     */
    @Test
    void eachErrorIsReadOnceWithItsContinuationLines() {
        Reactor.Module app = Reactor.Module.of(repo.resolve("app"), "app");
        Reactor.Module lib = Reactor.Module.of(repo.resolve("lib"), "lib");
        Compile compile = new Compile(project(app, lib));
        String view = asReported(app.sourceDir().resolve("a/View.java"));
        String util = asReported(lib.sourceDir().resolve("l/Util.java"));
        String output = """
                [ERROR] COMPILATION ERROR :\s
                [ERROR] %1$s:[7,31] cannot find symbol
                  symbol:   method three()
                  location: class l.Util
                [ERROR] %2$s:[2,19] incompatible types: java.lang.String cannot be converted to int
                [ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile (default-compile) on project app: Compilation failure: Compilation failure:\s
                [ERROR] %1$s:[7,31] cannot find symbol
                [ERROR]   symbol:   method three()
                [ERROR]   location: class l.Util
                [ERROR] %2$s:[2,19] incompatible types: java.lang.String cannot be converted to int
                [ERROR] -> [Help 1]
                """
                .formatted(view, util);

        assertEquals(List.of(new Compile.Message("View.java", 7, 31, "maven",
                "cannot find method three() in class Util", Optional.empty()),
                new Compile.Message("lib/Util.java", 2, 19, "maven",
                        "incompatible types: String cannot be converted to int",
                        Optional.empty())),
                MavenCompile.parseErrors(output, compile::diagnosticFileOf));
    }

    /**
     * A failure that names no source - a bad release, a plugin that would not
     * load - yields no diagnostic here; the caller reports Maven's own reason.
     */
    @Test
    void aFailureWithNoLocatedErrorYieldsNoDiagnostic() {
        String output = """
                [ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile (default-compile) on project app: Fatal error compiling: error: release version 99 not supported -> [Help 1]
                """;

        assertTrue(MavenCompile.parseErrors(output, Path::toString).isEmpty());
    }

    /**
     * Maven usually recompiles a whole module. A class it wrote with the bytes
     * the application already holds is not reported; a new class and changed
     * bytes are.
     */
    @Test
    void onlyNewClassesAndChangedBytesAreReported() throws IOException {
        Reactor.Module app = Reactor.Module.of(repo.resolve("app"), "app");
        MavenCompile maven = launched(
                writeClass(app, "a/Same.class", "same bytes"),
                writeClass(app, "a/Edited.class", "old bytes"));

        build(app, "a/Same.class", "same bytes");
        build(app, "a/Edited.class", "new bytes");
        build(app, "a/Added.class", "added");

        assertEquals(List.of("a.Added", "a.Edited"),
                maven.changedClasses(List.of(app)).written());
    }

    /**
     * Maven writes to a directory of its own, so a run that fails - after
     * deleting the module's previous classes - leaves the classes directory the
     * application loads from as it was. A run that succeeds changes there only
     * what it changed, and what an annotation processor generated beside the
     * classes.
     */
    @Test
    void onlyWhatChangedReachesTheClassesDirectory() throws IOException {
        Reactor.Module app = Reactor.Module.of(repo.resolve("app"), "app");
        Path same = writeClass(app, "a/Same.class", "same bytes");
        Path edited = writeClass(app, "a/Edited.class", "old bytes");
        Compile.Stamp sameStamp = Compile.stampOf(same).orElseThrow();
        MavenCompile maven = launched(same, edited);

        build(app, "a/Same.class", "same bytes");
        build(app, "a/Edited.class", "new bytes");
        build(app, "a/Added.class", "added");
        build(app, "META-INF/services/a.Service", "a.Impl");
        maven.install(List.of(app), Set.of());

        assertEquals(Optional.of(sameStamp), Compile.stampOf(same));
        assertEquals("new bytes", Files.readString(edited));
        assertEquals("added",
                Files.readString(app.classesDir().resolve("a/Added.class")));
        assertEquals("a.Impl", Files.readString(
                app.classesDir().resolve("META-INF/services/a.Service")));
    }

    /**
     * Whether a source needs compiling is answered by Maven's own output: an
     * edit that compiled to the same bytes is not copied on, so the classes
     * directory's copy stays older than the source. Before Maven has written
     * the class, the classes directory's copy answers.
     */
    @Test
    void aSourceIsComparedWithWhatMavenLastBuiltForIt() throws IOException {
        Reactor.Module app = Reactor.Module.of(repo.resolve("app"), "app");
        Path source = app.sourceDir().resolve("a/View.java");
        Path loaded = writeClass(app, "a/View.class", "bytes");
        MavenCompile maven = launched(loaded);

        assertEquals(loaded, maven.artifactFor(app, source));
        build(app, "a/View.class", "bytes");
        assertEquals(MavenCompile.stagingDir(app).resolve("a/View.class"),
                maven.artifactFor(app, source));
    }

    /**
     * An edit that compiled but never reached the application - its redefine
     * failed, under {@code --no-restart} - and was then reverted compiles back
     * to the bytes the application holds. There is nothing to redefine, but the
     * classes directory must not keep the edit for a later class load or a
     * restart to run.
     */
    @Test
    void aRevertedEditLeavesTheClassesDirectory() throws IOException {
        Reactor.Module app = Reactor.Module.of(repo.resolve("app"), "app");
        Path view = writeClass(app, "a/View.class", "old bytes");
        MavenCompile maven = launched(view);

        build(app, "a/View.class", "edited bytes");
        maven.changedClasses(List.of(app));
        maven.install(List.of(app), Set.of());
        build(app, "a/View.class", "old bytes");
        MavenCompile.Diff reverted = maven.changedClasses(List.of(app));
        maven.install(List.of(app), Set.of());

        assertEquals(List.of(), reverted.written());
        assertEquals("old bytes", Files.readString(view));
    }

    /**
     * A nested class taken out of a source that stays: Maven no longer builds
     * it, so it leaves the classes directory, and as the application holds it,
     * it is reported for a restart. A class Maven has never built and that no
     * Java source compiles to - another compiler's - stays.
     */
    @Test
    void aClassMavenNoLongerBuildsIsRemoved() throws IOException {
        Reactor.Module app = Reactor.Module.of(repo.resolve("app"), "app");
        Files.createDirectories(app.sourceDir().resolve("a"));
        Files.writeString(app.sourceDir().resolve("a/View.java"), "source");
        Path view = writeClass(app, "a/View.class", "view");
        Path removed = writeClass(app, "a/View$Removed.class", "removed");
        Path kotlin = writeClass(app, "k/Script.class", "kotlin");
        MavenCompile maven = launched(view, removed, kotlin);
        build(app, "a/View.class", "view");
        build(app, "a/View$Removed.class", "removed");

        Set<Path> before = maven.builtClassFiles(List.of(app));
        Files.delete(
                MavenCompile.stagingDir(app).resolve("a/View$Removed.class"));
        build(app, "a/View.class", "view without it");
        maven.changedClasses(List.of(app));
        maven.install(List.of(app), before);

        assertFalse(Files.exists(removed));
        assertTrue(Files.exists(kotlin));
        assertEquals(List.of("a.View$Removed"),
                maven.removedClasses(List.of(app)));
    }

    /**
     * What the application holds moves only once a redefine took the classes. A
     * compile that never reached the application - superseded, or applied with
     * {@code --no-restart} - has to report the same classes again on the next
     * run, which writes the same bytes.
     */
    @Test
    void classesAreReportedUntilTheApplicationHasThem() throws IOException {
        Reactor.Module app = Reactor.Module.of(repo.resolve("app"), "app");
        MavenCompile maven = launched(
                writeClass(app, "a/View.class", "old bytes"));

        build(app, "a/View.class", "new bytes");
        assertEquals(List.of("a.View"),
                maven.changedClasses(List.of(app)).written());
        build(app, "a/View.class", "new bytes");
        MavenCompile.Diff second = maven.changedClasses(List.of(app));
        assertEquals(List.of("a.View"), second.written());

        maven.markApplied(applied(second));
        build(app, "a/View.class", "new bytes");
        assertEquals(List.of(), maven.changedClasses(List.of(app)).written());
    }

    /**
     * Applies overlap: a newer compile can report other bytes while an older
     * one's redefine is still in flight. The older redefine going live must not
     * make the newer bytes count as live too.
     */
    @Test
    void aRedefineTakesOnlyTheBytesItsCompileReported() throws IOException {
        Reactor.Module app = Reactor.Module.of(repo.resolve("app"), "app");
        MavenCompile maven = launched(
                writeClass(app, "a/View.class", "old bytes"));

        build(app, "a/View.class", "first edit");
        MavenCompile.Diff older = maven.changedClasses(List.of(app));
        build(app, "a/View.class", "second edit");
        maven.changedClasses(List.of(app));
        maven.markApplied(applied(older));

        assertEquals(List.of("a.View"),
                maven.changedClasses(List.of(app)).written());
    }

    /** A successful compile that reported what a run changed. */
    private static Compile.Result applied(MavenCompile.Diff diff) {
        return new Compile.Result(true, List.of(), diff.written(), 0,
                diff.classFiles(), List.of());
    }

    /** A backend seeded as if the application had just loaded these files. */
    private static MavenCompile launched(Path... classFiles) {
        MavenCompile maven = new MavenCompile(null, null);
        Map<Path, Compile.Stamp> classes = new HashMap<>();
        for (Path file : classFiles) {
            classes.put(file, Compile.stampOf(file).orElseThrow());
        }
        maven.seed(classes);
        maven.takeMissingDigests();
        return maven;
    }

    /** A class file in the classes directory the application loads from. */
    private static Path writeClass(Reactor.Module module, String relative,
            String bytes) throws IOException {
        Path file = module.classesDir().resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, bytes);
        return file;
    }

    /**
     * A file as a Maven run writes it, with a modification time no earlier run
     * can share.
     */
    private void build(Reactor.Module module, String relative, String bytes)
            throws IOException {
        Path file = MavenCompile.stagingDir(module).resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, bytes);
        Files.setLastModifiedTime(file, FileTime
                .fromMillis(System.currentTimeMillis() + 2000L * ++runs));
    }

    /**
     * A path as maven-compiler-plugin prints it: a URI path on Windows, with a
     * slash ahead of the drive letter.
     */
    private static String asReported(Path source) {
        return File.separatorChar == '\\'
                ? "/" + source.toString().replace('\\', '/')
                : source.toString();
    }

    private static Launch.Project project(Reactor.Module... modules) {
        return new Launch.Project(List.of(modules), "", Map.of(),
                OptionalInt.empty());
    }
}
