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

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
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
        try {
            reconfigure(session);
        } catch (RuntimeException | LinkageError e) {
            System.out.println("[vaadin-dev] could not override the server "
                    + "plugin's configuration: " + e);
        }
    }

    private void reconfigure(MavenSession session) {
        String coordinates = session.getSystemProperties()
                .getProperty(PLUGIN_PROPERTY);
        String force = session.getSystemProperties()
                .getProperty(FORCE_PROPERTY);
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
