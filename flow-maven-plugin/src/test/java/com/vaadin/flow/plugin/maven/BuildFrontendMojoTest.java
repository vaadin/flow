/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import com.vaadin.flow.plugin.TestUtils;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_ENABLE_DEV_SERVER;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_PREFIX_ALIAS;
import static java.io.File.pathSeparator;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildFrontendMojoTest {
    public static final String TEST_PROJECT_RESOURCE_JS = "test_project_resource.js";
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File importsFile;
    private File generatedFolder;
    private File nodeModulesPath;
    private File flowPackagPath;
    private File projectFrontendResourcesDirectory;
    private String mainPackage;
    private String appPackage;
    private String webpackConfig;

    private File tokenFile;

    private final BuildFrontendMojo mojo = new BuildFrontendMojo();

    @Before
    public void setup() throws Exception {
        MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getRuntimeClasspathElements())
                .thenReturn(getClassPath());

        tokenFile = new File(temporaryFolder.getRoot(),
                VAADIN_SERVLET_RESOURCES + TOKEN_FILE);

        File npmFolder = temporaryFolder.getRoot();
        generatedFolder = new File(npmFolder, DEFAULT_GENERATED_DIR);
        importsFile = new File(generatedFolder, IMPORTS_NAME);
        nodeModulesPath = new File(npmFolder, NODE_MODULES);
        flowPackagPath = new File(nodeModulesPath, FLOW_NPM_PACKAGE_NAME);
        File frontendDirectory = new File(npmFolder, DEFAULT_FRONTEND_DIR);

        mainPackage = new File(npmFolder, PACKAGE_JSON).getAbsolutePath();
        appPackage = new File(generatedFolder, PACKAGE_JSON).getAbsolutePath();
        webpackConfig = new File(npmFolder, WEBPACK_CONFIG).getAbsolutePath();

        projectFrontendResourcesDirectory = new File(npmFolder,
                "flow_resources");

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
        ReflectionUtils.setVariableValueInObject(mojo, "compatibilityMode",
                "false");
        ReflectionUtils.setVariableValueInObject(mojo, "optimizeBundle",
                true);

        flowPackagPath.mkdirs();
        generatedFolder.mkdirs();

        setProject(mojo, npmFolder);

        // Install all imports used in the tests on node_modules so as we don't
        // need to run `npm install`
        createExpectedImports(frontendDirectory, nodeModulesPath);
        FileUtils.fileWrite(mainPackage, "UTF-8", "{}");
        FileUtils.fileWrite(appPackage, "UTF-8", "{}");
    }

    @After
    public void teardown() throws IOException {
        if (FileUtils.fileExists(mainPackage)) {
            FileUtils.fileDelete(mainPackage);
        }
        if (FileUtils.fileExists(appPackage)) {
            FileUtils.fileDelete(appPackage);
        }
        if (FileUtils.fileExists(webpackConfig)) {
            FileUtils.fileDelete(webpackConfig);
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
    public void should_copyProjectFrontendResources()
            throws MojoExecutionException, MojoFailureException {

        List<File> initialFiles = gatherFiles(nodeModulesPath);
        initialFiles.forEach(file -> Assert.assertFalse(String.format(
                "Test resource shouldn't exist before running mojo.", file),
                TEST_PROJECT_RESOURCE_JS.equals(file.getName())));
        mojo.execute();

        Set<String> projectFrontendResources = Stream
                .of(projectFrontendResourcesDirectory.listFiles())
                .map(File::getName).collect(Collectors.toSet());
        List<File> filesInNodeModules = gatherFiles(nodeModulesPath);
        filesInNodeModules.removeAll(initialFiles);

        Assert.assertEquals(
                "All project resources should be copied into the node_modules",
                projectFrontendResources.size(), filesInNodeModules.size());

        filesInNodeModules.forEach(file -> Assert.assertTrue(String.format(
                "Expected the copied file '%s' to be in the project resources",
                file), projectFrontendResources.contains(file.getName())));
    }

    @Test
    public void should_UpdateMainJsFile() throws Exception {
        Assert.assertFalse(importsFile.exists());

        List<String> expectedLines = new ArrayList<>(Arrays.asList(
                "const div = document.createElement('div');",
                "div.innerHTML = '<custom-style><style include=\"lumo-color lumo-typography\"></style></custom-style>';",
                "document.head.insertBefore(div.firstElementChild, document.head.firstChild);",
                "document.body.setAttribute('theme', 'dark');"));
        expectedLines.addAll(getExpectedImports());

        mojo.execute();

        assertContainsImports(true, expectedLines.toArray(new String[0]));

        Assert.assertTrue(
                new File(flowPackagPath, "ExampleConnector.js").exists());
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
    public void mavenGoal_when_packageJsonExists() throws Exception {
        FileUtils.fileWrite(appPackage, "{\"dependencies\":{\"foo\":\"bar\"}}");

        mojo.execute();
        JsonObject packageJsonObject = getPackageJson(appPackage);
        JsonObject dependencies = packageJsonObject.getObject("dependencies");

        assertContainsPackage(dependencies, "@vaadin/vaadin-button",
                "@vaadin/vaadin-element-mixin");

        Assert.assertFalse("Has foo", dependencies.hasKey("foo"));
    }

    @Test
    public void existingTokenFile_enableDevServerShouldBeAdded()
            throws IOException, IllegalAccessException, MojoExecutionException,
            MojoFailureException {

        File projectBase = temporaryFolder.getRoot();
        File webpackOutputDirectory = new File(projectBase,
                VAADIN_SERVLET_RESOURCES);

        ReflectionUtils.setVariableValueInObject(mojo, "generatedFolder",
                projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "webpackOutputDirectory",
                webpackOutputDirectory);

        JsonObject initialBuildInfo = Json.createObject();
        initialBuildInfo.put(SERVLET_PARAMETER_COMPATIBILITY_MODE, false);
        initialBuildInfo.put(SERVLET_PARAMETER_PRODUCTION_MODE, false);
        initialBuildInfo.put("npmFolder", "npm");
        initialBuildInfo.put("generatedFolder", "generated");
        initialBuildInfo.put("frontendFolder", "frontend");
        org.apache.commons.io.FileUtils.forceMkdir(tokenFile.getParentFile());
        org.apache.commons.io.FileUtils.write(tokenFile,
                JsonUtil.stringify(initialBuildInfo, 2) + "\n", "UTF-8");

        mojo.execute();

        String json = org.apache.commons.io.FileUtils
                .readFileToString(tokenFile, "UTF-8");
        JsonObject buildInfo = JsonUtil.parse(json);
        Assert.assertNotNull("devMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_ENABLE_DEV_SERVER));
        Assert.assertNotNull("compatibilityMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_COMPATIBILITY_MODE));
        Assert.assertNotNull("productionMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_PRODUCTION_MODE));
        Assert.assertNull("npmFolder should have been removed",
                buildInfo.get("npmFolder"));
        Assert.assertNull("generatedFolder should have been removed",
                buildInfo.get("generatedFolder"));
        Assert.assertNull("frontendFolder should have been removed",
                buildInfo.get("frontendFolder"));
    }

    @Test
    public void noTokenFile_noTokenFileShouldBeCreated()
            throws MojoExecutionException, MojoFailureException {
        mojo.execute();

        Assert.assertFalse(tokenFile.exists());
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
                            content.contains(addWebpackPrefix(s))));
        } else {
            Arrays.asList(imports)
                    .forEach(s -> Assert.assertFalse(
                            s + " found in:\n" + content,
                            content.contains(addWebpackPrefix(s))));
        }
    }

    private String addWebpackPrefix(String s) {
        if (s.startsWith("./")) {
            return WEBPACK_PREFIX_ALIAS + s.substring(2);
        }
        return s;
    }

    private void removeImports(String... imports) throws IOException {
        List<String> importsList = Arrays.asList(imports);

        List<String> current = FileUtils.loadFile(importsFile);

        Set<String> removed = current
                .stream().filter(line -> importsList.stream()
                        .map(this::addWebpackPrefix).anyMatch(line::contains))
                .collect(Collectors.toSet());

        current.removeAll(removed);

        String content = String.join("\n", current);

        replaceJsFile(content + "\n");
    }

    private void addImports(String... imports) throws IOException {
        String content = Arrays.stream(imports).map(this::addWebpackPrefix)
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
                "@vaadin/vaadin-date-picker/theme/lumo/vaadin-date-picker.js",
                "@vaadin/vaadin-date-picker/src/vaadin-month-calendar.js",
                "@vaadin/vaadin-element-mixin/vaadin-element-mixin.js",
                "@vaadin/vaadin-mixed-component/theme/lumo/vaadin-mixed-component.js",
                "@vaadin/vaadin-mixed-component/theme/lumo/vaadin-something-else.js",
                "@vaadin/flow-frontend/ExampleConnector.js",
                "./frontend-p3-template.js", "./local-p3-template.js",
                "./foo.js",
                "./vaadin-mixed-component/theme/lumo/vaadin-mixed-component.js",
                "./local-template.js", "./foo-dir/vaadin-npm-component.js");
    }

    private void createExpectedImports(File directoryWithImportsJs,
            File nodeModulesPath) throws IOException {
        for (String expectedImport : getExpectedImports()) {
            File newFile = resolveImportFile(directoryWithImportsJs,
                    nodeModulesPath, expectedImport);
            newFile.getParentFile().mkdirs();
            Assert.assertTrue(newFile.createNewFile());
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
