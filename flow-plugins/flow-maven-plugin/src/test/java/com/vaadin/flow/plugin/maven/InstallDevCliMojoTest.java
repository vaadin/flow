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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarOutputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallDevCliMojoTest {

    @TempDir
    File projectBase;

    private final MavenProject project = Mockito.mock(MavenProject.class);

    private final Log log = Mockito.mock(Log.class);

    @Test
    void execute_installsWithoutTheFlowBuildMachinery() throws Exception {
        // Only the project and the goal's own parameters are set: no
        // MojoExecution, no plugin context, nothing a Reflector or class
        // scanning could be built from. Installing the CLI must not need any.
        Mockito.when(project.getArtifacts()).thenReturn(Set.of());

        mojo(false).execute();

        assertTrue(Files.isRegularFile(
                projectBase.toPath().resolve(".vaadin/vaadin-dev")));
        verifyWarned("dev-loop daemon");
    }

    @Test
    void execute_skipHotswapAgent_doesNotLookForTheDaemon() throws Exception {
        mojo(true).execute();

        assertTrue(Files.isRegularFile(
                projectBase.toPath().resolve(".vaadin/vaadin-dev")));
        Mockito.verify(log)
                .info(ArgumentMatchers.<CharSequence> argThat(message -> message
                        .toString().contains("Skipping HotswapAgent")));
        Mockito.verify(project, Mockito.never()).getArtifacts();
        Mockito.verify(log, Mockito.never())
                .warn(ArgumentMatchers.any(CharSequence.class));
    }

    @Test
    void execute_daemonDependency_provisionsFromThatJar() throws Exception {
        // A daemon jar without the provisioning class, as from a release
        // before it existed: reaching its warning shows the goal found the
        // jar among the dependencies and handed it to the provisioning.
        Path jar = projectBase.toPath().resolve("flow-devloop-daemon.jar");
        try (OutputStream out = new JarOutputStream(
                Files.newOutputStream(jar))) {
            // empty
        }
        Artifact daemon = Mockito.mock(Artifact.class);
        Mockito.when(daemon.getGroupId()).thenReturn("com.vaadin");
        Mockito.when(daemon.getArtifactId()).thenReturn("flow-devloop-daemon");
        Mockito.when(daemon.getFile()).thenReturn(jar.toFile());
        Mockito.when(project.getArtifacts()).thenReturn(Set.of(daemon));

        mojo(false).execute();

        verifyWarned("older than this plugin");
    }

    private InstallDevCliMojo mojo(boolean skipHotswapAgent) throws Exception {
        InstallDevCliMojo mojo = new InstallDevCliMojo();
        mojo.setLog(log);
        ReflectionUtils.setVariableValueInObject(mojo, "project", project);
        ReflectionUtils.setVariableValueInObject(mojo, "targetDirectory",
                projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "skipHotswapAgent",
                skipHotswapAgent);
        return mojo;
    }

    private void verifyWarned(String fragment) {
        Mockito.verify(log).warn(ArgumentMatchers.<CharSequence> argThat(
                message -> message.toString().contains(fragment)));
    }
}
