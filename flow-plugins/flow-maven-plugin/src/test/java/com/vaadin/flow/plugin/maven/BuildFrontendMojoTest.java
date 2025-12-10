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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.jcip.annotations.NotThreadSafe;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.plugin.TestUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.pro.licensechecker.LicenseException;

import static com.vaadin.flow.server.Constants.COMMERCIAL_BANNER_TOKEN;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.InitParameters.APPLICATION_IDENTIFIER;
import static com.vaadin.flow.server.InitParameters.FRONTEND_HOTDEPLOY;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;
import static java.io.File.pathSeparator;

@NotThreadSafe
public class BuildFrontendMojoTest {
    public static final String TEST_PROJECT_RESOURCE_JS = "test_project_resource.js";
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File importsFile;
    private File nodeModulesPath;
    private File jarResourcesFolder;
    private File projectBase;
    private File projectFrontendResourcesDirectory;
    private String packageJson;
    private String viteConfig;
    private String viteGenerated;
    private File webpackOutputDirectory;
    private File resourceOutputDirectory;
    private File defaultJavaSource;
    private File openApiJsonFile;
    private File generatedTsFolder;
    private File tokenFile;
    private File jarResourcesSource;

    private final BuildFrontendMojo mojo = Mockito.spy(new BuildFrontendMojo());
    private Lookup lookup;
    private File frontendDirectory;

    @Before
    public void setup() throws Exception {
        projectBase = temporaryFolder.getRoot();

        tokenFile = new File(temporaryFolder.getRoot(),
                VAADIN_SERVLET_RESOURCES + FrontendUtils.TOKEN_FILE);

        File npmFolder = projectBase;
        nodeModulesPath = new File(npmFolder, FrontendUtils.NODE_MODULES);
        frontendDirectory = new File(npmFolder,
                FrontendUtils.DEFAULT_FRONTEND_DIR);
        importsFile = FrontendUtils.getFlowGeneratedImports(frontendDirectory);
        jarResourcesFolder = new File(
                new File(frontendDirectory, FrontendUtils.GENERATED),
                FrontendUtils.JAR_RESOURCES_FOLDER);
        packageJson = new File(npmFolder, PACKAGE_JSON).getAbsolutePath();
        viteConfig = new File(npmFolder, FrontendUtils.VITE_CONFIG)
                .getAbsolutePath();
        viteGenerated = new File(npmFolder, FrontendUtils.VITE_GENERATED_CONFIG)
                .getAbsolutePath();
        webpackOutputDirectory = new File(projectBase, VAADIN_WEBAPP_RESOURCES);
        resourceOutputDirectory = new File(projectBase,
                VAADIN_SERVLET_RESOURCES);
        jarResourcesSource = new File(projectBase,
                "jar-resources-source/META-INF/frontend");
        jarResourcesSource.mkdirs();

        projectFrontendResourcesDirectory = new File(npmFolder,
                "flow_resources");

        defaultJavaSource = new File(".", "src/test/java");
        File defaultJavaResource = new File(".", "src/test/resources");
        openApiJsonFile = new File(npmFolder,
                "target/classes/com/vaadin/hilla/openapi.json");
        generatedTsFolder = new File(npmFolder, "src/main/frontend/generated");

        Assert.assertTrue("Failed to create a test project resources",
                projectFrontendResourcesDirectory.mkdirs());
        Assert.assertTrue("Failed to create a test project file",
                new File(projectFrontendResourcesDirectory,
                        TEST_PROJECT_RESOURCE_JS).createNewFile());

        ReflectionUtils.setVariableValueInObject(mojo,
                "frontendResourcesDirectory",
                projectFrontendResourcesDirectory);

        ReflectionUtils.setVariableValueInObject(mojo, "webpackOutputDirectory",
                webpackOutputDirectory);
        ReflectionUtils.setVariableValueInObject(mojo,
                "resourceOutputDirectory", resourceOutputDirectory);
        ReflectionUtils.setVariableValueInObject(mojo, "frontendDirectory",
                frontendDirectory);
        ReflectionUtils.setVariableValueInObject(mojo,
                "generateEmbeddableWebComponents", false);
        ReflectionUtils.setVariableValueInObject(mojo, "npmFolder", npmFolder);
        ReflectionUtils.setVariableValueInObject(mojo, "generateBundle", false);
        ReflectionUtils.setVariableValueInObject(mojo, "runNpmInstall", false);
        ReflectionUtils.setVariableValueInObject(mojo, "optimizeBundle", true);
        ReflectionUtils.setVariableValueInObject(mojo, "forceProductionBuild",
                false);

        ReflectionUtils.setVariableValueInObject(mojo, "openApiJsonFile",
                openApiJsonFile);
        ReflectionUtils.setVariableValueInObject(mojo, "applicationProperties",
                new File(npmFolder,
                        "src/main/resources/application.properties"));
        ReflectionUtils.setVariableValueInObject(mojo, "javaSourceFolder",
                defaultJavaSource);
        ReflectionUtils.setVariableValueInObject(mojo, "javaResourceFolder",
                defaultJavaResource);
        ReflectionUtils.setVariableValueInObject(mojo, "generatedTsFolder",
                generatedTsFolder);
        ReflectionUtils.setVariableValueInObject(mojo, "nodeVersion",
                FrontendTools.DEFAULT_NODE_VERSION);
        ReflectionUtils.setVariableValueInObject(mojo, "nodeDownloadRoot",
                NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT);
        ReflectionUtils.setVariableValueInObject(mojo, "projectBasedir",
                projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "projectBuildDir",
                Paths.get(projectBase.toString(), "target").toString());
        ReflectionUtils.setVariableValueInObject(mojo, "postinstallPackages",
                Collections.emptyList());
        Mockito.doReturn(
                Set.of(jarResourcesSource.getParentFile().getParentFile()))
                .when(mojo).getJarFiles();

        setProject(mojo, npmFolder);

        // Install all imports used in the tests on node_modules so as we don't
        // need to run `npm install`
        createExpectedImports(frontendDirectory, nodeModulesPath);
        FileUtils.fileWrite(packageJson, "UTF-8",
                TestUtils.getInitialPackageJson().toString());

        lookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(new TestEndpointGeneratorTaskFactory()).when(lookup)
                .lookup(EndpointGeneratorTaskFactory.class);
        Mockito.doAnswer(invocation -> {
            Mockito.doReturn((ClassFinder) invocation.getArguments()[0])
                    .when(lookup).lookup(ClassFinder.class);
            return lookup;
        }).when(mojo).createLookup(Mockito.any(ClassFinder.class));
    }

    @After
    public void teardown() throws IOException {
        if (FileUtils.fileExists(packageJson)) {
            FileUtils.fileDelete(packageJson);
        }
        if (FileUtils.fileExists(viteConfig)) {
            FileUtils.fileDelete(viteConfig);
        }
        if (FileUtils.fileExists(viteGenerated)) {
            FileUtils.fileDelete(viteGenerated);
        }
    }

    static void setProject(AbstractMojo mojo, File baseFolder)
            throws Exception {
        mojo.setPluginContext(new HashMap<>());

        MavenProject project = new MavenProject();
        project.setGroupId("com.vaadin.testing");
        project.setArtifactId("my-application");
        project.setFile(baseFolder.toPath().resolve("pom.xml").toFile());
        project.setBuild(new Build());
        project.getBuild().setFinalName("finalName");

        List<String> classPath = getClassPath(baseFolder.toPath()).stream()
                // Exclude maven jars so classes will be loaded by them fake
                // maven.api realm that will be the same for the test class
                // and the mojo execution
                .filter(path -> !path.matches(".*([\\\\/])maven-.*\\.jar"))
                .toList();
        AtomicInteger dependencyCounter = new AtomicInteger();
        project.setArtifacts(classPath.stream().map(path -> {
            DefaultArtifactHandler artifactHandler = new DefaultArtifactHandler();
            artifactHandler.setAddedToClasspath(true);
            DefaultArtifact artifact = new DefaultArtifact("com.vaadin.testing",
                    "dep-" + dependencyCounter.incrementAndGet(), "1.0",
                    "compile", "jar", null, artifactHandler);
            artifact.setFile(new File(path));
            return artifact;
        }).collect(Collectors.toSet()));
        ReflectionUtils.setVariableValueInObject(mojo, "project", project);

        ClassWorld classWorld = new ClassWorld();
        ClassRealm mavenApiRealm = classWorld.newRealm("maven.api", null);
        mavenApiRealm.importFrom(MavenProject.class.getClassLoader(), "");
        ClassRealm pluginClassRealm = classWorld.newRealm("flow-plugin", null);

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setArtifacts(List.of());
        pluginDescriptor.setClassRealm(pluginClassRealm);
        pluginDescriptor.setPlugin(new Plugin());
        pluginDescriptor.setClassRealm(pluginClassRealm);
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);
        MojoExecution mojoExecution = new MojoExecution(mojoDescriptor);
        ReflectionUtils.setVariableValueInObject(mojo, "mojoExecution",
                mojoExecution);
    }

    @Test
    public void should_generateViteConfig() throws Exception {
        Assert.assertFalse(FileUtils.fileExists(viteConfig));
        mojo.execute();
        Assert.assertTrue(FileUtils.fileExists(viteConfig));
    }

    @Test
    public void should_generateViteGeneratedConfig() throws Exception {
        Assert.assertFalse(FileUtils.fileExists(viteGenerated));
        mojo.execute();
        Assert.assertTrue(FileUtils.fileExists(viteGenerated));
    }

    @Test
    public void should_copyProjectFrontendResources()
            throws MojoExecutionException, MojoFailureException {

        List<File> initialFiles = gatherFiles(jarResourcesFolder);
        initialFiles.forEach(file -> Assert.assertFalse(String.format(
                "Test resource shouldn't exist before running mojo.", file),
                TEST_PROJECT_RESOURCE_JS.equals(file.getName())));
        mojo.execute();

        Set<String> projectFrontendResources = Stream
                .of(projectFrontendResourcesDirectory.listFiles())
                .map(File::getName).collect(Collectors.toSet());

        Set<String> filesInFlowResourcesFolder = Stream
                .of(jarResourcesFolder.listFiles()).map(File::getName)
                .collect(Collectors.toSet());

        projectFrontendResources.forEach(fileName -> {
            Assert.assertTrue(String.format(
                    "Expected the copied file '%s' to be in the project resources",
                    fileName), filesInFlowResourcesFolder.contains(fileName));
        });
    }

    @Test
    public void changedBuildDirectory_resourcesCopiedNoTargetFolderExists()
            throws MojoExecutionException, MojoFailureException,
            IllegalAccessException, IOException {
        // Clean generated target folders.
        File target = new File(projectBase, TARGET);
        if (FileUtils.fileExists(target.toString())) {
            FileUtils.deleteDirectory(target);
        }
        openApiJsonFile = new File(projectBase,
                "build/generated-resources/openapi.json");

        ReflectionUtils.setVariableValueInObject(mojo, "openApiJsonFile",
                openApiJsonFile);
        ReflectionUtils.setVariableValueInObject(mojo, "projectBuildDir",
                "build");

        List<File> initialFiles = gatherFiles(jarResourcesFolder);
        initialFiles.forEach(file -> Assert.assertFalse(String.format(
                "Test resource shouldn't exist before running mojo.", file),
                TEST_PROJECT_RESOURCE_JS.equals(file.getName())));
        mojo.execute();

        Set<String> projectFrontendResources = Stream
                .of(projectFrontendResourcesDirectory.listFiles())
                .map(File::getName).collect(Collectors.toSet());

        Set<String> filesInFlowResourcesFolder = Stream
                .of(jarResourcesFolder.listFiles()).map(File::getName)
                .collect(Collectors.toSet());

        projectFrontendResources.forEach(fileName -> {
            Assert.assertTrue(String.format(
                    "Expected the copied file '%s' to be in the project resources",
                    fileName), filesInFlowResourcesFolder.contains(fileName));
        });

        final Set<String> generatedFiles = Stream
                .of(FrontendUtils.getFlowGeneratedFolder(frontendDirectory)
                        .listFiles())
                .map(File::getName).collect(Collectors.toSet());

        String generated = "'%s' should have been generated into 'build/frontend'";

        Assert.assertTrue(String.format(generated, FrontendUtils.IMPORTS_NAME),
                generatedFiles.contains(FrontendUtils.IMPORTS_NAME));
        Assert.assertTrue(
                String.format(generated, FrontendUtils.IMPORTS_D_TS_NAME),
                generatedFiles.contains(FrontendUtils.IMPORTS_D_TS_NAME));

        Assert.assertFalse("No 'target' directory should exist after build.",
                target.exists());
    }

    @Test
    public void should_UpdateMainJsFile() throws Exception {
        Assert.assertFalse(importsFile.exists());

        List<String> expectedLines = getExpectedImports();

        mojo.execute();

        assertContainsImports(true, expectedLines.toArray(new String[0]));

        Assert.assertTrue(
                new File(jarResourcesFolder, "/ExampleConnector.js").exists());
    }

    @Test
    public void shouldNot_UpdateJsFile_when_NoChanges() throws Exception {

        mojo.execute();
        long timestamp1 = importsFile.lastModified();

        // need to sleep because timestamp is in seconds
        sleep(1000);
        mojo.execute();
        long timestamp2 = importsFile.lastModified();

        Assert.assertEquals(timestamp1, timestamp2);
    }

    @Test
    public void should_ContainLumoThemeFiles() throws Exception {
        mojo.execute();

        assertContainsImports(true, "@vaadin/vaadin-lumo-styles/color.js",
                "@vaadin/vaadin-lumo-styles/typography.js",
                "@vaadin/vaadin-lumo-styles/sizing.js",
                "@vaadin/vaadin-lumo-styles/spacing.js",
                "@vaadin/vaadin-lumo-styles/style.js",
                "@vaadin/vaadin-lumo-styles/icons.js");
    }

    @Test
    public void shouldNot_ContainExternalUrls() throws Exception {
        mojo.execute();

        assertContainsImports(false, "https://foo.com/bar.js");
        assertContainsImports(false, "//foo.com/bar.js");
    }

    @Test
    public void should_AddImports() throws Exception {
        mojo.execute();
        removeImports("@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-template.js");
        assertContainsImports(false, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-template.js");

        mojo.execute();
        assertContainsImports(true, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-template.js");
    }

    @Test
    public void should_removeImports() throws Exception {
        mojo.execute();
        addImports("./added-import.js");
        assertContainsImports(true, "./added-import.js");

        mojo.execute();
        assertContainsImports(false, "./added-import.js");
    }

    @Test
    public void should_AddRemove_Imports() throws Exception {
        mojo.execute();

        removeImports("@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-template.js");
        addImports("./added-import.js");

        assertContainsImports(false, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-template.js");
        assertContainsImports(true, "./added-import.js");

        mojo.execute();

        assertContainsImports(true, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-template.js");
        assertContainsImports(false, "./added-import.js");
    }

    @Test
    public void mavenGoalWhenPackageJsonContainsDependencies_onlyFrameworkHandledDependencyIsTouched()
            throws Exception {
        ObjectNode json = TestUtils.getInitialPackageJson();
        ObjectNode dependencies = JacksonUtils.createObjectNode();
        dependencies.put("proj4", "2.9.0");
        dependencies.put("line-awesome", "1.3.0");
        // Make proj4 framework handled
        ((ObjectNode) json.get("vaadin").get("dependencies")).put("proj4",
                "2.9.0");
        json.set("dependencies", dependencies);
        FileUtils.fileWrite(packageJson, "UTF-8", json.toString());

        mojo.execute();
        ObjectNode packageObjectNode = getPackageJson(packageJson);
        dependencies = (ObjectNode) packageObjectNode.get("dependencies");

        assertContainsPackage(dependencies, "@vaadin/button",
                "@vaadin/vaadin-element-mixin");

        Assert.assertFalse("proj4 should have been removed",
                dependencies.has("proj4"));
        Assert.assertTrue("line-awesome should remain",
                dependencies.has("line-awesome"));
    }

    @Test
    public void existingTokenFile_parametersShouldBeRemoved()
            throws IOException, IllegalAccessException, MojoExecutionException,
            MojoFailureException {

        File projectBase = temporaryFolder.getRoot();
        File webpackOutputDirectory = new File(projectBase,
                VAADIN_WEBAPP_RESOURCES);
        File resourceOutputDirectory = new File(projectBase,
                VAADIN_SERVLET_RESOURCES);

        ReflectionUtils.setVariableValueInObject(mojo, "webpackOutputDirectory",
                webpackOutputDirectory);
        ReflectionUtils.setVariableValueInObject(mojo,
                "resourceOutputDirectory", resourceOutputDirectory);

        ObjectNode initialBuildInfo = JacksonUtils.createObjectNode();
        initialBuildInfo.put(SERVLET_PARAMETER_PRODUCTION_MODE, false);
        initialBuildInfo.put(Constants.NPM_TOKEN, "npm");
        initialBuildInfo.put(Constants.FRONTEND_TOKEN, "src/main/frontend");

        initialBuildInfo.put(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM,
                true);
        initialBuildInfo.put(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE, true);
        initialBuildInfo.put(
                InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE, true);
        initialBuildInfo.put(InitParameters.CI_BUILD, true);

        org.apache.commons.io.FileUtils.forceMkdir(tokenFile.getParentFile());
        org.apache.commons.io.FileUtils.write(tokenFile,
                initialBuildInfo.toPrettyString() + "\n", "UTF-8");

        mojo.execute();

        String json = org.apache.commons.io.FileUtils
                .readFileToString(tokenFile, "UTF-8");
        ObjectNode buildInfo = JacksonUtils.readTree(json);
        Assert.assertNull(
                "enable dev server token shouldn't be added " + "automatically",
                buildInfo.get(FRONTEND_HOTDEPLOY));
        Assert.assertNotNull("productionMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_PRODUCTION_MODE));
        Assert.assertNull("npmFolder should have been removed",
                buildInfo.get(Constants.NPM_TOKEN));
        Assert.assertNull("frontendFolder should have been removed",
                buildInfo.get(Constants.FRONTEND_TOKEN));

        Assert.assertNull(
                InitParameters.SERVLET_PARAMETER_ENABLE_PNPM
                        + "should have been removed",
                buildInfo.get(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM));
        Assert.assertNull(InitParameters.CI_BUILD + "should have been removed",
                buildInfo.get(InitParameters.CI_BUILD));
        Assert.assertNull(
                InitParameters.REQUIRE_HOME_NODE_EXECUTABLE
                        + "should have been removed",
                buildInfo.get(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE));
        Assert.assertNull(
                InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE
                        + "should have been removed",
                buildInfo.get(
                        InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE));
    }

    @Test
    public void existingTokenFile_defaultApplicationIdentifierWritten()
            throws IOException, MojoExecutionException, MojoFailureException {
        String expectedAppId = "app-" + StringUtil.getHash(
                "com.vaadin.testing:my-application", StandardCharsets.UTF_8);

        ObjectNode initialBuildInfo = JacksonUtils.createObjectNode();
        initialBuildInfo.put(SERVLET_PARAMETER_PRODUCTION_MODE, false);
        initialBuildInfo.put(Constants.NPM_TOKEN, "npm");
        initialBuildInfo.put(Constants.FRONTEND_TOKEN, "src/main/frontend");

        initialBuildInfo.put(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM,
                true);
        initialBuildInfo.put(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE, true);
        initialBuildInfo.put(
                InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE, true);
        initialBuildInfo.put(InitParameters.CI_BUILD, true);

        org.apache.commons.io.FileUtils.forceMkdir(tokenFile.getParentFile());
        org.apache.commons.io.FileUtils.write(tokenFile,
                initialBuildInfo.toPrettyString() + "\n", "UTF-8");

        mojo.execute();
        Assert.assertTrue("No token file could be found", tokenFile.exists());

        String json = org.apache.commons.io.FileUtils
                .readFileToString(tokenFile, "UTF-8");
        ObjectNode buildInfo = JacksonUtils.readTree(json);
        Assert.assertEquals(
                "Custom application identifier not written on token file",
                expectedAppId,
                buildInfo.get(APPLICATION_IDENTIFIER).textValue());
    }

    @Test
    public void existingTokenFile_customApplicationIdentifierWritten()
            throws IOException, MojoExecutionException, MojoFailureException,
            IllegalAccessException {
        String appId = "MY-APP-ID";
        ReflectionUtils.setVariableValueInObject(mojo, "applicationIdentifier",
                appId);

        ObjectNode initialBuildInfo = JacksonUtils.createObjectNode();
        initialBuildInfo.put(SERVLET_PARAMETER_PRODUCTION_MODE, false);
        initialBuildInfo.put(Constants.NPM_TOKEN, "npm");
        initialBuildInfo.put(Constants.FRONTEND_TOKEN, "src/main/frontend");

        initialBuildInfo.put(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM,
                true);
        initialBuildInfo.put(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE, true);
        initialBuildInfo.put(
                InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE, true);
        initialBuildInfo.put(InitParameters.CI_BUILD, true);

        org.apache.commons.io.FileUtils.forceMkdir(tokenFile.getParentFile());
        org.apache.commons.io.FileUtils.write(tokenFile,
                initialBuildInfo.toPrettyString() + "\n", "UTF-8");

        mojo.execute();
        Assert.assertTrue("No token file could be found", tokenFile.exists());

        String json = org.apache.commons.io.FileUtils
                .readFileToString(tokenFile, "UTF-8");
        ObjectNode buildInfo = JacksonUtils.readTree(json);
        Assert.assertEquals(
                "Custom application identifier not written on token file",
                appId, buildInfo.get(APPLICATION_IDENTIFIER).textValue());
    }

    @Test
    public void commercialComponent_noLicenseKey_commercialBannerEnabled_buildsWithCommercialBannerFlag()
            throws Throwable {

        ObjectNode initialBuildInfo = JacksonUtils.createObjectNode();
        tokenFile.getParentFile().mkdirs();
        Files.writeString(tokenFile.toPath(),
                initialBuildInfo.toPrettyString() + "\n",
                StandardCharsets.UTF_8);

        DefaultArtifact commercialComponent = createCommercialComponent();
        mojo.project.getArtifacts().add(commercialComponent);
        ReflectionUtils.setVariableValueInObject(mojo, "commercialWithBanner",
                true);

        runWithoutLicenseKeys(() -> {
            mojo.execute();

            String json = Files.readString(tokenFile.toPath(),
                    StandardCharsets.UTF_8);
            ObjectNode buildInfo = JacksonUtils.readTree(json);
            Assert.assertTrue(
                    "Commercial banner build token not written on token file",
                    buildInfo.get(COMMERCIAL_BANNER_TOKEN).booleanValue());
        });
    }

    @Test
    public void commercialComponent_noLicenseKey_commercialBannerNotEnabled_buildFails()
            throws Throwable {
        DefaultArtifact commercialComponent = createCommercialComponent();
        mojo.project.getArtifacts().add(commercialComponent);

        runWithoutLicenseKeys(() -> {
            Throwable exception = Assert
                    .assertThrows(MojoFailureException.class, mojo::execute);
            exception = exception.getCause();
            // Checking exception type by name because classes are loaded from
            // different classloaders
            while (exception != null && !exception.getClass().getName()
                    .equals(LicenseException.class.getName())) {
                exception = exception.getCause();
            }
            Assert.assertNotNull(
                    "Expected the build to fail because of LicenseException, but not found in stack trace",
                    exception);
            Assert.assertTrue(exception.getMessage()
                    .contains(InitParameters.COMMERCIAL_WITH_BANNER));
        });
    }

    @Test
    public void noTokenFile_tokenFileShouldBeCreated()
            throws MojoExecutionException, MojoFailureException {
        mojo.execute();

        Assert.assertTrue(tokenFile.exists());
    }

    @Test
    public void mavenGoal_generateOpenApiJson_when_itIsInClientSideMode()
            throws Exception {
        // Enable Hilla to generate openApi
        FileUtils.fileWrite(new File(frontendDirectory, "routes.tsx"), "UTF-8",
                """
                        import { serverSideRoutes } from "Frontend/generated/flow/Flow";
                        export const routes = [
                            {
                                element: <MainLayout />,
                                handle: { title: 'Main' }
                            },
                            ...serverSideRoutes
                        ] as RouteObject[];


                        export const router = createBrowserRouter(...routes]);
                        """);

        Assert.assertFalse(
                FileUtils.fileExists(openApiJsonFile.getAbsolutePath()));
        mojo.execute();
        Assert.assertTrue(
                FileUtils.fileExists(openApiJsonFile.getAbsolutePath()));
    }

    @Test
    public void mavenGoal_generateTsFiles_when_enabled() throws Exception {
        // Enable Hilla to generate ts files
        FileUtils.fileWrite(new File(frontendDirectory, "routes.tsx"), "UTF-8",
                """
                        import { serverSideRoutes } from "Frontend/generated/flow/Flow";
                        export const routes = [
                            {
                                element: <MainLayout />,
                                handle: { title: 'Main' }
                            },
                            ...serverSideRoutes
                        ] as RouteObject[];


                        export const router = createBrowserRouter(...routes]);
                        """);

        File connectClientApi = new File(generatedTsFolder,
                "connect-client.default.ts");
        File endpointClientApi = new File(generatedTsFolder, "MyEndpoint.ts");

        Assert.assertFalse(connectClientApi.exists());
        Assert.assertFalse(endpointClientApi.exists());
        mojo.execute();
        Assert.assertTrue(connectClientApi.exists());
        Assert.assertTrue(endpointClientApi.exists());
    }

    static void assertContainsPackage(JsonNode dependencies,
            String... packages) {
        Arrays.asList(packages).forEach(dep -> Assert
                .assertTrue("Missing " + dep, dependencies.has(dep)));
    }

    static void assertNotContainingPackages(JsonNode dependencies,
            String... packages) {
        Arrays.asList(packages).forEach(dep -> Assert
                .assertFalse("Not expecting " + dep, dependencies.has(dep)));
    }

    private void assertContainsImports(boolean contains, String... imports)
            throws IOException {
        String content = FileUtils.fileRead(importsFile);

        if (contains) {
            Arrays.asList(imports)
                    .forEach(s -> Assert.assertTrue(
                            s + " not found in:\n" + content,
                            content.contains(addFrontendPrefix(s))));
        } else {
            Arrays.asList(imports)
                    .forEach(s -> Assert.assertFalse(
                            s + " found in:\n" + content,
                            content.contains(addFrontendPrefix(s))));
        }
    }

    private String addFrontendPrefix(String s) {
        if (s.startsWith("./")) {
            return FrontendUtils.FRONTEND_FOLDER_ALIAS + s.substring(2);
        }
        return s;
    }

    private void removeImports(String... imports) throws IOException {
        List<String> importsList = Arrays.asList(imports);

        List<String> current = FileUtils.loadFile(importsFile);

        Set<String> removed = current.stream()
                .filter(line -> importsList.stream()
                        .map(this::addFrontendPrefix).anyMatch(line::contains))
                .collect(Collectors.toSet());

        current.removeAll(removed);

        String content = String.join("\n", current);

        replaceJsFile(content + "\n");
    }

    private void addImports(String... imports) throws IOException {
        String content = Arrays.stream(imports).map(this::addFrontendPrefix)
                .map(s -> "import '" + s + "';")
                .collect(Collectors.joining("\n"));

        replaceJsFile(content + "\n", StandardOpenOption.APPEND);
    }

    private void replaceJsFile(String content, OpenOption... options)
            throws IOException {
        Files.write(Paths.get(importsFile.toURI()),
                content.getBytes(StandardCharsets.UTF_8), options);
    }

    private List<String> getExpectedImports() {
        return Arrays.asList("@polymer/iron-icon/iron-icon.js",
                "@vaadin/vaadin-lumo-styles/spacing.js",
                "@vaadin/vaadin-lumo-styles/icons.js",
                "@vaadin/vaadin-lumo-styles/style.js",
                "@vaadin/vaadin-lumo-styles/typography.js",
                "@vaadin/vaadin-lumo-styles/color.js",
                "@vaadin/vaadin-lumo-styles/sizing.js",
                "@vaadin/vaadin-date-picker/src/vaadin-date-picker.js",
                "@vaadin/vaadin-date-picker/src/vaadin-month-calendar.js",
                "@vaadin/vaadin-element-mixin/vaadin-element-mixin.js",
                "@vaadin/vaadin-mixed-component/src/vaadin-mixed-component.js",
                "@vaadin/vaadin-mixed-component/src/vaadin-something-else.js",
                "./generated/jar-resources/ExampleConnector.js",
                "./local-p3-template.js", "./foo.js",
                "./vaadin-mixed-component/src/vaadin-mixed-component.js",
                "./local-template.js", "./foo-dir/vaadin-npm-component.js");
    }

    private void createExpectedImports(File directoryWithImportsJs,
            File nodeModulesPath) throws IOException {
        for (String expectedImport : getExpectedImports()) {
            if (expectedImport.startsWith("./generated/jar-resources/")) {
                File newFile = new File(jarResourcesSource, expectedImport
                        .substring("./generated/jar-resources/".length()));
                Assert.assertTrue(newFile.createNewFile());
            } else {
                File newFile = resolveImportFile(directoryWithImportsJs,
                        nodeModulesPath, expectedImport);
                newFile.getParentFile().mkdirs();
                Assert.assertTrue(newFile.createNewFile());
            }
        }
    }

    private File resolveImportFile(File directoryWithImportsJs,
            File nodeModulesPath, String jsImport) {
        File root = jsImport.startsWith("./") ? directoryWithImportsJs
                : nodeModulesPath;
        return new File(root, jsImport);
    }

    static void sleep(int ms) throws InterruptedException {
        Thread.sleep(ms); // NOSONAR
    }

    static ObjectNode getPackageJson(String packageJson) throws IOException {
        if (FileUtils.fileExists(packageJson)) {
            return JacksonUtils.readTree(FileUtils.fileRead(packageJson));

        } else {
            return JacksonUtils.createObjectNode();
        }
    }

    static List<String> getClassPath(Path projectFolder) {
        // Add folder with test classes
        List<String> classPaths = new ArrayList<>(Arrays.asList(
                projectFolder.resolve("target").resolve("test-classes")
                        .toString(),
                // Add this test jar which has some frontend resources
                // used in tests
                TestUtils.getTestJar("jar-with-frontend-resources.jar")
                        .getPath()));

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

    private List<File> gatherFiles(File root) {
        if (root.isFile()) {
            return Collections.singletonList(root);
        } else {
            File[] subdirectoryFiles = root.listFiles();
            if (subdirectoryFiles != null) {
                List<File> files = new ArrayList<>();
                for (File subdirectoryFile : subdirectoryFiles) {
                    files.addAll(gatherFiles(subdirectoryFile));
                }
                return files;
            }
            return Collections.emptyList();
        }
    }

    private DefaultArtifact createCommercialComponent()
            throws URISyntaxException {
        DefaultArtifactHandler artifactHandler = new DefaultArtifactHandler();
        artifactHandler.setAddedToClasspath(true);
        DefaultArtifact commercialComponent = new DefaultArtifact(
                "com.vaadin.testing", "commercial-component", "1.0", "compile",
                "jar", null, artifactHandler);
        commercialComponent.setFile(new File(
                getClass().getResource("/commercial-addon-1.0.0.jar").toURI()));
        return commercialComponent;
    }

    private void runWithoutLicenseKeys(ThrowingRunnable test) throws Throwable {
        String userHome = System.getProperty("user.home");
        File userHomeFolder = new File(userHome);
        Path vaadinHomeNodeFolder = userHomeFolder.toPath()
                .resolve(Path.of(".vaadin", "node"));
        File fakeUserHomeFolder = temporaryFolder.newFolder("fake-home");
        // Try to speed up test by copying existing node into the fake home
        if (Files.isDirectory(vaadinHomeNodeFolder)) {
            File fakeVaadinHomeNode = fakeUserHomeFolder.toPath()
                    .resolve(Path.of(".vaadin", "node")).toFile();
            fakeVaadinHomeNode.mkdirs();
            FileUtils.copyDirectoryStructure(vaadinHomeNodeFolder.toFile(),
                    fakeVaadinHomeNode);
        }
        try {
            System.setProperty("user.home",
                    fakeUserHomeFolder.getAbsolutePath());
            test.run();
        } finally {
            System.setProperty("user.home", userHome);
        }
    }

}
