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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A build plugin that runs an application server for the project, and
 * everything the dev loop needs in order to drive one.
 * <p>
 * Two shapes, and the difference is where the loop's JVM flags go. Jetty runs
 * the application in the build's own JVM, so {@code MAVEN_OPTS} carries them.
 * WildFly and TomEE have no such mode - each provisions a server and starts it
 * as a process of its own - so the flags go to a parameter of the plugin, named
 * by {@link #jvmFlagsProperty}, and reach the server's JVM from there.
 * <p>
 * A table rather than a class per server: the entries differ only in
 * coordinates, in which properties keep the server in-process, and in the line
 * the server logs once it has bound a port. Adding Tomcat or another container
 * later is therefore an entry in {@link #KNOWN} rather than a new subsystem,
 * which is the whole reason {@link AppRuntime} is shaped the way it is.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param name
 *            how this runtime is named in {@code status}, in the log and in
 *            {@code -Dvaadin.dev.runtime}
 * @param groupId
 *            the plugin's group
 * @param artifactId
 *            the plugin's artifact
 * @param goal
 *            the goal that runs the application
 * @param phase
 *            the lifecycle phase to run before the goal, empty when the goal
 *            needs nothing built first. A container that deploys a packaged WAR
 *            needs {@code package}; one that serves the module's own output
 *            does not
 * @param jvmFlagsProperty
 *            the user property carrying the flags the application's JVM must
 *            start with, whitespace-separated. Empty when the server runs in
 *            the build's own JVM, where {@code MAVEN_OPTS} is the channel
 *            instead
 * @param shellEscapedFlags
 *            whether that property is parsed the way a shell would, so that a
 *            backslash escapes the character after it and has to be doubled to
 *            survive. False for a plugin that merely splits on whitespace
 * @param goalProperties
 *            properties passed on the Maven command line to keep the server in
 *            the build's own JVM and its rescanner switched off
 * @param competing
 *            configuration that would fight the dev loop if a pom pins it
 * @param serving
 *            matches the line the server logs once it is listening, capturing
 *            the bound port in group 1
 */
record ServerPlugin(String name, String groupId, String artifactId, String goal,
        String phase, String jvmFlagsProperty, boolean shellEscapedFlags,
        Map<String, String> goalProperties, List<Competing> competing,
        Pattern serving) {

    /**
     * Where a Jetty connector announces the port it bound.
     * <p>
     * A real line reads {@code Started oejs.ServerConnector@34ab4818} followed
     * by the protocol group and then the host and port one:
     * {@code AbstractConnector} logs "Started {}" and
     * {@code AbstractNetworkConnector}'s toString ends in that second group.
     * Both groups are matched rather than skipped over with a greedy
     * {@code .*}, which is what keeps the scan free of backtracking - the class
     * name is one run up to its {@code @}, and a lookbehind asserts what that
     * run ends with.
     */
    private static final Pattern JETTY_SERVING = Pattern
            .compile("Started\\s++[^\\s{@]*+(?<=Connector)@[^\\s{]*+"
                    + "\\{[^{}]*+\\}\\{[^{}:]*+:(\\d++)\\}");

    /**
     * Where WildFly's Undertow subsystem announces the port it bound.
     * <p>
     * The message is {@code WFLYUT0006: Undertow %s listener %s listening on
     * %s:%d}, read out of {@code wildfly-undertow} itself rather than
     * remembered. Pinned to the plain HTTP listener, because an HTTPS one logs
     * the same message and the loop wants the port a browser is sent to. The
     * repetition before the port eats an IPv6 address's own colons, so the
     * captured group is the port either way.
     */
    private static final Pattern WILDFLY_SERVING = Pattern
            .compile("WFLYUT0006: Undertow HTTP listener \\S++ listening on "
                    + "(?:[^\\s:]*+:)++(\\d++)");

    /**
     * Where TomEE's Tomcat announces the port it bound.
     * <p>
     * Tomcat's own {@code abstractProtocolHandler.start=Starting
     * ProtocolHandler [{0}]}, whose argument is the endpoint name run through
     * {@code ObjectName.quote} - hence the quotes - and reads
     * {@code http-nio-8080}, or {@code http-nio-<address>-8080} when a
     * connector names one. Anchored on {@code http-} so that an AJP connector
     * starting first cannot answer for it.
     */
    private static final Pattern TOMEE_SERVING = Pattern.compile(
            "Starting ProtocolHandler \\[\"http-(?:[^\"-]*+-)*+(\\d++)\"\\]");

    /**
     * Every server the dev loop can drive through the project's own build.
     * <p>
     * The order is the order discovery tries them in, which matters only for a
     * project declaring more than one - the CDI starter declares WildFly and
     * TomEE both. WildFly is first there because that starter's own
     * {@code <defaultGoal>} runs it; {@code -Dvaadin.dev.runtime} is how a
     * project says otherwise.
     */
    static final List<ServerPlugin> KNOWN = List.of(jetty("ee10"),
            jetty("ee11"), wildfly(), tomee());

    /**
     * A configuration value the dev loop needs to hold but cannot set.
     * <p>
     * This exists because of a Maven rule with real teeth: a
     * {@code <configuration>} value in the pom beats the user property the same
     * parameter exposes, so {@code -Djetty.scan=0} does nothing to a pom that
     * writes {@code <scan>2</scan>}. The daemon cannot win that argument, so it
     * says so rather than letting two things restart the application on
     * schedules of their own.
     *
     * @param element
     *            the {@code <configuration>} element to look at
     * @param acceptable
     *            the values that leave the dev loop in sole charge
     * @param consequence
     *            what happens if the pom keeps its own value
     * @param fix
     *            what the developer should write instead
     */
    record Competing(String element, List<String> acceptable,
            String consequence, String fix) {
    }

    /**
     * Jetty 12's Maven plugin, one entry per Jakarta EE level.
     * <p>
     * {@code EMBED} is already the plugin's default deploy mode; it is passed
     * anyway, so the command line states what the loop depends on rather than
     * resting on a default that could move.
     *
     * @param ee
     *            the Jakarta EE level, as the plugin spells it
     */
    private static ServerPlugin jetty(String ee) {
        return new ServerPlugin("jetty-" + ee, "org.eclipse.jetty." + ee,
                "jetty-" + ee + "-maven-plugin", "run", "", "", false,
                Map.of("jetty.deployMode", "EMBED", "jetty.scan", "0"),
                List.of(new Competing("deployMode", List.of("EMBED"),
                        "the application would be a grandchild of the daemon, "
                                + "so its exit code would be lost and stopping "
                                + "it could orphan the JVM",
                        "remove <deployMode>, or set it to EMBED"),
                        new Competing("scan", List.of("0", "-1"),
                                "the plugin would redeploy the webapp on a "
                                        + "schedule of its own, competing with "
                                        + "every apply",
                                "set <scan>0</scan>")),
                JETTY_SERVING);
    }

    /**
     * WildFly's Maven plugin.
     * <p>
     * It has no embedded mode. {@code wildfly:run} provisions a server under
     * {@code target/} and starts it as a process of its own, which its
     * {@code javaHome}, {@code javaOpts} and {@code env} parameters - the last
     * documented as "passed to the process being started" - make plain. So the
     * loop's flags travel in {@code wildfly.javaOpts}, which the mojo's own
     * {@code setJavaOpts} splits on whitespace.
     * <p>
     * What it deploys is the packaged WAR, but the goal declares
     * {@code @Execute(phase = PACKAGE)} and forks the packaging itself, so no
     * phase is named here. Naming one would build the WAR twice.
     * <p>
     * {@code javaHome} defaults to {@code ${java.home}}, so the server runs on
     * the JVM Maven runs on, and the JDK chosen for the application - a JBR,
     * and with it enhanced class redefinition - carries over unasked.
     */
    private static ServerPlugin wildfly() {
        return new ServerPlugin("wildfly", "org.wildfly.plugins",
                "wildfly-maven-plugin", "run", "", "wildfly.javaOpts", false,
                Map.of(),
                List.of(new Competing("javaOpts", List.of(),
                        "the agents the loop needs would be dropped, and every "
                                + "apply would restart instead of hot reloading",
                        "remove <javaOpts>; the dev loop needs that parameter "
                                + "for its agents")),
                WILDFLY_SERVING);
    }

    /**
     * TomEE's Maven plugin.
     * <p>
     * A fork as well: {@code tomee:run} starts the server through
     * {@code RemoteServer}, and {@code args} - parsed straight into the forked
     * JVM's argument list - is where the loop's flags go. {@code javaagents}
     * would read better and cannot be used: it carries no user property, so no
     * command line can set it.
     * <p>
     * It deploys the packaged WAR and, unlike WildFly's, its goal forks no
     * lifecycle of its own, so {@code package} has to be asked for.
     * <p>
     * Its {@code Args.parse} reads the value the way a shell would, so a
     * backslash escapes whatever follows it and is then dropped. Measured on
     * Windows, {@code C:\\Users\\...\\hotswap-agent.jar} reached the JVM as
     * {@code C:Users...hotswap-agent.jar} and the JVM refused to start - hence
     * {@code shellEscapedFlags}. Quoting is no help: the parser checks for an
     * escape before it checks for a quote, so a backslash inside quotes is
     * eaten just the same.
     * <p>
     * {@code reloadOnUpdate} is TomEE's own redeploy-on-change, the second
     * driver of restarts that {@code jetty.scan} is for Jetty, and a generated
     * starter turns it on alongside a {@code <synchronization>} that copies
     * classes into the deployed webapp. Switching the reload off is what leaves
     * the loop in sole charge; the copying on its own changes nothing.
     */
    private static ServerPlugin tomee() {
        return new ServerPlugin("tomee", "org.apache.tomee.maven",
                "tomee-maven-plugin", "run", "package", "tomee-plugin.args",
                true, Map.of(),
                List.of(new Competing("reloadOnUpdate", List.of("false"),
                        "the plugin would redeploy the webapp whenever its "
                                + "synchronization copied a class, competing "
                                + "with every apply",
                        "set <reloadOnUpdate>false</reloadOnUpdate>"),
                        new Competing("args", List.of(),
                                "the agents the loop needs would be dropped, "
                                        + "and every apply would restart "
                                        + "instead of hot reloading",
                                "remove <args>; the dev loop needs that "
                                        + "parameter for its agents")),
                TOMEE_SERVING);
    }

    /**
     * Whether this server runs the application in the build's own JVM.
     *
     * @return {@code true} when {@code MAVEN_OPTS} carries the loop's JVM
     *         flags, {@code false} when {@link #jvmFlagsProperty} does
     */
    boolean embedded() {
        return jvmFlagsProperty.isBlank();
    }

    /**
     * The configuration the dev loop needs, as {@code element=value} pairs for
     * {@code DevLoopBuildExtension}.
     * <p>
     * Derived from {@link #competing} rather than written out again: those are
     * exactly the values a pom must not be allowed to keep, and the first
     * acceptable one is the value to force.
     *
     * @return the pairs, semicolon-separated
     */
    String forcedConfiguration() {
        StringBuilder forced = new StringBuilder();
        for (Competing value : competing) {
            // An entry with no acceptable value is a warning and nothing more.
            // There is no constant to force: the value it guards - the agents
            // for this launch - is composed per run and cannot be tabulated.
            if (value.acceptable().isEmpty()) {
                continue;
            }
            if (!forced.isEmpty()) {
                forced.append(';');
            }
            forced.append(value.element()).append('=')
                    .append(value.acceptable().get(0));
        }
        return forced.toString();
    }

    /**
     * How the goal is named on a command line, pinned to the version the
     * project declares when it declares one.
     *
     * @param declared
     *            the plugin as the project's poms declare it
     * @return the goal specification, e.g.
     *         {@code org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:12.1.13:run}
     */
    String goalSpecification(Reactor.PluginConfig declared) {
        return declared.coordinates() + ":" + goal;
    }
}
