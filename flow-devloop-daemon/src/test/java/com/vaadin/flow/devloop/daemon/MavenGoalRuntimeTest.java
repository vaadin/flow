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
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.devloop.mavenext.DevLoopBuildExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Under a build-plugin runtime the application JVM is Maven's own, so
 * {@code MAVEN_OPTS} is the only way in - and it is also where the developer's
 * project already keeps the flags its build needs. What goes in it is therefore
 * worth pinning down. The command line is composed here from a project on disk;
 * that Maven then starts the server from it needs a real project and is covered
 * by {@code flow-tests/test-devloop}.
 */
class MavenGoalRuntimeTest {

    private static final List<String> NEEDED = List.of("-javaagent:/ha.jar",
            "-XX:+AllowEnhancedClassRedefinition");

    private static final String JETTY = "org.eclipse.jetty.ee10:"
            + "jetty-ee10-maven-plugin:12.1.13";

    private static final String WILDFLY = "org.wildfly.plugins:"
            + "wildfly-maven-plugin:5.1.5.Final";

    private static final String TOMEE = "org.apache.tomee.maven:"
            + "tomee-maven-plugin:10.1.2";

    private static final String PAYARA = "fish.payara.maven.plugins:"
            + "payara-server-maven-plugin:1.3.0";

    private static final String CARGO = "org.codehaus.cargo:"
            + "cargo-maven3-plugin:1.10.29";

    private static final String LIBERTY = "io.openliberty.tools:"
            + "liberty-maven-plugin:3.12.3";

    @TempDir
    private Path repo;

    private final List<String> logged = new ArrayList<>();

    private final Launch.Log log = logged::add;

    private final List<String> properties = new ArrayList<>();

    @AfterEach
    void clearProperties() {
        properties.forEach(System::clearProperty);
    }

    /**
     * Without the extension the skip cannot be switched back on for the
     * application's module, so sending it would start nothing at all. What must
     * not go with it is the rest of the map: Payara Micro's {@code deployWar}
     * defaults to {@code false}, so a launch that dropped it brought up a Micro
     * with the application deployed in it nowhere - on a single-module project
     * too, where the skip was never needed at all.
     */
    @Test
    void goalProperties_withoutTheExtension_dropTheSkipAndNothingElse() {
        ServerPlugin micro = entry("payara-micro");

        assertEquals(Map.of("payara.deploy.war", "true"),
                MavenGoalRuntime.goalProperties(micro, false));
        assertEquals(Map.of("payara.skip", "true", "payara.deploy.war", "true"),
                MavenGoalRuntime.goalProperties(micro, true));
    }

    /**
     * An entry that switches nothing off has no half to drop, so the map goes
     * out whole either way.
     */
    @Test
    void goalProperties_anEntryThatSwitchesNothingOffKeepsItsWholeMap() {
        for (ServerPlugin plugin : ServerPlugin.KNOWN) {
            if (plugin.skippedOutsideTheApplication()) {
                continue;
            }
            assertEquals(plugin.goalProperties(),
                    MavenGoalRuntime.goalProperties(plugin, false),
                    plugin.name());
        }
    }

    /**
     * And the skip that is dropped is named by the table rather than guessed at
     * here: every entry that switches its goal off has exactly one, under
     * whatever prefix its own plugin gives it.
     */
    @Test
    void everyEntryThatSwitchesItsGoalOffNamesTheProperty() {
        for (ServerPlugin plugin : ServerPlugin.KNOWN) {
            assertEquals(plugin.skippedOutsideTheApplication(),
                    plugin.skipProperty().isPresent(), plugin.name());
            plugin.skipProperty()
                    .ifPresent(name -> assertTrue(
                            plugin.goalProperties().containsKey(name),
                            plugin.name()));
        }
    }

    /**
     * A {@code -D} is a Maven user property, and an un-prefixed name belongs to
     * nobody: {@code looseApplication} reaches every plugin in every module of
     * the reactor. The extension can write it into the application module's own
     * configuration instead, so with one there is nothing to broadcast - the
     * same ground Liberty's entry already declines to send {@code embedded} on.
     */
    @Test
    void goalProperties_anUnprefixedSettingTheExtensionCanForceIsNotSent() {
        ServerPlugin liberty = entry("liberty");

        assertEquals(Map.of(), MavenGoalRuntime.goalProperties(liberty, true));
        // Without one it is the only lever there is, and a degraded run beats
        // a rescanner competing with every apply.
        assertEquals(Map.of("looseApplication", "false"),
                MavenGoalRuntime.goalProperties(liberty, false));
    }

    /**
     * The skip is the exception. It is switched on across the whole reactor and
     * off again for one module, so the forced {@code <skip>false</skip>} is its
     * other half rather than its replacement - and Payara Server's parameter is
     * declared {@code ${skip}} under no other name.
     */
    @Test
    void goalProperties_theSkipIsSentEvenWithNoPrefix() {
        assertEquals(Map.of("skip", "true"),
                MavenGoalRuntime.goalProperties(entry("payara"), true));
    }

    /** And nothing else in the table goes out without a prefix. */
    @Test
    void goalProperties_nothingElseIsBroadcastAcrossTheReactor() {
        for (ServerPlugin plugin : ServerPlugin.KNOWN) {
            for (String name : MavenGoalRuntime.goalProperties(plugin, true)
                    .keySet()) {
                assertTrue(
                        name.contains(".") || plugin.skipProperty()
                                .filter(name::equals).isPresent(),
                        plugin.name() + " sends " + name
                                + " to every plugin in the reactor");
            }
        }
    }

    private static ServerPlugin entry(String name) {
        return ServerPlugin.KNOWN.stream()
                .filter(plugin -> name.equals(plugin.name())).findFirst()
                .orElseThrow();
    }

    @Test
    void mavenOpts_inheritedValueIsKept() {
        String opts = MavenGoalRuntime.mavenOpts("-Xmx2g -Dhttps.proxyPort=80",
                NEEDED);

        assertTrue(opts.contains("-Xmx2g"), opts);
        assertTrue(opts.contains("-Dhttps.proxyPort=80"), opts);
        assertTrue(opts.contains("-javaagent:/ha.jar"), opts);
    }

    /**
     * The JVM lets the later of two conflicting flags win, so the loop's own
     * have to come last: a project may set a heap size, but it must not be able
     * to turn off the redefinition the loop depends on.
     */
    @Test
    void mavenOpts_theLoopsFlagsComeLast() {
        String opts = MavenGoalRuntime.mavenOpts("-Xmx2g", NEEDED);

        assertEquals("-Xmx2g -javaagent:/ha.jar "
                + "-XX:+AllowEnhancedClassRedefinition", opts);
    }

    @Test
    void mavenOpts_nothingInherited_isJustTheLoopsFlags() {
        String expected = "-javaagent:/ha.jar "
                + "-XX:+AllowEnhancedClassRedefinition";

        assertEquals(expected, MavenGoalRuntime.mavenOpts(null, NEEDED));
        assertEquals(expected, MavenGoalRuntime.mavenOpts("   ", NEEDED));
    }

    /**
     * WildFly sorts module options apart from the rest before it builds the
     * server's command line, and the two-token form comes apart in the sorting:
     * measured, every {@code --add-opens} arrived ahead of every value and the
     * JVM refused to start with "--add-opens requires modules to be specified".
     */
    @Test
    void singleToken_foldsAModuleOptionOntoItsValue() {
        assertEquals(
                List.of("-javaagent:a.jar",
                        "--add-opens=java.base/java.net=ALL-UNNAMED",
                        "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED",
                        "-Dport=1"),
                MavenGoalRuntime
                        .singleToken(List.of("-javaagent:a.jar", "--add-opens",
                                "java.base/java.net=ALL-UNNAMED", "--add-opens",
                                "java.base/jdk.internal.loader=ALL-UNNAMED",
                                "-Dport=1")));
    }

    /** One already in the {@code =} form is left exactly as it is. */
    @Test
    void singleToken_leavesAnOptionThatAlreadyCarriesItsValue() {
        List<String> flags = List
                .of("--add-exports=java.base/sun.nio.ch=ALL-UNNAMED", "-Xmx2g");

        assertEquals(flags, MavenGoalRuntime.singleToken(flags));
    }

    /**
     * Maven expands {@code $MAVEN_OPTS} unquoted, so a flag with a space in it
     * reaches the JVM as two broken arguments. Nothing can fix that, so it is
     * named at launch rather than left to fail as a JVM that will not start.
     */
    @Test
    void unsplittable_namesTheFlagWithASpaceAndNoOther() {
        List<String> warnings = MavenGoalRuntime.unsplittable(List.of(
                "-javaagent:/opt/ha.jar", "-javaagent:/Program Files/ha.jar"));

        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("/Program Files/ha.jar"),
                warnings.get(0));
        assertTrue(warnings.get(0).contains("MAVEN_OPTS"), warnings.get(0));
    }

    /**
     * And for a forked server it is not MAVEN_OPTS that splits them. Naming it
     * anyway would send the developer looking in a variable the launch no
     * longer puts these flags in at all.
     */
    @Test
    void unsplittable_namesTheChannelTheForkedServerReadsFrom() {
        List<String> warnings = MavenGoalRuntime.unsplittable(
                List.of("-javaagent:/Program Files/ha.jar"),
                MavenGoalRuntime.splitter(entry("wildfly")));

        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("wildfly.javaOpts"),
                warnings.get(0));
        assertFalse(warnings.get(0).contains("MAVEN_OPTS"), warnings.get(0));
    }

    @Test
    void unsplittable_saysNothingWhenEveryFlagSurvives() {
        assertEquals(List.of(), MavenGoalRuntime.unsplittable(NEEDED));
    }

    /**
     * Payara Server's channel is a {@code List<String>}, which Maven splits on
     * commas before the plugin sees it - and the plugin then drops every piece
     * with no {@code =} in it, so a comma does not divide a flag, it deletes
     * most of it. The comma-bearing flags go to an argument file, which the JVM
     * expands itself, and the rest stay inline where the launch line shows
     * them.
     */
    @Test
    void withCommasInArgFile_onlyTheCommaBearingFlagsAreMoved(@TempDir Path dir)
            throws IOException {
        Path file = dir.resolve("payara-args.txt");

        List<String> passed = MavenGoalRuntime
                .withCommasInArgFile(List.of("-javaagent:/ha.jar",
                        "-DdisabledPlugins=Vaadin,Spring,SpringBoot,Jetty",
                        "-XX:+AllowEnhancedClassRedefinition"), file);

        assertEquals(
                List.of("-javaagent:/ha.jar",
                        "-XX:+AllowEnhancedClassRedefinition", "@" + file),
                passed);
        assertEquals("\"-DdisabledPlugins=Vaadin,Spring,SpringBoot,Jetty\"\n",
                Files.readString(file));
    }

    /**
     * Liberty's channel carries one flag per property, each becoming one line
     * of the server's generated jvm.options - so a line holding all of them
     * would reach the JVM as a single argument. The key is the flag's position,
     * which only has to make the names distinct.
     */
    @Test
    void flagProperties_oneSettingPerFlag() {
        assertEquals(List.of("-Dliberty.jvm.devloop0=-javaagent:/ha.jar",
                "-Dliberty.jvm.devloop1="
                        + "-XX:+AllowEnhancedClassRedefinition",
                "-Dliberty.jvm.devloop2=-DdisabledPlugins=Vaadin,Spring"),
                MavenGoalRuntime.flagProperties("liberty.jvm.devloop",
                        List.of("-javaagent:/ha.jar",
                                "-XX:+AllowEnhancedClassRedefinition",
                                "-DdisabledPlugins=Vaadin,Spring")));
    }

    /**
     * And nothing is written when nothing needs it, so a channel of this shape
     * costs a launch with no comma in it neither a file nor a token.
     */
    @Test
    void withCommasInArgFile_noCommaLeavesTheFlagsAndTheDiskAlone(
            @TempDir Path dir) throws IOException {
        Path file = dir.resolve("payara-args.txt");

        assertEquals(NEEDED,
                MavenGoalRuntime.withCommasInArgFile(NEEDED, file));
        assertFalse(Files.exists(file));
    }

    /**
     * An embedded server is Maven's own JVM, so the loop's flags ride in
     * {@code MAVEN_OPTS} and the system properties go on the command line, each
     * as an argument of its own. Its webapp loader is where HotswapAgent has to
     * find itself, which is what the extra class path is for.
     */
    @Test
    void invocation_embeddedServer_flagsGoToMavensOwnJvm() throws IOException {
        Path agent = Files.writeString(repo.resolve("ha.jar"), "");
        setProperty(HotswapAgentJar.OVERRIDE_PROPERTY, agent.toString());
        Launch launch = launchOf(module("jetty-app", JETTY));

        AppRuntime.Invocation invocation = runtimeOf(launch).invocation(
                projectOf(launch), NEEDED, List.of("-Dvaadin.x=a b"));

        List<String> command = invocation.command();
        assertTrue(command.contains("-Dvaadin.x=a b"), command.toString());
        assertTrue(command.contains(JETTY + ":run"), command.toString());
        // A module of its own compiles nothing for a sibling, so there is no
        // phase to name.
        assertFalse(command.contains("compile"), command.toString());
        assertTrue(invocation.environment().get("MAVEN_OPTS")
                .endsWith(String.join(" ", NEEDED)));
        assertTrue(hotswapAgentProperties(launch)
                .contains("extraClasspath=" + agent.toUri()));
    }

    /**
     * A forked server takes its flags from one plugin parameter, so they are
     * folded into one setting with every module option held together with its
     * value, and none of them go to Maven's own JVM. In a reactor the WAR the
     * server deploys has to be packaged, so the phase is named.
     */
    @Test
    void invocation_forkedServerInAReactor_packsTheFlagsIntoItsParameter()
            throws IOException {
        Launch launch = launchOf(moduleInAReactor(WILDFLY));

        AppRuntime.Invocation invocation = runtimeOf(launch).invocation(
                projectOf(launch),
                List.of("-javaagent:/ha.jar", "--add-opens",
                        "java.base/java.lang=ALL-UNNAMED"),
                List.of("-Dvaadin.x=1"));

        List<String> command = invocation.command();
        int modules = command.indexOf("-pl");
        assertEquals(List.of("-pl", ":app", "-am"),
                command.subList(modules, modules + 3));
        assertTrue(command.contains("package"), command.toString());
        assertTrue(command.contains(WILDFLY + ":run"), command.toString());
        assertTrue(command.contains("-Dwildfly.javaOpts=-javaagent:/ha.jar "
                + "--add-opens=java.base/java.lang=ALL-UNNAMED -Dvaadin.x=1"),
                command.toString());
        // Without the extension the skip cannot be undone for the application
        // module, so it is not sent at all.
        assertFalse(command.contains("-Dwildfly.skip=true"),
                command.toString());
        assertFalse(invocation.environment().get("MAVEN_OPTS")
                .contains("-javaagent:/ha.jar"));
        assertEquals(Reactor.real(repo).toString(),
                invocation.environment().get("MAVEN_BASEDIR"));
        // A forked server that finds no java configured runs the first one on
        // the PATH, so the chosen JVM has to be that one.
        assertTrue(
                invocation.environment().get("PATH")
                        .startsWith(launch.appJvm().home().resolve("bin")
                                + File.pathSeparator),
                invocation.environment().get("PATH"));
        assertFalse(hotswapAgentProperties(launch).contains("extraClasspath"));
    }

    /**
     * Neither of the embedded server's extras applies to a forked one, which
     * may also have to build a server before it can start it - and a flag the
     * parameter would split is named for that parameter, not for
     * {@code MAVEN_OPTS}.
     */
    @Test
    void forkedServer_hasNoEmbeddedExtrasAndNamesItsOwnSplitter()
            throws IOException {
        MavenGoalRuntime runtime = runtimeOf(
                launchOf(module("wildfly-app", WILDFLY)));

        assertEquals(List.of(), runtime.extraJvmFlags());
        assertEquals(Duration.ofMinutes(20), runtime.startupTimeout());
        List<String> warnings = runtime
                .warnings(List.of("-javaagent:/First Last/ha.jar"));
        assertTrue(warnings.get(0).contains("wildfly.javaOpts"),
                warnings.toString());
    }

    /**
     * TomEE's goal has no skip, so without the extension to bind it in the
     * application's module alone it would start a server in the reactor root
     * and block the build there. Saying so beats a start that hangs.
     */
    @Test
    void invocation_boundEntryWithoutTheExtension_isRefused()
            throws IOException {
        Launch launch = launchOf(moduleInAReactor(TOMEE));
        MavenGoalRuntime runtime = runtimeOf(launch);
        Launch.Project project = projectOf(launch);

        IOException refused = assertThrows(IOException.class,
                () -> runtime.invocation(project, NEEDED, List.of()));

        assertTrue(refused.getMessage().contains("cannot be kept to app"),
                refused.getMessage());
    }

    /**
     * With the extension the goal is bound to the phase rather than named, and
     * TomEE unescapes what it is handed, so a Windows path has its backslashes
     * doubled to arrive whole.
     */
    @Test
    void invocation_boundEntryWithTheExtension_bindsTheGoalInsteadOfNamingIt()
            throws IOException {
        Path extension = Files.writeString(repo.resolve("devloop.jar"), "");
        setProperty("vaadin.dev.agentJar", extension.toString());
        Launch launch = launchOf(moduleInAReactor(TOMEE));

        List<String> command = runtimeOf(launch).invocation(projectOf(launch),
                List.of("-javaagent:C:\\ha.jar"), List.of()).command();

        assertTrue(command.contains("-Dmaven.ext.class.path=" + extension),
                command.toString());
        assertTrue(command.contains(
                "-D" + DevLoopBuildExtension.BIND_PROPERTY + "=package:run"),
                command.toString());
        assertFalse(command.contains(TOMEE + ":run"), command.toString());
        assertTrue(
                command.contains("-Dtomee-plugin.args=-javaagent:C:\\\\ha.jar"),
                command.toString());
    }

    /**
     * Liberty's goal, named, runs in the sibling modules too and replaces each
     * one's jar with its target/classes before the WAR is packaged, so in a
     * reactor it is bound to package in the application module instead.
     */
    @Test
    void invocation_libertyInAReactor_bindsRunAfterThePackagedWar()
            throws IOException {
        Path extension = Files.writeString(repo.resolve("devloop.jar"), "");
        setProperty("vaadin.dev.agentJar", extension.toString());
        Launch launch = launchOf(moduleInAReactor(LIBERTY));

        List<String> command = runtimeOf(launch)
                .invocation(projectOf(launch), NEEDED, List.of()).command();

        assertTrue(command.contains("package"), command.toString());
        assertTrue(command.contains(
                "-D" + DevLoopBuildExtension.BIND_PROPERTY + "=package:run"),
                command.toString());
        assertFalse(
                command.stream().anyMatch(
                        arg -> arg.startsWith("io.openliberty.tools:")),
                command.toString());
    }

    /**
     * Payara splits its parameter on commas too, so a flag holding one goes to
     * an argument file the parameter names instead, and the log says where.
     */
    @Test
    void invocation_commaSplitParameter_movesACommaFlagToAFile()
            throws IOException {
        Launch launch = launchOf(module("payara-app", PAYARA));

        List<String> command = runtimeOf(launch)
                .invocation(projectOf(launch),
                        List.of("-javaagent:/ha.jar", "-Dlist=x,y"), List.of())
                .command();

        Path file = Launch.workDir(launch.reactor().app().dir())
                .resolve("payara-args.txt");
        assertTrue(command.contains("-Dpayara.javaCommandLineOptions="
                + "-javaagent:/ha.jar @" + file), command.toString());
        assertTrue(Files.readString(file).contains("-Dlist=x,y"));
        assertTrue(
                logged.stream().anyMatch(line -> line.contains(
                        "1 flag(s) with a comma in them go to " + file)),
                logged::toString);
    }

    /**
     * Cargo reads its flags from a project property, which only the extension
     * can set and is asked to by name; Liberty takes one flag per property.
     */
    @Test
    void invocation_eachChannelIsNamedTheWayItsPluginReadsIt()
            throws IOException {
        Launch cargo = launchOf(module("cargo-app", CARGO));
        Launch liberty = launchOf(module("liberty-app", LIBERTY));

        List<String> cargoCommand = runtimeOf(cargo)
                .invocation(projectOf(cargo), NEEDED, List.of()).command();
        List<String> libertyCommand = runtimeOf(liberty)
                .invocation(projectOf(liberty), NEEDED, List.of()).command();

        assertTrue(
                cargoCommand
                        .contains("-D" + DevLoopBuildExtension.PROPERTY_PREFIX
                                + "cargo.jvmargs=" + String.join(" ", NEEDED)),
                cargoCommand.toString());
        assertTrue(libertyCommand.containsAll(List.of(
                "-Dliberty.jvm.devloop0=-javaagent:/ha.jar",
                "-Dliberty.jvm.devloop1=-XX:+AllowEnhancedClassRedefinition")),
                libertyCommand.toString());
    }

    private void setProperty(String name, String value) {
        properties.add(name);
        System.setProperty(name, value);
    }

    private Launch launchOf(Path app) {
        // Named rather than searched for, and the JVM running the test rather
        // than a scan of the machine's JDKs.
        setProperty("vaadin.dev.maven", "mvn");
        setProperty("vaadin.dev.javaHome", System.getProperty("java.home"));
        return new Launch(Reactor.discover(app, log), log);
    }

    private MavenGoalRuntime runtimeOf(Launch launch) throws IOException {
        return (MavenGoalRuntime) AppRuntime.of(launch, log);
    }

    private static Launch.Project projectOf(Launch launch) {
        return new Launch.Project(List.of(launch.reactor().app()), "", Map.of(),
                OptionalInt.empty());
    }

    private static String hotswapAgentProperties(Launch launch)
            throws IOException {
        return Files.readString(launch.reactor().app().classesDir()
                .resolve("hotswap-agent.properties"));
    }

    private Path moduleInAReactor(String coordinates) throws IOException {
        Files.writeString(repo.resolve("pom.xml"), """
                <project>
                  <artifactId>root</artifactId>
                  <packaging>pom</packaging>
                  <modules>
                    <module>app</module>
                  </modules>
                </project>
                """);
        return module("app", coordinates);
    }

    /** A WAR module, with the model a build of it would have left behind. */
    private Path module(String name, String coordinates) throws IOException {
        Path app = repo.resolve(name);
        Files.createDirectories(
                app.resolve("src").resolve("main").resolve("java"));
        Files.createDirectories(app.resolve("target").resolve("classes"));
        Files.writeString(app.resolve("pom.xml"), """
                <project>
                  <artifactId>%s</artifactId>
                  <packaging>war</packaging>
                </project>
                """.formatted(name));
        Properties model = new Properties();
        model.setProperty("packaging", "war");
        model.setProperty("plugins", "1");
        model.setProperty("plugin.0", coordinates);
        Path file = app.resolve(EffectiveModel.FILE);
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file)) {
            model.store(writer, "test fixture");
        }
        return app;
    }
}
