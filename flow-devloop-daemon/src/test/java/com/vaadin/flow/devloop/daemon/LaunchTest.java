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
        Launch launch = new Launch(Reactor.discover(repo, text -> {
        }), text -> {
        });

        List<String> flags = launch.jvmFlags(text -> {
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

    private void setProperty(String name, String value) {
        properties.add(name);
        System.setProperty(name, value);
    }
}
