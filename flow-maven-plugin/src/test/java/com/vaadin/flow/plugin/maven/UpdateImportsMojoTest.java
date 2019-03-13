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
import static com.vaadin.flow.plugin.maven.UpdateNpmDependenciesMojoTest.sleep;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    public void should_UpdateMainJsFile() throws IOException {
        Assert.assertFalse(FileUtils.fileExists(importsFile));

        mojo.execute();

        assertContainsImports(true,
                "const div = document.createElement('div');",
                "div.innerHTML = '<custom-style><style include=\"lumo-color lumo-typography\"></style></custom-style>';",
                "document.head.insertBefore(div.firstElementChild, document.head.firstChild);",
                "document.body.setAttribute('theme', 'dark');",
                "@vaadin/vaadin-lumo-styles/icons.js",
                "@vaadin/vaadin-lumo-styles/spacing.js",
                "@vaadin/vaadin-lumo-styles/typography.js",
                "@vaadin/vaadin-lumo-styles/color.js",
                "@polymer/iron-icon/iron-icon.js",
                "./foo-dir/vaadin-npm-component.js",
                "./vaadin-mixed-component/theme/lumo/vaadin-mixed-component.js",
                "@vaadin/vaadin-element-mixin/theme/lumo/vaadin-element-mixin.js",
                "@vaadin/vaadin-element-mixin/src/something-else.js",
                "./local-p3-template.js",
                "@polymer/iron-icon",
                "./foo-dir/vaadin-npm-component.js",
                "./local-p3-template.js",
                "./foo.js",
                "./local-p2-template.js");
    }

    @Test
    public void shouldNot_UpdateJsFile_when_NoChanges() throws Exception {
        mojo.execute();
        long timestamp1 = FileUtils.getFile(importsFile).lastModified();

        // need to sleep because timestamp is in seconds
        sleep(1000);
        mojo.execute();
        long timestamp2 = FileUtils.getFile(importsFile).lastModified();

        Assert.assertTrue(timestamp1 == timestamp2);
    }

    @Test
    public void should_ContainLumoThemeFiles() throws IOException {
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
    public void should_AddImports() throws IOException {
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
    public void should_removeImports() throws IOException {
        mojo.execute();
        addImports("./added-import.js");
        assertContainsImports(true, "./added-import.js");

        mojo.execute();
        assertContainsImports(false, "./added-import.js");
    }

    @Test
    public void should_AddRemove_Imports() throws IOException {
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

        List<String> current = FileUtils.loadFile(new File(importsFile));

        Set<String> removed = current.stream()
                .filter(line -> importsList.stream()
                        .filter(jsImport -> line.contains(jsImport)).findAny()
                        .isPresent())
                .collect(Collectors.toSet());

        current.removeAll(removed);

        String content = current.stream().collect(Collectors.joining("\n"));

        replaceJsFile(content + "\n");
    }

    private void addImports(String... imports) throws IOException {
        String content = Arrays.asList(imports).stream()
                .map(s -> "import '" + s + "';")
                .collect(Collectors.joining("\n"));

        replaceJsFile(content + "\n", StandardOpenOption.APPEND);
    }

    private void replaceJsFile(String content, OpenOption... options)
            throws IOException {
        Files.write(Paths.get(new File(importsFile).toURI()),
                content.getBytes("UTF-8"), options);
    }

}
