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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Maven backend reads two things back that javac in-process hands over
 * directly: the errors, out of Maven's text output, and the classes it wrote,
 * out of the output directories.
 */
class MavenCompileTest {

    @TempDir
    private Path repo;

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
        Path same = writeClass(app, "a/Same.class", "same bytes");
        Path edited = writeClass(app, "a/Edited.class", "old bytes");
        MavenCompile maven = launched(same, edited);

        rewrite(same, "same bytes");
        rewrite(edited, "new bytes");
        writeClass(app, "a/Added.class", "added");

        assertEquals(List.of("a.Added", "a.Edited"),
                maven.changedClasses(List.of(app)).written());
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
        Path view = writeClass(app, "a/View.class", "old bytes");
        MavenCompile maven = launched(view);

        rewrite(view, "new bytes");
        assertEquals(List.of("a.View"),
                maven.changedClasses(List.of(app)).written());
        rewrite(view, "new bytes");
        assertEquals(List.of("a.View"),
                maven.changedClasses(List.of(app)).written());

        maven.markApplied();
        rewrite(view, "new bytes");
        assertEquals(List.of(), maven.changedClasses(List.of(app)).written());
    }

    /** A backend seeded as if the application had just loaded these files. */
    private static MavenCompile launched(Path... classFiles) {
        MavenCompile maven = new MavenCompile(null, null);
        Map<Path, Compile.Stamp> classes = new java.util.HashMap<>();
        for (Path file : classFiles) {
            classes.put(file, Compile.stampOf(file).orElseThrow());
        }
        maven.seed(classes);
        maven.takeMissingDigests();
        return maven;
    }

    private static Path writeClass(Reactor.Module module, String relative,
            String bytes) throws IOException {
        Path file = module.classesDir().resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, bytes);
        return file;
    }

    /** Rewrites a file with a modification time no earlier write can share. */
    private static void rewrite(Path file, String bytes) throws IOException {
        long before = Files.getLastModifiedTime(file).toMillis();
        Files.writeString(file, bytes);
        Files.setLastModifiedTime(file, FileTime.fromMillis(before + 2000));
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
