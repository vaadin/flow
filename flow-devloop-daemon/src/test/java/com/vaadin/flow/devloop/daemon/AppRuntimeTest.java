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
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which runtime a project gets decides what the daemon launches, and getting it
 * wrong launches the wrong thing - or nothing at all. The ordering cases below
 * are the ones that made the order what it is.
 */
class AppRuntimeTest {

    @TempDir
    private Path repo;

    private final List<String> logged = new java.util.ArrayList<>();

    private final Launch.Log log = logged::add;

    @Test
    void warWithJettyPlugin_runsThroughTheBuild() throws IOException {
        Path app = warModule("app", "12.1.13", "");

        AppRuntime runtime = runtimeOf(app);

        assertEquals("jetty-ee10", runtime.name());
    }

    /**
     * ee11 is the same plugin at another Jakarta EE level, and its run mojo
     * agrees with ee10 on every parameter this runtime depends on: {@code
     * deployMode} defaults to {@code EMBED} under {@code ${jetty.deployMode}},
     * {@code scan} to {@code -1} under {@code ${jetty.scan}}, and the goal
     * forks {@code test-compile}. Verified by hand end to end - detected as
     * {@code jetty-ee11}, served, hot-swapped a method body in 1.9s - so what
     * is pinned here is the table entry that has to keep agreeing with it.
     */
    @Test
    void ee11IsRecognisedToo() throws IOException {
        Path app = module("app", "war", plugin("org.eclipse.jetty.ee11",
                "jetty-ee11-maven-plugin", "12.1.13", ""));

        assertEquals("jetty-ee11", runtimeOf(app).name());
    }

    @Test
    void ee11DrivesTheSameGoalAndReadsTheSameReadinessLine()
            throws IOException {
        Path app = module("app", "war", plugin("org.eclipse.jetty.ee11",
                "jetty-ee11-maven-plugin", "12.1.13", ""));
        AppRuntime runtime = runtimeOf(app);

        // Jetty abbreviates the logger name, so the line carries
        // "oejs.ServerConnector" rather than the full class - which is why the
        // pattern matches on the "…Connector@" suffix rather than a full name.
        String line = "[INFO] Started oejs.ServerConnector@343c341d"
                + "{HTTP/1.1, (http/1.1)}{0.0.0.0:8898}";
        assertTrue(runtime.serving(line));
        assertEquals(OptionalInt.of(8898), runtime.port(line));
    }

    /**
     * A version the reactor cannot interpolate - {@code ${jetty.version}}
     * declared in a parent outside the checkout - leaves the goal unpinned, and
     * that is the right answer rather than a guess: Maven then resolves the
     * version from the project's own build section exactly as it would for any
     * other invocation. Observed doing precisely that when this fixture ran
     * under ee11 from its own reactor root.
     */
    @Test
    void anUnresolvableVersionLeavesTheGoalForMavenToPin() throws IOException {
        Path app = module("app", "war", plugin("org.eclipse.jetty.ee11",
                "jetty-ee11-maven-plugin", "${jetty.version}", ""));

        Reactor.PluginConfig declared = Reactor.discover(app, log)
                .plugin("org.eclipse.jetty.ee11", "jetty-ee11-maven-plugin")
                .orElseThrow();

        assertEquals("org.eclipse.jetty.ee11:jetty-ee11-maven-plugin",
                declared.coordinates());
    }

    /**
     * The case the whole ordering exists for: a Spring Boot application may be
     * packaged as a WAR and may even declare a servlet-container plugin, and
     * launching it through that plugin rather than through its own main class
     * would start something the developer never asked for.
     */
    @Test
    void springBootWarStaysOnItsMainClass() throws IOException {
        Path app = warModule("app", "12.1.13", "");
        packagedJarNaming(app, "com.example.Application");

        assertEquals(MainClassRuntime.NAME, runtimeOf(app).name());
    }

    /**
     * The other half of the ordering: a WAR project is entitled to carry some
     * unrelated main method, and that must not be taken for an entry point.
     */
    @Test
    void strayMainMethodDoesNotBeatTheServerPlugin() throws IOException {
        Path app = warModule("app", "12.1.13", "");
        // A real main method on the output, so that MainClass.discover would
        // find one. The build has still named nothing, and what the developer
        // deploys is the WAR.
        compileInto(app, """
                package tools;
                public class Importer {
                  public static void main(String[] args) { }
                }
                """);
        assertEquals(Optional.of("tools.Importer"),
                MainClass.discover(moduleOf(app), log));
        assertTrue(MainClass.namedByBuild(moduleOf(app)).isEmpty());

        assertEquals("jetty-ee10", runtimeOf(app).name());
    }

    @Test
    void noEntryPointAndNoServerPlugin_saysSoOnce() throws IOException {
        Path app = module("app", "jar", "");

        IOException thrown = assertThrows(IOException.class,
                () -> runtimeOf(app));

        assertTrue(thrown.getMessage().contains("cannot tell how to start"),
                thrown.getMessage());
        // Both escape hatches named, so the message is actionable.
        assertTrue(thrown.getMessage().contains("-Dvaadin.dev.mainClass"));
        assertTrue(thrown.getMessage().contains("-Dvaadin.dev.runtime"));
    }

    @Test
    void pluginVersionIsInterpolatedFromProperties() throws IOException {
        Path app = module("app", "war",
                "<properties><jetty.version>12.1.13</jetty.version>"
                        + "</properties>"
                        + plugin("org.eclipse.jetty.ee10",
                                "jetty-ee10-maven-plugin", "${jetty.version}",
                                ""));

        Reactor reactor = Reactor.discover(app, log);
        Reactor.PluginConfig declared = reactor
                .plugin("org.eclipse.jetty.ee10", "jetty-ee10-maven-plugin")
                .orElseThrow();

        assertEquals("org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:12.1.13",
                declared.coordinates());
    }

    /**
     * A pom that pins these wins over the command line, so the developer has to
     * be told rather than left to wonder why the app keeps redeploying itself.
     */
    @Test
    void pinnedScanAndDeployModeAreWarnedAbout() throws IOException {
        Path app = warModule("app", "12.1.13", "<configuration><scan>2</scan>"
                + "<deployMode>FORK</deployMode></configuration>");

        List<String> warnings = runtimeOf(app).warnings();

        assertEquals(2, warnings.size(), warnings.toString());
        assertTrue(
                warnings.stream()
                        .anyMatch(line -> line.contains("<scan>2</scan>")
                                && line.contains("set <scan>0</scan>")),
                warnings.toString());
        assertTrue(
                warnings.stream().anyMatch(
                        line -> line.contains("<deployMode>FORK</deployMode>")),
                warnings.toString());
    }

    @Test
    void scanAlreadyOff_isNotWarnedAbout() throws IOException {
        Path app = warModule("app", "12.1.13",
                "<configuration><scan>0</scan></configuration>");

        assertEquals(List.of(), runtimeOf(app).warnings());
    }

    @Test
    void jettyReportsItIsServing_andWhichPort() throws IOException {
        AppRuntime runtime = runtimeOf(warModule("app", "12.1.13", ""));
        String line = "[INFO] Started ServerConnector@6e1567f1"
                + "{HTTP/1.1, (http/1.1)}{0.0.0.0:8899}";

        assertTrue(runtime.serving(line));
        assertEquals(OptionalInt.of(8899), runtime.port(line));
    }

    /**
     * The line before the bind names the protocol, not the port. Taking it for
     * the readiness signal would wave a port clash through as a success.
     */
    @Test
    void jettyStartingUpIsNotYetServing() throws IOException {
        AppRuntime runtime = runtimeOf(warModule("app", "12.1.13", ""));

        assertFalse(runtime.serving(
                "[INFO] jetty-ee10-maven-plugin:12.1.13:run @ project-base"));
        assertFalse(runtime.serving("[INFO] Started oejs.Server@1b2c4efb"));
    }

    /**
     * Maven splits MAVEN_OPTS on whitespace and no quoting survives it, so an
     * agent jar under a path with a space has to be named rather than left to
     * fail as a JVM that would not start.
     */
    @Test
    void aFlagWithASpaceIsCalledOut() {
        List<String> warnings = MavenGoalRuntime.unsplittable(
                List.of("-javaagent:C:\\Users\\First Last\\ha.jar",
                        "-javaagent:/home/dev/ha.jar"));

        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("First Last"));
    }

    /** One source, compiled into the module's own output directory. */
    private void compileInto(Path module, String source) throws IOException {
        Path file = module.resolve("src").resolve("main").resolve("java")
                .resolve("tools").resolve("Importer.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
        Path classes = module.resolve("target").resolve("classes");
        assertEquals(0,
                javax.tools.ToolProvider.getSystemJavaCompiler().run(null, null,
                        null, "-d", classes.toString(), "-nowarn",
                        file.toString()),
                "test fixture did not compile");
    }

    private AppRuntime runtimeOf(Path app) throws IOException {
        return AppRuntime.of(new Launch(Reactor.discover(app, log), log), log);
    }

    private Reactor.Module moduleOf(Path app) {
        return Reactor.discover(app, log).app();
    }

    /**
     * An application module packaged as a WAR, with the Jetty 12 ee10 plugin.
     */
    private Path warModule(String name, String version, String configuration)
            throws IOException {
        return module(name, "war", plugin("org.eclipse.jetty.ee10",
                "jetty-ee10-maven-plugin", version, configuration));
    }

    private static String plugin(String groupId, String artifactId,
            String version, String configuration) {
        return "<build><plugins><plugin>" + "<groupId>" + groupId
                + "</groupId><artifactId>" + artifactId
                + "</artifactId><version>" + version + "</version>"
                + configuration + "</plugin></plugins></build>";
    }

    private Path module(String relative, String packaging, String extra)
            throws IOException {
        Path dir = repo.resolve(relative);
        Files.createDirectories(
                dir.resolve("src").resolve("main").resolve("java"));
        Files.createDirectories(dir.resolve("target").resolve("classes"));
        Files.writeString(dir.resolve("pom.xml"),
                "<project><artifactId>" + dir.getFileName()
                        + "</artifactId><packaging>" + packaging
                        + "</packaging>" + extra + "</project>\n");
        return dir;
    }

    /**
     * A jar in the module's target whose manifest names a class, which is how
     * the build states an entry point.
     */
    private void packagedJarNaming(Path module, String mainClass)
            throws IOException {
        Path jar = module.resolve("target").resolve("app.jar");
        java.util.jar.Manifest manifest = new java.util.jar.Manifest();
        manifest.getMainAttributes()
                .put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Start-Class", mainClass);
        try (java.util.jar.JarOutputStream out = new java.util.jar.JarOutputStream(
                Files.newOutputStream(jar), manifest)) {
            // The manifest is the whole point; the jar needs no entries.
        }
        assertEquals(Optional.of(mainClass),
                MainClass.namedByBuild(moduleOf(module)));
    }
}
