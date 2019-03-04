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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.NpmPackage;
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

    UpdateNpmDependenciesMojo updateNpmDependenciesMojo = new UpdateNpmDependenciesMojo();

    @Before
    public void setup() throws IOException,
            DependencyResolutionRequiredException, IllegalAccessException {
        project = Mockito.mock(MavenProject.class);

        ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        URL[] urls = ((URLClassLoader) classLoader).getURLs();
        List<String> classPaths = new ArrayList<>(urls.length);

        for (URL url : urls) {
            classPaths.add(url.getFile());
        }

        // Make sure current test's classpath is included.
        String testClassPath = UpdateNpmDependenciesMojoTest.class.getProtectionDomain()
                .getCodeSource().getLocation().getFile();
        if (!classPaths.contains(testClassPath)) {
            classPaths.add(testClassPath);
        }

        Mockito.when(project.getRuntimeClasspathElements())
                .thenReturn(classPaths);

        ReflectionUtils.setVariableValueInObject(updateNpmDependenciesMojo, "project",
                project);
    }

    @After
    public void teardown() throws IOException {
        FileUtils.fileDelete("package.json");
    }

    @Test
    public void mavenGoal_packageJsonMissing() throws IOException {

        updateNpmDependenciesMojo.execute();

        String packageJson = FileUtils.fileRead("package.json");

        Assert.assertTrue("Missing @vaadin/vaadin-button package",
                packageJson.contains("@vaadin/vaadin-button"));
        Assert.assertTrue("Missing @webcomponents/webcomponentsjs package",
                packageJson.contains("@webcomponents/webcomponentsjs"));
    }

    @Test
    public void mavenGoal_packageJsonExists()
            throws IllegalAccessException, IOException {

        FileUtils.fileWrite("package.json", "{}");

        updateNpmDependenciesMojo.execute();

        String packageJson = FileUtils.fileRead("package.json");

        Assert.assertTrue("Missing @vaadin/vaadin-button package",
                packageJson.contains("@vaadin/vaadin-button"));
        Assert.assertTrue(
                "@webcomponents/webcomponentsjs exists though it shouldn't",
                !packageJson.contains("@webcomponents/webcomponentsjs"));
    }

    @NpmPackage("@vaadin/vaadin-button")
    class ButtonComponent extends Component {

    }

}