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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.plugin.TestUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;

import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.assertContainsPackage;
import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.assertNotContainingPackages;
import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.getPackageJson;
import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.setProject;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.InitParameters.FRONTEND_HOTDEPLOY;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;

public class PrepareFrontendMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final PrepareFrontendMojo mojo = new PrepareFrontendMojo();
    private String packageJson;
    private File projectBase;
    private File webpackOutputDirectory;
    private File resourceOutputDirectory;
    private File tokenFile;
    private File defaultJavaSource;
    private File defaultJavaResource;
    private File generatedTsFolder;

    @Before
    public void setup() throws Exception {

        projectBase = temporaryFolder.getRoot();

        tokenFile = new File(temporaryFolder.getRoot(),
                VAADIN_SERVLET_RESOURCES + FrontendUtils.TOKEN_FILE);

        packageJson = new File(projectBase, PACKAGE_JSON).getAbsolutePath();
        webpackOutputDirectory = new File(projectBase, VAADIN_WEBAPP_RESOURCES);
        resourceOutputDirectory = new File(projectBase,
                VAADIN_SERVLET_RESOURCES);
        defaultJavaSource = new File(".", "src/test/java");
        defaultJavaResource = new File(".", "src/test/resources");
        generatedTsFolder = new File(projectBase,
                "src/main/frontend/generated");

        ReflectionUtils.setVariableValueInObject(mojo, Constants.NPM_TOKEN,
                projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "webpackOutputDirectory",
                webpackOutputDirectory);
        ReflectionUtils.setVariableValueInObject(mojo,
                "resourceOutputDirectory", resourceOutputDirectory);
        ReflectionUtils.setVariableValueInObject(mojo, "frontendDirectory",
                new File(projectBase, "src/main/frontend"));

        ReflectionUtils.setVariableValueInObject(mojo, "openApiJsonFile",
                new File(projectBase,
                        "target/generated-resources/openapi.json"));
        ReflectionUtils.setVariableValueInObject(mojo, "applicationProperties",
                new File(projectBase,
                        "src/main/resources/application.properties"));
        ReflectionUtils.setVariableValueInObject(mojo, "javaSourceFolder",
                defaultJavaSource);
        ReflectionUtils.setVariableValueInObject(mojo, "javaResourceFolder",
                defaultJavaResource);
        ReflectionUtils.setVariableValueInObject(mojo, "generatedTsFolder",
                generatedTsFolder);

        ReflectionUtils.setVariableValueInObject(mojo, "pnpmEnable",
                Constants.ENABLE_PNPM_DEFAULT);
        ReflectionUtils.setVariableValueInObject(mojo, "requireHomeNodeExec",
                true);
        ReflectionUtils.setVariableValueInObject(mojo, "nodeVersion",
                FrontendTools.DEFAULT_NODE_VERSION);
        ReflectionUtils.setVariableValueInObject(mojo, "nodeDownloadRoot",
                NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT);
        ReflectionUtils.setVariableValueInObject(mojo, "projectBasedir",
                projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "projectBuildDir",
                Paths.get(projectBase.toString(), "target").toString());

        setProject(mojo, projectBase);
    }

    @Test
    public void tokenFileShouldExist_noHotdeployTokenVisible()
            throws IOException, MojoExecutionException, MojoFailureException {
        mojo.execute();
        Assert.assertTrue("No token file could be found", tokenFile.exists());

        String json = org.apache.commons.io.FileUtils
                .readFileToString(tokenFile, "UTF-8");
        ObjectNode buildInfo = JacksonUtils.readTree(json);
        Assert.assertNull("Default HotDeploy token should not be available",
                buildInfo.get(FRONTEND_HOTDEPLOY));
        Assert.assertNotNull("productionMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_PRODUCTION_MODE));
    }

    @Test
    public void tokenFileShouldExist_hotdeployIsTrueTokenVisible()
            throws IOException, MojoExecutionException, MojoFailureException,
            IllegalAccessException {
        ReflectionUtils.setVariableValueInObject(mojo, "frontendHotdeploy",
                Boolean.TRUE);
        mojo.execute();
        Assert.assertTrue("No token file could be found", tokenFile.exists());

        String json = org.apache.commons.io.FileUtils
                .readFileToString(tokenFile, "UTF-8");
        ObjectNode buildInfo = JacksonUtils.readTree(json);
        Assert.assertNotNull("HotDeploy should be written",
                buildInfo.get(FRONTEND_HOTDEPLOY));
        Assert.assertTrue("HotDeploy should be enabled",
                buildInfo.get(FRONTEND_HOTDEPLOY).booleanValue());
    }

    @Test
    public void existingTokenFile_defaultFrontendHotdeployShouldBeRemoved()
            throws IOException, MojoExecutionException, MojoFailureException {

        ObjectNode initialBuildInfo = JacksonUtils.createObjectNode();
        initialBuildInfo.put(SERVLET_PARAMETER_PRODUCTION_MODE, false);
        initialBuildInfo.put(FRONTEND_HOTDEPLOY, true);
        org.apache.commons.io.FileUtils.forceMkdir(tokenFile.getParentFile());
        org.apache.commons.io.FileUtils.write(tokenFile,
                initialBuildInfo.toPrettyString() + "\n", "UTF-8");

        mojo.execute();

        String json = org.apache.commons.io.FileUtils
                .readFileToString(tokenFile, "UTF-8");
        ObjectNode buildInfo = JacksonUtils.readTree(json);
        Assert.assertNull("Default hotdeploy should not be added",
                buildInfo.get(FRONTEND_HOTDEPLOY));
        Assert.assertNotNull("productionMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_PRODUCTION_MODE));
    }

    @Test
    public void writeTokenFile_devModePropertiesAreWritten()
            throws IOException, MojoExecutionException, MojoFailureException {

        mojo.execute();

        String json = org.apache.commons.io.FileUtils
                .readFileToString(tokenFile, StandardCharsets.UTF_8);
        ObjectNode buildInfo = JacksonUtils.readTree(json);

        Assert.assertTrue(
                InitParameters.SERVLET_PARAMETER_ENABLE_PNPM
                        + "should have been written",
                buildInfo.has(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM));
        Assert.assertFalse(
                InitParameters.SERVLET_PARAMETER_ENABLE_PNPM
                        + "should have been disabled",
                buildInfo.get(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM)
                        .booleanValue());

        Assert.assertTrue(
                InitParameters.REQUIRE_HOME_NODE_EXECUTABLE
                        + "should have been written",
                buildInfo.has(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE));
        Assert.assertTrue(
                InitParameters.REQUIRE_HOME_NODE_EXECUTABLE
                        + "should have been enabled",
                buildInfo.get(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE)
                        .booleanValue());

        Assert.assertFalse(
                InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE
                        + "should not have been written",
                buildInfo.has(
                        InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE));
    }

    @Test
    public void mavenGoal_when_packageJsonMissing_shouldNotGenerateDefault()
            throws Exception {
        Assert.assertFalse(FileUtils.fileExists(packageJson));
        mojo.execute();
        Assert.assertFalse(FileUtils.fileExists(packageJson));
    }

    @Test
    public void mavenGoal_when_frontendGeneratedExists_shouldClearFolder()
            throws Exception {
        if (!generatedTsFolder.mkdirs()) {
            Assert.fail("Failed to generate Frontend/generated folders.");
        }
        final File flowFolder = new File(generatedTsFolder, "flow");
        if (!flowFolder.mkdir()) {
            Assert.fail("Failed to generate flow folder");
        }
        final File oldFile = new File(flowFolder, "old.js");
        if (!oldFile.createNewFile()) {
            Assert.fail("Failed to generate old.js in Frontend/generated/flow");
        }

        mojo.execute();
        Assert.assertTrue("Missing generated folder",
                generatedTsFolder.exists());
        Assert.assertFalse("Old file should have been removed",
                oldFile.exists());
    }

    @Test
    public void should_updateAndKeepDependencies_when_packageJsonExists()
            throws Exception {
        ObjectNode json = TestUtils.getInitialPackageJson();
        json.set("dependencies", JacksonUtils.createObjectNode());
        ((ObjectNode) json.get("dependencies")).put("foo", "bar");
        FileUtils.fileWrite(packageJson, json.toString());
        mojo.execute();
        assertPackageJsonContent();

        ObjectNode packageJsonObject = getPackageJson(packageJson);
        assertContainsPackage(packageJsonObject.get("dependencies"), "foo");
    }

    @Test
    public void jarPackaging_copyProjectFrontendResources()
            throws MojoExecutionException, MojoFailureException,
            IllegalAccessException {
        mojo.project.setPackaging("jar");
        MavenProject project = Mockito.spy(mojo.project);
        ReflectionUtils.setVariableValueInObject(mojo, "project", project);

        mojo.execute();

        Mockito.verify(project, Mockito.atLeastOnce()).getArtifacts();
    }

    private void assertPackageJsonContent() throws IOException {
        ObjectNode packageJsonObject = getPackageJson(packageJson);

        assertNotContainingPackages(packageJsonObject.get("dependencies"),
                "@polymer/polymer");

        assertContainsPackage(packageJsonObject.get("devDependencies"), "vite",
                "@rollup/plugin-replace", "rollup-plugin-brotli",
                "vite-plugin-checker");
    }
}
