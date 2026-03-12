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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;

import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.getPackageJson;
import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.setProject;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanFrontendMojoTest {
    @TempDir
    Path tempDir;

    private final CleanFrontendMojo mojo = new CleanFrontendMojo();
    private String packageJson;
    private File projectBase;
    private MavenProject project;
    private File frontendGenerated;

    @BeforeEach
    void setup() throws Exception {

        projectBase = tempDir.toFile();

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
    void mavenGoal_when_packageJsonMissing() throws Exception {
        assertFalse(FileUtils.fileExists(packageJson));
        mojo.execute();
    }

    @Test
    void should_removeNodeModulesFolder()
            throws MojoFailureException, MojoExecutionException {
        final File nodeModules = new File(projectBase,
                FrontendUtils.NODE_MODULES);
        assertTrue(nodeModules.mkdirs(),
                "Failed to create 'node_modules'");
        mojo.execute();
        assertFalse(nodeModules.exists(),
                "'node_modules' was not removed.");
    }

    @Test
    void should_notRemoveNodeModulesFolder_hilla()
            throws MojoFailureException, IOException, MojoExecutionException {
        enableHilla();
        final File nodeModules = new File(projectBase,
                FrontendUtils.NODE_MODULES);
        assertTrue(nodeModules.mkdirs(),
                "Failed to create 'node_modules'");
        mojo.execute();
        assertTrue(nodeModules.exists(),
                "'node_modules' should not be removed.");
    }

    @Test
    void should_removeCompressedDevBundle()
            throws MojoFailureException, IOException, MojoExecutionException {
        final File devBundleDir = new File(projectBase,
                Constants.BUNDLE_LOCATION);
        final File devBundle = new File(projectBase,
                Constants.DEV_BUNDLE_COMPRESSED_FILE_LOCATION);
        assertTrue(devBundleDir.mkdirs(),
                "Failed to create 'dev-bundle' folder");
        assertTrue(devBundle.createNewFile());
        mojo.execute();
        assertFalse(devBundle.exists(),
                "'dev.bundle' was not removed.");
        assertFalse(devBundleDir.exists(),
                "Empty 'bundle' directory was not removed.");
    }

    @Test
    void should_removeOldDevBundle()
            throws MojoFailureException, MojoExecutionException {
        final File devBundleDir = new File(projectBase, "src/main/dev-bundle/");
        assertTrue(devBundleDir.mkdirs(),
                "Failed to create 'dev-bundle' folder");
        mojo.execute();
        assertFalse(devBundleDir.exists(),
                "Bundle directory was not removed.");
    }

    @Test
    void should_removeFrontendGeneratedFolder()
            throws MojoFailureException, IOException, MojoExecutionException {
        assertTrue(frontendGenerated.mkdirs(),
                "Failed to create 'frontend/generated'");
        FileUtils.fileWrite(new File(frontendGenerated, "my_theme.js"),
                "fakeThemeFile");

        mojo.execute();
        assertFalse(frontendGenerated.exists(),
                "Generated frontend folder 'frontend/generated' was not removed.");
    }

    @Test
    void should_removeGeneratedFolderForCustomFrontendFolder()
            throws MojoFailureException, IOException, IllegalAccessException,
            MojoExecutionException {

        File customFrontendFolder = new File(projectBase, "src/main/frontend");
        File customFrontendGenerated = new File(customFrontendFolder,
                "generated");
        assertTrue(customFrontendGenerated.mkdirs(),
                "Failed to create 'src/main/frontend/generated'");
        FileUtils.fileWrite(new File(customFrontendFolder, "my_theme.js"),
                "fakeThemeFile");

        ReflectionUtils.setVariableValueInObject(mojo, "frontendDirectory",
                customFrontendFolder);

        mojo.execute();
        assertTrue(customFrontendFolder.exists(),
                "Custom frontend folder 'src/main/frontend' has been removed.");
        assertFalse(customFrontendGenerated.exists(),
                "Generated frontend folder 'src/main/frontend/generated' was not removed.");
    }

    @Test
    void should_removeNpmPackageLockFile()
            throws MojoFailureException, IOException, MojoExecutionException {
        final File packageLock = new File(projectBase, "package-lock.json");
        FileUtils.fileWrite(packageLock, "{ \"fake\": \"lock\"}");

        mojo.execute();
        assertFalse(packageLock.exists(),
                "package-lock.json was not removed");
    }

    @Test
    void should_notRemoveNpmPackageLockFile_hilla()
            throws MojoFailureException, IOException, MojoExecutionException {
        enableHilla();
        final File packageLock = new File(projectBase, "package-lock.json");
        FileUtils.fileWrite(packageLock, "{ \"fake\": \"lock\"}");

        mojo.execute();
        assertTrue(packageLock.exists(),
                "package-lock.json should not be removed");
    }

    @Test
    void should_removePnpmFile()
            throws MojoFailureException, IOException, MojoExecutionException {
        final File pnpmFile = new File(projectBase, ".pnpmfile.cjs");
        FileUtils.fileWrite(pnpmFile, "{ \"fake\": \"pnpmfile\"}");

        mojo.execute();
        assertFalse(pnpmFile.exists(),
                ".pnpmfile.cjs was not removed");
    }

    @Test
    void should_removePnpmPackageLockFile()
            throws MojoFailureException, IOException, MojoExecutionException {
        final File pnpmLock = new File(projectBase, "pnpm-lock.yaml");
        FileUtils.fileWrite(pnpmLock, "lockVersion: -1");
        mojo.execute();
        assertFalse(pnpmLock.exists(),
                "pnpm-lock.yaml was not removed");
    }

    @Test
    void should_cleanPackageJson_removeVaadinAndHashObjects()
            throws MojoFailureException, IOException, MojoExecutionException {
        ObjectNode json = createInitialPackageJson();
        FileUtils.fileWrite(packageJson, json.toString());
        mojo.execute();
        assertPackageJsonContent();

        ObjectNode packageObjectNode = getPackageJson(packageJson);

        assertFalse(packageObjectNode.has("vaadin"),
                "'vaadin' object was left in package.json");
        assertFalse(packageObjectNode.has("hash"),
                "'hash' object was left in package.json");
    }

    @Test
    void should_cleanPackageJson_removeVaadinDependenciesInOverrides()
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
    void should_keepUserDependencies_whenPackageJsonEdited()
            throws MojoFailureException, IOException, MojoExecutionException {
        ObjectNode json = createInitialPackageJson();
        json.set("dependencies", JacksonUtils.createObjectNode());
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
        Arrays.asList(packages).forEach(dep -> assertFalse(dependencies.has(dep), "Has " + dep));
    }

    static void assertContainsPackage(JsonNode dependencies,
            String... packages) {
        Arrays.asList(packages).forEach(dep -> assertTrue(dependencies.has(dep), "Not Have " + dep));
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
