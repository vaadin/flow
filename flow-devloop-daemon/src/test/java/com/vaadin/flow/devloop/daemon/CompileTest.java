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
    void compile_emitsBytecodeTheApplicationsJvmCanLoad() throws IOException {
        Reactor.Module app = module("app", "Main", """
                package app;
                public class Main { }
                """);
        Launch.Project project = new Launch.Project(List.of(app),
                app.classesDir().toString(),
                Map.of(app.artifactId(), app.classesDir().toString()),
                java.util.OptionalInt.of(21));

        Compile.Result result = new Compile(project)
                .compile(List.of(source(app, "Main")), project);

        assertTrue(result.success(), () -> "errors: " + result.errors());
        // Major 65 is Java 21. Without --release javac would emit at the
        // daemon's own level, which the app's JVM may be too old to load.
        assertEquals(65, majorVersion(
                app.classesDir().resolve("app").resolve("Main.class")));
    }

    private int majorVersion(Path classFile) throws IOException {
        byte[] header = Files.readAllBytes(classFile);
        return ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
    }

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
    void stale_reportsADeletedSourceAgainstTheInventory() throws IOException {
        // A walk only sees what is there, so the fingerprint map is what
        // answers: a deleted route or bean would otherwise be a silent "no
        // changes" over a class the JVM is still serving.
        Reactor.Module app = module("app", "Main", """
                package app;
                public class Main { }
                """);
        Launch.Project project = project(app);
        Compile compile = new Compile(project);
        Path gone = source(app, "Main");
        compile.compile(List.of(gone), project);
        compile.seedFromDisk();
        Files.delete(gone);

        Compile.Changes changes = compile.stale();

        assertEquals(List.of(gone), changes.deleted());
        assertTrue(changes.modified().isEmpty());
        // Not forgotten when it is acted on: the class stays loaded until the
        // application restarts, and only the restart's re-seed clears it.
        assertEquals(List.of(gone), compile.stale().deleted());
        compile.seedFromDisk();
        assertTrue(compile.stale().isEmpty());
    }

    @Test
    void removeClassArtifacts_takesTheClassAndItsNestedClassesOff()
            throws IOException {
        // The .class is what a restart would load the removed type back from.
        Reactor.Module app = module("app", "Main", """
                package app;
                public class Main {
                    static class Inner { }
                    Runnable r = new Runnable() { public void run() { } };
                }
                """);
        Launch.Project project = project(app);
        Compile compile = new Compile(project);
        Path gone = source(app, "Main");
        compile.compile(List.of(gone), project);
        Path classes = app.classesDir().resolve("app");
        assertTrue(Files.isRegularFile(classes.resolve("Main.class")));
        assertTrue(Files.isRegularFile(classes.resolve("Main$Inner.class")));
        assertTrue(Files.isRegularFile(classes.resolve("Main$1.class")));
        Files.delete(gone);

        List<Path> removed = compile.removeClassArtifacts(List.of(gone));

        assertEquals(List.of(classes.resolve("Main$1.class"),
                classes.resolve("Main$Inner.class"),
                classes.resolve("Main.class")), removed);
        assertFalse(Files.exists(classes.resolve("Main.class")));
        assertFalse(Files.exists(classes.resolve("Main$Inner.class")));
        assertFalse(Files.exists(classes.resolve("Main$1.class")));
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
                        + File.pathSeparator + repo.resolve("added.jar")),
                before.release());

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
    void staleFrontend_afterSeeding_isQuiet() throws IOException {
        Compile compile = withFrontend("views/main.ts", "themes/t/styles.css");
        compile.seedFromDisk();

        assertTrue(compile.staleFrontend().isEmpty());
    }

    @Test
    void staleFrontend_seesEditsAndAdditions() throws IOException {
        Compile compile = withFrontend("views/main.ts");
        compile.seedFromDisk();
        Path added = write("app/src/main/frontend/views/extra.ts",
                "export {};");
        touch("app/src/main/frontend/views/main.ts");

        Compile.FrontendChanges changes = compile.staleFrontend();

        assertEquals(2, changes.modified().size());
        assertTrue(changes.modified().contains(added));
        assertTrue(changes.deleted().isEmpty());
    }

    @Test
    void staleFrontend_reportsADeletionOnceAndThenForgetsIt()
            throws IOException {
        Compile compile = withFrontend("views/main.ts", "views/gone.ts");
        compile.seedFromDisk();
        Path gone = repo.resolve("app/src/main/frontend/views/gone.ts");
        Files.delete(gone);

        Compile.FrontendChanges first = compile.staleFrontend();
        assertEquals(List.of(gone), first.deleted());

        // Acting on it is what forgets it; otherwise every later apply would
        // escalate again over a file nobody is going to restore.
        compile.markFrontendNotified(first);
        assertTrue(compile.staleFrontend().isEmpty());
    }

    @Test
    void staleFrontend_seedingKeepsAnEditMadeSinceTheAppStarted()
            throws IOException {
        // The "start, edit, first apply" sequence. A frontend file has no
        // artifact to be newer than, so a baseline taken while the app is
        // already running would otherwise swallow the edit and answer "no
        // changes" to the very change it was asked about.
        Compile compile = withFrontend("views/main.ts", "views/settled.ts");
        long appStarted = System.currentTimeMillis();
        touch("app/src/main/frontend/views/main.ts");

        compile.seedFromDisk(appStarted);

        Compile.FrontendChanges changes = compile.staleFrontend();
        assertEquals(
                List.of(repo.resolve("app/src/main/frontend/views/main.ts")),
                changes.modified());
    }

    @Test
    void staleFrontend_neverOffersBuildOutput() throws IOException {
        // The one rule whose failure makes every single apply noisy: generated/
        // is rewritten by the build, not by the developer.
        Compile compile = withFrontend("views/main.ts");
        compile.seedFromDisk();
        write("app/src/main/frontend/generated/vaadin.ts", "export {};");
        write("app/src/main/frontend/node_modules/dep/index.js",
                "module.exports={};");

        assertTrue(compile.staleFrontend().isEmpty());
    }

    @Test
    void staleFrontend_withNoFrontendFolder_isEmpty() throws IOException {
        Reactor.Module app = module("app", "Main", """
                package app;
                public class Main { }
                """);
        Compile compile = new Compile(project(app));
        compile.seedFromDisk();

        assertTrue(compile.staleFrontend().isEmpty());
    }

    @Test
    void staleResources_separateWhatTheAppServesFromWhatItReadAtStartup()
            throws IOException {
        Reactor.Module app = module("app", "Main", """
                package app;
                public class Main { }
                """);
        Path served = write(
                "app/src/main/resources/META-INF/resources/site.css", "body{}");
        Path config = write("app/src/main/resources/application.properties",
                "server.port=8080");
        write("app/src/main/resources/.application.properties.swp", "vim");
        Compile compile = new Compile(project(app));

        Compile.ResourceChanges changes = compile.staleResources();

        // Copying application.properties onto the classpath does not change
        // what the running JVM was configured with, so it must not be reported
        // as something a push can make live.
        assertEquals(List.of(served), changes.live().modified());
        assertEquals(List.of(config), changes.startup().modified());
    }

    @Test
    void staleResources_reportADeletedResourceTheWalkCannotSee()
            throws IOException {
        Reactor.Module app = module("app", "Main", """
                package app;
                public class Main { }
                """);
        Path served = write(
                "app/src/main/resources/META-INF/resources/site.css", "body{}");
        Path config = write("app/src/main/resources/application.properties",
                "server.port=8080");
        Compile compile = new Compile(project(app));
        compile.copyResources(compile.staleResources().copies());
        compile.seedResources();
        Files.delete(served);
        Files.delete(config);

        Compile.ResourceChanges changes = compile.staleResources();

        assertEquals(List.of(served), changes.live().deleted());
        assertEquals(List.of(config), changes.startup().deleted());
    }

    @Test
    void removeResourceCopies_takesTheFileOffTheClasspath() throws IOException {
        // The copy is what the application reads: a deletion that leaves it
        // behind goes on being served until the next full Maven build.
        Reactor.Module app = module("app", "Main", """
                package app;
                public class Main { }
                """);
        Path served = write(
                "app/src/main/resources/META-INF/resources/site.css", "body{}");
        Compile compile = new Compile(project(app));
        compile.copyResources(List.of(served));
        Path copy = app.classesDir().resolve("META-INF/resources/site.css");
        assertTrue(Files.isRegularFile(copy));
        compile.seedResources();
        Files.delete(served);

        assertEquals(List.of(copy),
                compile.removeResourceCopies(List.of(served)));
        assertFalse(Files.exists(copy));

        // Acting on it is what forgets it; otherwise every later apply would
        // offer a file nobody is going to restore.
        compile.forgetResources(List.of(served));
        assertTrue(compile.staleResources().isEmpty());
    }

    @Test
    void resourceKind_isDecidedByThePublicResourceRoots() {
        assertEquals(Compile.ResourceKind.LIVE,
                Compile.resourceKindOf("META-INF/resources/site.css"));
        assertEquals(Compile.ResourceKind.LIVE,
                Compile.resourceKindOf("static/styles.css"));
        assertEquals(Compile.ResourceKind.STARTUP,
                Compile.resourceKindOf("application.properties"));
        // Folded into the dev bundle at startup, not served from here.
        assertEquals(Compile.ResourceKind.STARTUP,
                Compile.resourceKindOf("META-INF/frontend/my-view.js"));
        assertEquals(Compile.ResourceKind.IGNORED,
                Compile.resourceKindOf("static/.styles.css.swp"));
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

    /** An application module with a frontend folder holding these files. */
    private Compile withFrontend(String... frontendFiles) throws IOException {
        Reactor.Module app = module("app", "Main", """
                package app;
                public class Main { }
                """);
        for (String relative : frontendFiles) {
            write("app/src/main/frontend/" + relative, "export {};");
        }
        return new Compile(project(app));
    }

    private Path write(String relative, String content) throws IOException {
        Path file = repo.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    /**
     * A fingerprint is modification time plus size, so a rewrite of the same
     * length needs the timestamp moved to be visible - and a same-millisecond
     * write would otherwise make this test flaky on a coarse clock.
     */
    private void touch(String relative) throws IOException {
        Path file = repo.resolve(relative);
        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime
                .fromMillis(System.currentTimeMillis() + 2000));
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
        return new Launch.Project(all, classpath, compile,
                java.util.OptionalInt.empty());
    }
}
