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
package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DeprecatedPropertyMigrationTest {

    private PrepareFrontendMojo mojo;
    private Log mockLog;
    private MavenProject project;
    private MojoDescriptor mojoDescriptor;
    private MavenSession session;

    @BeforeEach
    void setup() throws Exception {
        mojo = new PrepareFrontendMojo();
        mockLog = Mockito.mock(Log.class);
        ReflectionUtils.setVariableValueInObject(mojo, "log", mockLog);

        project = new MavenProject();
        ReflectionUtils.setVariableValueInObject(mojo, "project", project);
        ReflectionUtils.setVariableValueInObject(mojo, "projectBasedir",
                new File("."));

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setArtifacts(List.of());
        mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);

        addParameter("nodeDownloadRoot", "${vaadin.node.download.root}");
        addParameter("nodeVersion", "${vaadin.node.version}");
        addParameter("pnpmEnable", "${vaadin.pnpm.enable}");
        addParameter("bunEnable", "${vaadin.bun.enable}");
        addParameter("useGlobalPnpm", "${vaadin.pnpm.global}");
        addParameter("requireHomeNodeExec", "${vaadin.require.home.node}");
        addParameter("nodeFolder", "${vaadin.node.folder}");
        addParameter("projectBuildDir", "${vaadin.build.folder}");
        addParameter("postinstallPackages",
                "${vaadin.npm.postinstallPackages}");
        addParameter("frontendHotdeploy", "${vaadin.frontend.hotdeploy}");
        addParameter("reactEnable", "${vaadin.react.enable}");
        addParameter("npmExcludeWebComponents",
                "${vaadin.npm.excludeWebComponents}");
        addParameter("frontendExtraFileExtensions",
                "${vaadin.devmode.frontendExtraFileExtensions}");
        addParameter("applicationIdentifier",
                "${vaadin.applicationIdentifier}");
        addParameter("skip", "${vaadin.skip}");

        DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
        session = new MavenSession(null, request,
                new DefaultMavenExecutionResult(), List.of());
        session.setCurrentProject(project);
    }

    private void addParameter(String name, String expression) throws Exception {
        Parameter param = new Parameter();
        param.setName(name);
        param.setExpression(expression);
        mojoDescriptor.addParameter(param);
    }

    private void setUserProperty(String key, String value) {
        session.getUserProperties().setProperty(key, value);
    }

    private void resolve() {
        DeprecatedPropertyResolver.resolve(mojo, mojoDescriptor, session,
                project);
    }

    @Test
    void oldStringProperty_acceptedWithWarning() throws Exception {
        setUserProperty("node.download.root", "https://example.com/dist/");

        resolve();

        assertEquals("https://example.com/dist/", ReflectionUtils
                .getValueIncludingSuperclasses("nodeDownloadRoot", mojo));
        verify(mockLog).warn(contains("'node.download.root' is deprecated"));
        verify(mockLog).warn(contains("'vaadin.node.download.root' instead"));
    }

    @Test
    void oldBooleanProperty_acceptedWithWarning() throws Exception {
        setUserProperty("pnpm.enable", "true");

        resolve();

        assertEquals(true, ReflectionUtils
                .getValueIncludingSuperclasses("pnpmEnable", mojo));
        verify(mockLog).warn(contains("'pnpm.enable' is deprecated"));
    }

    @Test
    void oldBoxedBooleanProperty_acceptedWithWarning() throws Exception {
        setUserProperty("react.enable", "true");

        resolve();

        assertEquals(Boolean.TRUE, ReflectionUtils
                .getValueIncludingSuperclasses("reactEnable", mojo));
        verify(mockLog).warn(contains("'react.enable' is deprecated"));
    }

    @Test
    void oldListProperty_acceptedWithWarning() throws Exception {
        setUserProperty("npm.postinstallPackages", "pkg1,pkg2");

        resolve();

        @SuppressWarnings("unchecked")
        List<String> packages = (List<String>) ReflectionUtils
                .getValueIncludingSuperclasses("postinstallPackages", mojo);
        assertTrue(packages.contains("pkg1"));
        assertTrue(packages.contains("pkg2"));
        verify(mockLog)
                .warn(contains("'npm.postinstallPackages' is deprecated"));
    }

    @Test
    void newPropertyOnly_noWarning() throws Exception {
        setUserProperty("vaadin.node.download.root",
                "https://example.com/dist/");

        resolve();

        verify(mockLog, never()).warn(Mockito.any(CharSequence.class));
    }

    @Test
    void bothOldAndNew_newWins() throws Exception {
        setUserProperty("node.download.root", "https://old.example.com/");
        setUserProperty("vaadin.node.download.root",
                "https://new.example.com/");

        // Simulate Maven having already set the field via the new property
        ReflectionUtils.setVariableValueInObject(mojo, "nodeDownloadRoot",
                "https://new.example.com/");

        resolve();

        assertEquals("https://new.example.com/", ReflectionUtils
                .getValueIncludingSuperclasses("nodeDownloadRoot", mojo));
        verify(mockLog).warn(contains("'node.download.root'"));
        verify(mockLog).warn(contains("will be ignored"));
    }

    @Test
    void notMigratedProperty_noWarning() throws Exception {
        // "skip" was never a valid Vaadin property name; vaadin.skip always
        // had the prefix. Setting -Dskip=true must not trigger a warning.
        setUserProperty("skip", "true");

        resolve();

        assertEquals(Boolean.FALSE,
                ReflectionUtils.getValueIncludingSuperclasses("skip", mojo));

        verify(mockLog, never()).warn(Mockito.any(CharSequence.class));
    }

    @Test
    void oldPropertyViaProjectProperties_acceptedWithWarning()
            throws Exception {
        project.getProperties().setProperty("node.download.root",
                "https://project.example.com/");

        resolve();

        assertEquals("https://project.example.com/", ReflectionUtils
                .getValueIncludingSuperclasses("nodeDownloadRoot", mojo));
        verify(mockLog).warn(contains("'node.download.root' is deprecated"));
    }
}
