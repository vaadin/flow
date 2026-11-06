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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Classpath membership is what decides whether a pom edit restarts the app.
 * Maven reorders a classpath for edits that change nothing about what is on it,
 * and restarting an app to hand it the same jars in a different sequence is
 * noise a developer cannot tell from a whim.
 */
class LaunchTest {

    private final List<String> properties = new ArrayList<>();

    @AfterEach
    void clearProperties() {
        properties.forEach(System::clearProperty);
    }

    @Test
    void membership_ignoresOrder() {
        assertEquals(Launch.membership(classpath("b.jar", "a.jar")),
                Launch.membership(classpath("a.jar", "b.jar")));
    }

    @Test
    void membership_ignoresDuplicates() {
        assertEquals(Launch.membership(classpath("a.jar")),
                Launch.membership(classpath("a.jar", "a.jar")));
    }

    @Test
    void membership_seesAnAddedEntry() {
        assertNotEquals(Launch.membership(classpath("a.jar")),
                Launch.membership(classpath("a.jar", "b.jar")));
    }

    @Test
    void forwardedToApp_carriesTheVaadinAndSpringNamespaces() {
        // VAADIN_DEV_DAEMON_OPTS is the only channel there is - the app is
        // launched with no program arguments - so anything a developer has to
        // set on the application has to come through here.
        assertTrue(Launch.forwardedToApp("spring.profiles.active"));
        assertTrue(Launch.forwardedToApp("spring.datasource.url"));
        assertTrue(Launch.forwardedToApp("spring.main.banner-mode"));
        // The steering case this allowlist already existed for.
        assertTrue(Launch.forwardedToApp("vaadin.frontend.hotdeploy"));
    }

    @Test
    void forwardedToApp_holdsBackWhatTheLoopItselfSets() {
        // These four are put on the app's command line with the value the loop
        // requires. The forwarding runs after them and a later -D wins, so a
        // forwarded copy does not merely duplicate - it overrides. For
        // devtools that would put Spring's own restart back in the ring
        // against the daemon, which is the one thing the loop cannot share.
        assertFalse(Launch.forwardedToApp("spring.devtools.restart.enabled"));
        assertFalse(Launch.forwardedToApp("vaadin.launch-browser"));
        assertFalse(Launch.forwardedToApp("vaadin.devloop.classes"));
        // And without this one, a developer who happened to set
        // VAADIN_DEV_DAEMON_OPTS="-Dvaadin.devloop.launch=apply" would have
        // every launch reported as an escalated apply.
        assertFalse(Launch.forwardedToApp("vaadin.devloop.launch"));
    }

    @Test
    void forwardedToApp_leavesTheDaemonsOwnJvmPropertiesBehind() {
        // Not "forward everything": these describe the daemon's process, and
        // another process's answers are worse than none.
        assertFalse(Launch.forwardedToApp("user.dir"));
        assertFalse(Launch.forwardedToApp("java.class.path"));
        assertFalse(Launch.forwardedToApp("os.name"));
    }

    @Test
    void forwardedToApp_holdsBackTheJvmArgsProperty() {
        // The app gets the flags themselves; a -D copy of the raw value would
        // only carry its | separators into MAVEN_OPTS.
        assertFalse(Launch.forwardedToApp(Launch.JVM_ARGS_PROPERTY));
    }

    @Test
    void configuredJvmFlags_isEmptyWhenUnset() {
        assertEquals(List.of(), Launch.readConfiguredJvmFlags());
        setProperty(Launch.JVM_ARGS_PROPERTY, "  ");
        assertEquals(List.of(), Launch.readConfiguredJvmFlags());
    }

    @Test
    void configuredJvmFlags_splitsOnWhitespace() {
        setProperty(Launch.JVM_ARGS_PROPERTY,
                " --add-exports java.base/jdk.internal.misc=ALL-UNNAMED\t-Xmx2g ");
        assertEquals(
                List.of("--add-exports",
                        "java.base/jdk.internal.misc=ALL-UNNAMED", "-Xmx2g"),
                Launch.readConfiguredJvmFlags());
    }

    @Test
    void configuredJvmFlags_splitsOnPipe() {
        // The CLI splits VAADIN_DEV_DAEMON_OPTS on whitespace without honouring
        // quotes, so through it several flags can only be joined with |. A
        // comma has to survive: it separates --add-exports targets.
        setProperty(Launch.JVM_ARGS_PROPERTY,
                "-Xmx2g|--add-exports=java.base/jdk.internal.misc=a,b||-XX:+UseZGC|");
        assertEquals(List.of("-Xmx2g",
                "--add-exports=java.base/jdk.internal.misc=a,b", "-XX:+UseZGC"),
                Launch.readConfiguredJvmFlags());
    }

    private static String classpath(String... entries) {
        return String.join(File.pathSeparator, entries);
    }

    @Test
    void projectIfResolved_isEmptyUntilOneHasBeenResolved(@TempDir Path repo)
            throws IOException {
        // The baseline an application's registration builds is taken from this,
        // on the thread answering that registration - so "nothing resolved
        // yet" has to be an answer it can give rather than a Maven run it
        // sets off. A caller that gets nothing here leaves the baseline to the
        // first apply, which is where resolving belongs.
        Files.createDirectories(
                repo.resolve("src").resolve("main").resolve("java"));
        Files.writeString(repo.resolve("pom.xml"), """
                <project>
                  <artifactId>app</artifactId>
                  <packaging>jar</packaging>
                </project>
                """);
        Launch launch = new Launch(Reactor.discover(repo, text -> {
        }), text -> {
        });

        assertTrue(launch.projectIfResolved().isEmpty());
        // Nor is anything launched, which leaves a classpath nothing can have
        // drifted from - answered without resolving either.
        assertTrue(launch.classpathDrift().isEmpty());
    }

    /**
     * Maven 4 resolves a module of a build aggregating the reactor from above
     * to its {@code target/classes}, so a directory on the classpath is not
     * necessarily one of the reactor's own modules - while a module the reactor
     * aggregates from beside its root still is, and so is one under the root
     * that the pom walk did not enumerate.
     */
    @Test
    void selectModules_takesOnlyTheReactorsOwnModules(@TempDir Path workspace)
            throws IOException {
        Path root = reactorFixture(workspace);
        List<String> log = new ArrayList<>();

        assertEquals(List.of("app", "lib", "unlisted", "shared"),
                selectedFromFixture(root.resolve("app"), workspace, log));
        assertEquals(1, log.size(), log::toString);
        assertTrue(
                log.get(0).startsWith("module "
                        + Reactor.real(workspace.resolve("outside"))
                        + " is on the classpath but not part of reactor"),
                log::toString);
    }

    /**
     * When discovery finds no aggregator the reactor is the application alone,
     * which says nothing about which classpath modules belong to the build, so
     * every one Maven put there stays in the loop.
     */
    @Test
    void selectModules_reactorOfOne_keepsEveryClasspathModule(
            @TempDir Path workspace) throws IOException {
        reactorFixture(workspace);
        Path app = module(workspace.resolve("standalone"), "standalone");

        assertEquals(
                List.of("standalone", "lib", "unlisted", "shared", "outside"),
                selectedFromFixture(app, workspace, new ArrayList<>()));
    }

    /**
     * The candidates and the root are resolved paths, so a classpath entry
     * spelled through a symlink must be resolved before it is matched.
     */
    @Test
    void selectModules_resolvesSymlinkedClasspathEntries(
            @TempDir Path workspace) throws IOException {
        Path root = reactorFixture(workspace);
        Path alias = Files.createSymbolicLink(workspace.resolve("alias"),
                workspace);

        assertEquals(List.of("app", "lib", "unlisted", "shared"),
                selectedFromFixture(root.resolve("app"), alias,
                        new ArrayList<>()));
    }

    /**
     * A reactor aggregating {@code app}, {@code lib} and {@code ../shared},
     * plus {@code unlisted} under its root but in no {@code <modules>}, and
     * {@code outside} beside it and aggregated by nothing.
     */
    private static Path reactorFixture(Path workspace) throws IOException {
        Path root = workspace.resolve("fixture");
        Files.createDirectories(root);
        Files.writeString(root.resolve("pom.xml"), """
                <project>
                  <artifactId>fixture</artifactId>
                  <packaging>pom</packaging>
                  <modules>
                    <module>app</module>
                    <module>lib</module>
                    <module>../shared</module>
                  </modules>
                </project>
                """);
        module(root.resolve("app"), "app");
        module(root.resolve("lib"), "lib");
        module(root.resolve("unlisted"), "unlisted");
        module(workspace.resolve("shared"), "shared");
        module(workspace.resolve("outside"), "outside");
        return root;
    }

    /**
     * The modules selected for {@code app} from the fixture's output
     * directories, with the classpath entries spelled under {@code entryBase}
     * instead of the real workspace and the launch's log lines added to
     * {@code log}.
     */
    private static List<String> selectedFromFixture(Path app, Path entryBase,
            List<String> log) {
        Launch launch = new Launch(Reactor.discover(app, text -> {
        }), log::add);
        List<String> entries = Stream
                .of("fixture/lib", "fixture/unlisted", "shared", "outside")
                .map(dir -> entryBase.resolve(dir).resolve("target")
                        .resolve("classes").toString())
                .toList();
        return launch.selectModules(entries).stream()
                .map(Reactor.Module::artifactId).toList();
    }

    private static Path module(Path dir, String artifactId) throws IOException {
        Files.createDirectories(
                dir.resolve("src").resolve("main").resolve("java"));
        Files.createDirectories(dir.resolve("target").resolve("classes"));
        Files.writeString(dir.resolve("pom.xml"), """
                <project>
                  <artifactId>%s</artifactId>
                  <packaging>jar</packaging>
                </project>
                """.formatted(artifactId));
        return dir;
    }

    /**
     * {@code disabledPlugins} only ever reached the system class loader: every
     * other loader's configuration re-reads the empty {@code disabledPlugins=}
     * bundled in HotswapAgent's jar, which shadows the system property. So a
     * container's own loaders kept JacksonPlugin, and its patch deadlocked
     * Payara Micro's boot against Hazelcast's bootstrap. The global key is the
     * one every loader consults.
     */
    @Test
    void jvmFlags_disableHotswapAgentPluginsForEveryClassLoader(
            @TempDir Path repo) throws IOException {
        List<String> flags = launchWithAgents(repo).jvmFlags(text -> {
        });

        String prefix = "-Dhotswapagent.disablePlugin=";
        List<String> disabled = flags.stream()
                .filter(flag -> flag.startsWith(prefix))
                .flatMap(flag -> Stream
                        .of(flag.substring(prefix.length()).split(",")))
                .toList();
        assertTrue(disabled.contains("JacksonPlugin"), flags.toString());
        assertTrue(disabled.contains("Vaadin"), flags.toString());
        assertTrue(
                flags.stream().noneMatch(
                        flag -> flag.startsWith("-DdisabledPlugins=")),
                flags.toString());
    }

    /**
     * The developer's flags come after the loop's and the runtime's, the
     * property's before those of {@code --jvm-args}, because the JVM lets the
     * last of a repeated flag win.
     */
    @Test
    void composeJvmFlags_putsTheDevelopersFlagsLast(@TempDir Path repo)
            throws IOException {
        Launch launch = launchWithAgents(repo);
        setProperty(Launch.JVM_ARGS_PROPERTY, "-Xmx1g|-Dfrom=property");
        launch.requestJvmFlags(List.of("-Xmx2g", "-Dfrom=command-line"));

        List<String> flags = launch.composeJvmFlags(text -> {
        }, List.of("-Druntime=extra"));

        List<String> own = launch.jvmFlags(text -> {
        });
        assertEquals(own, flags.subList(0, own.size()), flags.toString());
        assertEquals(
                List.of("-Druntime=extra", "-Xmx1g", "-Dfrom=property",
                        "-Xmx2g", "-Dfrom=command-line"),
                flags.subList(own.size(), flags.size()));
    }

    private Launch launchWithAgents(Path repo) throws IOException {
        Files.writeString(repo.resolve("pom.xml"), """
                <project>
                  <artifactId>app</artifactId>
                  <packaging>jar</packaging>
                </project>
                """);
        Path jar = Files.writeString(repo.resolve("ha.jar"), "");
        setProperty(HotswapAgentJar.OVERRIDE_PROPERTY, jar.toString());
        setProperty("vaadin.dev.agentJar", jar.toString());
        setProperty("vaadin.dev.javaHome", System.getProperty("java.home"));
        return new Launch(Reactor.discover(repo, text -> {
        }), text -> {
        });
    }

    private void setProperty(String name, String value) {
        properties.add(name);
        System.setProperty(name, value);
    }
}
