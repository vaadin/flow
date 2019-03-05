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

import static com.vaadin.flow.plugin.maven.UpdateNpmDependenciesMojoTest.getClassPath;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


public class UpdateImportsMojoTest {

    private MavenProject project;
    private UpdateImportsMojo mojo = new UpdateImportsMojo();
    private String importsFile;

    @Before
    public void setup() throws IOException, DependencyResolutionRequiredException, IllegalAccessException {
        project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getRuntimeClasspathElements()).thenReturn(getClassPath());

        File tmp = File.createTempFile("foo", "");
        importsFile = tmp.getParent() + "/flow-imports.js";

        ReflectionUtils.setVariableValueInObject(mojo, "project", project);
        ReflectionUtils.setVariableValueInObject(mojo, "jsFile", importsFile);
        ReflectionUtils.setVariableValueInObject(mojo, "convertHtml", true);
    }

    @After
    public void teardown() throws IOException {
        FileUtils.fileDelete(importsFile);
    }

    @Test
    public void should_updateMainJsFile() throws IOException {
        mojo.execute();
        String content = FileUtils.fileRead(importsFile);

        Arrays.asList(
                "@polymer/iron-icon",
                "./foo-dir/vaadin-npm-component.js",
                "./bar-dir/vaadin-mixed-component.js",
                "@vaadin/vaadin-element-mixin",
                "./local-p3-template.js",
                "./foo.js",
                "./local-p2-template.js")
        .forEach(s -> Assert.assertTrue(content.contains("import '" + s + "';")));
    }
}
