/*
 * Copyright 2000-2020 Vaadin Ltd.
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
 *
 */
package com.vaadin.flow.plugin.maven;

import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FLOW_RESOURCES_FOLDER;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static java.io.File.pathSeparator;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.connect.Endpoint;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.TaskGenerateConnect;
import com.vaadin.flow.server.frontend.TaskGenerateOpenApi;
import com.vaadin.flow.server.frontend.fusion.TaskGenerateConnectImpl;
import com.vaadin.flow.server.frontend.fusion.TaskGenerateOpenApiImpl;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

@Ignore
public class BuildFrontendMojoEndpointTest {
  public static final String TEST_PROJECT_RESOURCE_JS = "test_project_resource.js";
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File generatedFolder;
    private File flowResourcesFolder;
    private File projectFrontendResourcesDirectory;
    private File defaultJavaSource;
    private String openApiJsonFile;
    private File generatedTsFolder;

    private final BuildFrontendMojo mojo = Mockito.spy(new BuildFrontendMojo());

    @Before
    public void setup() throws Exception {
        MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getRuntimeClasspathElements())
                .thenReturn(getClassPath());

        File npmFolder = temporaryFolder.getRoot();
        generatedFolder = new File(npmFolder, DEFAULT_GENERATED_DIR);
        flowResourcesFolder = new File(npmFolder, DEFAULT_FLOW_RESOURCES_FOLDER);
        File frontendDirectory = new File(npmFolder, DEFAULT_FRONTEND_DIR);

        projectFrontendResourcesDirectory = new File(npmFolder,
                "flow_resources");

        defaultJavaSource = new File(".", "src/test/java");
        openApiJsonFile = new File(npmFolder,
                "target/generated-resources/openapi.json").getAbsolutePath();
        generatedTsFolder = new File(npmFolder, "frontend/generated");

        Assert.assertTrue("Failed to create a test project resources",
                projectFrontendResourcesDirectory.mkdirs());
        Assert.assertTrue("Failed to create a test project file",
                new File(projectFrontendResourcesDirectory,
                        TEST_PROJECT_RESOURCE_JS).createNewFile());

        ReflectionUtils.setVariableValueInObject(mojo,
                "frontendResourcesDirectory",
                projectFrontendResourcesDirectory);

        ReflectionUtils.setVariableValueInObject(mojo, "project", project);
        ReflectionUtils.setVariableValueInObject(mojo, "generatedFolder",
                generatedFolder);
        ReflectionUtils.setVariableValueInObject(mojo, "frontendDirectory",
                frontendDirectory);
        ReflectionUtils.setVariableValueInObject(mojo,
                "generateEmbeddableWebComponents", false);
        ReflectionUtils.setVariableValueInObject(mojo, "npmFolder", npmFolder);
        ReflectionUtils.setVariableValueInObject(mojo, "generateBundle", false);
        ReflectionUtils.setVariableValueInObject(mojo, "runNpmInstall", false);
        ReflectionUtils.setVariableValueInObject(mojo, "optimizeBundle", true);

        ReflectionUtils.setVariableValueInObject(mojo, "openApiJsonFile",
                new File(npmFolder, "target/generated-resources/openapi.json"));
        ReflectionUtils.setVariableValueInObject(mojo, "applicationProperties",
                new File(npmFolder,
                        "src/main/resources/application.properties"));
        ReflectionUtils.setVariableValueInObject(mojo, "javaSourceFolder",
                defaultJavaSource);
        ReflectionUtils.setVariableValueInObject(mojo, "generatedTsFolder",
                generatedTsFolder);
        ReflectionUtils.setVariableValueInObject(mojo, "nodeVersion",
                FrontendTools.DEFAULT_NODE_VERSION);
        ReflectionUtils.setVariableValueInObject(mojo, "nodeDownloadRoot",
                NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT);

        flowResourcesFolder.mkdirs();
        generatedFolder.mkdirs();

        setProject(mojo, npmFolder);

        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(new TaskGenerateConnectImpl()).when(lookup).lookup(TaskGenerateConnect.class);
        Mockito.doReturn(new TaskGenerateOpenApiImpl()).when(lookup).lookup(TaskGenerateOpenApi.class);
        Mockito.doReturn(lookup).when(mojo).createLookup(Mockito.any(ClassFinder.class));
    }

    static void setProject(AbstractMojo mojo, File baseFolder)
            throws Exception {
        Build buildMock = mock(Build.class);
        when(buildMock.getFinalName()).thenReturn("finalName");
        MavenProject project = mock(MavenProject.class);
        when(project.getBasedir()).thenReturn(baseFolder);
        when(project.getBuild()).thenReturn(buildMock);
        when(project.getRuntimeClasspathElements()).thenReturn(getClassPath());
        ReflectionUtils.setVariableValueInObject(mojo, "project", project);
    }

    @Test
    public void mavenGoal_generateOpenApiJson_when_itIsInClientSideMode()
            throws Exception {
        Assert.assertFalse(FileUtils.fileExists(openApiJsonFile));
        mojo.execute();
        Assert.assertTrue(FileUtils.fileExists(openApiJsonFile));
    }

    @Test
    public void mavenGoal_generateTsFiles_when_enabled() throws Exception {
        File connectClientApi = new File(generatedTsFolder,
                "connect-client.default.ts");
        File endpointClientApi = new File(generatedTsFolder, "MyEndpoint.ts");

        Assert.assertFalse(connectClientApi.exists());
        Assert.assertFalse(endpointClientApi.exists());
        mojo.execute();
        Assert.assertTrue(connectClientApi.exists());
        Assert.assertTrue(endpointClientApi.exists());
    }

    static List<String> getClassPath() {
        // Add folder with test classes
        List<String> classPaths = new ArrayList<>(
                Arrays.asList("target/test-classes"));

        // Add other paths already present in the system classpath
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        if (classLoader instanceof URLClassLoader) {
            URL[] urls = ((URLClassLoader) classLoader).getURLs();
            for (URL url : urls) {
                classPaths.add(url.getFile());
            }
        } else {
            String[] paths = System.getProperty("java.class.path")
                    .split(pathSeparator);
            for (String path : paths) {
                classPaths.add(path);
            }
        }
        return classPaths;
    }

    @Endpoint
    public class MyEndpoint {
        public void foo(String bar) {
        }

        public String bar(String baz) {
            return baz;
        }
    }
}
