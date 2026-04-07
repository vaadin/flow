/*
 * Copyright 2000-2026 Vaadin Ltd.
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
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.Template;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.testutil.TestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskCopyTemplateFilesTest {

    @Tag("my-lit-element-view")
    @JsModule("./my-lit-element-view.js")
    public static class MyLitElementView implements Template {
    }

    @TempDir
    File temporaryFolder;

    private File projectDirectory;
    private File resourceOutputDirectory;

    private ClassFinder finder;

    @BeforeEach
    void setup() throws IOException, ClassNotFoundException {
        // creating non-existing folder to make sure the execute() creates
        // the folder if missing
        projectDirectory = Files
                .createTempDirectory(temporaryFolder.toPath(), "tmp").toFile();
        resourceOutputDirectory = new File(projectDirectory,
                "target/" + Constants.VAADIN_SERVLET_RESOURCES);
        finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getSubTypesOf(Template.class))
                .thenReturn(Collections.singleton(MyLitElementView.class));
        Class clazz = JsModule.class;
        Mockito.when(finder.loadClass(JsModule.class.getName()))
                .thenReturn(clazz);
    }

    @Test
    void should_copyTemplateFilesFromDefaultFrontendDirectory()
            throws Exception {
        executeTaskCopyTemplateFiles(FrontendUtils.FRONTEND);
    }

    @Test
    void should_copyTemplateFilesFromCustomFrontendDirectory()
            throws Exception {
        executeTaskCopyTemplateFiles("frontend-custom");
    }

    private void executeTaskCopyTemplateFiles(String frontedDirectoryName)
            throws Exception {
        // prepare frontend resource
        File frontendDirectory = new File(projectDirectory,
                frontedDirectoryName);
        frontendDirectory.mkdirs();
        new File(frontendDirectory, "my-lit-element-view.js").createNewFile();

        Options options = new Options(Mockito.mock(Lookup.class),
                projectDirectory)
                .withBuildResultFolders(frontendDirectory,
                        resourceOutputDirectory)
                .withFrontendDirectory(frontendDirectory);
        TaskCopyTemplateFiles task = new TaskCopyTemplateFiles(finder, options);
        task.execute();

        List<String> files = TestUtils
                .listFilesRecursively(resourceOutputDirectory);
        assertTrue(
                files.stream().anyMatch(
                        file -> file.contains("my-lit-element-view.js")),
                "TS resource should have been copied");
    }

}
