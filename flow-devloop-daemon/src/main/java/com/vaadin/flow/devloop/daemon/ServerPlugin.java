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
 * A build plugin that runs an application server in the build's own JVM, and
 * everything the dev loop needs in order to drive one.
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

    /** Every server the dev loop can drive through the project's own build. */
    static final List<ServerPlugin> KNOWN = List.of(jetty("ee10"),
            jetty("ee11"));

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
                "jetty-" + ee + "-maven-plugin", "run",
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
