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

import static com.vaadin.flow.plugin.maven.UpdateNpmDependenciesMojo.PACKAGE_JSON;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import elemental.json.Json;
import elemental.json.JsonObject;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class UpdateNpmDependenciesMojoTest {

    MavenProject project;

    UpdateNpmDependenciesMojo mojo = new UpdateNpmDependenciesMojo();

    String packageJsonFileName;

    @Before
    public void setup() throws IOException, DependencyResolutionRequiredException, IllegalAccessException {
        project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getRuntimeClasspathElements()).thenReturn(getClassPath());

        File tmp = File.createTempFile("foo", "");
        tmp.delete();
        packageJsonFileName = tmp.getParent() + "/" + PACKAGE_JSON;

        System.err.println(packageJsonFileName);

        ReflectionUtils.setVariableValueInObject(mojo, "project", project);
        ReflectionUtils.setVariableValueInObject(mojo, "npmFolder", tmp.getParent());
        ReflectionUtils.setVariableValueInObject(mojo, "convertHtml", true);
    }

    static List<String> getClassPath() {
        // Add folder with test classes
        List<String> classPaths = new ArrayList<>(Arrays.asList("target/test-classes"));

        // Add other paths already present in the system classpath
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL[] urls = ((URLClassLoader) classLoader).getURLs();
        for (URL url : urls) {
            classPaths.add(url.getFile());
        }
        return classPaths;
    }

    @After
    public void teardown() throws IOException {
        FileUtils.fileDelete(packageJsonFileName);
    }

    @Test
    public void mavenGoal_packageJsonMissing() throws IOException {
        Assert.assertFalse(FileUtils.fileExists(packageJsonFileName));

        mojo.execute();

        assertPackageJsonContent();
    }

    @Test
    public void mavenGoal_packageJsonExists() throws Exception {

        FileUtils.fileWrite(packageJsonFileName, "{}");
        long timestamp1 = FileUtils.getFile(packageJsonFileName).lastModified();

        // need to sleep because timestamp is in seconds
        sleep(1000);
        mojo.execute();
        long timestamp2 = FileUtils.getFile(packageJsonFileName).lastModified();

        sleep(1000);
        mojo.execute();
        long timestamp3 = FileUtils.getFile(packageJsonFileName).lastModified();

        Assert.assertTrue(timestamp1 < timestamp2);
        Assert.assertTrue(timestamp2 == timestamp3);

        assertPackageJsonContent();
    }

    private void assertPackageJsonContent() throws IOException {
        JsonObject packageJson = getPackageJson();

        JsonObject dependencies = packageJson.getObject("dependencies");

        Assert.assertTrue("Missing @vaadin/vaadin-button package",
                dependencies.hasKey("@vaadin/vaadin-button"));
        Assert.assertTrue("Missing @webcomponents/webcomponentsjs package",
                dependencies.hasKey("@webcomponents/webcomponentsjs"));
        Assert.assertTrue("Missing @polymer/iron-icon package",
                dependencies.hasKey("@polymer/iron-icon"));

        JsonObject devDependencies = packageJson.getObject("devDependencies");

        Assert.assertTrue("Missing webpack dev package",
                devDependencies.hasKey("webpack"));
        Assert.assertTrue("Missing webpack-cli dev package",
                devDependencies.hasKey("webpack-cli"));
        Assert.assertTrue("Missing webpack-dev-server dev package",
                devDependencies.hasKey("webpack-dev-server"));
        Assert.assertTrue("Missing webpack-plugin-install-deps dev package",
                devDependencies.hasKey("webpack-plugin-install-deps"));
        Assert.assertTrue("Missing webpack-babel-multi-target-plugin dev package",
                devDependencies.hasKey("webpack-babel-multi-target-plugin"));
        Assert.assertTrue("Missing copy-webpack-plugin dev package",
                devDependencies.hasKey("copy-webpack-plugin"));
    }

    static void sleep(int ms) throws InterruptedException {
        Thread.sleep(ms); //NOSONAR
    }

    private JsonObject getPackageJson() throws IOException {
        if (FileUtils.fileExists(packageJsonFileName)) {
            return Json.parse(FileUtils.fileRead(packageJsonFileName));

        } else {
            return Json.createObject();
        }
    }

}
