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
import java.time.Duration;
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
 * Jetty is run <em>in the build's own JVM</em> ({@code EMBED}), which is what
 * keeps the application a direct child of the daemon. The application JVM is
 * then Maven's JVM, so the agents and the JVM flags travel in
 * {@code MAVEN_OPTS} and the JDK choice travels in {@code JAVA_HOME}.
 * <p>
 * WildFly, TomEE, both Payaras, Liberty and Cargo offer no such mode: each
 * starts the server as a process of its own, so the application is a
 * grandchild. Stopping one is still reliable, because {@link AppProcess} ends a
 * launch descendants-first; what is lost is the application's own exit code,
 * since the code the daemon waits on is Maven's. For those the agents, the JVM
 * flags and the settings the application reads all travel in the plugin's own
 * parameter - see {@link #forkedJvmFlags} - because neither Maven's command
 * line nor its environment reaches a JVM that Maven forked.
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
        // Jetty serves the module's own output, so there is nothing to build
        // for it beyond the compile a multi-module reactor already needs. A
        // container that deploys a packaged WAR needs that WAR to exist, and
        // names the phase that makes one.
        // An entry whose own goal forks the packaging names no phase, but that
        // fork covers the application's module alone - a sibling stops at the
        // phase named here. At `compile` it has no jar, and maven-war-plugin
        // writes its target/classes directory into WEB-INF/lib under the jar's
        // name: an empty entry, and the sibling's classes missing at runtime.
        // So a forked container names `package` either way.
        String phase = plugin.phase();
        if (phase.isBlank() && reactor.isMultiModule()) {
            phase = plugin.embedded() ? "compile" : "package";
        }
        if (!phase.isBlank()) {
            command.add(phase);
        }
        command.add(plugin.goalSpecification(declared));
        goalProperties()
                .forEach((key, value) -> command.add("-D" + key + "=" + value));
        if (plugin.embedded()) {
            // Read by the application long after Maven has started, so the
            // command line is the right place for them - and the right place
            // precisely because ProcessBuilder passes each one as its own
            // argument, which MAVEN_OPTS cannot do for a value containing a
            // space.
            command.addAll(systemProperties);
        } else {
            command.addAll(forkedJvmFlags(jvmFlags, systemProperties));
        }

        writeHotswapAgentProperties(project);

        Map<String, String> environment = new LinkedHashMap<>();
        // The inherited value is kept either way; the loop's own flags are
        // added only when the application is going to run in this JVM. Adding
        // them for a forked server would put the agents on Maven instead,
        // where they would find no application and instrument nothing.
        environment.put("MAVEN_OPTS", mavenOpts(System.getenv("MAVEN_OPTS"),
                plugin.embedded() ? jvmFlags : List.of()));
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
     * The {@code -D} settings that keep the plugin in the shape the loop needs.
     * <p>
     * The table's, except for an entry that switches its own goal off for the
     * whole reactor so that the extension can switch it back on for the
     * application's own module alone - Cargo and both Payaras. Passing the
     * first half without the second would start nothing at all, which is worse
     * than the degraded run {@link #warnings()} describes, so when there is no
     * extension neither half is sent and the goal runs everywhere instead. That
     * is what the warning about it is for.
     *
     * @return the settings to pass
     */
    private Map<String, String> goalProperties() {
        if (plugin.skippedOutsideTheApplication()
                && configurationOverride().isEmpty()) {
            return Map.of();
        }
        return plugin.goalProperties();
    }

    /**
     * Everything a forked server's JVM must start with, as {@code -D} settings.
     * <p>
     * Neither channel the embedded case uses reaches a forked server:
     * {@code MAVEN_OPTS} starts Maven, and a {@code -D} on Maven's command line
     * sets a property in Maven's JVM. So both the agents and the settings the
     * application reads - the daemon's port and token among them - travel in
     * the plugin's own parameter, which is the one thing a forked server hands
     * on to the process it starts.
     * <p>
     * Joined with spaces and passed as one setting, because that is how most of
     * those plugins take it: WildFly's {@code setJavaOpts} splits the value on
     * whitespace, TomEE parses {@code args} the way a shell would, and Cargo
     * runs its own copy of Ant's {@code translateCommandline} over it. Liberty
     * is the exception - its channel carries one flag per property, so there
     * the same flags go out as one setting each; see {@link #flagProperties}.
     * Either way a value with a space in it is as unsplittable as in
     * {@code MAVEN_OPTS}, a {@code jvm.options} line being split on whitespace
     * too. The flags are already reported by {@code Launch}; the settings are
     * not, and only reach this channel for a forked server, so they are checked
     * here.
     * <p>
     * The name on the left of the {@code =} is the plugin's own parameter for a
     * plugin that exposes one, and otherwise a request to the build extension
     * to put that name on the project's model - see {@link #warnings()} for
     * what happens when there is no extension to ask.
     *
     * @param jvmFlags
     *            the flags the loop needs the application JVM to start with
     * @param systemProperties
     *            the {@code -D} settings the application reads
     * @return the arguments carrying them, one for most channels
     */
    private List<String> forkedJvmFlags(List<String> jvmFlags,
            List<String> systemProperties) throws IOException {
        unsplittable(systemProperties, splitter()).forEach(log::line);
        List<String> forked = new ArrayList<>(jvmFlags);
        forked.addAll(systemProperties);
        List<String> tokens = singleToken(forked);
        if (plugin.commaSplitFlags()) {
            tokens = withoutCommas(tokens);
        }
        if (plugin.shellEscapedFlags()) {
            tokens = tokens.stream().map(MavenGoalRuntime::escapeBackslashes)
                    .toList();
        }
        if (plugin.perPropertyFlags()) {
            return flagProperties(flagsSetting(), tokens);
        }
        return List.of("-D" + flagsSetting() + "=" + String.join(" ", tokens));
    }

    /**
     * One {@code -D} per flag, for a channel that carries a single flag in each
     * property.
     * <p>
     * Liberty's shape, and the reason {@link #forkedJvmFlags} returns a list
     * rather than one argument. Every Maven property named
     * {@code liberty.jvm.<key>} becomes one line of the server's generated
     * {@code jvm.options}, so the flags cannot be joined - a line holding all
     * of them would reach the JVM as a single argument. The key only has to
     * make the names distinct, so it is the flag's position, which also makes
     * the launch line the daemon logs read in the order the flags were composed
     * in.
     * <p>
     * The order they are <em>written</em> in is not this method's to decide:
     * the plugin reads them out of a {@code Properties}, whose iteration order
     * is unspecified. That is why {@link #singleToken} has already folded every
     * module option onto its value by the time this is reached.
     *
     * @param prefix
     *            the property name to append each flag's position to
     * @param tokens
     *            the flags, one per property
     * @return the settings to pass, one per flag
     */
    static List<String> flagProperties(String prefix, List<String> tokens) {
        List<String> settings = new ArrayList<>();
        for (int index = 0; index < tokens.size(); index++) {
            settings.add("-D" + prefix + index + "=" + tokens.get(index));
        }
        return settings;
    }

    /**
     * What splits this plugin's channel, as a clause reading "and ...".
     * <p>
     * Naming {@code MAVEN_OPTS} would send a reader looking in the wrong place
     * for a forked server, and so would naming the property for a channel that
     * is not one value but a file the plugin writes from many.
     *
     * @return the clause for {@link #unsplittable}
     */
    private String splitter() {
        if (plugin.perPropertyFlags()) {
            return plugin.artifactId() + " writes " + plugin.jvmFlagsProperty()
                    + "* into the server's jvm.options, whose lines its "
                    + "launcher splits on whitespace";
        }
        return plugin.artifactId() + " splits " + plugin.jvmFlagsProperty()
                + " on whitespace";
    }

    /**
     * Moves every flag with a comma in it into a JVM argument file.
     * <p>
     * For a channel Maven splits on commas - a plugin parameter declared
     * {@code List<String>}, which is Payara Server's shape - a comma does not
     * merely divide the value. Maven's converter splits on it with no escaping
     * available, and the plugin then makes a key and a value of each piece at
     * its first {@code =} and <em>discards any piece that has none</em>. So
     * {@code -DdisabledPlugins=Vaadin,Spring,SpringBoot,Jetty} would reach the
     * server as {@code -DdisabledPlugins=Vaadin}, with three quarters of it
     * gone and nothing logged. Measured against nothing yet - the mechanism is
     * read out of Plexus's converter and the mojo, and that is exactly why it
     * is worth writing down rather than discovering later as HotswapAgent's
     * Vaadin plugin quietly competing with every apply.
     * <p>
     * A JVM argument file is the way out, and it costs the channel nothing: the
     * JVM expands {@code @file} itself, so the plugin passes the token through
     * as one more argument, and the file's own quoting carries a comma without
     * further ado. Only the flags that need it go there, so the rest stay
     * visible in the launch line the daemon logs.
     * <p>
     * Written per module beside everything else the daemon keeps under
     * {@code target/devloop/}, and named after the runtime so two of them could
     * never collide.
     *
     * @param tokens
     *            the flags as they would have been passed inline
     * @return the same flags, with the comma-bearing ones replaced by one
     *         {@code @file} token
     * @throws IOException
     *             if the argument file cannot be written
     */
    private List<String> withoutCommas(List<String> tokens) throws IOException {
        long affected = tokens.stream().filter(MavenGoalRuntime::hasComma)
                .count();
        if (affected == 0) {
            return tokens;
        }
        Path file = Launch.workDir(launch.reactor().app().dir())
                .resolve(plugin.name() + "-args.txt");
        List<String> reduced = withCommasInArgFile(tokens, file);
        // The path travels in the same whitespace-separated value as the rest,
        // so a space in it breaks exactly as a space in any other flag does.
        unsplittable(List.of("@" + file), splitter()).forEach(log::line);
        log.line(affected + " flag(s) with a comma in them go to " + file
                + ", which " + plugin.jvmFlagsProperty()
                + " cannot carry intact");
        return reduced;
    }

    /**
     * The same flags, with the comma-bearing ones moved into an argument file.
     *
     * @param tokens
     *            the flags as they would have been passed inline
     * @param file
     *            the argument file to write, if any flag needs one
     * @return the flags to pass inline, with one {@code @file} token at the end
     *         when the file was written
     * @throws IOException
     *             if the argument file cannot be written
     */
    static List<String> withCommasInArgFile(List<String> tokens, Path file)
            throws IOException {
        List<String> inline = new ArrayList<>();
        List<String> quoted = new ArrayList<>();
        for (String token : tokens) {
            (hasComma(token) ? quoted : inline).add(token);
        }
        if (quoted.isEmpty()) {
            return inline;
        }
        AppProcess.writeArgFile(file, quoted);
        inline.add("@" + file);
        return inline;
    }

    private static boolean hasComma(String flag) {
        return flag.indexOf(',') >= 0;
    }

    /**
     * What the {@code -D} carrying the flags is called on Maven's command line.
     *
     * @return the plugin's own user property, or the setting that asks the
     *         build extension for a project property of that name
     */
    private String flagsSetting() {
        return plugin.projectPropertyFlags()
                ? DevLoopBuildExtension.PROPERTY_PREFIX
                        + plugin.jvmFlagsProperty()
                : plugin.jvmFlagsProperty();
    }

    /**
     * Doubles every backslash, for a plugin that unescapes what it is given.
     * <p>
     * A no-op anywhere a path has no backslashes in it, which is everywhere but
     * Windows - and on Windows it is the difference between the agent jar being
     * found and the JVM refusing to start. See {@code ServerPlugin}'s TomEE
     * entry for the parser this answers.
     *
     * @param flag
     *            one flag
     * @return the flag with its backslashes doubled
     */
    private static String escapeBackslashes(String flag) {
        return flag.replace("\\", "\\\\");
    }

    /**
     * Folds a module option and its value into the one token {@code --x=y}.
     * <p>
     * A JVM takes {@code --add-opens java.base/java.io=ALL-UNNAMED} as two
     * arguments or as one with an {@code =} between them, and for
     * {@code MAVEN_OPTS} either does. Not so for a forked server: WildFly sorts
     * what it is handed into module options and the rest before building the
     * command line, and two-token options come apart in the sorting. Measured,
     * seven {@code --add-opens} arrived in a row ahead of their seven values
     * and the server JVM refused to start at all -
     * {@code Error: --add-opens requires modules to be specified}.
     * <p>
     * The {@code =} form cannot be separated from its value by anything that
     * reorders whole tokens, which is why WildFly writes its own module options
     * that way too.
     *
     * @param flags
     *            the flags, as a JVM command line would take them
     * @return the same flags, with every module option in one token
     */
    static List<String> singleToken(List<String> flags) {
        List<String> folded = new ArrayList<>();
        for (int index = 0; index < flags.size(); index++) {
            String flag = flags.get(index);
            boolean wantsValue = flag.startsWith("--add-")
                    && flag.indexOf('=') < 0 && index + 1 < flags.size();
            folded.add(wantsValue ? flag + "=" + flags.get(++index) : flag);
        }
        return folded;
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
            // extraClasspath only for an embedded server. It is what the two
            // --add-opens in extraJvmFlags pay for, and those do not go to a
            // forked server; writing it anyway would ask HotswapAgent for a
            // swap it has not been given the access to make, and log a failure
            // for it on every start.
            String extraClasspath = plugin.embedded()
                    ? "extraClasspath=" + launch.ensureHotswapAgent().toUri()
                            + System.lineSeparator()
                    : "";
            Files.writeString(classes.resolve("hotswap-agent.properties"),
                    "# Written by the Vaadin dev loop; see MavenGoalRuntime."
                            + System.lineSeparator()
                            + "# Only read when HotswapAgent is on the JVM, so"
                            + " it does nothing in a normal build."
                            + System.lineSeparator() + extraClasspath
                            + "disabledPlugins="
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
     * The application's own module is named alongside the plugin, because the
     * effective model the extension edits is the one Maven resolved: a plugin a
     * reactor parent declares belongs to every module that inherits it, and an
     * entry the loop switches off across the reactor - Cargo, WildFly, both
     * Payaras - would be switched straight back on in the reactor root by the
     * very {@code <skip>false</skip>} that is meant for the application alone.
     * It is the same {@code artifactId} the {@code -pl :<app>} above selects.
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
                        "-D" + DevLoopBuildExtension.MODULE_PROPERTY + "="
                                + launch.reactor().app().artifactId(),
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
     * <p>
     * For a plugin whose JVM flags travel as a project property that jar is not
     * a nicety but the whole channel: without it the agents never reach the
     * server at all, and the loop would run as a build-and-restart loop without
     * saying why. That one is first, because it is the one that stops the loop
     * working rather than merely competing with it.
     */
    @Override
    public List<String> warnings() {
        boolean rewritten = !configurationOverride().isEmpty();
        List<String> warnings = new ArrayList<>();
        if (plugin.projectPropertyFlags() && !rewritten) {
            warnings.add("WARNING: " + plugin.artifactId() + " takes the JVM "
                    + "flags for the server it starts from the Maven project "
                    + "property " + plugin.jvmFlagsProperty() + ", which no "
                    + "command line can set - only the dev loop's build "
                    + "extension can, and this daemon is not running from a "
                    + "jar so it has none. The agents the loop needs will be "
                    + "dropped, and every apply will restart instead of hot "
                    + "reloading. Please run the daemon from its jar.");
        } else if (plugin.skippedOutsideTheApplication() && !rewritten) {
            // Only when the warning above has not already said it: that one
            // ends in the same instruction, and a second copy of it would be
            // noise rather than news.
            warnings.add("WARNING: a goal named on a Maven command line runs "
                    + "on every module in the reactor, and " + plugin.goal()
                    + " has to be switched off for all but the application's "
                    + "own - which only the dev loop's build extension can do, "
                    + "and this daemon is not running from a jar so it has "
                    + "none. The reactor root will start a server of its own "
                    + "with nothing deployed in it, and the start will time "
                    + "out. Please run the daemon from its jar.");
        }
        for (ServerPlugin.Competing competing : plugin.competing()) {
            // The extension can only force a constant, so an entry that names
            // no acceptable value is beyond it: those name a parameter the
            // loop needs for itself, whose value is composed per launch. That
            // one is worth saying even when the extension is in play.
            boolean forceable = !competing.acceptable().isEmpty();
            if (rewritten && forceable) {
                continue;
            }
            Optional<String> configured = declared
                    .configured(competing.element());
            if (configured.isEmpty()
                    || competing.acceptable().stream().anyMatch(value -> value
                            .equalsIgnoreCase(configured.get()))) {
                continue;
            }
            String why = forceable
                    ? "and this daemon is not running from a jar so it "
                            + "cannot override that for you"
                    : "which the dev loop needs for itself";
            warnings.add("WARNING: " + plugin.artifactId() + " is configured "
                    + "with <" + competing.element() + ">" + configured.get()
                    + "</" + competing.element() + ">, " + why + " - "
                    + competing.consequence() + ". Please " + competing.fix()
                    + ".");
        }
        return warnings;
    }

    /**
     * Longer for a forked container, which may have to build a server before it
     * can start one.
     * <p>
     * {@code wildfly:run} provisions a server under {@code target/} the first
     * time it runs, laying out a few hundred megabytes of modules, and
     * {@code cargo:run} downloads and unpacks one. Measured against a warm
     * local repository WildFly's still outran the five minutes an embedded
     * start is given, and the loop killed a provision that was making progress
     * - on every first run, since the provision it killed was never finished
     * either. The window is only ever reached when something is wrong, so
     * widening it for the case that is legitimately slow costs a failing start
     * nothing but patience.
     */
    @Override
    public Duration startupTimeout() {
        return plugin.embedded() ? AppRuntime.super.startupTimeout()
                : Duration.ofMinutes(20);
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
     * <p>
     * None of it goes to a forked server, and the opens least of all. They buy
     * HotswapAgent the right to replace {@code ucp} on every
     * {@code URLClassLoader} in the JVM, which is worth it where the webapp
     * loader is one and cannot otherwise see the agent. A container of its own
     * is neither: the agent is already on that JVM's class path, and JBoss
     * Modules hands the deployment a {@code ModuleClassLoader} that the swap
     * could not help in any case. What the swap does reach there is the
     * server's own loaders - measured against WildFly 38, the transactions
     * subsystem then failed to read {@code jbossts-properties.xml} out of its
     * own jar and the boot was unrecoverable, listeners bound and all.
     */
    @Override
    public List<String> extraJvmFlags() {
        if (!plugin.embedded()) {
            // Neither of the embedded flags applies to a forked server, and
            // the opens are actively harmful there; see below.
            return List.of();
        }
        return List.of(
                // Only an embedded server shares its JVM with the build, so
                // only there is the plugin's own loader present to exclude.
                //
                // Unescaped dots: this is a regular expression, and a dot
                // matching any character rather than only a dot cannot widen
                // it onto anything else.
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
        return unsplittable(flags, "Maven splits MAVEN_OPTS on whitespace");
    }

    /**
     * The same, for a channel that is not {@code MAVEN_OPTS}.
     * <p>
     * A forked server is handed its JVM flags in one whitespace-separated
     * value, so the same space breaks the same way - but naming MAVEN_OPTS as
     * the splitter would send a reader looking in the wrong place.
     *
     * @param flags
     *            the values to check
     * @param splitter
     *            what splits them, as a clause reading "and ..."
     * @return one warning per value that cannot survive the trip
     */
    static List<String> unsplittable(List<String> flags, String splitter) {
        List<String> warnings = new ArrayList<>();
        for (String flag : flags) {
            if (flag.chars().anyMatch(Character::isWhitespace)) {
                warnings.add("WARNING: " + flag + " contains a space, and "
                        + splitter + ", so the "
                        + "application JVM will not receive it intact. Move "
                        + "the file it names to a path without spaces.");
            }
        }
        return warnings;
    }
}
