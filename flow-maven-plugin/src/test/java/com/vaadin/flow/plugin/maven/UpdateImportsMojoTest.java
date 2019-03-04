/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import static com.vaadin.flow.plugin.maven.UpdateImportsMojo.IMPORTS_FILE_PARAMETER;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;


public class UpdateImportsMojoTest {

    private MavenProject project;
    private UpdateImportsMojo importMojo = new UpdateImportsMojo();
    private String importsFile;

    @Before
    public void setup() throws IOException, DependencyResolutionRequiredException, IllegalAccessException {
        project = Mockito.mock(MavenProject.class);
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        URL[] urls = ((URLClassLoader) classLoader).getURLs();
        List<String> classPaths = new ArrayList<>(urls.length);

        for (URL url : urls) {
            classPaths.add(url.getFile());
        }

        Mockito.when(project.getRuntimeClasspathElements()).thenReturn(classPaths);

        ReflectionUtils.setVariableValueInObject(importMojo, "project", project);

        File tmp = File.createTempFile("foo", "");
        importsFile = tmp.getParent() + "/flow-imports.js";

        System.setProperty(IMPORTS_FILE_PARAMETER, importsFile);
    }

    @After
    public void teardown() throws IOException {
        System.clearProperty(IMPORTS_FILE_PARAMETER);
        FileUtils.fileDelete(importsFile);
    }

    @Test
    public void mavenGoal_packageJsonMissing() throws IOException {
        importMojo.execute();
        String content = FileUtils.fileRead(importsFile);
        Assert.assertEquals(
                "import '@vaadin/vaadin-component';\n" +
                "import 'vaadin-npm-component/vaadin-npm-component.js';\n" +
                "import './local-p3-template.js';\n" +
                "import './local-p2-template.js';\n" +
                "import 'vaadin-component/vaadin-mixed-component.js';", content);
    }

    @HtmlImport("frontend://bower_components/vaadin-component/vaadin-bower-component.html")
    public static class VaadinBowerComponent extends Component {
    }

    @NpmPackage("@vaadin/vaadin-npm-component")
    @JsModule("vaadin-npm-component/vaadin-npm-component.js")
    public static class VaadinNpmComponent extends Component {
    }

    @HtmlImport("frontend://bower_components/vaadin-component/vaadin-mixed-component.html")
    @NpmPackage("@vaadin/vaadin-component")
    @JsModule("vaadin-component/vaadin-mixed-component.js")
    public static class VaadinMixedComponent extends Component {
    }

    @HtmlImport("frontend://local-p2-template.html")
    public static class LocalP2Template extends Component {
    }

    @JsModule("./local-p3-template.js")
    public static class LocalP3Template extends Component {
    }
}
