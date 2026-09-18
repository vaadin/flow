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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The extension's second job: writing down what Maven resolved the module to
 * be. The daemon reads poms with the JDK alone, so this file is the only place
 * an active profile or a parent outside the checkout is a settled fact rather
 * than a guess - which makes its shape part of the contract between the two.
 */
class DevLoopBuildExtensionTest {

    @TempDir
    private Path target;

    @Test
    void theBuildPluginsAreWrittenWithTheirCoordinates() {
        Properties model = DevLoopBuildExtension
                .modelOf(project(jetty("org.eclipse.jetty.ee11",
                        "jetty-ee11-maven-plugin", "12.1.13")));

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
                .modelOf(project(jetty("org.eclipse.jetty.ee10",
                        "jetty-ee10-maven-plugin", null)));

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
        Plugin plugin = jetty("org.eclipse.jetty.ee11",
                "jetty-ee11-maven-plugin", "12.1.13");
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
        MavenProject project = project(jetty("org.eclipse.jetty.ee11",
                "jetty-ee11-maven-plugin", "12.1.13"));
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
    void theFileLandsUnderTheModulesBuildDirectory() throws IOException {
        MavenProject project = project(jetty("org.eclipse.jetty.ee11",
                "jetty-ee11-maven-plugin", "12.1.13"));

        DevLoopBuildExtension.writeModel(project);

        Path file = target.resolve(DevLoopBuildExtension.MODEL_FILE);
        assertTrue(Files.isRegularFile(file), file.toString());
        Properties written = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            written.load(reader);
        }
        assertEquals("org.eclipse.jetty.ee11:jetty-ee11-maven-plugin:12.1.13",
                written.getProperty("plugin.0"));
    }

    private MavenProject project(Plugin... plugins) {
        Build build = new Build();
        build.setDirectory(target.toString());
        for (Plugin plugin : plugins) {
            build.addPlugin(plugin);
        }
        Model model = new Model();
        model.setGroupId("com.example");
        model.setArtifactId("app");
        model.setVersion("1.0");
        model.setPackaging("war");
        model.setBuild(build);
        return new MavenProject(model);
    }

    private static Plugin jetty(String groupId, String artifactId,
            String version) {
        Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        plugin.setVersion(version);
        return plugin;
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
