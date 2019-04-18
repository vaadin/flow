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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import com.vaadin.flow.plugin.TestUtils;
import com.vaadin.flow.server.frontend.NodeUpdateImports;
import com.vaadin.flow.server.frontend.NodeUpdatePackages;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class UpdateNpmDependenciesMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private String packageJson;
    private String webpackConfig;

    private final NodeUpdatePackagesMojo mojo = new NodeUpdatePackagesMojo();

    @Before
    public void setup() throws Exception {
        File baseDir = temporaryFolder.getRoot();

        packageJson = new File(baseDir, PACKAGE_JSON).getAbsolutePath();
        webpackConfig = new File(baseDir, WEBPACK_CONFIG).getAbsolutePath();

        ReflectionUtils.setVariableValueInObject(mojo, "npmFolder", baseDir);
        ReflectionUtils.setVariableValueInObject(mojo, "nodeModulesPath", new File(baseDir, "node_modules"));
        ReflectionUtils.setVariableValueInObject(mojo, "generatedFlowImports", new File(baseDir, NodeUpdateImports.FLOW_IMPORTS_FILE));
        ReflectionUtils.setVariableValueInObject(mojo, "convertHtml", true);
        ReflectionUtils.setVariableValueInObject(mojo, "frontendDirectory", new File(baseDir, "frontend"));
        ReflectionUtils.setVariableValueInObject(mojo, "webpackTemplate", WEBPACK_CONFIG);
        setProject("war", "war_output");
    }

    private void stubNpmInstall() throws Exception {
        NodeUpdatePackages updater = (NodeUpdatePackages)mojo.getUpdater();
        NodeUpdatePackages spy = Mockito.spy(updater);
        Mockito.doAnswer(invocation -> {
                System.err.println("Skipping npm install");
                return null;
        })
        .when(spy).executeNpmInstall(Mockito.any());
        ReflectionUtils.setVariableValueInObject(mojo, "updater", spy);
    }

    private void setProject(String packaging, String outputDirectory) throws Exception {
        String baseDir = temporaryFolder.getRoot().getPath() + File.separator;
        Build buildMock = mock(Build.class);
        when(buildMock.getOutputDirectory()).thenReturn(baseDir + outputDirectory);
        when(buildMock.getDirectory()).thenReturn(baseDir + outputDirectory);
        when(buildMock.getFinalName()).thenReturn("finalName");

        MavenProject project = mock(MavenProject.class);
        when(project.getBasedir()).thenReturn(new File(baseDir));
        when(project.getPackaging()).thenReturn(packaging);
        when(project.getBuild()).thenReturn(buildMock);
        when(project.getRuntimeClasspathElements()).thenReturn(getClassPath());
        ReflectionUtils.setVariableValueInObject(mojo, "project", project);
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

    @After
    public void teardown() {
        FileUtils.fileDelete(packageJson);
        FileUtils.fileDelete(webpackConfig);
    }

    @Test
    public void assertWebpackContent_jar() throws Exception {
        Assert.assertFalse(FileUtils.fileExists(webpackConfig));
        final String expectedOutput = "jar_output";
        setProject("jar", expectedOutput);

        stubNpmInstall();
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

        stubNpmInstall();
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

        stubNpmInstall();
        mojo.execute();
    }

    @Test
    public void mavenGoal_packageJsonMissing() throws Exception {
        Assert.assertFalse(FileUtils.fileExists(packageJson));

        stubNpmInstall();
        mojo.execute();

        assertPackageJsonContent();

        Assert.assertTrue(FileUtils.fileExists(webpackConfig));
    }

    @Test
    public void mavenGoal_packageJsonExists() throws Exception {

        FileUtils.fileWrite(packageJson, "{}");
        long tsPackage1 = FileUtils.getFile(packageJson).lastModified();
        long tsWebpack1 = FileUtils.getFile(webpackConfig).lastModified();

        stubNpmInstall();

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

}
