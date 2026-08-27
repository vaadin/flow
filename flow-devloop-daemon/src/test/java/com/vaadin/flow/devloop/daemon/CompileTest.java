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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The compile leg is one instance across every module in the loop, but one
 * javac invocation per module: javac takes a single {@code -d}, so a shared
 * pass would write a library's classes into the application's output, where
 * Maven would never look for them again.
 */
class CompileTest {

    @TempDir
    private Path repo;

    @Test
    void compile_writesEachModulesClassesIntoItsOwnOutput() throws IOException {
        Reactor.Module app = module("app", "Main", """
                package app;
                public class Main {
                    public static String greet() { return shared.Util.text(); }
                }
                """);
        Reactor.Module shared = module("shared", "Util", """
                package shared;
                public class Util {
                    public static String text() { return "hello"; }
                }
                """);
        Launch.Project project = project(app, shared);
        Compile compile = new Compile(project);

        Compile.Result result = compile.compile(
                List.of(source(shared, "Util"), source(app, "Main")), project);

        assertTrue(result.success(), () -> "errors: " + result.errors());
        assertTrue(Files.isRegularFile(
                shared.classesDir().resolve("shared").resolve("Util.class")));
        assertTrue(Files.isRegularFile(
                app.classesDir().resolve("app").resolve("Main.class")));
        // The point of grouping: the sibling's class must not appear under the
        // application's output.
        assertFalse(Files.exists(
                app.classesDir().resolve("shared").resolve("Util.class")));
        assertEquals(List.of("app.Main", "shared.Util"),
                result.writtenClasses());
    }

    @Test
    void compile_dependencyErrorEndsTheCompileWithoutTheConsequences()
            throws IOException {
        Reactor.Module app = module("app", "Main", """
                package app;
                public class Main {
                    public static String greet() { return shared.Util.text(); }
                }
                """);
        Reactor.Module shared = module("shared", "Util", """
                package shared;
                public class Util {
                    public static String text() { return missing(); }
                }
                """);
        Launch.Project project = project(app, shared);
        Compile compile = new Compile(project);

        Compile.Result result = compile.compile(
                List.of(source(shared, "Util"), source(app, "Main")), project);

        assertFalse(result.success());
        // Only the upstream module's error: the application's would be a
        // consequence, and reporting it would bury the line to fix.
        assertEquals(1, result.errors().size());
        assertEquals("shared/Util.java", result.errors().get(0).file());
    }

    @Test
    void diagnostic_namesTheModuleForAFileOutsideTheApplication()
            throws IOException {
        Reactor.Module app = module("app", "Main", """
                package app;
                public class Main { }
                """);
        Reactor.Module shared = module("shared", "Util", """
                package shared;
                public class Util { void x() { nope(); } }
                """);
        Launch.Project project = project(app, shared);
        Compile compile = new Compile(project);

        Compile.Result result = compile.compile(List.of(source(shared, "Util")),
                project);

        // "Util.java:2" in a project with five modules is not enough to open
        // the file with.
        assertEquals("shared/Util.java", result.errors().get(0).file());
    }

    @Test
    void stale_reportsASourceWhoseClassFileIsMissing() throws IOException {
        Reactor.Module app = module("app", "Main", """
                package app;
                public class Main { }
                """);
        Compile compile = new Compile(project(app));

        Compile.Changes changes = compile.stale();

        assertEquals(List.of(source(app, "Main")), changes.modified());
    }

    @Test
    void seedFromDisk_makesAnUntouchedProjectReportNoChanges()
            throws IOException {
        Reactor.Module app = module("app", "Main", """
                package app;
                public class Main { }
                """);
        Launch.Project project = project(app);
        Compile compile = new Compile(project);
        compile.compile(List.of(source(app, "Main")), project);

        compile.seedFromDisk();

        assertTrue(compile.stale().isEmpty());
    }

    @Test
    void classpathForced_recompilesTheWholeModuleWhoseClasspathMoved()
            throws IOException {
        Reactor.Module app = module("app", "Main", """
                package app;
                public class Main { }
                """);
        Launch.Project before = project(app);
        Compile compile = new Compile(before);
        // A pom edit changes no source file, so the source scan cannot see that
        // a module no longer compiles; the classpath comparison is what does.
        Launch.Project after = new Launch.Project(List.of(app),
                before.appClasspath(),
                Map.of(app.artifactId(), before.appClasspath()
                        + File.pathSeparator + repo.resolve("added.jar")));

        assertEquals(List.of(source(app, "Main")),
                compile.classpathForced(after));
        assertEquals(List.of("app"), compile.classpathChangedModules(after));
    }

    @Test
    void relative_namesASiblingAsTheDeveloperWouldTypeIt() throws IOException {
        Reactor.Module app = module("app", "Main", """
                package app;
                public class Main { }
                """);
        Reactor.Module shared = module("shared", "Util", """
                package shared;
                public class Util { }
                """);
        Compile compile = new Compile(project(app, shared));

        assertEquals("../shared/src/main/java/shared/Util.java",
                compile.relative(source(shared, "Util")));
    }

    @Test
    void tidy_collapsesJavacsRepetitionAndShortensNames() {
        // javac says "cannot find symbol symbol: method bar() location: class
        // com.example.Foo" over three lines; output length is a real cost for
        // an
        // agent reading this every apply.
        assertEquals("cannot find method bar() in class Foo", Compile.tidy(
                "cannot find symbol symbol:   method bar() location:   class com.example.Foo"));
    }

    private Reactor.Module module(String name, String simpleName, String source)
            throws IOException {
        Path dir = repo.resolve(name);
        String pkg = source.lines().filter(line -> line.startsWith("package "))
                .findFirst().orElseThrow().replace("package ", "")
                .replace(";", "").trim();
        Path sourceDir = dir.resolve("src").resolve("main").resolve("java")
                .resolve(pkg.replace('.', File.separatorChar));
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve(simpleName + ".java"), source);
        Files.createDirectories(dir.resolve("target").resolve("classes"));
        return Reactor.Module.of(dir, name);
    }

    private Path source(Reactor.Module module, String simpleName)
            throws IOException {
        try (var walk = Files.walk(module.sourceDir())) {
            return walk
                    .filter(path -> path.getFileName().toString()
                            .equals(simpleName + ".java"))
                    .findFirst().orElseThrow();
        }
    }

    /**
     * Each module compiles against its own module's output plus the others', as
     * {@code Launch.assemble} would produce it.
     */
    private Launch.Project project(Reactor.Module... modules) {
        List<Reactor.Module> all = List.of(modules);
        String classpath = String.join(File.pathSeparator, all.stream()
                .map(module -> module.classesDir().toString()).toList());
        Map<String, String> compile = new java.util.LinkedHashMap<>();
        all.forEach(module -> compile.put(module.artifactId(), classpath));
        return new Launch.Project(all, classpath, compile);
    }
}
