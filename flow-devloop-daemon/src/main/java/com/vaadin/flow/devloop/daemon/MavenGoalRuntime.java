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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;

import com.vaadin.flow.devloop.mavenext.DevLoopBuildExtension;

/**
 * A WAR application, started by running the project's own build plugin.
 * <p>
 * A WAR has no entry point, and the servlet container that would run it is not
 * on its classpath - it is a build plugin. So the build is the only thing that
 * knows how to start such an application, and the daemon asks it to, rather
 * than shipping a container of its own and guessing at the project's
 * configuration.
 * <p>
 * The plugin is run <em>in the build's own JVM</em> ({@code EMBED}), which is
 * what keeps the application a direct child of the daemon: a forked deploy mode
 * would make it a grandchild, and {@link AppProcess} exists because exit codes
 * are lost and kills orphan JVMs when that happens. Since the application JVM
 * is then Maven's JVM, the agents and the JVM flags travel in
 * {@code MAVEN_OPTS} and the JDK choice travels in {@code JAVA_HOME} - the
 * plugin's own {@code jvmArgs} parameter is no use here, because it only
 * applies to a fork.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class MavenGoalRuntime implements AppRuntime {

    private final Launch launch;
    private final ServerPlugin plugin;
    private final Reactor.PluginConfig declared;
    private final Launch.Log log;

    MavenGoalRuntime(Launch launch, ServerPlugin plugin,
            Reactor.PluginConfig declared, Launch.Log log) {
        this.launch = launch;
        this.plugin = plugin;
        this.declared = declared;
        this.log = log;
    }

    @Override
    public String name() {
        return plugin.name();
    }

    @Override
    public Invocation invocation(Launch.Project project, List<String> jvmFlags,
            List<String> systemProperties) throws IOException {
        Reactor reactor = launch.reactor();
        List<String> command = new ArrayList<>();
        command.add(launch.mavenCommand().toString());
        // -nsu rather than -o: the run has to be able to fetch the plugin the
        // first time, and unlike the classpath resolve there is no cheap retry
        // for it - a failure here is an application that never started.
        command.addAll(List.of("-B", "-ntp", "-nsu"));
        if (reactor.isMultiModule()) {
            // The same -pl/-am the classpath resolve uses, and load-bearing for
            // the same reason: only a module whose compile phase ran in this
            // session gets its target/classes substituted for its installed
            // jar, so without it an edit in a sibling module never reaches the
            // running application.
            command.addAll(
                    List.of("-f", reactor.root().resolve("pom.xml").toString(),
                            "-pl", ":" + reactor.app().artifactId(), "-am"));
        }
        command.addAll(Launch.extraMavenArguments());
        command.addAll(configurationOverride());
        // The run goal forks a lifecycle as far as test-compile, and the loop
        // has no use for test sources - or for their failing to compile.
        command.add("-Dmaven.test.skip=true");
        if (reactor.isMultiModule()) {
            command.add("compile");
        }
        command.add(plugin.goalSpecification(declared));
        plugin.goalProperties()
                .forEach((key, value) -> command.add("-D" + key + "=" + value));
        // Read by the application long after Maven has started, so the command
        // line is the right place for them - and the right place precisely
        // because ProcessBuilder passes each one as its own argument, which
        // MAVEN_OPTS cannot do for a value containing a space.
        command.addAll(systemProperties);

        writeHotswapAgentProperties(project);

        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("MAVEN_OPTS",
                mavenOpts(System.getenv("MAVEN_OPTS"), jvmFlags));
        // The application JVM is Maven's JVM, so this is the only way the
        // JetBrains Runtime that Jvm chose is the one the application runs on -
        // and with it, enhanced class redefinition.
        environment.put("JAVA_HOME", launch.appJvm().home().toString());
        // The launcher derives maven.multiModuleProjectDirectory by walking up
        // from the working directory looking for .mvn, and the working
        // directory here is the application module rather than the reactor
        // root. Setting it explicitly takes that guess out of the picture.
        environment.put("MAVEN_BASEDIR", reactor.root().toString());
        return new Invocation(command, environment, false);
    }

    /**
     * Puts the disabled-plugin list where the application's own class loader
     * will find it.
     * <p>
     * {@code -DdisabledPlugins} does not reach it. HotswapAgent decides whether
     * a plugin may transform a class by asking the {@code PluginConfiguration}
     * of <em>that class's own class loader</em>, and it builds one per loader
     * from a {@code hotswap-agent.properties} found on it. A servlet
     * container's webapp loader has no such file and, inside a build plugin's
     * class realm, no sight of the system properties either - so the list was
     * accepted in silence and disabled nothing for exactly the classes it was
     * meant to protect. Measured against a WAR starter: HotswapAgent's own
     * Vaadin plugin went on transforming {@code VaadinService} and
     * {@code VaadinServlet}, which is the second driver of page reloads the
     * loop disables it to prevent.
     * <p>
     * The same file carries {@code extraClasspath}, naming the HotswapAgent jar
     * itself. HotswapAgent copies its <em>plugin</em> classes into the
     * application's class loader so that instrumented code can reach them, and
     * leaves the rest of itself to be found through that loader's parent chain
     * - which inside a build plugin's class realm never reaches the system
     * class path. Measured: the CGLIB recorder the Proxy plugin copies in
     * failed to initialise on {@code AgentLogger}, on every redefine, and apply
     * read the trace as the application having thrown. With the jar on the
     * loader's own class path the rest of HotswapAgent is one lookup away, and
     * every plugin - Proxy included - works there. What that loader ends up
     * loading of it is the logging classes and nothing that carries state.
     * <p>
     * Written into the module's compiled output, which is the webapp loader's
     * {@code WEB-INF/classes}, so the file is found exactly where it is needed.
     * It is inert anywhere else: nothing reads it unless HotswapAgent is on the
     * JVM, which no production run has. A {@code clean} removes it like any
     * other build output.
     */
    private void writeHotswapAgentProperties(Launch.Project project) {
        Path classes = project.app().classesDir();
        try {
            Files.createDirectories(classes);
            Files.writeString(classes.resolve("hotswap-agent.properties"),
                    "# Written by the Vaadin dev loop; see MavenGoalRuntime."
                            + System.lineSeparator()
                            + "# Only read when HotswapAgent is on the JVM, so"
                            + " it does nothing in a normal build."
                            + System.lineSeparator() + "extraClasspath="
                            + launch.ensureHotswapAgent().toUri()
                            + System.lineSeparator() + "disabledPlugins="
                            + Launch.DISABLED_HOTSWAP_PLUGINS
                            + System.lineSeparator());
        } catch (IOException e) {
            // Not fatal: the application still starts, and the consequence is
            // noise in its log rather than a loop that cannot work.
            log.line("WARNING: could not write hotswap-agent.properties to "
                    + classes + " (" + e.getMessage()
                    + "); HotswapAgent's own plugins may compete with apply");
        }
    }

    /**
     * Makes the plugin's configuration what this run needs, whatever the pom
     * says.
     * <p>
     * A {@code <configuration>} value in the pom beats the user property the
     * same parameter exposes, so the {@code -D} settings above lose to anything
     * the project pins - and a rescanner left running redeploys the webapp
     * underneath an apply that has already reported success. Requiring every
     * project to write {@code <scan>0</scan>} would be a requirement on humans
     * and agents that a tool should not need, so the daemon puts its own jar on
     * {@code maven.ext.class.path} and lets
     * {@link com.vaadin.flow.devloop.mavenext.DevLoopBuildExtension} edit the
     * effective model in memory instead. Nothing is written to the project.
     * <p>
     * Empty when the daemon is running from an exploded build directory rather
     * than a jar, which is the one case there is nothing to point Maven at; the
     * warnings then stand as the fallback.
     */
    private List<String> configurationOverride() {
        return launch.agentJar().filter(Files::isRegularFile)
                .map(jar -> List.of("-Dmaven.ext.class.path=" + jar,
                        "-D" + DevLoopBuildExtension.PLUGIN_PROPERTY + "="
                                + plugin.groupId() + ":" + plugin.artifactId(),
                        "-D" + DevLoopBuildExtension.FORCE_PROPERTY + "="
                                + plugin.forcedConfiguration()))
                .orElseGet(List::of);
    }

    @Override
    public boolean serving(String line) {
        return plugin.serving().matcher(line).find();
    }

    @Override
    public OptionalInt port(String line) {
        Matcher matcher = plugin.serving().matcher(line);
        if (!matcher.find() || matcher.groupCount() < 1) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    /**
     * What this project configures that the dev loop needs but, in this one
     * case, cannot override.
     * <p>
     * Normally there is nothing to say: a {@code <configuration>} value in the
     * pom beats the {@code -D} settings above, so the build extension rewrites
     * it instead and the developer need do nothing. The extension needs a jar
     * to point Maven at, though, and a daemon running from an exploded build
     * directory has none - so that is the case this warns about, rather than
     * asking every project to change its pom.
     */
    @Override
    public List<String> warnings() {
        if (!configurationOverride().isEmpty()) {
            return List.of();
        }
        List<String> warnings = new ArrayList<>();
        for (ServerPlugin.Competing competing : plugin.competing()) {
            Optional<String> configured = declared
                    .configured(competing.element());
            if (configured.isEmpty()
                    || competing.acceptable().stream().anyMatch(value -> value
                            .equalsIgnoreCase(configured.get()))) {
                continue;
            }
            warnings.add("WARNING: " + plugin.artifactId() + " is configured "
                    + "with <" + competing.element() + ">" + configured.get()
                    + "</" + competing.element() + ">, and this daemon is "
                    + "not running from a jar so it cannot override that for "
                    + "you - " + competing.consequence() + ". Please "
                    + competing.fix() + ".");
        }
        return warnings;
    }

    /**
     * Keeps HotswapAgent out of the build tool's own class loaders.
     * <p>
     * Running the application inside the build's JVM means the build's class
     * loaders are in that JVM too, and one of them matters: the Vaadin Maven
     * plugin scans the project through a loader of its own, which therefore
     * holds a second copy of the application's classes. HotswapAgent tries to
     * instrument those copies and cannot - that loader cannot see HotswapAgent
     * itself - so every apply logged
     * {@code NoClassDefFoundError: org/hotswap/agent/logging/AgentLogger} from
     * a plugin transformer, and the apply escalated to a restart over an error
     * that had nothing to do with the change.
     * <p>
     * A regular expression over the loader's class name, comma-separated, and
     * merged from the system properties exactly as {@code disabledPlugins} is -
     * so a wrong name here would be accepted in silence and exclude nothing.
     * <p>
     * And the two {@code --add-opens}: the price of the {@code extraClasspath}
     * that lets HotswapAgent see itself from the webapp class loader; see
     * {@link #writeHotswapAgentProperties}.
     */
    @Override
    public List<String> extraJvmFlags() {
        // Unescaped dots: this is a regular expression, and a dot matching any
        // character rather than only a dot cannot widen it onto anything else.
        return List.of(
                "-DexcludedClassLoaderPatterns="
                        + "com.vaadin.flow.plugin.maven.Reflector.*",
                // What HotswapAgent needs in order to honour the
                // extraClasspath written by writeHotswapAgentProperties:
                // it swaps URLClassLoader.ucp for a javassist proxy of
                // jdk.internal.loader.URLClassPath, and both packages
                // have to be open to it. Without the second one it fails
                // in silence, at debug level - which is how the setting
                // was once concluded not to work at all.
                "--add-opens", "java.base/java.net=ALL-UNNAMED", "--add-opens",
                "java.base/jdk.internal.loader=ALL-UNNAMED");
    }

    /**
     * {@code MAVEN_OPTS} for the run: what the developer's environment already
     * says, and then the loop's own flags.
     * <p>
     * Replacing the inherited value would be a silent change to how this
     * project builds. {@code MAVEN_OPTS} is where a developer puts the heap the
     * build needs, the proxy or trust store it goes through, and any flag their
     * own toolchain requires - and a run started through the dev loop is still
     * that project's build, so dropping them would make the loop fail where a
     * plain {@code mvn} run works, for reasons nothing in the log would
     * explain.
     * <p>
     * The loop's flags go last, because the JVM lets the later of two
     * conflicting flags win: a heap size in the environment is honoured, while
     * the agent, the opens and the redefinition switch cannot be turned off by
     * an inherited value. {@code -javaagent} is additive, so nothing here
     * displaces an agent the developer asked for.
     * <p>
     * Not checked by {@link #unsplittable}: a space in the inherited value
     * breaks it exactly as it breaks a plain {@code mvn} run, which is the
     * developer's own arrangement and not something this loop introduced.
     *
     * @param inherited
     *            {@code MAVEN_OPTS} as the daemon's environment has it, which
     *            may be {@code null} or blank
     * @param jvmFlags
     *            the flags the loop needs the application JVM to start with
     * @return the value to launch with
     */
    static String mavenOpts(String inherited, List<String> jvmFlags) {
        String needed = String.join(" ", jvmFlags);
        if (inherited == null || inherited.isBlank()) {
            return needed;
        }
        return needed.isEmpty() ? inherited.strip()
                : inherited.strip() + " " + needed;
    }

    /**
     * Whether a JVM flag can survive the trip through {@code MAVEN_OPTS}.
     * <p>
     * Maven's launcher expands {@code $MAVEN_OPTS} unquoted, so the shell
     * splits it on whitespace and no amount of quoting inside the value
     * survives. A flag holding a path with a space in it therefore reaches the
     * JVM as two broken arguments. Nothing here can fix that, so the flag is
     * named instead of failing later as a JVM that would not start.
     *
     * @param flags
     *            the flags bound for {@code MAVEN_OPTS}
     * @return one warning per flag that cannot survive, empty when all can
     */
    static List<String> unsplittable(List<String> flags) {
        List<String> warnings = new ArrayList<>();
        for (String flag : flags) {
            if (flag.chars().anyMatch(Character::isWhitespace)) {
                warnings.add("WARNING: " + flag + " contains a space, and "
                        + "Maven splits MAVEN_OPTS on whitespace, so the "
                        + "application JVM will not receive it intact. Move "
                        + "the file it names to a path without spaces.");
            }
        }
        return warnings;
    }
}
