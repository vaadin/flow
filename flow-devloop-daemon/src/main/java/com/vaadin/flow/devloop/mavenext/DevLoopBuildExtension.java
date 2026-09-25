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
package com.vaadin.flow.devloop.mavenext;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Overrides the server plugin's configuration for the run the dev loop drives,
 * without touching the project's pom.
 * <p>
 * Maven gives a {@code <configuration>} value in the pom precedence over the
 * user property the same parameter exposes, so {@code -Djetty.scan=0} on the
 * command line does nothing to a project that writes {@code <scan>2</scan>},
 * which a generated WAR starter does by default. The plugin then redeploys the
 * webapp on a schedule of its own, the second driver of what the transaction
 * model owns: measured, one {@code apply} that reported a clean hot swap was
 * followed by the plugin tearing the context down and rebuilding it, losing
 * every page's state for a change that was already live.
 * <p>
 * Asking every project to write {@code <scan>0</scan>} is not a fix - it is a
 * requirement on humans and agents that a tool should not need. A build
 * extension is the supported way to change a model the daemon does not own: it
 * runs inside the application's own build, before any mojo, and edits the
 * effective plugin configuration in memory. Nothing is written to the project.
 * <p>
 * A second plugin shape needs more than a rewrite: Cargo's run mojo exposes no
 * user property on any parameter that could carry the loop's JVM flags, and
 * reads a Maven project property named {@code cargo.*} instead. So the
 * extension also sets project properties for the run, named one at a time by
 * {@link #PROPERTY_PREFIX}.
 * <p>
 * A third needs the goal itself. A goal named on a Maven command line runs in
 * every project in the reactor, and TomEE's run mojo has no {@code skip} and no
 * packaging check to escape that with - named, it starts a server in the
 * reactor root and blocks the build before the application module is built. A
 * goal <em>bound to a phase</em> runs only where the model carries it, so for
 * that shape the daemon names no goal at all and asks the extension to add the
 * execution here, in the application's module alone; see
 * {@link #BIND_PROPERTY}.
 * <p>
 * Both are applied to the one module the daemon names in
 * {@link #MODULE_PROPERTY} and to no other; see there for why an inherited
 * plugin makes that scope load-bearing rather than tidy.
 * <p>
 * This class lives in the daemon's jar because that jar's path is the one thing
 * the daemon always knows - it is already its own {@code -javaagent} - so
 * putting the extension anywhere else would mean a second artifact to resolve
 * and keep in step. It is compiled against Maven's API at {@code provided}
 * scope and is never loaded in the daemon's own JVM; only Maven ever sees it.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class DevLoopBuildExtension extends AbstractMavenLifecycleParticipant {

    /** {@code groupId:artifactId} of the plugin to reconfigure. */
    public static final String PLUGIN_PROPERTY = "vaadin.devloop.ext.plugin";

    /**
     * {@code artifactId} of the module the daemon runs that plugin in.
     * <p>
     * Everything the extension forces is applied to that module and to no
     * other, and the reason is that {@code getBuildPlugins} is the
     * <em>effective</em> model: a plugin a reactor parent declares is in every
     * module that inherits it, the reactor root among them. Reconfiguring each
     * one would undo the very thing the reconfiguration is for - Cargo, WildFly
     * and both Payaras are switched off across the reactor by a
     * {@code -D<name>.skip=true} and switched back on for the application by a
     * forced {@code <skip>false</skip>}, so a forced {@code false} everywhere
     * would start a server in the reactor root, or fail there on a packaging
     * that builds no deployment, before the build ever reached the
     * application's own module.
     * <p>
     * The artifactId alone, because that is what the daemon already names the
     * module with: the run is {@code -pl :<artifactId> -am}, so a reactor in
     * which two modules answered to it is one Maven could not have been asked
     * to run this goal in to begin with.
     */
    public static final String MODULE_PROPERTY = "vaadin.devloop.ext.module";

    /**
     * What to force on it, as {@code element=value} pairs separated by
     * semicolons. Passed rather than hard-coded so that the values stay in
     * {@code ServerPlugin}, where the rest of a server's description lives, and
     * a second container needs no change here.
     */
    public static final String FORCE_PROPERTY = "vaadin.devloop.ext.force";

    /**
     * Prefix of a setting naming a Maven <em>project</em> property to put on
     * the model, as {@code <prefix><name>=<value>}.
     * <p>
     * The channel for a plugin whose parameter carries no user property at all,
     * which is the shape Cargo has: every element of its run mojo that could
     * hold the loop's JVM flags is nested and settable from a pom only, while
     * any project property named {@code cargo.*} is read and injected as a
     * container configuration property. Setting one is therefore the only way
     * in, and doing it here rather than in the pom leaves the project
     * untouched, exactly as {@link #FORCE_PROPERTY} does.
     * <p>
     * One setting per property, rather than the {@code name=value} list
     * {@link #FORCE_PROPERTY} uses, because there is no separator that would be
     * safe: a value carried this way holds the application's whole JVM command
     * line, and on Windows {@code -Dvaadin.devloop.classes=} alone contains
     * semicolons.
     */
    public static final String PROPERTY_PREFIX = "vaadin.devloop.ext.property.";

    /**
     * A goal to bind in that module, as {@code <phase>:<goal>}.
     * <p>
     * How a goal is kept to one module when the plugin offers no way of its
     * own. {@code -pl} chooses which projects are in the reactor, not which of
     * them a named goal runs in - it runs in all of them - and no command line
     * can say otherwise. A goal bound to a phase runs where the model carries
     * it, and the model is what this extension edits. So the daemon names the
     * phase alone and asks for the goal here.
     * <p>
     * Added to the plugin's executions rather than replacing them: the project
     * may have executions of its own, and this run is still that project's
     * build.
     * <p>
     * It is given a copy of the plugin's own {@code <configuration>}, and that
     * is not a convenience. Maven merges a plugin-level configuration into each
     * execution's while it is <em>building the model</em>, and afterwards
     * consults the plugin-level one for a goal named on the command line alone
     * ({@code DefaultLifecycleExecutionPlanCalculator} passes
     * {@code allowPluginLevelConfig} only for
     * {@code MojoExecution.Source.CLI}). An execution added after the model was
     * built has missed that merge and would run on the mojo's defaults.
     * Measured: {@code <tomeeHttpPort>8892</tomeeHttpPort>} and
     * {@code <context>ROOT</context>} were both in the effective model and the
     * server still came up on 8080 under the module's {@code finalName}.
     * <p>
     * Within a phase Maven runs executions in the order their plugins appear in
     * the effective model, and merges the lifecycle-injected ones in first - so
     * {@code war:war} is already scheduled ahead of an execution added to a
     * pom-declared plugin at {@code package}, which is what the goal needs: it
     * deploys the WAR that phase builds.
     */
    public static final String BIND_PROPERTY = "vaadin.devloop.ext.bind";

    /**
     * The id given to the execution {@link #BIND_PROPERTY} adds, which is also
     * what makes adding it twice a no-op.
     */
    static final String BOUND_EXECUTION = "vaadin-devloop-run";

    /**
     * Where each module's effective model is left, relative to the module's own
     * directory.
     * <p>
     * The daemon reads poms with the JDK's XML parser and no Maven at all,
     * which settles most questions but not the two Maven alone can answer:
     * which profiles are active, and what a module inherits from a parent
     * outside the checkout. Both are already decided by the time this runs, so
     * writing them down costs nothing and saves the daemon from guessing.
     * <p>
     * Relative to the module and not to {@code ${project.build.directory}},
     * deliberately: the reader is the daemon, which has no Maven to ask where
     * that directory points, so a project that moves it -
     * {@code <build><directory>build</directory></build>} - would have the two
     * ends looking at different paths and the model would read as simply
     * missing. {@code target/devloop/} is where the daemon keeps everything
     * else it writes per module, and it is the same path on the reading side;
     * see {@code EffectiveModel#FILE}.
     */
    public static final String MODEL_FILE = "target/devloop/model.properties";

    /*
     * System.out is the only sink this module may use: the enforcer rule in its
     * pom bans every logging framework, so that the daemon jar can be put on
     * maven.ext.class.path - and on Maven's own stdout is where a line about
     * the build belongs anyway. Hence java:S106 here and below.
     */
    @Override
    @SuppressWarnings("java:S106")
    public void afterProjectsRead(MavenSession session) {
        // Never fatal. An extension that throws fails the whole build, and the
        // application not starting at all would be a far worse outcome than a
        // rescanner competing with apply - which the daemon warns about anyway.
        // Recorded before anything is overridden, so the file describes the
        // project as Maven resolved it rather than as the dev loop bent it.
        try {
            session.getProjects().forEach(DevLoopBuildExtension::writeModel);
        } catch (RuntimeException | LinkageError e) {
            System.out.println("[vaadin-dev] could not record the effective "
                    + "model: " + e);
        }
        try {
            reconfigure(session);
        } catch (RuntimeException | LinkageError e) {
            System.out.println("[vaadin-dev] could not override the server "
                    + "plugin's configuration: " + e);
        }
    }

    /**
     * Writes one module's effective model where the daemon looks for it.
     * <p>
     * Written in {@code afterProjectsRead}, which is before the first mojo
     * runs: a build that then fails to compile still leaves a current answer,
     * and the answer is what the daemon needs in order to decide how to launch
     * the application at all.
     * <p>
     * A {@link Properties} file rather than a format of its own, because the
     * daemon may use nothing but the JDK to read it and a configuration value
     * is the developer's own text - it can hold anything, newlines included,
     * and {@code Properties} is what escapes it correctly at both ends.
     *
     * @param project
     *            the module, as Maven resolved it
     */
    static void writeModel(MavenProject project) {
        File basedir = project.getBasedir();
        if (basedir == null) {
            // A model assembled in memory rather than read from a pom: there is
            // no module directory to be relative to, and no daemon watching one
            // either.
            return;
        }
        Path file = basedir.toPath().resolve(MODEL_FILE);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                modelOf(project).store(writer,
                        "Written by the Vaadin dev loop: " + project.getId()
                                + " as this build resolved it. Regenerated on "
                                + "every resolve, and read by nothing else.");
            }
        } catch (IOException | RuntimeException e) {
            // Never fatal, for the same reason the override above is not: the
            // daemon falls back to reading the poms itself.
            System.out.println("[vaadin-dev] could not write " + file + ": "
                    + e.getMessage());
        }
    }

    /**
     * The effective model, reduced to what the daemon asks Maven about.
     * <p>
     * {@code getBuildPlugins} is the whole point of going through Maven: it is
     * the plugins this build really runs, with the active profiles applied and
     * inheritance resolved through parents the daemon cannot even see. A
     * {@code <pluginManagement>} entry is not in it, which is the distinction
     * the daemon could not draw on its own.
     *
     * @param project
     *            the module, as Maven resolved it
     * @return the properties to store
     */
    static Properties modelOf(MavenProject project) {
        Properties values = new Properties();
        values.setProperty("artifactId",
                String.valueOf(project.getArtifactId()));
        values.setProperty("packaging", String.valueOf(project.getPackaging()));
        List<String> profiles = new ArrayList<>();
        for (Profile profile : project.getActiveProfiles()) {
            profiles.add(profile.getId());
        }
        values.setProperty("profiles", String.join(",", profiles));
        List<Plugin> plugins = project.getBuildPlugins();
        values.setProperty("plugins", String.valueOf(plugins.size()));
        for (int index = 0; index < plugins.size(); index++) {
            Plugin plugin = plugins.get(index);
            String key = "plugin." + index;
            values.setProperty(key, plugin.getGroupId() + ":"
                    + plugin.getArtifactId() + ":"
                    + (plugin.getVersion() == null ? "" : plugin.getVersion()));
            // The plugin's own configuration first and its executions' after
            // it, later winning: configuration that would fight the dev loop
            // counts wherever it is declared, exactly as when the daemon reads
            // the pom itself.
            configurationOf(plugin.getConfiguration(), key, values);
            for (PluginExecution execution : plugin.getExecutions()) {
                configurationOf(execution.getConfiguration(), key, values);
            }
        }
        return values;
    }

    /** The simple children of one {@code <configuration>}, if there is one. */
    private static void configurationOf(Object configuration, String key,
            Properties values) {
        if (!(configuration instanceof Xpp3Dom dom)) {
            return;
        }
        for (Xpp3Dom child : dom.getChildren()) {
            if (child.getValue() != null) {
                values.setProperty(key + "." + child.getName(),
                        child.getValue().trim());
            }
        }
    }

    /**
     * One of the two settings above, as the daemon passed it.
     * <p>
     * A {@code -D} on a Maven command line is a <em>user</em> property. It also
     * turns up among the system properties, but only through a
     * {@code System.setProperty} that Maven 3's CLI documents as deprecated and
     * that Maven 4 does not do - so reading the system properties alone would
     * one day leave the override silently undone, with the project's own
     * {@code <scan>} surviving and the rescanner redeploying underneath an
     * apply that had already reported a hot swap. The user properties are
     * therefore the answer, and the system properties stay as the fallback for
     * a Maven that was given the setting in its JVM rather than on its command
     * line.
     *
     * @param session
     *            the build in progress
     * @param name
     *            the property to read
     * @return its value, or {@code null} when this build was not given one
     */
    private static String property(MavenSession session, String name) {
        String value = session.getUserProperties().getProperty(name);
        return value != null ? value
                : session.getSystemProperties().getProperty(name);
    }

    @SuppressWarnings("java:S106")
    private void reconfigure(MavenSession session) {
        String coordinates = property(session, PLUGIN_PROPERTY);
        if (coordinates == null || coordinates.isBlank()) {
            return;
        }
        String module = property(session, MODULE_PROPERTY);
        if (module == null || module.isBlank()) {
            // Never fatal, for the reason afterProjectsRead gives: a run whose
            // plugin keeps the pom's configuration is a degraded run, which
            // the daemon warns about, while one that reconfigured every module
            // in the reactor could fail before the application had started.
            System.out.println("[vaadin-dev] no application module was named, "
                    + "so the server plugin's configuration is left as the "
                    + "project wrote it");
            return;
        }
        String force = property(session, FORCE_PROPERTY);
        boolean forcing = force != null && !force.isBlank();
        String bind = property(session, BIND_PROPERTY);
        boolean binding = bind != null && !bind.isBlank();
        Map<String, String> properties = projectProperties(session);
        if (!forcing && !binding && properties.isEmpty()) {
            return;
        }
        int colon = coordinates.indexOf(':');
        if (colon < 0) {
            return;
        }
        String groupId = coordinates.substring(0, colon);
        String artifactId = coordinates.substring(colon + 1);
        for (MavenProject project : session.getProjects()) {
            if (!module.equals(project.getArtifactId())) {
                // Any other module in the reactor has the plugin by
                // inheritance alone, and is exactly where the goal has to stay
                // switched off.
                continue;
            }
            for (Plugin plugin : project.getBuildPlugins()) {
                if (groupId.equals(plugin.getGroupId())
                        && artifactId.equals(plugin.getArtifactId())) {
                    if (forcing) {
                        apply(project, plugin, force);
                    }
                    applyProperties(project, plugin, properties);
                    if (binding) {
                        bind(project, plugin, bind);
                    }
                }
            }
        }
    }

    /**
     * The project properties this run was asked to set, by name.
     * <p>
     * The user properties are collected last and so win, for the reason
     * {@link #property} gives: a {@code -D} on a Maven command line is a user
     * property, and its appearing among the system properties too is a Maven 3
     * convenience that Maven 4 does not repeat.
     *
     * @param session
     *            the build in progress
     * @return the properties to set, empty when this build was asked for none
     */
    static Map<String, String> projectProperties(MavenSession session) {
        Map<String, String> asked = new LinkedHashMap<>();
        collect(session.getSystemProperties(), asked);
        collect(session.getUserProperties(), asked);
        return asked;
    }

    private static void collect(Properties from, Map<String, String> into) {
        if (from == null) {
            return;
        }
        for (String name : from.stringPropertyNames()) {
            if (name.startsWith(PROPERTY_PREFIX)
                    && name.length() > PROPERTY_PREFIX.length()) {
                into.put(name.substring(PROPERTY_PREFIX.length()),
                        from.getProperty(name));
            }
        }
    }

    /**
     * Puts those properties on one module's model, keeping what the pom already
     * had.
     * <p>
     * Added to rather than replaced, and that is not politeness. The property
     * this exists for is {@code cargo.jvmargs}, which is a perfectly ordinary
     * thing for a project to have written for itself - the heap the container
     * needs, a trust store, a flag its own stack requires - and a dev-loop run
     * is still that project's build. Replacing it would make the loop fail
     * where a plain {@code mvn cargo:run} works, for a reason nothing in the
     * log would explain.
     * <p>
     * The loop's value goes last, because these reach a JVM command line where
     * the later of two conflicting flags wins: a heap size the pom asks for is
     * honoured, while the agents cannot be switched off by one. That is the
     * same precedence {@code MavenGoalRuntime.mavenOpts} applies to
     * {@code MAVEN_OPTS}, for the same reason.
     * <p>
     * Values are never logged. The value carries the application's whole JVM
     * command line, and that command line carries the token the daemon
     * authenticates the application with; the name alone is enough to say what
     * happened.
     *
     * @param project
     *            the module running the plugin
     * @param plugin
     *            the plugin the daemon named, for the message
     * @param properties
     *            the properties to set
     */
    @SuppressWarnings("java:S106")
    private void applyProperties(MavenProject project, Plugin plugin,
            Map<String, String> properties) {
        Properties model = project.getProperties();
        properties.forEach((name, value) -> {
            String existing = model.getProperty(name);
            boolean adding = existing != null && !existing.isBlank();
            String effective = adding ? existing.strip() + " " + value : value;
            if (!effective.equals(existing)) {
                // Said out loud for the same reason a forced element is: this
                // run is not the build the pom describes.
                System.out.println("[vaadin-dev] " + plugin.getArtifactId()
                        + ": " + (adding ? "adding to" : "setting")
                        + " the project property " + name + " for this run in "
                        + project.getArtifactId());
            }
            model.setProperty(name, effective);
        });
    }

    /**
     * Binds one goal of that plugin to a phase in that module.
     * <p>
     * {@link #BIND_PROPERTY} says why this is the only way to keep such a goal
     * to one module, and why the execution needs a copy of the plugin's
     * configuration. Three things are this method's own.
     * <p>
     * It must survive being asked twice - Maven may read a model more than once
     * and two bound executions would be two servers on one port - so an
     * execution already carrying the id is reused rather than added beside.
     * <p>
     * The copy is taken after {@link #apply} has run, because {@code apply}
     * strips a forced element from every execution's configuration, which for
     * this one would strip the value the copy exists to carry.
     * <p>
     * {@code flushExecutionMap} because {@link Plugin} caches its executions by
     * id the first time it is asked for them, and a map built before this ran
     * would not have the new one in it.
     *
     * @param project
     *            the module running the plugin
     * @param plugin
     *            the plugin to add the execution to
     * @param bind
     *            the binding, as {@code <phase>:<goal>}
     */
    @SuppressWarnings("java:S106")
    private void bind(MavenProject project, Plugin plugin, String bind) {
        int colon = bind.indexOf(':');
        if (colon <= 0 || colon == bind.length() - 1) {
            return;
        }
        String phase = bind.substring(0, colon).trim();
        String goal = bind.substring(colon + 1).trim();
        PluginExecution execution = null;
        for (PluginExecution existing : plugin.getExecutions()) {
            if (BOUND_EXECUTION.equals(existing.getId())) {
                execution = existing;
            }
        }
        boolean added = execution == null;
        if (added) {
            execution = new PluginExecution();
            execution.setId(BOUND_EXECUTION);
            execution.setPhase(phase);
            execution.addGoal(goal);
            plugin.addExecution(execution);
            plugin.flushExecutionMap();
        }
        if (plugin.getConfiguration() instanceof Xpp3Dom configuration) {
            execution.setConfiguration(new Xpp3Dom(configuration));
        }
        if (!added) {
            return;
        }
        // Said out loud for the reason a forced element is: this run is not the
        // build the pom describes.
        System.out.println("[vaadin-dev] " + plugin.getArtifactId()
                + ": running " + goal + " at " + phase + " in "
                + project.getArtifactId() + " for this run (the command line "
                + "names no goal, so it runs in that module and no other)");
    }

    @SuppressWarnings("java:S106")
    private void apply(MavenProject project, Plugin plugin, String force) {
        Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
        if (configuration == null) {
            configuration = new Xpp3Dom("configuration");
            plugin.setConfiguration(configuration);
        }
        for (String pair : force.split(";")) {
            int equals = pair.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String element = pair.substring(0, equals).trim();
            String value = pair.substring(equals + 1).trim();
            Xpp3Dom child = configuration.getChild(element);
            if (child == null) {
                child = new Xpp3Dom(element);
                configuration.addChild(child);
            }
            if (!value.equals(child.getValue())) {
                // Said out loud, in the application's own log: the dev loop is
                // overriding something the project asked for, and that is not
                // something to do silently.
                System.out.println("[vaadin-dev] " + plugin.getArtifactId()
                        + ": using <" + element + ">" + value + "</" + element
                        + "> for this run (the pom says "
                        + (child.getValue() == null ? "nothing"
                                : ("<" + element + ">" + child.getValue() + "</"
                                        + element + ">"))
                        + ") in " + project.getArtifactId());
            }
            child.setValue(value);
        }
        // Executions carry their own configuration and it wins over the
        // plugin's, so a value pinned there would survive everything above.
        plugin.getExecutions().forEach(execution -> {
            Xpp3Dom own = (Xpp3Dom) execution.getConfiguration();
            if (own != null) {
                for (String pair : force.split(";")) {
                    int equals = pair.indexOf('=');
                    if (equals > 0) {
                        removeChild(own, pair.substring(0, equals).trim());
                    }
                }
            }
        });
    }

    private static void removeChild(Xpp3Dom parent, String name) {
        for (int index = parent.getChildCount() - 1; index >= 0; index--) {
            if (name.equals(parent.getChild(index).getName())) {
                parent.removeChild(index);
            }
        }
    }
}
