/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.plugin.TestUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.InitParameters.FRONTEND_HOTDEPLOY;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_D_TS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;
import static com.vaadin.flow.server.frontend.FrontendUtils.VITE_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.VITE_GENERATED_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND_FOLDER_ALIAS;
import static java.io.File.pathSeparator;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getRuntimeClasspathElements())
                .thenReturn(getClassPath());

        List<String> packages = Arrays
                .stream(System.getProperty("java.class.path")
                        .split(File.pathSeparatorChar + ""))
                .collect(Collectors.toList());
        Mockito.when(project.getRuntimeClasspathElements())
                .thenReturn(packages);
        Mockito.when(project.getCompileClasspathElements())
                .thenReturn(Collections.emptyList());

        projectBase = temporaryFolder.getRoot();

        tokenFile = new File(temporaryFolder.getRoot(),
                VAADIN_SERVLET_RESOURCES + TOKEN_FILE);

        File npmFolder = projectBase;
        nodeModulesPath = new File(npmFolder, NODE_MODULES);
        frontendDirectory = new File(npmFolder, DEFAULT_FRONTEND_DIR);
        importsFile = FrontendUtils.getFlowGeneratedImports(frontendDirectory);
        jarResourcesFolder = new File(
                new File(frontendDirectory, FrontendUtils.GENERATED),
                FrontendUtils.JAR_RESOURCES_FOLDER);
        packageJson = new File(npmFolder, PACKAGE_JSON).getAbsolutePath();
        viteConfig = new File(npmFolder, VITE_CONFIG).getAbsolutePath();
        viteGenerated = new File(npmFolder, VITE_GENERATED_CONFIG)
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
        openApiJsonFile = new File(npmFolder,
                "target/classes/dev/hilla/openapi.json");
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
        Mockito.when(mojo.getJarFiles()).thenReturn(
                Set.of(jarResourcesSource.getParentFile().getParentFile()));

        setProject(mojo, npmFolder);

        // Install all imports used in the tests on node_modules so as we don't
        // need to run `npm install`
        createExpectedImports(frontendDirectory, nodeModulesPath);
        FileUtils.fileWrite(packageJson, "UTF-8",
                TestUtils.getInitialPackageJson().toJson());

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
        Build buildMock = mock(Build.class);
        when(buildMock.getFinalName()).thenReturn("finalName");
        MavenProject project = mock(MavenProject.class);
        when(project.getBasedir()).thenReturn(baseFolder);
        when(project.getBuild()).thenReturn(buildMock);
        when(project.getRuntimeClasspathElements()).thenReturn(getClassPath());
        ReflectionUtils.setVariableValueInObject(mojo, "project", project);
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

        Assert.assertTrue(String.format(generated, IMPORTS_NAME),
                generatedFiles.contains(IMPORTS_NAME));
        Assert.assertTrue(String.format(generated, IMPORTS_D_TS_NAME),
                generatedFiles.contains(IMPORTS_D_TS_NAME));

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
        JsonObject json = TestUtils.getInitialPackageJson();
        JsonObject dependencies = Json.createObject();
        dependencies.put("proj4", "2.9.0");
        dependencies.put("line-awesome", "1.3.0");
        // Make proj4 framework handled
        json.getObject("vaadin").getObject("dependencies").put("proj4",
                "2.9.0");
        json.put("dependencies", dependencies);
        FileUtils.fileWrite(packageJson, "UTF-8", json.toJson());

        mojo.execute();
        JsonObject packageJsonObject = getPackageJson(packageJson);
        dependencies = packageJsonObject.getObject("dependencies");

        assertContainsPackage(dependencies, "@vaadin/button",
                "@vaadin/vaadin-element-mixin");

        Assert.assertFalse("proj4 should have been removed",
                dependencies.hasKey("proj4"));
        Assert.assertTrue("line-awesome should remain",
                dependencies.hasKey("line-awesome"));
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

        JsonObject initialBuildInfo = Json.createObject();
        initialBuildInfo.put(SERVLET_PARAMETER_PRODUCTION_MODE, false);
        initialBuildInfo.put(Constants.NPM_TOKEN, "npm");
        initialBuildInfo.put(Constants.FRONTEND_TOKEN, "frontend");

        initialBuildInfo.put(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM,
                true);
        initialBuildInfo.put(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE, true);
        initialBuildInfo.put(
                InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE, true);
        initialBuildInfo.put(InitParameters.CI_BUILD, true);

        org.apache.commons.io.FileUtils.forceMkdir(tokenFile.getParentFile());
        org.apache.commons.io.FileUtils.write(tokenFile,
                JsonUtil.stringify(initialBuildInfo, 2) + "\n", "UTF-8");

        mojo.execute();

        String json = org.apache.commons.io.FileUtils
                .readFileToString(tokenFile, "UTF-8");
        JsonObject buildInfo = JsonUtil.parse(json);
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
    public void noTokenFile_noTokenFileShouldBeCreated()
            throws MojoExecutionException, MojoFailureException {
        mojo.execute();

        Assert.assertFalse(tokenFile.exists());
    }

    @Test
    public void mavenGoal_generateOpenApiJson_when_itIsInClientSideMode()
            throws Exception {
        Assert.assertFalse(
                FileUtils.fileExists(openApiJsonFile.getAbsolutePath()));
        mojo.execute();
        Assert.assertTrue(
                FileUtils.fileExists(openApiJsonFile.getAbsolutePath()));
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

    static void assertContainsPackage(JsonObject dependencies,
            String... packages) {
        Arrays.asList(packages).forEach(dep -> Assert
                .assertTrue("Missing " + dep, dependencies.hasKey(dep)));
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
            return FRONTEND_FOLDER_ALIAS + s.substring(2);
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
                "./vaadin-mixed-component/theme/lumo/vaadin-mixed-component.js",
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

    static JsonObject getPackageJson(String packageJson) throws IOException {
        if (FileUtils.fileExists(packageJson)) {
            return Json.parse(FileUtils.fileRead(packageJson));

        } else {
            return Json.createObject();
        }
    }

    static List<String> getClassPath() {
        // Add folder with test classes
        List<String> classPaths = new ArrayList<>(
                Arrays.asList("target/test-classes",
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
}
