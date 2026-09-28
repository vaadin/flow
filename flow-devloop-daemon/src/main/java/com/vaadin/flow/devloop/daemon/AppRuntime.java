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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * How this project's application is started, and how its output reads.
 * <p>
 * There are two shapes of Vaadin application, and the difference is not
 * cosmetic. One has an entry point - a Spring Boot class, or any
 * {@code public static void main} - and is launched as
 * {@code java -cp ... <MainClass>}. The other is a WAR deployed into a servlet
 * container, which has no entry point at all and whose server is not even on
 * its classpath: the server lives in a build plugin. For that shape the build
 * is the only thing that knows how to start the application, so the daemon runs
 * the project's own run goal rather than inventing a server of its own.
 * <p>
 * What both have in common, and what this interface exists to preserve, is that
 * the application is a <em>direct child</em> of the daemon. That is what makes
 * an exit code meaningful and a stop clean; see {@link AppProcess}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
interface AppRuntime {

    /**
     * One launch, as a process the daemon can own.
     *
     * @param command
     *            the command line
     * @param environment
     *            variables to add to the child's environment
     * @param argFile
     *            whether everything after the first element may be moved into a
     *            JVM argument file; true only when the command <em>is</em> a
     *            JVM invocation
     */
    record Invocation(List<String> command, Map<String, String> environment,
            boolean argFile) {
    }

    /**
     * How this runtime is named in {@code status}, in the log and in
     * {@code -Dvaadin.dev.runtime}.
     *
     * @return the runtime name
     */
    String name();

    /**
     * Composes the launch.
     *
     * @param project
     *            the resolved build
     * @param jvmFlags
     *            flags only a JVM command line accepts - the agents, the opens,
     *            the redefinition switch
     * @param systemProperties
     *            {@code -D} settings the application reads
     * @return the invocation
     * @throws IOException
     *             if the launch cannot be composed
     */
    Invocation invocation(Launch.Project project, List<String> jvmFlags,
            List<String> systemProperties) throws IOException;

    /**
     * Whether one line of the application's log says its web server is
     * listening. This is the signal a port clash never reaches, which is what
     * makes it usable as "the port really is ours".
     *
     * @param line
     *            a log line
     * @return {@code true} if the server is up
     */
    boolean serving(String line);

    /**
     * The port the application bound, when the line that said it was serving
     * also said which port.
     *
     * @param line
     *            a log line for which {@link #serving} is {@code true}
     * @return the port, or empty when the line does not carry one
     */
    default OptionalInt port(String line) {
        return OptionalInt.empty();
    }

    /**
     * Configuration in this project that will fight the dev loop, in words, so
     * that a developer is told rather than left to deduce it from behaviour.
     *
     * @return one line per problem, empty when there is none
     */
    default List<String> warnings() {
        return List.of();
    }

    /**
     * JVM flags this runtime needs on top of the ones every launch gets.
     *
     * @return the extra flags, empty when the runtime needs none
     */
    default List<String> extraJvmFlags() {
        return List.of();
    }

    /**
     * Which runtime this project needs.
     * <p>
     * The order is what matters, because the cheap signals are also the
     * ambiguous ones. A WAR project may perfectly well carry some unrelated
     * {@code public static void main}, so "has a main method" cannot be asked
     * first; and a Spring Boot application may itself be packaged as a WAR, so
     * "is packaged war" cannot be asked first either. What is unambiguous is a
     * build that <em>names</em> an entry point - a packaged jar's
     * {@code Start-Class}, or {@code @SpringBootApplication} - and that is
     * therefore asked before anything is read off the packaging.
     *
     * @param launch
     *            the launch this runtime will serve
     * @param log
     *            where the decision is reported
     * @return the runtime to launch with
     * @throws IOException
     *             if the project looks like neither shape
     */
    static AppRuntime of(Launch launch, Launch.Log log) throws IOException {
        Reactor reactor = launch.reactor();
        String configured = System.getProperty("vaadin.dev.runtime");
        if (configured != null && !configured.isBlank()) {
            return forced(launch, log, configured.trim());
        }
        if (MainClass.namedByBuild(reactor.app()).isPresent()) {
            return new MainClassRuntime(launch, log);
        }
        Optional<AppRuntime> server = serverRuntime(launch, log);
        if (server.isPresent()) {
            return server.get();
        }
        if (MainClass.discover(reactor.app(), log).isPresent()) {
            return new MainClassRuntime(launch, log);
        }
        throw new IOException("cannot tell how to start this application: no "
                + "entry point under " + reactor.app().classesDir()
                + ", and no supported server plugin in its pom (looked for "
                + names() + "). Build the module once, or name the class with "
                + "-Dvaadin.dev.mainClass, or the runtime with "
                + "-Dvaadin.dev.runtime");
    }

    /**
     * The runtime a project declares a known build plugin for, if any.
     *
     * @param launch
     *            the launch the runtime will be composed from
     * @param log
     *            where the choice is announced
     * @return the runtime, or empty when the pom declares no known plugin
     */
    private static Optional<AppRuntime> serverRuntime(Launch launch,
            Launch.Log log) {
        Reactor reactor = launch.reactor();
        for (ServerPlugin plugin : ServerPlugin.KNOWN) {
            Optional<Reactor.PluginConfig> declared = reactor
                    .plugin(plugin.groupId(), plugin.artifactId());
            if (declared.isPresent()) {
                log.line("runtime: " + plugin.name() + " (the build runs "
                        + plugin.artifactId() + ", packaging "
                        + reactor.packaging() + ")");
                return Optional.of(new MavenGoalRuntime(launch, plugin,
                        declared.get(), log));
            }
        }
        return Optional.empty();
    }

    /**
     * {@code -Dvaadin.dev.runtime}, which overrides every heuristic above.
     *
     * @param launch
     *            the launch the runtime will be composed from
     * @param log
     *            where the choice is announced
     * @param name
     *            the runtime asked for by name
     * @return the runtime that name asks for
     * @throws IOException
     *             if no runtime goes by that name, or the project declares no
     *             plugin for the one that does
     */
    private static AppRuntime forced(Launch launch, Launch.Log log, String name)
            throws IOException {
        if (MainClassRuntime.NAME.equals(name)) {
            return new MainClassRuntime(launch, log);
        }
        for (ServerPlugin plugin : ServerPlugin.KNOWN) {
            if (!plugin.name().equals(name)) {
                continue;
            }
            Reactor.PluginConfig declared = launch.reactor()
                    .plugin(plugin.groupId(), plugin.artifactId())
                    .orElseThrow(() -> new IOException("vaadin.dev.runtime="
                            + name + " but this build does not run "
                            + plugin.artifactId()));
            return new MavenGoalRuntime(launch, plugin, declared, log);
        }
        throw new IOException("vaadin.dev.runtime=" + name
                + " is not a runtime this daemon knows; try " + names());
    }

    /**
     * Every runtime name this daemon answers to, for a message that has to
     * offer the alternatives.
     *
     * @return the names, comma-separated
     */
    private static String names() {
        List<String> known = new ArrayList<>();
        known.add(MainClassRuntime.NAME);
        ServerPlugin.KNOWN.forEach(plugin -> known.add(plugin.name()));
        return String.join(", ", known);
    }
}
