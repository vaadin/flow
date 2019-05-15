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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
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

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_PREFIX_ALIAS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodeBuildFrontendMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File importsFile;
    private File generatedFolder;
    private File nodeModulesPath;
    private File flowPackagPath;
    private String mainPackage;
    private String appPackage;
    private String webpackConfig;

    private final NodeBuildFrontendMojo mojo = new NodeBuildFrontendMojo();

    @Before
    public void setup() throws Exception {
        MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getRuntimeClasspathElements()).thenReturn(getClassPath());

        File npmFolder = temporaryFolder.getRoot();
        generatedFolder = new File(npmFolder, DEFAULT_GENERATED_DIR);
        importsFile = new File(generatedFolder, IMPORTS_NAME);
        nodeModulesPath = new File(npmFolder, NODE_MODULES);
        flowPackagPath = new File(nodeModulesPath, FLOW_NPM_PACKAGE_NAME);
        File frontendDirectory = new File(npmFolder, DEFAULT_FRONTEND_DIR);

        mainPackage = new File(npmFolder, PACKAGE_JSON).getAbsolutePath();
        appPackage = new File(generatedFolder, PACKAGE_JSON).getAbsolutePath();
        webpackConfig = new File(npmFolder, WEBPACK_CONFIG).getAbsolutePath();

        ReflectionUtils.setVariableValueInObject(mojo, "project", project);
        ReflectionUtils.setVariableValueInObject(mojo, "generatedFolder", generatedFolder);
        ReflectionUtils.setVariableValueInObject(mojo, "frontendDirectory", frontendDirectory);
        ReflectionUtils.setVariableValueInObject(mojo, "generateEmbeddableWebComponents", false);
        ReflectionUtils.setVariableValueInObject(mojo, "convertHtml", true);
        ReflectionUtils.setVariableValueInObject(mojo, "npmFolder", npmFolder);
        ReflectionUtils.setVariableValueInObject(mojo, "generateBundle", false);
        ReflectionUtils.setVariableValueInObject(mojo, "runNpmInstall", false);

        flowPackagPath.mkdirs();
        generatedFolder.mkdirs();

        setProject(mojo, "war", "war_output");

        // Install all imports used in the tests on node_modules so as we don't
        // need to run `npm install`
        createExpectedImports(frontendDirectory, nodeModulesPath);
        FileUtils.fileWrite(mainPackage, "UTF-8", "{}");
        FileUtils.fileWrite(appPackage, "UTF-8", "{}");
    }

    @After
    public void teardown() {
        FileUtils.fileDelete(mainPackage);
        FileUtils.fileDelete(appPackage);
        FileUtils.fileDelete(webpackConfig);
    }

    static void setProject(AbstractMojo mojo, String packaging, String outputDirectory) throws Exception {
        Build buildMock = mock(Build.class);
        when(buildMock.getOutputDirectory()).thenReturn(outputDirectory);
        when(buildMock.getDirectory()).thenReturn(outputDirectory);
        when(buildMock.getFinalName()).thenReturn("finalName");

        MavenProject project = mock(MavenProject.class);
        when(project.getBasedir()).thenReturn(new File("."));
        when(project.getPackaging()).thenReturn(packaging);
        when(project.getBuild()).thenReturn(buildMock);
        when(project.getRuntimeClasspathElements()).thenReturn(getClassPath());
        ReflectionUtils.setVariableValueInObject(mojo, "project", project);
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

        Assert.assertTrue(new File(flowPackagPath, "ExampleConnector.js").exists());
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

        assertContainsImports(true,
                "@vaadin/vaadin-lumo-styles/color.js",
                "@vaadin/vaadin-lumo-styles/typography.js",
                "@vaadin/vaadin-lumo-styles/sizing.js",
                "@vaadin/vaadin-lumo-styles/spacing.js",
                "@vaadin/vaadin-lumo-styles/style.js",
                "@vaadin/vaadin-lumo-styles/icons.js");
    }

    @Test
    public void should_AddImports() throws Exception {
        mojo.execute();
        removeImports("@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");
        assertContainsImports(false, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");

        mojo.execute();
        assertContainsImports(true, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");
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
                "./local-p2-template.js");
        addImports("./added-import.js");

        assertContainsImports(false, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");
        assertContainsImports(true, "./added-import.js");

        mojo.execute();

        assertContainsImports(true, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");
        assertContainsImports(false, "./added-import.js");
    }

    @Test
    public void mavenGoal_when_packageJsonExists() throws Exception {
        FileUtils.fileWrite(appPackage, "{\"dependencies\":{\"foo\":\"bar\"}}");

        mojo.execute();
        JsonObject packageJsonObject = getPackageJson(appPackage);
        JsonObject dependencies = packageJsonObject.getObject("dependencies");

        assertContainsPackage(dependencies,
            "@polymer/iron-icon",
            "@vaadin/vaadin-button",
            "@vaadin/vaadin-date-picker",
            "@vaadin/vaadin-element-mixin",
            "@vaadin/vaadin-core-shrinkwrap");

        Assert.assertFalse("Has foo", dependencies.hasKey("foo"));
    }

    static void assertContainsPackage(JsonObject dependencies, String... packages) {
        Arrays.asList(packages)
            .forEach(dep -> Assert.assertTrue("Missing " + dep, dependencies.hasKey(dep)));
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
        return Arrays.asList(
            "@polymer/iron-icon/iron-icon.js",
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
            "./frontend-p3-template.js",
            "./local-p3-template.js",
            "./foo.js",
            "./vaadin-mixed-component/theme/lumo/vaadin-mixed-component.js",
            "./local-p2-template.js",
            "./foo-dir/vaadin-npm-component.js"
        );
    }

    private void createExpectedImports(File directoryWithImportsJs, File nodeModulesPath) throws IOException {
        for (String expectedImport : getExpectedImports()) {
            File newFile = resolveImportFile(directoryWithImportsJs, nodeModulesPath, expectedImport);
            newFile.getParentFile().mkdirs();
            Assert.assertTrue(newFile.createNewFile());
        }
    }

    private File resolveImportFile(File directoryWithImportsJs, File nodeModulesPath, String jsImport) {
        File root = jsImport.startsWith("./") ? directoryWithImportsJs : nodeModulesPath;
        return new File(root, jsImport);
    }


    static void sleep(int ms) throws InterruptedException {
        Thread.sleep(ms); //NOSONAR
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
        List<String> classPaths = new ArrayList<>(Arrays.asList(
                "target/test-classes",
                // Add this test jar which has some frontend resources used in tests
                TestUtils.getTestJar("jar-with-frontend-resources.jar").getPath()
        ));

        // Add other paths already present in the system classpath
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL[] urls = ((URLClassLoader) classLoader).getURLs();
        for (URL url : urls) {
            classPaths.add(url.getFile());
        }

        return classPaths;
    }
}
