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
 *
 */
package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;

import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.getPackageJson;
import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.setProject;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;

public class CleanFrontendMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final CleanFrontendMojo mojo = new CleanFrontendMojo();
    private String packageJson;
    private File projectBase;
    private MavenProject project;
    private File frontendGenerated;

    @Before
    public void setup() throws Exception {

        projectBase = temporaryFolder.getRoot();

        project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getBasedir()).thenReturn(projectBase);

        packageJson = new File(projectBase, PACKAGE_JSON).getAbsolutePath();
        frontendGenerated = new File(projectBase,
                "src/main/frontend/generated");

        ReflectionUtils.setVariableValueInObject(mojo, Constants.NPM_TOKEN,
                projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "webpackOutputDirectory",
                new File(projectBase, VAADIN_WEBAPP_RESOURCES));
        ReflectionUtils.setVariableValueInObject(mojo,
                "resourceOutputDirectory",
                new File(projectBase, VAADIN_SERVLET_RESOURCES));
        ReflectionUtils.setVariableValueInObject(mojo, "frontendDirectory",
                new File(projectBase, "src/main/frontend"));

        ReflectionUtils.setVariableValueInObject(mojo, "openApiJsonFile",
                new File(projectBase,
                        "target/generated-resources/openapi.json"));
        ReflectionUtils.setVariableValueInObject(mojo, "applicationProperties",
                new File(projectBase,
                        "src/main/resources/application.properties"));
        ReflectionUtils.setVariableValueInObject(mojo, "javaSourceFolder",
                new File(".", "src/test/java"));
        ReflectionUtils.setVariableValueInObject(mojo, "generatedTsFolder",
                frontendGenerated);

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
    public void mavenGoal_when_packageJsonMissing() throws Exception {
        Assert.assertFalse(FileUtils.fileExists(packageJson));
        mojo.execute();
    }

    @Test
    public void should_removeNodeModulesFolder()
            throws MojoFailureException, MojoExecutionException {
        final File nodeModules = new File(projectBase, NODE_MODULES);
        Assert.assertTrue("Failed to create 'node_modules'",
                nodeModules.mkdirs());
        mojo.execute();
        Assert.assertFalse("'node_modules' was not removed.",
                nodeModules.exists());
    }

    @Test
    public void should_notRemoveNodeModulesFolder_hilla()
            throws MojoFailureException, IOException, MojoExecutionException {
        enableHilla();
        final File nodeModules = new File(projectBase, NODE_MODULES);
        Assert.assertTrue("Failed to create 'node_modules'",
                nodeModules.mkdirs());
        mojo.execute();
        Assert.assertTrue("'node_modules' should not be removed.",
                nodeModules.exists());
    }

    @Test
    public void should_removeCompressedDevBundle()
            throws MojoFailureException, IOException, MojoExecutionException {
        final File devBundleDir = new File(projectBase,
                Constants.BUNDLE_LOCATION);
        final File devBundle = new File(projectBase,
                Constants.DEV_BUNDLE_COMPRESSED_FILE_LOCATION);
        Assert.assertTrue("Failed to create 'dev-bundle' folder",
                devBundleDir.mkdirs());
        Assert.assertTrue(devBundle.createNewFile());
        mojo.execute();
        Assert.assertFalse("'dev.bundle' was not removed.", devBundle.exists());
        Assert.assertFalse("Empty 'bundle' directory was not removed.",
                devBundleDir.exists());
    }

    @Test
    public void should_removeOldDevBundle()
            throws MojoFailureException, MojoExecutionException {
        final File devBundleDir = new File(projectBase, "src/main/dev-bundle/");
        Assert.assertTrue("Failed to create 'dev-bundle' folder",
                devBundleDir.mkdirs());
        mojo.execute();
        Assert.assertFalse("Bundle directory was not removed.",
                devBundleDir.exists());
    }

    @Test
    public void should_removeFrontendGeneratedFolder()
            throws MojoFailureException, IOException, MojoExecutionException {
        Assert.assertTrue("Failed to create 'frontend/generated'",
                frontendGenerated.mkdirs());
        FileUtils.fileWrite(new File(frontendGenerated, "my_theme.js"),
                "fakeThemeFile");

        mojo.execute();
        Assert.assertFalse(
                "Generated frontend folder 'frontend/generated' was not removed.",
                frontendGenerated.exists());
    }

    @Test
    public void should_removeGeneratedFolderForCustomFrontendFolder()
            throws MojoFailureException, IOException, IllegalAccessException,
            MojoExecutionException {

        File customFrontendFolder = new File(projectBase, "src/main/frontend");
        File customFrontendGenerated = new File(customFrontendFolder,
                "generated");
        Assert.assertTrue("Failed to create 'src/main/frontend/generated'",
                customFrontendGenerated.mkdirs());
        FileUtils.fileWrite(new File(customFrontendFolder, "my_theme.js"),
                "fakeThemeFile");

        ReflectionUtils.setVariableValueInObject(mojo, "frontendDirectory",
                customFrontendFolder);

        mojo.execute();
        Assert.assertTrue(
                "Custom frontend folder 'src/main/frontend' has been removed.",
                customFrontendFolder.exists());
        Assert.assertFalse(
                "Generated frontend folder 'src/main/frontend/generated' was not removed.",
                customFrontendGenerated.exists());
    }

    @Test
    public void should_removeNpmPackageLockFile()
            throws MojoFailureException, IOException, MojoExecutionException {
        final File packageLock = new File(projectBase, "package-lock.json");
        FileUtils.fileWrite(packageLock, "{ \"fake\": \"lock\"}");

        mojo.execute();
        Assert.assertFalse("package-lock.json was not removed",
                packageLock.exists());
    }

    @Test
    public void should_notRemoveNpmPackageLockFile_hilla()
            throws MojoFailureException, IOException, MojoExecutionException {
        enableHilla();
        final File packageLock = new File(projectBase, "package-lock.json");
        FileUtils.fileWrite(packageLock, "{ \"fake\": \"lock\"}");

        mojo.execute();
        Assert.assertTrue("package-lock.json should not be removed",
                packageLock.exists());
    }

    @Test
    public void should_removePnpmFile()
            throws MojoFailureException, IOException, MojoExecutionException {
        final File pnpmFile = new File(projectBase, ".pnpmfile.cjs");
        FileUtils.fileWrite(pnpmFile, "{ \"fake\": \"pnpmfile\"}");

        mojo.execute();
        Assert.assertFalse(".pnpmfile.cjs was not removed", pnpmFile.exists());
    }

    @Test
    public void should_removePnpmPackageLockFile()
            throws MojoFailureException, IOException, MojoExecutionException {
        final File pnpmLock = new File(projectBase, "pnpm-lock.yaml");
        FileUtils.fileWrite(pnpmLock, "lockVersion: -1");
        mojo.execute();
        Assert.assertFalse("pnpm-lock.yaml was not removed", pnpmLock.exists());
    }

    @Test
    public void should_cleanPackageJson_removeVaadinAndHashObjects()
            throws MojoFailureException, IOException, MojoExecutionException {
        ObjectNode json = createInitialPackageJson();
        FileUtils.fileWrite(packageJson, json.toString());
        mojo.execute();
        assertPackageJsonContent();

        ObjectNode packageObjectNode = getPackageJson(packageJson);

        Assert.assertFalse("'vaadin' object was left in package.json",
                packageObjectNode.has("vaadin"));
        Assert.assertFalse("'hash' object was left in package.json",
                packageObjectNode.has("hash"));
    }

    @Test
    public void should_cleanPackageJson_removeVaadinDependenciesInOverrides()
            throws MojoFailureException, IOException, MojoExecutionException {
        ObjectNode json = createInitialPackageJson(true);
        FileUtils.fileWrite(packageJson, json.toString());

        assertContainsPackage(json.get("overrides"), "@polymer/polymer");

        mojo.execute();

        ObjectNode packageObjectNode = getPackageJson(packageJson);
        assertNotContainsPackage(packageObjectNode.get("overrides"),
                "@polymer/polymer");
    }

    @Test
    public void should_keepUserDependencies_whenPackageJsonEdited()
            throws MojoFailureException, IOException, MojoExecutionException {
        ObjectNode json = createInitialPackageJson();
        json.replace("dependencies", JacksonUtils.createObjectNode());
        ((ObjectNode) json.get("dependencies")).put("foo", "bar");
        FileUtils.fileWrite(packageJson, json.toString());
        mojo.execute();
        assertPackageJsonContent();

        ObjectNode packageObjectNode = getPackageJson(packageJson);
        assertContainsPackage(packageObjectNode.get("dependencies"), "foo");
    }

    private void assertPackageJsonContent() throws IOException {
        ObjectNode packageObjectNode = getPackageJson(packageJson);

        assertNotContainsPackage(packageObjectNode.get("dependencies"),
                "@polymer/polymer", "@webcomponents/webcomponentsjs");

        assertNotContainsPackage(packageObjectNode.get("devDependencies"),
                "vite");
    }

    private void enableHilla() throws IOException {
        // Add fake com.vaadin.hilla.EndpointController class to make project
        // detected as Hilla project with endpoints.
        Files.createDirectories(Paths.get(projectBase.toString(), "target")
                .resolve("test-classes/com/vaadin/hilla"));
        Files.createFile(Paths.get(projectBase.toString(), "target").resolve(
                "test-classes/com/vaadin/hilla/EndpointController.class"));
        Files.createDirectories(
                Paths.get(projectBase.toString(), "src/main/frontend"));
        Files.createFile(Paths.get(projectBase.toString(), "src/main/frontend")
                .resolve("index.ts"));
    }

    static void assertNotContainsPackage(JsonNode dependencies,
            String... packages) {
        Arrays.asList(packages).forEach(
                dep -> Assert.assertFalse("Has " + dep, dependencies.has(dep)));
    }

    static void assertContainsPackage(JsonNode dependencies,
            String... packages) {
        Arrays.asList(packages).forEach(dep -> Assert
                .assertTrue("Not Have " + dep, dependencies.has(dep)));
    }

    static ObjectNode createInitialPackageJson() {
        return createInitialPackageJson(false);
    }

    static ObjectNode createInitialPackageJson(boolean withOverrides) {
        ObjectNode packageJson = JacksonUtils.createObjectNode();
        ObjectNode vaadinPackages = JacksonUtils.createObjectNode();

        vaadinPackages.set("dependencies", JacksonUtils.createObjectNode());
        ObjectNode defaults = (ObjectNode) vaadinPackages.get("dependencies");
        defaults.put("@polymer/polymer", "3.2.0");
        defaults.put("@webcomponents/webcomponentsjs", "^2.2.10");

        packageJson.set("dependencies", defaults);

        vaadinPackages.set("devDependencies", JacksonUtils.createObjectNode());
        defaults = (ObjectNode) vaadinPackages.get("devDependencies");
        defaults.put("vite", "3.4.5");
        packageJson.set("devDependencies", defaults);

        vaadinPackages.put("hash", "");
        packageJson.set("vaadin", vaadinPackages);

        if (withOverrides) {
            ObjectNode overrides = JacksonUtils.createObjectNode();
            overrides.put("@polymer/polymer", "$@polymer/polymer");
            packageJson.set("overrides", overrides);
        }

        return packageJson;
    }
}
