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
import java.nio.file.Files;
import java.util.Set;

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

    @Test
    void execute_installsWithoutTheFlowBuildMachinery() throws Exception {
        // Only the project and the goal's own parameters are set: no
        // MojoExecution, no plugin context, nothing a Reflector or class
        // scanning could be built from. Installing the CLI must not need any.
        MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getArtifacts()).thenReturn(Set.of());
        Log log = Mockito.mock(Log.class);
        InstallDevCliMojo mojo = new InstallDevCliMojo();
        mojo.setLog(log);
        ReflectionUtils.setVariableValueInObject(mojo, "project", project);
        ReflectionUtils.setVariableValueInObject(mojo, "targetDirectory",
                projectBase);

        mojo.execute();

        assertTrue(Files.isRegularFile(
                projectBase.toPath().resolve(".vaadin/vaadin-dev")));
        Mockito.verify(log).warn(ArgumentMatchers.<CharSequence> argThat(
                message -> message.toString().contains("dev-loop daemon")));
    }
}
