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
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        Path app = warModule("app", "12.1.13");

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
        Path app = ee11Module("app", "12.1.13");

        assertEquals("jetty-ee11", runtimeOf(app).name());
    }

    @Test
    void ee11DrivesTheSameGoalAndReadsTheSameReadinessLine()
            throws IOException {
        Path app = ee11Module("app", "12.1.13");
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
     * The case the whole ordering exists for: a Spring Boot application may be
     * packaged as a WAR and may even declare a servlet-container plugin, and
     * launching it through that plugin rather than through its own main class
     * would start something the developer never asked for.
     */
    @Test
    void springBootWarStaysOnItsMainClass() throws IOException {
        Path app = warModule("app", "12.1.13");
        packagedJarNaming(app, "com.example.Application");

        assertEquals(MainClassRuntime.NAME, runtimeOf(app).name());
    }

    /**
     * The other half of the ordering: a WAR project is entitled to carry some
     * unrelated main method, and that must not be taken for an entry point.
     */
    @Test
    void strayMainMethodDoesNotBeatTheServerPlugin() throws IOException {
        Path app = warModule("app", "12.1.13");
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

    /**
     * A pom that pins these wins over the command line, so the developer has to
     * be told rather than left to wonder why the app keeps redeploying itself.
     */
    @Test
    void pinnedScanAndDeployModeAreWarnedAbout() throws IOException {
        Path app = warModule("app", "12.1.13",
                Map.of("scan", "2", "deployMode", "FORK"));

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
        Path app = warModule("app", "12.1.13", Map.of("scan", "0"));

        assertEquals(List.of(), runtimeOf(app).warnings());
    }

    @Test
    void jettyReportsItIsServing_andWhichPort() throws IOException {
        AppRuntime runtime = runtimeOf(warModule("app", "12.1.13"));
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
        AppRuntime runtime = runtimeOf(warModule("app", "12.1.13"));

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

    /**
     * A pom edit is what moves the answer, and until the next resolve the model
     * describes the project as it was. Believing it then would start the
     * application the way a pom that no longer exists asked for.
     */
    @Test
    void aModelOlderThanThePomIsNotBelieved() throws IOException {
        Path app = warModule("app", "12.1.13");
        Path pom = app.resolve("pom.xml");
        Files.setLastModifiedTime(pom,
                FileTime.fromMillis(Files
                        .getLastModifiedTime(app.resolve(EffectiveModel.FILE))
                        .toMillis() + 2000));

        IOException thrown = assertThrows(IOException.class,
                () -> runtimeOf(app));

        assertTrue(thrown.getMessage().contains("cannot tell how to start"),
                thrown.getMessage());
    }

    /**
     * The model lists what the build runs, so a module with no server plugin in
     * it has none - whatever the pom declares in a {@code <pluginManagement>}
     * or in a profile that never ran.
     */
    @Test
    void aModelWithoutAServerPluginMeansThereIsNone() throws IOException {
        Path app = module("app", "war",
                "<build><pluginManagement><plugins>"
                        + pluginElement("org.eclipse.jetty.ee10",
                                "jetty-ee10-maven-plugin", "12.1.13", "")
                        + "</plugins></pluginManagement></build>");
        writeModel(app, "org.apache.maven.plugins:maven-compiler-plugin:3.13.0",
                Map.of());

        IOException thrown = assertThrows(IOException.class,
                () -> runtimeOf(app));

        assertTrue(thrown.getMessage().contains("cannot tell how to start"),
                thrown.getMessage());
    }

    /**
     * {@code -Dvaadin.dev.runtime} names the plugin to run, and the build is
     * still what says whether it runs one: forcing a runtime the project does
     * not build with would otherwise fail inside Maven, with a message about a
     * goal rather than about the project.
     */
    @Test
    void aForcedRuntimeTheBuildDoesNotRunIsRefused() throws IOException {
        Path app = warModule("app", "12.1.13");

        System.setProperty("vaadin.dev.runtime", "jetty-ee11");
        try {
            IOException thrown = assertThrows(IOException.class,
                    () -> runtimeOf(app));
            assertTrue(thrown.getMessage().contains("jetty-ee11-maven-plugin"),
                    thrown.getMessage());
        } finally {
            System.clearProperty("vaadin.dev.runtime");
        }
    }

    /** One module's model, as the build extension leaves it. */
    private void writeModel(Path app, String coordinates,
            Map<String, String> configuration) throws IOException {
        Properties model = new Properties();
        model.setProperty("packaging", "war");
        model.setProperty("plugins", "1");
        model.setProperty("plugin.0", coordinates);
        configuration.forEach(
                (name, value) -> model.setProperty("plugin.0." + name, value));
        Path file = app.resolve(EffectiveModel.FILE);
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file)) {
            model.store(writer, "test fixture");
        }
    }

    /**
     * Deciding costs a scan of the module's output, and the answer is quoted in
     * the log, so it is worth keeping - as long as it is dropped when the thing
     * it was read out of is.
     */
    @Test
    void theRuntimeIsDecidedOncePerReactor() throws IOException {
        Launch launch = launchFor(warModule("app", "12.1.13"));

        AppRuntime first = launch.runtime();

        assertSame(first, launch.runtime());
    }

    /**
     * A pom edit can move the answer - to another EE level, to another plugin
     * version, or to an application with an entry point of its own - and the
     * daemon outlives the edit. Before this, the launch decision was made once
     * and the application went on being started the way the poms read when the
     * daemon came up.
     */
    @Test
    void aPomEditReDecidesHowTheApplicationStarts() throws IOException {
        Path app = warModule("app", "12.1.13");
        Launch launch = launchFor(app);
        assertEquals("jetty-ee10", launch.runtime().name());

        // The same module, as a build of its edited pom would leave it.
        writeModel(app,
                "org.eclipse.jetty.ee11:jetty-ee11-maven-plugin:12.1.13",
                Map.of());
        launch.rereadReactor(log);

        assertEquals("jetty-ee11", launch.runtime().name());
    }

    /**
     * A pom edit that drops the plugin leaves nothing to start the app with.
     */
    @Test
    void aPomEditThatRemovesTheServerPluginIsReportedRatherThanCached()
            throws IOException {
        Path app = warModule("app", "12.1.13");
        Launch launch = launchFor(app);
        assertEquals("jetty-ee10", launch.runtime().name());

        Files.delete(app.resolve(EffectiveModel.FILE));
        launch.rereadReactor(log);

        IOException thrown = assertThrows(IOException.class, launch::runtime);
        assertTrue(thrown.getMessage().contains("cannot tell how to start"),
                thrown.getMessage());
    }

    private Launch launchFor(Path app) {
        return new Launch(Reactor.discover(app, log), log);
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
    private Path warModule(String name, String version) throws IOException {
        return warModule(name, version, Map.of());
    }

    /**
     * A WAR module, with the model a build of it would have left behind: the
     * pom is where its packaging comes from, and the model is where everything
     * about the server plugin does.
     */
    private Path warModule(String name, String version,
            Map<String, String> configuration) throws IOException {
        Path app = module(name, "war", "");
        writeModel(app,
                "org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:" + version,
                configuration);
        return app;
    }

    /** The same at the other Jakarta EE level. */
    private Path ee11Module(String name, String version) throws IOException {
        Path app = module(name, "war", "");
        writeModel(app,
                "org.eclipse.jetty.ee11:jetty-ee11-maven-plugin:" + version,
                Map.of());
        return app;
    }

    private static String plugin(String groupId, String artifactId,
            String version, String configuration) {
        return "<build><plugins>"
                + pluginElement(groupId, artifactId, version, configuration)
                + "</plugins></build>";
    }

    /** One {@code <plugin>}, for a pom that puts it somewhere of its own. */
    private static String pluginElement(String groupId, String artifactId,
            String version, String configuration) {
        return "<plugin>" + "<groupId>" + groupId + "</groupId><artifactId>"
                + artifactId + "</artifactId><version>" + version + "</version>"
                + configuration + "</plugin>";
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
