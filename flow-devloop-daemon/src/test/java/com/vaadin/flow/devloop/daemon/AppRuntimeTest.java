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
import java.util.Set;

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
     * WildFly and TomEE are the two forked containers: neither has an embedded
     * mode, so the table entry carries a phase and a JVM-flag parameter that
     * Jetty's does not, and the readiness lines come from another catalogue
     * entirely. The coordinates are the ones the CDI starter declares.
     */
    private static final String WILDFLY = "org.wildfly.plugins:"
            + "wildfly-maven-plugin:5.1.5.Final";

    private static final String TOMEE = "org.apache.tomee.maven:"
            + "tomee-maven-plugin:10.1.2";

    /**
     * Cargo is a plugin that drives containers rather than a container, and it
     * is how the loop runs Apache Tomcat: Apache's own
     * {@code tomcat7-maven-plugin} stopped at Tomcat 7 and
     * {@code javax.servlet} in 2013, so a Jakarta EE application cannot be
     * deployed through it at all.
     */
    private static final String CARGO = "org.codehaus.cargo:"
            + "cargo-maven3-plugin:1.10.29";

    /**
     * Payara ships two runtimes and a project declares one or the other, so
     * there are two entries rather than one. Both fork, both run {@code start}
     * rather than the {@code dev} whose watcher would fight every apply, and
     * they differ in the one thing the table has to know: Payara Server's
     * channel is a {@code List<String>} that Maven splits on commas, while
     * Micro's is read as a plain string.
     */
    private static final String PAYARA = "fish.payara.maven.plugins:"
            + "payara-server-maven-plugin:1.3.0";

    private static final String PAYARA_MICRO = "fish.payara.maven.plugins:"
            + "payara-micro-maven-plugin:2.6.0";

    /**
     * Open Liberty forks like the four above it, and differs in the one thing
     * the table has to know: its channel carries one flag per property rather
     * than all of them in one value. The coordinates are the ones this
     * repository's own CDI suite declares.
     */
    private static final String LIBERTY = "io.openliberty.tools:"
            + "liberty-maven-plugin:3.12.3";

    @Test
    void warWithWildflyPlugin_runsThroughTheBuild() throws IOException {
        Path app = serverModule("wf", WILDFLY, Map.of());

        assertEquals("wildfly", runtimeOf(app).name());
    }

    @Test
    void warWithTomeePlugin_runsThroughTheBuild() throws IOException {
        Path app = serverModule("te", TOMEE, Map.of());

        assertEquals("tomee", runtimeOf(app).name());
    }

    @Test
    void warWithCargoPlugin_runsThroughTheBuild() throws IOException {
        Path app = serverModule("tc", CARGO, Map.of());

        assertEquals("cargo", runtimeOf(app).name());
    }

    /**
     * Cargo's own line rather than the container's, so that one entry answers
     * for every container it drives - and so that a project which sends the
     * container's output to a file still has a readiness signal.
     */
    @Test
    void cargoReportsItIsServing_andWhichPort() throws IOException {
        AppRuntime runtime = runtimeOf(serverModule("tc", CARGO, Map.of()));

        // AbstractLocalContainer.start, whose argument is the name Cargo gives
        // the container it installed.
        String line = "[INFO] Tomcat 10.x started on port [8080]";
        assertTrue(runtime.serving(line));
        assertEquals(OptionalInt.of(8080), runtime.port(line));
    }

    /** The line before it names no port, so it is not the readiness signal. */
    @Test
    void cargoStartingUpIsNotYetServing() throws IOException {
        AppRuntime runtime = runtimeOf(serverModule("tc", CARGO, Map.of()));

        assertFalse(runtime.serving("[INFO] Tomcat 10.x starting..."));
        assertFalse(runtime.serving("[INFO] Tomcat 10.x is stopped"));
    }

    @Test
    void warWithPayaraPlugin_runsThroughTheBuild() throws IOException {
        Path app = serverModule("ps", PAYARA, Map.of());

        assertEquals("payara", runtimeOf(app).name());
    }

    @Test
    void warWithPayaraMicroPlugin_runsThroughTheBuild() throws IOException {
        Path app = serverModule("pm", PAYARA_MICRO, Map.of());

        assertEquals("payara-micro", runtimeOf(app).name());
    }

    /**
     * The plugin's own line rather than the server's, and for a sharper reason
     * than Cargo's: the domain is started with no {@code --verbose}, so the
     * kernel's listener line may go only to the domain's {@code server.log} and
     * never reach the stream the daemon reads. What does reach it is the line
     * the plugin logs once the admin endpoint has answered <em>and</em> the
     * deployment has succeeded, which is a stronger signal than a bound socket.
     */
    @Test
    void payaraReportsItIsServing_andWhichPort() throws IOException {
        AppRuntime runtime = runtimeOf(serverModule("ps", PAYARA, Map.of()));

        String line = "[INFO] devloop-app application deployed successfully :"
                + " http://localhost:8080/";
        assertTrue(runtime.serving(line));
        assertEquals(OptionalInt.of(8080), runtime.port(line));
    }

    /**
     * That message degrades to a portless one when the follow-up call for the
     * application's details fails. Matching it would mean reporting a start as
     * serving on a port nobody had been told.
     */
    @Test
    void payaraWithoutAUrlIsNotTheServingLine() throws IOException {
        AppRuntime runtime = runtimeOf(serverModule("ps", PAYARA, Map.of()));

        assertFalse(runtime.serving(
                "[INFO] devloop-app application deployed successfully."));
    }

    /**
     * Micro logs the kernel's own listener line, and a real one carries ANSI
     * colour - its default logging configuration sets {@code ansiColor=true} -
     * around the level and the logger name, both of which come before the
     * message. Anchoring on the message text is what steps over them.
     */
    @Test
    void payaraMicroReportsItIsServing_andWhichPort() throws IOException {
        AppRuntime runtime = runtimeOf(
                serverModule("pm", PAYARA_MICRO, Map.of()));

        String line = "[2023-11-19T22:05:42.526+0000] [] [\u001b[1;92mINFO"
                + "\u001b[0m] [NCLS-CORE-00101] [javax.enterprise.system.core]"
                + " [tid: _ThreadID=23] [levelValue: 800] Network Listener"
                + " http-listener started in: 49ms - bound to [/0.0.0.0:8080]";
        assertTrue(runtime.serving(line));
        assertEquals(OptionalInt.of(8080), runtime.port(line));
    }

    /**
     * The elapsed time is a {@code long} put through {@code MessageFormat}, so
     * it is grouped by the JVM's locale and reads {@code 5,072ms} in one and
     * {@code 5 072ms} in another. It is stepped over rather than matched, and
     * this is what says so.
     */
    @Test
    void payaraMicroReadsThePortPastAGroupedElapsedTime() throws IOException {
        AppRuntime runtime = runtimeOf(
                serverModule("pm", PAYARA_MICRO, Map.of()));

        assertEquals(OptionalInt.of(8080), runtime.port("Network Listener "
                + "http-listener started in: 5,072ms - bound to [/0.0.0.0:8080]"));
        assertEquals(OptionalInt.of(8080), runtime.port("Network Listener "
                + "http-listener started in: 5 072ms - bound to [/0.0.0.0:8080]"));
    }

    /** And an IPv6 address's own colons are eaten, as WildFly's are. */
    @Test
    void payaraMicroReadsThePortPastAnIpv6Address() throws IOException {
        AppRuntime runtime = runtimeOf(
                serverModule("pm", PAYARA_MICRO, Map.of()));

        assertEquals(OptionalInt.of(8080),
                runtime.port("Network Listener http-listener started in: 3ms"
                        + " - bound to [/0:0:0:0:0:0:0:1:8080]"));
    }

    /**
     * An HTTPS listener logs the very same message, and a project that enabled
     * one would otherwise have the loop send a browser to the wrong port. The
     * name is matched literally so it cannot.
     */
    @Test
    void payaraMicroHttpsListenerIsNotTheServingLine() throws IOException {
        AppRuntime runtime = runtimeOf(
                serverModule("pm", PAYARA_MICRO, Map.of()));

        assertFalse(runtime.serving("Network Listener https-listener started"
                + " in: 1ms - bound to [/0.0.0.0:8181]"));
        // And the line announcing one that is switched off carries a port too.
        assertFalse(runtime.serving("Network listener https-listener on port"
                + " 8443 disabled per domain.xml"));
    }

    @Test
    void warWithLibertyPlugin_runsThroughTheBuild() throws IOException {
        Path app = serverModule("ol", LIBERTY, Map.of());

        assertEquals("liberty", runtimeOf(app).name());
    }

    /**
     * Liberty's own line, logged once the application is installed and
     * reachable - the same stronger-than-a-bound-socket signal Payara Server's
     * entry reads. The message id and the URL are matched and the words between
     * them stepped over, Liberty logging in the JVM's own locale out of a
     * translated message catalogue.
     */
    @Test
    void libertyReportsItIsServing_andWhichPort() throws IOException {
        AppRuntime runtime = runtimeOf(serverModule("ol", LIBERTY, Map.of()));

        String line = "[INFO] [AUDIT   ] CWWKT0016I: Web application available"
                + " (default_host): http://localhost:9080/devloop-app/";
        assertTrue(runtime.serving(line));
        assertEquals(OptionalInt.of(9080), runtime.port(line));
        // And the same for an application deployed at the root context.
        assertEquals(OptionalInt.of(8080),
                runtime.port("CWWKT0016I: Web application available"
                        + " (default_host): http://localhost:8080/"));
    }

    /**
     * The line announcing the application being <em>removed</em> carries the
     * very same URL, and the server-ready line carries no port at all. Reading
     * either as the application serving would send the loop a port nobody was
     * listening on, or none.
     */
    @Test
    void libertyRemovingOrMerelyStartingIsNotTheServingLine()
            throws IOException {
        AppRuntime runtime = runtimeOf(serverModule("ol", LIBERTY, Map.of()));

        assertFalse(runtime.serving("[INFO] [AUDIT   ] CWWKT0017I: Web"
                + " application removed (default_host):"
                + " http://localhost:9080/devloop-app/"));
        assertFalse(runtime.serving("[INFO] [AUDIT   ] CWWKF0011I: The"
                + " defaultServer server is ready to run a smarter planet."
                + " The defaultServer server started in 8.028 seconds."));
    }

    /**
     * Liberty deploys a loose-application XML pointing straight at
     * {@code target/classes} by default, and its own application monitor polls
     * what it deployed - so the server would restart the application under
     * every apply. Unlike {@code jetty.scan} that monitor lives in the
     * project's {@code server.xml}, where nothing in the table can reach it, so
     * the deployment shape is what has to change: a packaged WAR does not
     * change between restarts and leaves the loop in sole charge.
     */
    @Test
    void libertyDeploysAPackagedWar() {
        assertEquals(Map.of("looseApplication", "false"),
                entry("liberty").goalProperties());

        String forced = entry("liberty").forcedConfiguration();
        assertTrue(forced.contains("looseApplication=false"), forced);
        // And an embedded server would run in Maven's JVM, which reads no
        // jvm.options and so would start with none of the loop's agents.
        assertTrue(forced.contains("embedded=false"), forced);
    }

    /**
     * Cargo's run mojo exposes no user property on any parameter that could
     * carry the loop's flags, so the only channel is a Maven project property
     * the build extension sets - and a daemon running from an exploded build
     * directory has no jar to point Maven at, so there is no extension and no
     * channel. That has to be said out loud: the application would otherwise
     * start without the agents and every apply would quietly restart.
     */
    @Test
    void cargoWithoutTheExtensionSaysTheAgentsCannotGetThrough()
            throws IOException {
        AppRuntime runtime = runtimeOf(serverModule("tc", CARGO, Map.of()));

        List<String> warnings = runtime.warnings();

        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("cargo.jvmargs"), warnings.get(0));
    }

    /**
     * A project may declare Cargo for its integration tests alone, so the
     * container's own plugin is the better answer when a pom has both. Cargo
     * has a container of its own for Liberty too, so that entry has to sit
     * ahead of Cargo's in the table just as TomEE's does.
     */
    @Test
    void theContainersOwnPluginBeatsCargo() throws IOException {
        Path app = module("both", "war", "");
        writeModel(app, List.of(CARGO, TOMEE));
        assertEquals("tomee", runtimeOf(app).name());

        Path liberty = module("bothLiberty", "war", "");
        writeModel(liberty, List.of(CARGO, LIBERTY));
        assertEquals("liberty", runtimeOf(liberty).name());
    }

    /**
     * And the same for Payara, which Cargo can also drive - it has a container
     * of its own for it. A project declaring both means its own plugin, so both
     * Payara entries have to sit ahead of Cargo's in the table. The order is
     * the whole of the rule, so it is worth a test that fails if someone
     * appends a future entry after {@code cargo()} rather than before it.
     */
    @Test
    void payarasOwnPluginsBeatCargoToo() throws IOException {
        Path server = module("bothServer", "war", "");
        writeModel(server, List.of(CARGO, PAYARA));
        assertEquals("payara", runtimeOf(server).name());

        Path micro = module("bothMicro", "war", "");
        writeModel(micro, List.of(CARGO, PAYARA_MICRO));
        assertEquals("payara-micro", runtimeOf(micro).name());
    }

    @Test
    void wildflyReportsItIsServing_andWhichPort() throws IOException {
        AppRuntime runtime = runtimeOf(serverModule("wf", WILDFLY, Map.of()));

        // WFLYUT0006, as wildfly-undertow's own message catalogue spells it:
        // "Undertow %s listener %s listening on %s:%d".
        String line = "15:21:03,112 INFO  [org.wildfly.extension.undertow] "
                + "(MSC service thread 1-4) WFLYUT0006: Undertow HTTP "
                + "listener default listening on 127.0.0.1:8080";
        assertTrue(runtime.serving(line));
        assertEquals(OptionalInt.of(8080), runtime.port(line));
    }

    /**
     * The HTTPS listener logs the same message, and its port is not the one a
     * browser is sent to - so it must not be read as the application serving.
     */
    @Test
    void wildflyHttpsListenerIsNotTheServingLine() throws IOException {
        AppRuntime runtime = runtimeOf(serverModule("wf", WILDFLY, Map.of()));

        assertFalse(runtime.serving("WFLYUT0006: Undertow HTTPS listener "
                + "default listening on 127.0.0.1:8443"));
    }

    /**
     * The deployment WildFly boots from its persisted configuration logs
     * "Deployed" too, from the boot thread; only the goal's own deploy comes
     * from the management handler, first time or replacing the booted one.
     * Lines as JBoss EAP 8.1 logged them.
     */
    @Test
    void wildflyIsDeployedOnlyByTheGoalsOwnDeployment() throws IOException {
        AppRuntime runtime = runtimeOf(serverModule("wf", WILDFLY, Map.of()));

        assertFalse(runtime.deployed("16:33:05,692 INFO  [org.jboss.as.server] "
                + "(Controller Boot Thread) WFLYSRV0010: Deployed \"ROOT.war\" "
                + "(runtime-name : \"ROOT.war\")"));
        assertTrue(runtime.deployed("16:33:22,780 INFO  [org.jboss.as.server] "
                + "(management-handler-thread - 2) WFLYSRV0016: Replaced "
                + "deployment \"ROOT.war\" with deployment \"ROOT.war\""));
        assertTrue(runtime.deployed("16:33:22,780 INFO  [org.jboss.as.server] "
                + "(management-handler-thread - 1) WFLYSRV0010: Deployed "
                + "\"ROOT.war\" (runtime-name : \"ROOT.war\")"));
    }

    @Test
    void tomeeReportsItIsServing_andWhichPort() throws IOException {
        AppRuntime runtime = runtimeOf(serverModule("te", TOMEE, Map.of()));

        // Tomcat's abstractProtocolHandler.start, whose argument is the
        // endpoint name run through ObjectName.quote - hence the quotes.
        String line = "21-Sep-2026 15:21:03.112 INFO [main] "
                + "org.apache.coyote.AbstractProtocol.start Starting "
                + "ProtocolHandler [\"http-nio-8080\"]";
        assertTrue(runtime.serving(line));
        assertEquals(OptionalInt.of(8080), runtime.port(line));
    }

    /**
     * A connector that names an address puts it between the protocol and the
     * port, so the port is the last segment rather than the second.
     */
    @Test
    void tomeeReadsThePortPastABoundAddress() throws IOException {
        AppRuntime runtime = runtimeOf(serverModule("te", TOMEE, Map.of()));

        assertEquals(OptionalInt.of(8081), runtime.port(
                "Starting ProtocolHandler [\"http-nio-127.0.0.1-8081\"]"));
    }

    /**
     * The CDI starter declares WildFly and TomEE both, so the table's order
     * decides and not the pom's - and the developer has to be able to decide
     * otherwise. TomEE is written first here for exactly that reason.
     */
    @Test
    void bothServersDeclared_theTableDecidesAndThePropertyOverrules()
            throws IOException {
        Path app = module("both", "war", "");
        writeModel(app, List.of(TOMEE, WILDFLY));

        assertEquals("wildfly", runtimeOf(app).name());

        System.setProperty("vaadin.dev.runtime", "tomee");
        try {
            assertEquals("tomee", runtimeOf(app).name());
        } finally {
            System.clearProperty("vaadin.dev.runtime");
        }
    }

    /**
     * TomEE's redeploy-on-change is the second driver of restarts that
     * {@code jetty.scan} is for Jetty, and the generated starter turns it on.
     */
    @Test
    void tomeeReloadOnUpdateIsWarnedAbout() throws IOException {
        AppRuntime runtime = runtimeOf(
                serverModule("te", TOMEE, Map.of("reloadOnUpdate", "true")));

        assertTrue(
                runtime.warnings().stream().anyMatch(
                        line -> line.contains("<reloadOnUpdate>true")),
                runtime.warnings().toString());
    }

    /**
     * The extension forces constants. The parameter that carries the loop's own
     * agents has no constant to force - its value is composed per launch - so
     * it must stay out of the forced set, where an empty value would blank what
     * the launch had just put there.
     */
    @Test
    void theAgentParameterIsNotAmongTheForcedConfiguration() {
        assertEquals("reloadOnUpdate=false",
                entry("tomee").forcedConfiguration());
    }

    /**
     * WildFly's run mojo declares {@code @Execute(phase = PACKAGE)} and forks
     * the packaging itself; TomEE's declares nothing, so the command line has
     * to ask for it. Naming a phase for WildFly too would build the WAR twice,
     * and leaving it off TomEE would deploy whatever WAR was lying about.
     */
    @Test
    void onlyTheContainerThatForksNoLifecycleOfItsOwnIsGivenAPhase() {
        assertEquals("", entry("wildfly").phase());
        assertEquals("package", entry("tomee").phase());
        assertEquals("package", entry("cargo").phase());
        assertEquals("", entry("jetty-ee10").phase());
        // Neither Payara mojo declares @Execute of any kind, so both are in
        // TomEE's position rather than WildFly's: nothing builds the WAR for
        // them, and a goal run without the phase would deploy whatever was
        // lying about in target from a previous build.
        assertEquals("package", entry("payara").phase());
        assertEquals("package", entry("payara-micro").phase());
        // Liberty declares no @Execute either, and although its run mojo
        // invokes war:war itself it names package: its goal is bound to that
        // phase rather than named, and a bound goal needs a phase to run at.
        assertEquals("package", entry("liberty").phase());
    }

    /**
     * A goal named on a Maven command line runs on <em>every</em> project in
     * the reactor, and the loop names one with {@code -pl :app -am} so that a
     * sibling module builds in the same session. Jetty's mojo supports
     * {@code war} packaging alone and skips the rest; Cargo's does not -
     * measured against this repository's own multi-module fixture it ran first
     * on the reactor root and failed the build before anything started. So the
     * goal is switched off for the whole reactor by its user property and
     * switched back on, by a {@code <configuration>} value the extension
     * writes, for the one module that declares the plugin.
     */
    @Test
    void cargoRunsOnTheApplicationsOwnModuleAlone() {
        assertEquals(Map.of("cargo.maven.skip", "true"),
                entry("cargo").goalProperties());
        assertEquals("skip=false", entry("cargo").forcedConfiguration());
    }

    /**
     * WildFly pays the same price. {@code RunMojo} never looks at a project's
     * packaging: it builds the deployment's file name from
     * {@code ${project.build.finalName}} and that packaging, and fails when no
     * such file exists - so the reactor root ends the build asking for a
     * deployment named after a {@code pom}. A single-module project never shows
     * it.
     */
    @Test
    void wildflyRunsOnTheApplicationsOwnModuleAlone() {
        assertEquals(Map.of("wildfly.skip", "true"),
                entry("wildfly").goalProperties());
        assertTrue(
                entry("wildfly").forcedConfiguration().contains("skip=false"),
                entry("wildfly").forcedConfiguration());
    }

    /**
     * And the other way round for the containers that need no such treatment: a
     * skip switched on for them and never switched off would start nothing.
     */
    @Test
    void noOtherRuntimeSwitchesItsGoalOff() {
        assertEquals(Map.of(), entry("tomee").goalProperties());
    }

    /**
     * Payara Micro's goal property is the one that is not a skip, and it is
     * switched the other way: {@code deployWar} defaults to <em>false</em> and
     * it is the {@code dev} goal that turns it on, so a project whose pom
     * relies on {@code payara-micro:dev} declares it nowhere and the
     * {@code start} the loop runs would bring up a server with the application
     * deployed in it nowhere at all. The forced element is what holds a pom
     * that pins it off, the {@code -D} what covers a daemon with no extension.
     */
    @Test
    void payaraMicroIsToldToDeployTheApplication() {
        assertEquals(Map.of("payara.skip", "true", "payara.deploy.war", "true"),
                entry("payara-micro").goalProperties());
        assertTrue(
                entry("payara-micro").forcedConfiguration()
                        .contains("deployWar=true"),
                entry("payara-micro").forcedConfiguration());
    }

    /**
     * Both Payara entries pay Cargo's price, and for the same reason. Measured:
     * {@code payara-micro:start} named on the command line ran first on the
     * reactor <em>root</em>, started a Payara Micro there, reported
     * {@code Deployed 0 archive(s)} and blocked the reactor before the
     * application module was built at all. So the goal is switched off for the
     * whole reactor by its user property and switched back on, by a
     * {@code <configuration>} value the extension writes, for the one module
     * that declares the plugin.
     */
    @Test
    void bothPayarasRunOnTheApplicationsOwnModuleAlone() {
        assertEquals("true", entry("payara").goalProperties().get("skip"));
        assertEquals("true",
                entry("payara-micro").goalProperties().get("payara.skip"));
        for (String name : List.of("payara", "payara-micro")) {
            assertTrue(entry(name).skippedOutsideTheApplication(), name);
            assertTrue(entry(name).forcedConfiguration().contains("skip=false"),
                    name + ": " + entry(name).forcedConfiguration());
        }
    }

    /**
     * The two halves belong together, so the flag saying an entry has them has
     * to agree with the entries that do. An entry that set the skip and forgot
     * the force would start nothing at all.
     */
    @Test
    void onlyTheEntriesThatSwitchTheirGoalOffSaySo() {
        assertTrue(entry("cargo").skippedOutsideTheApplication());
        assertTrue(entry("wildfly").skippedOutsideTheApplication());
        assertFalse(entry("jetty-ee10").skippedOutsideTheApplication());
        // TomEE has no such half to set: its run mojo declares no skip
        // parameter at all, which is why its goal is bound in the application
        // module rather than switched off outside it - see below.
        assertFalse(entry("tomee").skippedOutsideTheApplication());
        // Nor has Liberty one it could use: the skip its run mojo reads has no
        // prefix and would reach every plugin that reads ${skip}. Its goal is
        // bound in the application module too.
        assertFalse(entry("liberty").skippedOutsideTheApplication());
    }

    /**
     * The fourth way of keeping a goal to one module, and the only one that
     * needs no cooperation from the plugin: do not name the goal at all, and
     * let the build extension bind it to a phase in the application's own
     * model. TomEE is the entry that needs it - its run mojo has no skip of any
     * kind, and {@code AbstractTomEEMojo.execute} unzips a TomEE and ends in
     * {@code run()} whatever the module's packaging is. Liberty needs it too,
     * for the opposite reason: its run mojo does keep the server to the
     * farthest downstream project, but on the way through every other module it
     * replaces that module's jar with its {@code target/classes}, so the WAR it
     * then packages carries an empty directory for each sibling. The rest of
     * the table can be kept to one module by a means that leaves the command
     * line saying what it runs.
     */
    @Test
    void onlyTomeeAndLibertyAreBoundRatherThanNamed() {
        for (ServerPlugin plugin : ServerPlugin.KNOWN) {
            assertEquals(Set.of("tomee", "liberty").contains(plugin.name()),
                    plugin.boundInTheApplication(), plugin.name());
        }
    }

    /**
     * Two things a bound entry cannot do without, and neither is checkable at
     * the point of use: the phase is half of what the extension is told to
     * bind, so an entry with none would ask for {@code :run}; and a goal that
     * is never named cannot also be switched off by a user property, the skip
     * inversion and the binding being two answers to the same question.
     */
    @Test
    void aBoundEntryNamesItsPhaseAndSwitchesNothingOff() {
        for (ServerPlugin plugin : ServerPlugin.KNOWN) {
            if (!plugin.boundInTheApplication()) {
                continue;
            }
            assertFalse(plugin.phase().isBlank(), plugin.name());
            assertFalse(plugin.skippedOutsideTheApplication(), plugin.name());
            assertEquals(Optional.empty(), plugin.skipProperty(),
                    plugin.name());
        }
    }

    /**
     * And without the extension neither half is sent: the goal then runs on
     * every module, which is a bad day, but sending only the skip would start
     * nothing at all, which is a worse one. The warning is what makes the
     * difference diagnosable.
     */
    @Test
    void payaraWithoutTheExtensionSaysTheGoalCannotBeKeptToOneModule()
            throws IOException {
        AppRuntime runtime = runtimeOf(serverModule("ps", PAYARA, Map.of()));

        List<String> warnings = runtime.warnings();

        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("every module in the reactor"),
                warnings.get(0));
    }

    /**
     * Both Payara entries force their plugin's own watcher and log rewriting
     * off. {@code start} already defaults all of them off, so these only hold a
     * pom that asks for them back - but that pom would otherwise win, a
     * {@code <configuration>} value beating the user property of the same
     * parameter, and the loop would be sharing the application with a second
     * thing redeploying it.
     */
    @Test
    void bothPayarasForceTheirOwnRedeployersOff() {
        for (String name : List.of("payara", "payara-micro")) {
            String forced = entry(name).forcedConfiguration();
            assertTrue(forced.contains("autoDeploy=false"),
                    name + ": " + forced);
            assertTrue(forced.contains("liveReload=false"),
                    name + ": " + forced);
            // A goal that returned as soon as the server was up would leave the
            // daemon owning a Maven that had already exited.
            assertTrue(forced.contains("daemon=false"), name + ": " + forced);
        }
    }

    /**
     * Cargo is the only one whose flags parameter no {@code -D} can set: every
     * element of its run mojo that could carry them is nested and settable from
     * a pom alone, so the build extension has to put the value on the model
     * instead. Getting this wrong for one of the others would send its flags
     * through an extension that was never asked to set that property, and the
     * server would start without the agents.
     */
    @Test
    void onlyCargoTakesItsFlagsFromAProjectProperty() {
        assertTrue(entry("cargo").projectPropertyFlags());
        assertEquals("cargo.jvmargs", entry("cargo").jvmFlagsProperty());
        assertFalse(entry("wildfly").projectPropertyFlags());
        assertFalse(entry("tomee").projectPropertyFlags());
        assertFalse(entry("jetty-ee10").projectPropertyFlags());
        // Both Payaras expose a user property of their own, so neither needs
        // the extension to get its flags through - which is why neither is
        // listed among the warnings a daemon without one produces.
        assertFalse(entry("payara").projectPropertyFlags());
        assertFalse(entry("payara-micro").projectPropertyFlags());
        // And Liberty reads the system properties a -D sets as well as the
        // project's own, so it needs no extension either.
        assertFalse(entry("liberty").projectPropertyFlags());
    }

    /**
     * Payara Server's channel is declared {@code List<String>}, and Maven
     * splits such a user property on commas before the plugin sees it - a bare
     * comma split with no escaping available. The mojo then makes a key and a
     * value of each piece at its first {@code =} and drops any piece with none,
     * so the loop's own
     * {@code -DdisabledPlugins=Vaadin,Spring,SpringBoot,Jetty} would arrive as
     * {@code -DdisabledPlugins=Vaadin} and nothing would say so. Micro's is a
     * plain string read straight off the user properties and split on
     * whitespace alone, so it has no such problem and must not pay the cost of
     * an argument file for it.
     */
    @Test
    void onlyTheListValuedChannelIsSplitOnCommas() {
        assertTrue(entry("payara").commaSplitFlags());
        assertFalse(entry("payara-micro").commaSplitFlags());
        assertFalse(entry("wildfly").commaSplitFlags());
        assertFalse(entry("tomee").commaSplitFlags());
        assertFalse(entry("cargo").commaSplitFlags());
        assertFalse(entry("jetty-ee10").commaSplitFlags());
        // A jvm.options line is taken whole, so a comma in one divides nothing.
        assertFalse(entry("liberty").commaSplitFlags());
    }

    /**
     * Liberty's channel carries one flag per property - each
     * {@code liberty.jvm.*} becomes one line of the server's generated
     * {@code jvm.options} - so the flags go out as a {@code -D} each. Every
     * other channel is one value holding all of them, and splitting one of
     * those up would name properties no plugin reads, leaving the server
     * starting with no agents at all.
     */
    @Test
    void onlyLibertyTakesOneFlagPerProperty() {
        assertTrue(entry("liberty").perPropertyFlags());
        assertFalse(entry("wildfly").perPropertyFlags());
        assertFalse(entry("tomee").perPropertyFlags());
        assertFalse(entry("payara").perPropertyFlags());
        assertFalse(entry("payara-micro").perPropertyFlags());
        assertFalse(entry("cargo").perPropertyFlags());
        assertFalse(entry("jetty-ee10").perPropertyFlags());
    }

    /**
     * Neither Payara entry has a parameter it must warn about rather than
     * force, which is the shape WildFly's {@code <javaOpts>} and TomEE's
     * {@code <args>} are in: for both of those the pom's value beats the
     * command line and the value the loop would need to write is composed per
     * launch, so the daemon can only ask. Payara's plugins append the user
     * property's value to the pom's list instead of replacing it, so the
     * question never arises - and an entry with no acceptable value added here
     * by accident would produce a warning on every start that no developer
     * could act on.
     */
    @Test
    void neitherPayaraHasAParameterItCanOnlyWarnAbout() {
        for (String name : List.of("payara", "payara-micro")) {
            assertTrue(entry(name).competing().stream()
                    .noneMatch(value -> value.acceptable().isEmpty()), name);
        }
    }

    /**
     * TomEE unescapes the value it is handed and WildFly does not, so only one
     * of them may have its backslashes doubled. Getting it the wrong way round
     * is invisible on Linux and stops the JVM starting on Windows.
     */
    @Test
    void onlyTheShellParsedChannelIsEscaped() {
        assertTrue(entry("tomee").shellEscapedFlags());
        assertFalse(entry("wildfly").shellEscapedFlags());
        // Cargo runs Ant's translateCommandline over the value: quotes group
        // and whitespace separates, but a backslash is an ordinary character.
        assertFalse(entry("cargo").shellEscapedFlags());
        assertFalse(entry("jetty-ee10").shellEscapedFlags());
        // Payara Server's parser does treat a backslash as an escape, but only
        // before a quote or another backslash - so a Windows path arrives whole
        // and doubling it would be the thing that broke it. Micro's splits on
        // whitespace and nothing else.
        assertFalse(entry("payara").shellEscapedFlags());
        assertFalse(entry("payara-micro").shellEscapedFlags());
        // And Liberty writes the value into a file verbatim.
        assertFalse(entry("liberty").shellEscapedFlags());
    }

    /**
     * Jetty is the only one that runs in the build's JVM, so it is the only one
     * whose flags go in MAVEN_OPTS rather than a parameter of the plugin.
     */
    @Test
    void onlyJettyIsEmbedded() {
        assertTrue(entry("jetty-ee10").embedded());
        assertFalse(entry("wildfly").embedded());
        assertFalse(entry("cargo").embedded());
        assertFalse(entry("payara").embedded());
        assertFalse(entry("payara-micro").embedded());
        assertEquals("wildfly.javaOpts", entry("wildfly").jvmFlagsProperty());
        assertEquals("tomee-plugin.args", entry("tomee").jvmFlagsProperty());
        assertEquals("payara.javaCommandLineOptions",
                entry("payara").jvmFlagsProperty());
        // Undocumented, and the only channel the mojo offers: every parameter
        // that could carry the flags is nested and pom-only, but exec.args is
        // read straight off the session's user properties.
        assertEquals("exec.args", entry("payara-micro").jvmFlagsProperty());
        assertFalse(entry("liberty").embedded());
        // Not a property name but a prefix: Liberty takes one flag per
        // property, and the flag's position is what makes the names distinct.
        assertEquals("liberty.jvm.devloop",
                entry("liberty").jvmFlagsProperty());
    }

    private static ServerPlugin entry(String name) {
        return ServerPlugin.KNOWN.stream()
                .filter(plugin -> name.equals(plugin.name())).findFirst()
                .orElseThrow();
    }

    /** A WAR module whose build runs one of the forked containers. */
    private Path serverModule(String name, String coordinates,
            Map<String, String> configuration) throws IOException {
        Path app = module(name, "war", "");
        writeModel(app, coordinates, configuration);
        return app;
    }

    /** A module whose build runs several known plugins, in pom order. */
    private void writeModel(Path app, List<String> coordinates)
            throws IOException {
        Properties model = new Properties();
        model.setProperty("packaging", "war");
        model.setProperty("plugins", String.valueOf(coordinates.size()));
        for (int index = 0; index < coordinates.size(); index++) {
            model.setProperty("plugin." + index, coordinates.get(index));
        }
        Path file = app.resolve(EffectiveModel.FILE);
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file)) {
            model.store(writer, "test fixture");
        }
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
