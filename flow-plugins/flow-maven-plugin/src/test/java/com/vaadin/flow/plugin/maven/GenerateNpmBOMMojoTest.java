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
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.plugin.TestUtils;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.setProject;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;

public class GenerateNpmBOMMojoTest {

    private String bomFilename;

    private File resourceOutputDirectory;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File jarResourcesSource;
    private File nodeModulesDir;

    private GenerateNpmBOMMojo mojo;

    private Lookup lookup;

    @Before
    public void setUp() throws Exception {
        this.mojo = Mockito.spy(new GenerateNpmBOMMojo());

        File projectBase = temporaryFolder.getRoot();
        File frontendDirectory = new File(projectBase, DEFAULT_FRONTEND_DIR);
        resourceOutputDirectory = new File(projectBase,
                VAADIN_SERVLET_RESOURCES);
        jarResourcesSource = new File(projectBase,
                "jar-resources-source/META-INF/frontend");
        jarResourcesSource.mkdirs();
        bomFilename = new File(resourceOutputDirectory, "bom-npm.json")
                .getAbsolutePath();

        nodeModulesDir = new File(temporaryFolder.getRoot(), "node_modules");
        boolean nodeModulesDirCreated = nodeModulesDir.mkdir();
        Assert.assertTrue(nodeModulesDirCreated);

        String manifestFilePath = new File(projectBase, PACKAGE_JSON)
                .getAbsolutePath();
        // set Mojo properties
        ReflectionUtils.setVariableValueInObject(mojo, "ignoreNpmErrors",
                false);
        ReflectionUtils.setVariableValueInObject(mojo, "packageLockOnly",
                false);
        ReflectionUtils.setVariableValueInObject(mojo, "omit", "dev");
        ReflectionUtils.setVariableValueInObject(mojo, "flattenComponents",
                false);
        ReflectionUtils.setVariableValueInObject(mojo, "shortPURLs", false);
        ReflectionUtils.setVariableValueInObject(mojo, "outputReproducible",
                false);
        ReflectionUtils.setVariableValueInObject(mojo, "validate", true);
        ReflectionUtils.setVariableValueInObject(mojo, "mcType", "application");
        ReflectionUtils.setVariableValueInObject(mojo, "outputFormat", "json");
        ReflectionUtils.setVariableValueInObject(mojo, "outputFilePath",
                bomFilename);
        ReflectionUtils.setVariableValueInObject(mojo, "packageManifest",
                manifestFilePath);
        ReflectionUtils.setVariableValueInObject(mojo, "specVersion", "1.4");
        ReflectionUtils.setVariableValueInObject(mojo, "frontendDirectory",
                frontendDirectory);
        ReflectionUtils.setVariableValueInObject(mojo, "projectBasedir",
                projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "projectBuildDir",
                Paths.get(projectBase.toString(), "target").toString());
        ReflectionUtils.setVariableValueInObject(mojo, "nodeVersion",
                FrontendTools.DEFAULT_NODE_VERSION);
        ReflectionUtils.setVariableValueInObject(mojo, "npmFolder",
                projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "productionMode", false);
        Mockito.doReturn(
                Set.of(jarResourcesSource.getParentFile().getParentFile()))
                .when(mojo).getJarFiles();

        FileUtils.fileWrite(manifestFilePath, "UTF-8",
                TestUtils.getInitialPackageJson().toString());
        lookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(new TestEndpointGeneratorTaskFactory()).when(lookup)
                .lookup(EndpointGeneratorTaskFactory.class);
        Mockito.doAnswer(invocation -> {
            Mockito.doReturn(invocation.getArguments()[0]).when(lookup)
                    .lookup(ClassFinder.class);
            return lookup;
        }).when(mojo).createLookup(Mockito.any(ClassFinder.class));

        setProject(mojo, projectBase);
        // Prevent unwanted resources to be present on classpath
        mojo.project.setArtifacts(Set.of());
    }

    @Test
    public void shouldGenerateSBOM() throws Exception {
        Assert.assertFalse(Files.exists(Paths.get(bomFilename)));
        mojo.execute();
        Assert.assertTrue(Files.exists(Paths.get(bomFilename)));
    }

    @Test(expected = MojoFailureException.class)
    public void shouldFailWhenNoPackageJsonIsPresent() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "packageManifest", "");
        mojo.execute();
    }

    @Test
    public void shouldRunNpmInstallIfNodeModulesIsNotPresent()
            throws Exception {
        Assert.assertTrue(nodeModulesDir.delete());
        Assert.assertFalse(nodeModulesDir.exists());
        mojo.execute();
        Assert.assertTrue(nodeModulesDir.exists());
    }

    @Test
    public void shouldSkipNpmInstallIfNodeModulesIsPresent() throws Exception {
        List<String> originalContent = TestUtils
                .listFilesRecursively(nodeModulesDir);

        mojo.execute();

        List<String> newContent = TestUtils
                .listFilesRecursively(nodeModulesDir);
        Assert.assertArrayEquals(originalContent.toArray(),
                newContent.toArray());
    }

}
