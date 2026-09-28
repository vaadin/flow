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
import java.util.List;
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
     * What to force on it, as {@code element=value} pairs separated by
     * semicolons. Passed rather than hard-coded so that the values stay in
     * {@code ServerPlugin}, where the rest of a server's description lives, and
     * a second container needs no change here.
     */
    public static final String FORCE_PROPERTY = "vaadin.devloop.ext.force";

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

    private void reconfigure(MavenSession session) {
        String coordinates = property(session, PLUGIN_PROPERTY);
        String force = property(session, FORCE_PROPERTY);
        if (coordinates == null || force == null || coordinates.isBlank()
                || force.isBlank()) {
            return;
        }
        int colon = coordinates.indexOf(':');
        if (colon < 0) {
            return;
        }
        String groupId = coordinates.substring(0, colon);
        String artifactId = coordinates.substring(colon + 1);
        for (MavenProject project : session.getProjects()) {
            for (Plugin plugin : project.getBuildPlugins()) {
                if (groupId.equals(plugin.getGroupId())
                        && artifactId.equals(plugin.getArtifactId())) {
                    apply(project, plugin, force);
                }
            }
        }
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
