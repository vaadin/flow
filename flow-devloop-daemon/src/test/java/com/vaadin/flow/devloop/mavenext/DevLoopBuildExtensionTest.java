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

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The extension's second job: writing down what Maven resolved the module to
 * be. The daemon reads poms with the JDK alone, so this file is the only place
 * an active profile or a parent outside the checkout is a settled fact rather
 * than a guess - which makes its shape part of the contract between the two.
 */
class DevLoopBuildExtensionTest {

    /** The artifactId every module built here has unless it is renamed. */
    private static final String APP = "app";

    @TempDir
    private Path module;

    @Test
    void theBuildPluginsAreWrittenWithTheirCoordinates() {
        Properties model = DevLoopBuildExtension
                .modelOf(project(jetty("ee11", "12.1.13")));

        assertEquals("1", model.getProperty("plugins"));
        assertEquals("org.eclipse.jetty.ee11:jetty-ee11-maven-plugin:12.1.13",
                model.getProperty("plugin.0"));
        assertEquals("war", model.getProperty("packaging"));
    }

    /**
     * A plugin with no version is Maven's business to pin, and the daemon has
     * to be able to tell that from a version it should pass on.
     */
    @Test
    void aPluginWithNoVersionLeavesTheCoordinatesOpen() {
        Properties model = DevLoopBuildExtension
                .modelOf(project(jetty("ee10", null)));

        assertEquals("org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:",
                model.getProperty("plugin.0"));
    }

    /**
     * Configuration is what the loop checks for values that would fight it, and
     * an execution's counts as much as the plugin's own - it is what Maven will
     * act on when that execution runs.
     */
    @Test
    void configurationIsWrittenForThePluginAndItsExecutions() {
        Plugin plugin = jetty("ee11", "12.1.13");
        plugin.setConfiguration(configuration("scan", "2"));
        PluginExecution execution = new PluginExecution();
        execution.setConfiguration(configuration("deployMode", "FORK"));
        plugin.addExecution(execution);

        Properties model = DevLoopBuildExtension.modelOf(project(plugin));

        assertEquals("2", model.getProperty("plugin.0.scan"));
        assertEquals("FORK", model.getProperty("plugin.0.deployMode"));
    }

    /** The profiles Maven ran with, which is the answer poms cannot give. */
    @Test
    void theActiveProfilesAreNamed() {
        MavenProject project = project(jetty("ee11", "12.1.13"));
        project.setActiveProfiles(List.of(profile("jetty"), profile("ide")));

        assertEquals("jetty,ide",
                DevLoopBuildExtension.modelOf(project).getProperty("profiles"));
    }

    /** Nothing declared is an answer too: it says the project runs none. */
    @Test
    void aModuleWithNoBuildPluginsSaysSo() {
        Properties model = DevLoopBuildExtension.modelOf(project());

        assertEquals("0", model.getProperty("plugins"));
        assertNull(model.getProperty("plugin.0"));
    }

    @Test
    void theFileLandsWhereTheDaemonLooksForIt() throws IOException {
        MavenProject project = project(module.resolve("target"),
                jetty("ee11", "12.1.13"));

        DevLoopBuildExtension.writeModel(project);

        assertEquals("org.eclipse.jetty.ee11:jetty-ee11-maven-plugin:12.1.13",
                writtenModel().getProperty("plugin.0"));
    }

    /**
     * The reader is the daemon, which resolves this path against the module and
     * has no Maven to ask where the build directory was moved to. A project
     * that moves it must still be found, so the file does not move with it.
     */
    @Test
    void aMovedBuildDirectoryDoesNotMoveTheFile() throws IOException {
        MavenProject project = project(module.resolve("build"),
                jetty("ee11", "12.1.13"));

        DevLoopBuildExtension.writeModel(project);

        assertEquals("org.eclipse.jetty.ee11:jetty-ee11-maven-plugin:12.1.13",
                writtenModel().getProperty("plugin.0"));
        assertFalse(Files.exists(module.resolve("build")),
                "nothing belongs under the configured build directory");
    }

    /**
     * A model assembled in memory has no module directory to be relative to,
     * and no daemon watching one either: writing it anywhere would be a guess.
     */
    @Test
    void aProjectWithNoModuleDirectoryWritesNothing() {
        MavenProject project = project(module.resolve("target"));
        project.setFile(null);

        DevLoopBuildExtension.writeModel(project);

        assertFalse(Files.exists(module.resolve("target")));
    }

    /**
     * The override the whole extension exists for: the daemon names the plugin
     * and what to force on it, and a value the project declared is replaced for
     * this run. {@code -D} on a Maven command line is a user property, which is
     * where it is read from.
     */
    @Test
    void aUserPropertyForcesTheConfiguration() {
        Plugin jetty = jetty("ee11", "12.1.13");
        jetty.setConfiguration(configuration("scan", "2"));

        afterProjectsRead(
                userProperties("org.eclipse.jetty.ee11:jetty-ee11-maven-plugin",
                        "scan=0"),
                new Properties(), project(jetty));

        assertEquals("0", forced(jetty, "scan"));
    }

    /**
     * Maven 3's CLI copies every {@code -D} into the system properties as well,
     * and a daemon may equally have put the setting in Maven's own JVM. Both
     * still work; only the order changed.
     */
    @Test
    void aSystemPropertyStillForcesTheConfiguration() {
        Plugin jetty = jetty("ee11", "12.1.13");
        jetty.setConfiguration(configuration("scan", "2"));

        afterProjectsRead(new Properties(),
                userProperties("org.eclipse.jetty.ee11:jetty-ee11-maven-plugin",
                        "scan=0"),
                project(jetty));

        assertEquals("0", forced(jetty, "scan"));
    }

    /** An element the project never declared is added rather than skipped. */
    @Test
    void anUndeclaredElementIsAdded() {
        Plugin jetty = jetty("ee11", "12.1.13");

        afterProjectsRead(
                userProperties("org.eclipse.jetty.ee11:jetty-ee11-maven-plugin",
                        "scan=0;deployMode=EMBED"),
                new Properties(), project(jetty));

        assertEquals("0", forced(jetty, "scan"));
        assertEquals("EMBED", forced(jetty, "deployMode"));
    }

    /**
     * An execution's own configuration wins over the plugin's when that
     * execution runs, so a value pinned there would survive the override.
     */
    @Test
    void anExecutionsOwnValueIsRemoved() {
        Plugin jetty = jetty("ee11", "12.1.13");
        PluginExecution execution = new PluginExecution();
        execution.setConfiguration(configuration("scan", "2"));
        jetty.addExecution(execution);

        afterProjectsRead(
                userProperties("org.eclipse.jetty.ee11:jetty-ee11-maven-plugin",
                        "scan=0"),
                new Properties(), project(jetty));

        assertEquals("0", forced(jetty, "scan"));
        assertNull(((Xpp3Dom) execution.getConfiguration()).getChild("scan"));
    }

    /** Only the plugin the daemon named is touched. */
    @Test
    void anotherPluginIsLeftAlone() {
        Plugin compiler = plugin("org.apache.maven.plugins",
                "maven-compiler-plugin", "3.13.0");
        compiler.setConfiguration(configuration("scan", "2"));

        afterProjectsRead(
                userProperties("org.eclipse.jetty.ee11:jetty-ee11-maven-plugin",
                        "scan=0"),
                new Properties(), project(compiler));

        assertEquals("2", forced(compiler, "scan"));
    }

    /**
     * A build the daemon is not driving is an ordinary build: it must come out
     * exactly as the project wrote it.
     */
    @Test
    void withoutThePropertiesNothingIsForced() {
        Plugin jetty = jetty("ee11", "12.1.13");
        jetty.setConfiguration(configuration("scan", "2"));

        afterProjectsRead(new Properties(), new Properties(), project(jetty));

        assertEquals("2", forced(jetty, "scan"));
    }

    /** Coordinates that name no artifact are not a plugin to look for. */
    @Test
    void coordinatesWithoutAnArtifactForceNothing() {
        Plugin jetty = jetty("ee11", "12.1.13");
        jetty.setConfiguration(configuration("scan", "2"));

        afterProjectsRead(userProperties("jetty-ee11-maven-plugin", "scan=0"),
                new Properties(), project(jetty));

        assertEquals("2", forced(jetty, "scan"));
    }

    /**
     * The second channel: a plugin whose parameter carries no user property at
     * all - Cargo's, whose run mojo takes its JVM flags from a nested element a
     * pom alone can write, and which reads any {@code cargo.*} project property
     * instead. Setting one here is the only way the loop's agents reach the
     * container Cargo starts.
     */
    @Test
    void aUserPropertySetsAProjectProperty() {
        Plugin cargo = cargo("1.10.29");
        MavenProject project = project(cargo);

        afterProjectsRead(
                projectProperties("org.codehaus.cargo:cargo-maven3-plugin",
                        "cargo.jvmargs", "-javaagent:/ha.jar -Dp=1"),
                new Properties(), project);

        assertEquals("-javaagent:/ha.jar -Dp=1",
                project.getProperties().getProperty("cargo.jvmargs"));
    }

    /**
     * A value the project already declared is kept, and the loop's own added
     * after it.
     * <p>
     * Unlike a forced {@code <configuration>} element, which the loop does own
     * for the run, {@code cargo.jvmargs} is an ordinary thing for a project to
     * have written for itself - the heap its container needs, a trust store -
     * and a dev-loop run is still that project's build. Replacing it would make
     * the loop fail where a plain {@code mvn cargo:run} works.
     */
    @Test
    void aDeclaredProjectPropertyIsAddedTo() {
        Plugin cargo = cargo("1.10.29");
        MavenProject project = project(cargo);
        project.getProperties().setProperty("cargo.jvmargs", "-Xmx2g");

        afterProjectsRead(
                projectProperties("org.codehaus.cargo:cargo-maven3-plugin",
                        "cargo.jvmargs", "-javaagent:/ha.jar"),
                new Properties(), project);

        // The loop's flags last: these reach a JVM command line, where the
        // later of two conflicting flags wins, so the pom's heap size is
        // honoured and the agents cannot be switched off by one.
        assertEquals("-Xmx2g -javaagent:/ha.jar",
                project.getProperties().getProperty("cargo.jvmargs"));
    }

    /**
     * And a blank declaration is not something to append to, which would leave
     * the value with a space in front of it and every flag one position out.
     */
    @Test
    void aBlankDeclaredProjectPropertyIsSimplySet() {
        Plugin cargo = cargo("1.10.29");
        MavenProject project = project(cargo);
        project.getProperties().setProperty("cargo.jvmargs", "   ");

        afterProjectsRead(
                projectProperties("org.codehaus.cargo:cargo-maven3-plugin",
                        "cargo.jvmargs", "-javaagent:/ha.jar"),
                new Properties(), project);

        assertEquals("-javaagent:/ha.jar",
                project.getProperties().getProperty("cargo.jvmargs"));
    }

    /**
     * There is nothing to force on Cargo, so the force setting arrives blank -
     * and that must not be read as "this build is not the daemon's", which
     * would leave the project property unset and the agents behind.
     */
    @Test
    void nothingToForceStillSetsTheProjectProperty() {
        Plugin cargo = cargo("1.10.29");
        MavenProject project = project(cargo);
        Properties user = projectProperties(
                "org.codehaus.cargo:cargo-maven3-plugin", "cargo.jvmargs",
                "-javaagent:/ha.jar");
        user.setProperty(DevLoopBuildExtension.FORCE_PROPERTY, "");

        afterProjectsRead(user, new Properties(), project);

        assertEquals("-javaagent:/ha.jar",
                project.getProperties().getProperty("cargo.jvmargs"));
    }

    /**
     * A plugin a reactor parent declares is in the effective model of every
     * module that inherits it, so forcing the configuration wherever the plugin
     * is found would reach the reactor root as well - and for Cargo, WildFly
     * and both Payaras that is the opposite of the intent. Those entries are
     * switched off across the reactor by a {@code *.skip=true} on the command
     * line and switched back on by exactly this forced
     * {@code <skip>false</skip>}, so a root that got one too would start a
     * server, or fail on a packaging that builds no deployment, before the
     * build ever reached the application.
     */
    @Test
    void anInheritedPluginIsForcedInTheApplicationModuleAlone() {
        Plugin inherited = cargo("1.10.29");
        Plugin declared = cargo("1.10.29");
        MavenProject root = module("root", inherited);
        MavenProject app = module(APP, declared);

        afterProjectsRead(
                userProperties("org.codehaus.cargo:cargo-maven3-plugin",
                        "skip=false"),
                new Properties(), root, app);

        assertEquals("false", forced(declared, "skip"));
        assertNull(inherited.getConfiguration());
    }

    /** The project properties are scoped to that module for the same reason. */
    @Test
    void anInheritedPluginSetsThePropertyInTheApplicationModuleAlone() {
        Plugin inherited = cargo("1.10.29");
        Plugin declared = cargo("1.10.29");
        MavenProject root = module("root", inherited);
        MavenProject app = module(APP, declared);

        afterProjectsRead(
                projectProperties("org.codehaus.cargo:cargo-maven3-plugin",
                        "cargo.jvmargs", "-javaagent:/ha.jar"),
                new Properties(), root, app);

        assertEquals("-javaagent:/ha.jar",
                app.getProperties().getProperty("cargo.jvmargs"));
        assertNull(root.getProperties().getProperty("cargo.jvmargs"));
    }

    /**
     * And a build that names no module at all is one nothing can be scoped to,
     * so nothing is rewritten: a run whose plugin keeps the pom's configuration
     * is degraded and said so in the log, while one that rewrote every module
     * could fail before the application had started.
     */
    @Test
    void withoutTheModuleNothingIsForced() {
        Plugin jetty = jetty("ee11", "12.1.13");
        jetty.setConfiguration(configuration("scan", "2"));
        Properties user = userProperties(
                "org.eclipse.jetty.ee11:jetty-ee11-maven-plugin", "scan=0");
        user.remove(DevLoopBuildExtension.MODULE_PROPERTY);

        afterProjectsRead(user, new Properties(), project(jetty));

        assertEquals("2", forced(jetty, "scan"));
    }

    /** A module that does not run the named plugin keeps its own model. */
    @Test
    void anotherModulesPropertiesAreLeftAlone() {
        Plugin compiler = plugin("org.apache.maven.plugins",
                "maven-compiler-plugin", "3.13.0");
        MavenProject project = project(compiler);

        afterProjectsRead(
                projectProperties("org.codehaus.cargo:cargo-maven3-plugin",
                        "cargo.jvmargs", "-javaagent:/ha.jar"),
                new Properties(), project);

        assertNull(project.getProperties().getProperty("cargo.jvmargs"));
    }

    /**
     * The prefix on its own names no property, so it is not one to set - an
     * empty name would go on the model as the empty string.
     */
    @Test
    void thePrefixAloneNamesNothing() {
        Properties user = new Properties();
        user.setProperty(DevLoopBuildExtension.PROPERTY_PREFIX, "value");
        DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setUserProperties(user);
        request.setSystemProperties(new Properties());
        MavenSession session = new MavenSession(null, request,
                new DefaultMavenExecutionResult(), List.of(project()));

        assertTrue(DevLoopBuildExtension.projectProperties(session).isEmpty());
    }

    /**
     * The third channel, and the one that is not a rewrite but the launch
     * itself: TomEE's run mojo has no skip of any kind, so a {@code tomee:run}
     * named on a command line would start a server in every module of the
     * reactor. A goal bound to a phase runs where the model carries it, and
     * this is the model.
     */
    @Test
    void aBoundGoalBecomesAnExecutionOnThatModulesPlugin() {
        Plugin tomee = tomee("10.1.2");

        afterProjectsRead(
                bindProperties("org.apache.tomee.maven:tomee-maven-plugin",
                        "package:run"),
                new Properties(), project(tomee));

        assertEquals(1, tomee.getExecutions().size());
        PluginExecution bound = tomee.getExecutions().get(0);
        assertEquals("package", bound.getPhase());
        assertEquals(List.of("run"), bound.getGoals());
        // Looked up by id at least once by Maven itself, so the cached map has
        // to know about it too.
        assertTrue(tomee.getExecutionsAsMap().containsKey(bound.getId()));
    }

    /**
     * Maven may read a model more than once, and a build that bound the goal
     * twice would start two servers on one port. The id is what makes the
     * second ask a no-op.
     */
    @Test
    void bindingTwiceLeavesOneExecution() {
        Plugin tomee = tomee("10.1.2");
        MavenProject app = project(tomee);
        Properties properties = bindProperties(
                "org.apache.tomee.maven:tomee-maven-plugin", "package:run");

        afterProjectsRead(properties, new Properties(), app);
        afterProjectsRead(properties, new Properties(), app);

        assertEquals(1, tomee.getExecutions().size());
    }

    /**
     * And it is bound in the application's module and in no other, which is the
     * whole point: a plugin a reactor parent declares is in every module that
     * inherits it, and an execution added to the reactor root would start the
     * server there instead.
     */
    @Test
    void anotherModuleGetsNoBoundExecution() {
        Plugin inherited = tomee("10.1.2");

        afterProjectsRead(
                bindProperties("org.apache.tomee.maven:tomee-maven-plugin",
                        "package:run"),
                new Properties(), module("root", inherited));

        assertTrue(inherited.getExecutions().isEmpty());
    }

    /**
     * The project's own executions are that project's business. This run is
     * still its build, so the goal is added to them rather than instead of them
     * - the same reasoning the Cargo project property is added under.
     */
    @Test
    void theProjectsOwnExecutionsSurviveTheBinding() {
        Plugin tomee = tomee("10.1.2");
        PluginExecution own = new PluginExecution();
        own.setId("build-tomee");
        own.setPhase("pre-integration-test");
        own.addGoal("build");
        tomee.addExecution(own);

        afterProjectsRead(
                bindProperties("org.apache.tomee.maven:tomee-maven-plugin",
                        "package:run"),
                new Properties(), project(tomee));

        assertEquals(2, tomee.getExecutions().size());
        assertEquals("build-tomee", tomee.getExecutions().get(0).getId());
    }

    /**
     * Half a binding is not a binding. An execution with no goal would be an
     * invalid model, and failing the build is the one thing this extension may
     * never do.
     */
    @Test
    void aBindingWithNoGoalBindsNothing() {
        Plugin tomee = tomee("10.1.2");

        afterProjectsRead(
                bindProperties("org.apache.tomee.maven:tomee-maven-plugin",
                        "package:"),
                new Properties(), project(tomee));
        afterProjectsRead(
                bindProperties("org.apache.tomee.maven:tomee-maven-plugin",
                        "package"),
                new Properties(), project(tomee));

        assertTrue(tomee.getExecutions().isEmpty());
    }

    /**
     * The bound execution carries the plugin's own configuration, and it has to
     * carry it <em>itself</em>: Maven merges a plugin-level configuration into
     * each execution's while it builds the model, and after that consults the
     * plugin-level one for a goal named on the command line alone. An execution
     * added here has missed that merge. Measured against the TomEE fixture, the
     * cost of getting this wrong was a server on 8080 under the module's
     * finalName, with the pom's {@code <tomeeHttpPort>} and {@code <context>}
     * both sitting in the effective model.
     */
    @Test
    void theBoundExecutionCarriesThePluginsOwnConfiguration() {
        Plugin tomee = tomee("10.1.2");
        tomee.setConfiguration(configuration("tomeeHttpPort", "8892"));

        afterProjectsRead(
                bindProperties("org.apache.tomee.maven:tomee-maven-plugin",
                        "package:run"),
                new Properties(), project(tomee));

        Xpp3Dom bound = (Xpp3Dom) tomee.getExecutions().get(0)
                .getConfiguration();
        assertEquals("8892", bound.getChild("tomeeHttpPort").getValue());
        // A copy and not the same object: a mojo may rewrite what it is given,
        // and the plugin's own configuration is not this execution's to edit.
        assertNotSame(tomee.getConfiguration(), bound);
    }

    /**
     * And it carries the forced value, not the one the pom wrote - which is
     * only true because the copy is taken after the forcing. Taken before, the
     * execution would carry the pom's value and beat the very override the
     * extension exists for; and {@code apply} strips a forced element from
     * every execution's configuration, so a copy made earlier would have lost
     * it either way.
     */
    @Test
    void theBoundExecutionCarriesTheForcedValueRatherThanThePoms() {
        Plugin tomee = tomee("10.1.2");
        tomee.setConfiguration(configuration("reloadOnUpdate", "true"));
        Properties properties = bindProperties(
                "org.apache.tomee.maven:tomee-maven-plugin", "package:run");
        properties.setProperty(DevLoopBuildExtension.FORCE_PROPERTY,
                "reloadOnUpdate=false");

        afterProjectsRead(properties, new Properties(), project(tomee));

        Xpp3Dom bound = (Xpp3Dom) tomee.getExecutions().get(0)
                .getConfiguration();
        assertEquals("false", bound.getChild("reloadOnUpdate").getValue());
    }

    private static Properties bindProperties(String coordinates, String bind) {
        Properties properties = new Properties();
        properties.setProperty(DevLoopBuildExtension.PLUGIN_PROPERTY,
                coordinates);
        properties.setProperty(DevLoopBuildExtension.MODULE_PROPERTY, APP);
        properties.setProperty(DevLoopBuildExtension.BIND_PROPERTY, bind);
        return properties;
    }

    private static Properties projectProperties(String coordinates, String name,
            String value) {
        Properties properties = new Properties();
        properties.setProperty(DevLoopBuildExtension.PLUGIN_PROPERTY,
                coordinates);
        properties.setProperty(DevLoopBuildExtension.MODULE_PROPERTY, APP);
        properties.setProperty(DevLoopBuildExtension.PROPERTY_PREFIX + name,
                value);
        return properties;
    }

    private void afterProjectsRead(Properties user, Properties system,
            MavenProject... projects) {
        DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setUserProperties(user);
        request.setSystemProperties(system);
        MavenSession session = new MavenSession(null, request,
                new DefaultMavenExecutionResult(), List.of(projects));
        new DevLoopBuildExtension().afterProjectsRead(session);
    }

    private static Properties userProperties(String coordinates, String force) {
        Properties properties = new Properties();
        properties.setProperty(DevLoopBuildExtension.PLUGIN_PROPERTY,
                coordinates);
        properties.setProperty(DevLoopBuildExtension.MODULE_PROPERTY, APP);
        properties.setProperty(DevLoopBuildExtension.FORCE_PROPERTY, force);
        return properties;
    }

    private static String forced(Plugin plugin, String element) {
        Xpp3Dom child = ((Xpp3Dom) plugin.getConfiguration()).getChild(element);
        return child == null ? null : child.getValue();
    }

    private Properties writtenModel() throws IOException {
        Path file = module.resolve(DevLoopBuildExtension.MODEL_FILE);
        assertTrue(Files.isRegularFile(file), file.toString());
        Properties written = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            written.load(reader);
        }
        return written;
    }

    private MavenProject project(Plugin... plugins) {
        return project(module.resolve("target"), plugins);
    }

    private MavenProject module(String artifactId, Plugin... plugins) {
        MavenProject project = project(plugins);
        project.getModel().setArtifactId(artifactId);
        return project;
    }

    private MavenProject project(Path buildDirectory, Plugin... plugins) {
        Build build = new Build();
        build.setDirectory(buildDirectory.toString());
        for (Plugin plugin : plugins) {
            build.addPlugin(plugin);
        }
        Model model = new Model();
        model.setGroupId("com.example");
        model.setArtifactId(APP);
        model.setVersion("1.0");
        model.setPackaging("war");
        model.setBuild(build);
        MavenProject project = new MavenProject(model);
        // What gives the project its basedir, exactly as reading a pom does.
        project.setFile(module.resolve("pom.xml").toFile());
        return project;
    }

    /**
     * Any plugin, by coordinates. The named helpers below are the ones the
     * tests reach for; this is here for a plugin that turns up once - the
     * compiler, standing in for "some other plugin in the same build".
     */
    private static Plugin plugin(String groupId, String artifactId,
            String version) {
        Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        plugin.setVersion(version);
        return plugin;
    }

    /**
     * Jetty's plugin at one Jakarta EE level, named the way
     * {@code ServerPlugin.jetty} names it: the level is the only thing that
     * differs between the two entries.
     *
     * @param ee
     *            the Jakarta EE level, as the plugin spells it
     * @param version
     *            the version the pom declares, or {@code null} for a pom that
     *            leaves it to Maven
     */
    private static Plugin jetty(String ee, String version) {
        return plugin("org.eclipse.jetty." + ee,
                "jetty-" + ee + "-maven-plugin", version);
    }

    /** TomEE's plugin, the one entry whose goal the extension binds. */
    private static Plugin tomee(String version) {
        return plugin("org.apache.tomee.maven", "tomee-maven-plugin", version);
    }

    /**
     * Cargo's plugin, the one entry whose JVM flags go on a project property.
     */
    private static Plugin cargo(String version) {
        return plugin("org.codehaus.cargo", "cargo-maven3-plugin", version);
    }

    private static Profile profile(String id) {
        Profile profile = new Profile();
        profile.setId(id);
        return profile;
    }

    private static Xpp3Dom configuration(String name, String value) {
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom child = new Xpp3Dom(name);
        child.setValue(value);
        configuration.addChild(child);
        return configuration;
    }
}
