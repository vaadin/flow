/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.nio.file.Paths;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;

public class GenerateMavenBOMMojoTest {

    private String bomFilename;

    private File resourceOutputDirectory;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private GenerateMavenBOMMojo mojo;

    @Before
    public void setUp() throws Exception {
        this.mojo = new GenerateMavenBOMMojo();

        MavenProject project = Mockito.mock(MavenProject.class);
        File projectBase = temporaryFolder.getRoot();
        Mockito.when(project.getBasedir()).thenReturn(projectBase);
        resourceOutputDirectory = new File(projectBase,
                VAADIN_SERVLET_RESOURCES);
        bomFilename = new File(resourceOutputDirectory, "bom.json")
                .getAbsolutePath();

        // set Mojo properties
        ReflectionUtils.setVariableValueInObject(mojo, "projectType",
                "application");
        ReflectionUtils.setVariableValueInObject(mojo, "schemaVersion", "1.4");
        ReflectionUtils.setVariableValueInObject(mojo, "includeBomSerialNumber",
                true);
        ReflectionUtils.setVariableValueInObject(mojo, "includeCompileScope",
                true);
        ReflectionUtils.setVariableValueInObject(mojo, "includeProvidedScope",
                true);
        ReflectionUtils.setVariableValueInObject(mojo, "includeRuntimeScope",
                true);
        ReflectionUtils.setVariableValueInObject(mojo, "includeTestScope",
                false);
        ReflectionUtils.setVariableValueInObject(mojo, "includeSystemScope",
                true);
        ReflectionUtils.setVariableValueInObject(mojo, "includeLicenseText",
                false);
        ReflectionUtils.setVariableValueInObject(mojo, "outputReactorProjects",
                true);
        ReflectionUtils.setVariableValueInObject(mojo, "excludeTypes",
                new String[0]);
        ReflectionUtils.setVariableValueInObject(mojo, "excludeArtifactId",
                new String[0]);
        ReflectionUtils.setVariableValueInObject(mojo, "excludeGroupId",
                new String[0]);
        ReflectionUtils.setVariableValueInObject(mojo, "outputFormat", "json");
        ReflectionUtils.setVariableValueInObject(mojo, "outputName", "bom");
        ReflectionUtils.setVariableValueInObject(mojo, "outputDirectory",
                resourceOutputDirectory.getAbsolutePath());
    }

    @Test
    public void shouldGenerateSBOM() throws Exception {
        Assert.assertFalse(Files.exists(Paths.get(bomFilename)));
        mojo.execute();
        Assert.assertTrue(Files.exists(Paths.get(bomFilename)));
    }

}
