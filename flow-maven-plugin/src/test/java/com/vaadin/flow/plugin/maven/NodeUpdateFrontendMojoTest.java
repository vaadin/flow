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

import com.vaadin.flow.plugin.TestUtils;
import com.vaadin.flow.server.frontend.NodeUpdater;
import elemental.json.Json;
import elemental.json.JsonObject;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.WebpackUpdater.WEBPACK_CONFIG;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodeUpdateFrontendMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private File importsFile;
    private File nodeModulesPath;

    private String packageJson;
    private String webpackConfig;

    private final NodeUpdateFrontendMojo mojo = new NodeUpdateFrontendMojo();

    @Before
    public void setup() throws Exception {
        MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getRuntimeClasspathElements()).thenReturn(getClassPath());

        File tmpRoot = temporaryFolder.getRoot();
        importsFile = new File(tmpRoot, "flow-imports.js");
        nodeModulesPath = new File(tmpRoot, "node_modules");

        packageJson = new File(tmpRoot, PACKAGE_JSON).getAbsolutePath();
        webpackConfig = new File(tmpRoot, WEBPACK_CONFIG).getAbsolutePath();

        ReflectionUtils.setVariableValueInObject(mojo, "project", project);
        ReflectionUtils.setVariableValueInObject(mojo, "jsFile", importsFile);
        ReflectionUtils.setVariableValueInObject(mojo, "convertHtml", true);
        ReflectionUtils.setVariableValueInObject(mojo, "npmFolder", tmpRoot);
        ReflectionUtils.setVariableValueInObject(mojo, "nodeModulesPath", nodeModulesPath);
        ReflectionUtils.setVariableValueInObject(mojo, "generateBundle", false);
        ReflectionUtils.setVariableValueInObject(mojo, "webpackTemplate", WEBPACK_CONFIG);

        Assert.assertTrue(getFlowPackage().mkdirs());

        setProject("war", "war_output");

        createExpectedImports(importsFile.getParentFile(), nodeModulesPath);
    }

    @After
    public void teardown() {
        FileUtils.fileDelete(packageJson);
        FileUtils.fileDelete(webpackConfig);
    }
    
    private void setProject(String packaging, String outputDirectory) throws Exception {
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

        Assert.assertTrue(getFlowPackage().exists());
        Assert.assertTrue(new File(getFlowPackage(), "ExampleConnector.js").exists());
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
    public void assertWebpackContent_jar() throws Exception {
        Assert.assertFalse(FileUtils.fileExists(webpackConfig));
        final String expectedOutput = "jar_output";
        setProject("jar", expectedOutput);

        mojo.execute();

        Files.lines(Paths.get(webpackConfig))
                .peek(line -> Assert.assertFalse(line.contains("{{")))
                .filter(line -> line.contains(expectedOutput))
                .findAny()
                .orElseThrow(() -> new AssertionError(String.format(
                        "Did not find expected output directory '%s' in the resulting webpack config",
                        expectedOutput)));
    }

    @Test
    public void assertWebpackContent_war() throws Exception {
        Assert.assertFalse(FileUtils.fileExists(webpackConfig));
        String expectedOutput = "war_output";
        setProject("war", expectedOutput);

        mojo.execute();

        Files.lines(Paths.get(webpackConfig))
                .peek(line -> Assert.assertFalse(line.contains("{{")))
                .filter(line -> line.contains(expectedOutput))
                .findAny()
                .orElseThrow(() -> new AssertionError(String.format(
                        "Did not find expected output directory '%s' in the resulting webpack config",
                        expectedOutput)));
    }

    @Test
    public void assertWebpackContent_NotWarNotJar() throws Exception {
        String unexpectedPackaging = "notWarAndNotJar";

        setProject(unexpectedPackaging, "whatever");

        exception.expect(IllegalStateException.class);
        exception.expectMessage(unexpectedPackaging);
        mojo.execute();
    }

    @Test
    public void mavenGoal_when_packageJsonMissing() throws Exception {
        Assert.assertFalse(FileUtils.fileExists(packageJson));

        mojo.execute();

        assertPackageJsonContent();

        Assert.assertTrue(FileUtils.fileExists(webpackConfig));
    }

    @Test
    public void mavenGoal_when_packageJsonExists() throws Exception {

        FileUtils.fileWrite(packageJson, "{}");
        long tsPackage1 = FileUtils.getFile(packageJson).lastModified();
        long tsWebpack1 = FileUtils.getFile(webpackConfig).lastModified();

        // need to sleep because timestamp is in seconds
        sleep(1000);
        mojo.execute();
        long tsPackage2 = FileUtils.getFile(packageJson).lastModified();
        long tsWebpack2 = FileUtils.getFile(webpackConfig).lastModified();

        sleep(1000);
        mojo.execute();
        long tsPackage3 = FileUtils.getFile(packageJson).lastModified();
        long tsWebpack3 = FileUtils.getFile(webpackConfig).lastModified();

        Assert.assertTrue(tsPackage1 < tsPackage2);
        Assert.assertTrue(tsWebpack1 < tsWebpack2);
        Assert.assertEquals(tsPackage2, tsPackage3);
        Assert.assertEquals(tsWebpack2, tsWebpack3);

        assertPackageJsonContent();
    }

    private void assertPackageJsonContent() throws IOException {
        JsonObject packageJsonObject = getPackageJson();

        JsonObject dependencies = packageJsonObject.getObject("dependencies");

        Assert.assertTrue("Missing @vaadin/vaadin-button package",
                dependencies.hasKey("@vaadin/vaadin-button"));
        Assert.assertTrue("Missing @webcomponents/webcomponentsjs package",
                dependencies.hasKey("@webcomponents/webcomponentsjs"));
        Assert.assertTrue("Missing @polymer/iron-icon package",
                dependencies.hasKey("@polymer/iron-icon"));

        JsonObject devDependencies = packageJsonObject.getObject("devDependencies");

        Assert.assertTrue("Missing webpack dev package",
                devDependencies.hasKey("webpack"));
        Assert.assertTrue("Missing webpack-cli dev package",
                devDependencies.hasKey("webpack-cli"));
        Assert.assertTrue("Missing webpack-dev-server dev package",
                devDependencies.hasKey("webpack-dev-server"));
        Assert.assertTrue("Missing webpack-babel-multi-target-plugin dev package",
                devDependencies.hasKey("webpack-babel-multi-target-plugin"));
        Assert.assertTrue("Missing copy-webpack-plugin dev package",
                devDependencies.hasKey("copy-webpack-plugin"));
    }

    private void assertContainsImports(boolean contains, String... imports)
            throws IOException {
        String content = FileUtils.fileRead(importsFile);

        if (contains) {
            Arrays.asList(imports)
                    .forEach(s -> Assert.assertTrue(
                            s + " not found in:\n" + content,
                            content.contains(s)));
        } else {
            Arrays.asList(imports).forEach(s -> Assert.assertFalse(
                    s + " found in:\n" + content, content.contains(s)));
        }
    }

    private void removeImports(String... imports) throws IOException {
        List<String> importsList = Arrays.asList(imports);

        List<String> current = FileUtils.loadFile(importsFile);

        Set<String> removed = current.stream()
                .filter(line -> importsList.stream()
                        .anyMatch(line::contains))
                .collect(Collectors.toSet());

        current.removeAll(removed);

        String content = String.join("\n", current);

        replaceJsFile(content + "\n");
    }

    private void addImports(String... imports) throws IOException {
        String content = Arrays.stream(imports)
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

    private void deleteExpectedImports(File directoryWithImportsJs, File nodeModulesPath) {
        for (String expectedImport : getExpectedImports()) {
            Assert.assertTrue(resolveImportFile(directoryWithImportsJs, nodeModulesPath, expectedImport).delete());
        }
    }

    private File resolveImportFile(File directoryWithImportsJs, File nodeModulesPath, String jsImport) {
        File root = jsImport.startsWith("./") ? directoryWithImportsJs : nodeModulesPath;
        return new File(root, jsImport);
    }

    private File getFlowPackage() {
        return NodeUpdater.getFlowPackage(nodeModulesPath);
    }

    static void sleep(int ms) throws InterruptedException {
        Thread.sleep(ms); //NOSONAR
    }

    private JsonObject getPackageJson() throws IOException {
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
